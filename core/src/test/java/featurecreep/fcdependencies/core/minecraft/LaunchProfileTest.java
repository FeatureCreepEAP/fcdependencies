package featurecreep.fcdependencies.core.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.Test;

class LaunchProfileTest {

	private static final Platform MAC = new Platform("osx", "arm64");
	private static final Platform LINUX = new Platform("linux", "x86_64");

	private static final LaunchProfile.Layout LAYOUT = new LaunchProfile.Layout(
			Path.of("/mc/client.jar"), Path.of("/mc/libraries"), Path.of("/mc/assets"),
			Path.of("/mc/natives"), Path.of("/mc/run"), "Dev");

	private static final String MODERN_JSON = """
			{
			  "id": "26.3",
			  "mainClass": "net.minecraft.client.main.Main",
			  "assetIndex": {"id": "26", "url": "https://example/26.json", "sha1": "s"},
			  "libraries": [
			    {"name": "a:b:1", "downloads": {"artifact": {"path": "a/b/1/b-1.jar", "url": "u", "sha1": "s", "size": 1}}}
			  ],
			  "arguments": {
			    "jvm": [
			      "-Djava.library.path=${natives_directory}",
			      {"rules": [{"action": "allow", "os": {"name": "osx"}}], "value": "-XstartOnFirstThread"},
			      "-cp", "${classpath}"
			    ],
			    "game": [
			      "--username", "${auth_player_name}",
			      "--version", "${version_name}",
			      "--gameDir", "${game_directory}",
			      "--assetsDir", "${assets_root}",
			      "--assetIndex", "${assets_index_name}",
			      {"rules": [{"action": "allow", "features": {"is_demo_user": true}}], "value": "--demo"}
			    ]
			  }
			}
			""";

	private static final String LEGACY_JSON = """
			{
			  "id": "1.8",
			  "mainClass": "net.minecraft.client.main.Main",
			  "assets": "legacy",
			  "minecraftArguments": "--username ${auth_player_name} --version ${version_name} --assetsDir ${assets_root}"
			}
			""";

	@Test
	void classpathStartsWithClientJarThenLibraries() {
		LaunchProfile profile = LaunchProfile.forClient(ModelNode.fromJSONString(MODERN_JSON), LINUX, LAYOUT);

		assertEquals(List.of(
				Path.of("/mc/client.jar").toString(),
				Path.of("/mc/libraries/a/b/1/b-1.jar").toString()), profile.classpath());
	}

	@Test
	void modernArgumentsSubstitutePlaceholdersAndApplyRules() {
		LaunchProfile profile = LaunchProfile.forClient(ModelNode.fromJSONString(MODERN_JSON), MAC, LAYOUT);

		assertEquals("net.minecraft.client.main.Main", profile.mainClass());
		assertEquals(List.of("-Djava.library.path=/mc/natives", "-XstartOnFirstThread"), profile.jvmArgs());
		assertEquals(List.of(
				"--username", "Dev",
				"--version", "26.3",
				"--gameDir", "/mc/run",
				"--assetsDir", "/mc/assets",
				"--assetIndex", "26"), profile.gameArgs());
	}

	@Test
	void osRuleExcludesJvmArgumentsOnOtherPlatforms() {
		LaunchProfile profile = LaunchProfile.forClient(ModelNode.fromJSONString(MODERN_JSON), LINUX, LAYOUT);

		assertEquals(List.of("-Djava.library.path=/mc/natives"), profile.jvmArgs());
	}

	@Test
	void classpathFlagAndItsValueAreNotPassedAsJvmArguments() {
		LaunchProfile profile = LaunchProfile.forClient(ModelNode.fromJSONString(MODERN_JSON), MAC, LAYOUT);

		assertFalse(profile.jvmArgs().contains("-cp"));
		assertFalse(profile.jvmArgs().stream().anyMatch(arg -> arg.contains("client.jar")));
	}

	@Test
	void legacyMinecraftArgumentsAreSplitAndSubstituted() {
		LaunchProfile profile = LaunchProfile.forClient(ModelNode.fromJSONString(LEGACY_JSON), LINUX, LAYOUT);

		assertEquals(List.of("-Djava.library.path=/mc/natives"), profile.jvmArgs());
		assertEquals(List.of(
				"--username", "Dev",
				"--version", "1.8",
				"--assetsDir", "/mc/assets"), profile.gameArgs());
	}

	@Test
	void unknownPlaceholdersAreLeftAlone() {
		assertEquals("--x ${unknown}", LaunchProfile.substitute("--x ${unknown}", Map.of("a", "b")));
	}
}
