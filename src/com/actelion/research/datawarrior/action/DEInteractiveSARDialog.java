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

package com.actelion.research.datawarrior.action;

import com.actelion.research.chem.coords.CoordinateInventor;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

import com.actelion.research.chem.*;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.gui.*;


public class DEInteractiveSARDialog extends JDialog
                implements ActionListener,DrawAreaListener,ItemListener,Runnable {
    private static final long serialVersionUID = 0x20090206;
 
    private static final String[] COMPOUND_FILTER_OPTIONS = {
                              "All compounds",
                              "All matching compounds",
                              "Non-matching compounds" };
    private static final int OPTION_ALL_COMPOUNDS = 0;
    private static final int OPTION_MATCHING_COMPOUNDS = 1;
    private static final int OPTION_NON_MATCHING_COMPOUNDS = 2;
    private static final String COMPOUND_FILTER_OPTION_BB = "Compounds matching B";

    private static final String OPTION_BUILDING_BLOCK = "Building Block ";
    private static final String OPTION_SUFFIX_NEW = " (new)";

	private CompoundTableModel mTableModel;
	private Frame  			   mParentFrame;
	private SARSourceRecord[]  mSourceRecord;
	private CompoundCollectionPane<SARSourceRecord> mCompoundPane;
    private CompoundCollectionPane<BuildingBlock> mBuildingBlockPane;
	private SSSearcherWithIndex mSearcher; // is used for non-event-dispatcher-thread searches
	private JDrawArea          mDrawArea;
	private JButton            mButtonAddBuildingBlock,mButtonAnalyze;
	private JComboBox          mComboBoxCompoundFilter,mComboBoxABB;
	private SARStatusPanel     mStatusPanel;
	private JProgressDialog    mProgressDialog;
	private JProgressPanel     mProgressPanel;
    private int                mIDCodeColumn,mFragFpColumn;
    private boolean            mSkipIncompletelyMatching;
    private MoleculeContext    mCurrentMoleculeContext;
    private AbstractBuildingBlock mCurrentABB;
    private ArrayList<AbstractBuildingBlock> mABBList;

    public DEInteractiveSARDialog(Frame owner, CompoundTableModel tableModel, int idcodeColumn) {
        super(owner, "Define SAR Building Blocks", true);

		mParentFrame = owner;
		mTableModel = tableModel;
		mIDCodeColumn = idcodeColumn;
		mFragFpColumn = tableModel.getChildColumn(idcodeColumn, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
		mSearcher = new SSSearcherWithIndex();

		if (mTableModel.isMultiLineColumn(idcodeColumn)) {
            showMessageDialog("Column '"+mTableModel.getColumnTitle(idcodeColumn)+"' contains multiple structures per cell.");
		    return;
		    }

		AbstractBuildingBlock.initializeIDs();
		mABBList = new ArrayList<AbstractBuildingBlock>();
		mCurrentABB = null;

        boolean visibleOnly = false;
        if (mTableModel.getTotalRowCount() != mTableModel.getRowCount()) {
            int option = JOptionPane.showConfirmDialog(this,
                    "Some records are not visible.\nDo you want to exclude them from the analysis?",
                    "Visible Records Only?",
                    JOptionPane.YES_NO_OPTION);

            visibleOnly = (option == JOptionPane.YES_OPTION);
            }
        if (visibleOnly) {
            mSourceRecord = new SARSourceRecord[mTableModel.getRowCount()];
            for (int row=0; row<mTableModel.getRowCount(); row++)
                if (mTableModel.getRecord(row).getData(idcodeColumn) != null)
                    mSourceRecord[row] = new SARSourceRecord(mTableModel.getRecord(row));
            }
        else {
            mSourceRecord = new SARSourceRecord[mTableModel.getTotalRowCount()];
            for (int row=0; row<mTableModel.getTotalRowCount(); row++)
                if (mTableModel.getTotalRecord(row).getData(idcodeColumn) != null)
                    mSourceRecord[row] = new SARSourceRecord(mTableModel.getTotalRecord(row));
            }

        int gap = HiDPIHelper.scale(8);
        double[][] size = { {gap, HiDPIHelper.scale(26), gap, HiDPIHelper.scale(120), 2*gap, TableLayout.FILL, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL, gap},
                {gap, TableLayout.FILL,
                 gap, TableLayout.PREFERRED,
                 gap, HiDPIHelper.scale(120),
                 gap, TableLayout.PREFERRED, gap} };
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new TableLayout(size));

        mStatusPanel = new SARStatusPanel(mSourceRecord);
        mainPanel.add(mStatusPanel, "1,1,1,5");

        mDrawArea = new JDrawArea(new StereoMolecule(), 0);
        mDrawArea.setPreferredSize(new Dimension(HiDPIHelper.scale(400), HiDPIHelper.scale(240)));
        mDrawArea.toolChanged(JDrawToolbar.cToolLassoPointer);
        mDrawArea.addDrawAreaListener(this);

        mainPanel.add(mDrawArea, "5,1,9,1");

        mButtonAddBuildingBlock = new JButton("Take Selection As ");
        mButtonAddBuildingBlock.addActionListener(this);
        mainPanel.add(mButtonAddBuildingBlock, "6,3");
        mButtonAddBuildingBlock.setEnabled(false);

        mComboBoxABB = new JComboBox();
        mComboBoxABB.addItemListener(this);
        configureComboBoxABB();
        mainPanel.add(mComboBoxABB, "8,3");

        mComboBoxCompoundFilter = new JComboBox(COMPOUND_FILTER_OPTIONS);
        mComboBoxCompoundFilter.addItemListener(this);
        mainPanel.add(mComboBoxCompoundFilter, "1,7,3,7");

        CompoundCollectionModel<SARSourceRecord> model1 = new DefaultCompoundCollectionModel<SARSourceRecord>() {
            public StereoMolecule getMolecule(int index) {
                CompoundRecord cr = ((SARSourceRecord)getCompound(index)).getCompoundRecord();
                StereoMolecule mol = mTableModel.getChemicalStructure(cr, mIDCodeColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
                long[] fragFp = (long[])cr.getData(mFragFpColumn);
                MoleculeContext context = new MoleculeContext(mol, fragFp);
                context.matchBuildingBlocks(mABBList);
                context.colorizeBuildingBlocks();
                return mol;
                }

            public void addMolecule(int index, StereoMolecule mol) {}
            public void setMolecule(int index, StereoMolecule mol) {}
            };
        mCompoundPane = new CompoundCollectionPane<SARSourceRecord>(model1, true) {
		    private static final long serialVersionUID = 0x20090206;

		    public void setSelection(int index) {
		        if (index == -1) {
		            mDrawArea.setMolecule(new StereoMolecule());
		            }
		        else {
		            // create molecule context with the correct size
	                CompoundRecord cr = ((SARSourceRecord)getModel().getCompound(index)).getCompoundRecord();
	                StereoMolecule mol = mTableModel.getChemicalStructure(cr, mIDCodeColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
	                long[] fragFp = (long[])cr.getData(mFragFpColumn);
		            mCurrentMoleculeContext = new MoleculeContext(mol, fragFp);
		            mCurrentMoleculeContext.matchBuildingBlocks(mABBList);
		            mCurrentMoleculeContext.colorizeBuildingBlocks();
		            mDrawArea.setMolecule(mol);
		            }
		        }
		    };
		mCompoundPane.setSelectable(true);
		mainPanel.add(mCompoundPane, "3,1,3,5");
        applyCompoundPaneFilter();

        CompoundCollectionModel<BuildingBlock> model2 = new DefaultCompoundCollectionModel<BuildingBlock>() {
            public StereoMolecule getMolecule(int index) {
                return ((BuildingBlock)super.getCompound(index)).getMoleculeForEdit();
                }

            public StereoMolecule getMoleculeForDisplay(int index) {
                return ((BuildingBlock)super.getCompound(index)).getMoleculeForDisplay();
                }

            public void addMolecule(int index, StereoMolecule mol) {
                showMessageDialog("You cannot create new fragments this way.\nPlease select a fragment above and optionally modify it.");
                }

            public void setMolecule(int index, StereoMolecule mol) {
                showMessageDialog("Sorry, fragment modification is not supported yet.");
//              super.setCompound(index, new BuildingBlock(mol, mCurrentBBType));
                // TODO add connectivity information
                }
            };
		mBuildingBlockPane = new CompoundCollectionPane<BuildingBlock>(model2, false);
		mBuildingBlockPane.setEditable(true);
        mainPanel.add(mBuildingBlockPane, "5,5,9,5");

        mProgressPanel = new JProgressPanel(false);

        JPanel ibp = new JPanel();
		ibp.setLayout(new GridLayout(1, 2, 8, 0));
		JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(this);
		ibp.add(bcancel);
		mButtonAnalyze = new JButton("Analyze");
		mButtonAnalyze.addActionListener(this);
		mButtonAnalyze.setEnabled(false);
		ibp.add(mButtonAnalyze);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.add(ibp, BorderLayout.EAST);
        buttonPanel.add(mProgressPanel, BorderLayout.WEST);
        mainPanel.add(buttonPanel, "5,7,9,7");

        getContentPane().add(mainPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(owner);
	    setVisible(true);
		}

    private void configureComboBoxABB() {
        mComboBoxABB.removeItemListener(this);

        mComboBoxABB.removeAllItems();
        for (AbstractBuildingBlock abb:mABBList)
            mComboBoxABB.addItem(getComboBoxOption(abb));
        mComboBoxABB.addItem(getComboBoxOption(null));

        mComboBoxABB.setSelectedItem(getComboBoxOption(mCurrentABB));

        mComboBoxABB.addItemListener(this);
        }

    private void addComboBoxCompoundFilterOption(int bbID) {
        String item = COMPOUND_FILTER_OPTION_BB+bbID;
        mComboBoxCompoundFilter.addItem(item);
        mComboBoxCompoundFilter.setSelectedItem(item);
        }

    private String getComboBoxOption(AbstractBuildingBlock abb) {
        return (abb != null) ?
            OPTION_BUILDING_BLOCK+abb.getID()
          : OPTION_BUILDING_BLOCK+AbstractBuildingBlock.getNextID()+OPTION_SUFFIX_NEW;
        }

    private void applyCompoundPaneFilter() {
        int option = mComboBoxCompoundFilter.getSelectedIndex();
        int bbID = (option < COMPOUND_FILTER_OPTIONS.length) ? -1
                 : Integer.parseInt(((String)mComboBoxCompoundFilter.getSelectedItem()).substring(COMPOUND_FILTER_OPTION_BB.length()));
        ArrayList<SARSourceRecord> recordList = new ArrayList<SARSourceRecord>();
        for (SARSourceRecord r:mSourceRecord)
            if (option == OPTION_ALL_COMPOUNDS
             || (option == OPTION_MATCHING_COMPOUNDS) && r.isMatchingAny()
             || (option == OPTION_NON_MATCHING_COMPOUNDS) && !r.isMatchingAny()
             || (bbID != -1 && r.isMatching(bbID)))
                recordList.add(r);

        mCompoundPane.getModel().setCompoundList(recordList);
        }

    private void addSelectedBBToList() {
        new Thread(new Runnable() {
            public void run() {
                final BuildingBlock bb = createAndMatchSelectionAsBB();
                if (bb != null) {
                    try {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                mBuildingBlockPane.getModel().addCompound(bb);
                    
                                if (mCurrentABB == null) {
                                    mCurrentABB = new AbstractBuildingBlock();
                                    mCurrentABB.add(bb);
                                    mABBList.add(mCurrentABB);
                                    configureComboBoxABB();
                                    addComboBoxCompoundFilterOption(mCurrentABB.getID());
                                    }
                                else {
                                    mCurrentABB.add(bb);
                                    }

                                // apply currently defined building blocks to current reference molecule
                                mCurrentMoleculeContext.matchBuildingBlocks(mABBList);
                                mCurrentMoleculeContext.colorizeBuildingBlocks();
                                mDrawArea.getMolecule().removeAtomSelection();
                                mDrawArea.moleculeChanged();
                                mButtonAddBuildingBlock.setEnabled(false);
                                mButtonAnalyze.setEnabled(true);

                                mCompoundPane.collectionUpdated(0, Integer.MAX_VALUE);
                                mStatusPanel.update(mABBList);
                                }
                            } );
                        }
                    catch (Exception e) {
                        e.printStackTrace();
                        }
                    }
                }
            } ).start();
        }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == mButtonAddBuildingBlock) {
            addSelectedBBToList();
            return;
            }

        if (e.getSource() == mButtonAnalyze) {
			if (true) { //TODO check stuff
			    if (checkForCompleteMatches()) {
    			    mProgressDialog = new JProgressDialog(mParentFrame);
    
    				Thread t = new Thread(this, "DEInteractiveSARAnalyzer");
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.start();
			        }
				}
			}
		setVisible(false);
		dispose();
		}

    private boolean checkForCompleteMatches() {
        int missingBBID = Integer.MAX_VALUE;
        int missingBBCount = 0;
        for (SARSourceRecord sr:mSourceRecord) {
            if (sr.isMatchingAny() && !sr.isMatchingAll(mABBList)) {
                for (AbstractBuildingBlock abb:mABBList) {
                    if (!sr.isMatching(abb.getID())) {
                        if (missingBBID > abb.getID()) {
                            missingBBID = abb.getID();
                            missingBBCount = 0;
                            }
                        if (missingBBID == abb.getID()) {
                            missingBBCount++;
                            }
                        }
                    }
                }
            }

        if (missingBBID != Integer.MAX_VALUE) {
            StringBuffer parentBuf = new StringBuffer();
            for (AbstractBuildingBlock abb:mABBList) {
                if (abb.getID() == missingBBID) {
                    for (BBConnection bbc:abb.getConnectionList()) {
                        if (!bbc.isDownwardsConnection()) {
                            if (parentBuf.length() != 0)
                                parentBuf.append(" and ");
                            parentBuf.append("B"+bbc.bbID+":"+(bbc.anchorNo+1));
                            }
                        }
                    }
                }
            String[] option = {"Cancel","Skip Incomplete","Analyze All"};
            Object selectedOption = JOptionPane.showInputDialog(mParentFrame,
                    "B"+missingBBID+" connected to "+parentBuf.toString()
                    + " cannot be found in "+missingBBCount+" molecule(s).\n"
                    + "Do you want to skip records with incompletely matching building blocks?",
                    "Building block missing",
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    option,
                    option[1]
                    );
            mSkipIncompletelyMatching = selectedOption.equals(option[1]);
            return !selectedOption.equals(option[0]);
            }

        return true;
        }

    public void contentChanged(DrawAreaEvent e) {
        if (e.getType() == DrawAreaEvent.TYPE_SELECTION_CHANGED) {
            int selectedAtomCount = 0;
            boolean selectionModified = false;
            StereoMolecule mol = mDrawArea.getMolecule();
            for (int atom=0; atom<mol.getAtoms(); atom++) {
                if (mol.isSelectedAtom(atom)) {
                    if (mCurrentMoleculeContext.isUsedAtom(atom)) {
                        mol.setAtomSelection(atom, false);
                        selectionModified = true;
                        }
                    else {
                        selectedAtomCount++;
                        }
                    }
                }
            if (selectionModified)
                mDrawArea.repaint();

            boolean selectedAtomCountOK = (selectedAtomCount > 0 && selectedAtomCount < mol.getAllAtoms());

            ArrayList<BBConnection> connectionList = getUpwardsConnectionsFromSelection(mol);
            boolean connectionOK = mCurrentMoleculeContext.getMatchingAtomCount() == 0 || connectionList.size() != 0;

            if (selectedAtomCountOK && connectionOK) {
                mButtonAddBuildingBlock.setEnabled(true);

                AbstractBuildingBlock selectedABB = null;
                if (mCurrentMoleculeContext.getMatchingAtomCount() == 0 && mABBList.size() != 0) {
                    selectedABB = mABBList.get(0);
                    }
                else if (connectionList.size() != 0) {
                    for (AbstractBuildingBlock abb:mABBList) {
                        if (abb.matchesUpwardsConnections(connectionList)) {
                            selectedABB = abb;
                            break;
                            }
                        }
                    }

                setCurrentABB(selectedABB);
                }
            else {
                mButtonAddBuildingBlock.setEnabled(false);
                }
            }
        }

    /**
     * Set current abstract building block.
     * Update UI elements (combobox and bb pane) accordingly.
     * @param abb existing ABB or null for new one
     */
    private void setCurrentABB(AbstractBuildingBlock abb) {
        mComboBoxABB.setSelectedItem(getComboBoxOption(abb));
        mCurrentABB = abb;
        }

    private void showMessageDialog(String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            JOptionPane.showMessageDialog(mParentFrame, message);
            }
        else {
                // if we are not in the event dispatcher thread we need to use invokeAndWait
            final String _message = message;
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(mParentFrame, _message);
                        }
                    } );
                }
            catch (Exception e) {
                }
            }
        }

    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == mComboBoxABB
         && e.getStateChange() == ItemEvent.SELECTED) {
            String item = (String)mComboBoxABB.getSelectedItem();

            AbstractBuildingBlock selectedABB = null;
            for (AbstractBuildingBlock abb:mABBList) {
                if (getComboBoxOption(abb).equals(item)) {
                    selectedABB = abb;
                    break;
                    }
                }

            // update model of buildingBlockPane to show correct list
            if (mCurrentABB != selectedABB) {
                mCurrentABB = selectedABB;

                mBuildingBlockPane.getModel().clear();
                if (mCurrentABB != null)
                    for (BuildingBlock bb:mCurrentABB)
                        mBuildingBlockPane.getModel().addCompound(bb);
                }
            }
        else if (e.getSource() == mComboBoxCompoundFilter
              && e.getStateChange() == ItemEvent.SELECTED) {
            applyCompoundPaneFilter();
            }
        }

    private BuildingBlock createAndMatchSelectionAsBB() {
        StereoMolecule mol = mDrawArea.getMolecule();
        mol.ensureHelperArrays(Molecule.cHelperNeighbours);

        int selectedAtomCount = 0;
        boolean[] includeAtom = new boolean[mol.getAtoms()];
        for (int atom=0; atom<mol.getAtoms(); atom++) {
            if (mol.isSelectedAtom(atom)) {
                selectedAtomCount++;
                includeAtom[atom] = true;
                }
            }

        // copy selected atoms as new building block molecule
        int bondCount = 0;
        for (int bond=0; bond<mol.getBonds(); bond++)
            if (mol.isSelectedAtom(mol.getBondAtom(0, bond))
             && mol.isSelectedAtom(mol.getBondAtom(1, bond)))
                bondCount++;
        StereoMolecule bbMol = new StereoMolecule(selectedAtomCount, bondCount);
        int[] atomMap = new int[includeAtom.length];
        mol.copyMoleculeByAtoms(bbMol, includeAtom, true, atomMap);
        bbMol.removeAtomSelection();
        bbMol.setFragment(true);

        // ensure that we don't have multiple fragments
        bbMol.ensureHelperArrays(Molecule.cHelperNeighbours);
        int[] fragmentNo = new int[bbMol.getAtoms()];
        int fragmentCount = bbMol.getFragmentNumbers(fragmentNo, false, true);
        if (fragmentCount != 1) {
            showMessageDialog("Your selection covers disconnected areas.");
            return null;
            }

        // find upwards connections
        ArrayList<BBConnection> connectionList = getUpwardsConnectionsFromSelection(mol);
        for (BBConnection bbc:connectionList)
            bbc.atom = atomMap[bbc.atom];   // translate atom indices into fragment space

        // find downwards connections
        int upwardConnectionCount = connectionList.size();
        boolean[] recordMatches = findDownwardsConnections(bbMol, connectionList);
        if (upwardConnectionCount == connectionList.size()) {
            showMessageDialog("This building block has no substituent in any molecule.\nYou don't need to define it as building block.\nIt will be automatically found as substituent.");
            return null;
            }

        // remove atomConfiguration:unknown for atoms that aren't stereo centers anymore
        bbMol.ensureHelperArrays(Molecule.cHelperParities);
        for (int atom=0; atom<bbMol.getAtoms(); atom++)
            if (bbMol.isAtomConfigurationUnknown(atom)
             && !bbMol.isAtomStereoCenter(atom))
                bbMol.setAtomConfigurationUnknown(atom, false);

        int index = 0;
        for (SARSourceRecord sr:mSourceRecord)
            if (recordMatches[index++])
                sr.addMatchingBB(mCurrentABB);

        return new BuildingBlock(bbMol, connectionList.toArray(new BBConnection[0]), mSearcher);
        }

    private ArrayList<BBConnection> getUpwardsConnectionsFromSelection(StereoMolecule mol) {
        ArrayList<BBConnection> connectionList = new ArrayList<BBConnection>();
        for (int atom=0; atom<mol.getAtoms(); atom++) {
            if (mol.isSelectedAtom(atom)) {
                for (int i=0; i<mol.getConnAtoms(atom); i++) {
                    int connAtom = mol.getConnAtom(atom, i);
                    if (!mol.isSelectedAtom(connAtom)
                     && mCurrentMoleculeContext.isUsedAtom(connAtom)) {
                        int connBond = mol.getConnBond(atom, i);
                        int bondType = BBConnection.createBondType(mol, connBond);
                        BBConnection connection = mCurrentMoleculeContext.getDownwardsConnection(connAtom, bondType);
                        connectionList.add(new BBConnection(atom,
                                                            mCurrentMoleculeContext.getBBID(connAtom),
                                                            connection.anchorNo,
                                                            bondType));
                        }
                    }
                }
            }
        return connectionList;
        }

    private boolean[] findDownwardsConnections(StereoMolecule bbMol, ArrayList<BBConnection> connectionList) {
        int[] upwardConnectionCount = new int[bbMol.getAtoms()];
        int[][] bondType = new int[bbMol.getAtoms()][];
        boolean[] recordMatches = new boolean[mSourceRecord.length];

        for (BBConnection c:connectionList)
            upwardConnectionCount[c.atom]++;

        int recordIndex = -1;
        mProgressPanel.startProgress("", 0, mSourceRecord.length);
        long[] bbFragFp = mSearcher.createLongIndex(bbMol);
        for (SARSourceRecord sr:mSourceRecord) {
            mProgressPanel.updateProgress(++recordIndex);
            MoleculeContext context = sr.matchBuildingBlocks(mSearcher, mIDCodeColumn, mFragFpColumn, mABBList);
            int[] bestMatch = context.getPreferredMatch(bbMol, bbFragFp, connectionList.toArray(new BBConnection[0]));
            if (bestMatch != null) {
                recordMatches[recordIndex] = true;

                ExtendedMolecule mol = context.getMolecule();
                boolean[] isBBMember = new boolean[mol.getAtoms()];
                for (int atom=0; atom<bbMol.getAtoms(); atom++)
                    isBBMember[bestMatch[atom]] = true;
                for (int atom=0; atom<bbMol.getAtoms(); atom++) {
                    int downwardConnectionCount = mol.getConnAtoms(bestMatch[atom])
                                                - bbMol.getConnAtoms(atom)
                                                - upwardConnectionCount[atom];
                    if (downwardConnectionCount != 0) {
                        int[] bt = new int[downwardConnectionCount];
                        int index = 0;
                        for (int i=0; i<mol.getConnAtoms(bestMatch[atom]); i++) {
                            int connAtom = mol.getConnAtom(bestMatch[atom], i);
                            if (!isBBMember[connAtom] && !context.isUsedAtom(connAtom)) {
                                int connBond = mol.getConnBond(bestMatch[atom], i);
                                bt[index++] = BBConnection.createBondType(mol, connBond);
                                }
                            }
                        java.util.Arrays.sort(bt);
                        if (bondType[atom] == null) {
                            bondType[atom] = bt;
                            }
                        else {
                            // first make sure that bt is not the larger array
                            if (bondType[atom].length < bt.length) {
                                int[] temp = bondType[atom];
                                bondType[atom] = bt;
                                bt = temp;
                                }
                            int shift = 0;
                            for (int i=0; i<bt.length; i++) {
                                while ((bt[i] > bondType[atom][i+shift])
                                    && (shift < bondType[atom].length - bt.length))
                                    shift++;
                                bondType[atom][i+shift] |= bt[i];
                                }
                            }
                        }
                    }
                }
            }
        mProgressPanel.stopProgress();

        int pointNo = 0;
        for (int atom=0; atom<bbMol.getAtoms(); atom++)
            if (bondType[atom] != null)
                for (int i=0; i<bondType[atom].length; i++)
                    connectionList.add(new BBConnection(atom,
                                                        pointNo++,
                                                        bondType[atom][i]));

        return recordMatches;
        }

	public void run() {
		runAnalysis();

		mProgressDialog.stopProgress();
	    mProgressDialog.close(null);
		}

	private void runAnalysis() {
        // bbIndex translates from bbID to a zero based building block index
        int[] bbIndex = new int[AbstractBuildingBlock.getNextID()];
        for (int i=0; i<mABBList.size(); i++)
            bbIndex[mABBList.get(i).getID()] = i;

	    ArrayList<String> newColumnNameList = new ArrayList<String>();
	    int sarSeriesColumn = getColumnIndex("SAR Series", false, newColumnNameList);
	    int[] buildingBlockColumn = new int[mABBList.size()];
	    for (int i=0; i<buildingBlockColumn.length; i++)
	        buildingBlockColumn[i] = getColumnIndex("B "+mABBList.get(i).getID(), true, newColumnNameList);

	    boolean[][] substituentIsBB = new boolean[mABBList.size()][];
        for (int i=0; i<buildingBlockColumn.length; i++)
            substituentIsBB[i] = new boolean[mABBList.get(i).getDownwardConnectionCount()];
        for (int i=0; i<buildingBlockColumn.length; i++)
            for (BBConnection bbc:mABBList.get(i).getConnectionList())
                if (!bbc.isDownwardsConnection())
                    substituentIsBB[bbIndex[bbc.bbID]][bbc.anchorNo] = true;

	    int[][] substituentColumn = new int[mABBList.size()][];
        for (int i=0; i<buildingBlockColumn.length; i++) {
            substituentColumn[i] = new int[mABBList.get(i).getDownwardConnectionCount()];
            for (int j=0; j<substituentColumn[i].length; j++)
                if (!substituentIsBB[i][j])
                    substituentColumn[i][j] = getColumnIndex("R"+(j+1)+" at B"+mABBList.get(i).getID(), true, newColumnNameList);
            }

        int sarSeriesNo = 1;
        if (sarSeriesColumn < mTableModel.getTotalColumnCount()) {
            for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
                int series = (int)mTableModel.getDoubleAt(row, sarSeriesColumn);
                if (!Double.isNaN(series) && sarSeriesNo <= series)
                    sarSeriesNo = series + 1;
                }
            }

        int firstNewColumn = Integer.MAX_VALUE;
        if (newColumnNameList.size() != 0)
            firstNewColumn = mTableModel.addNewColumns(newColumnNameList.toArray(new String[0]));

        mProgressDialog.startProgress("Analyzing substituents...", 0, mSourceRecord.length);

        StereoMolecule fragment = new StereoMolecule();
	    for (SARSourceRecord sr:mSourceRecord) {
	        if (mProgressDialog.threadMustDie())
	            break;
	        mProgressDialog.updateProgress(-1);

	        MoleculeContext context = sr.matchBuildingBlocks(mSearcher, mIDCodeColumn, mFragFpColumn, mABBList);
	        for (int i=0; i<buildingBlockColumn.length; i++) {
	            AbstractBuildingBlock abb = mABBList.get(i);
	            if (sr.isMatching(abb.getID())
	             && (sr.isMatchingAll(mABBList) || !mSkipIncompletelyMatching)) {
	                sr.getCompoundRecord().setData((""+sarSeriesNo).getBytes(), sarSeriesColumn);

	                context.getRealBuildingBlock(fragment, abb.getID());
	                String idcode = new Canonizer(fragment, Canonizer.ENCODE_ATOM_CUSTOM_LABELS).getIDCode();
	                sr.getCompoundRecord().setData(idcode.getBytes(), buildingBlockColumn[i]);
	                mTableModel.removeChildDescriptorsAndCoordinates(sr.getCompoundRecord(), buildingBlockColumn[i]);

	                for (int j=0; j<substituentColumn[i].length; j++) {
	                    if (!substituentIsBB[i][j]) {
    	                    context.getRealSubstituent(fragment, abb.getID(), j);
    	                    if (fragment.getAllAtoms() != 0) {
    	                        idcode = new Canonizer(fragment, Canonizer.ENCODE_ATOM_CUSTOM_LABELS).getIDCode();
    	                        sr.getCompoundRecord().setData(idcode.getBytes(), substituentColumn[i][j]);
    	    	                mTableModel.removeChildDescriptorsAndCoordinates(sr.getCompoundRecord(), substituentColumn[i][j]);
    	                        }
	                        }
	                    }
	                }
	            }
	        }

	    if (sarSeriesColumn < firstNewColumn)
	        mTableModel.finalizeChangeAlphaNumericalColumn(sarSeriesColumn, 0, mTableModel.getTotalRowCount());
        for (int i=0; i<buildingBlockColumn.length; i++) {
            if (buildingBlockColumn[i] < firstNewColumn)
                mTableModel.finalizeChangeChemistryColumn(buildingBlockColumn[i], 0, mTableModel.getTotalRowCount(), false);
            else
                mTableModel.setColumnProperty(buildingBlockColumn[i], CompoundTableModel.cColumnPropertySpecialType, CompoundTableModel.cColumnTypeIDCode);

            for (int j=0; j<substituentColumn[i].length; j++) {
                if (!substituentIsBB[i][j]) {
                    if (substituentColumn[i][j] < firstNewColumn)
                        mTableModel.finalizeChangeChemistryColumn(substituentColumn[i][j], 0, mTableModel.getTotalRowCount(), false);
                    else
                        mTableModel.setColumnProperty(substituentColumn[i][j], CompoundTableModel.cColumnPropertySpecialType, CompoundTableModel.cColumnTypeIDCode);
                    }
                }
            }
                
	    if (newColumnNameList.size() != 0)
	        mTableModel.finalizeNewColumns(firstNewColumn, mProgressDialog);
		}

	private int getColumnIndex(String columnName, boolean isStructureColumn, ArrayList<String> newColumnTitleList) {
	    int column = mTableModel.findColumn(columnName);
        if (column != -1 && isStructureColumn) {
            String specialType = mTableModel.getColumnSpecialType(column);
            if (specialType == null || !specialType.equals(CompoundTableModel.cColumnTypeIDCode))
                column = -1;
            }
	    if (column == -1) {
	        column = mTableModel.getTotalColumnCount() + newColumnTitleList.size();
            newColumnTitleList.add(columnName);
	        }
	    return column;
	    }
    }


