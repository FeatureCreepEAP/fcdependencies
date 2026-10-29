package featurecreep.fcdependencies.core.minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.zip.ZipFile;

/** Puts a project's own FeatureCreep module jar where FeatureCreep discovers mods during runClient and runServer. */
public final class FeatureCreepModInstaller {

	static final String MODULE_DESCRIPTOR = "module.xml";
	static final String MODS_DIRECTORY = "mods";

	private FeatureCreepModInstaller() {
	}

	public static Optional<Path> install(Path jar, Path runDir) throws IOException {
		if (!isModule(jar)) return Optional.empty();
		Path mods = Files.createDirectories(runDir.resolve(MODS_DIRECTORY));
		Path target = mods.resolve(jar.getFileName().toString());
		Files.copy(jar, target, StandardCopyOption.REPLACE_EXISTING);
		return Optional.of(target);
	}

	static boolean isModule(Path jar) throws IOException {
		try (ZipFile zip = new ZipFile(jar.toFile())) {
			return zip.getEntry(MODULE_DESCRIPTOR) != null;
		}
	}
}
