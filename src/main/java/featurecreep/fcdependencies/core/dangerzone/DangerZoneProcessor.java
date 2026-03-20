package featurecreep.fcdependencies.core.dangerzone;

import java.io.File;

public class DangerZoneProcessor {

	public static final String VERSION = "2.7";

	public DangerZoneResult process(String version, File workingDir) throws Exception {

		if (!VERSION.equals(version)) {
			throw new IllegalArgumentException("Unsupported DangerZone version: " + version + ". Supported: " + VERSION);
		}

		DangerZoneZipService zipService = new DangerZoneZipService();

		File runtimeZip = zipService.downloadRuntimeZip(workingDir);
		File sourcesZip = zipService.downloadSourcesZip(workingDir);

		File jarFile = zipService.extractRuntimeJar(runtimeZip, workingDir);
		File sourcesDir = zipService.extractSourcesDirectory(sourcesZip, workingDir);

		return new DangerZoneResult(jarFile, sourcesDir);
	}
}