class BBConnection {
    private int allowedBondTypes;

    private static final int BB_ID_DOWNWARDS_CONNECTION = -2;
    public int atom;
    public int bbID;
    public int anchorNo;
    public boolean isMatched;

    /**
     * Constructs a new connection by cloning connection, but using atom
     * @param atom atom within fragment that carries/requires the upward connection
     * @param connection 
     */
    public BBConnection(int atom, BBConnection connection) {
        this.atom = atom;
        this.bbID = connection.bbID;
        this.anchorNo = connection.anchorNo;
        this.allowedBondTypes = connection.allowedBondTypes;
        }

    /**
     * Constructs an upwards connection constraint/description for a building block
     * @param atom atom within fragment that carries/requires the upward connection
     * @param bbIndex fragment index of externally connected fragment
     * @param bbAnchorNo anchor no of externally connected fragment
     * @param allowedBondTypes one or more bond types allowed for this connection
     */
    public BBConnection(int atom, int bbIndex, int bbAnchorNo, int allowedBondTypes) {
        this.atom = atom;
        this.bbID = bbIndex;
        this.anchorNo = bbAnchorNo;
        this.allowedBondTypes = allowedBondTypes;
        }

    /**
     * Constructs a downwards connection constraint/description for a building block
     * @param atom atom within fragment that carries/requires the downward connection
     * @param bbAnchorNo anchor no of externally connected fragment
     * @param allowedBondTypes one or more bond types allowed for this connection
     */
    public BBConnection(int atom, int bbAnchorNo, int allowedBondTypes) {
        this.atom = atom;
        this.bbID = BB_ID_DOWNWARDS_CONNECTION;
        this.anchorNo = bbAnchorNo;
        this.allowedBondTypes = allowedBondTypes;
        }

