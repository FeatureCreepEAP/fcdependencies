package featurecreep.fcdependencies.core.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MinecraftDownloaderTest {

	private static final String ABC_SHA1 = "a9993e364706816aba3e25717850c26c9cd0d89d";

	@TempDir
	Path tmp;

	@Test
	void sha1MatchesKnownDigest() throws Exception {
		Path file = Files.writeString(tmp.resolve("abc.txt"), "abc");

		assertEquals(ABC_SHA1, MinecraftDownloader.sha1(file));
	}

	@Test
	void matchesIgnoresHashCaseAndChecksSize() throws Exception {
		Path file = Files.writeString(tmp.resolve("abc.txt"), "abc");

		assertTrue(MinecraftDownloader.matches(file, ABC_SHA1.toUpperCase(Locale.ROOT), 3));
		assertFalse(MinecraftDownloader.matches(file, ABC_SHA1, 4));
		assertFalse(MinecraftDownloader.matches(file, "0000", -1));
	}

	@Test
	void missingHashOnlyChecksSize() throws Exception {
		Path file = Files.writeString(tmp.resolve("abc.txt"), "abc");

		assertTrue(MinecraftDownloader.matches(file, null, -1));
		assertTrue(MinecraftDownloader.matches(file, "", 3));
	}

	@Test
	void existingMatchingFileIsNotDownloadedAgain() throws Exception {
		Path target = tmp.resolve("lib/abc.jar");
		Files.createDirectories(target.getParent());
		Files.writeString(target, "abc");
		List<String> logs = new ArrayList<>();

		// The URL is unreachable on purpose: a skipped download never opens it.
		new MinecraftDownloader(logs::add).download("http://invalid.invalid/abc.jar", target, ABC_SHA1, 3);

		assertTrue(logs.isEmpty());
	}

	@Test
	void downloadWritesVerifiedContentWithoutLeavingTempFile() throws Exception {
		Path source = Files.writeString(tmp.resolve("source.jar"), "abc");
		Path target = tmp.resolve("out/abc.jar");

		new MinecraftDownloader(message -> { }).download(source.toUri().toString(), target, ABC_SHA1, 3);

		assertEquals("abc", Files.readString(target));
		assertFalse(Files.exists(target.resolveSibling("abc.jar.part")));
	}

	@Test
	void checksumMismatchIsRejectedAndNothingIsKept() throws Exception {
		Path source = Files.writeString(tmp.resolve("source.jar"), "abc");
		Path target = tmp.resolve("out/abc.jar");
		MinecraftDownloader downloader = new MinecraftDownloader(message -> { });

		assertThrows(IOException.class,
				() -> downloader.download(source.toUri().toString(), target, "0000", -1));

		assertFalse(Files.exists(target));
		assertFalse(Files.exists(target.resolveSibling("abc.jar.part")));
	}
}
