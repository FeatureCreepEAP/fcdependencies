package featurecreep.fcdependencies.core.minecraft;

import org.jboss.dmr.ModelNode;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class MinecraftJarService {

	public File downloadJar(String version, ModelNode json, MinecraftSide side, File outputDir) throws Exception {

		if (MinecraftVersionSupport.requiresCustomJar(version)) {
			System.out.println("[DEBUG] Using custom jar download path.");
			return downloadCustomJar(side, outputDir);
		}

		String key = side == MinecraftSide.CLIENT ? "client" : "server";
		File out = new File(outputDir, key + ".jar");

		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		if (out.exists()) {
			System.out.println("[DEBUG] Jar already exists, deleting: " + out.getAbsolutePath());
			Files.delete(out.toPath());
		}

		String url = json.get("downloads").get(key).get("url").asString();

		System.out.println("[DEBUG] Downloading from: " + url);

		try (InputStream in = new URL(url).openStream()) {
			Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

		System.out.println("[DEBUG] Download complete.");

		return out;
	}

	private File downloadCustomJar(MinecraftSide side, File dir) throws Exception {

		String url = side == MinecraftSide.CLIENT
				? "https://piston-data.mojang.com/v1/objects/4509ee9b65f226be61142d37bf05f8d28b03417b/client.jar"
				: "https://piston-data.mojang.com/v1/objects/3ca78d5068bf9b422f694d3f0820e289581c0f0d/server.jar";

		File out = new File(dir, side == MinecraftSide.CLIENT ? "client.jar" : "server.jar");

		if (out.exists()) {
			return out;
		}

		try (InputStream in = new URL(url).openStream()) {
			Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

		return out;
	}
}