    public boolean isDownwardsConnection() {
        return (bbID == BB_ID_DOWNWARDS_CONNECTION);
        }

    public boolean bondTypeMatches(int bondType) {
        return (bondType & ~allowedBondTypes) == 0;
        }

    public int getDisplayBondType() {
        switch (allowedBondTypes) {
        case Molecule.cBondTypeSingle:
        case Molecule.cBondTypeDouble:
        case Molecule.cBondTypeTriple:
        case Molecule.cBondTypeDelocalized:
            return allowedBondTypes;
        default:
            if ((allowedBondTypes & Molecule.cBondTypeSingle) != 0)
                return Molecule.cBondTypeSingle;
            if ((allowedBondTypes & Molecule.cBondTypeDelocalized) != 0)
                return Molecule.cBondTypeDelocalized;
            return Molecule.cBondTypeDouble;
            }
        }

    public int getDisplayBondQueryFeatures() {
        switch (allowedBondTypes) {
        case Molecule.cBondTypeSingle:
        case Molecule.cBondTypeDouble:
        case Molecule.cBondTypeTriple:
        case Molecule.cBondTypeDelocalized:
        case Molecule.cBondTypeMetalLigand:
            return 0;
        default:
            return (((allowedBondTypes & Molecule.cBondTypeSingle) == 0) ? 0 : Molecule.cBondQFSingle)
                 + (((allowedBondTypes & Molecule.cBondTypeDouble) == 0) ? 0 : Molecule.cBondQFDouble)
                 + (((allowedBondTypes & Molecule.cBondTypeTriple) == 0) ? 0 : Molecule.cBondQFTriple)
                 + (((allowedBondTypes & Molecule.cBondTypeDelocalized) == 0) ? 0 : Molecule.cBondQFDelocalized)
                 + (((allowedBondTypes & Molecule.cBondTypeMetalLigand) == 0) ? 0 : Molecule.cBondTypeMetalLigand);
            }
        }

