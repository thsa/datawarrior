package chemaxon.marvin.calculations;

import chemaxon.marvin.plugin.PluginException;
import chemaxon.struc.Molecule;

public class pKaPlugin {
	public static final int ACIDIC = 0;
	public static final int BASIC = 0;

	public void getMacropKaValues(int type, double[] pKa, int[] index) throws PluginException {
		if (pKa == null)
			throw new PluginException();
	}

	public boolean isLicensed() { return false; };
	public void setMaxIons(int max) {}
	public void setBasicpKaLowerLimit(double l) {}
	public void setAcidicpKaUpperLimit(double l) {}
	public void setpHLower(double l) {}
	public void setpHUpper(double l) {}
	public void setpHStep(double l) {}
	public void setMolecule(Molecule l) {}
	public void run() {}
}

