package featurecreep.fcdependencies.core.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LaunchPlannerTest {

	private static final FeatureCreepVersions VERSIONS = new FeatureCreepVersions("1", "2", "3", "4");

	@TempDir
	Path temp;

	@Test
	void coordinatesCoverEveryFeatureCreepArtifact() {
		assertEquals(List.of(
				"com.asbestosstar:featurecreep-bootstrap:2",
				"com.asbestosstar:featurecreep-loader:1",
				"com.asbestosstar:featurecreep-api:3",
				"com.asbestosstar:featurecreepmc-26.3:4"),
				LaunchPlanner.featureCreepCoordinates("26.3", VERSIONS));
	}

	@Test
	void vanillaServerUsesManifestMainClass() throws IOException {
		Path versionDir = Files.createDirectories(temp.resolve("version"));
		Path serverJar = jarWithMainClass(versionDir, "server.jar", "net.minecraft.server.Main");
		Path runDir = temp.resolve("run");

		LaunchPlanner.Plan plan = LaunchPlanner.server(versionDir, runDir, RunMode.VANILLA, List.of());

		assertEquals("net.minecraft.server.Main", plan.mainClass());
		assertEquals(List.of(serverJar.toString()), plan.classpath());
		assertEquals(List.of("nogui"), plan.args());
		assertTrue(plan.jvmArgs().isEmpty());
		assertTrue(Files.isDirectory(runDir));
	}

	@Test
	void featureCreepServerRunsThroughBootstrapAgent() throws IOException {
		Path versionDir = Files.createDirectories(temp.resolve("version"));
		Path serverJar = jarWithMainClass(versionDir, "server.jar", "net.minecraft.server.Main");
		Path bootstrap = jarWithMainClass(temp, "featurecreep-bootstrap-2.jar", "asbestosstar.bootstrap.FeatureCreepMain");
		Path loader = jarWithMainClass(temp, "featurecreep-loader-1.jar", "example.Loader");

		LaunchPlanner.Plan plan = LaunchPlanner.server(versionDir, temp.resolve("run"), RunMode.FEATURECREEP,
				List.of(bootstrap.toString(), loader.toString()));

		assertEquals("asbestosstar.bootstrap.minecraft.VanillaMinecraftServerLauncher", plan.mainClass());
		assertEquals(List.of("-Dfeaturecreep.minecraft.host=vanilla", "-Dfeaturecreep.launch.managed=true",
				"-DlibraryDirectory=" + versionDir.resolve("libraries"), "-javaagent:" + bootstrap), plan.jvmArgs());
		assertEquals(List.of(serverJar.toString(), bootstrap.toString(), loader.toString()), plan.classpath());
		assertEquals(List.of("nogui"), plan.args());
	}

	@Test
	void featureCreepWithoutBootstrapJarFails() throws IOException {
		Path versionDir = Files.createDirectories(temp.resolve("version"));
		jarWithMainClass(versionDir, "server.jar", "net.minecraft.server.Main");

		assertThrows(IllegalStateException.class,
				() -> LaunchPlanner.server(versionDir, temp.resolve("run"), RunMode.FEATURECREEP, List.of()));
	}

	@Test
	void describeNamesTheLaunchDetails() {
		LaunchPlanner.Plan plan = new LaunchPlanner.Plan("net.minecraft.server.Main", List.of("a.jar"),
				List.of("-Dx=1"), List.of("nogui"), temp);

		List<String> lines = LaunchPlanner.describe(plan, "/jdk/bin/java");

		assertTrue(lines.contains("java: /jdk/bin/java"));
		assertTrue(lines.contains("main class: net.minecraft.server.Main"));
		assertTrue(lines.contains("jvm args: -Dx=1"));
		assertTrue(lines.contains("program args: nogui"));
	}

	@Test
	void runModeParsesCaseInsensitively() {
		assertEquals(RunMode.FEATURECREEP, RunMode.parse("FeatureCreep"));
		assertEquals(RunMode.VANILLA, RunMode.parse("vanilla"));
	}

	@Test
	void runModeRejectsUnknownValues() {
		assertThrows(IllegalArgumentException.class, () -> RunMode.parse("forge"));
	}

	@Test
	void featureCreepVersionsRequireEveryField() {
		assertThrows(IllegalArgumentException.class, () -> new FeatureCreepVersions("1", " ", "3", "4"));
	}

	private static Path jarWithMainClass(Path dir, String name, String mainClass) throws IOException {
		Path jar = dir.resolve(name);
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);
		try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
			out.flush();
		}
		return jar;
	}
}
