package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.model.CompoundTableModel;

public abstract class DETaskAbstractFromStructure extends DETaskAbstractFromChemistry {
	public DETaskAbstractFromStructure(DEFrame parent, int descriptorClass, boolean editableColumnNames, boolean useMultipleCores) {
		super(parent, descriptorClass, editableColumnNames, useMultipleCores);
		}

	@Override
	protected String getColumnType() {
		return CompoundTableModel.cColumnTypeIDCode;
		}

	@Override
	protected int getDescriptorType() {
		return DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE;
		}

	@Override
	protected String getTypeName() {
		return "Structure";
		}

	/**
	 * Derived classes may overwrite this to directly assign values to compound table cells.
	 * The default implementation calls getNewColumnValue() for every new table cell.
	 * If one or more existing columns are updated rather than all properties written
	 * into new columns, then this method must be overridden and in postProcess()
	 * finalizeChangeColumn() must be called on all updated columns of the table model (!!!).
	 * @param row
	 * @param containerMol container molecule to be repeatedly used if type is TYPE_STRUCTURE
	 * @param firstNewColumn
	 */
	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) throws Exception {
		assert(firstNewColumn != -1);
		StereoMolecule mol = getChemicalStructure(row, containerMol);
		if (mol != null)
			for (int i = 0; i < getNewColumnCount(); i++)
				getTableModel().setTotalValueAt(getNewColumnValue(mol, getDescriptor(row), i), row, firstNewColumn + i);
		}

	/**
	 * Derived classes must either override this or override processRow() instead.
	 * @param mol is guaranteed to be != null
	 * @param descriptor
	 * @param column (one of the) new column(s)
	 * @return
	 */
	protected String getNewColumnValue(StereoMolecule mol, Object descriptor, int column) {
		return null;
		}
	}
