package featurecreep.fcdependencies.core.minecraft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import com.asbestosstar.assistremapper.Mappings;
import com.asbestosstar.assistremapper.remapper.JarRemapper;

public class MinecraftRemapper {

	public File remap(String version, File inputJar, Mappings mappings, File workingDir) throws Exception {

		if (!MinecraftVersionSupport.requiresRemap(version) || mappings == null) {
			return inputJar;
		}
		System.out.println("[DEBUG] Remapping input jar: " + inputJar.getAbsolutePath());
		System.out.println("[DEBUG] Exists? " + inputJar.exists());
		// Output directory for remapped classes
		File remapDir = new File(workingDir, "remapped");
		if (remapDir.exists()) {
			deleteDirectory(remapDir.toPath());
		}
		remapDir.mkdirs();

		File outputJar = new File(workingDir, "minecraft-remapped.jar");

		if (outputJar.exists()) {
			return outputJar;
		}

		// Run remapper → outputs classes into directory
		JarRemapper remapper = new JarRemapper(mappings, remapDir.getAbsolutePath());

		remapper.remapJar(new JarFile(inputJar));

		// Zip directory into final mapped jar
		zipDirectory(remapDir.toPath(), outputJar.toPath());

		// Cleanup directory
		deleteDirectory(remapDir.toPath());

		return outputJar;
	}

	private void zipDirectory(Path sourceDir, Path outputJar) throws IOException {

		try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar.toFile()))) {

			Files.walk(sourceDir).filter(Files::isRegularFile).forEach(path -> {
				try {
					String entryName = sourceDir.relativize(path).toString().replace("\\", "/");

					jos.putNextEntry(new JarEntry(entryName));
					Files.copy(path, jos);
					jos.closeEntry();

				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	private void deleteDirectory(Path path) throws IOException {
		if (!Files.exists(path))
			return;

		Files.walk(path).sorted((a, b) -> b.compareTo(a)) // delete subs first
				.forEach(p -> {
					try {
						Files.delete(p);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
	}
}