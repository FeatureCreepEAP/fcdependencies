package featurecreep.fcdependencies.core.minecraft;

import java.io.File;

import org.jboss.dmr.ModelNode;

import com.asbestosstar.assistremapper.Mappings;

import featurecreep.fcdependencies.core.DecompilerService;

public class MinecraftProcessor {

	public MinecraftResult process(String version, MinecraftSide side, DecompilerService.Engine engine, File workingDir)
			throws Exception {

		MinecraftVersionManifestService manifest = new MinecraftVersionManifestService();
		MojmapService mojmapService = new MojmapService();
		MinecraftJarService jarService = new MinecraftJarService();
		MinecraftRemapper remapper = new MinecraftRemapper();
		DecompilerService decompiler = new DecompilerService();

		ModelNode json = manifest.getVersionJson(version);

		File vanillaJar = jarService.downloadJar(version, json, side, workingDir);

		Mappings mappings = mojmapService.downloadMappings(json, side);

		File mappedJar = remapper.remap(version, vanillaJar, mappings, workingDir);

		File sourcesDir = new File(workingDir, "sources");
		if (!sourcesDir.exists()) {
			sourcesDir.mkdirs();
		}

		decompiler.decompile(engine, mappedJar.getAbsolutePath(), sourcesDir.getAbsolutePath());

		return new MinecraftResult(mappedJar, sourcesDir);
	}
}