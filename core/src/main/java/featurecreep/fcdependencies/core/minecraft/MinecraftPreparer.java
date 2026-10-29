package featurecreep.fcdependencies.core.minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.jboss.dmr.ModelNode;

/** Downloads the Minecraft files needed to launch one side of a version into a version directory. */
public final class MinecraftPreparer {

	private MinecraftPreparer() {
	}

	public static void prepare(String version, MinecraftSide side, Path root, Consumer<String> log) throws Exception {
		Files.createDirectories(root.resolve("natives"));

		ModelNode json = new MinecraftVersionManifestService().getVersionJson(version);
		Files.writeString(root.resolve(LaunchPlanner.VERSION_JSON), json.toJSONString(true));

		MinecraftDownloader downloader = new MinecraftDownloader(log);
		downloader.downloadSide(json, side, root);

		if (side == MinecraftSide.CLIENT) {
			downloader.downloadLibraries(LibraryResolver.resolve(json, Platform.current()), root.resolve("libraries"));
			downloader.downloadAssets(json, root.resolve("assets"));
		}
		log.accept("Minecraft " + version + " " + side.name().toLowerCase() + " ready in " + root.toAbsolutePath());
	}
}
