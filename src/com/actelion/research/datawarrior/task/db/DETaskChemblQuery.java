/*
 * Copyright 2017 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.datawarrior.task.db;

import com.actelion.research.chem.StructureSearchSpecification;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandlerLongFFP512;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DELogWriter;
import com.actelion.research.datawarrior.DETable;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableDetailHandler;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.ByteArrayComparator;
import info.clearthought.layout.TableLayout;
import org.openmolecules.chembl.ChemblServerConstants;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;

public class DETaskChemblQuery extends DETaskStructureQuery implements ChemblServerConstants,ItemListener,KeyListener,ListSelectionListener {
	static final long serialVersionUID = 0x20061004;

	public static final String TASK_NAME = "Search ChEMBL Database";

    private static final String PROPERTY_TARGET_LIST = "targetList";
    private static final String PROPERTY_DOC_ID_LIST = "docIDList";
    private static final String PROPERTY_GROUP_STRUCTURES = "groupStructures";
    private static final String PROPERTY_FILTER = "filter";		// property only used to rebuild dialog, not for running task
    private static final String PROPERTY_FAMILY = "family";		// property only used to rebuild dialog, not for running task

	private static final String ITEM_ANY = "<any>";

	private static final String PUBMED_URL = "http://www.ncbi.nlm.nih.gov/pubmed/%s?dopt=Abstract";

	private static final String[] GROUP_COLUMNS = { COLUMN_TITLE_TYPE, COLUMN_TITLE_TARGET_ID };

	private static Target[]			sTarget;
	private static ProteinClass[]	sProteinClass;
	private static String           sDBVersion = "ChEMBL ??";

	private JTextField				mTextFieldFilter,mTextFieldDocIDs;
	private JList					mListTarget;
	private JTextArea				mTextAreaTargetDetail;
	private JScrollPane				mScrollPaneTargetDetail;
	private JComboBox[]				mComboBoxFamily;
	private JCheckBox				mCheckBoxGroupByStructure;
	private byte[][]				mTargetID;
	private String[]                mColumnTitle;
	private HashMap<String,byte[]>	mAssayDetails;

	public DETaskChemblQuery(DEFrame owner, DataWarrior application) {
		super(owner, application);
		}

	@Override
	public JPanel createDialogContent() {
		try {
			retrieveProteinClasses();
			if (sProteinClass != null)
				retrieveTargets();
			}
		catch (Exception e) {
			showErrorMessage(e.toString());
			}

		JPanel panel = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, 2*gap, HiDPIHelper.scale(512), gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, 4*gap, TableLayout.PREFERRED,
									gap, TableLayout.PREFERRED, 3*gap, TableLayout.PREFERRED, 3*gap, TableLayout.PREFERRED, gap} };
		panel.setLayout(new TableLayout(size));

		panel.add(createComboBoxSearchType(SEARCH_TYPES_SSS_SIM_EXACT_NOSTEREO_TAUTO), "1,5");
		panel.add(createComboBoxQuerySource(), "3,5");
		panel.add(createSimilaritySlider(), "1,7");
		panel.add(createStructureView(), "3,7");

		mTextFieldFilter = new JTextField();
		mTextFieldFilter.addKeyListener(this);
		panel.add(new JLabel("Target contains:", JLabel.RIGHT), "1,1");
		panel.add(mTextFieldFilter, "3,1");

		JPanel filterPanel = new JPanel();
		double[][] fs = { {TableLayout.FILL, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, TableLayout.FILL},
						  {TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2,
						   TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED} };
		filterPanel.setLayout(new TableLayout(fs));
		mComboBoxFamily = new JComboBox[PROTEIN_CLASS_LEVELS];
		for (int i=0; i<PROTEIN_CLASS_LEVELS; i++) {
			mComboBoxFamily[i] = new JComboBox();
			mComboBoxFamily[i].addItemListener(this);
			filterPanel.add(new JLabel("Level "+(i+1)), "1,"+(2*i));
			filterPanel.add(mComboBoxFamily[i], "3,"+(2*i));
			updateComboBoxFamily(i);
			}
		filterPanel.setBorder(BorderFactory.createTitledBorder("Hierarchical Protein Family Filter"));
		panel.add(filterPanel, "1,3,3,3");

		mListTarget = new JList();
		mListTarget.addListSelectionListener(this);
		JScrollPane spane1 = new JScrollPane(mListTarget, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		panel.add(new JLabel("Select Target(s):"), "5,1");
		panel.add(spane1, "5,3");

		panel.add(new JLabel("Target Detail:"), "5,5");
		mTextAreaTargetDetail = new JTextArea();
		mTextAreaTargetDetail.setBorder(null);
		mTextAreaTargetDetail.setEditable(false);
		mTextAreaTargetDetail.setLineWrap(true);
		mTextAreaTargetDetail.setWrapStyleWord(true);
		mScrollPaneTargetDetail = new JScrollPane(mTextAreaTargetDetail, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		panel.add(mScrollPaneTargetDetail, "5,7");

		mTextFieldDocIDs = new JTextField();
		panel.add(new JLabel("Pubmed-ID(s) or DOI(s):", JLabel.RIGHT), "1,9");
		panel.add(mTextFieldDocIDs, "3,9,5,9");

		mCheckBoxGroupByStructure = new JCheckBox("Group results with same compound, target, and result type");
		mCheckBoxGroupByStructure.setHorizontalAlignment(SwingConstants.CENTER);

		JPanel bottomPanel = new JPanel();
		double[][] bs = { {TableLayout.FILL, TableLayout.PREFERRED}, {TableLayout.PREFERRED} };
		bottomPanel.setLayout(new TableLayout(bs));
		bottomPanel.add(mCheckBoxGroupByStructure, "0,0");
		bottomPanel.add(new JLabel("Database Version:".concat(sDBVersion)), "1,0");
		panel.add(bottomPanel, "1,11,5,11");

		updateTargetList();

		return panel;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/databases.html#Chembl";
		}

	private void retrieveProteinClasses() {
		if (sProteinClass == null) {
			byte[][][] proteinClassTable = new ChemblCommunicator(this, "datawarrior").getProteinClassDictionary();
			if (proteinClassTable != null) {
				ArrayList<ProteinClass> proteinClassList = new ArrayList<ProteinClass>();
				for (byte[][] proteinClassRow:proteinClassTable)
					if (proteinClassRow[CLASS_DICTIONARY_COLUMN_NAME] != null)
						proteinClassList.add(new ProteinClass(proteinClassRow));
				sProteinClass = proteinClassList.toArray(new ProteinClass[0]);
				}
			}
		}

	private void retrieveTargets() {
		if (sTarget == null) {
			ArrayList<Target> targetList = new ArrayList<Target>();

			Object[] versionAndTargets = new ChemblCommunicator(this, "datawarrior").getVersionAndTargets();
			if (versionAndTargets != null)
				sDBVersion = (String)versionAndTargets[0];

			byte[][][] targetTable = (versionAndTargets != null) ? (byte[][][])versionAndTargets[1] : null;
			if (targetTable != null) {
				for (byte[][] targetRow:targetTable)
					if (targetRow[TARGET_COLUMN_NAME] != null)
						targetList.add(new Target(targetRow));
				}
			sTarget = targetList.toArray(new Target[0]);
			}
		mTargetID = new byte[sTarget.length+1][];
		}

	@Override
	protected String getSQL() {
		return null;
		}

	@Override
	protected String[] getColumnNames() {
		// return names without structure related columns
		String[] name = new String[mColumnTitle.length-3];
		for (int i=3; i<mColumnTitle.length; i++)
			name[i-3] = mColumnTitle[i];
		return name;
		}

	/**
	 * Creates a valid StructureSearchSpecification from the configuration
	 * of the currently executing task, provided that the configuration
	 * contains the necessary information.
	 * @return valid SSSpec or null
	 */
	protected StructureSearchSpecification getStructureSearchSpecification(Properties configuration) {
		StructureSearchSpecification ssSpec = super.getStructureSearchSpecification(configuration);

		if (ssSpec != null
		 && !DescriptorConstants.DESCRIPTOR_FFP512.shortName.equals(getDescriptorShortName(configuration))) {
			ssSpec.setLargestFragmentOnly(true);
			ssSpec.removeDescriptors();
			}

		return ssSpec;
		}

	@Override
	protected int getIdentifierColumn() {
		return 0;
		}

	@Override
	protected String getDocumentTitle()  {
		return "Data From ChEMBL Database";
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		for (int i=0; i<mComboBoxFamily.length; i++)
			if (mComboBoxFamily[i].getSelectedIndex() > 0)
				configuration.setProperty(PROPERTY_FAMILY+i, (String)mComboBoxFamily[i].getSelectedItem());

		String filter = mTextFieldFilter.getText();
		if (filter.length() != 0)
			configuration.setProperty(PROPERTY_FILTER, filter);

		int[] targetIndex = mListTarget.getSelectedIndices();
		if (targetIndex.length != 0) {
			StringBuilder targetBuilder = new StringBuilder(new String(mTargetID[targetIndex[0]]));
			for (int i=1; i<targetIndex.length; i++)
				targetBuilder.append(","+new String(mTargetID[targetIndex[i]]));
			configuration.setProperty(PROPERTY_TARGET_LIST, targetBuilder.toString());
			}

		configuration.setProperty(PROPERTY_GROUP_STRUCTURES, mCheckBoxGroupByStructure.isSelected() ? "true" : "false");

		String docIDs = mTextFieldDocIDs.getText();
		if (docIDs.length() != 0)
			configuration.setProperty(PROPERTY_DOC_ID_LIST, docIDs);

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		for (int i=0; i<mComboBoxFamily.length; i++) {
			String value = configuration.getProperty(PROPERTY_FAMILY+i);
			if (value != null)
				mComboBoxFamily[i].setSelectedItem(value);
			}

		String filter = configuration.getProperty(PROPERTY_FILTER);
		if (filter != null)
			mTextFieldFilter.setText(filter);

		updateTargetList();

		mListTarget.clearSelection();
		String targetString = configuration.getProperty(PROPERTY_TARGET_LIST);
		if (targetString != null) {
			for (int i=0; i<mListTarget.getModel().getSize(); i++) {
				String targetID = new String(mTargetID[i]);
				if (targetString.contains(targetID+",") || targetString.endsWith(targetID))
					mListTarget.addSelectionInterval(i, i);
				}
			}

		String value = configuration.getProperty(PROPERTY_DOC_ID_LIST);
		if (value != null)
			mTextFieldDocIDs.setText(value);

		String group = configuration.getProperty(PROPERTY_GROUP_STRUCTURES);
		mCheckBoxGroupByStructure.setSelected(group != null && group.equals("true"));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();

		mComboBoxFamily[0].setSelectedItem(0);
		mTextFieldFilter.setText("");

		updateTargetList();

		mListTarget.clearSelection();

		mTextFieldDocIDs.setText("");

		mCheckBoxGroupByStructure.setSelected(false);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		StructureSearchSpecification ssSpec = getStructureSearchSpecification(configuration);
		if (ssSpec != null) {
			String errorMessage = ssSpec.validate();
			if (errorMessage != null) {
				showErrorMessage(errorMessage);
				return false;
				}
			}

		String targetList = configuration.getProperty(PROPERTY_TARGET_LIST);
		String docIDList = configuration.getProperty(PROPERTY_DOC_ID_LIST);
		if (targetList == null && docIDList == null && ssSpec == null) {
			showErrorMessage("Neither are targets selected, PMIDs or DOIs given, nor is structure search defined.");
			return false;
			}
		if (targetList != null && targetList.replaceAll("[^,]","").length() >= MAX_QUERY_TARGETS) {
			showErrorMessage("More than "+MAX_QUERY_TARGETS+" targets selected.");
			return false;
			}

		return super.isConfigurationValid(configuration, isLive);
		}

	private void updateComboBoxFamily(int level) {
		if (sProteinClass != null) {
			mComboBoxFamily[level].removeItemListener(this);
			String parentTargetFamily = (level == 0) ? "" : (String)mComboBoxFamily[level-1].getSelectedItem();
			if (parentTargetFamily != null && parentTargetFamily.equals(ITEM_ANY))
				parentTargetFamily = null;
			mComboBoxFamily[level].removeAllItems();
			if (parentTargetFamily != null) {
				mComboBoxFamily[level].addItem(ITEM_ANY);
				if (sProteinClass != null) {
					TreeSet<String> familySet = new TreeSet<String>();
					for (ProteinClass pc:sProteinClass)
						if (pc.level == level && (level == 0 || sProteinClass[pc.parent].name.equals(parentTargetFamily)))
							familySet.add(pc.name);
		
					for (String family:familySet)
						mComboBoxFamily[level].addItem(family);
					}
				}
			mComboBoxFamily[level].setEnabled(sProteinClass != null && (level == 0 || parentTargetFamily != null));
			mComboBoxFamily[level].addItemListener(this);
			}
		}

	private void updateTargetList() {
		String[] nameFilter = (mTextFieldFilter.getText().length() == 0) ? null : mTextFieldFilter.getText().toLowerCase().split(" ");

		int queryLevel = -1;
		while (queryLevel+1 < PROTEIN_CLASS_LEVELS
			&& mComboBoxFamily[queryLevel+1].getSelectedItem() != null
			&& !ITEM_ANY.equals(mComboBoxFamily[queryLevel+1].getSelectedItem()))
			queryLevel++;

		String[] queryClassName = null;
		if (queryLevel != -1) {
			queryClassName = new String[queryLevel+1];
			for (int i=0; i<=queryLevel; i++)
				queryClassName[i] = (String)mComboBoxFamily[i].getSelectedItem();
			}

		ArrayList<String> itemList = new ArrayList<String>();
		if (sTarget != null) {
			for (Target t:sTarget) {
				int[][] targetClassIndex = getProteinClassIndexHierarchy(t);
	
				boolean violatesFilter = false;
				if (nameFilter != null) {
					for (String filter:nameFilter) {
						if (filter.length() != 0) {
							boolean matchFound =
								t.name.toLowerCase().contains(filter)
							 || t.type.toLowerCase().contains(filter)
		//					 || t.synonyms.toLowerCase().contains(filter)
							 || t.organism.toLowerCase().contains(filter)
							 || t.accession.toLowerCase().contains(filter);

							if (!matchFound) {	// check if we have a match in the protein class names
								for (int level=0; level<PROTEIN_CLASS_LEVELS; level++) {
									if (targetClassIndex[level] != null) {
										for (int i=0; i<targetClassIndex[level].length; i++) {
											if (sProteinClass[targetClassIndex[level][i]].name.toLowerCase().contains(filter)) {
												matchFound = true;
												break;
												}
											}
										}
									if (matchFound)
										break;
									}
								}
							if (!matchFound)
								violatesFilter = true;
							}
						}
					}
	
				if (!violatesFilter && queryLevel != -1) {
					if (targetClassIndex[queryLevel] == null) {
						violatesFilter = true;
						}
					else {
						boolean proteinClassFound = false;
						for (int i=0; i<targetClassIndex[queryLevel].length; i++) {
							if (queryClassName[queryLevel].equals(sProteinClass[targetClassIndex[queryLevel][i]].name)) {
								proteinClassFound = true;
								break;
								}
							}
						if (!proteinClassFound)
							violatesFilter = true;
						}
					}
	
				if (!violatesFilter) {
					mTargetID[itemList.size()] = t.id;
					itemList.add(t.getDisplayName());
					}
				}
			}

		mListTarget.setListData(itemList.toArray(new String[0]));
		updateTargetDetail(null);
		}

	/**
	 * From the highest populated protein class code(s) defined in the target (usually only one level is populated)
	 * construct parent class code(s) for the target in a stepwise manner.
	 * @param t
	 * @return
	 */
	private int[][] getProteinClassIndexHierarchy(Target t) {
		int[][] targetClassIndex = new int[PROTEIN_CLASS_LEVELS][];
		for (int level=PROTEIN_CLASS_LEVELS-1; level>=0; level--) {
			if (t.classIndex[level] != null && t.classIndex[level].length != 0) {
				int indexCount = t.classIndex[level].length;
				targetClassIndex[level] = t.classIndex[level];
				for (int i=level-1; i>=0; i--) {
					targetClassIndex[i] = new int[indexCount];
					for (int j=0; j<indexCount; j++)
						targetClassIndex[i][j] = sProteinClass[targetClassIndex[i+1][j]].parent;
					}
				break;
				}
			}
		return targetClassIndex;
		}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			for (int i=0; i<PROTEIN_CLASS_LEVELS; i++) {
				if (e.getSource() == mComboBoxFamily[i]) {
					for (int j=i+1; j<PROTEIN_CLASS_LEVELS; j++)
						updateComboBoxFamily(j);
					updateTargetList();
					}
				}
			}
		}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			int[] index = mListTarget.getSelectedIndices();
			if (index.length == 1) {
				byte[] targetID = mTargetID[index[0]];
				ByteArrayComparator comparator = new ByteArrayComparator();
				for (Target t:sTarget) {
					if (comparator.compare(t.id, targetID) == 0) {
						updateTargetDetail(t);
						return;
						}
					}
				}
			updateTargetDetail(null);
			}
		}

	private void updateTargetDetail(Target t) {
		if (t == null) {
			mTextAreaTargetDetail.setText("");
			}
		else {
			StringBuffer buf = new StringBuffer();
			buf.append("Preferred Name: "+t.name+"\n");
			buf.append("Target Type: "+t.type+"\n");
			buf.append("UniProt Accession #: "+t.accession+"\n");
//			buf.append("Synonyms: "+t.synonyms+"\n");
			buf.append("Organism: "+t.organism+"\n");
			buf.append("Target Protein Classification:");
			if (t.classIndex == null) {
				buf.append("<not available>");
				}
			else {
				int[][] targetClassIndex = getProteinClassIndexHierarchy(t);
				for (int level=0; level<PROTEIN_CLASS_LEVELS; level++) {
					buf.append("\n");
					buf.append(level+1);
					buf.append(": ");
					if (targetClassIndex[level] != null) {
						for (int i=0; i<targetClassIndex[level].length; i++) {
							if (i != 0)
								buf.append(", ");
							if (targetClassIndex[level][i] == -1) {
								buf.append("parsing error");
								}
							else {
								buf.append(sProteinClass[targetClassIndex[level][i]].name);
								}
							}
						}
					}
				}

			mTextAreaTargetDetail.setText(buf.toString());
			mTextAreaTargetDetail.setCaretPosition(0);
			}
		}

	public void keyPressed(KeyEvent arg0) {}
	public void keyTyped(KeyEvent arg0) {}

	public void keyReleased(KeyEvent arg0) {
		updateTargetList();
		}

	@Override
	protected void retrieveRecords() throws Exception {
		TreeMap<String,Object> query = new TreeMap<String,Object>();

		String targetString = getTaskConfiguration().getProperty(PROPERTY_TARGET_LIST);
		if (targetString != null) {
			String[] targetIDList = targetString.split(",");
			byte[][] targetID = new byte[targetIDList.length][];
			for (int i=0; i<targetIDList.length; i++)
				targetID[i] = targetIDList[i].getBytes();

			query.put(QUERY_TARGET_LIST, targetID);
			}

		String docIDString = getTaskConfiguration().getProperty(PROPERTY_DOC_ID_LIST);
		if (docIDString != null) {
			String[] docIDList = docIDString.split("[,;\\s]+");
			byte[][] docID = new byte[docIDList.length][];
			for (int i=0; i<docIDList.length; i++)
				docID[i] = docIDList[i].trim().getBytes();

			query.put(QUERY_DOC_ID_LIST, docID);
			}

		StructureSearchSpecification ssSpec = getStructureSearchSpecification(getTaskConfiguration());
   		if (ssSpec != null)
   			query.put(QUERY_STRUCTURE_SEARCH_SPEC, ssSpec);

		query.put(QUERY_SUPPORTS_DYNAMIC_COLUMNS, "true");

		mColumnTitle = null;
		mResultList = new ArrayList<>();
   		byte[][][] resultTable = new ChemblCommunicator(this, "datawarrior").search(query);
		if (resultTable != null) {
			for (byte[][] resultLine:resultTable) {
				if (mColumnTitle == null) {  // first line is header
					mColumnTitle = new String[resultTable[0].length];
					for (int i = 0; i<resultTable[0].length; i++)
						mColumnTitle[i] = new String(resultTable[0][i]);
					}
				else {
					Object[] row = new Object[resultLine.length];
					for (int i=0; i<resultLine.length; i++)
						row[i] = resultLine[i];
					row[RESULT_COLUMN_FFP512] = DescriptorHandlerLongFFP512.getDefaultInstance().decode((byte[])row[RESULT_COLUMN_FFP512]);
					mResultList.add(row);
					}
				}
			}

		String group = getTaskConfiguration().getProperty(PROPERTY_GROUP_STRUCTURES);
		if (group != null && group.equals("true")) {
			boolean failed = false;
			int[] groupColumns = new int[GROUP_COLUMNS.length];
			for (int i=0; i<GROUP_COLUMNS.length; i++) {
				groupColumns[i] = findColumn(GROUP_COLUMNS[i]);
				if (groupColumns[i] == -1) {
					failed = true;
					break;
					}
				}
			if (!failed)
				groupByStructure(null, groupColumns);
			}

		if (mResultList.size() != 0) {
			int assayIndexColumn = findColumn(COLUMN_TITLE_ASSAY_INDEX);
			int assayCategoryColumn = findColumn(COLUMN_TITLE_ASSAY_CATEGORY);
			if (assayIndexColumn != -1 && assayCategoryColumn != -1)
				retrieveAssayDescriptions(assayIndexColumn, assayCategoryColumn);
			}

		DELogWriter.writeEntry("retrieveChEMBL", "records:"+mResultList.size());
		}

	private int findColumn(String columnTitle) {
		if (mColumnTitle != null)
			for (int i=0; i<mColumnTitle.length; i++)
				if (mColumnTitle[i].equals(columnTitle))
					return i;
		return -1;
		}

	private void retrieveAssayDescriptions(int assayIndexColumn, int assayCategoryColumn) {
		String detailSeparator = CompoundTableModel.cDefaultDetailSeparator + "0"
							   + CompoundTableModel.cDetailIndexSeparator;
		TreeSet<byte[]> assaySet = new TreeSet<>(new ByteArrayComparator());
		for (int row=0; row<mResultList.size(); row++) {
			Object[] result = mResultList.get(row);
			String assayIndices = new String((byte[])result[assayIndexColumn]);
			while (assayIndices != null) {
				String assayIndex = null;
				int index = assayIndices.indexOf('\n');
				if (index != -1) {
					assayIndex = assayIndices.substring(0, index);
					assayIndices = assayIndices.substring(index+1);
					}
				else {
					assayIndex = assayIndices;
					assayIndices = null;
					}
				String detailCarrier = (result[assayCategoryColumn] == null) ? "" : new String((byte[])result[assayCategoryColumn]);
				result[assayCategoryColumn] = (detailCarrier + detailSeparator + assayIndex).getBytes();
				assaySet.add(assayIndex.getBytes());
				}
			}

		byte[][][] detailTable = new ChemblCommunicator(this, "datawarrior").getAssayDetailTable(assaySet.toArray(new byte[0][]));

		mAssayDetails = new HashMap<>();
		if (detailTable != null) {
			for (byte[][] detailRow:detailTable) {
				String assayID = new String(detailRow[0]);
				byte[] detail = detailRow[1];
				if (detail == null)
					detail = "NULL".getBytes();
				mAssayDetails.put(assayID, detail);
				}
			}
		}

	@Override
	protected void setColumnProperties(CompoundTableModel tableModel) {
		int assayIndexColumn = findColumn(COLUMN_TITLE_ASSAY_INDEX);
		if (assayIndexColumn != -1)
			tableModel.setColumnProperty(assayIndexColumn, CompoundTableModel.cColumnPropertyIsDisplayable, "false");

		int pubmedIDColumn = findColumn(COLUMN_TITLE_PUBMED_ID);
		if (pubmedIDColumn != -1) {
			tableModel.setColumnProperty(pubmedIDColumn, CompoundTableModel.cColumnPropertyLookupCount, "1");
			tableModel.setColumnProperty(pubmedIDColumn, CompoundTableModel.cColumnPropertyLookupName + "0", "Pubmed Abstract");
			tableModel.setColumnProperty(pubmedIDColumn, CompoundTableModel.cColumnPropertyLookupURL + "0", PUBMED_URL);
			}

		int assayCategoryColumn = findColumn(COLUMN_TITLE_ASSAY_CATEGORY);
		if (assayCategoryColumn != -1 && !mAssayDetails.isEmpty()) {
			tableModel.allocateColumnDetail(assayCategoryColumn, "Assay Description", "text/plain", CompoundTableDetailHandler.EMBEDDED);
			CompoundTableDetailHandler detailHandler = mTargetFrame.getTableModel().getDetailHandler();
			detailHandler.setEmbeddedDetailMap(mAssayDetails);
			}
		mAssayDetails = null;
		}

	@Override
    protected int getRuntimePropertiesMode() {
    	return CompoundTableEvent.cSpecifierNoRuntimeProperties;
    	}

	@Override
    protected void setRuntimeProperties() {
    	CompoundTableModel targetTableModel = mTargetFrame.getTableModel();

        DETable table = mTargetFrame.getMainFrame().getMainPane().getTable();

        int structureColumn = table.convertTotalColumnIndexToView(RESULT_COLUMN_IDCODE);
        if (structureColumn != -1)
	        table.getColumnModel().getColumn(structureColumn).setPreferredWidth(HiDPIHelper.scale(120));
		int modifierColumn = table.convertTotalColumnIndexToView(findColumn(COLUMN_TITLE_MODIFIER));
		if (modifierColumn != -1)
			table.getColumnModel().getColumn(modifierColumn).setPreferredWidth(HiDPIHelper.scale(24));
		int unitColumn = table.convertTotalColumnIndexToView(findColumn(COLUMN_TITLE_UNIT));
		if (unitColumn != -1)
			table.getColumnModel().getColumn(unitColumn).setPreferredWidth(HiDPIHelper.scale(40));
		int pValueColumn = table.convertTotalColumnIndexToView(findColumn(COLUMN_TITLE_PVALUE));
		if (pValueColumn != -1)
			table.getColumnModel().getColumn(pValueColumn).setPreferredWidth(HiDPIHelper.scale(50));
		int targetNameColumn = table.convertTotalColumnIndexToView(findColumn(COLUMN_TITLE_TARGET_NAME));
		if (targetNameColumn != -1)
			table.getColumnModel().getColumn(targetNameColumn).setPreferredWidth(HiDPIHelper.scale(150));
		int targetOrganismColumn = table.convertTotalColumnIndexToView(findColumn(COLUMN_TITLE_TARGET_ORGANISM));
		if (targetOrganismColumn != -1)
			table.getColumnModel().getColumn(targetOrganismColumn).setPreferredWidth(HiDPIHelper.scale(150));
		int targetDescColumn = table.convertTotalColumnIndexToView(findColumn(COLUMN_TITLE_TARGET_DESCRIPTION));
		if (targetDescColumn != -1)
			table.getColumnModel().getColumn(targetDescColumn).setPreferredWidth(HiDPIHelper.scale(150));
		int referenceColumn = table.convertTotalColumnIndexToView(findColumn(COLUMN_TITLE_TARGET_DESCRIPTION));
		if (referenceColumn != -1)
			table.getColumnModel().getColumn(referenceColumn).setPreferredWidth(HiDPIHelper.scale(180));

        mTargetFrame.getMainFrame().getPruningPanel().addDefaultFilters();
        mTargetFrame.getMainFrame().getMainPane().addStructureView("Structure", "Table\tbottom\t0.6", structureColumn);
        }

	private class ProteinClass {
		String name;
		int parent,level;

		public ProteinClass(byte[][] proteinClassRow) {
			name = createString(proteinClassRow[CLASS_DICTIONARY_COLUMN_NAME]);
			level = createInteger(proteinClassRow[CLASS_DICTIONARY_COLUMN_LEVEL]) - 1;	// level 0 is already filtering
			parent = createInteger(proteinClassRow[CLASS_DICTIONARY_COLUMN_PARENT]);
			}

		private String createString(byte[] bytes) {
			return bytes == null ? "" : new String(bytes);
			}

		private int createInteger(byte[] bytes) {
			try {
				return bytes == null ? -1 : Integer.parseInt(new String(bytes));
				}
			catch (NumberFormatException nfe) {
				return -1;
				}
			}
		}

	private class Target {
		byte[] id;
		String name;
		String type;
		String accession;
		String organism;
		int[][] classIndex;

		public Target(byte[][] targetRow) {
			id = targetRow[TARGET_COLUMN_ID];
			name = createString(targetRow[TARGET_COLUMN_NAME]);
			type = createString(targetRow[TARGET_COLUMN_TYPE]);
			organism = createString(targetRow[TARGET_COLUMN_ORGANISM]);
			accession = createString(targetRow[TARGET_COLUMN_ACCESSION]);
//			synonyms = createString(targetRow[TARGET_COLUMN_SYNONYM]);
			classIndex = new int[PROTEIN_CLASS_LEVELS][];
			for (int level=0; level<PROTEIN_CLASS_LEVELS; level++)
				classIndex[level] = createIntegerArray(targetRow[TARGET_COLUMN_CLASSIFICATION_LEVEL_1+level]);
			}

		private int[] createIntegerArray(byte[] bytes) {
			if (bytes == null || bytes.length == 0)
				return null;
			String[] entry = new String(bytes).split(",");
			int[] value = new int[entry.length];
			for (int i=0; i<entry.length; i++) {
				try {
					value[i] = Integer.parseInt(entry[i]);
					}
				catch (NumberFormatException nfe) {
					value[i] = -1;
					}
				}
			return value;
			}

		private String createString(byte[] bytes) {
			return bytes == null ? "" : new String(bytes);
			}

		protected String getDisplayName() {
			return (organism.length() == 0) ? name : name+" ["+organism+"]";
			}
		}
	}
