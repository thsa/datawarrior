package com.actelion.research.datawarrior.plugin;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import org.openmolecules.datawarrior.plugin.IChemistryPanel;
import org.openmolecules.datawarrior.plugin.IUserInterfaceHelper;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

public class PluginGUIHelper implements IUserInterfaceHelper {
	private CompoundTableModel mTableModel;
	private boolean mIsInteractive;

	public PluginGUIHelper(DEFrame owner, boolean isInteractive) {
		if (owner != null)
			mTableModel = owner.getTableModel();
		mIsInteractive = isInteractive;
	}

	public float getUIScaleFactor() {
		return HiDPIHelper.getUIScaleFactor();
		}

	public float getRetinaScaleFactor() {
		return HiDPIHelper.getRetinaScaleFactor();
		}

	public IChemistryPanel getChemicalEditor() {
		return new PluginChemistryPanel();
		}

	public JComboBox<String> getComboBoxForColumnSelection(int columnType) {
		ArrayList<String> columnNames = new ArrayList<>();
		if (mTableModel != null) {
			for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
				if (mTableModel.isColumnDisplayable(i)
				 && (((columnType & COLUMN_TYPE_STRUCTURE) != 0 && mTableModel.isColumnTypeStructure(i))
				  || ((columnType & COLUMN_TYPE_REACTION) != 0 && mTableModel.isColumnTypeReaction(i))
				  || ((columnType & COLUMN_TYPE_DATE) != 0 && mTableModel.isColumnTypeDate(i))
				  || ((columnType & COLUMN_TYPE_NUMERICAL) != 0 && mTableModel.isColumnTypeDouble(i) && !mTableModel.isColumnTypeDate(i))
				  || ((columnType & COLUMN_TYPE_CATEGORIES) != 0 && mTableModel.isColumnTypeCategory(i))
				  || ((columnType & COLUMN_TYPE_TEXT) != 0 && mTableModel.isColumnTypeString(i))))
					columnNames.add(mTableModel.getColumnTitle(i));
		}
		String[] columnName = columnNames.toArray(new String[0]);
		Arrays.sort(columnName);
		JComboBox<String> comboBox = new JComboBox<>(columnName);
		comboBox.setEditable(!mIsInteractive);
		return comboBox;
	}
}
