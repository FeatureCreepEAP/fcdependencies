package featurecreep.fcdependencies.core.minecraft;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MinecraftVersionSupportTest {

	@Test
	void runTasksNeedVersion26_1_2OrNewer() {
		assertFalse(MinecraftVersionSupport.supportsRunTasks("1.21.11"));
		assertFalse(MinecraftVersionSupport.supportsRunTasks("26.1"));
		assertFalse(MinecraftVersionSupport.supportsRunTasks("26.1.1"));
		assertTrue(MinecraftVersionSupport.supportsRunTasks("26.1.2"));
		assertTrue(MinecraftVersionSupport.supportsRunTasks("26.3"));
		assertTrue(MinecraftVersionSupport.supportsRunTasks("27.0"));
	}

	@Test
	void snapshotVersionsUseTheirNumericPrefix() {
		assertTrue(MinecraftVersionSupport.supportsRunTasks("26.4-snapshot-6"));
	}

	@Test
	void requireRunSupportRejectsOlderVersionsWithTheVersionInTheMessage() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> MinecraftVersionSupport.requireRunSupport("1.21.11"));
		assertTrue(error.getMessage().contains("1.21.11"));
	}

	@Test
	void requireRunSupportAcceptsNewerVersions() {
		assertDoesNotThrow(() -> MinecraftVersionSupport.requireRunSupport("26.3"));
	}

	@Test
	void missingComponentsCountAsZero() {
		assertEquals(0, MinecraftVersionSupport.compareVersions("26.1", "26.1.0"));
		assertTrue(MinecraftVersionSupport.compareVersions("26.10", "26.9") > 0);
	}
}
