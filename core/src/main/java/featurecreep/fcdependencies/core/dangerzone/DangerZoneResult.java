package featurecreep.fcdependencies.core.dangerzone;

import java.io.File;

public class DangerZoneResult {

	public final File jarFile;
	public final File sourcesDir;

	public DangerZoneResult(File jarFile, File sourcesDir) {
		this.jarFile = jarFile;
		this.sourcesDir = sourcesDir;
	}
}
