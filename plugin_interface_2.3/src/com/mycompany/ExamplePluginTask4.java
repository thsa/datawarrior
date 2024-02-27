package com.mycompany;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.ScaffoldHelper;
import com.actelion.research.chem.StereoMolecule;
import org.openmolecules.datawarrior.plugin.IPluginHelper;
import org.openmolecules.datawarrior.plugin.IPluginTask;
import org.openmolecules.datawarrior.plugin.IUserInterfaceHelper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Properties;

/**
 * This PluginTask calculates the scaffolds from the structures withen a chosen column.
 * The user may choose from two different scaffold types, Murcko or most central ring system.
 * He also may select both. Then the plugin generates the respective number of new columns
 * and defines their type to be COLUMN_TYPE_STRUCTURE_FROM_IDCODE.
 * Then, in a loop over all rows, the plugin gets the chemical structures from the currently
 * open DataWarrior table, detects and extracts the scaffold(s) and writes them into (a) new column(s).
 * Under the hood, DataWarrior generates (an) invisible column(s) to store scaffold atom coordinates.
 * Finally, it calls finalizeNewColumns(), which causes DataWarrior to analyse the columns and to
 * create new default filters for them.
 * In comparison to ExamplePluginTask3 this task generates structure columns rather than alphanumerical
 * columns and, more importantly, this task uses functionality of OpenChemLib to extract scaffolds
 * from the source molecules. This means that openchemlib.jar needs to be in the classpath when
 * building the plugin, but it MUST NOT be part of the plugin jar itself, because openchemlib
 * is part of DataWarrior already.
 */
public class ExamplePluginTask4 implements IPluginTask {
	private static final String CONFIGURATION_STRUCTURE_COLUMN = "structureColumn";
	private static final String CONFIGURATION_ACTION = "action";
	private static final String[] ACTION_NAME = { "Murcko Scaffold", "Central Ring System" };
	private static final String[] ACTION_CODE = { "murcko", "ringsystem" };

	private JComboBox<String> mComboBox;
	private JCheckBox[] mCheckBox;

	@Override public String getTaskCode() {
		return "OpenMoleculesExample004";
	}

	@Override public String getTaskName() {
		return "Plugin Example: Extract Scaffold";
	}

	/**
	 * This method expects a JPanel with all UI-elements for defining the task details.
	 * These may include elements to define a structure search and/or alphanumerical
	 * search criteria. 'Cancel' and 'OK' buttons are provided outside of this panel.
	 * @param dialogHelper is not used in this example
	 * @return
	 */
	@Override public JComponent createDialogContent(IUserInterfaceHelper dialogHelper) {
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(4+ ACTION_NAME.length, 1, 8, 8));
		panel.add(new JLabel("Select Structure Column:"));

		mComboBox = dialogHelper.getComboBoxForColumnSelection(IUserInterfaceHelper.COLUMN_TYPE_STRUCTURE);
		panel.add(mComboBox);

		panel.add(new JLabel(""));

		panel.add(new JLabel("Select scaffold type:"));
		mCheckBox = new JCheckBox[ACTION_NAME.length];
		for (int i = 0; i<ACTION_NAME.length; i++) {
			mCheckBox[i] = new JCheckBox(ACTION_NAME[i]);
			panel.add(mCheckBox[i]);
		}

