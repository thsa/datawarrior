package com.actelion.research.datawarrior.plugin;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.db.DETaskPluginTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import org.openmolecules.datawarrior.plugin.IChemistryPanel;
import org.openmolecules.datawarrior.plugin.IPluginHelper;
import org.openmolecules.datawarrior.plugin.IUserInterfaceHelper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class PluginGUIHelper implements IUserInterfaceHelper {
	private DataWarrior mApplication;
	private Dialog mDialog;
	private DETaskPluginTask mPluginTask;
	private CompoundTableModel mTableModel;
	private boolean mIsInteractive;
	private String mDefaultButtonText;

	public PluginGUIHelper(DataWarrior application, Dialog dialog, DETaskPluginTask task, boolean isInteractive) {
		mApplication = application;
		mDialog = dialog;
		mPluginTask = task;
		DEFrame frame = application.getActiveFrame();
		if (frame != null)
			mTableModel = frame.getTableModel();

		mIsInteractive = isInteractive;
	}

	@Override
	public Dialog getParentDialog() {
		return mDialog;
	}

	public String getDefaultButtonText() {
		return (mDefaultButtonText == null) ? "OK" : mDefaultButtonText;
	}

	@Override
	public void setDefaultButtonText(String text) {
		mDefaultButtonText = text;
	}

	@Override
	public float getUIScaleFactor() {
		return HiDPIHelper.getUIScaleFactor();
		}

	@Override
	public float getRetinaScaleFactor() {
		return HiDPIHelper.getRetinaScaleFactor();
		}

	@Override
	public IChemistryPanel getChemicalEditor() {
		return new PluginChemistryPanel();
		}

	@Override
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

	@Override
	public IPluginHelper getPluginHelper() {
		return new PluginHelper(mApplication, mPluginTask, null);
	}
}
