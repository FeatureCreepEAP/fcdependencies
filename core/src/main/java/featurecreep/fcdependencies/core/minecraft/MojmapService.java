package featurecreep.fcdependencies.core.minecraft;

import com.asbestosstar.assistremapper.mappings.Proguard;
import com.asbestosstar.assistremapper.Mappings;
import org.jboss.dmr.ModelNode;

import java.io.InputStream;
import java.net.URL;

public class MojmapService {

	public Mappings downloadMappings(ModelNode versionJson, MinecraftSide side) throws Exception {

		String key = side == MinecraftSide.CLIENT ? "client_mappings" : "server_mappings";

		if (!versionJson.get("downloads").has(key)) {
			return null;
		}

		String url = versionJson.get("downloads").get(key).get("url").asString();

		InputStream stream = new URL(url).openStream();

		// MojMap is Proguard format (reverse needed)
		return new Proguard(stream).getReverse();
	}
}