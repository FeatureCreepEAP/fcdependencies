package featurecreep.fcdependencies.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import featurecreep.fcdependencies.core.minecraft.MinecraftPreparer;
import featurecreep.fcdependencies.core.minecraft.MinecraftSide;

@DisableCachingByDefault(because = "Minecraft files are checked against the version manifest on every run")
public abstract class PrepareMinecraftTask extends DefaultTask {

	@Input
	public abstract Property<String> getMinecraftVersion();

	@Input
	public abstract Property<MinecraftSide> getSide();

	@Internal
	public abstract DirectoryProperty getOutputDirectory();

	public PrepareMinecraftTask() {
		getOutputs().upToDateWhen(task -> false);
	}

	@TaskAction
	public void prepare() throws Exception {
		MinecraftPreparer.prepare(getMinecraftVersion().get(), getSide().get(),
				getOutputDirectory().get().getAsFile().toPath(), message -> getLogger().lifecycle(message));
	}
}
