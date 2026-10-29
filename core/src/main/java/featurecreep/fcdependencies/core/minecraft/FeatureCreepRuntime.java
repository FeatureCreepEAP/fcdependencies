package featurecreep.fcdependencies.core.minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * The standalone Mixin runtime FeatureCreep loads for vanilla launches. FeatureCreep's installer stages these jars
 * into the launcher's libraries directory and the launcher passes that directory as {@code libraryDirectory}; this
 * mirrors that layout so the launch can find them without any network access.
 */
public final class FeatureCreepRuntime {

	public static final String FABRIC_REPOSITORY = "https://maven.fabricmc.net/";

	private static final String ASM_VERSION = "9.8";
	private static final List<String> MIXIN_COORDINATES = List.of(
			"net.fabricmc:sponge-mixin:0.17.4+mixin.0.8.7",
			"io.github.llamalad7:mixinextras-common:0.5.5",
			"org.jboss.modules:jboss-modules:2.3.0",
			"org.ow2.asm:asm:" + ASM_VERSION,
			"org.ow2.asm:asm-analysis:" + ASM_VERSION,
			"org.ow2.asm:asm-commons:" + ASM_VERSION,
			"org.ow2.asm:asm-tree:" + ASM_VERSION,
			"org.ow2.asm:asm-util:" + ASM_VERSION);

	public record Library(String group, String artifact, String version, Path file) {
	}

	private FeatureCreepRuntime() {
	}

	public static List<String> libraryCoordinates(String minecraftVersion, FeatureCreepVersions versions) {
		List<String> coordinates = new ArrayList<>(LaunchPlanner.featureCreepCoordinates(minecraftVersion, versions));
		coordinates.addAll(MIXIN_COORDINATES);
		return coordinates;
	}

	public static Path stage(Path libraryDirectory, Library library) throws IOException {
		Path dir = libraryDirectory.resolve(library.group().replace('.', '/')).resolve(library.artifact())
				.resolve(library.version());
		Files.createDirectories(dir);
		Path target = dir.resolve(library.artifact() + "-" + library.version() + ".jar");
		Files.copy(library.file(), target, StandardCopyOption.REPLACE_EXISTING);
		return target;
	}
}
