package com.mycompany;

import org.openmolecules.datawarrior.plugin.IPluginHelper;
import org.openmolecules.datawarrior.plugin.IPluginTask;
import org.openmolecules.datawarrior.plugin.IUserInterfaceHelper;

import java.util.Properties;
import javax.swing.*;

/**
 * PluginTask creating a simple table, one with structures from SMILES
 * and one with structures created from molfiles.
 */
public class ExamplePluginTask1 implements IPluginTask {
	private final String CONFIGURATION_SELECTED = "selected";

	private final String[] OPTIONS = { "Simple", "SMILES", "Molfiles" };

	private final String[][] TABLE1 = {{ "123", "Dog" }, { "234", "Cat" }, { "326", "Cat" },
 		{ "422", "Rat" }, { "339", "Cat" }, { "318", "Dog" }, { "127", "Cat" }, { "551", "Rat" }};
	private final String[][] TABLE2 = {{ "Nc1ccncc1C=O", "Mouse" }, { "CCCO", "Rat" }};
	private final String[][] TABLE3 = {{ "1", "Mouse", "4.1" }, { "2", "Rat", "8.2" }, { "3", "Rat", "6.8" }, { "4", "Mouse", "2.2" }};

	public JComboBox<String> mComboBox;

	@Override public String getTaskCode() {
		return "OpenMoleculesExample001";
	}

	@Override public String getTaskName() {
		return "Plugin Example: Simple";
	}

	/**
	 * This method expects a JPanel with all UI-elements for defining a database query.
	 * These may include elements to define a structure search and/or alphanumerical
	 * search criteria. 'Cancel' and 'OK' buttons are provided outside of this panel.
	 * @param dialogHelper gives access to a chemistry panel to let the user draw a chemical (sub-)structure
	 * @return
	 */
	@Override public JComponent createDialogContent(IUserInterfaceHelper dialogHelper) {
		mComboBox = new JComboBox<String>(OPTIONS);
		mComboBox.setSelectedIndex(0);

		JPanel panel = new JPanel();
		panel.add(new JLabel("Select one:"));
		panel.add(mComboBox);

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
		Properties configuration = new Properties();
		configuration.setProperty(CONFIGURATION_SELECTED, (String)mComboBox.getSelectedItem());
		return configuration;
	}

	/**
	 * This method populates an empty database query dialog with a previously configured database query.
	 * @param configuration
	 */
	@Override public void setDialogConfiguration(Properties configuration) {
		String selected = configuration.getProperty(CONFIGURATION_SELECTED, OPTIONS[0]);
		mComboBox.setSelectedItem(selected);
	}

	/**
	 * Checks, whether the given database query configuration is a valid one.
	 * If not, the this method should return a short and clear error message
	 * intended for the user in order to correct the dialog setting.
	 * @param configuration
	 * @return user-interpretable error message or null, if query configuration is valid
	 */
	@Override public String checkConfiguration(Properties configuration) {
		return null;    // no need to check, any configuration will be acceptable
	}

