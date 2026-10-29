package featurecreep.fcdependencies.core.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FeatureCreepModInstallerTest {

	@TempDir
	Path temp;

	@Test
	void copiesModuleJarIntoModsDirectory() throws IOException {
		Path jar = jarWithEntries(temp.resolve("mod.jar"), FeatureCreepModInstaller.MODULE_DESCRIPTOR);
		Path runDir = temp.resolve("run");

		Optional<Path> installed = FeatureCreepModInstaller.install(jar, runDir);

		Path expected = runDir.resolve("mods").resolve("mod.jar");
		assertEquals(Optional.of(expected), installed);
		assertTrue(Files.exists(expected));
	}

	@Test
	void replacesPreviouslyInstalledCopy() throws IOException {
		Path runDir = temp.resolve("run");
		Path first = jarWithEntries(temp.resolve("first/mod.jar"), FeatureCreepModInstaller.MODULE_DESCRIPTOR);
		Path rebuilt = jarWithEntries(temp.resolve("second/mod.jar"), FeatureCreepModInstaller.MODULE_DESCRIPTOR, "rebuilt.marker");
		FeatureCreepModInstaller.install(first, runDir);

		FeatureCreepModInstaller.install(rebuilt, runDir);

		try (ZipFile installed = new ZipFile(runDir.resolve("mods").resolve("mod.jar").toFile())) {
			assertNotNull(installed.getEntry("rebuilt.marker"));
		}
	}

	@Test
	void ignoresJarsWithoutModuleDescriptor() throws IOException {
		Path jar = jarWithEntries(temp.resolve("library.jar"), "com/example/Library.class");
		Path runDir = temp.resolve("run");

		assertEquals(Optional.empty(), FeatureCreepModInstaller.install(jar, runDir));
		assertFalse(Files.exists(runDir.resolve("mods")));
	}

	private static Path jarWithEntries(Path jar, String... entries) throws IOException {
		Files.createDirectories(jar.getParent());
		try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
			for (String entry : entries) {
				out.putNextEntry(new ZipEntry(entry));
				out.closeEntry();
			}
		}
		return jar;
	}
}
