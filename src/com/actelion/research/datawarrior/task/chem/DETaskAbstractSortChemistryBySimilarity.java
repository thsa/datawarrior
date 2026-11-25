package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorHandler;
import com.actelion.research.chem.descriptor.DescriptorHandlerStandardFactory;
import com.actelion.research.chem.descriptor.DescriptorInfo;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.JEditableStructureView;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import static com.actelion.research.chem.descriptor.DescriptorConstants.DESCRIPTOR_Flexophore;
import static com.actelion.research.chem.descriptor.DescriptorConstants.DESCRIPTOR_LIST;

/**
 * Created by thomas on 9/22/16.
 */
public abstract class DETaskAbstractSortChemistryBySimilarity extends ConfigurableTask implements ActionListener {
	private static final String PROPERTY_CHEMISTRY_COLUMN = "structureColumn";
	private static final String PROPERTY_IDCODE = "idcode";
	private static final String PROPERTY_IDCOORDINATES = "idcoords";
	private static final String PROPERTY_DESCRIPTOR_TYPE = "descriptorType";
	private static final String PROPERTY_DESCRIPTOR = "descriptor";

	protected static final String PROPERTY_REACTION_PART = "reactionPart";

	private final CompoundTableModel mTableModel;
	private final CompoundRecord mRecord;
	private final int mDescriptorColumn;
	private JComboBox<String> mComboBoxStructureColumn,mComboBoxDescriptorType;
	private JEditableStructureView mStructureView;

	public DETaskAbstractSortChemistryBySimilarity(DEFrame parent, int descriptorColumn, CompoundRecord record) {
		super(parent, record == null);
		mTableModel = parent.getTableModel();
		mDescriptorColumn = descriptorColumn;
		mRecord = record;
		}

	protected abstract String getChemistryName();
	protected abstract String getParentColumnType();
	protected abstract JComboBox<String> getComboBoxReactionPart();

	@Override
	public Properties getPredefinedConfiguration() {
		if (mRecord != null && mDescriptorColumn != -1) {
			Properties configuration = new Properties();
			int structureColumn = mTableModel.getParentColumn(mDescriptorColumn);
			int coordinateColumn = mTableModel.getChildColumn(structureColumn, CompoundTableConstants.cColumnType2DCoordinates);
			configuration.setProperty(PROPERTY_CHEMISTRY_COLUMN, mTableModel.getColumnTitleNoAlias(structureColumn));
			configuration.setProperty(PROPERTY_IDCODE, mTableModel.getValue(mRecord, structureColumn));
			if (coordinateColumn != -1)
				configuration.setProperty(PROPERTY_IDCOORDINATES, mTableModel.getValue(mRecord, coordinateColumn));
			configuration.setProperty(PROPERTY_DESCRIPTOR_TYPE, mTableModel.getDescriptorHandler(mDescriptorColumn).getInfo().shortName);

			Object descriptor = mRecord.getData(mDescriptorColumn);
			if (descriptor != null)
				configuration.setProperty(PROPERTY_DESCRIPTOR, mTableModel.getDescriptorHandler(mDescriptorColumn).encode(descriptor));
			return configuration;
			}

		return null;
		}

