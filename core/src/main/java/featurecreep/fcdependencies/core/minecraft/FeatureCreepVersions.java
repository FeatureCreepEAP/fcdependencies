package featurecreep.fcdependencies.core.minecraft;

/** Maven versions of the FeatureCreep artifacts used when running FeatureCreep instead of vanilla. */
public record FeatureCreepVersions(String loader, String bootstrap, String api, String minecraftApi) {

	public FeatureCreepVersions {
		require("FeatureCreep loader version", loader);
		require("FeatureCreep bootstrap version", bootstrap);
		require("FeatureCreep API version", api);
		require("FeatureCreep Minecraft API version", minecraftApi);
	}

	private static void require(String name, String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(name + " is required when runMode is featurecreep");
		}
	}
}
