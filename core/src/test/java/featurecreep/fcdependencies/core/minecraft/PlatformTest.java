package featurecreep.fcdependencies.core.minecraft;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.Test;

class PlatformTest {

	private static final Platform MAC = new Platform("osx", "arm64");
	private static final Platform LINUX = new Platform("linux", "x86_64");

	@Test
	void missingRulesAllowEverything() {
		assertTrue(MAC.allowed(null));
	}

	@Test
	void osRuleOnlyMatchesThatOperatingSystem() {
		ModelNode rules = ModelNode.fromJSONString("[{\"action\":\"allow\",\"os\":{\"name\":\"osx\"}}]");
		assertTrue(MAC.allowed(rules));
		assertFalse(LINUX.allowed(rules));
	}

	@Test
	void lastMatchingRuleWins() {
		ModelNode rules = ModelNode.fromJSONString(
				"[{\"action\":\"allow\"},{\"action\":\"disallow\",\"os\":{\"name\":\"osx\"}}]");
		assertFalse(MAC.allowed(rules));
		assertTrue(LINUX.allowed(rules));
	}

	@Test
	void architectureIsChecked() {
		ModelNode rules = ModelNode.fromJSONString("[{\"action\":\"allow\",\"os\":{\"arch\":\"arm64\"}}]");
		assertTrue(MAC.allowed(rules));
		assertFalse(LINUX.allowed(rules));
	}

	@Test
	void featureRulesNeverMatch() {
		ModelNode rules = ModelNode.fromJSONString("[{\"action\":\"allow\",\"features\":{\"is_demo_user\":true}}]");
		assertFalse(MAC.allowed(rules));
	}

	@Test
	void currentPlatformUsesMojangOsNames() {
		assertTrue(Set.of("osx", "windows", "linux").contains(Platform.current().osName()));
	}
}