	/**
	 * This method performes the database query. Typically it reads the query configuration from
	 * the given Properties object, sends it to a database server, retrieves a result and populates
	 * a new window's table with the retrieved data. The passed IPluginHelper object provides
	 * all needed methods to create a new DataWarrior window, to allocate result columns,
	 * to populate these columns with chemical and alphanumerical content, and to show an error
	 * message if something goes wrong.<br>
	 * The query definition must be taken from the passed configuration object and NOT from
	 * any UI-elements of the dialog. The concept is to completely separate query definition
	 * from query execution, which is the basis for DataWarrior's macros to work.
	 * If an error occurrs, then this method should call dwInterface.showErrorMessage().
	 * @param configuration
	 * @param dwInterface
	 */
	@Override public void run(Properties configuration, IPluginHelper dwInterface) {
		String selected = configuration.getProperty(CONFIGURATION_SELECTED);
		if (OPTIONS[0].equals(selected)) {
			// This example simulates a lengthy database retrieval with a delay of 0.2 sec between rows.
			dwInterface.initializeData(2, TABLE1.length, "Simple Example");
			dwInterface.setColumnTitle(0,"Number");
			dwInterface.setColumnTitle(1,"Animal");
			int row = 0;
			for (String[] rowData:TABLE1) {
				// we simulate a lengthy database retrieval to allow user to cancel
				try { Thread.sleep(200); } catch (InterruptedException ie) {}

				if (dwInterface.isCancelled())	// just exit if the user pressed cancel
					return;

				int column = 0;
				for (String value:rowData)
					dwInterface.setCellData(column++, row, value);
				row++;
			}
			dwInterface.finalizeData(null);
		}
		else if (OPTIONS[1].equals(selected)) {
			// This example shows how to use SMILES codes are converted into chemical structure.
			// We actually generate a SMILES column and a chemical structure column.
			dwInterface.initializeData(3, TABLE2.length, "Smiles Example");
			dwInterface.setColumnTitle(0,"Structure");
			dwInterface.setColumnTitle(1,"Smiles");
			dwInterface.setColumnTitle(2,"Animal");
			dwInterface.setColumnType(0, IPluginHelper.COLUMN_TYPE_STRUCTURE_FROM_SMILES);
			int row = 0;
			for (String[] rowData:TABLE2) {
				// we pass the smiles to be converted into a structure
				dwInterface.setCellData(0, row, rowData[0]);   
				int column = 1;
				for (String value:rowData)
					// Here we pass all alphanumerical content including the SMILES a second time.
					dwInterface.setCellData(column++, row, value);
				row++;
			}
			dwInterface.finalizeData(null);
		}
		else if (OPTIONS[2].equals(selected)) {
			// We use the first column for the structure (from molfile) the three more columns for the data.
			// This example also uses a custom template with predefined view and filter settings.
			dwInterface.initializeData(4, TABLE3.length, "Molfile Example");
			dwInterface.setColumnTitle(0,"Structure");
			dwInterface.setColumnTitle(1,"ID");
			dwInterface.setColumnTitle(2,"Animal");
			dwInterface.setColumnTitle(3,"Size");
			dwInterface.setColumnType(0, IPluginHelper.COLUMN_TYPE_STRUCTURE_FROM_MOLFILE);
			int row = 0;
			for (String[] rowData:TABLE3) {
				// we pass a molfile to be converted into a structure
				dwInterface.setCellData(0, row, MOLFILE[row]);

				int column = 1;
				for (String value:rowData)
					dwInterface.setCellData(column++, row, value);
				row++;
			}
			dwInterface.finalizeData(TEMPLATE);
		}
	}

	String[] MOLFILE = { "\n" +
			"Actelion Java MolfileCreator 1.0\n" +
			"\n" +
			"  8  8  0  0  0  0  0  0  0  0999 V2000\n" +
			"    0.0000   -0.0000   -0.0000 Br  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"    1.4673   -0.1526   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"    2.3477    1.0800   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"    1.8664    2.5238   -0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"    3.8268    1.0917   -0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"    3.0638    3.4159   -0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"    4.2846    2.5355   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"    5.7050    3.0051   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"  1  2  1  0  0  0  0\n" +
			"  2  3  1  0  0  0  0\n" +
			"  3  4  2  0  0  0  0\n" +
			"  3  5  1  0  0  0  0\n" +
			"  4  6  1  0  0  0  0\n" +
			"  5  7  2  0  0  0  0\n" +
			"  7  8  1  0  0  0  0\n" +
			"  6  7  1  0  0  0  0\n" +
			"M  END\n", "\n" +
			"Actelion Java MolfileCreator 1.0\n" +
			"\n" +
			"  8  8  0  0  0  0  0  0  0  0999 V2000\n" +
			"    0.0000   -0.0000   -0.0000 S   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"    1.2036    0.8881   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   -1.1919    0.8881   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"    0.7245    2.3254   -0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"    2.6175    0.4324   -0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   -0.7596    2.3254   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   -1.6593    3.5641   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   -3.1551    3.4355   -0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"  1  2  1  0  0  0  0\n" +
			"  1  3  1  0  0  0  0\n" +
			"  2  4  2  0  0  0  0\n" +
			"  2  5  1  0  0  0  0\n" +
			"  3  6  2  0  0  0  0\n" +
			"  6  7  1  0  0  0  0\n" +
			"  7  8  1  0  0  0  0\n" +
			"  4  6  1  0  0  0  0\n" +
			"M  END\n", "\n" +
			"Actelion Java MolfileCreator 1.0\n" +
			"\n" +
			"  8  8  0  0  0  0  0  0  0  0999 V2000\n" +
			"    0.0000   -0.0000   -0.0000 S   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"    0.4590    1.4359   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   -1.5065    0.0118   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   -0.7650    2.3186   -0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"    1.8831    1.8361   -0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   -1.9655    1.4477   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   -2.2715   -1.2711   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   -3.4249    1.8478   -0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"  1  2  1  0  0  0  0\n" +
			"  1  3  1  0  0  0  0\n" +
			"  2  4  2  0  0  0  0\n" +
			"  2  5  1  0  0  0  0\n" +
			"  3  6  1  0  0  0  0\n" +
			"  3  7  1  1  0  0  0\n" +
			"  6  8  2  0  0  0  0\n" +
			"  4  6  1  0  0  0  0\n" +
			"M  END\n", "\n" +
			"Actelion Java MolfileCreator 1.0\n" +
			"\n" +
			"  8  8  0  0  0  0  0  0  0  0999 V2000\n" +
			"    0.0000   -0.0000   -0.0000 S   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"    0.4587    1.4348   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   -1.5054    0.0118   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   -0.7644    2.3286   -0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"    1.8817    1.9170   -0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   -1.9640    1.4466   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   -2.3992   -1.1878   -0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   -1.7994   -2.5403   -0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"  1  2  1  0  0  0  0\n" +
			"  1  3  1  0  0  0  0\n" +
			"  2  4  2  0  0  0  0\n" +
			"  2  5  1  0  0  0  0\n" +
			"  3  6  2  0  0  0  0\n" +
			"  3  7  1  0  0  0  0\n" +
			"  7  8  2  0  0  0  0\n" +
			"  4  6  1  0  0  0  0\n" +
			"M  END\n" };