	@Override
	public JComponent createDialogContent() {
		JComboBox<String> comboBoxReactionPart = getComboBoxReactionPart();

		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED,
							comboBoxReactionPart == null ? 0 : gap, comboBoxReactionPart == null ? 0 : TableLayout.PREFERRED,
							gap, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap, HiDPIHelper.scale(320), gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		int[] structureColumn = getStructureColumnList();

		mComboBoxStructureColumn = new JComboBox<>();
		if (structureColumn != null)
			for (int column : structureColumn)
				mComboBoxStructureColumn.addItem(mTableModel.getColumnTitle(column));
		content.add(new JLabel(getChemistryName()+" column:"), "1,1");
		content.add(mComboBoxStructureColumn, "3,1");
		mComboBoxStructureColumn.setEditable(!isInteractive());
		if (isInteractive())
			mComboBoxStructureColumn.addActionListener(this);

		if (comboBoxReactionPart != null) {
			content.add(new JLabel("Reaction part:"), "1,3");
			content.add(comboBoxReactionPart, "3,3");
			}

		mComboBoxDescriptorType = new JComboBox<>();
		populateComboBoxDescriptor(structureColumn == null? -1 : structureColumn[0],
								   comboBoxReactionPart == null ? null : (String)comboBoxReactionPart.getSelectedItem());
		content.add(new JLabel("Descriptor:"), "1,5");
		content.add(mComboBoxDescriptorType, "3,5");

		content.add(new JLabel("Reference structure for similarity:"), "1,7,3,7");
		mStructureView = new JEditableStructureView();
		mStructureView.setClipboardHandler(new ClipboardHandler());
		content.add(mStructureView, "1,9,3,9");

		return content;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String structureColumn = (String)mComboBoxStructureColumn.getSelectedItem();
		if (structureColumn != null)
			configuration.setProperty(PROPERTY_CHEMISTRY_COLUMN, structureColumn);

		String descriptorType = (String)mComboBoxDescriptorType.getSelectedItem();
		if (descriptorType != null)
			configuration.setProperty(PROPERTY_DESCRIPTOR_TYPE, descriptorType);

		StereoMolecule mol = mStructureView.getMolecule();
		if (mol != null && mol.getAllAtoms() != 0) {
			Canonizer canonizer = new Canonizer(mol);
			configuration.setProperty(PROPERTY_IDCODE, canonizer.getIDCode());
			configuration.setProperty(PROPERTY_IDCOORDINATES, canonizer.getEncodedCoordinates());
			if (descriptorType != null) {
				DescriptorHandler dh = DescriptorHandlerStandardFactory.getFactory().create(descriptorType);
				Object descriptor = dh.createDescriptor(mol);
				configuration.setProperty(PROPERTY_DESCRIPTOR, dh.encode(descriptor));
				}
			}

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String structureColumnName = configuration.getProperty(PROPERTY_CHEMISTRY_COLUMN, "");
		String descriptorName = configuration.getProperty(PROPERTY_DESCRIPTOR_TYPE, "");
		if (!structureColumnName.isEmpty()) {
			int column = mTableModel.findColumn(structureColumnName);
			if (column != -1) {
				mComboBoxStructureColumn.setSelectedItem(mTableModel.getColumnTitle(column));

				if (!descriptorName.isEmpty()) {
					int descriptorColumn = mTableModel.getChildColumn(column, descriptorName);
					if (descriptorColumn != -1 || !isInteractive())
						mComboBoxDescriptorType.setSelectedItem(descriptorName);
					}
				}
			else if (!isInteractive()) {
				mComboBoxStructureColumn.setSelectedItem(structureColumnName);
				if (descriptorName != null)
					mComboBoxDescriptorType.setSelectedItem(descriptorName);
				}
			}
		else if (!isInteractive()) {
			mComboBoxStructureColumn.setSelectedItem("Structure");
			mComboBoxDescriptorType.setSelectedItem(descriptorName);
			}
		else if (mComboBoxStructureColumn.getItemCount() != 0) {
			mComboBoxStructureColumn.setSelectedIndex(0);
			}

		String idcode = configuration.getProperty(PROPERTY_IDCODE, "");
		if (idcode != null) {
			new IDCodeParser().parse(mStructureView.getMolecule(), idcode, configuration.getProperty(PROPERTY_IDCOORDINATES));
			mStructureView.structureChanged();
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxStructureColumn.getItemCount() != 0)
			mComboBoxStructureColumn.setSelectedIndex(0);
		else if (!isInteractive())
			mComboBoxStructureColumn.setSelectedItem(getChemistryName());
		if (mComboBoxDescriptorType.getItemCount() != 0)
			mComboBoxDescriptorType.setSelectedIndex(0);

		mStructureView.getMolecule().clear();
		mStructureView.structureChanged();
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxStructureColumn
		 || e.getSource() == getComboBoxReactionPart()) {
			String reactionPart = (getComboBoxReactionPart() == null) ? null : (String)getComboBoxReactionPart().getSelectedItem();
			populateComboBoxDescriptor(mTableModel.findColumn((String)mComboBoxStructureColumn.getSelectedItem()), reactionPart);
			}
		}

	private void populateComboBoxDescriptor(int structureColumn, String reactionPart) {
		mComboBoxDescriptorType.removeAllItems();
		if (!isInteractive()) {
			for (DescriptorInfo di:DESCRIPTOR_LIST)
				mComboBoxDescriptorType.addItem(di.shortName);
			}
		else if (structureColumn != -1) {
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				if (mTableModel.getParentColumn(column) == structureColumn
				 && mTableModel.isDescriptorColumn(column)
				 && (reactionPart == null || reactionPart.equals(mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyReactionPart))))
					mComboBoxDescriptorType.addItem(mTableModel.getDescriptorHandler(column).getInfo().shortName);
			}
		}

	private int[] getStructureColumnList() {
		int[] structureColumn = null;

		int[] idcodeColumn = mTableModel.getSpecialColumnList(getParentColumnType());
		if (idcodeColumn != null) {
			int count = 0;
			for (int column:idcodeColumn)
				if (hasDescriptorColumn(column))
					count++;

			if (count != 0) {
				structureColumn = new int[count];
				count = 0;
				for (int column:idcodeColumn)
					if (hasDescriptorColumn(column))
						structureColumn[count++] = column;
				}
			}

		return structureColumn;
		}

	private boolean hasDescriptorColumn(int parentColumn) {
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			if (mTableModel.isDescriptorColumn(column)
			 && mTableModel.getParentColumn(column) == parentColumn) {
				String reactionPart = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyReactionPart);
				if (reactionPart == null || !reactionPart.equals(CompoundTableConstants.cReactionPartReaction))
					return true;
				}
			}
		return false;
		}

	@Override
	public boolean isConfigurable() {
		int[] columnList = mTableModel.getSpecialColumnList(getParentColumnType());
		if (columnList != null)
			for (int column:columnList)
				if (hasDescriptorColumn(column))
					return true;

		return false;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String structureColumnName = configuration.getProperty(PROPERTY_CHEMISTRY_COLUMN, "");
		if (structureColumnName.isEmpty()) {
			showErrorMessage(getChemistryName()+" column not defined.");
			return false;
			}
		String idcode = configuration.getProperty(PROPERTY_IDCODE, "");
		if (idcode.isEmpty()) {
			showErrorMessage("Reference structure not defined.");
			return false;
			}
		String descriptorName = configuration.getProperty(PROPERTY_DESCRIPTOR_TYPE, "");
		if (descriptorName.isEmpty()) {
			showErrorMessage("Descriptor type not defined.");
			return false;
			}
		if (configuration.getProperty(PROPERTY_DESCRIPTOR, "").isEmpty()) {
			showErrorMessage("Missing descriptor.");
			return false;
			}

		if (isLive) {
			int structureColumn = mTableModel.findColumn(structureColumnName);
			if (structureColumn == -1) {
				showErrorMessage(getChemistryName()+" column '"+structureColumnName+"' not found.");
				return false;
				}
			int descriptorColumn = mTableModel.getChildColumn(structureColumn, descriptorName);
			if (descriptorColumn == -1) {
				showErrorMessage("Column '"+structureColumnName+"' has no '"+descriptorName+"' descriptor.");
				return false;
				}
			if (!mTableModel.isDescriptorAvailable(descriptorColumn)) {
				showErrorMessage("The calculation of the '"+descriptorName+"' descriptor of column '"+structureColumnName+"' hasn't finished yet.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		String idcode = configuration.getProperty(PROPERTY_IDCODE);
		String reactionPart = configuration.getProperty(PROPERTY_REACTION_PART);
		int chemistryColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_CHEMISTRY_COLUMN));
		int descriptorColumn = mTableModel.getChildColumn(chemistryColumn, configuration.getProperty(PROPERTY_DESCRIPTOR_TYPE), reactionPart);
		Object descriptor = mTableModel.getDescriptorHandler(descriptorColumn).decode(configuration.getProperty(PROPERTY_DESCRIPTOR));

		if (isInteractive()
		 && isSlowSimilarityCalculation(descriptorColumn)
		 && mTableModel.getStructureSimilarityListFromCache(idcode, descriptorColumn) == null) {
			// calculate the similarities in a background thread behind a progress dialog and sort when they are complete
			final JProgressDialog pd = new JProgressDialog(getParentFrame());
			new Thread(() -> {
				mTableModel.createSimilarityListSMP(null, descriptor, idcode, descriptorColumn, pd, true);
				float[] similarity = mTableModel.getSimilarityListSMP(false);
				SwingUtilities.invokeLater(() -> {
					pd.close();
					if (similarity != null)	// null if cancelled
						mTableModel.sortBySimilarity(similarity, descriptorColumn);
					} );
				}, "Similarity Sort").start();
			return;
			}

		float[] similarity = createSimilarityList(idcode, descriptor, descriptorColumn);
		if (similarity != null)	// null if cancelled
			mTableModel.sortBySimilarity(similarity, descriptorColumn);
		}

	private boolean isSlowSimilarityCalculation(int descriptorColumn) {
		return DESCRIPTOR_Flexophore.shortName.equals(mTableModel.getColumnSpecialType(descriptorColumn))
			|| mTableModel.getTotalRowCount() > 400000;
		}

	private float[] createSimilarityList(String idcode, Object descriptor, int descriptorColumn) {
		return isSlowSimilarityCalculation(descriptorColumn) ?

				// if we have the slow 3DPPMM2 then use a progress dialog
				createSimilarityListSMP(idcode, descriptor, descriptorColumn)

				// else calculate similarity list in current thread
				: mTableModel.createStructureSimilarityList(null, descriptor, descriptorColumn);
	}

	private float[] createSimilarityListSMP(String idcode, Object descriptor, int descriptorColumn) {
		float[] similarity = mTableModel.getStructureSimilarityListFromCache(idcode, descriptorColumn);
		if (similarity != null)
			return similarity;

		mTableModel.createSimilarityListSMP(null, descriptor, idcode, descriptorColumn, getProgressController(), true);
		return mTableModel.getSimilarityListSMP(false);
		}
	}
