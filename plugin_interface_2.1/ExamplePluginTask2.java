import org.openmolecules.datawarrior.plugin.IChemistryPanel;
import org.openmolecules.datawarrior.plugin.IPluginHelper;
import org.openmolecules.datawarrior.plugin.IPluginTask;
import org.openmolecules.datawarrior.plugin.IUserInterfaceHelper;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Properties;
import javax.swing.*;

/**
 * PluginTask with a dialog that embeds a structure editor.
 */
public class ExamplePluginTask2 implements ActionListener,IPluginTask {
	private static final String CONFIGURATION_QUERY_TYPE = "queryType";
	private static final String CONFIGURATION_QUERY_STRUCTURE = "queryStructure";
	private static final String CONFIGURATION_SIMILARITY_LIMIT = "similarityLimit";

	private static final String[] SEARCH_TYPE_OPTIONS = {"Superstructures", "Similar structures"};
	private static final String[] QUERY_TYPE_CODE = {"sss", "sim"};
	private static final int QUERY_TYPE_SUBSTRUCTURE = 0;
	private static final int QUERY_TYPE_SIMILARITY = 1;

	private JComboBox<String> mComboBox;
	private IChemistryPanel mChemistryPanel;
	private JSlider mSimilaritySlider;

	@Override
	public String getTaskCode() {
		return "OpenMoleculesExample001";
	}

	@Override
	public String getTaskName() {
		return "Plugin Example: Structure Search";
	}

	/**
	 * This method expects a JPanel with all UI-elements for defining a database query.
	 * These may include elements to define a structure search and/or alphanumerical
	 * search criteria. 'Cancel' and 'OK' buttons are provided outside of this panel.
	 * @param dialogHelper gives access to a chemistry panel to let the user draw a chemical (sub-)structure
	 * @return
	 */
	@Override
	public JComponent createDialogContent(IUserInterfaceHelper dialogHelper) {
		mComboBox = new JComboBox<String>(SEARCH_TYPE_OPTIONS);
		mComboBox.setSelectedIndex(0);
		mComboBox.addActionListener(this);

		JPanel panel1 = new JPanel();
		panel1.add(new JLabel("Search type:"));
		panel1.add(mComboBox);

		int width = (int) (dialogHelper.getUIScaleFactor() * 160);
		int height = (int) (dialogHelper.getUIScaleFactor() * 116);
		mChemistryPanel = dialogHelper.getChemicalEditor();
		mChemistryPanel.setMode(IChemistryPanel.MODE_FRAGMENT);
		((JComponent) mChemistryPanel).setPreferredSize(new Dimension(width, height));

		JPanel panel2 = new JPanel();
		panel2.add(createSimilaritySlider(dialogHelper, height));
		panel2.add((JComponent) mChemistryPanel);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(panel1, BorderLayout.NORTH);
		panel.add(panel2, BorderLayout.CENTER);

		return panel;
	}

