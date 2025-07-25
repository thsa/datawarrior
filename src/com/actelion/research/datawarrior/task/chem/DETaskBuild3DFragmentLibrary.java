package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.chem.IDCodeParserWithoutCoordinateInvention;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.shredder.Fragment3D;
import com.actelion.research.chem.shredder.Fragmenter3D;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.ArrayUtils;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

public class DETaskBuild3DFragmentLibrary extends ConfigurableTask {
	public static final String TASK_NAME = "Build 3D-Fragment Library";

	private static final int MIN_EXIT_VECTORS = 2;
	public static final int MAX_EXIT_VECTORS = 5;

	private static final String PROPERTY_3D_COORDS_COLUMN = "coords3DColumn";
	private static final String PROPERTY_ID_COLUMN = "idColumn";
	private static final String PROPERTY_MAX_BOND_FLEXIBILITY_SUM = "maxBondFlexibilitySum";
	private static final String PROPERTY_MIN_FRAGMENT_ATOMS = "minFragmentAtoms";
	private static final String PROPERTY_MAX_FRAGMENT_ATOMS = "maxFragmentAtoms";
	private static final String PROPERTY_MIN_EXIT_VECTORS = "minExitVectors";
	private static final String PROPERTY_MAX_EXIT_VECTORS = "maxExitVectors";

	private static final String FRAGMENT_COLUMN_NAME = "Fragment";

	private final DataWarrior mApplication;
	private DEFrame mTargetFrame;
	private final CompoundTableModel mTableModel;
	private JComboBox<String> mComboBoxStructureColumn,mComboBoxSourceID;
	private JTextField mTextFieldMaxBondFlexibilitySum,mTextFieldMinAtoms,mTextFieldMaxAtoms,mTextFieldMaxExits,mTextFieldMinExits;

