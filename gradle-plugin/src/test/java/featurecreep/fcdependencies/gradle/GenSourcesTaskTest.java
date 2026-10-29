package featurecreep.fcdependencies.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import featurecreep.fcdependencies.core.DecompilerService;

class GenSourcesTaskTest {

	@TempDir Path temp;

	@Test
	void packsSourceTreeIntoJarWithForwardSlashEntries() throws Exception {
		Path sources = temp.resolve("sources");
		Files.createDirectories(sources.resolve("net/minecraft/world"));
		Files.writeString(sources.resolve("net/minecraft/world/Level.java"), "class Level {}", StandardCharsets.UTF_8);
		Files.writeString(sources.resolve("Root.java"), "class Root {}", StandardCharsets.UTF_8);
		Path jar = temp.resolve("out/sources.jar");

		GenSourcesTask.packSources(sources, jar);

		try (JarFile file = new JarFile(jar.toFile())) {
			Set<String> names = file.stream().map(entry -> entry.getName()).collect(Collectors.toSet());
			assertEquals(Set.of("net/minecraft/world/Level.java", "Root.java"), names);
		}
	}

	@Test
	void installsSourcesJarIntoLocalMavenLayout() throws Exception {
		Path jar = temp.resolve("minecraft-26.3-client-sources.jar");
		Files.writeString(jar, "jar", StandardCharsets.UTF_8);
		Path repo = temp.resolve("m2");

		Path installed = GenSourcesTask.installSources(repo, "26.3", jar);

		assertEquals(repo.resolve("net/minecraft/minecraft/26.3/minecraft-26.3-client-sources.jar"), installed);
		assertEquals("jar", Files.readString(installed));
	}

	@Test
	void deleteRecursivelyRemovesTreeAndToleratesMissingDirectory() throws Exception {
		Path dir = temp.resolve("work");
		Files.createDirectories(dir.resolve("a/b"));
		Files.writeString(dir.resolve("a/b/file.txt"), "x", StandardCharsets.UTF_8);

		GenSourcesTask.deleteRecursively(dir);
		assertTrue(Files.notExists(dir));

		GenSourcesTask.deleteRecursively(dir);
	}

	@Test
	void parsesDecompilerNamesCaseInsensitively() {
		assertEquals(DecompilerService.Engine.VINEFLOWER, FcDependenciesGradlePlugin.parseDecompiler("vineflower"));
		assertEquals(DecompilerService.Engine.FABRIC_CFR, FcDependenciesGradlePlugin.parseDecompiler("Fabric_CFR"));
	}

	@Test
	void rejectsUnknownDecompiler() {
		GradleException error = assertThrows(GradleException.class,
				() -> FcDependenciesGradlePlugin.parseDecompiler("procyon"));
		assertTrue(error.getMessage().contains("procyon"));
	}
}
