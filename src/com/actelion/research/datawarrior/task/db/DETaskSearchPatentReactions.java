package com.actelion.research.datawarrior.task.db;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.ExtendedDepictor;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.StructureSearchSpecification;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.chem.reaction.ReactionSearchSpecification;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DELogWriter;
import com.actelion.research.datawarrior.DEPruningPanel.FilterException;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.JEditableChemistryView;
import com.actelion.research.gui.JEditableStructureView;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JCompoundTableForm;
import com.actelion.research.util.ByteArrayComparator;
import com.actelion.research.util.ColorHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class DETaskSearchPatentReactions extends ConfigurableTask implements ActionListener,PatentReactionServerConstants {
	public static final String TASK_NAME = "Search EU/US Patent Reactions";

	private static final String PROPERTY_TEXT_QUERY = "textQuery";

	private static final String PROPERTY_QUERY_SOURCE = "querySource";
	private static final String PROPERTY_SEARCH_TYPE = "searchType";
	private static final String PROPERTY_IDCODE = "idcode";
	private static final String PROPERTY_IDCOORDS = "idcoordinates";
	private static final String PROPERTY_MAPPING = "mapping";
	private static final String PROPERTY_SIMILARITY_1 = "similarity1";
	private static final String PROPERTY_SIMILARITY_2 = "similarity2";
	private static final String PROPERTY_DESCRIPTOR_NAME = "descriptorName";	// not used yet; for future use

	private static final String[] STRUCTURE_SEARCH_TYPE_TEXT = {
			"Transformation (Reaction-SS)",
			"Similar Reactions",
			"Retron (Formed Substructure)",
			"Substructure in Products", "Similar Products", "Equal Products",
			"Substructure in Reactants", "Similar Reactants", "Equal Reactants" };
	private static final String[] SEARCH_TYPE_CODE = {"subrxn", "simrxn", "retron", "sssP", "simP", "exactP", "sssR", "simR", "exactR" /*, "noStereo", "tautomer"*/ };
	public static final int SEARCH_TYPE_REACTION_SSS = 0;
	public static final int SEARCH_TYPE_REACTION_SIM = 1;
	public static final int SEARCH_TYPE_RETRON = 2;
	public static final int SEARCH_TYPE_PRODUCT_SSS = 3;
	public static final int SEARCH_TYPE_PRODUCT_SIM = 4;
	public static final int SEARCH_TYPE_PRODUCT_EXACT = 5;
	public static final int SEARCH_TYPE_REACTANT_SSS = 6;
	public static final int SEARCH_TYPE_REACTANT_SIM = 7;
	public static final int SEARCH_TYPE_REACTANT_EXACT = 8;

	private static final String[] QUERY_SOURCE = {"the reaction/structure drawn below", "any selected reaction/structure"};
	private static final String[] QUERY_SOURCE_CODE = {"drawn", "selected"};
	private static final int QUERY_SOURCE_DRAWN = 0;
	private static final int QUERY_SOURCE_SELECTED = 1;

	private static final int CHEMISTRY_COLUMN_COUNT = STRUCTURE_COLUMN_TITLE.length + DESCRIPTOR_COLUMN_TITLE.length;
	private static final int REACTANT_FFP_COLUMN = 4;   // The server delivers the reactant (and product) FFPs as array of FFPs
	private static final int PRODUCT_FFP_COLUMN = 5;    //  for every reactant (product). Thus, We need to merge them.

	private static final String DOC_TITLE = "Reactions From EU And US Patents";

	private DataWarrior         	mApplication;
	private DEFrame         		mTargetFrame;
	private CompoundTableModel      mSourceTableModel;
	private JTextField[]	        mQueryTextField;
	private JPanel                  mChemistryPanel;
	private CardLayout              mChemistryCardLayout;
	private JComboBox   		    mComboBoxSearchType,mComboBoxQuerySource;
	private JEditableStructureView  mStructureView;
	private JEditableChemistryView  mReactionView;
	private JSlider         		mStructureSimSlider,mReactionSimSlider1,mReactionSimSlider2;
	private String[]                mQueryField;
	private int                     mIDCodeColumn;
	private boolean                 mAreReactionsSelected,mAreSubReactionsSelected,mAreStructuresSelected,mAreSubStructuresSelected,mDisableEvents;

	public DETaskSearchPatentReactions(DEFrame parent, DataWarrior application) {
		super(parent, true);
		mSourceTableModel = parent.getTableModel();
		mApplication = application;

		mAreReactionsSelected = false;
		mAreSubReactionsSelected = false;
		mAreStructuresSelected = false;
		mAreSubStructuresSelected = false;
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++) {
			if (mSourceTableModel.isColumnTypeStructure(column)) {
				if ("true".equals(mSourceTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyIsFragment)))
					mAreSubStructuresSelected = true;
				else
					mAreStructuresSelected = true;
				}
			if (mSourceTableModel.isColumnTypeReaction(column)) {
				if ("true".equals(mSourceTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyIsFragment)))
					mAreSubReactionsSelected = true;
				else
					mAreReactionsSelected = true;
				}
			}
		if (mAreReactionsSelected || mAreSubReactionsSelected || mAreStructuresSelected || mAreSubStructuresSelected) {
			boolean selectionFound = false;
			for (int row=0; row<mSourceTableModel.getRowCount(); row++) {
				if (mSourceTableModel.isSelected(row)) {
					selectionFound = true;
					break;
					}
				}
			if (!selectionFound) {
				mAreReactionsSelected = false;
				mAreSubReactionsSelected = false;
				mAreStructuresSelected = false;
				mAreSubStructuresSelected = false;
				}
			}
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String structureSearchType = configuration.getProperty(PROPERTY_SEARCH_TYPE);
		String firstTextQuery = configuration.getProperty(PROPERTY_TEXT_QUERY+"1");
		if (structureSearchType == null && firstTextQuery == null) {
			showErrorMessage("No search criteria specified.");
			return false;
			}

		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

