package featurecreep.fcdependencies.core.minecraft;

import java.util.Locale;

import org.jboss.dmr.ModelNode;

/** Evaluates Mojang launcher rules against an operating system name and architecture. */
public final class Platform {

	private final String osName;
	private final String arch;

	public Platform(String osName, String arch) {
		this.osName = osName;
		this.arch = arch;
	}

	public static Platform current() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String rawArch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
		String osName = os.contains("mac") ? "osx" : os.contains("win") ? "windows" : "linux";
		String arch = rawArch.equals("aarch64") || rawArch.equals("arm64") ? "arm64" : "x86_64";
		return new Platform(osName, arch);
	}

	public String osName() {
		return osName;
	}

	/** Mojang rules: the last matching rule decides; no rules means allowed; rules that never match mean disallowed. */
	public boolean allowed(ModelNode rules) {
		if (rules == null || !rules.isDefined()) return true;

		boolean allowed = false;
		for (ModelNode rule : rules.asList()) {
			if (matches(rule)) {
				allowed = "allow".equals(rule.get("action").asString());
			}
		}
		return allowed;
	}

	private boolean matches(ModelNode rule) {
		// Feature flags (demo user, custom resolution, ...) are never enabled by this tool.
		if (rule.hasDefined("features")) return false;
		if (!rule.hasDefined("os")) return true;

		ModelNode os = rule.get("os");
		if (os.hasDefined("name") && !os.get("name").asString().equals(osName)) return false;
		return !os.hasDefined("arch") || os.get("arch").asString().equals(arch);
	}
}
