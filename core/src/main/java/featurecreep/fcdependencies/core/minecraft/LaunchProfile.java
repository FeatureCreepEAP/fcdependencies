package featurecreep.fcdependencies.core.minecraft;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/** Main class, classpath and arguments for launching a vanilla client from its version JSON. */
public final class LaunchProfile {

	public record Layout(Path clientJar, Path libraryDir, Path assetsDir, Path nativesDir, Path gameDir, String playerName) {
	}

	private static final String NO_UUID = "00000000-0000-0000-0000-000000000000";

	private final String mainClass;
	private final List<String> classpath;
	private final List<String> jvmArgs;
	private final List<String> gameArgs;

	private LaunchProfile(String mainClass, List<String> classpath, List<String> jvmArgs, List<String> gameArgs) {
		this.mainClass = mainClass;
		this.classpath = classpath;
		this.jvmArgs = jvmArgs;
		this.gameArgs = gameArgs;
	}

	public static LaunchProfile forClient(ModelNode versionJson, Platform platform, Layout layout) {
		String versionId = versionJson.get("id").asString();
		String assetIndex = versionJson.hasDefined("assetIndex") && versionJson.get("assetIndex").hasDefined("id")
				? versionJson.get("assetIndex").get("id").asString()
				: versionJson.hasDefined("assets") ? versionJson.get("assets").asString() : "";

		List<String> classpath = new ArrayList<>();
		classpath.add(layout.clientJar().toString());
		for (LibraryResolver.Artifact artifact : LibraryResolver.resolve(versionJson, platform)) {
			classpath.add(layout.libraryDir().resolve(artifact.path()).toString());
		}

		Map<String, String> values = new HashMap<>();
		values.put("auth_player_name", layout.playerName());
		values.put("version_name", versionId);
		values.put("game_directory", layout.gameDir().toString());
		values.put("assets_root", layout.assetsDir().toString());
		values.put("assets_index_name", assetIndex);
		values.put("auth_uuid", NO_UUID);
		values.put("auth_access_token", "0");
		values.put("auth_xuid", "0");
		values.put("clientid", "0");
		values.put("user_type", "legacy");
		values.put("version_type", "release");
		values.put("natives_directory", layout.nativesDir().toString());
		values.put("library_directory", layout.libraryDir().toString());
		values.put("launcher_name", "fcdependencies");
		values.put("launcher_version", "0.1.0");
		values.put("classpath_separator", File.pathSeparator);
		values.put("classpath", String.join(File.pathSeparator, classpath));

		List<String> jvm = new ArrayList<>();
		List<String> game = new ArrayList<>();
		if (versionJson.hasDefined("arguments")) {
			ModelNode arguments = versionJson.get("arguments");
			if (arguments.hasDefined("jvm")) collect(arguments.get("jvm"), platform, values, jvm);
			if (arguments.hasDefined("game")) collect(arguments.get("game"), platform, values, game);
		} else {
			jvm.add("-Djava.library.path=" + layout.nativesDir());
			if (versionJson.hasDefined("minecraftArguments")) {
				for (String token : versionJson.get("minecraftArguments").asString().split(" ")) {
					game.add(substitute(token, values));
				}
			}
		}

		return new LaunchProfile(versionJson.get("mainClass").asString(), classpath, withoutClasspath(jvm), game);
	}

	public String mainClass() {
		return mainClass;
	}

	public List<String> classpath() {
		return classpath;
	}

	public List<String> jvmArgs() {
		return jvmArgs;
	}

	public List<String> gameArgs() {
		return gameArgs;
	}

	private static void collect(ModelNode entries, Platform platform, Map<String, String> values, List<String> out) {
		for (ModelNode entry : entries.asList()) {
			if (entry.getType() == ModelType.STRING) {
				out.add(substitute(entry.asString(), values));
				continue;
			}
			if (!platform.allowed(entry.hasDefined("rules") ? entry.get("rules") : null)) continue;
			ModelNode value = entry.get("value");
			if (value.getType() == ModelType.LIST) {
				for (ModelNode v : value.asList()) out.add(substitute(v.asString(), values));
			} else {
				out.add(substitute(value.asString(), values));
			}
		}
	}

	private static List<String> withoutClasspath(List<String> args) {
		List<String> result = new ArrayList<>();
		for (int i = 0; i < args.size(); i++) {
			String arg = args.get(i);
			if (arg.equals("-cp") || arg.equals("-classpath")) {
				i++;
				continue;
			}
			result.add(arg);
		}
		return result;
	}

	static String substitute(String template, Map<String, String> values) {
		String result = template;
		for (Map.Entry<String, String> entry : values.entrySet()) {
			result = result.replace("${" + entry.getKey() + "}", entry.getValue());
		}
		return result;
	}
}
