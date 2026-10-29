package featurecreep.fcdependencies.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;

import featurecreep.fcdependencies.core.minecraft.FeatureCreepModInstaller;
import featurecreep.fcdependencies.core.minecraft.FeatureCreepRuntime;
import featurecreep.fcdependencies.core.minecraft.FeatureCreepVersions;
import featurecreep.fcdependencies.core.minecraft.LaunchPlanner;
import featurecreep.fcdependencies.core.minecraft.MinecraftPreparer;
import featurecreep.fcdependencies.core.minecraft.MinecraftSide;
import featurecreep.fcdependencies.core.minecraft.RunMode;

public abstract class AbstractFcMojo extends AbstractMojo {

	@Parameter(property = "minecraftVersion", required = true)
	protected String minecraftVersion;

	@Parameter(property = "runMode", defaultValue = "vanilla")
	protected String runMode;

	@Parameter(property = "versionDirectory")
	protected File versionDirectory;

	@Parameter(property = "runDirectory")
	protected File runDirectory;

	@Parameter(property = "playerName", defaultValue = "Dev")
	protected String playerName;

	@Parameter(property = "featureCreepLoaderVersion")
	protected String featureCreepLoaderVersion;

	@Parameter(property = "featureCreepBootstrapVersion")
	protected String featureCreepBootstrapVersion;

	@Parameter(property = "featureCreepApiVersion")
	protected String featureCreepApiVersion;

	@Parameter(property = "featureCreepMinecraftApiVersion")
	protected String featureCreepMinecraftApiVersion;

	@Parameter(property = "javaExecutable", defaultValue = "${java.home}/bin/java")
	protected String javaExecutable;

	@Component
	protected MavenProject project;

	@Component
	protected RepositorySystem repositorySystem;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	protected RepositorySystemSession repositorySession;

	@Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
	protected List<RemoteRepository> remoteRepositories;

	@Override
	public final void execute() throws MojoExecutionException {
		try {
			run();
		} catch (MojoExecutionException e) {
			throw e;
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	protected abstract void run() throws Exception;

	protected final RunMode mode() {
		return RunMode.parse(runMode);
	}

	protected final Path versionDir() {
		return versionDirectory != null ? versionDirectory.toPath()
				: Path.of(project.getBuild().getDirectory(), "fcdependencies", minecraftVersion);
	}

	protected final Path runDir() {
		return runDirectory != null ? runDirectory.toPath() : project.getBasedir().toPath().resolve("run");
	}

	protected final void prepare(MinecraftSide side) throws Exception {
		MinecraftPreparer.prepare(minecraftVersion, side, versionDir(), message -> getLog().info(message));
	}

	protected final void installModule() throws IOException {
		File artifact = project.getArtifact().getFile();
		if (artifact == null) {
			getLog().warn("project jar is not built, so the FeatureCreep module was not installed; run package first");
			return;
		}
		Optional<Path> installed = FeatureCreepModInstaller.install(artifact.toPath(), runDir());
		if (installed.isPresent()) {
			getLog().info("installed FeatureCreep module " + installed.get());
		} else {
			getLog().info(artifact.getName() + " has no module.xml, so it was not installed as a FeatureCreep module");
		}
	}

	protected final List<String> featureCreepJars(RunMode mode) throws Exception {
		if (mode == RunMode.VANILLA) {
			return List.of();
		}

		CollectRequest collect = new CollectRequest();
		collect.setRepositories(remoteRepositories);
		for (String coordinate : LaunchPlanner.featureCreepCoordinates(minecraftVersion, featureCreepVersions())) {
			collect.addDependency(new Dependency(new DefaultArtifact(coordinate), "runtime"));
		}

		getLog().info("resolving FeatureCreep artifacts");
		DependencyResult result = repositorySystem.resolveDependencies(repositorySession,
				new DependencyRequest(collect, null));
		List<String> jars = result.getArtifactResults().stream()
				.map(artifact -> artifact.getArtifact().getFile().getAbsolutePath())
				.toList();
		getLog().info("resolved " + jars.size() + " FeatureCreep jars");
		jars.forEach(jar -> getLog().debug("  " + jar));
		return jars;
	}

	protected final void stageLibraries() throws Exception {
		List<RemoteRepository> repositories = new ArrayList<>(remoteRepositories);
		repositories.add(new RemoteRepository.Builder("fabricmc", "default", FeatureCreepRuntime.FABRIC_REPOSITORY).build());

		CollectRequest collect = new CollectRequest();
		collect.setRepositories(repositories);
		for (String coordinate : FeatureCreepRuntime.libraryCoordinates(minecraftVersion, featureCreepVersions())) {
			collect.addDependency(new Dependency(new DefaultArtifact(coordinate), "runtime"));
		}

		Path libraries = LaunchPlanner.libraryDirectory(versionDir());
		getLog().info("staging FeatureCreep runtime libraries into " + libraries);
		DependencyResult result = repositorySystem.resolveDependencies(repositorySession,
				new DependencyRequest(collect, null));
		for (ArtifactResult artifact : result.getArtifactResults()) {
			var resolved = artifact.getArtifact();
			FeatureCreepRuntime.stage(libraries, new FeatureCreepRuntime.Library(resolved.getGroupId(),
					resolved.getArtifactId(), resolved.getVersion(), resolved.getFile().toPath()));
		}
	}

	private FeatureCreepVersions featureCreepVersions() {
		return new FeatureCreepVersions(featureCreepLoaderVersion, featureCreepBootstrapVersion,
				featureCreepApiVersion, featureCreepMinecraftApiVersion);
	}

	protected final void launch(LaunchPlanner.Plan plan) throws Exception {
		LaunchPlanner.describe(plan, javaExecutable).forEach(line -> getLog().info("  " + line));
		List<String> classpath = new ArrayList<>(project.getRuntimeClasspathElements());
		classpath.addAll(plan.classpath());
		classpath.forEach(entry -> getLog().debug("  classpath: " + entry));

		List<String> command = new ArrayList<>();
		command.add(javaExecutable);
		command.addAll(plan.jvmArgs());
		command.add("-cp");
		command.add(String.join(File.pathSeparator, classpath));
		command.add(plan.mainClass());
		command.addAll(plan.args());

		Process process = new ProcessBuilder(command)
				.directory(plan.workingDirectory().toFile())
				.inheritIO()
				.start();
		int exitCode = process.waitFor();
		if (exitCode != 0) {
			throw new MojoExecutionException("Minecraft exited with code " + exitCode);
		}
	}
}
