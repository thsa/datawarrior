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

package com.actelion.research.table;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.gui.table.ChemistryCellRenderer;
import com.actelion.research.gui.table.ChemistryRenderPanel;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.DetailTableModel;
import com.actelion.research.table.view.VisualizationColor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class DetailTableCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 0x20061009;

    private DetailTableModel mTableModel;
    private ChemistryCellRenderer       mChemistryRenderer;
	private MultiLineCellRenderer       mMultiLineRenderer;
	private CompoundTableColorHandler	mColorHandler;

    public DetailTableCellRenderer(DetailTableModel tableModel) {
		super();
		mTableModel = tableModel;
    	}

    public void setColorHandler(CompoundTableColorHandler colorHandler) {
    	mColorHandler = colorHandler;
    	}

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row, int col) {
        if (mTableModel.getColumnCount() == 0)
            return null;

        int column = mTableModel.getParentModel().convertFromDisplayableColumnIndex(row);
        if (col == 1 && (value instanceof StereoMolecule || value instanceof Reaction)) {
            if (mChemistryRenderer == null) {
                mChemistryRenderer = new ChemistryCellRenderer();
                mChemistryRenderer.setAlternateRowBackground(true);
                }

            return colorize((ChemistryRenderPanel)mChemistryRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col), column);
			}
        else {
            if (mMultiLineRenderer == null) {
                mMultiLineRenderer = new MultiLineCellRenderer();
                mMultiLineRenderer.setAlternateRowBackground(true);
                }

            return colorize((JComponent)mMultiLineRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col), column);
    		}
        }

	private JComponent colorize(JComponent c, int column) {
		if (mColorHandler != null) {
	    	CompoundRecord record = mTableModel.getCompoundRecord();
	    	if (record != null) {
	    		if (mColorHandler.hasColorAssigned(column, CompoundTableColorHandler.FOREGROUND)) {
				    VisualizationColor vc = mColorHandler.getVisualizationColor(column, CompoundTableColorHandler.FOREGROUND);
	    			c.setForeground(vc.getColorForForeground(record));
	    			}

	    		if (mColorHandler.hasColorAssigned(column, CompoundTableColorHandler.BACKGROUND)) {
				    VisualizationColor vc = mColorHandler.getVisualizationColor(column, CompoundTableColorHandler.BACKGROUND);
	    			c.setBackground(vc.getColorForBackground(record));
	    			}
	    		}
			}
		return c;
		}
	}