		return panel;
	}

	/**
	 * This method is called after the users presses the dialog's 'OK' button.
	 * At this time the dialog is still shown. This method expects a Properties
	 * object containing all UI-elements' states converted into key-value pairs
	 * describing the user defined database query. This query configuration is
	 * used later for two purposes:<br>
	 * - to run the task independent of the actual dialog<br>
	 * - to populate a dialog with a query that has been performed earlier<br>
	 * @return query configuration
	 */
	@Override public Properties getDialogConfiguration() {
		StringBuilder sb = new StringBuilder();
		Properties configuration = new Properties();
		if (mComboBox.getItemCount() != 0)
			configuration.setProperty(CONFIGURATION_STRUCTURE_COLUMN, (String)mComboBox.getSelectedItem());

		for (int i = 0; i<ACTION_CODE.length; i++) {
			if (mCheckBox[i].isSelected()) {
				if (sb.length() != 0)
					sb.append(",");
				sb.append(ACTION_CODE[i]);
			}
		}
		configuration.setProperty(CONFIGURATION_ACTION, sb.toString());
		return configuration;
	}

	/**
	 * This method configures an empty dialog according to the given configuration object.
	 * @param configuration
	 */
	@Override public void setDialogConfiguration(Properties configuration) {
		mComboBox.setSelectedItem(configuration.getProperty(CONFIGURATION_STRUCTURE_COLUMN));

		String[] propertyCode = configuration.getProperty(CONFIGURATION_ACTION, "").split(",");
		for (int i = 0; i<ACTION_CODE.length; i++) {
			boolean found = false;
			for (String code:propertyCode) {
				if (ACTION_CODE[i].equals(code)) {
					found = true;
					break;
				}
			}
			mCheckBox[i].setSelected(found);
		}
	}

	/**
	 * Checks, whether the given dialog configuration is a valid one.
	 * If not, then this method should return a short and clear error message
	 * intended for the user in order to correct the dialog setting.
	 * @param configuration
	 * @return user-interpretable error message or null, if query configuration is valid
	 */
	@Override public String checkConfiguration(Properties configuration) {
		String structureColumn = configuration.getProperty(CONFIGURATION_STRUCTURE_COLUMN, "");
		if (structureColumn.isEmpty())
			return "No chemical structures found.";

		String properties = configuration.getProperty(CONFIGURATION_ACTION, "");
		if (properties.isEmpty())
			return "You need to select at least one action.";

		return null;
	}

	/**
	 * This method performes does all the work. First, it determines from the configuration
	 * which properties need to be calculated. Then, it determines the source structure column.
	 * Then, it creates new columns for all selected scaffold types and, in a loop, determines
	 * scaffold structures and writes them into the new cells. Finally, DataWarrior is notified
	 * that all cells have received their final content, which causes DataWarrior to analyze
	 * the cells, detect their data types and create reasonable default filters.
	 * Any task configuration must be taken from the passed configuration object and NOT from
	 * any UI-elements of the dialog. The concept is to completely separate task definition
	 * from task execution, which is the basis for DataWarrior's macros to work.
	 * If an error occurrs, then this method should call dwInterface.showErrorMessage().
	 * @param configuration
	 * @param dwInterface
	 */
	@Override public void run(Properties configuration, IPluginHelper dwInterface) {
		try {
			int structureColumn = dwInterface.findColumn(configuration.getProperty(CONFIGURATION_STRUCTURE_COLUMN));
			if (structureColumn == -1)
				structureColumn = dwInterface.getStructureColumn();
			if (structureColumn == -1) {
				dwInterface.showErrorMessage("No structure column found.");
				return;
			}

			ArrayList<String> columnTitleList = new ArrayList<>();
			boolean[] doAction = new boolean[ACTION_CODE.length];
			String[] actionCode = configuration.getProperty(CONFIGURATION_ACTION, "").split(",");
			for (int i = 0; i<ACTION_CODE.length; i++) {
				for (String code : actionCode) {
					if (ACTION_CODE[i].equals(code)) {
						columnTitleList.add(ACTION_NAME[i]);
						doAction[i] = true;
						break;
					}
				}
			}

			int firstNewColumn = dwInterface.initializeNewColumns(columnTitleList.toArray(new String[0]));
			for (int i=0; i<columnTitleList.size(); i++)
				dwInterface.setColumnType(firstNewColumn+i, IPluginHelper.COLUMN_TYPE_STRUCTURE_FROM_IDCODE);

			for (int row = 0; row<dwInterface.getTotalRowCount(); row++) {
				String structure = dwInterface.getCellDataAsIDCode(row, structureColumn);
				if (structure != null) {
					StereoMolecule mol = new IDCodeParser().getCompactMolecule(structure);
					if (mol != null) {
						int column = firstNewColumn;
						for (int i = 0; i<ACTION_CODE.length; i++) {
							if (doAction[i]) {
								StereoMolecule scaffold = getScaffold(mol, i);
								if (scaffold != null) {
									Canonizer canonizer = new Canonizer(scaffold);
									String idcode = canonizer.getIDCode();
									String coords = canonizer.getEncodedCoordinates();
									dwInterface.setCellData(column, row, idcode.concat(" ").concat(coords));
								}
								column++;
							}
						}
					}
				}
			}

			dwInterface.finalizeNewColumns(firstNewColumn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private StereoMolecule getScaffold(StereoMolecule mol, int actionNo) {
		switch (actionNo) {
			case 0:
				return ScaffoldHelper.getMurckoScaffold(mol,false);
			case 1:
				return ScaffoldHelper.getMostCentralRingSystem(mol);
		}
		return null;
	}
}