	String TEMPLATE = "<datawarrior properties>\n" +
				"<axisColumn_Animal Sizes_0=\"Animal\">\n" +
				"<axisColumn_Animal Sizes_1=\"Size\">\n" +
				"<axisColumn_Size Histogram_0=\"Animal\">\n" +
				"<axisColumn_Size Histogram_1=\"<unassigned>\">\n" +
				"<background_Animal Sizes=\"-1383\">\n" +
				"<background_Size Histogram=\"-7864339\">\n" +
				"<chartColumn_Size Histogram=\"Size\">\n" +
				"<chartMode_Size Histogram=\"sum\">\n" +
				"<chartType_Animal Sizes=\"scatter\">\n" +
				"<chartType_Size Histogram=\"bars\">\n" +
				"<colorColumn_Size Histogram=\"Animal\">\n" +
				"<colorCount_Size Histogram=\"2\">\n" +
				"<colorListMode_Size Histogram=\"Categories\">\n" +
				"<color_Size Histogram_0=\"-11992833\">\n" +
				"<color_Size Histogram_1=\"-65494\">\n" +
				"<columnWidth_Table_Animal=\"79\">\n" +
				"<columnWidth_Table_ID=\"79\">\n" +
				"<columnWidth_Table_Size=\"79\">\n" +
				"<columnWidth_Table_Structure=\"99\">\n" +
				"<defaultColor_Animal Sizes=\"-8056785\">\n" +
				"<detailView=\"height[Data]=0.5;height[Structure]=0.5\">\n" +
				"<filter0=\"#structure#\tStructure\">\n" +
				"<filter1=\"#category#\tAnimal\">\n" +
				"<filter2=\"#string#\tID\">\n" +
				"<filter3=\"#double#\tSize\">\n" +
				"<fontSize_Animal Sizes=\"3.0\">\n" +
				"<fontSize_Size Histogram=\"2.2\">\n" +
				"<labelColumn_Animal Sizes_midCenter=\"Structure\">\n" +
				"<labelSize_Animal Sizes=\"2.0\">\n" +
				"<mainSplitting=\"0.71257\">\n" +
				"<mainView=\"Size Histogram\">\n" +
				"<mainViewCount=\"3\">\n" +
				"<mainViewDockInfo0=\"root\">\n" +
				"<mainViewDockInfo1=\"Table\tbottom\t0.466\">\n" +
				"<mainViewDockInfo2=\"Size Histogram\tright\t0.5\">\n" +
				"<mainViewName0=\"Table\">\n" +
				"<mainViewName1=\"Size Histogram\">\n" +
				"<mainViewName2=\"Animal Sizes\">\n" +
				"<mainViewType0=\"tableView\">\n" +
				"<mainViewType1=\"2Dview\">\n" +
				"<mainViewType2=\"2Dview\">\n" +
				"<markersize_Size Histogram=\"1.21\">\n" +
				"<rightSplitting=\"0.57455\">\n" +
				"<rowHeight_Table=\"80\">\n" +
				"<showNaNValues_Animal Sizes=\"true\">\n" +
				"<showNaNValues_Size Histogram=\"true\">\n" +
				"<sizeColumn_Size Histogram=\"Size\">\n" +
				"<suppressGrid_Animal Sizes=\"true\">\n" +
				"<suppressGrid_Size Histogram=\"true\">\n" +
				"</datawarrior properties>";
}
