package featurecreep.fcdependencies.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;

import featurecreep.fcdependencies.core.minecraft.FeatureCreepModInstaller;
import featurecreep.fcdependencies.core.minecraft.FeatureCreepRuntime;
import featurecreep.fcdependencies.core.minecraft.FeatureCreepVersions;
import featurecreep.fcdependencies.core.minecraft.LaunchPlanner;
import featurecreep.fcdependencies.core.minecraft.MinecraftSide;
import featurecreep.fcdependencies.core.minecraft.MinecraftVersionSupport;
import featurecreep.fcdependencies.core.minecraft.RunMode;

public class FcDependenciesGradlePlugin implements Plugin<Project> {

	static final String EXTENSION = "fcdependencies";
	static final String GROUP = "fcdependencies";
	static final String FEATURECREEP_CONFIGURATION = "fcdependenciesFeatureCreep";
	static final String FEATURECREEP_LIBRARIES_CONFIGURATION = "fcdependenciesFeatureCreepLibraries";
	static final int JAVA_VERSION = 25;
	static final List<String> RUN_TASKS = List.of("runClient", "runServer");

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(JavaPlugin.class);

		FcDependenciesExtension ext = project.getExtensions().create(EXTENSION, FcDependenciesExtension.class);
		ext.getPlayerName().convention("Dev");
		ext.getRunDirectory().convention(project.getRootProject().getLayout().getProjectDirectory().dir("run"));
		ext.getRunMode().convention(RunMode.VANILLA.name().toLowerCase());

		Provider<Directory> versionDir = project.getLayout().getBuildDirectory()
				.dir(ext.getMinecraftVersion().map(version -> "fcdependencies/" + version));

		TaskProvider<PrepareMinecraftTask> prepareClient = registerPrepare(project, ext, versionDir,
				"fcdependenciesPrepareClient", MinecraftSide.CLIENT);
		TaskProvider<PrepareMinecraftTask> prepareServer = registerPrepare(project, ext, versionDir,
				"fcdependenciesPrepareServer", MinecraftSide.SERVER);

		project.getDependencies().add("compileOnly",
				project.files(versionDir.map(dir -> dir.file("client.jar"))).builtBy(prepareClient));

		Configuration featureCreep = project.getConfigurations().create(FEATURECREEP_CONFIGURATION, config -> {
			config.setVisible(false);
			config.setCanBeConsumed(false);
			config.withDependencies(deps -> LaunchPlanner
					.featureCreepCoordinates(ext.getMinecraftVersion().get(), featureCreepVersions(ext))
					.forEach(coordinate -> deps.add(project.getDependencies().create(coordinate))));
		});

		project.getRepositories().maven(repo -> {
			repo.setUrl(FeatureCreepRuntime.FABRIC_REPOSITORY);
			repo.content(content -> content.includeGroup("net.fabricmc"));
		});
		Configuration featureCreepLibraries = project.getConfigurations().create(FEATURECREEP_LIBRARIES_CONFIGURATION, config -> {
			config.setVisible(false);
			config.setCanBeConsumed(false);
			config.withDependencies(deps -> FeatureCreepRuntime
					.libraryCoordinates(ext.getMinecraftVersion().get(), featureCreepVersions(ext))
					.forEach(coordinate -> deps.add(project.getDependencies().create(coordinate))));
		});

		project.afterEvaluate(p -> {
			if (!ext.getMinecraftVersion().isPresent()) {
				throw new GradleException("Set fcdependencies { minecraftVersion = \"26.3\" } to use FCDependencies.");
			}
		});

		project.getGradle().getTaskGraph().whenReady(graph -> {
			boolean runRequested = graph.getAllTasks().stream()
					.anyMatch(task -> task.getProject().equals(project) && RUN_TASKS.contains(task.getName()));
			if (runRequested) {
				MinecraftVersionSupport.requireRunSupport(ext.getMinecraftVersion().get());
				if (runMode(ext) == RunMode.FEATURECREEP) {
					featureCreepVersions(ext);
				}
			}
		});

