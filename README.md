# fcdependencies

Build Tools for Building FeatureCreep. Can be run below fpmbuild and uses fpmbuild

DUE TO THE SUNSET OF PAGURE [https://discussion.fedoraproject.org/t/decommissioning-of-pagure-io-anticipated-by-flock-2026/181997](https://discussion.fedoraproject.org/t/decommissioning-of-pagure-io-anticipated-by-flock-2026/181997) COMBINED WITH COMMUNITY STANDARDS, WE ARE MOVING TO GITHUB [https://github.com/FeatureCreepEAP/fcdependencies/](https://github.com/FeatureCreepEAP/fcdependencies/) FOR THE ORIGINAL PAGURE PLEASE SEE [https://pagure.io/fcdependencies](https://pagure.io/fcdependencies)

## Modules

- `core` (`com.asbestosstar:fcdependencies-core`): downloads Minecraft and builds launch plans. Shared by both build tools.
- `gradle-plugin` (`com.asbestosstar.fcdependencies`): Gradle plugin, built with JDK 25.
- `maven-plugin` (`com.asbestosstar:fcdependencies-maven-plugin`): Maven plugin, built with JDK 25.

## Running Minecraft (26.1.2 and newer)

Both plugins provide the same tasks and goals. Vanilla is the default. Set `runMode` to `featurecreep` to launch FeatureCreep.

| Gradle | Maven | Purpose |
| --- | --- | --- |
| `fcdependenciesPrepareClient` | `fcdependencies:prepare-client` | Download client files |
| `fcdependenciesPrepareServer` | `fcdependencies:prepare-server` | Download server files |
| `runClient` | `fcdependencies:run-client` | Launch the client |
| `runServer` | `fcdependencies:run-server` | Launch the dedicated server |

Gradle only: `genSources` downloads the client, remaps it and decompiles it into `build/fcdependencies/<version>/minecraft-<version>-client-sources.jar` for IDE navigation. Choose the decompiler with `decompiler = 'vineflower'` (default) or `'fabric_cfr'`.

Gradle:

```groovy
fcdependencies {
    minecraftVersion = '26.3'
    runMode = 'featurecreep' // or 'vanilla' (default)
    featureCreepLoaderVersion = '12'
    featureCreepBootstrapVersion = '12'
    featureCreepApiVersion = '12'
    featureCreepMinecraftApiVersion = '12'
    playerName = 'Dev'            // optional
    runDirectory = file('run')    // optional
}
```

Maven (`<configuration>` of `com.asbestosstar:fcdependencies-maven-plugin`):

```xml
<configuration>
    <minecraftVersion>26.3</minecraftVersion>
    <runMode>featurecreep</runMode>
    <featureCreepLoaderVersion>12</featureCreepLoaderVersion>
    <featureCreepBootstrapVersion>12</featureCreepBootstrapVersion>
    <featureCreepApiVersion>12</featureCreepApiVersion>
    <featureCreepMinecraftApiVersion>12</featureCreepMinecraftApiVersion>
</configuration>
```

The FeatureCreep versions are required when `runMode` is `featurecreep`. The artifacts resolve from the project's repositories. `featurecreepMinecraftApiVersion` is the `featurecreepmc-<minecraftVersion>` artifact.

Maven runs Minecraft with `javaExecutable` (default: the JVM running Maven). Use JDK 25.