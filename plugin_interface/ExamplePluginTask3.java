import org.openmolecules.datawarrior.plugin.IPluginHelper;
import org.openmolecules.datawarrior.plugin.IPluginTask;
import org.openmolecules.datawarrior.plugin.IUserInterfaceHelper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Properties;

/**
 * This PluginTask offers three different properties to be calculated from the chemical structure.
 * The user is asked, which of the properties shall be calculated. Then the plugin generates
 * the respective number of new columns. Then, in a loop over all rows, the plugin gets the
 * chemical structures from the currently open DataWarrior table, calculates the properties,
 * and writes them into the new columns. Finally, it calls finalizeNewColumns(), which causes
 * DataWarrior to analyse the columns and to create new default filters for them.
 */
public class ExamplePluginTask3 implements IPluginTask {
	private static final String CONFIGURATION_STRUCTURE_COLUMN = "structureColumn";
	private static final String CONFIGURATION_PROPERTY_LIST = "propertyList";
	private static final String[] PROPERTY_NAME = { "Smiles Length", "Smiles 'C' Count", "Ratio O/C" };
	private static final String[] PROPERTY_CODE = { "slen", "ccount", "o/c" };

	private JComboBox<String> mComboBox;
	private JCheckBox[] mCheckBox;

	@Override public String getTaskCode() {
		return "OpenMoleculesExample003";
	}

	@Override public String getTaskName() {
		return "Plugin Example: Add Properties";
	}

	/**
	 * This method expects a JPanel with all UI-elements for defining a database query.
	 * These may include elements to define a structure search and/or alphanumerical
	 * search criteria. 'Cancel' and 'OK' buttons are provided outside of this panel.
	 * @param dialogHelper is not used in this example
	 * @return
	 */
	@Override public JComponent createDialogContent(IUserInterfaceHelper dialogHelper) {

		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(4+PROPERTY_NAME.length, 1, 8, 8));
		panel.add(new JLabel("Select Structure Column:"));

		mComboBox = dialogHelper.getComboBoxForColumnSelection(IUserInterfaceHelper.COLUMN_TYPE_STRUCTURE);
		panel.add(mComboBox);

		panel.add(new JLabel(""));

		panel.add(new JLabel("Select Properties"));
		mCheckBox = new JCheckBox[PROPERTY_NAME.length];
		for (int i=0; i<PROPERTY_NAME.length; i++) {
			mCheckBox[i] = new JCheckBox(PROPERTY_NAME[i]);
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
	 * - to run the query independent from the actual dialog<br>
	 * - to populate a dialog with a query that has been performed earlier<br>
	 * @return query configuration
	 */
	@Override public Properties getDialogConfiguration() {
		StringBuilder sb = new StringBuilder();
		Properties configuration = new Properties();
		configuration.setProperty(CONFIGURATION_STRUCTURE_COLUMN, (String)mComboBox.getSelectedItem());

		for (int i=0; i<PROPERTY_CODE.length; i++) {
			if (mCheckBox[i].isSelected()) {
				if (sb.length() != 0)
					sb.append(",");
				sb.append(PROPERTY_CODE[i]);
			}
		}
		configuration.setProperty(CONFIGURATION_PROPERTY_LIST, sb.toString());
		return configuration;
	}

	/**
	 * This method configures an empty dialog according to the given configuration object.
	 * @param configuration
	 */
	@Override public void setDialogConfiguration(Properties configuration) {
		mComboBox.setSelectedItem(configuration.getProperty(CONFIGURATION_STRUCTURE_COLUMN));

		String[] propertyCode = configuration.getProperty(CONFIGURATION_PROPERTY_LIST, "").split(",");
		for (int i=0; i<PROPERTY_CODE.length; i++) {
			boolean found = false;
			for (String code:propertyCode) {
				if (PROPERTY_CODE[i].equals(code)) {
					found = true;
					break;
				}
			}
			mCheckBox[i].setSelected(found);
		}
	}

	/**
	 * Checks, whether the given dialog configuration is a valid one.
	 * If not, the this method should return a short and clear error message
	 * intended for the user in order to correct the dialog setting.
	 * @param configuration
	 * @return user-interpretable error message or null, if query configuration is valid
	 */
	@Override public String checkConfiguration(Properties configuration) {
		String properties = configuration.getProperty(CONFIGURATION_PROPERTY_LIST, "");
		if (properties.length() == 0)
			return "You need to select at least one of the properies.";

		return null;
	}

	/**
	 * This method performes does all the work. First, it determines from the configuration
	 * which properties need to be calculated. Then, it determines the source structure column.
	 * Then, it creates new columns for all selected properties and, in a loop, calculates
	 * the properties and writes them into the new cells. Finally, DataWarrior is notified
	 * that all cells have received their final content, which causes DataWarrior to analyze
	 * the cells, detect their data types and create reasonable default filters.
	 * The property list definition must be taken from the passed configuration object and NOT from
	 * any UI-elements of the dialog. The concept is to completely separate query definition
	 * from query execution, which is the basis for DataWarrior's macros to work.
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
			boolean[] calcProperty = new boolean[PROPERTY_CODE.length];
			String[] propertyCode = configuration.getProperty(CONFIGURATION_PROPERTY_LIST, "").split(",");
			for (int i = 0; i<PROPERTY_CODE.length; i++) {
				for (String code : propertyCode) {
					if (PROPERTY_CODE[i].equals(code)) {
						columnTitleList.add(PROPERTY_NAME[i]);
						calcProperty[i] = true;
						break;
					}
				}
			}

			int firstNewColumn = dwInterface.initializeNewColumns(columnTitleList.toArray(new String[0]));

			for (int row = 0; row<dwInterface.getTotalRowCount(); row++) {
				String smiles = dwInterface.getCellDataAsSmiles(row, structureColumn);
				if (smiles != null) {
					int column = firstNewColumn;
					for (int i = 0; i<PROPERTY_CODE.length; i++) {
						if (calcProperty[i])
							dwInterface.setCellData(column++, row, calculateProperty(smiles, i));
					}
				}
			}

			dwInterface.finalizeNewColumns(firstNewColumn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String calculateProperty(String smiles, int propertyNo) {
		switch (propertyNo) {
			case 0:
				return ""+smiles.length();
			case 1:
				int count = 0;
				for (int i=0; i<smiles.length(); i++)
					if (smiles.charAt(i) == 'C' || smiles.charAt(i) == 'c')
						count++;
				return ""+count;
			case 2:
				int countC = 0;
				int countO = 0;
				for (int i=0; i<smiles.length(); i++) {
					if (smiles.charAt(i) == 'C' || smiles.charAt(i) == 'c')
						countC++;
					if (smiles.charAt(i) == 'O' || smiles.charAt(i) == 'o')
						countO++;
				}
				return ""+(float)countO/(float)countC;
		}
		return "unknown";
	}
}
