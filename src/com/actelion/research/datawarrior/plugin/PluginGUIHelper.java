package com.actelion.research.datawarrior.plugin;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import org.openmolecules.datawarrior.plugin.IChemistryPanel;
import org.openmolecules.datawarrior.plugin.IUserInterfaceHelper;

public class PluginGUIHelper implements IUserInterfaceHelper {
	public float getUIScaleFactor() {
		return HiDPIHelper.getUIScaleFactor();
		}

	public float getRetinaScaleFactor() {
		return HiDPIHelper.getRetinaScaleFactor();
		}

	public IChemistryPanel getChemicalEditor() {
		return new PluginChemistryPanel();
		}
	}
