package featurecreep.fcdependencies.core.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FeatureCreepRuntimeTest {

	private static final FeatureCreepVersions VERSIONS = new FeatureCreepVersions("12", "12", "12", "12");

	@TempDir
	Path temp;

	@Test
	void libraryCoordinatesIncludeTheStandaloneMixinRuntime() {
		var coordinates = FeatureCreepRuntime.libraryCoordinates("26.3", VERSIONS);

		assertTrue(coordinates.contains("net.fabricmc:sponge-mixin:0.17.4+mixin.0.8.7"));
		assertTrue(coordinates.contains("org.ow2.asm:asm-util:9.8"));
		assertTrue(coordinates.contains("com.asbestosstar:featurecreep-bootstrap:12"));
	}

	@Test
	void stagesLibraryIntoMavenLayout() throws IOException {
		Path source = Files.writeString(temp.resolve("sponge-mixin.jar"), "jar");
		Path libraries = temp.resolve("libraries");

		Path staged = FeatureCreepRuntime.stage(libraries,
				new FeatureCreepRuntime.Library("net.fabricmc", "sponge-mixin", "0.17.4+mixin.0.8.7", source));

		assertEquals(libraries.resolve("net/fabricmc/sponge-mixin/0.17.4+mixin.0.8.7/sponge-mixin-0.17.4+mixin.0.8.7.jar"),
				staged);
		assertEquals("jar", Files.readString(staged));
	}
}
