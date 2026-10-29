package featurecreep.fcdependencies.maven;

import java.io.IOException;
import java.util.List;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import featurecreep.fcdependencies.core.minecraft.LaunchPlanner;
import featurecreep.fcdependencies.core.minecraft.MinecraftSide;
import featurecreep.fcdependencies.core.minecraft.RunMode;

@Mojo(name = "run-server", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class RunServerMojo extends AbstractRunMojo {

	@Override
	protected MinecraftSide side() {
		return MinecraftSide.SERVER;
	}

	@Override
	protected LaunchPlanner.Plan plan(RunMode mode, List<String> featureCreepJars) throws IOException {
		return LaunchPlanner.server(versionDir(), runDir(), mode, featureCreepJars);
	}
}
