package featurecreep.fcdependencies.core.dangerzone;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DangerZoneZipService {

	private static final String RUNTIME_URL = "https://download1351.mediafire.com/qog9xu11fapgYBDEyxG1zAutTZgx-C4K2SS9RBAWizWeVELInRVSOqs2mF0g_kN_kW_gqpf9Frbp459IdkCzbOJIUzzsux8hMzWDf-rsKJgh_PdYJfFNZcR5QVUlPjUq-p8QzEf5R2tXRo1vzgIG9jNAEJqrOjmF9NNNkIYdMyzNLQ/ph3apn92ftvl8l8/DZVR2.8.zip";
	private static final String SOURCES_URL = "https://download1523.mediafire.com/lgriy7onquggMfXq0IzSKxxs22BD25R_T50OZS7uc1LMgnfS7lbZN8NC1XWt9J0B6Pdd-cUuEPIB8SwUSUNimgSnwzz-zCUi3wHrDZkjX0tn8gf9nNOGCrv7BM6kqFhAEOGDH48ftkLXQMqcn6jFZwl_-185KVTOhLgO8h4I9FujcA/vzqt67jmw51gls2/DZVR_2.8_FULL_MULTIPLAYER_buildkit.zip";
	private static final String RUNTIME_JAR_NAME = "DangerZone.jar";

	public File downloadRuntimeZip(File workingDir) throws Exception {
		return downloadFile(RUNTIME_URL, new File(workingDir, "dangerzone-runtime.zip"));
	}

	public File downloadSourcesZip(File workingDir) throws Exception {
		return downloadFile(SOURCES_URL, new File(workingDir, "dangerzone-sources.zip"));
	}

	public File extractRuntimeJar(File zipFile, File workingDir) throws Exception {

		File out = new File(workingDir, "dangerzone-2.7.jar");
		if (out.exists() && out.length() > 0) {
			return out;
		}

		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (!entry.isDirectory() && matchesFileName(entry.getName(), RUNTIME_JAR_NAME)) {
					Files.copy(zis, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
					return out;
				}
			}
		}

		throw new IllegalStateException("Could not find " + RUNTIME_JAR_NAME + " in " + zipFile.getName());
	}

	public File extractSourcesDirectory(File zipFile, File workingDir) throws Exception {

		File sourcesRoot = new File(workingDir, "sources");
		Path sourcePath = sourcesRoot.toPath();
		if (Files.exists(sourcePath)) {
			return sourcesRoot;
		}

		Files.createDirectories(sourcePath);

		boolean foundSource = false;
		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}

				String normalized = entry.getName().replace('\\', '/');
				String relative = extractRelativeSourcePath(normalized);
				if (relative == null) {
					continue;
				}

				Path target = sourcePath.resolve(relative).normalize();
				if (!target.startsWith(sourcePath)) {
					throw new IllegalStateException("Blocked zip entry outside sources dir: " + normalized);
				}

				Files.createDirectories(target.getParent());
				Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
				foundSource = true;
			}
		}

		if (!foundSource) {
			throw new IllegalStateException("Could not find src/ directory contents in " + zipFile.getName());
		}

		return sourcesRoot;
	}

	private File downloadFile(String url, File out) throws Exception {

		File parent = out.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}

		if (out.exists() && out.length() > 0) {
			return out;
		}

		try (InputStream in = new URL(url).openStream()) {
			Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

		return out;
	}

	private boolean matchesFileName(String entryName, String expectedFileName) {
		String normalized = entryName.replace('\\', '/');
		int slash = normalized.lastIndexOf('/');
		String fileName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
		return expectedFileName.equals(fileName);
	}

	private String extractRelativeSourcePath(String entryName) {
		String normalized = entryName.replace('\\', '/');
		int srcIndex = normalized.indexOf("src/");
		if (srcIndex < 0) {
			return null;
		}

		String relative = normalized.substring(srcIndex + 4);
		if (relative.isBlank()) {
			return null;
		}

		return relative;
	}
}
