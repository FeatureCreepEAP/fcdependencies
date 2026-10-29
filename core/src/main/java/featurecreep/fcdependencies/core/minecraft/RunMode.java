package featurecreep.fcdependencies.core.minecraft;

public enum RunMode {
	VANILLA,
	FEATURECREEP;

	public static RunMode parse(String value) {
		for (RunMode mode : values()) {
			if (mode.name().equalsIgnoreCase(value)) return mode;
		}
		throw new IllegalArgumentException("runMode must be 'vanilla' or 'featurecreep', was '" + value + "'");
	}
}