    public static int createBondType(ExtendedMolecule mol, int bond) {
        return (mol.isDelocalizedBond(bond)) ? Molecule.cBondTypeDelocalized
                                             : mol.getBondTypeSimple(bond);
        }
    }


class AbstractBuildingBlock extends ArrayList<BuildingBlock> {
    private static final long serialVersionUID = 0x20090611;
    private static int sNextBuildingBlockID;

    private int mID;

    public static void initializeIDs() {
        sNextBuildingBlockID = 1;
        }

    public static int getNextID() {
        return sNextBuildingBlockID;
        }

    public AbstractBuildingBlock() {
        super();
        mID = sNextBuildingBlockID++;
        }

    public int getID() {
        return mID;
        }

    public BBConnection[] getConnectionList() {
        // TODO maintain connection information in this class rather than in individual building blocks
        return get(0).getConnectionList();
        }

    public int getDownwardConnectionCount() {
        // TODO maintain connection information in this class rather than in individual building blocks
        int count = 0;
        BBConnection[] connection = get(0).getConnectionList();
        for(BBConnection bbc:connection)
            if (bbc.isDownwardsConnection())
                count++;
        return count;
        }

    public boolean matchesUpwardsConnections(ArrayList<BBConnection> bbcl) {
        // TODO maintain connection information in this class rather than in individual building blocks
        BBConnection[] connection = get(0).getConnectionList();
        int foundCount = 0;
        for (int i=0; i<connection.length; i++) {
            if (!connection[i].isDownwardsConnection()) {
                boolean found = false;
                for (BBConnection bbc:bbcl) {
                    if (connection[i].bbID == bbc.bbID
                     && connection[i].anchorNo == bbc.anchorNo) {
                        found = true;
                        break;
                        }
                    }
                if (!found)
                    return false;
                foundCount++;
                }
            }
        return (foundCount == bbcl.size());
        }
    }