	private JComponent createSimilaritySlider(IUserInterfaceHelper dialogHelper, int height) {
		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		labels.put(new Integer(70), new JLabel("70%"));
		labels.put(new Integer(80), new JLabel("80%"));
		labels.put(new Integer(90), new JLabel("90%"));
		labels.put(new Integer(100), new JLabel("100%"));
		mSimilaritySlider = new JSlider(JSlider.VERTICAL, 70, 100, 85);
		mSimilaritySlider.setMinorTickSpacing(1);
		mSimilaritySlider.setMajorTickSpacing(10);
		mSimilaritySlider.setLabelTable(labels);
		mSimilaritySlider.setPaintLabels(true);
		mSimilaritySlider.setPaintTicks(true);
		int width = mSimilaritySlider.getPreferredSize().width;
		mSimilaritySlider.setMinimumSize(new Dimension(width, height));
		mSimilaritySlider.setPreferredSize(new Dimension(width, height));
		mSimilaritySlider.setEnabled(false);
		JPanel spanel = new JPanel();
		spanel.add(mSimilaritySlider);
		spanel.setBorder(BorderFactory.createTitledBorder("Similarity"));
		return spanel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBox) {
			boolean isSubstructureSearch = (mComboBox.getSelectedIndex() == QUERY_TYPE_SUBSTRUCTURE);
			mSimilaritySlider.setEnabled(!isSubstructureSearch);
			mChemistryPanel.setMode(isSubstructureSearch ? IChemistryPanel.MODE_FRAGMENT : IChemistryPanel.MODE_MOLECULE);
		}
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
	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(CONFIGURATION_QUERY_TYPE, QUERY_TYPE_CODE[mComboBox.getSelectedIndex()]);
		if (!mChemistryPanel.isEmptyMolecule())
			configuration.setProperty(CONFIGURATION_QUERY_STRUCTURE, mChemistryPanel.getMoleculeAsMolfileV2());
		if (mComboBox.getSelectedIndex() == QUERY_TYPE_SIMILARITY)
			configuration.setProperty(CONFIGURATION_SIMILARITY_LIMIT, Integer.toString(mSimilaritySlider.getValue()));
		return configuration;
	}

	/**
	 * This method populates an empty database query dialog with a previously configured database query.
	 * @param configuration
	 */
	@Override
	public void setDialogConfiguration(Properties configuration) {
		boolean isSubstructureQuery = QUERY_TYPE_CODE[0].equals(configuration.getProperty(CONFIGURATION_QUERY_TYPE));
		mComboBox.setSelectedIndex(isSubstructureQuery ? QUERY_TYPE_SUBSTRUCTURE : QUERY_TYPE_SIMILARITY);
		String molfile = configuration.getProperty(CONFIGURATION_QUERY_STRUCTURE);
		if (molfile != null)
			mChemistryPanel.setMoleculeFromMolfile(molfile);
		mChemistryPanel.setMode(isSubstructureQuery ? IChemistryPanel.MODE_FRAGMENT : IChemistryPanel.MODE_MOLECULE);
		mSimilaritySlider.setEnabled(!isSubstructureQuery);
		if (!isSubstructureQuery)
			mSimilaritySlider.setValue(Integer.parseInt(configuration.getProperty(CONFIGURATION_SIMILARITY_LIMIT, "85")));

	}

	/**
	 * Checks, whether the given database query configuration is a valid one.
	 * If not, the this method should return a short and clear error message
	 * intended for the user in order to correct the dialog setting.
	 * @param configuration
	 * @return user-interpretable error message or null, if query configuration is valid
	 */
	@Override
	public String checkConfiguration(Properties configuration) {
		if (configuration.getProperty(CONFIGURATION_QUERY_STRUCTURE) == null) {
			boolean isSubstructureQuery = QUERY_TYPE_CODE[0].equals(configuration.getProperty(CONFIGURATION_QUERY_TYPE));
			return "You need to draw a " + (isSubstructureQuery ? "sub-structure." : "molecule.");
		}
		return null;    // perfectly defined query
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
	@Override
	public void run(Properties configuration, IPluginHelper dwInterface) {
		boolean isSubstructureQuery = QUERY_TYPE_CODE[0].equals(configuration.getProperty(CONFIGURATION_QUERY_TYPE));
		String queryMolfile = configuration.getProperty(CONFIGURATION_QUERY_STRUCTURE);
		int similarityLimit = isSubstructureQuery ? 0 : Integer.parseInt(configuration.getProperty(CONFIGURATION_SIMILARITY_LIMIT, "85"));

		// Now one would send this information to a server and get some result back...
		// If the server returns a result table, then the following code illustrates
		// how to create a new DataWarrior window and how to populate its data table
		// with the result data from the server.

		// We use the first column for the structure (from molfile) and one more column for an artificial ID.
		// This example also uses a custom template with predefined view and filter settings.
		dwInterface.initializeData(2, RESULT_MOLFILES.length, "Molfile Example");
		dwInterface.setColumnTitle(0, "Structure");
		dwInterface.setColumnTitle(1, "ID");

		// We assume that we get molfiles from the server. If we get SMILES or IDCodes instead,
		// we would have to use a different column type here.
		dwInterface.setColumnType(0, IPluginHelper.COLUMN_TYPE_STRUCTURE_FROM_MOLFILE);

		int row = 0;
		for (String molfile : RESULT_MOLFILES) {
			// For demonstration purposes we just use dummy molfiles and structure-IDs
			dwInterface.setCellData(0, row, RESULT_MOLFILES[row]);
			dwInterface.setCellData(1, row, "ID-"+(++row));
		}

		// This creates default views (one 2D-view and one 3D-view) and filters.
		// For creating custom views and filters pass a template as String as it is
		// written into a template file (.dwat).
		dwInterface.finalizeData(null);
	}

	String[] RESULT_MOLFILES = {"\n" +
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
			"M  END\n"};
}