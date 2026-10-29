package featurecreep.fcdependencies.gradle;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import featurecreep.fcdependencies.core.DecompilerService;
import featurecreep.fcdependencies.core.minecraft.MinecraftProcessor;
import featurecreep.fcdependencies.core.minecraft.MinecraftResult;
import featurecreep.fcdependencies.core.minecraft.MinecraftSide;

public abstract class GenSourcesTask extends DefaultTask {

	@Input
	public abstract Property<String> getMinecraftVersion();

	@Input
	public abstract Property<DecompilerService.Engine> getDecompiler();

	@OutputFile
	public abstract RegularFileProperty getSourcesJar();

	@Internal
	public abstract DirectoryProperty getWorkingDirectory();

	@Internal
	public abstract DirectoryProperty getLocalRepository();

	@TaskAction
	public void generate() throws Exception {
		Path workingDir = getWorkingDirectory().get().getAsFile().toPath();
		Path sourcesJar = getSourcesJar().get().getAsFile().toPath();
		String version = getMinecraftVersion().get();

		deleteRecursively(workingDir);
		MinecraftResult result = new MinecraftProcessor().process(version, MinecraftSide.CLIENT,
				getDecompiler().get(), workingDir.toFile());

		packSources(result.sourcesDir.toPath(), sourcesJar);
		Path installed = installSources(getLocalRepository().get().getAsFile().toPath(), version, sourcesJar);
		getLogger().lifecycle("fcdependencies: installed sources to " + installed);
		deleteRecursively(workingDir);
	}

	static void packSources(Path sourcesDir, Path jar) throws Exception {
		Files.createDirectories(jar.getParent());
		try (Stream<Path> files = Files.walk(sourcesDir);
				OutputStream out = Files.newOutputStream(jar);
				JarOutputStream jos = new JarOutputStream(out)) {
			Iterator<Path> iterator = files.filter(Files::isRegularFile).iterator();
			while (iterator.hasNext()) {
				Path file = iterator.next();
				jos.putNextEntry(new JarEntry(sourcesDir.relativize(file).toString().replace('\\', '/')));
				Files.copy(file, jos);
				jos.closeEntry();
			}
		}
	}

	static Path installSources(Path localRepository, String version, Path sourcesJar) throws Exception {
		Path target = localRepository.resolve("net/minecraft/minecraft/" + version + "/minecraft-" + version
				+ "-client-sources.jar");
		Files.createDirectories(target.getParent());
		Files.copy(sourcesJar, target, StandardCopyOption.REPLACE_EXISTING);
		return target;
	}

	static void deleteRecursively(Path dir) throws Exception {
		if (!Files.exists(dir)) return;
		try (Stream<Path> paths = Files.walk(dir)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}
}