class BuildingBlock {
    private StereoMolecule mDisplayMol,mSSSQuery;
    private BBConnection[] mConnection;
    private long[] mFragFp;

    public BuildingBlock(StereoMolecule query, BBConnection[] connection, SSSearcherWithIndex searcher) {
        mConnection = connection;
        query.ensureHelperArrays(Molecule.cHelperNeighbours);
        createDisplayMol(query);

        mSSSQuery = query;
        addConstraintQueryFeatures();
        mFragFp = searcher.createLongIndex(mSSSQuery);
        }

    public BBConnection[] getConnectionList() {
        return mConnection;
        }

    public StereoMolecule getMoleculeForQuery() {
        return mSSSQuery;
        }

    /**
     * Adds upwards connection constraints as labeled atoms to the query fragment
     * @return query fragment with added and labeled upwards anchor atoms
     */
    public StereoMolecule getMoleculeForEdit() {
        StereoMolecule editMol = new StereoMolecule(mSSSQuery);
        for (int i=0; i<mConnection.length; i++) {
            if (!mConnection[i].isDownwardsConnection()) {
                int atom = editMol.addAtom(0);
                editMol.setAtomCustomLabel(atom, "B"+mConnection[i].bbID+":"+(mConnection[i].anchorNo+1));
                editMol.addBond(mConnection[i].atom, atom, mConnection[i].getDisplayBondType());
                }
            }
        new CoordinateInventor().invent(editMol);
        return editMol;
        }

