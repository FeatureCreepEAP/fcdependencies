package featurecreep.fcdependencies.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.transfer.artifact.install.ArtifactInstaller;
import org.jboss.dmr.ModelNode;

import featurecreep.fcdependencies.core.DecompilerService;
import featurecreep.fcdependencies.core.dangerzone.DangerZoneProcessor;
import featurecreep.fcdependencies.core.dangerzone.DangerZoneResult;
import featurecreep.fcdependencies.core.minecraft.MinecraftProcessor;
import featurecreep.fcdependencies.core.minecraft.MinecraftResult;
import featurecreep.fcdependencies.core.minecraft.MinecraftSide;
import featurecreep.fcdependencies.core.minecraft.MinecraftVersionManifestService;

@Mojo(name = "fcdependencies", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.NONE)
public class FcDependenciesMojo extends AbstractMojo {

	private static final String MINECRAFT_GROUP_ID = "net.minecraft";
	private static final String MINECRAFT_ARTIFACT_ID = "minecraft";
	private static final String DANGERZONE_GROUP_ID = "net.dangerzonegame";
	private static final String DANGERZONE_ARTIFACT_ID = "dangerzone";
	private static final String DANGERZONE_LWJGL_VERSION = "3.3.0";

	@Parameter(property = "version", required = true)
	private String version;

	@Parameter(property = "side", defaultValue = "CLIENT")
	private String side;

	@Parameter(property = "decompiler", defaultValue = "VINEFLOWER")
	private String decompiler;

	@Component
	private MavenProject project;

	@Component
	private ArtifactInstaller artifactInstaller;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession session;

	@Parameter(property = "app", required = true)
	private String app;

	@Override
	public void execute() throws MojoExecutionException {

		try {
			if ("minecraft".equalsIgnoreCase(app)) {
				executeMinecraft();
				return;
			}

			if ("dangerzone".equalsIgnoreCase(app)) {
				executeDangerZone();
				return;
			}

			getLog().info("App '" + app + "' not handled by fcdependencies. Skipping.");
		} catch (Exception e) {
			throw new MojoExecutionException(app + " processing failed", e);
		}
	}

	private void executeMinecraft() throws Exception {
		String classifier = side.toLowerCase();

		if (isAlreadyInstalled(MINECRAFT_GROUP_ID, MINECRAFT_ARTIFACT_ID, version, classifier)) {
			getLog().info("Minecraft already installed. Skipping.");
			addDependency(MINECRAFT_GROUP_ID, MINECRAFT_ARTIFACT_ID, version, classifier);
			return;
		}

		getLog().info("Resolving Minecraft " + version);

		MinecraftVersionManifestService manifest = new MinecraftVersionManifestService();
		ModelNode versionJson = manifest.getVersionJson(version);

		File workingDir = createWorkingDirectory(MINECRAFT_GROUP_ID, MINECRAFT_ARTIFACT_ID, classifier);

		MinecraftProcessor processor = new MinecraftProcessor();
		MinecraftResult result = processor.process(version, MinecraftSide.fromString(side),
				DecompilerService.Engine.valueOf(decompiler), workingDir);

		File mappedJar = result.mappedJar;
		File sourcesDir = result.sourcesDir;

		if (!mappedJar.exists() || mappedJar.length() == 0) {
			throw new IllegalStateException("Mapped jar missing or empty.");
		}

		installJarToLocalRepo(MINECRAFT_GROUP_ID, MINECRAFT_ARTIFACT_ID, version, mappedJar, classifier,
				createMinecraftPom(versionJson));

		File sourcesJar = createSourcesJar(sourcesDir, MINECRAFT_ARTIFACT_ID, version, classifier);
		installSourcesJar(MINECRAFT_GROUP_ID, MINECRAFT_ARTIFACT_ID, version, sourcesJar, classifier);
		addDependency(MINECRAFT_GROUP_ID, MINECRAFT_ARTIFACT_ID, version, classifier);

		deleteDirectory(workingDir.toPath());
	}

