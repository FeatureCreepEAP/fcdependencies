package featurecreep.fcdependencies.core.minecraft;

public final class MinecraftVersionSupport {

	public static boolean requiresRemap(String version) {

		int major = getMajor(version);

		if (major >= 26)
			return false;

		if (version.compareTo("1.21.11") >= 0)
			return false;

		return version.compareTo("1.14") >= 0;
	}

	public static boolean requiresCustomJar(String version) {
		return version.equals("1.21.11");
	}

	public static String resolveManifestVersion(String version) {
		if ("26.1.2".equals(version)) return "26.1";
		if ("26.4".equals(version)) return "26.4-snapshot1";
		return version;
	}

	private static int getMajor(String version) {
		int dot = version.indexOf('.');
		if (dot == -1)
			return Integer.parseInt(version);
		return Integer.parseInt(version.substring(0, dot));
	}
}