    public StereoMolecule getMoleculeForDisplay() {
        return mDisplayMol;
        }

    public long[] getFragFp() {
        return mFragFp;
        }

    /**
     * Check whether upward connection constraints can potentially satisfied
     * with the given list of available (already matched) building blocks.
     * @param bbIndexList list of building blocks already matched in a MoleculeContext
     * @return true if this building qualifies as a match
     */
    public boolean qualifiesAsMatch(int[] bbIndexList) {
        for (BBConnection connection:mConnection) {
            if (!connection.isDownwardsConnection()) {
                // If this building block requires a connection to
                // a previously matched building block, then bbIndexList
                // must contain it. Otherwise a match is not possible.
                boolean found = false;
                if (bbIndexList != null) {
                    for (int i=0; i<bbIndexList.length; i++) {
                        if (bbIndexList[i] == connection.bbID) {
                            found = true;
                            break;
                            }
                        }
                    }
                if (!found)
                    return false;
                }
            }
        return true;
        }

    private void createDisplayMol(StereoMolecule mol) {
        mDisplayMol = new StereoMolecule(mol.getAtoms()+mConnection.length,
                                        mol.getBonds()+mConnection.length);
        mol.copyMolecule(mDisplayMol);
        for (int i=0; i<mConnection.length; i++) {
            int atom = mDisplayMol.addAtom(0);
            if (mConnection[i].isDownwardsConnection())
                mDisplayMol.setAtomCustomLabel(atom, "R"+(mConnection[i].anchorNo+1));
            else
                mDisplayMol.setAtomCustomLabel(atom, "B"+mConnection[i].bbID+":"+(mConnection[i].anchorNo+1));
            
            int bond = mDisplayMol.addBond(mConnection[i].atom, atom, mConnection[i].getDisplayBondType());
            mDisplayMol.setBondQueryFeature(bond, mConnection[i].getDisplayBondQueryFeatures(), true);
            }
        new CoordinateInventor().invent(mDisplayMol);
        }

    private void addConstraintQueryFeatures() {
        // Mark all atoms with upwards connection constraint to require another neighbour
        for (BBConnection c:mConnection)
            if (!c.isDownwardsConnection())
                mSSSQuery.setAtomQueryFeature(c.atom, Molecule.cAtomQFMoreNeighbours, true);
        }
    }


class MoleculeContext {
    // Colors should match SARStatusPanel.BB_COLOR
    private static final int[] BB_ATOM_COLOR = { Molecule.cAtomColorDarkRed,
                                                 Molecule.cAtomColorOrange,
                                                 Molecule.cAtomColorGreen,
                                                 Molecule.cAtomColorDarkGreen,
                                                 Molecule.cAtomColorMagenta,
                                                 Molecule.cAtomColorBlue
                                                 };
    private int mMatchingAtomCount,mAtomCount;
    private int[] mBBID;
    private ArrayList<BBConnection> mList;
    private boolean[] mAtomUsed;
    private SSSearcherWithIndex mSearcher;

    public static int getAtomColor(int bbID) {
        return BB_ATOM_COLOR[bbID % BB_ATOM_COLOR.length];
        }

    public MoleculeContext(byte[] idcode, long[] fragFp) {
        this(new SSSearcherWithIndex(), idcode, fragFp);
        }

    public MoleculeContext(SSSearcherWithIndex searcher, byte[] idcode, long[] fragFp) {
        mSearcher = searcher;
        mSearcher.setMolecule(idcode, fragFp);
        mAtomCount = new IDCodeParser().getAtomCount(idcode, 0);
        }

    public MoleculeContext(StereoMolecule mol, long[] fragFp) {
        this(new SSSearcherWithIndex(), mol, fragFp);
        }