	private void executeDangerZone() throws Exception {
		if (isAlreadyInstalled(DANGERZONE_GROUP_ID, DANGERZONE_ARTIFACT_ID, version, null)) {
			getLog().info("DangerZone already installed. Skipping.");
			addDependency(DANGERZONE_GROUP_ID, DANGERZONE_ARTIFACT_ID, version, null);
			return;
		}

		getLog().info("Resolving DangerZone " + version);

		File workingDir = createWorkingDirectory(DANGERZONE_GROUP_ID, DANGERZONE_ARTIFACT_ID, null);
		DangerZoneProcessor processor = new DangerZoneProcessor();
		DangerZoneResult result = processor.process(version, workingDir);

		if (!result.jarFile.exists() || result.jarFile.length() == 0) {
			throw new IllegalStateException("DangerZone jar missing or empty.");
		}

		installJarToLocalRepo(DANGERZONE_GROUP_ID, DANGERZONE_ARTIFACT_ID, version, result.jarFile, null,
				createDangerZonePom());

		File sourcesJar = createSourcesJar(result.sourcesDir, DANGERZONE_ARTIFACT_ID, version, null);
		installSourcesJar(DANGERZONE_GROUP_ID, DANGERZONE_ARTIFACT_ID, version, sourcesJar, null);
		addDependency(DANGERZONE_GROUP_ID, DANGERZONE_ARTIFACT_ID, version, null);

		deleteDirectory(workingDir.toPath());
	}

	private void installSourcesJar(String groupId, String artifactId, String version, File sourcesJar, String classifier)
			throws Exception {

		if (sourcesJar == null || !sourcesJar.exists()) {
			throw new IllegalStateException("Sources jar missing.");
		}

		String sourcesClassifier = (classifier == null || classifier.isEmpty()) ? "sources" : classifier + "-sources";

		org.apache.maven.artifact.Artifact sourcesArtifact = new org.apache.maven.artifact.DefaultArtifact(groupId,
				artifactId, version, "compile", "jar", sourcesClassifier,
				new org.apache.maven.artifact.handler.DefaultArtifactHandler("jar"));

		sourcesArtifact.setFile(sourcesJar);

		artifactInstaller.install(session.getProjectBuildingRequest(),
				java.util.Collections.singletonList(sourcesArtifact));

		getLog().info("Installed " + artifactId + " sources (" + sourcesClassifier + ")");
	}

