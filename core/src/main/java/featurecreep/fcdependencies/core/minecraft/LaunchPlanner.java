package featurecreep.fcdependencies.core.minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import org.jboss.dmr.ModelNode;

/** Builds the main class, classpath and arguments for runClient and runServer. Used by every build tool front end. */
public final class LaunchPlanner {

	public static final String VERSION_JSON = "version.json";
	public static final List<String> SERVER_ARGS = List.of("nogui");

	private static final String FEATURECREEP_GROUP = "com.asbestosstar";
	private static final String BOOTSTRAP_PREFIX = "featurecreep-bootstrap-";
	private static final String FEATURECREEP_CLIENT_MAIN = "asbestosstar.bootstrap.minecraft.VanillaMinecraftLauncher";
	private static final String FEATURECREEP_SERVER_MAIN = "asbestosstar.bootstrap.minecraft.VanillaMinecraftServerLauncher";
	private static final List<String> FEATURECREEP_FLAGS = List.of(
			"-Dfeaturecreep.minecraft.host=vanilla",
			"-Dfeaturecreep.launch.managed=true");

	private LaunchPlanner() {
	}

	public record Plan(String mainClass, List<String> classpath, List<String> jvmArgs, List<String> args, Path workingDirectory) {

		public Plan {
			classpath = List.copyOf(classpath);
			jvmArgs = List.copyOf(jvmArgs);
			args = List.copyOf(args);
		}
	}

	public static List<String> featureCreepCoordinates(String minecraftVersion, FeatureCreepVersions versions) {
		return List.of(
				FEATURECREEP_GROUP + ":featurecreep-bootstrap:" + versions.bootstrap(),
				FEATURECREEP_GROUP + ":featurecreep-loader:" + versions.loader(),
				FEATURECREEP_GROUP + ":featurecreep-api:" + versions.api(),
				FEATURECREEP_GROUP + ":featurecreepmc-" + minecraftVersion + ":" + versions.minecraftApi());
	}

	public static Plan client(Path versionDir, Path runDir, String playerName, RunMode mode, List<String> featureCreepJars)
			throws IOException {
		ModelNode json = ModelNode.fromJSONString(Files.readString(versionDir.resolve(VERSION_JSON)));
		prepareRunDirectory(runDir);
		LaunchProfile profile = LaunchProfile.forClient(json, Platform.current(), new LaunchProfile.Layout(
				versionDir.resolve("client.jar"), versionDir.resolve("libraries"), versionDir.resolve("assets"),
				versionDir.resolve("natives"), runDir, playerName));

		if (mode == RunMode.VANILLA) {
			return new Plan(profile.mainClass(), profile.classpath(), profile.jvmArgs(), profile.gameArgs(), runDir);
		}
		return featureCreep(versionDir, FEATURECREEP_CLIENT_MAIN, profile.classpath(), profile.jvmArgs(), profile.gameArgs(),
				runDir, featureCreepJars);
	}

	public static Path libraryDirectory(Path versionDir) {
		return versionDir.resolve("libraries");
	}

	public static Plan server(Path versionDir, Path runDir, RunMode mode, List<String> featureCreepJars) throws IOException {
		prepareRunDirectory(runDir);
		Path serverJar = versionDir.resolve("server.jar");
		List<String> classpath = List.of(serverJar.toString());

		if (mode == RunMode.VANILLA) {
			return new Plan(mainClassOf(serverJar), classpath, List.of(), SERVER_ARGS, runDir);
		}
		return featureCreep(versionDir, FEATURECREEP_SERVER_MAIN, classpath, List.of(), SERVER_ARGS, runDir, featureCreepJars);
	}

	public static List<String> describe(Plan plan, String javaExecutable) {
		return List.of(
				"java: " + javaExecutable,
				"main class: " + plan.mainClass(),
				"working directory: " + plan.workingDirectory().toAbsolutePath(),
				"jvm args: " + (plan.jvmArgs().isEmpty() ? "(none)" : String.join(" ", plan.jvmArgs())),
				"program args: " + (plan.args().isEmpty() ? "(none)" : String.join(" ", plan.args())));
	}

	private static Plan featureCreep(Path versionDir, String mainClass, List<String> classpath, List<String> jvmArgs,
			List<String> args, Path runDir, List<String> featureCreepJars) throws IOException {
		Path bootstrap = bootstrapJar(featureCreepJars);
		List<String> jvm = new ArrayList<>(jvmArgs);
		jvm.addAll(FEATURECREEP_FLAGS);
		jvm.add("-DlibraryDirectory=" + libraryDirectory(versionDir));
		jvm.add("-javaagent:" + bootstrap);

		List<String> fullClasspath = new ArrayList<>(classpath);
		fullClasspath.addAll(featureCreepJars);
		return new Plan(mainClass, fullClasspath, jvm, args, runDir);
	}

	private static Path bootstrapJar(List<String> jars) {
		return jars.stream()
				.map(Path::of)
				.filter(jar -> jar.getFileName().toString().startsWith(BOOTSTRAP_PREFIX))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("featurecreep-bootstrap jar was not resolved"));
	}

	private static void prepareRunDirectory(Path runDir) throws IOException {
		Files.createDirectories(runDir);
	}

	private static String mainClassOf(Path jar) throws IOException {
		try (JarFile file = new JarFile(jar.toFile())) {
			String main = file.getManifest().getMainAttributes().getValue("Main-Class");
			if (main == null) throw new IOException("No Main-Class in " + jar);
			return main;
		}
	}
}