    public MoleculeContext(SSSearcherWithIndex searcher, StereoMolecule mol, long[] fragFp) {
        mSearcher = searcher;
        mSearcher.setMolecule(mol, fragFp);
        mol.ensureHelperArrays(Molecule.cHelperNeighbours);
        mAtomCount = mol.getAtoms();
        }

    private void init() {
        mBBID = new int[mAtomCount];
        mAtomUsed = new boolean[mAtomCount];
        mList = new ArrayList<BBConnection>();
        }

    public ExtendedMolecule getMolecule() {
        return mSearcher.getMolecule();
        }

    public int getBBID(int atom) {
        return mBBID[atom];
        }

    public BBConnection getDownwardsConnection(int atom, int bondType) {
        // TODO find a matching connection rather than taking the first
        for (BBConnection c:mList)
            if (c.atom == atom && c.bondTypeMatches(bondType))
                return c;
        return null;
        }

    public boolean isUsedAtom(int atom) {
        return mAtomUsed[atom];
        }

    public int getMatchingAtomCount() {
        return mMatchingAtomCount;
        }

    /**
     * Tries to match currently defined building blocks on given molecule.
     * If building blocks are matched, then describe the match in this MoleculeContext.
     * @param abbl
     */
    public int[] matchBuildingBlocks(ArrayList<AbstractBuildingBlock> abbl) {
        init();
        int[] bbIDList = null;
        mMatchingAtomCount = 0;
        for (AbstractBuildingBlock abb:abbl) {
            for (BuildingBlock bb:abb) {
                if (bb.qualifiesAsMatch(bbIDList)) {
                    int[] bestMatch = getPreferredMatch(bb.getMoleculeForQuery(), bb.getFragFp(), bb.getConnectionList());
    
                    // if we have a valid match
                    if (bestMatch != null) {
                        int bbID = abb.getID();
                        bbIDList = addIDToList(bbID, bbIDList);
    
                        for (int atom:bestMatch) {
                            mAtomUsed[atom] = true;
                            mBBID[atom] = bbID;
                            }
                        for (BBConnection c:bb.getConnectionList()) {
                            BBConnection constraint = new BBConnection(bestMatch[c.atom], c);
                            mList.add(constraint);
                            if (!c.isDownwardsConnection()) {
                                constraint.isMatched = true;
                                getMatchingDownwardsConnection(bestMatch[c.atom], c).isMatched = true;
                                }
                            }

                        mMatchingAtomCount += bestMatch.length;
                        if (mMatchingAtomCount == mAtomUsed.length)
                            return bbIDList;
    
                        break;
                        }
                    }
                }
            }
        return bbIDList;
        }

    public static int[] addIDToList(int bbID, int[] bbIDList) {
        int[] newBBIDList = null;
        if (bbIDList == null) {
            newBBIDList = new int[1];
            newBBIDList[0] = bbID;
            }
        else {
            newBBIDList = new int[bbIDList.length+1];
            for (int i=0; i<bbIDList.length; i++)
                newBBIDList[i] = bbIDList[i];
            newBBIDList[bbIDList.length] = bbID;
            }
        return newBBIDList;
        }

    public int[] getPreferredMatch(StereoMolecule bbMol, long[] bbFragFp, BBConnection[] connectionList) {
        mSearcher.setFragment(bbMol, bbFragFp);
        int matchCount = mSearcher.findFragmentInMolecule(SSSearcher.cCountModeRigorous,
                                                          SSSearcher.cDefaultMatchMode,
                                                          mAtomUsed);
        int[] bestMatch = null;
        int bestMatchValue = Integer.MAX_VALUE;
        for (int i=0; i<matchCount; i++) {
            ExtendedMolecule mol = mSearcher.getMolecule();
            int[] matchingAtom = mSearcher.getMatchList().get(i);

            boolean constraintsViolated = false;
            for (BBConnection c:connectionList) {
                if (!c.isDownwardsConnection()) {
                    // If this building block requires a connection to
                    // a previously matched building block, then find
                    // the connection atom of the latter. This must have
                    // been matched before and therefore must have the
                    // atomExcluded flag set.

                    if (getMatchingDownwardsConnection(matchingAtom[c.atom], c) == null) {
                        constraintsViolated = true;
                        break;
                        }
                    }
                }

            if (constraintsViolated)
                continue;

            // If we have multiple matches that satisfy the connection
            // constraints to other building blocks, then take that match
            // where substituents are at lowest indices of building block atoms.
            int matchValue = 0;
            for (int j=0; j<matchingAtom.length; j++) {
                if (mol.getConnAtoms(matchingAtom[j]) > bbMol.getConnAtoms(j))
                    matchValue += (matchingAtom.length > 31) ? j : (1 << j);
                }
            if (bestMatchValue > matchValue) {
                bestMatchValue = matchValue;
                bestMatch = matchingAtom;
                }
            }

        return bestMatch;
        }

    private BBConnection getMatchingDownwardsConnection(int atom, BBConnection upwardsConnection) {
        ExtendedMolecule mol = mSearcher.getMolecule();
        for (int j=0; j<mol.getConnAtoms(atom); j++) {
            if (mAtomUsed[mol.getConnAtom(atom, j)]) {
                int connAtom = mol.getConnAtom(atom, j);
                int connBond = mol.getConnBond(atom, j);
                if (mBBID[connAtom] == upwardsConnection.bbID
                 && upwardsConnection.bondTypeMatches(BBConnection.createBondType(mol, connBond))) {
                    for (BBConnection downwardsConnection:mList)
                        // TODO return the closest match, not just one
                        if (!downwardsConnection.isMatched
                         && downwardsConnection.atom == connAtom
                         && downwardsConnection.anchorNo == upwardsConnection.anchorNo)
                            return downwardsConnection;
                    }
                }
            }
        return null;
        }

    /**
     * Colorizes atoms in the context's refMol that are part
     * of building blocks defined in the MoleculeContext.
     */
    public void colorizeBuildingBlocks() {
        ExtendedMolecule mol = mSearcher.getMolecule();
        mol.removeAtomColors();
        for (int atom=0; atom<mol.getAtoms(); atom++) {
            if (mAtomUsed[atom])
                mol.setAtomColor(atom, getAtomColor(mBBID[atom]));
            }
        }

    /**
     * Returns the structure of the real fragment that was matched by
     * one of the BuildingBlocks representing the AbstractBuildingBlock
     * with the id bbID. For every potential substituent an R-group is
     * attached to the fragment structure before the latter is returned.
     * @param bbID
     * @return
     */
    public void getRealBuildingBlock(StereoMolecule bb, int bbID) {
        ExtendedMolecule mol = mSearcher.getMolecule();
        boolean[] includeAtom = new boolean[mol.getAtoms()];
        for (int atom=0; atom<mol.getAtoms(); atom++)
            includeAtom[atom] = (bbID == mBBID[atom]);
        int[] atomMap = new int[includeAtom.length];
        mol.copyMoleculeByAtoms(bb, includeAtom, true, atomMap);
        for (BBConnection c:mList) {
            if (includeAtom[c.atom]) {
                int newAtom = -1;
                int anchorNo = c.anchorNo;
                if (c.isDownwardsConnection()) {
                    newAtom = bb.addAtom(142+anchorNo-((anchorNo<3)?0:16));
                    }
                else {
                    newAtom = bb.addAtom(0);
                    bb.setAtomCustomLabel(newAtom, "B"+c.bbID+":"+(c.anchorNo+1));
                    }
                bb.addBond(atomMap[c.atom], newAtom, c.getDisplayBondType());
                }
            }
        }

