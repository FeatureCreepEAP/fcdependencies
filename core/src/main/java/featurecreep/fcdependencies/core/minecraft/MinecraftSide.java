package featurecreep.fcdependencies.core.minecraft;

public enum MinecraftSide {

	CLIENT("client", "client_mappings"), SERVER("server", "server_mappings");

	private final String jarKey;
	private final String mappingsKey;

	MinecraftSide(String jarKey, String mappingsKey) {
		this.jarKey = jarKey;
		this.mappingsKey = mappingsKey;
	}

	/**
	 * Key used inside version JSON downloads section Example: downloads.client.url
	 */
	public String getJarKey() {
		return jarKey;
	}

	/**
	 * Key used inside version JSON downloads section Example:
	 * downloads.client_mappings.url
	 */
	public String getMappingsKey() {
		return mappingsKey;
	}

	/**
	 * Case-insensitive parsing from Maven parameter
	 */
	public static MinecraftSide fromString(String value) {
		if (value == null) {
			return CLIENT;
		}

		for (MinecraftSide side : values()) {
			if (side.name().equalsIgnoreCase(value)) {
				return side;
			}
		}

		throw new IllegalArgumentException("Invalid Minecraft side: " + value + ". Allowed values: CLIENT, SERVER");
	}

	/**
	 * True if client side
	 */
	public boolean isClient() {
		return this == CLIENT;
	}

	/**
	 * True if server side
	 */
	public boolean isServer() {
		return this == SERVER;
	}
}