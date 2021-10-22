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

package com.actelion.research.datawarrior.task.chem.elib;

import com.actelion.research.chem.*;
import com.actelion.research.chem.prediction.MolecularPropertyHelper;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.TaskUIDelegate;
import com.actelion.research.gui.*;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class UIDelegateELib implements ActionListener,TaskConstantsELib,TaskUIDelegate {

	private static final String DEFAULT_OPTION = "Default Structures";
	private static final String RANDOM_OPTION = "Random structures";
	private static final String FILE_OPTION = "Structure(s) from a file";
	private static final String CUSTOM_OPTION = "Custom structure(s)";
	private static final String HITLIST_OPTION = "(s) of list '";
	private static final String SELECTED_OPTION = "Selected ";
	private static final String ALL_OPTION = "All ";

	// random molecule generation constants
	private static final String RANDOM_MOLECULE_SEED = "eM@Hz@";
	private static final int FINAL_KEEP_SIZE_MUTATION_COUNT = 8;
	private static final int MAX_GROW_MUTATIONS = 128;	// to exit almost endless loops if highly preferred back&forth mutations suppress real changes

	//	private static final String LSD_IDCODE = "fa{q@@DZjCHhhhddXeEhmEjt\\e[WkUUSUADPtRLJr@@";
	private static final String[] DEFAULT_SET = {
			"fb}I@@DRTXFQQJJJIJIZsbm^mENVh@HBHjB@@",
			"ffka`@B`xXhaHRDaDaHQDRDGBTeztPirV`jBjbjjFjHUDrJbX@",
			"fj}qb@LNKAFPAFRRFJIJIIQVVb{rRkUAPE@PP@@",
			"fc\u007FPR@L^`apPAFRREQIJKQPyJJEJcEV}E^uiBBQjjh@HbDR@@",
			"fa{q@@DZjCHhhhddXeEhmEjt\\e[WkUUSUADPtRLJr@@",
			"fj}`c@I^BCDDDEbLbbRabTtRbJhqQdy`@jjA`HbGJ@@",
			"fnkq@@L\\jCHeEDehdhddlZ\\TZI\\uZZjfJBbdH@@",
			"fbmi`@DTxIPDyrJIQQJjJKILyIW`mLtuSUUVMKtC@@" };

	private Frame				mParentFrame;
	private CompoundTableModel	mTableModel;
	private JComboBox			mComboBoxStartCompounds,mComboBoxGenerations,mComboBoxCompounds,
								mComboBoxSurvivalCount,mComboBoxCreateLike,mComboBoxFitnessOption;
	private JPanel				mFitnessPanel;
	private JScrollPane			mFitnessScrollpane;
	private CompoundCollectionPane<StereoMolecule> mFirstGenerationPane;
	private DefaultCompoundCollectionModel.Molecule mFirstGeneration;

	public UIDelegateELib(Frame parent, CompoundTableModel tableModel) {
		mParentFrame = parent;
		mTableModel = tableModel;
		}

	@Override
	public JComponent createDialogContent() {
		JPanel p1 = new JPanel();
		final int scaled4 = HiDPIHelper.scale(4);
		final int scaled8 = HiDPIHelper.scale(8);
		double[][] size1 = { {scaled8, TableLayout.PREFERRED, scaled8, TableLayout.PREFERRED, TableLayout.FILL,
				TableLayout.PREFERRED, scaled8, TableLayout.PREFERRED, scaled8},
							 {scaled8, TableLayout.PREFERRED, scaled8, HiDPIHelper.scale(104), 2*scaled8,
								TableLayout.PREFERRED, scaled4, TableLayout.PREFERRED, scaled4, TableLayout.PREFERRED,
								3*scaled8, TableLayout.PREFERRED, scaled8, TableLayout.FILL, scaled8} };
		p1.setLayout(new TableLayout(size1));

		p1.add(new JLabel("1st generation:", JLabel.RIGHT), "1,1");

		mComboBoxStartCompounds = new JComboBox();
		mComboBoxStartCompounds.addItem(DEFAULT_OPTION);
		mComboBoxStartCompounds.addItem(RANDOM_OPTION);
		mComboBoxStartCompounds.addItem(CUSTOM_OPTION);
		mComboBoxStartCompounds.addItem(FILE_OPTION);
		addStructureOptions(mComboBoxStartCompounds);
		mComboBoxStartCompounds.addActionListener(this);
		p1.add(mComboBoxStartCompounds, "3,1");

		p1.add(new JLabel("(Selected sub-structures are kept untouched)", JLabel.RIGHT), "4,1,7,1");

		mFirstGeneration = new DefaultCompoundCollectionModel.Molecule();
		mFirstGenerationPane = new CompoundCollectionPane<StereoMolecule>(mFirstGeneration, false);
		mFirstGenerationPane.setEditable(true);
		mFirstGenerationPane.setClipboardHandler(new ClipboardHandler());
		mFirstGenerationPane.setShowValidationError(true);
		p1.add(mFirstGenerationPane, "1,3,7,3");

		mComboBoxGenerations = new JComboBox(GENERATION_OPTIONS);
		mComboBoxCompounds = new JComboBox(COMPOUND_OPTIONS);
		mComboBoxCompounds.setSelectedItem(DEFAULT_COMPOUNDS);
		mComboBoxCompounds.setEditable(true);
		mComboBoxSurvivalCount = new JComboBox(SURVIVAL_OPTIONS);
		mComboBoxSurvivalCount.setSelectedItem(DEFAULT_SURVIVALS);
		mComboBoxSurvivalCount.setEditable(true);
		p1.add(mComboBoxGenerations, "1,5");
		p1.add(mComboBoxCompounds, "1,7");
		p1.add(mComboBoxSurvivalCount, "1,9");
		p1.add(new JLabel("Generations"), "3,5");
		p1.add(new JLabel("Compounds per generation"), "3,7");
		p1.add(new JLabel("Compounds survive a generation"), "3,9");

		p1.add(new JLabel("Create compounds like"), "5,5");
		mComboBoxCreateLike = new JComboBox(COMPOUND_KIND_TEXT);
		p1.add(mComboBoxCreateLike, "7,5");

		p1.add(new JLabel("Fitness Criteria"), "1,11");
		JButton bAdd = new JButton("Add Criterion");
		bAdd.setActionCommand("addOption");
		bAdd.addActionListener(this);
		p1.add(bAdd, "5,11");
		mComboBoxFitnessOption = new JComboBox();
		mComboBoxFitnessOption.addItem(FitnessPanel.STRUCTURE_OPTION_TEXT);
		mComboBoxFitnessOption.addItem(FitnessPanel.CONFORMER_OPTION_TEXT);
		mComboBoxFitnessOption.addItem(FitnessPanel.DOCKING_OPTION_TEXT);
		for (int i = 0; i< MolecularPropertyHelper.getPropertyCount(); i++)
			mComboBoxFitnessOption.addItem(MolecularPropertyHelper.getPropertyName(i));
		mComboBoxFitnessOption.setSelectedIndex(0); // structure option
		p1.add(mComboBoxFitnessOption, "7,11");

		mFitnessPanel = new JPanel() {
		    private static final long serialVersionUID = 0x20140724;

		    @Override
		    public void paintComponent(Graphics g) {
		    	super.paintComponent(g);

		    	if (getComponentCount() == 0) {
		            Dimension theSize = getSize();
		            Insets insets = getInsets();
		            theSize.width -= insets.left + insets.right;
		            theSize.height -= insets.top + insets.bottom;

	    	        g.setColor(Color.GRAY);
	    	        FontMetrics metrics = g.getFontMetrics();
	    	        final String message = "<To add fitness criteria select type and click 'Add Criterion'>";
	    	        Rectangle2D bounds = metrics.getStringBounds(message, g);
	    	        g.drawString(message, (int)(insets.left+theSize.width-bounds.getWidth())/2,
	    	                                   (insets.top+theSize.height-metrics.getHeight())/2+metrics.getAscent());
		    		}

		    	Rectangle r = new Rectangle();
		    	g.setColor(Color.GRAY);
		    	for (int i=1; i<getComponentCount(); i++) {
		    		getComponent(i).getBounds(r);
					g.drawLine(r.x+2, r.y-3, r.x+r.width-3, r.y-3);
					g.drawLine(r.x+2, r.y-2, r.x+r.width-3, r.y-2);
		    		}
		    	}
			};
		Dimension fitnessPanelSize = new Dimension(HiDPIHelper.scale(512), HiDPIHelper.scale(256));
		mFitnessPanel.setLayout(new VerticalFlowLayout());
		mFitnessScrollpane = new JScrollPane(mFitnessPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		mFitnessScrollpane.setMinimumSize(fitnessPanelSize);
		mFitnessScrollpane.setPreferredSize(fitnessPanelSize);
//		mFitnessScrollpane.setBorder(BorderFactory.createEmptyBorder());
		p1.add(mFitnessScrollpane, "1,13,7,13");

		return p1;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxStartCompounds) {
			String startSetOption = (String)mComboBoxStartCompounds.getSelectedItem();
			mFirstGeneration.clear();
			if (startSetOption.equals(DEFAULT_OPTION)) {
				for (String idcode:DEFAULT_SET)
					mFirstGeneration.addCompound(new IDCodeParser(true).getCompactMolecule(idcode));
				}
			else if (startSetOption.equals(RANDOM_OPTION)) {
				int count = 4 * Integer.parseInt((String)mComboBoxSurvivalCount.getSelectedItem());
				ConcurrentLinkedQueue<StereoMolecule> compounds = createRandomStartSet(count);
				while (!compounds.isEmpty())
					mFirstGeneration.addCompound(compounds.poll());
				}
			else if (startSetOption.equals(FILE_OPTION)) {
				ArrayList<StereoMolecule> compounds = new FileHelper(mParentFrame).readStructuresFromFile(false);
				if (compounds != null)
					for (StereoMolecule mol:compounds)
						mFirstGeneration.addCompound(mol);
				}
			else if (!startSetOption.equals(CUSTOM_OPTION)) {
				ArrayList<MoleculeWithDescriptor> mwdl = getSelectedMolecules(startSetOption, null);
				if (mwdl != null)
					for (MoleculeWithDescriptor mwd:mwdl)
						mFirstGeneration.addCompound(mwd.mMol);
				}
			return;
			}
		if (e.getActionCommand().equals("addOption")) {
			int type = mComboBoxFitnessOption.getSelectedIndex() - FitnessPanel.SPECIAL_OPTION_COUNT;
			mFitnessPanel.add(FitnessPanel.createFitnessPanel(mParentFrame, this, type));
			mFitnessScrollpane.validate();
			mFitnessScrollpane.repaint();
			return;
			}
		}

	protected void addStructureOptions(JComboBox comboBox) {
		int[] column = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode);
		if (column != null) {
			for (int i=0; i<column.length; i++)
				comboBox.addItem(SELECTED_OPTION + mTableModel.getColumnTitle(column[i]) + "(s)");
			for (int i=0; i<column.length; i++)
				comboBox.addItem(ALL_OPTION + mTableModel.getColumnTitle(column[i]) + "(s)");

			CompoundTableListHandler hh = mTableModel.getListHandler();
			for (int i = 0; i<hh.getListCount(); i++) {
				String hitlistName = hh.getListName(i);
				for (int j=0; j<column.length; j++)
					comboBox.addItem(mTableModel.getColumnTitle(column[j])+HITLIST_OPTION+hitlistName+"'");
				}
			}
		}

	protected ArrayList<MoleculeWithDescriptor> getSelectedMolecules(String comboBoxOption, String descriptorType) {
		int flag = -1;
		int idcodeColumn = -1;
		if (comboBoxOption.contains(HITLIST_OPTION)) {
			int index = comboBoxOption.indexOf(HITLIST_OPTION);
			idcodeColumn = mTableModel.findColumn(comboBoxOption.substring(0, index));
			String hitlistName = comboBoxOption.substring(index+HITLIST_OPTION.length(), comboBoxOption.length()-1);
			CompoundTableListHandler hh = mTableModel.getListHandler();
			flag = hh.getListFlagNo(hh.getListIndex(hitlistName));
			}
		else if (comboBoxOption.startsWith(SELECTED_OPTION)) {
			idcodeColumn = mTableModel.findColumn(comboBoxOption.substring(SELECTED_OPTION.length(), comboBoxOption.length()-3));
			flag = CompoundRecord.cFlagSelected;
			}
		else if (comboBoxOption.startsWith(ALL_OPTION)) {
			idcodeColumn = mTableModel.findColumn(comboBoxOption.substring(ALL_OPTION.length(), comboBoxOption.length()-3));
			flag = -2;
			}
		int descriptorColumn = (descriptorType == null) ? -1 : mTableModel.getChildColumn(idcodeColumn, descriptorType);
		ArrayList<MoleculeWithDescriptor> moleculeList = new ArrayList<MoleculeWithDescriptor>();
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			CompoundRecord record = mTableModel.getTotalRecord(row);
			if (flag == -2 || record.isFlagSet(flag)) {
				StereoMolecule mol = mTableModel.getChemicalStructure(record, idcodeColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
				if (mol != null) {
					mol.removeAtomColors();
					Object descriptor = (descriptorColumn == -1) ? null : record.getData(descriptorColumn);
					moleculeList.add(new MoleculeWithDescriptor(mol, descriptor));
					}
				}
			}
		return (moleculeList.size() == 0) ? null : moleculeList;
		}

	private ConcurrentLinkedQueue<StereoMolecule> createRandomStartSet(int molCount) {
		final int MIN_ATOMS = 12;
		final int MAX_ATOMS = 18;
		final ConcurrentLinkedQueue<StereoMolecule> moleculeQueue = new ConcurrentLinkedQueue<>();
		final AtomicInteger remaining = new AtomicInteger(molCount);

		int kind = mComboBoxCreateLike.getSelectedIndex();
		final AtomTypeList atomTypeList = createAtomTypeList("/resources/"+TaskConstantsELib.COMPOUND_KIND_FILE[kind]);

		final JProgressDialog pg = new JProgressDialog(mParentFrame);
		pg.startProgress("Generating random start set...", 0, molCount);

		new Thread(() -> {
			int threadCount = Math.min(molCount, Runtime.getRuntime().availableProcessors());
			Thread[] thread = new Thread[threadCount];
			for (int i=0; i<threadCount; i++) {
				thread[i] = new Thread("Molecule Generator "+(i+1)) {
					public void run() {
						final Random random = new Random();
						Mutator mutator = new Mutator(atomTypeList);
						StereoMolecule mol = new StereoMolecule();

						int m = remaining.decrementAndGet();
						while (m >= 0 && !pg.threadMustDie()) {
							pg.updateProgress(molCount - m - 1);

							new IDCodeParser().parse(mol, RANDOM_MOLECULE_SEED);

							double randomValue = random.nextDouble();
							int targetAtomCount = MIN_ATOMS + (int)Math.round(randomValue * (MAX_ATOMS - MIN_ATOMS));

							for (int i=0; i<MAX_GROW_MUTATIONS && mol.getAllAtoms()<targetAtomCount; i++)
								if (null == mutator.mutate(mol, Mutator.MUTATION_GROW | Mutator.MUTATION_KEEP_SIZE, false))
									break;

							for (int i = 0; i< FINAL_KEEP_SIZE_MUTATION_COUNT; i++)
								if (null == mutator.mutate(mol, Mutator.MUTATION_KEEP_SIZE, false))
									break;

							moleculeQueue.add(new StereoMolecule(mol));
							m = remaining.decrementAndGet();
							}
						}
					};
				}

			for (Thread t:thread)
				t.start();

			for (Thread t:thread)
				try { t.join(); } catch (InterruptedException ie) {}

			pg.setVisible(false);
			pg.dispose();
			} ).start();

		pg.setVisible(true);

		return moleculeQueue;
		}

	private AtomTypeList createAtomTypeList(String fileName) {
		try {
			return new AtomTypeList(fileName, AtomTypeCalculator.cPropertiesForMutator);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		StringBuilder sb = new StringBuilder();
		for (int i=0; i<mFirstGeneration.getSize(); i++) {
			if (sb.length() != 0)
				sb.append('\t');
			sb.append(new Canonizer(mFirstGeneration.getMolecule(i), Canonizer.ENCODE_ATOM_SELECTION).getIDCode());
			}
		configuration.setProperty(PROPERTY_START_SET, sb.toString());

		configuration.setProperty(PROPERTY_SURVIVAL_COUNT, (String)mComboBoxSurvivalCount.getSelectedItem());
		configuration.setProperty(PROPERTY_GENERATION_COUNT, (String)mComboBoxGenerations.getSelectedItem());
		configuration.setProperty(PROPERTY_GENERATION_SIZE, (String)mComboBoxCompounds.getSelectedItem());

		configuration.setProperty(PROPERTY_COMPOUND_KIND, COMPOUND_KIND_CODE[mComboBoxCreateLike.getSelectedIndex()]);

		int fitnessOptionCount = mFitnessPanel.getComponentCount();
		configuration.setProperty(PROPERTY_FITNESS_PARAM_COUNT, Integer.toString(fitnessOptionCount));
		for (int i=0; i<fitnessOptionCount; i++) {
			FitnessPanel fp = (FitnessPanel)mFitnessPanel.getComponent(i);
			configuration.setProperty(PROPERTY_FITNESS_PARAM_CONFIG+i, fp.getConfiguration());
			}

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		for (String idcode:configuration.getProperty(PROPERTY_START_SET, "").split("\\t"))
			mFirstGeneration.addCompound(new IDCodeParser(true).getCompactMolecule(idcode));

		mComboBoxSurvivalCount.setSelectedItem(configuration.getProperty(PROPERTY_SURVIVAL_COUNT, DEFAULT_SURVIVALS));
		mComboBoxGenerations.setSelectedItem(configuration.getProperty(PROPERTY_GENERATION_COUNT, DEFAULT_GENERATIONS));
		mComboBoxCompounds.setSelectedItem(configuration.getProperty(PROPERTY_GENERATION_SIZE, DEFAULT_COMPOUNDS));

		mComboBoxCreateLike.setSelectedIndex(AbstractTask.findListIndex(configuration.getProperty(PROPERTY_COMPOUND_KIND), COMPOUND_KIND_CODE, 0));

		int fitnessOptionCount = Integer.parseInt(configuration.getProperty(PROPERTY_FITNESS_PARAM_COUNT));
		for (int i=0; i<fitnessOptionCount; i++) {
			String config = configuration.getProperty(PROPERTY_FITNESS_PARAM_CONFIG+i);
			if (config != null)
				mFitnessPanel.add(FitnessPanel.createFitnessPanel(mParentFrame, this, config));
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		for (String idcode:DEFAULT_SET)
			mFirstGeneration.addCompound(new IDCodeParser(true).getCompactMolecule(idcode));

		mComboBoxSurvivalCount.setSelectedItem(DEFAULT_SURVIVALS);
		mComboBoxGenerations.setSelectedItem(DEFAULT_GENERATIONS);
		mComboBoxCompounds.setSelectedItem(DEFAULT_COMPOUNDS);

		mComboBoxCreateLike.setSelectedIndex(0);
		}
	}
