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

	private static int getMajor(String version) {
		int dot = version.indexOf('.');
		if (dot == -1)
			return Integer.parseInt(version);
		return Integer.parseInt(version.substring(0, dot));
	}
}