    public void getRealSubstituent(StereoMolecule substituent, int bbID, int anchorNo) {
        StereoMolecule mol = (StereoMolecule)mSearcher.getMolecule();
        substituent.deleteMolecule();
        for (BBConnection c:mList) {
            if (!c.isMatched
             && mBBID[c.atom] == bbID
             && c.anchorNo == anchorNo) {
                for (int i=0; i<mol.getConnAtoms(c.atom); i++) {
                    int connAtom = mol.getConnAtom(c.atom, i);
                    // TODO matches the first unused atom to the first anchor. Might be done better
                    if (!mAtomUsed[connAtom]) {
                        int bondType = BBConnection.createBondType(mol, mol.getConnBond(c.atom, i));
                        if (c.bondTypeMatches(bondType)) {
                            c.isMatched = true;
                            getSubstituent(mol, substituent, c.atom, connAtom);
                            return;
                            }
                        }
                    }
                }
            }
        }

    public void getSubstituent(StereoMolecule mol, StereoMolecule substituent, int coreAtom, int firstAtom) {
        int[] graphAtom = new int[mol.getAtoms()];
        boolean[] isMemberAtom = new boolean[mol.getAtoms()];

        graphAtom[0] = coreAtom;
        graphAtom[1] = firstAtom;
        isMemberAtom[coreAtom] = true;
        isMemberAtom[firstAtom] = true;
        int current = 1;
        int highest = 1;
        while (current <= highest) {
            for (int i=0; i<mol.getConnAtoms(graphAtom[current]); i++) {
                int candidate = mol.getConnAtom(graphAtom[current], i);
                if (!isMemberAtom[candidate] && !mAtomUsed[candidate]) {
                    isMemberAtom[candidate] = true;
                    graphAtom[++highest] = candidate;
                    }
                }
            current++;
            }

        int[] atomMap = new int[isMemberAtom.length];
        mol.copyMoleculeByAtoms(substituent, isMemberAtom, true, atomMap);
        substituent.changeAtom(atomMap[coreAtom], 0, 0, -1, 0);

        for (int atom=0; atom<mol.getAtoms(); atom++) {
            if (isMemberAtom[atom] && atom != coreAtom) {
                for (int i=0; i<mol.getConnAtoms(atom); i++) {
                    int connAtom = mol.getConnAtom(atom, i);
                    if (!isMemberAtom[connAtom]) {
                        // TODO check esrGroupOffset=0 ???
                        int newAtom = mol.copyAtom(substituent, connAtom, 0, 0);
                        int connBond = mol.getConnBond(atom, i);
                        int bondType = mol.isDelocalizedBond(connBond) ? Molecule.cBondTypeDelocalized : mol.getBondType(connBond);
                        substituent.addBond(atomMap[atom], newAtom, bondType);
                        for (BBConnection dc:mList) {
                            if (dc.atom == connAtom
                             && dc.bondTypeMatches(BBConnection.createBondType(mol, connBond))) {
                                substituent.setAtomCustomLabel(newAtom, "B"+mBBID[connAtom]+":"+(dc.anchorNo+1));
                                }
                            }
                        }
                    }
                }
            }
        }
    }


class SARStatusPanel extends JPanel {
    private static final long serialVersionUID = 0x20090717;

    // Colors should match MoleculeContext.BB_ATOM_COLOR
    private static final Color[] BB_COLOR = {
                                        new Color(160,0,0), // dark red
                                        Color.orange,
                                        Color.green,
                                        new Color(0,160,0), // dark green
                                        Color.magenta,
                                        Color.blue };

    private SARSourceRecord[] mSourceRecord;
    private int[] mCount;

    public SARStatusPanel(SARSourceRecord[] sourceRecord) {
        mSourceRecord = sourceRecord;
        }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension size = getSize();
        g.setColor(Color.gray);
        g.draw3DRect(0, 0, size.width-1, size.height-1, false);
        size.width -= 2;
        size.height -= 2;
        g.setColor(Color.white);
        g.fillRect(1, 1, size.width, size.height);
        if (mCount != null) {
            for (int bb=0; bb<mCount.length; bb++) {
                int x1 = 1 + bb * size.width / mCount.length;
                int x2 = 1 + (bb+1) * size.width / mCount.length;
                int y2 = 1 + mCount[bb] * size.height / mSourceRecord.length;
                g.setColor(BB_COLOR[(bb+1) % BB_COLOR.length]);
                g.fill3DRect(x1, 1, x2-x1, y2-1, true);
                }
            }
        }

    public void update(ArrayList<AbstractBuildingBlock> abbList) {
        mCount = new int[abbList.size()];
        for (SARSourceRecord sr:mSourceRecord)
            for (int i=0; i<abbList.size(); i++)
                if (sr.isMatching(abbList.get(i).getID()))
                    mCount[i]++;
        repaint();
        }
    }

class SARSourceRecord {
    private CompoundRecord mCompoundRecord;
    private int[] mMatchingBBIDs;

    public SARSourceRecord(CompoundRecord record) {
        mCompoundRecord = record;
        }

    public CompoundRecord getCompoundRecord() {
        return mCompoundRecord;
        }

    public MoleculeContext matchBuildingBlocks(SSSearcherWithIndex searcher, int idcodeColumn, int fragFpColumn, ArrayList<AbstractBuildingBlock> abbl) {
        byte[] idcode = (byte[])mCompoundRecord.getData(idcodeColumn);
        long[] fragFp = (fragFpColumn == -1) ? null : (long[])mCompoundRecord.getData(fragFpColumn);
        MoleculeContext context = new MoleculeContext(searcher, idcode, fragFp);
        mMatchingBBIDs = context.matchBuildingBlocks(abbl);
        return context;
        }

    public void addMatchingBB(AbstractBuildingBlock abb) {
        int bbID = (abb == null) ? AbstractBuildingBlock.getNextID() : abb.getID();
        mMatchingBBIDs = MoleculeContext.addIDToList(bbID, mMatchingBBIDs);
        }

    public boolean isMatchingAll(ArrayList<AbstractBuildingBlock> abbl) {
        return mMatchingBBIDs.length == abbl.size();
        }

    public boolean isMatchingAny() {
        return (mMatchingBBIDs != null);
        }

    public boolean isMatching(int bbID) {
        if (mMatchingBBIDs != null)
            for (int matchingBBID:mMatchingBBIDs)
                if (bbID == matchingBBID)
                    return true;
        return false;
        }
    }