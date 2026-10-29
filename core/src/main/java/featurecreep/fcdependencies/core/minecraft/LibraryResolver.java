package featurecreep.fcdependencies.core.minecraft;

import java.util.ArrayList;
import java.util.List;

import org.jboss.dmr.ModelNode;

public final class LibraryResolver {

	/** A file listed in a version JSON, with the path relative to the libraries directory. */
	public record Artifact(String path, String url, String sha1, long size) {
	}

	private LibraryResolver() {
	}

	public static List<Artifact> resolve(ModelNode versionJson, Platform platform) {
		List<Artifact> artifacts = new ArrayList<>();
		if (!versionJson.hasDefined("libraries")) return artifacts;

		for (ModelNode library : versionJson.get("libraries").asList()) {
			if (!platform.allowed(library.hasDefined("rules") ? library.get("rules") : null)) continue;
			if (!library.hasDefined("downloads")) continue;

			ModelNode downloads = library.get("downloads");
			if (downloads.hasDefined("artifact")) {
				artifacts.add(toArtifact(downloads.get("artifact")));
			}

			if (library.hasDefined("natives") && downloads.hasDefined("classifiers")) {
				ModelNode natives = library.get("natives");
				String classifier = natives.hasDefined(platform.osName())
						? natives.get(platform.osName()).asString().replace("${arch}", "64")
						: null;
				if (classifier != null && downloads.get("classifiers").hasDefined(classifier)) {
					artifacts.add(toArtifact(downloads.get("classifiers").get(classifier)));
				}
			}
		}
		return artifacts;
	}

	private static Artifact toArtifact(ModelNode node) {
		String sha1 = node.hasDefined("sha1") ? node.get("sha1").asString() : null;
		long size = node.hasDefined("size") ? node.get("size").asLong() : -1L;
		return new Artifact(node.get("path").asString(), node.get("url").asString(), sha1, size);
	}
}
