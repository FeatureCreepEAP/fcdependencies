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
		if ("26.4".equals(version)) return "26.4-snapshot-6";
		return version;
	}

	public static boolean supportsRunTasks(String version) {
		return compareVersions(version, "26.1.2") >= 0;
	}

	public static void requireRunSupport(String version) {
		if (!supportsRunTasks(version)) {
			throw new IllegalArgumentException("runClient and runServer require Minecraft 26.1.2 or newer, got " + version);
		}
	}

	static int compareVersions(String a, String b) {
		String[] left = a.split("\\.");
		String[] right = b.split("\\.");
		for (int i = 0; i < Math.max(left.length, right.length); i++) {
			int l = i < left.length ? leadingNumber(left[i]) : 0;
			int r = i < right.length ? leadingNumber(right[i]) : 0;
			if (l != r) return Integer.compare(l, r);
		}
		return 0;
	}

	private static int leadingNumber(String part) {
		int end = 0;
		while (end < part.length() && Character.isDigit(part.charAt(end))) end++;
		return end == 0 ? 0 : Integer.parseInt(part.substring(0, end));
	}

	private static int getMajor(String version) {
		int dot = version.indexOf('.');
		if (dot == -1)
			return Integer.parseInt(version);
		return Integer.parseInt(version.substring(0, dot));
	}
}