//	@Override
//	public String getHelpURL() {
//		return "/html/help/actelion.html#Mercury";
//	}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
	}

	@Override
	public JComponent createDialogContent() {
		JPanel content = new JPanel();
		content.setLayout(new BorderLayout());

		int gap = HiDPIHelper.scale(8);

		JPanel searchTypePanel = new JPanel();
		double[][] size1 = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL, gap},
							 {gap, TableLayout.PREFERRED, gap} };
		searchTypePanel.setLayout(new TableLayout(size1));
		searchTypePanel.add(new JLabel("Search for"), "1,1");
		searchTypePanel.add(createComboBoxSearchType(), "3,1");
		searchTypePanel.add(createComboBoxQuerySource(), "5,1");
		content.add(searchTypePanel, BorderLayout.NORTH);

		int chemGap = HiDPIHelper.scale(60);
		JPanel structurePanel = new JPanel();
		double[][] size2s = { {gap, TableLayout.FILL, gap, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, gap},
							  {gap, chemGap, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, chemGap, 2*gap} };
		structurePanel.setLayout(new TableLayout(size2s));
		mStructureSimSlider = createSimilaritySlider1();
		structurePanel.add(createStructureView(), "1,1,1,5");
		structurePanel.add(mStructureSimSlider, "3,2,5,2");
		structurePanel.add(new JLabel("Structure", JLabel.CENTER), "4,3");
		structurePanel.add(new JLabel("Similarity", JLabel.CENTER), "4,4");

		JPanel reactionPanel = new JPanel();
		double[][] size2r = { {gap, TableLayout.FILL, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL,
									TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL, gap},
							  {gap, TableLayout.FILL, gap, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, 2*gap} };
		reactionPanel.setLayout(new TableLayout(size2r));

		mReactionSimSlider1 = createSimilaritySlider1();
		mReactionSimSlider2 = createSimilaritySlider2();
		reactionPanel.add(createReactionView(), "1,1,9,1");
		reactionPanel.add(new JLabel("Reaction Center", JLabel.CENTER), "2,4");
		reactionPanel.add(new JLabel("Similarity", JLabel.CENTER), "2,5");
		reactionPanel.add(mReactionSimSlider1, "4,3,4,6");
		reactionPanel.add(new JLabel("Periphery", JLabel.CENTER), "6,4");
		reactionPanel.add(new JLabel("Similarity", JLabel.CENTER), "6,5");
		reactionPanel.add(mReactionSimSlider2, "8,3,8,6");

		mChemistryCardLayout = new CardLayout();
		mChemistryPanel = new JPanel();
		mChemistryPanel.setLayout(mChemistryCardLayout);
		mChemistryPanel.add(reactionPanel, "r");
		mChemistryPanel.add(structurePanel, "s");
		mChemistryCardLayout.show(mChemistryPanel, "r");
		content.add(mChemistryPanel, BorderLayout.CENTER);

		mQueryField = new PatentReactionCommunicator(this, "DataWarrior").getQueryFields();
		if (mQueryField != null) {
			double[][] size3 = new double[2][];
			double[] size3h = { gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED,
							  2*gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap };
			int verticalFieldCount = (mQueryField.length+1) >> 1;
			size3[0] = size3h;
			size3[1] = new double[2*(verticalFieldCount+1)];
			for (int i=0; i<verticalFieldCount+1; i++) {
				size3[1][2*i] = TableLayout.PREFERRED;
				size3[1][2*i+1] = gap;
				}
			JPanel queryFieldPanel = new JPanel();
			queryFieldPanel.setLayout(new TableLayout(size3));

			mQueryTextField = new JTextField[mQueryField.length];
			for (int i=0; i<mQueryField.length; i++) {
				int x = (i<verticalFieldCount) ? 1 : 5;
				int y = 2 * (i % verticalFieldCount);
				queryFieldPanel.add(new JLabel(mQueryField[i]+":", JLabel.RIGHT), ""+x+","+y);
				queryFieldPanel.add(mQueryTextField[i] = new JTextField(12), ""+(x+2)+","+y);
				}
			queryFieldPanel.add(new JLabel("use '<', '>', '-' for numerical fields and ',' for text fields. Examples: \"80-90\" or \"mukaiyama,sakurai\"", JLabel.CENTER),
					"1,"+(verticalFieldCount*2)+",7,"+(verticalFieldCount*2));

			content.add(queryFieldPanel, BorderLayout.SOUTH);
			}

		return content;
		}

	private JComponent createComboBoxSearchType() {
		mComboBoxSearchType = new JComboBox(STRUCTURE_SEARCH_TYPE_TEXT);
		mComboBoxSearchType.setMaximumRowCount(12);
		mComboBoxSearchType.addActionListener(this);
		return mComboBoxSearchType;
		}

	private JComponent createComboBoxQuerySource() {
		mComboBoxQuerySource = new JComboBox(QUERY_SOURCE);
		mComboBoxQuerySource.addActionListener(this);
		mComboBoxQuerySource.setEnabled(mAreReactionsSelected || mAreSubReactionsSelected || mAreStructuresSelected || mAreSubStructuresSelected);
		return mComboBoxQuerySource;
		}

	private JComponent createReactionView() {
		mReactionView = new JEditableChemistryView(ExtendedDepictor.TYPE_REACTION);
		Reaction rxn = new Reaction();
		rxn.setFragment(true);
		mReactionView.setContent(rxn);
		mReactionView.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(getContrastGrey(0.2f, mReactionView.getBackground()), 2, true), "Reaction  "));
		return mReactionView;
		}

	private JComponent createStructureView() {
		StereoMolecule mol = new StereoMolecule();
		mol.setFragment(true);
		int scaled100 = HiDPIHelper.scale(100);
		mStructureView = new JEditableStructureView(mol);
		mStructureView.setMinimumSize(new Dimension(scaled100, scaled100));
		mStructureView.setPreferredSize(new Dimension(scaled100, scaled100));
		mStructureView.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(getContrastGrey(0.2f, mStructureView.getBackground()), 2, true), "Structure  "));
		return mStructureView;
		}

	protected Color getContrastGrey(float contrast, Color color) {
		float brightness = ColorHelper.perceivedBrightness(color);

		if (contrast == 1f)
			return (brightness > 0.5) ? Color.BLACK : Color.WHITE;

		float range = (brightness > 0.5) ? brightness : 1f-brightness;

		// enhance contrast for middle bright backgrounds
		contrast = (float)Math.pow(contrast, range);

		return (brightness > 0.5) ?
				Color.getHSBColor(0.0f, 0.0f, brightness - range*contrast)
				: Color.getHSBColor(0.0f, 0.0f, brightness + range*contrast);
		}

	private JSlider createSimilaritySlider1() {
		Hashtable<Integer,JLabel> labels = new Hashtable<>();
		labels.put(Integer.valueOf(70), new JLabel("70%"));
		labels.put(Integer.valueOf(80), new JLabel("80%"));
		labels.put(Integer.valueOf(90), new JLabel("90%"));
		labels.put(Integer.valueOf(100), new JLabel("100%"));
		JSlider slider = new JSlider(JSlider.HORIZONTAL, 70, 100, 90);
		slider.setMinorTickSpacing(1);
		slider.setMajorTickSpacing(10);
		slider.setLabelTable(labels);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		int height = slider.getPreferredSize().height;
		slider.setMinimumSize(new Dimension(HiDPIHelper.scale(150), height));
		slider.setPreferredSize(new Dimension(HiDPIHelper.scale(150), height));
		slider.setEnabled(false);
		return slider;
		}

	private JSlider createSimilaritySlider2() {
		Hashtable<Integer,JLabel> labels = new Hashtable<>();
		labels.put(Integer.valueOf(0), new JLabel("0%"));
		labels.put(Integer.valueOf(50), new JLabel("50%"));
		labels.put(Integer.valueOf(100), new JLabel("100%"));
		JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		slider.setMinorTickSpacing(5);
		slider.setMajorTickSpacing(25);
		slider.setLabelTable(labels);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		int height = slider.getPreferredSize().height;
		slider.setMinimumSize(new Dimension(HiDPIHelper.scale(150), height));
		slider.setPreferredSize(new Dimension(HiDPIHelper.scale(150), height));
		slider.setEnabled(false);
		return slider;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (mDisableEvents)
			return;

		int searchType = mComboBoxSearchType.getSelectedIndex();
		boolean isSSS = isSSS(searchType);
		boolean isSim = isSim(searchType);
		boolean isRxn = isRxn(searchType);

		if (e.getSource() == mComboBoxSearchType) {
			if (isRxn) {
				Reaction rxn = mReactionView.getReaction();
				if (rxn.isFragment() ^ isSSS) {
					rxn.setFragment(isSSS);
					mReactionView.setContent(rxn);
					}
				}
			else {
				if (mStructureView.getMolecule().isFragment() ^ isSSS) {
					mStructureView.getMolecule().setFragment(isSSS);
					mStructureView.structureChanged();
					}
				}

			if (mComboBoxQuerySource.getSelectedIndex()==QUERY_SOURCE_SELECTED) {
				mDisableEvents = true;
				if (isRxn) {
					if ((isSSS && !mAreSubReactionsSelected)
					 || (isSim && !mAreReactionsSelected))
						mComboBoxQuerySource.setSelectedIndex(QUERY_SOURCE_DRAWN);

					}
				else {
					if ((isSSS && !mAreSubStructuresSelected)
					 || (isSim && !mAreStructuresSelected))
						mComboBoxQuerySource.setSelectedIndex(QUERY_SOURCE_DRAWN);
					}
				mDisableEvents = false;
				}

			mChemistryCardLayout.show(mChemistryPanel, isRxn ? "r" : "s");
			enableChemistryItems();
			return;
			}
		else if (e.getSource() == mComboBoxQuerySource) {
			boolean isMultiQuery = (mComboBoxQuerySource.getSelectedIndex()==QUERY_SOURCE_SELECTED);
			if (isMultiQuery) {
				if (isRxn) {
					if (isSSS && !mAreSubReactionsSelected && mAreReactionsSelected) {
						mDisableEvents = true;
						mComboBoxSearchType.setSelectedIndex(SEARCH_TYPE_REACTION_SIM);
						mDisableEvents = false;

						Reaction rxn = mReactionView.getReaction();
						rxn.setFragment(false);
						mReactionView.setContent(rxn);
						}
					else if (isSim && !mAreReactionsSelected && mAreSubReactionsSelected) {
						mDisableEvents = true;
						mComboBoxSearchType.setSelectedIndex(SEARCH_TYPE_REACTION_SSS);
						mDisableEvents = false;

						Reaction rxn = mReactionView.getReaction();
						rxn.setFragment(true);
						mReactionView.setContent(rxn);
						}
					}
				else {
					if (isSSS && !mAreSubStructuresSelected && mAreStructuresSelected) {
						mDisableEvents = true;
						if (searchType == SEARCH_TYPE_REACTANT_SSS)
							mComboBoxSearchType.setSelectedIndex(SEARCH_TYPE_REACTANT_SIM);
						else
							mComboBoxSearchType.setSelectedIndex(SEARCH_TYPE_PRODUCT_SIM);
						mDisableEvents = false;

						mStructureView.getMolecule().setFragment(false);
						mStructureView.structureChanged();
						}
					else if (isSim && !mAreStructuresSelected && mAreSubStructuresSelected) {
						mDisableEvents = true;
						if (searchType == SEARCH_TYPE_REACTANT_SIM)
							mComboBoxSearchType.setSelectedIndex(SEARCH_TYPE_REACTANT_SSS);
						else
							mComboBoxSearchType.setSelectedIndex(SEARCH_TYPE_PRODUCT_SSS);
						mDisableEvents = false;

						mStructureView.getMolecule().setFragment(true);
						mStructureView.structureChanged();
						}
					}
				}
			enableChemistryItems();
			return;
			}
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		int searchType = mComboBoxSearchType.getSelectedIndex();
		boolean isRxn = isRxn(searchType);
		boolean isSim = isSim(searchType);

		configuration.setProperty(PROPERTY_SEARCH_TYPE, SEARCH_TYPE_CODE[searchType]);

		boolean drawnChemistryAvailable = false;

		if (isRxn) {
			if (mReactionView.isEnabled() && !mReactionView.getReaction().isEmpty()) {
				String[] rxnCode = ReactionEncoder.encode(mReactionView.getReaction(), false);
				configuration.setProperty(PROPERTY_IDCODE, rxnCode[0]);
				configuration.setProperty(PROPERTY_MAPPING, rxnCode[1]);
				configuration.setProperty(PROPERTY_IDCOORDS, rxnCode[2]);
				drawnChemistryAvailable = true;
				}

			if (drawnChemistryAvailable || mComboBoxQuerySource.getSelectedIndex() == QUERY_SOURCE_SELECTED) {
				configuration.setProperty(PROPERTY_QUERY_SOURCE, QUERY_SOURCE_CODE[mComboBoxQuerySource.getSelectedIndex()]);
				if (isSim) {
					configuration.setProperty(PROPERTY_SIMILARITY_1, "" + mReactionSimSlider1.getValue());
					configuration.setProperty(PROPERTY_SIMILARITY_2, "" + mReactionSimSlider2.getValue());
					}
				}
			}
		else {
			if (mStructureView.isEnabled() && mStructureView.getMolecule().getAllAtoms() != 0) {
				Canonizer canonizer = new Canonizer(mStructureView.getMolecule());
				configuration.setProperty(PROPERTY_IDCODE, canonizer.getIDCode());
				configuration.setProperty(PROPERTY_IDCOORDS, canonizer.getEncodedCoordinates());
				drawnChemistryAvailable = true;
				}

			if (drawnChemistryAvailable || mComboBoxQuerySource.getSelectedIndex() == QUERY_SOURCE_SELECTED) {
				configuration.setProperty(PROPERTY_QUERY_SOURCE, QUERY_SOURCE_CODE[mComboBoxQuerySource.getSelectedIndex()]);
				if (isSim)
					configuration.setProperty(PROPERTY_SIMILARITY_1, ""+mStructureSimSlider.getValue());
				}
			}

		if (mQueryField != null) {
			int count = 0;
			for (int i=0; i<mQueryField.length; i++) {
				String query = mQueryTextField[i].getText();
				if (query.length() != 0)
					configuration.setProperty(PROPERTY_TEXT_QUERY +(++count), mQueryField[i]+","+query);
				}
			}

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mDisableEvents = true;

		int searchType = findListIndex(configuration.getProperty(PROPERTY_SEARCH_TYPE), SEARCH_TYPE_CODE, SEARCH_TYPE_REACTION_SSS);
		boolean isRxn = isRxn(searchType);
		boolean isSim = isSim(searchType);

		mChemistryCardLayout.show(mChemistryPanel, isRxn ? "r" : "s");

		mComboBoxSearchType.setSelectedIndex(searchType);

		String querySource = configuration.getProperty(PROPERTY_QUERY_SOURCE);

		// don't use "any selected structure" if we have no selection
		if (QUERY_SOURCE_CODE[QUERY_SOURCE_SELECTED].equals(querySource)
				&& isInteractive()
				&& !mSourceTableModel.hasSelectedRows()) {
			mComboBoxQuerySource.setSelectedIndex(QUERY_SOURCE_DRAWN);
			}
		else {
			mComboBoxQuerySource.setSelectedIndex(findListIndex(querySource, QUERY_SOURCE_CODE, QUERY_SOURCE_DRAWN));
			}

		String idcode = configuration.getProperty(PROPERTY_IDCODE);
		String coords = configuration.getProperty(PROPERTY_IDCOORDS);
		String mapping = configuration.getProperty(PROPERTY_MAPPING);
		if (isRxn) {
			Reaction rxn = ReactionEncoder.decode(idcode, mapping, coords, null, null, true, null);
			if (rxn != null)
				mReactionView.setContent(rxn);
			if (isSim) {
				mReactionSimSlider1.setValue(Integer.parseInt(configuration.getProperty(PROPERTY_SIMILARITY_1)));
				mReactionSimSlider2.setValue(Integer.parseInt(configuration.getProperty(PROPERTY_SIMILARITY_2)));
				}
			}
		else {
			mStructureView.setIDCode(idcode, coords);
			if (isSim)
				mStructureSimSlider.setValue(Integer.parseInt(configuration.getProperty(PROPERTY_SIMILARITY_1)));
			}

		mDisableEvents = false;

		enableChemistryItems();

		int count = 0;
		while (mQueryField != null) {
			String textQuery = configuration.getProperty(PROPERTY_TEXT_QUERY+(++count), "");
			if (textQuery.length() == 0)
				break;

			int index = textQuery.indexOf(",");
			if (index != -1) {
				String key = textQuery.substring(0, index);
				String value = textQuery.substring(index+1);
				for (int i=0; i<mQueryField.length; i++)
					if (key .equals(mQueryField[i]))
						mQueryTextField[i].setText(value);
				}
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mDisableEvents = true;

		mComboBoxSearchType.setSelectedIndex(SEARCH_TYPE_REACTION_SSS);
		mComboBoxQuerySource.setSelectedIndex(QUERY_SOURCE_DRAWN);

		mStructureView.setIDCode(null, null);
		Reaction rxn = new Reaction();
		rxn.setFragment(true);
		mReactionView.setContent(rxn);

		mReactionSimSlider1.setValue(80);
		mReactionSimSlider2.setValue(0);
		mStructureSimSlider.setValue(80);

		mDisableEvents = false;

		enableChemistryItems();

		if (mQueryTextField != null)
			for (JTextField textField:mQueryTextField)
				textField.setText("");
		}

	private boolean isRxn(int searchType) {
		return searchType == SEARCH_TYPE_REACTION_SSS
			|| searchType == SEARCH_TYPE_REACTION_SIM;
		}

	private boolean isSSS(int searchType) {
		return searchType == SEARCH_TYPE_RETRON
			|| searchType == SEARCH_TYPE_REACTION_SSS
			|| searchType == SEARCH_TYPE_REACTANT_SSS
			|| searchType == SEARCH_TYPE_PRODUCT_SSS;
		}

	private boolean isSim(int searchType) {
		return searchType == SEARCH_TYPE_REACTION_SIM
			|| searchType == SEARCH_TYPE_REACTANT_SIM
			|| searchType == SEARCH_TYPE_PRODUCT_SIM;
		}

	private void enableChemistryItems() {
		int searchType = mComboBoxSearchType.getSelectedIndex();
		boolean isRxn = isRxn(searchType);
		boolean isSim = isSim(searchType);
		boolean isSSS = isSSS(searchType);
		mStructureSimSlider.setEnabled(isSim);
		mReactionSimSlider1.setEnabled(isSim);
		mReactionSimSlider2.setEnabled(isSim);
		mComboBoxQuerySource.setEnabled(isRxn ? ((isSim && mAreReactionsSelected) || (isSSS && mAreSubReactionsSelected))
											  : ((isSim && mAreStructuresSelected) || (isSSS && mAreSubStructuresSelected)));
		mStructureView.setEnabled(mComboBoxQuerySource.getSelectedIndex()==QUERY_SOURCE_DRAWN);
		mReactionView.setEnabled(mComboBoxQuerySource.getSelectedIndex()==QUERY_SOURCE_DRAWN);
		}

	private ReactionSearchSpecification getReactionSearchSpecification(Properties configuration) {
		int searchType = getReactionSearchType(configuration);
		if (searchType == -1)
			return null;

		if (searchType == ReactionSearchSpecification.TYPE_RETRON) {
			String[] idcode = null;
			String descriptorName = DescriptorConstants.DESCRIPTOR_FFP512.shortName;
			long[][] descriptor = null;
			int querySource = findListIndex(configuration.getProperty(PROPERTY_QUERY_SOURCE), QUERY_SOURCE_CODE, QUERY_SOURCE_DRAWN);
			if (querySource == QUERY_SOURCE_DRAWN) {
				String idcodeString = configuration.getProperty(PROPERTY_IDCODE);
				if (idcodeString == null)
					return null;

				idcode = new String[1];
				idcode[0] = idcodeString;
				}
			else {
				byte[][] bytes = getSelectedIDCodes(true);
				idcode = new String[bytes.length];
				for (int i=0; i<bytes.length; i++)
					idcode[i] = new String(bytes[i]);
				descriptor = (long[][])getSelectedDescriptors(descriptorName);
				}

			ReactionSearchSpecification spec = new ReactionSearchSpecification(idcode, descriptor);

			String errorMessage = spec.validate();
			if (errorMessage == null)
				return spec;
			}
		else {
			boolean isSSS = searchType == ReactionSearchSpecification.TYPE_SUBREACTION;
			String[] query;
			long[][] reactionDescriptor = null;
			long[][] reactantDescriptor = null;
			long[][] productDescriptor = null;
			int querySource = findListIndex(configuration.getProperty(PROPERTY_QUERY_SOURCE), QUERY_SOURCE_CODE, QUERY_SOURCE_DRAWN);
			if (querySource == QUERY_SOURCE_DRAWN) {
				String idcode = configuration.getProperty(PROPERTY_IDCODE);
				String coords = configuration.getProperty(PROPERTY_IDCOORDS);
				String mapping = configuration.getProperty(PROPERTY_MAPPING);

				if (idcode == null || coords == null || mapping == null)
					return null;

				query = new String[1];
				query[0] = idcode+ReactionEncoder.OBJECT_DELIMITER_STRING+mapping+ReactionEncoder.OBJECT_DELIMITER_STRING+coords;
				}
			else {
				Object[][] reactionObjects = getSelectedReactionsWithDescriptors(isSSS);
				query = new String[reactionObjects.length];
				reactionDescriptor = new long[reactionObjects.length][];
				reactantDescriptor = new long[reactionObjects.length][];
				productDescriptor = new long[reactionObjects.length][];
				for (int i=0; i<reactionObjects.length; i++) {
					query[i] = (String)reactionObjects[i][0];
					reactionDescriptor[i] = (long[])reactionObjects[i][1];
					reactantDescriptor[i] = (long[])reactionObjects[i][2];
					productDescriptor[i] = (long[])reactionObjects[i][3];
					}
				}

			ReactionSearchSpecification spec = new ReactionSearchSpecification(searchType, query,
							reactionDescriptor, reactantDescriptor, productDescriptor,
							getSimilarity1Value(configuration), getSimilarity2Value(configuration));

			String errorMessage = spec.validate();
			if (errorMessage == null)
				return spec;
			}

		return null;
		}

		/**
		 * Creates a valid StructureSearchSpecification from the configuration
		 * of the currently executing task, provided that the configuration
		 * contains the necessary information.
		 * @return valid SSSpec or null
		 */
	private StructureSearchSpecification getStructureSearchSpecification(Properties configuration) {
		if (isRxn(findListIndex(configuration.getProperty(PROPERTY_SEARCH_TYPE), SEARCH_TYPE_CODE, -1)))
			return null;

		byte[][] idcode = null;
		String descriptorName = configuration.getProperty(PROPERTY_DESCRIPTOR_NAME, DescriptorConstants.DESCRIPTOR_FFP512.shortName);

		int searchType = getStructureSearchType(configuration);

		Object[] descriptor = null;
		int querySource = findListIndex(configuration.getProperty(PROPERTY_QUERY_SOURCE), QUERY_SOURCE_CODE, QUERY_SOURCE_DRAWN);
		if (querySource == QUERY_SOURCE_DRAWN) {
			String idcodeString = configuration.getProperty(PROPERTY_IDCODE);
			if (idcodeString == null)
				return null;

			idcode = new byte[1][];
			idcode[0] = idcodeString.getBytes();
			}
		else {
			idcode = getSelectedIDCodes(searchType == StructureSearchSpecification.TYPE_SUBSTRUCTURE);
			if (searchType == StructureSearchSpecification.TYPE_SIMILARITY)
				descriptor = getSelectedDescriptors(descriptorName);
			}

		StructureSearchSpecification spec = new StructureSearchSpecification(getStructureSearchType(configuration), idcode,
				descriptor, descriptorName, getSimilarity1Value(configuration));

		String errorMessage = spec.validate();
		if (errorMessage == null)
			return spec;

		showErrorMessage(errorMessage);
		return null;
		}

	private int getReactionSearchType(Properties configuration) {
		int searchType = findListIndex(configuration.getProperty(PROPERTY_SEARCH_TYPE), SEARCH_TYPE_CODE, -1);

		return searchType == SEARCH_TYPE_REACTION_SSS ? ReactionSearchSpecification.TYPE_SUBREACTION
			 : searchType == SEARCH_TYPE_REACTION_SIM ? ReactionSearchSpecification.TYPE_SIMILARITY
			 : searchType == SEARCH_TYPE_RETRON ? ReactionSearchSpecification.TYPE_RETRON : -1;
		}

	private int getStructureSearchType(Properties configuration) {
		int searchType = findListIndex(configuration.getProperty(PROPERTY_SEARCH_TYPE), SEARCH_TYPE_CODE, -1);

		if (isRxn(searchType))
			return -1;

		return isSSS(searchType) ? StructureSearchSpecification.TYPE_SUBSTRUCTURE
				: isSim(searchType) ? StructureSearchSpecification.TYPE_SIMILARITY
				: StructureSearchSpecification.TYPE_EXACT_STRICT;
		}

	private int getStructureSearchTarget(Properties configuration) {
		int searchType = findListIndex(configuration.getProperty(PROPERTY_SEARCH_TYPE), SEARCH_TYPE_CODE, -1);

		if (searchType == SEARCH_TYPE_REACTANT_SSS
		 || searchType == SEARCH_TYPE_REACTANT_SIM
		 || searchType == SEARCH_TYPE_REACTANT_EXACT)
			return STRUCTURE_SEARCH_TARGET_REACTANT;

		if (searchType == SEARCH_TYPE_PRODUCT_SSS
		 || searchType == SEARCH_TYPE_PRODUCT_SIM
		 || searchType == SEARCH_TYPE_PRODUCT_EXACT)
			return STRUCTURE_SEARCH_TARGET_PRODUCT;

		return -1;
		}

	/**
	 * If no configuration was set then the behaviour is undefined.
	 * @return similarity value of current configuration (0.0 <= value <= 1.0).
	 */
	private float getSimilarity1Value(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_SIMILARITY_1);
		if (value != null)
			try { return Float.parseFloat(value) / 100; } catch (NumberFormatException e) {}
		return Float.NaN;
		}

	/**
	 * If no configuration was set then the behaviour is undefined.
	 * @return similarity value of current configuration (0.0 <= value <= 1.0).
	 */
	private float getSimilarity2Value(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_SIMILARITY_2);
		if (value != null)
			try { return Float.parseFloat(value) / 100; } catch (NumberFormatException e) {}
		return Float.NaN;
		}

	/**
	 * Creates an array of all selected reaction-codes.
	 * If no rows are selected or not structure column exists,
	 * then an empty array is returned.
	 * @return array of selected idcodes
	 */
	private Object[][] getSelectedReactionsWithDescriptors(final boolean isSSS) {
		int reactionColumn = getReactionColumn(isSSS);
		if (reactionColumn == -1)
			return null;

		int mappingColumn = mSourceTableModel.getChildColumn(reactionColumn, CompoundTableConstants.cColumnTypeReactionMapping);
		if (mappingColumn == -1)
			return null;

//		int coordsColumn = mSourceTableModel.getChildColumn(reactionColumn, CompoundTableConstants.cColumnType2DCoordinates);

		int reactionDescriptorColumn = mSourceTableModel.getChildColumn(reactionColumn, DescriptorConstants.DESCRIPTOR_ReactionFP.shortName);
		int reactantDescriptorColumn = mSourceTableModel.getChildColumn(reactionColumn, DescriptorConstants.DESCRIPTOR_FFP512.shortName, CompoundTableConstants.cReactionPartReactants);
		int productDescriptorColumn = mSourceTableModel.getChildColumn(reactionColumn, DescriptorConstants.DESCRIPTOR_FFP512.shortName, CompoundTableConstants.cReactionPartProducts);

		ArrayList<Object[]> reactionList = new ArrayList<>();
		for (int row=0; row<mSourceTableModel.getRowCount(); row++) {
			if (mSourceTableModel.isSelected(row)) {
				byte[] idcode = (byte[])mSourceTableModel.getRecord(row).getData(reactionColumn);
				byte[] mapping = (byte[])mSourceTableModel.getRecord(row).getData(mappingColumn);
//				byte[] coords = coordsColumn == -1 ? null : (byte[])mSourceTableModel.getRecord(row).getData(coordsColumn);

				if (idcode != null && mapping != null) {
					Object[] rowObjects = new Object[4];
					rowObjects[0] = new String(idcode)+ReactionEncoder.OBJECT_DELIMITER_STRING+new String(mapping);
					rowObjects[1] = mSourceTableModel.getRecord(row).getData(reactionDescriptorColumn);
					rowObjects[2] = mSourceTableModel.getRecord(row).getData(reactantDescriptorColumn);
					rowObjects[3] = mSourceTableModel.getRecord(row).getData(productDescriptorColumn);
					reactionList.add(rowObjects);
					}
				}
			}
		return reactionList.toArray(new Object[0][]);
		}

	private int getReactionColumn(boolean isSSS) {
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (mSourceTableModel.isColumnTypeReaction(column))
				if (isSSS == "true".equals(mSourceTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyIsFragment)))
					return column;

		return -1;
		}

	/**
	 * Creates an array of all selected idcodes. May ask user
	 * to select one of multiple structure columns.
	 * If no rows are selected or not structure column exists,
	 * then an empty array is returned.
	 * @return array of selected idcodes
	 */
	private byte[][] getSelectedIDCodes(final boolean isSSS) {
		mIDCodeColumn = -1;
		try {
			SwingUtilities.invokeAndWait(() -> mIDCodeColumn = selectIDCodeColumn(isSSS) );
			}
		catch (Exception e) {}

		TreeSet<byte[]> idcodeSet = new TreeSet<byte[]>(new ByteArrayComparator());
		for (int row=0; row<mSourceTableModel.getRowCount(); row++)
			if (mSourceTableModel.isSelected(row))
				idcodeSet.add((byte[])mSourceTableModel.getRecord(row).getData(mIDCodeColumn));
		return idcodeSet.toArray(new byte[0][]);
	}

	/**
	 * May only be called after calling getSelectedIDCodes()!!!
	 * @return descriptors of selected rows
	 */
	private Object[] getSelectedDescriptors(String descriptorShortName) {
		if (mIDCodeColumn == -1)
			return null;

		int descriptorColumn = mSourceTableModel.getChildColumn(mIDCodeColumn, descriptorShortName);
		if (descriptorColumn == -1)
			return null;

		TreeMap<byte[],Object> map = new TreeMap<>(new ByteArrayComparator());
		for (int row=0; row<mSourceTableModel.getRowCount(); row++) {
			if (mSourceTableModel.isSelected(row)) {
				CompoundRecord record = mSourceTableModel.getRecord(row);
				map.put((byte[])record.getData(mIDCodeColumn), record.getData(descriptorColumn));
			}
		}
		return map.values().toArray(new Object[0]);
	}

	private int selectIDCodeColumn(boolean isSSS) {
		int idcodeColumn = -1;
		ArrayList<String> structureColumnList = null;
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++) {
			if (mSourceTableModel.isColumnTypeStructure(column)
					&& (!isSSS ^ "true".equals(mSourceTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyIsFragment)))) {
				if (idcodeColumn == -1) {
					idcodeColumn = column;
				}
				else if (structureColumnList == null) {
					structureColumnList = new ArrayList<String>();
					structureColumnList.add(mSourceTableModel.getColumnTitle(idcodeColumn));
					structureColumnList.add(mSourceTableModel.getColumnTitle(column));
				}
				else {
					structureColumnList.add(mSourceTableModel.getColumnTitle(column));
				}
			}
		}

		if (structureColumnList != null) {
			String idcodeColumnName = (String)JOptionPane.showInputDialog(getParentFrame(),
					"Please select the column containing the query structures!",
					"Select Structure Column",
					JOptionPane.QUESTION_MESSAGE,
					null,
					structureColumnList.toArray(),
					structureColumnList.get(0));
			idcodeColumn = mSourceTableModel.findColumn(idcodeColumnName);
		}

		return idcodeColumn;
	}

	private Object[][] retrieveRecords(Properties configuration) {
		TreeMap<String,Object> query = new TreeMap<>();

		ReactionSearchSpecification rsSpec = getReactionSearchSpecification(configuration);
		if (rsSpec != null) {
			query.put(QUERY_REACTION_SEARCH_SPEC, rsSpec);
			}
		else {
			StructureSearchSpecification ssSpec = getStructureSearchSpecification(configuration);
			if (ssSpec != null) {
				query.put(QUERY_STRUCTURE_SEARCH_SPEC, ssSpec);
				query.put(QUERY_KEY_STRUCTURE_SEARCH_TARGET, STRUCTURE_SEARCH_TARGET_CODE[getStructureSearchTarget(configuration)]);
				}
			}

		ArrayList<String[]> keyValuePairs = new ArrayList<>();
		int count = 0;
		while (true) {
			String queryText = configuration.getProperty(PROPERTY_TEXT_QUERY+(++count), "");
			if (queryText.length() == 0)
				break;

			int index = queryText.indexOf(",");
			if (index != -1) {
				String[] keyValuePair = new String[2];
				keyValuePair[0] = queryText.substring(0, index);
				keyValuePair[1] = queryText.substring(index+1);
				keyValuePairs.add(keyValuePair);
				}
			}
		if (keyValuePairs.size() != 0)
			query.put(QUERY_KEY_VALUE_PAIRS, keyValuePairs.toArray(new String[0][]));

		Object[][] resultTable = new PatentReactionCommunicator(this, "datawarrior").search(query);

		if (resultTable != null)
			DELogWriter.writeEntry("retrievePatentReactions", "records:"+(resultTable.length-1));

		return resultTable;
		}

	private long[] mergeLongDescriptors(Object o) {
		if (o != null && o instanceof long[][]) {
			long[][] ffpArray = (long[][])o;
			if (ffpArray.length == 0)
				return null;

			long[] mergedFFP = null;
			for (int i=0; i<ffpArray.length; i++) {
				if (ffpArray[i] != null) {
					if (mergedFFP == null)
						mergedFFP = ffpArray[i];
					else
						for (int j = 0; j<mergedFFP.length; j++)
							mergedFFP[j] |= ffpArray[i][j];
					}
				}

			return mergedFFP;
			}

		return null;
		}

	@Override
	public void runTask(Properties configuration) {
		startProgress("Searching And Retrieving Patent Reactions On/From Server...", 0, 0);

		Object[][] resultTable;
		try {
			resultTable = retrieveRecords(configuration);
			if (resultTable == null)
				return;     // message was already shown
			}
		catch (Exception e) {
			e.printStackTrace();
			showErrorMessage(e.toString());
			return;
			}
		if (resultTable.length <= 1) {
			showMessage("Your query did not retrieve any records.", WARNING_MESSAGE);
			return;
			}

		startProgress("Populating table...", 0, 0);

		if (!threadMustDie()) {
			mTargetFrame = mApplication.getEmptyFrame(DOC_TITLE);
			int columnCount = resultTable[0].length;
			CompoundTableModel tableModel = mTargetFrame.getTableModel();
			tableModel.initializeTable(resultTable.length-1, columnCount);

			tableModel.prepareReactionColumns(0, "Reaction", false, true, true, false,
					true, true, true, false);
			for (int column=CHEMISTRY_COLUMN_COUNT; column<columnCount; column++)
				tableModel.setColumnName(new String((byte[])resultTable[0][column]), column);

			startProgress("Populating Table...", 0, resultTable.length-1);
			for (int i=1; i<resultTable.length; i++) {
				updateProgress(i-1);

				for (int column=0; column<columnCount; column++) {
					Object value = resultTable[i][column];
					if (value != null && (column == REACTANT_FFP_COLUMN || column == PRODUCT_FFP_COLUMN))
						value = mergeLongDescriptors(value);
					tableModel.setTotalDataAt(value, i - 1, column);
					}
				}
			}

		if (mTargetFrame != null) {
			CompoundTableModel tableModel = mTargetFrame.getTableModel();
			tableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, this);

			try {
				SwingUtilities.invokeLater(() -> {
					mTargetFrame.setTitle(DOC_TITLE);
					setRuntimeProperties(tableModel);
				} );
			}
			catch (Exception e) {}
		}
	}

	private void setRuntimeProperties(CompoundTableModel tableModel) {
		JCompoundTableForm form = mTargetFrame.getMainFrame().getMainPane().addFormView("Form View", "Table\tbottom\t0.5", false).getCompoundTableForm();
		form.setFormLayoutDescriptor("TableLayout,7,8.0,0.17,8.0,-1.0,8.0,-1.0,8.0,17,8.0,-1.0,8.0,-1.0,8.0,-1.0,8.0,-1.0,8.0,-1.0,8.0,-1.0,8.0,-1.0,8.0,0.41,8.0");
		try {
			form.addFormObject("Reaction\treaction\t3,1,5,7,full,full");
			form.addFormObject("Reactants\ttextLine\t3,9,3,11,full,full");
			form.addFormObject("ID\ttextLine\t1,1,1,1,full,full");
			form.addFormObject("Preparation\ttextArea\t1,15,5,15,full,full");
			form.addFormObject("Patent No\ttextLine\t1,3,1,9,full,full");
			form.addFormObject("Products\ttextLine\t5,9,5,11,full,full");
			form.addFormObject("Reagents\ttextLine\t3,13,5,13,full,full");
			}
		catch (NumberFormatException e) {}

		try {
			mTargetFrame.getMainFrame().getPruningPanel().addReactionFilter(tableModel, RESULT_COLUMN_RXNCODE, null);
			mTargetFrame.getMainFrame().getPruningPanel().addRetronFilter(tableModel, RESULT_COLUMN_RXNCODE, null);
			for (int column=CHEMISTRY_COLUMN_COUNT; column<tableModel.getTotalColumnCount(); column++)
				mTargetFrame.getMainFrame().getPruningPanel().addDefaultFilter(column);
		}
		catch (FilterException fe) {}
	}
}