		Provider<JavaLauncher> launcher = project.getExtensions().getByType(JavaToolchainService.class)
				.launcherFor(spec -> spec.getLanguageVersion().set(JavaLanguageVersion.of(JAVA_VERSION)));

		Callable<Object> jarForFeatureCreep = () -> runMode(ext) == RunMode.FEATURECREEP
				? project.getTasks().named("jar")
				: List.of();

		project.getTasks().register("runClient", JavaExec.class, task -> {
			task.setGroup(GROUP);
			task.setDescription("Launches the Minecraft client (26.1.2+). Vanilla unless runMode is 'featurecreep'.");
			task.dependsOn(prepareClient, jarForFeatureCreep);
			task.getJavaLauncher().set(launcher);
			task.doFirst(t -> configureClient(task, ext, versionDir, featureCreep, featureCreepLibraries));
		});

		project.getTasks().register("runServer", JavaExec.class, task -> {
			task.setGroup(GROUP);
			task.setDescription("Launches the Minecraft dedicated server (26.1.2+). Vanilla unless runMode is 'featurecreep'.");
			task.dependsOn(prepareServer, jarForFeatureCreep);
			task.getJavaLauncher().set(launcher);
			task.doFirst(t -> configureServer(task, ext, versionDir, featureCreep, featureCreepLibraries));
		});
	}

	private static TaskProvider<PrepareMinecraftTask> registerPrepare(Project project, FcDependenciesExtension ext,
			Provider<Directory> versionDir, String name, MinecraftSide side) {
		return project.getTasks().register(name, PrepareMinecraftTask.class, task -> {
			task.setGroup(GROUP);
			task.setDescription("Downloads the Minecraft " + side.name().toLowerCase() + " files for the configured version.");
			task.getMinecraftVersion().set(ext.getMinecraftVersion());
			task.getSide().set(side);
			task.getOutputDirectory().set(versionDir);
		});
	}

	static RunMode runMode(FcDependenciesExtension ext) {
		try {
			return RunMode.parse(ext.getRunMode().get());
		} catch (IllegalArgumentException e) {
			throw new GradleException("fcdependencies: " + e.getMessage(), e);
		}
	}

	static FeatureCreepVersions featureCreepVersions(FcDependenciesExtension ext) {
		try {
			return new FeatureCreepVersions(
					ext.getFeatureCreepLoaderVersion().getOrNull(),
					ext.getFeatureCreepBootstrapVersion().getOrNull(),
					ext.getFeatureCreepApiVersion().getOrNull(),
					ext.getFeatureCreepMinecraftApiVersion().getOrNull());
		} catch (IllegalArgumentException e) {
			throw new GradleException("fcdependencies: " + e.getMessage(), e);
		}
	}

	private static void configureClient(JavaExec exec, FcDependenciesExtension ext, Provider<Directory> versionDir,
			Configuration featureCreep, Configuration featureCreepLibraries) {
		RunMode mode = runMode(ext);
		try {
			LaunchPlanner.Plan plan = LaunchPlanner.client(versionDir.get().getAsFile().toPath(), runDirectory(ext),
					ext.getPlayerName().get(), mode, featureCreepJars(exec, featureCreep, mode));
			stageLibraries(exec, versionDir, mode, featureCreepLibraries);
			installModule(exec, ext, mode);
			logLaunch(exec, ext, "client", mode, plan);
			apply(exec, plan);
		} catch (IOException e) {
			throw new GradleException("Could not prepare the Minecraft client launch", e);
		}
	}

	private static void configureServer(JavaExec exec, FcDependenciesExtension ext, Provider<Directory> versionDir,
			Configuration featureCreep, Configuration featureCreepLibraries) {
		RunMode mode = runMode(ext);
		try {
			LaunchPlanner.Plan plan = LaunchPlanner.server(versionDir.get().getAsFile().toPath(), runDirectory(ext), mode,
					featureCreepJars(exec, featureCreep, mode));
			stageLibraries(exec, versionDir, mode, featureCreepLibraries);
			installModule(exec, ext, mode);
			logLaunch(exec, ext, "server", mode, plan);
			apply(exec, plan);
		} catch (IOException e) {
			throw new GradleException("Could not prepare the Minecraft server launch", e);
		}
	}

	private static List<String> featureCreepJars(JavaExec exec, Configuration featureCreep, RunMode mode) {
		if (mode == RunMode.VANILLA) return List.of();
		Logger log = exec.getLogger();
		log.lifecycle("fcdependencies: resolving FeatureCreep artifacts");
		List<String> jars = featureCreep.getFiles().stream().map(File::getAbsolutePath).toList();
		log.lifecycle("fcdependencies: resolved " + jars.size() + " FeatureCreep jars");
		jars.forEach(jar -> log.info("  " + jar));
		return jars;
	}

	private static void stageLibraries(JavaExec exec, Provider<Directory> versionDir, RunMode mode, Configuration runtime)
			throws IOException {
		if (mode != RunMode.FEATURECREEP) return;
		Path libraries = LaunchPlanner.libraryDirectory(versionDir.get().getAsFile().toPath());
		exec.getLogger().lifecycle("fcdependencies: staging FeatureCreep runtime libraries into " + libraries);
		for (ResolvedArtifactResult artifact : runtime.getIncoming().getArtifacts().getArtifacts()) {
			if (artifact.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier id) {
				FeatureCreepRuntime.stage(libraries, new FeatureCreepRuntime.Library(id.getGroup(), id.getModule(),
						id.getVersion(), artifact.getFile().toPath()));
			}
		}
	}

	private static void installModule(JavaExec exec, FcDependenciesExtension ext, RunMode mode) throws IOException {
		if (mode != RunMode.FEATURECREEP) return;
		Logger log = exec.getLogger();
		File jar = exec.getProject().getTasks().named("jar", Jar.class).get().getArchiveFile().get().getAsFile();
		Optional<Path> installed = FeatureCreepModInstaller.install(jar.toPath(), runDirectory(ext));
		if (installed.isPresent()) {
			log.lifecycle("fcdependencies: installed FeatureCreep module " + installed.get());
		} else {
			log.lifecycle("fcdependencies: " + jar.getName() + " has no module.xml, so it was not installed as a FeatureCreep module");
		}
	}

	private static void logLaunch(JavaExec exec, FcDependenciesExtension ext, String side, RunMode mode,
			LaunchPlanner.Plan plan) {
		Logger log = exec.getLogger();
		log.lifecycle("fcdependencies: starting Minecraft " + ext.getMinecraftVersion().get() + " " + side
				+ " (" + mode.name().toLowerCase() + ")");
		String java = exec.getJavaLauncher().get().getExecutablePath().getAsFile().getAbsolutePath();
		LaunchPlanner.describe(plan, java).forEach(line -> log.lifecycle("  " + line));
		plan.classpath().forEach(entry -> log.info("  classpath: " + entry));
	}

	private static void apply(JavaExec exec, LaunchPlanner.Plan plan) {
		exec.setWorkingDir(plan.workingDirectory().toFile());
		exec.getMainClass().set(plan.mainClass());
		exec.setClasspath(mainRuntimeClasspath(exec).plus(exec.getProject().files(plan.classpath())));
		exec.setJvmArgs(plan.jvmArgs());
		exec.setArgs(plan.args());
	}

	private static FileCollection mainRuntimeClasspath(JavaExec exec) {
		return exec.getProject().getExtensions().getByType(SourceSetContainer.class)
				.getByName("main").getRuntimeClasspath();
	}

	private static Path runDirectory(FcDependenciesExtension ext) {
		return ext.getRunDirectory().get().getAsFile().toPath();
	}
}