	public DETaskBuild3DFragmentLibrary(DEFrame parent) {
		super(parent, true);
		mApplication = parent.getApplication();
		mTableModel = parent.getTableModel();
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
	}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode) == null) {
			showErrorMessage("No column with chemical structures found.");
			return false;
			}
		if (mTableModel.getSpecialColumnList(CompoundTableModel.cColumnType3DCoordinates) == null) {
			showErrorMessage("No structure column with 3-dimensional atom coordinates found.\nYou may generate conformers or get 3D-structures from the COD database.");
			return false;
			}

		return true;
		}

	@Override
	public JPanel createDialogContent() {
		JPanel content = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED,
							2*gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap>>1, TableLayout.PREFERRED,
							 gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap>>1, TableLayout.PREFERRED, gap} };
		content.setLayout(new TableLayout(size));

		int[] idcodeColumn = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnType3DCoordinates);
		mComboBoxStructureColumn = new JComboBox<>();
		if (idcodeColumn != null)
			for (int column:idcodeColumn)
				mComboBoxStructureColumn.addItem(mTableModel.getColumnTitle(column));

		mComboBoxStructureColumn.setEditable(!isInteractive());
		content.add(new JLabel("3D-structure column:"), "1,1");
		content.add(mComboBoxStructureColumn, "3,1");

		mComboBoxSourceID = new JComboBox<>();
		mComboBoxSourceID.addItem("<none>");
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.getColumnSpecialType(column) == null && !mTableModel.isMultiEntryColumn(column))
				mComboBoxSourceID.addItem(mTableModel.getColumnTitle(column));
		content.add(new JLabel("Compound-ID colum:"), "1,3");
		content.add(mComboBoxSourceID, "3,3");

		content.add(new JLabel("3D-Fragment properties"), "1,5");

		content.add(new JLabel("Minimum non-hydrogen atoms:"), "1,7");
		mTextFieldMinAtoms = new JTextField(6);
		content.add(mTextFieldMinAtoms, "3,7");

		content.add(new JLabel("Maximum non-hydrogen atoms:"), "1,9");
		mTextFieldMaxAtoms = new JTextField(6);
		content.add(mTextFieldMaxAtoms, "3,9");

		content.add(new JLabel("Maximum bond flexibility sum:"), "1,11");
		mTextFieldMaxBondFlexibilitySum = new JTextField(6);
		content.add(mTextFieldMaxBondFlexibilitySum, "3,11");

		content.add(new JLabel("Minimum exit vectors:"), "1,13");
		mTextFieldMinExits = new JTextField(6);
		content.add(mTextFieldMinExits, "3,13");

		content.add(new JLabel("Maximum exit vectors:"), "1,15");
		mTextFieldMaxExits = new JTextField(6);
		content.add(mTextFieldMaxExits, "3,15");

		return content;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/conformers.html#FragmentLib";
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_3D_COORDS_COLUMN, mTableModel.getColumnTitleNoAlias((String)mComboBoxStructureColumn.getSelectedItem()));
		if (mComboBoxSourceID.getSelectedIndex() != 0)
			configuration.setProperty(PROPERTY_ID_COLUMN, mTableModel.getColumnTitleNoAlias((String)mComboBoxSourceID.getSelectedItem()));
		configuration.setProperty(PROPERTY_MIN_FRAGMENT_ATOMS, mTextFieldMinAtoms.getText());
		configuration.setProperty(PROPERTY_MAX_FRAGMENT_ATOMS, mTextFieldMaxAtoms.getText());
		configuration.setProperty(PROPERTY_MAX_BOND_FLEXIBILITY_SUM, mTextFieldMaxBondFlexibilitySum.getText());
		configuration.setProperty(PROPERTY_MIN_EXIT_VECTORS, mTextFieldMinExits.getText());
		configuration.setProperty(PROPERTY_MAX_EXIT_VECTORS, mTextFieldMaxExits.getText());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String structureColumn = configuration.getProperty(PROPERTY_3D_COORDS_COLUMN, "");
		if (!structureColumn.isEmpty()) {
			int column = mTableModel.findColumn(structureColumn);
			if (column != -1 && CompoundTableConstants.cColumnType3DCoordinates.equals(mTableModel.getColumnSpecialType(column)))
				mComboBoxStructureColumn.setSelectedItem(mTableModel.getColumnTitle(column));
			else if (!isInteractive())
				mComboBoxStructureColumn.setSelectedItem(structureColumn);
			else if (mComboBoxStructureColumn.getItemCount() != 0)
				mComboBoxStructureColumn.setSelectedIndex(0);
			}
		else if (!isInteractive()) {
			mComboBoxStructureColumn.setSelectedItem(CompoundTableConstants.cColumnType3DCoordinates);
			}
		String idColumn = configuration.getProperty(PROPERTY_ID_COLUMN, "");
		if (!idColumn.isEmpty()) {
			int column = mTableModel.findColumn(idColumn);
			if (column != -1 && mTableModel.getColumnSpecialType(column) == null && !mTableModel.isMultiEntryColumn(column))
				mComboBoxSourceID.setSelectedItem(mTableModel.getColumnTitle(column));
			else
				mComboBoxSourceID.setSelectedIndex(0);
		}
		else {
			mComboBoxSourceID.setSelectedIndex(0);
		}

		mTextFieldMinAtoms.setText(configuration.getProperty(PROPERTY_MIN_FRAGMENT_ATOMS, ""));
		mTextFieldMaxAtoms.setText(configuration.getProperty(PROPERTY_MAX_FRAGMENT_ATOMS, ""));
		mTextFieldMaxBondFlexibilitySum.setText(configuration.getProperty(PROPERTY_MAX_BOND_FLEXIBILITY_SUM, ""));
		mTextFieldMinExits.setText(configuration.getProperty(PROPERTY_MIN_EXIT_VECTORS, ""));
		mTextFieldMaxExits.setText(configuration.getProperty(PROPERTY_MAX_EXIT_VECTORS, ""));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxStructureColumn.getItemCount() != 0)
			mComboBoxStructureColumn.setSelectedIndex(0);
		else if (!isInteractive())
			mComboBoxStructureColumn.setSelectedItem(CompoundTableConstants.cColumnType3DCoordinates);
		mComboBoxSourceID.setSelectedIndex(0);
		mTextFieldMinAtoms.setText("5");
		mTextFieldMaxAtoms.setText("15");
		mTextFieldMaxBondFlexibilitySum.setText("1.0");
		mTextFieldMinExits.setText("2");
		mTextFieldMaxExits.setText("4");
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String structureColumn = configuration.getProperty(PROPERTY_3D_COORDS_COLUMN, "");
		if (structureColumn.isEmpty()) {
			showErrorMessage("No 3D-structure column defined.");
			return false;
			}
		if (isLive) {
			int column = mTableModel.findColumn(structureColumn);
			if (column == -1) {
				showErrorMessage("Structure column '"+structureColumn+"' not found.");
				return false;
				}
			if (!CompoundTableConstants.cColumnType3DCoordinates.equals(mTableModel.getColumnSpecialType(column))) {
				showErrorMessage("Column '"+structureColumn+"' does not contain 3D-structures.");
				return false;
				}
			}

		int minAtoms = 0;
		try {
			minAtoms = Integer.parseInt(configuration.getProperty(PROPERTY_MIN_FRAGMENT_ATOMS, ""));
			if (minAtoms < 4) {
				showErrorMessage("Value for minimum fragment atoms must be larger than 3.");
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Value for minimum fragment atom count is not numerical.");
			return false;
			}

		try {
			int maxAtoms = Integer.parseInt(configuration.getProperty(PROPERTY_MAX_FRAGMENT_ATOMS, ""));
			if (maxAtoms < minAtoms) {
				showErrorMessage("Value for maximum fragment atoms must not be smaller than the minimum.");
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Value for maximum fragment atom count is not numerical.");
			return false;
			}

		try {
			float maxBondFlexibilitySum = Float.parseFloat(configuration.getProperty(PROPERTY_MAX_BOND_FLEXIBILITY_SUM, ""));
			if (maxBondFlexibilitySum < 0 || maxBondFlexibilitySum > 5) {
				showErrorMessage("A meaningful bond flexibility sum should be (much) smaller than 5.");
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("The maximum sum of bond flexibilities is not numerical.");
			return false;
			}

		int minExits = 0;
		try {
			minExits = Integer.parseInt(configuration.getProperty(PROPERTY_MIN_EXIT_VECTORS, ""));
			if (minExits < 2) {
				showErrorMessage("Value for minimum exit vectors cannot be smaller than "+MIN_EXIT_VECTORS+".");
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Value for minimum exit vectors is not numerical.");
			return false;
			}

		try {
			int maxExits = Integer.parseInt(configuration.getProperty(PROPERTY_MAX_EXIT_VECTORS, ""));
			if (maxExits < minExits || maxExits > MAX_EXIT_VECTORS) {
				showErrorMessage("Maximum exit vector value must not be smaller than the minimum nor larger than "+MAX_EXIT_VECTORS);
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Value for maximum exit vectors is not numerical.");
			return false;
		}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		int coordsColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_3D_COORDS_COLUMN));
		int idcodeColumn = mTableModel.getParentColumn(coordsColumn);

		int idColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_ID_COLUMN));
		if (idColumn != -1 && (mTableModel.getColumnSpecialType(idColumn) != null || mTableModel.isMultiEntryColumn(idColumn)))
			idColumn = -1;
		int _idColumn = idColumn;

		int minAtoms = Integer.parseInt(configuration.getProperty(DETaskBuild3DFragmentLibrary.PROPERTY_MIN_FRAGMENT_ATOMS));
		int maxAtoms = Integer.parseInt(configuration.getProperty(DETaskBuild3DFragmentLibrary.PROPERTY_MAX_FRAGMENT_ATOMS));
		float maxBondFlexibilitySum = Float.parseFloat(configuration.getProperty(DETaskBuild3DFragmentLibrary.PROPERTY_MAX_BOND_FLEXIBILITY_SUM));
		int minExits = Integer.parseInt(configuration.getProperty(DETaskBuild3DFragmentLibrary.PROPERTY_MIN_EXIT_VECTORS));
		int maxExits = Integer.parseInt(configuration.getProperty(DETaskBuild3DFragmentLibrary.PROPERTY_MAX_EXIT_VECTORS));

		final int totalRowCount = mTableModel.getTotalRowCount();
		final AtomicInteger remaining = new AtomicInteger(totalRowCount);
		final ConcurrentSkipListSet<Fragment3D> fragmentSet = new ConcurrentSkipListSet<>();

		startProgress("Processing molecules...", 0, totalRowCount);

		int threadCount = Math.min(totalRowCount, Runtime.getRuntime().availableProcessors());
		Thread[] thread = new Thread[threadCount];
		for (int i=0; i<threadCount; i++) {
			thread[i] = new Thread("3D-Fragment Generator "+(i+1)) {
				public void run() {
					Fragmenter3D fragmenter = new Fragmenter3D(minAtoms, maxAtoms, maxBondFlexibilitySum, minExits, maxExits);
					StereoMolecule mol = new StereoMolecule();
					IDCodeParserWithoutCoordinateInvention parser = new IDCodeParserWithoutCoordinateInvention();

					int m = remaining.decrementAndGet();
					while (m >= 0 && !threadMustDie()) {
						int row = totalRowCount - m - 1;
						updateProgress(row);
						CompoundRecord record = mTableModel.getTotalRecord(row);
						byte[] idcode = (byte[])record.getData(idcodeColumn);
						byte[] coords = (byte[])record.getData(coordsColumn);
						if (idcode != null && coords != null) {
							String molID = (_idColumn == -1 || record.getData(_idColumn) == null ? null : new String((byte[])record.getData(_idColumn)));
							try {
								int coordsIndex = -1;
								while (true) {
									parser.parse(mol, idcode, coords, 0, coordsIndex+1);
									coordsIndex = ArrayUtils.indexOf(coords, (byte)' ', coordsIndex+1);
									if (mol.getAllAtoms() != 0)
										fragmentSet.addAll(fragmenter.buildFragments(mol, molID, true));
									if (coordsIndex == -1)
										break;
									}
								}
							catch (Exception e) {
								e.printStackTrace();
								}
							}
						m = remaining.decrementAndGet();
						}
					}
				};
			}

		for (Thread t:thread)
			t.start();

		for (Thread t:thread)
			try { t.join(); } catch (InterruptedException ie) {}

		if (!fragmentSet.isEmpty()) {
			mTargetFrame = mApplication.getEmptyFrame("3D-Fragments");

			CompoundTableModel tableModel = mTargetFrame.getTableModel();
			tableModel.initializeTable(fragmentSet.size(), 4+(idColumn == -1 ? 0 : 1));
			tableModel.prepareStructureColumns(0, FRAGMENT_COLUMN_NAME, false, true);

			tableModel.setColumnName(CompoundTableConstants.cColumnType3DCoordinates, 2);
			tableModel.setColumnProperty(2, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType3DCoordinates);
			tableModel.setColumnProperty(2, CompoundTableConstants.cColumnPropertyParentColumn, FRAGMENT_COLUMN_NAME);

			tableModel.setColumnName("Exit Vectors", 3);
			if (idColumn != -1)
				tableModel.setColumnName("Source Compound-ID", 4);

			int row = 0;
			for (Fragment3D fragment : fragmentSet) {
				tableModel.setTotalValueAt(fragment.getIDCode(), row, 0);
				tableModel.setTotalValueAt(fragment.getIDCoordinates(), row, 2);
				tableModel.setTotalValueAt(Integer.toString(fragment.getExitAtoms().length), row, 3);
				if (idColumn != -1)
					tableModel.setTotalValueAt(fragment.getSourceID(), row, 4);
				row++;
			}

			tableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, this);

			SwingUtilities.invokeLater(() -> {
				mTargetFrame.getMainFrame().setMainSplitting(0.6);
				mTargetFrame.getMainFrame().setRightSplitting(0.5);
				mTargetFrame.getMainFrame().getDetailPane().setProperties("height[Data]=0.0;height[Fragment]=0.0;height[3D-Fragment]=1.0");
				DEMainPane mainPane = mTargetFrame.getMainFrame().getMainPane();
				mainPane.addStructureView("3D-Fragments", null, 0);
				mainPane.getTable().getColumnModel().getColumn(0).setPreferredWidth(HiDPIHelper.scale(160));
				try {
					mTargetFrame.getMainFrame().getPruningPanel().addStructureFilter(tableModel, 0, null, null);
					mTargetFrame.getMainFrame().getPruningPanel().addCategoryFilter(tableModel, 3);
					}
				catch (Exception e) {}
				} );
			}
		}
	}
