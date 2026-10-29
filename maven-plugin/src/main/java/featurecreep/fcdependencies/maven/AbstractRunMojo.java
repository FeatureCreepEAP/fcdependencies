package featurecreep.fcdependencies.maven;

import java.io.IOException;
import java.util.List;

import featurecreep.fcdependencies.core.minecraft.LaunchPlanner;
import featurecreep.fcdependencies.core.minecraft.MinecraftSide;
import featurecreep.fcdependencies.core.minecraft.MinecraftVersionSupport;
import featurecreep.fcdependencies.core.minecraft.RunMode;

public abstract class AbstractRunMojo extends AbstractFcMojo {

	protected abstract MinecraftSide side();

	protected abstract LaunchPlanner.Plan plan(RunMode mode, List<String> featureCreepJars) throws IOException;

	@Override
	protected final void run() throws Exception {
		MinecraftVersionSupport.requireRunSupport(minecraftVersion);
		RunMode mode = mode();
		getLog().info("starting Minecraft " + minecraftVersion + " " + side().name().toLowerCase()
				+ " (" + mode.name().toLowerCase() + ")");
		prepare(side());
		if (mode == RunMode.FEATURECREEP) {
			stageLibraries();
			installModule();
		}
		launch(plan(mode, featureCreepJars(mode)));
	}
}
