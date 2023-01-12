import org.openmolecules.datawarrior.plugin.IPluginInitializer;

import java.awt.Frame;
import java.util.Properties;
import java.io.File;
import javax.swing.*;

public class PluginInitializer implements IPluginInitializer {
	@Override
	public void initialize(File pluginDir, Properties config) {
		JOptionPane.showMessageDialog(null, "PluginInitializer: I am initializing!!!");
	}
}
