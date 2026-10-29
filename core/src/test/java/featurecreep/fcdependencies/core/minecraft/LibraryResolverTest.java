package featurecreep.fcdependencies.core.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.Test;

class LibraryResolverTest {

	private static final Platform MAC = new Platform("osx", "arm64");
	private static final Platform LINUX = new Platform("linux", "x86_64");

	private static final String VERSION_JSON = """
			{
			  "libraries": [
			    {
			      "name": "a:b:1",
			      "downloads": {"artifact": {"path": "a/b/1/b-1.jar", "url": "https://example/b-1.jar", "sha1": "aa", "size": 3}}
			    },
			    {
			      "name": "c:d:1",
			      "rules": [{"action": "allow", "os": {"name": "osx"}}],
			      "downloads": {"artifact": {"path": "c/d/1/d-1.jar", "url": "https://example/d-1.jar", "sha1": "bb", "size": 4}}
			    },
			    {
			      "name": "n:native:1",
			      "natives": {"linux": "natives-linux", "osx": "natives-osx"},
			      "downloads": {
			        "artifact": {"path": "n/native/1/native-1.jar", "url": "https://example/n-1.jar", "sha1": "cc", "size": 5},
			        "classifiers": {
			          "natives-linux": {"path": "n/native/1/native-1-natives-linux.jar", "url": "https://example/nl.jar", "sha1": "dd", "size": 6},
			          "natives-osx": {"path": "n/native/1/native-1-natives-osx.jar", "url": "https://example/no.jar", "sha1": "ee", "size": 7}
			        }
			      }
			    }
			  ]
			}
			""";

	@Test
	void linuxGetsUnrestrictedLibrariesAndLinuxNatives() {
		List<LibraryResolver.Artifact> artifacts = LibraryResolver.resolve(ModelNode.fromJSONString(VERSION_JSON), LINUX);

		assertEquals(List.of(
				"a/b/1/b-1.jar",
				"n/native/1/native-1.jar",
				"n/native/1/native-1-natives-linux.jar"), paths(artifacts));
	}

	@Test
	void macGetsOsxOnlyLibrariesAndOsxNatives() {
		List<LibraryResolver.Artifact> artifacts = LibraryResolver.resolve(ModelNode.fromJSONString(VERSION_JSON), MAC);

		assertEquals(List.of(
				"a/b/1/b-1.jar",
				"c/d/1/d-1.jar",
				"n/native/1/native-1.jar",
				"n/native/1/native-1-natives-osx.jar"), paths(artifacts));
	}

	@Test
	void artifactKeepsUrlShaAndSize() {
		LibraryResolver.Artifact artifact = LibraryResolver.resolve(ModelNode.fromJSONString(VERSION_JSON), LINUX).get(0);

		assertEquals("https://example/b-1.jar", artifact.url());
		assertEquals("aa", artifact.sha1());
		assertEquals(3L, artifact.size());
	}

	@Test
	void missingLibrariesYieldNoArtifacts() {
		assertEquals(List.of(), LibraryResolver.resolve(ModelNode.fromJSONString("{\"id\":\"26.3\"}"), LINUX));
	}

	private static List<String> paths(List<LibraryResolver.Artifact> artifacts) {
		return artifacts.stream().map(LibraryResolver.Artifact::path).toList();
	}
}
