package featurecreep.fcdependencies.core.minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.jboss.dmr.ModelNode;

public final class MinecraftDownloader {

	private static final String ASSET_BASE_URL = "https://resources.download.minecraft.net/";
	private static final int ASSET_THREADS = 8;

	private final Consumer<String> log;

	public MinecraftDownloader(Consumer<String> log) {
		this.log = log;
	}

	public Path downloadSide(ModelNode versionJson, MinecraftSide side, Path root) throws IOException {
		Path target = root.resolve(side == MinecraftSide.CLIENT ? "client.jar" : "server.jar");
		ModelNode download = versionJson.get("downloads").get(side.getJarKey());
		download(download.get("url").asString(), target, download.get("sha1").asString(), -1L);
		return target;
	}

	public void downloadLibraries(List<LibraryResolver.Artifact> artifacts, Path libraryDir) throws IOException {
		for (LibraryResolver.Artifact artifact : artifacts) {
			download(artifact.url(), libraryDir.resolve(artifact.path()), artifact.sha1(), artifact.size());
		}
	}

	public void downloadAssets(ModelNode versionJson, Path assetsDir) throws IOException {
		if (!versionJson.hasDefined("assetIndex")) return;

		ModelNode assetIndex = versionJson.get("assetIndex");
		Path indexFile = assetsDir.resolve("indexes").resolve(assetIndex.get("id").asString() + ".json");
		download(assetIndex.get("url").asString(), indexFile, assetIndex.get("sha1").asString(), -1L);

		ModelNode index = ModelNode.fromJSONString(Files.readString(indexFile));
		List<String> hashes = new ArrayList<>();
		if (index.hasDefined("objects")) {
			for (String name : index.get("objects").keys()) {
				hashes.add(index.get("objects").get(name).get("hash").asString());
			}
		}

		ExecutorService pool = Executors.newFixedThreadPool(ASSET_THREADS);
		try {
			List<Future<?>> futures = new ArrayList<>();
			for (String hash : hashes) {
				String prefix = hash.substring(0, 2);
				futures.add(pool.submit(() -> {
					download(ASSET_BASE_URL + prefix + "/" + hash,
							assetsDir.resolve("objects").resolve(prefix).resolve(hash), hash, -1L);
					return null;
				}));
			}
			for (Future<?> future : futures) {
				future.get();
			}
		} catch (Exception e) {
			throw new IOException("Asset download failed", e);
		} finally {
			pool.shutdownNow();
		}
		log.accept("Assets ready: " + hashes.size() + " objects");
	}

	/** Downloads to a temporary file first, and skips the download when the existing file already matches. */
	void download(String url, Path target, String expected, long size) throws IOException {
		if (Files.isRegularFile(target) && matches(target, expected, size)) return;

		Files.createDirectories(target.getParent());
		Path temp = target.resolveSibling(target.getFileName() + ".part");
		log.accept("Downloading " + target.getFileName());
		try (InputStream in = URI.create(url).toURL().openStream()) {
			Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
		}
		if (!matches(temp, expected, size)) {
			Files.deleteIfExists(temp);
			throw new IOException("Checksum or size mismatch for " + url);
		}
		Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
	}

	static boolean matches(Path file, String expectedSha1, long expectedSize) throws IOException {
		if (expectedSize >= 0 && Files.size(file) != expectedSize) return false;
		return expectedSha1 == null || expectedSha1.isEmpty() || expectedSha1.equalsIgnoreCase(sha1(file));
	}

	static String sha1(Path file) throws IOException {
		try (InputStream in = Files.newInputStream(file)) {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) != -1) {
				digest.update(buffer, 0, read);
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}
}
