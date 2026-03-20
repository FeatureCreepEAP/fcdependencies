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
import featurecreep.fcdependencies.core.minecraft.MinecraftProcessor;
import featurecreep.fcdependencies.core.minecraft.MinecraftResult;
import featurecreep.fcdependencies.core.minecraft.MinecraftSide;
import featurecreep.fcdependencies.core.minecraft.MinecraftVersionManifestService;

@Mojo(name = "fcdependencies", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.NONE)
public class FcDependenciesMojo extends AbstractMojo {

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

			if (!"minecraft".equalsIgnoreCase(app)) {
				getLog().info("App '" + app + "' not handled by fcdependencies. Skipping.");
				return;
			}

			String classifier = side.toLowerCase();

			if (isAlreadyInstalled(classifier)) {
				getLog().info("Minecraft already installed. Skipping.");
				addMinecraftDependency();
				return;
			}

			getLog().info("Resolving Minecraft " + version);

			MinecraftVersionManifestService manifest = new MinecraftVersionManifestService();
			ModelNode versionJson = manifest.getVersionJson(version);

			File workingDir = createWorkingDirectory(classifier);

			MinecraftProcessor processor = new MinecraftProcessor();

			MinecraftResult result = processor.process(version, MinecraftSide.valueOf(side),
					DecompilerService.Engine.valueOf(decompiler), workingDir);

			File mappedJar = result.mappedJar;
			File sourcesDir = result.sourcesDir;

			if (!mappedJar.exists() || mappedJar.length() == 0) {
				throw new IllegalStateException("Mapped jar missing or empty.");
			}

			installJarToLocalRepo(mappedJar, classifier, versionJson);

			File sourcesJar = createSourcesJar(sourcesDir, classifier);
			installSourcesJar(sourcesJar, classifier);

			deleteDirectory(workingDir.toPath());

		} catch (Exception e) {
			throw new MojoExecutionException("Minecraft processing failed", e);
		}
	}

	private void installSourcesJar(File sourcesJar, String classifier) throws Exception {

		if (sourcesJar == null || !sourcesJar.exists()) {
			throw new IllegalStateException("Sources jar missing.");
		}

		String sourcesClassifier = (classifier == null || classifier.isEmpty()) ? "sources" : classifier + "-sources";

		org.apache.maven.artifact.Artifact sourcesArtifact = new org.apache.maven.artifact.DefaultArtifact(
				"net.minecraft", "minecraft", version, "compile", "jar", sourcesClassifier,
				new org.apache.maven.artifact.handler.DefaultArtifactHandler("jar"));

		sourcesArtifact.setFile(sourcesJar);

		artifactInstaller.install(session.getProjectBuildingRequest(),
				java.util.Collections.singletonList(sourcesArtifact));

		getLog().info("Installed Minecraft sources (" + sourcesClassifier + ")");
	}

	private File createSourcesJar(File sourcesDir, String classifier) throws Exception {

		File sourcesJar = new File(sourcesDir.getParentFile(),
				"minecraft-" + version + "-" + classifier + "-sources.jar");

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

	private File createWorkingDirectory(String classifier) {

		File repoBase = new File(session.getLocalRepository().getBasedir());

		File base = new File(repoBase, "net/minecraft/minecraft/" + version + "/.fcwork-" + classifier);

		if (!base.exists()) {
			base.mkdirs();
		}

		return base;
	}

	private boolean isAlreadyInstalled(String classifier) {

		File repoBase = new File(session.getLocalRepository().getBasedir());

		File artifactFile = new File(repoBase,
				"net/minecraft/minecraft/" + version + "/minecraft-" + version + "-" + classifier + ".jar");

		return artifactFile.exists();
	}

	private void installJarToLocalRepo(File jarFile, String classifier, ModelNode versionJson) throws Exception {

		File pomFile = createMinecraftPom(versionJson);

		org.apache.maven.artifact.Artifact jarArtifact = new org.apache.maven.artifact.DefaultArtifact("net.minecraft",
				"minecraft", version, "compile", "jar", classifier,
				new org.apache.maven.artifact.handler.DefaultArtifactHandler("jar"));

		jarArtifact.setFile(jarFile);

		org.apache.maven.artifact.Artifact pomArtifact = new org.apache.maven.artifact.DefaultArtifact("net.minecraft",
				"minecraft", version, "compile", "pom", null,
				new org.apache.maven.artifact.handler.DefaultArtifactHandler("pom"));

		pomArtifact.setFile(pomFile);

		artifactInstaller.install(session.getProjectBuildingRequest(),
				java.util.Arrays.asList(pomArtifact, jarArtifact));

		getLog().info("Installed Minecraft " + version + " (" + classifier + ")");
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

		String pomContent = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" "
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
				+ "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 "
				+ "https://maven.apache.org/xsd/maven-4.0.0.xsd\">" +

				"<modelVersion>4.0.0</modelVersion>" +

				"<groupId>net.minecraft</groupId>" + "<artifactId>minecraft</artifactId>" + "<version>" + version
				+ "</version>" +

				"<properties>" + "<maven.compiler.source>" + javaVersion + "</maven.compiler.source>"
				+ "<maven.compiler.target>" + javaVersion + "</maven.compiler.target>" + "</properties>" +

				"<repositories>" + "  <repository>" + "    <id>Minecraft</id>" + "    <name>Minecraft</name>"
				+ "    <url>https://libraries.minecraft.net</url>" + "  </repository>" + "</repositories>" +

				"<dependencies>" + depsBuilder + "</dependencies>" +

				"</project>";

		File pomFile = File.createTempFile("minecraft-", ".pom");
		Files.writeString(pomFile.toPath(), pomContent);

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

	private void addMinecraftDependency() {

		Dependency dependency = new Dependency();
		dependency.setGroupId("net.minecraft");
		dependency.setArtifactId("minecraft");
		dependency.setVersion(version);
		dependency.setClassifier(side.toLowerCase());
		dependency.setScope("compile");

		project.getModel().addDependency(dependency);
	}
}