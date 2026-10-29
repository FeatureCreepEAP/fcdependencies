package featurecreep.fcdependencies.maven;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import featurecreep.fcdependencies.core.minecraft.MinecraftSide;

@Mojo(name = "prepare-client", requiresDependencyResolution = ResolutionScope.NONE)
public class PrepareClientMojo extends AbstractFcMojo {

	@Override
	protected void run() throws Exception {
		prepare(MinecraftSide.CLIENT);
	}
}
