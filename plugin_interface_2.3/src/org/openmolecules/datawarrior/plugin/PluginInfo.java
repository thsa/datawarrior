package org.openmolecules.datawarrior.plugin;

import javax.swing.*;

public class PluginInfo {
	public static void main(final String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				JOptionPane.showMessageDialog(null,
					"This plugin extends DataWarrior's functionality.\n"
					+ "To use the plugin you need to move this file\n"
					+ "into the DataWarrior plugin folder.");
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		} );
	}
}
