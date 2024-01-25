package com.actelion.research.datawarrior.task.chem.dev;

import com.actelion.research.chem.IsomericSmilesCreator;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.model.CompoundTableModel;

import java.util.Properties;

public class DevTaskCreateTaggedSmiles extends AbstractTaskWithoutConfiguration {
    public static final long serialVersionUID = 0x20150812;

    public static final String TASK_NAME = "Test Export Smiles";

    private CompoundTableModel mTableModel;

    public DevTaskCreateTaggedSmiles(DEFrame parent) {
        super(parent, true);
        mTableModel = parent.getTableModel();
    }

    @Override
    public DEFrame getNewFrontFrame() {
        return null;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public boolean isConfigurable() {
        if (mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode) == null) {
            showErrorMessage("No chemical structure column found.");
            return false;
        }

        return true;
    }

    @Override
    public void runTask(Properties configuration) {
        int idcodeColumn = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode)[0];
        int rowCount = mTableModel.getTotalRowCount();

        StereoMolecule mol = new StereoMolecule();

        startProgress("Generating SMILES...", 0, rowCount);

        String[] columnTitle = new String[1];
        columnTitle[0] = "Tagged Smiles";
        final int smilesColumn = mTableModel.addNewColumns(columnTitle);

        for (int r=0; r<rowCount; r++) {
            if (threadMustDie())
                break;
            if ((r % 256) == 255)
                updateProgress(r);

            mTableModel.getChemicalStructure(mTableModel.getTotalRecord(r), idcodeColumn, CompoundTableModel.ATOM_COLOR_MODE_EXPLICIT, mol);
            if (mol.getAllAtoms() != 0) {
                for (int atom=0; atom<mol.getAllAtoms(); atom++) {
                    int color = mol.getAtomColor(atom);
                    if (color == Molecule.cAtomColorBlue) {
                        mol.addBond(atom, mol.addAtom("D"));
                        mol.setAtomCharge(atom, mol.getAtomCharge(atom)+1);
                    }
                    else if (color == Molecule.cAtomColorRed) {
                        mol.addBond(atom, mol.addAtom("D"));
                    }
                }
            }

            String smiles = new IsomericSmilesCreator(mol).getSmiles();
            mTableModel.setTotalValueAt(smiles, r, smilesColumn);
        }

        mTableModel.finalizeNewColumns(smilesColumn, this);
    }
}
