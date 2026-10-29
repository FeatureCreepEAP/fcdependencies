package featurecreep.fcdependencies.core.minecraft;

import java.io.File;

public class MinecraftResult {
	public final File mappedJar;
	public final File sourcesDir;

	public MinecraftResult(File mappedJar, File sourcesDir) {
		this.mappedJar = mappedJar;
		this.sourcesDir = sourcesDir;
	}
}