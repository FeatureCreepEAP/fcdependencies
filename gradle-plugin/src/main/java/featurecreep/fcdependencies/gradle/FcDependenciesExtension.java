package featurecreep.fcdependencies.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

public abstract class FcDependenciesExtension {

	/** Minecraft version, for example 26.3. Must be 26.1.2 or newer. */
	public abstract Property<String> getMinecraftVersion();

	/** Player name used by runClient in offline mode. */
	public abstract Property<String> getPlayerName();

	/** Working directory for runClient and runServer. */
	public abstract DirectoryProperty getRunDirectory();

	/** "vanilla" (default) or "featurecreep". Selects what runClient and runServer launch. */
	public abstract Property<String> getRunMode();

	/** FeatureCreep loader version, resolved from Maven. Required when runMode is "featurecreep". */
	public abstract Property<String> getFeatureCreepLoaderVersion();

	/** FeatureCreep bootstrap version, resolved from Maven. Required when runMode is "featurecreep". */
	public abstract Property<String> getFeatureCreepBootstrapVersion();

	/** FeatureCreep API version, resolved from Maven. Required when runMode is "featurecreep". */
	public abstract Property<String> getFeatureCreepApiVersion();

	/** Version of the featurecreepmc-MINECRAFT_VERSION artifact, resolved from Maven. Required when runMode is "featurecreep". */
	public abstract Property<String> getFeatureCreepMinecraftApiVersion();
}