	private File createSourcesJar(File sourcesDir, String artifactId, String version, String classifier) throws Exception {

		String classifierSuffix = (classifier == null || classifier.isBlank()) ? "" : "-" + classifier;
		File sourcesJar = new File(sourcesDir.getParentFile(),
				artifactId + "-" + version + classifierSuffix + "-sources.jar");

		try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(
				new java.io.FileOutputStream(sourcesJar))) {

			Files.walk(sourcesDir.toPath()).filter(Files::isRegularFile).forEach(path -> {
				try {
					String entryName = sourcesDir.toPath().relativize(path).toString().replace("\\", "/");

					jos.putNextEntry(new java.util.jar.JarEntry(entryName));
					Files.copy(path, jos);
					jos.closeEntry();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}

		return sourcesJar;
	}

	private File createWorkingDirectory(String groupId, String artifactId, String classifier) {

		File repoBase = new File(session.getLocalRepository().getBasedir());
		String classifierSuffix = (classifier == null || classifier.isBlank()) ? "" : "-" + classifier;
		File base = new File(repoBase,
				groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/.fcwork" + classifierSuffix);

		if (!base.exists()) {
			base.mkdirs();
		}

		return base;
	}

	private boolean isAlreadyInstalled(String groupId, String artifactId, String version, String classifier) {

		File repoBase = new File(session.getLocalRepository().getBasedir());
		String classifierSuffix = (classifier == null || classifier.isBlank()) ? "" : "-" + classifier;
		File artifactFile = new File(repoBase,
				groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version
						+ classifierSuffix + ".jar");

		return artifactFile.exists();
	}

	private void installJarToLocalRepo(String groupId, String artifactId, String version, File jarFile, String classifier,
			File pomFile) throws Exception {

		org.apache.maven.artifact.Artifact jarArtifact = new org.apache.maven.artifact.DefaultArtifact(groupId,
				artifactId, version, "compile", "jar", classifier,
				new org.apache.maven.artifact.handler.DefaultArtifactHandler("jar"));

		jarArtifact.setFile(jarFile);

		org.apache.maven.artifact.Artifact pomArtifact = new org.apache.maven.artifact.DefaultArtifact(groupId,
				artifactId, version, "compile", "pom", null,
				new org.apache.maven.artifact.handler.DefaultArtifactHandler("pom"));

		pomArtifact.setFile(pomFile);

		artifactInstaller.install(session.getProjectBuildingRequest(), java.util.Arrays.asList(pomArtifact, jarArtifact));

		String display = (classifier == null || classifier.isBlank()) ? version : version + " (" + classifier + ")";
		getLog().info("Installed " + artifactId + " " + display);
	}

	private File createMinecraftPom(ModelNode versionJson) throws Exception {

		int javaVersion = versionJson.get("javaVersion").get("majorVersion").asInt();

		StringBuilder depsBuilder = new StringBuilder();

		for (ModelNode lib : versionJson.get("libraries").asList()) {
			if (!lib.has("name"))
				continue;

			String[] split = lib.get("name").asString().split(":");
			if (split.length != 3)
				continue;

			depsBuilder.append("<dependency>\n");
			depsBuilder.append("<groupId>").append(split[0]).append("</groupId>\n");
			depsBuilder.append("<artifactId>").append(split[1]).append("</artifactId>\n");
			depsBuilder.append("<version>").append(split[2]).append("</version>\n");
			depsBuilder.append("</dependency>\n");
		}

		return createPomFile(MINECRAFT_GROUP_ID, MINECRAFT_ARTIFACT_ID, version, versionJson.get("javaVersion").get("majorVersion").asInt(),
				depsBuilder.toString(), "Minecraft", "https://libraries.minecraft.net");
	}

	private File createDangerZonePom() throws Exception {
		StringBuilder depsBuilder = new StringBuilder();
		depsBuilder.append("<dependency>\n");
		depsBuilder.append("<groupId>org.lwjgl</groupId>\n");
		depsBuilder.append("<artifactId>lwjgl</artifactId>\n");
		depsBuilder.append("<version>").append(DANGERZONE_LWJGL_VERSION).append("</version>\n");
		depsBuilder.append("</dependency>\n");

		return createPomFile(DANGERZONE_GROUP_ID, DANGERZONE_ARTIFACT_ID, version, 8, depsBuilder.toString(), null,
				null);
	}

	private File createPomFile(String groupId, String artifactId, String version, int javaVersion, String dependenciesXml,
			String repositoryId, String repositoryUrl) throws Exception {
		StringBuilder pomContent = new StringBuilder();
		pomContent.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" ");
		pomContent.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
		pomContent.append("xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 ");
		pomContent.append("https://maven.apache.org/xsd/maven-4.0.0.xsd\">");
		pomContent.append("<modelVersion>4.0.0</modelVersion>");
		pomContent.append("<groupId>").append(groupId).append("</groupId>");
		pomContent.append("<artifactId>").append(artifactId).append("</artifactId>");
		pomContent.append("<version>").append(version).append("</version>");
		pomContent.append("<properties><maven.compiler.source>").append(javaVersion)
				.append("</maven.compiler.source><maven.compiler.target>").append(javaVersion)
				.append("</maven.compiler.target></properties>");

		if (repositoryId != null && repositoryUrl != null) {
			pomContent.append("<repositories><repository><id>").append(repositoryId).append("</id><name>")
					.append(repositoryId).append("</name><url>").append(repositoryUrl)
					.append("</url></repository></repositories>");
		}

		pomContent.append("<dependencies>").append(dependenciesXml).append("</dependencies>");
		pomContent.append("</project>");

		File pomFile = File.createTempFile(artifactId + "-", ".pom");
		Files.writeString(pomFile.toPath(), pomContent.toString());
		return pomFile;
	}

	private void deleteDirectory(Path path) throws IOException {
		if (!Files.exists(path))
			return;

		Files.walk(path).sorted((a, b) -> b.compareTo(a)).forEach(p -> {
			try {
				Files.delete(p);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void addDependency(String groupId, String artifactId, String version, String classifier) {

		Dependency dependency = new Dependency();
		dependency.setGroupId(groupId);
		dependency.setArtifactId(artifactId);
		dependency.setVersion(version);
		if (classifier != null && !classifier.isBlank()) {
			dependency.setClassifier(classifier);
		}
		dependency.setScope("compile");

		project.getModel().addDependency(dependency);
	}
}
