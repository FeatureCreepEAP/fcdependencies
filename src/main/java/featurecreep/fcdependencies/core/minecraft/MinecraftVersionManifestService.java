package featurecreep.fcdependencies.core.minecraft;

import java.net.URL;

import org.jboss.dmr.ModelNode;

public class MinecraftVersionManifestService {

	private static final String MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest.json";

	public ModelNode getVersionJson(String version) throws Exception {

		String manifestVersion = MinecraftVersionSupport.resolveManifestVersion(version);
		ModelNode manifest = ModelNode.fromJSONStream(new URL(MANIFEST).openStream());

		for (ModelNode node : manifest.get("versions").asList()) {
			if (node.get("id").asString().equals(manifestVersion)) {
				return ModelNode.fromJSONStream(new URL(node.get("url").asString()).openStream());
			}
		}

		throw new IllegalArgumentException("Version not found: " + version + " (manifest id " + manifestVersion + ")");
	}
}