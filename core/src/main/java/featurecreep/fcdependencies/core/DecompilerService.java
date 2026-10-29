package featurecreep.fcdependencies.core;

public class DecompilerService {

	public enum Engine {
		FABRIC_CFR, VINEFLOWER
	}

	public void decompile(Engine engine, String jar, String output) throws Exception {

		switch (engine) {

		case FABRIC_CFR:
			org.benf.cfr.reader.Main.main(new String[] { jar, "--outputdir", output });
			break;

		case VINEFLOWER:
			org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler.main(new String[] { jar, output });
			break;
		}
	}
}