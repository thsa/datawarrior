package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.model.CompoundTableModel;

public abstract class DETaskAbstractFromReaction extends DETaskAbstractFromChemistry {
	public DETaskAbstractFromReaction(DEFrame parent, int descriptorClass, boolean editableColumnNames, boolean useMultipleCores) {
		super(parent, descriptorClass, editableColumnNames, useMultipleCores);
		}

	@Override
	protected String getColumnType() {
		return CompoundTableModel.cColumnTypeRXNCode;
		}

	@Override
	protected int getDescriptorType() {
		return DescriptorConstants.DESCRIPTOR_TYPE_REACTION;
		}

	@Override
	protected String getTypeName() {
		return "Reaction";
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
	 * @param threadIndex
	 */

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) throws Exception {
		assert(firstNewColumn != -1);
		Reaction rxn = getChemicalReaction(row);
		if (rxn != null)
			for (int i=0; i<getNewColumnCount(); i++)
				getTableModel().setTotalValueAt(getNewColumnValue(rxn, getDescriptor(row), i), row, firstNewColumn + i);
		}

	/**
	 * Derived classes must either override this or override processRow() instead.
	 * @param rxn is guaranteed to be != null
	 * @param descriptor
	 * @param column (one of the) new column(s)
	 * @return
	 */
	protected String getNewColumnValue(Reaction rxn, Object descriptor, int column) {
		return null;
		}
	}
