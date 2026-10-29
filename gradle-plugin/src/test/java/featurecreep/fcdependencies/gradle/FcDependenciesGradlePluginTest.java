package featurecreep.fcdependencies.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.gradle.api.Project;
import org.gradle.api.tasks.JavaExec;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

class FcDependenciesGradlePluginTest {

	@Test
	void registersRunTasksAndPrepareTasks() {
		Project project = ProjectBuilder.builder().build();
		project.getPluginManager().apply(FcDependenciesGradlePlugin.class);
		project.getExtensions().getByType(FcDependenciesExtension.class).getMinecraftVersion().set("26.3");

		assertInstanceOf(JavaExec.class, project.getTasks().getByName("runClient"));
		assertInstanceOf(JavaExec.class, project.getTasks().getByName("runServer"));
		assertNotNull(project.getTasks().findByName("fcdependenciesPrepareClient"));
		assertNotNull(project.getTasks().findByName("fcdependenciesPrepareServer"));
	}

	@Test
	void playerNameDefaultsToDev() {
		Project project = ProjectBuilder.builder().build();
		project.getPluginManager().apply(FcDependenciesGradlePlugin.class);

		assertEquals("Dev", project.getExtensions().getByType(FcDependenciesExtension.class).getPlayerName().get());
	}
}
