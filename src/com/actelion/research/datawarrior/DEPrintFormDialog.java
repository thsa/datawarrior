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

package com.actelion.research.datawarrior;

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JCompoundTableForm;

public class DEPrintFormDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 20140219L;
	private static final String[] RECORD_OPTIONS = {"Current Record",
            										"Visible Records",
            										"All Records"};
    private static final String[] COUNT_OPTIONS = {"1", "2", "3", "4","5", "6", "7", "8"};
    
    private CompoundTableModel	mTableModel;
	private JCompoundTableForm	mCompoundTableForm;
	private JComboBox			mComboBoxRecords,mComboBoxColumns,mComboBoxRows;
	private DEPrintFormPreview	mPreview;
	private JLabel				mInfoLabel;
	private boolean				mIsOK;

    public DEPrintFormDialog(Frame parent, CompoundTableModel tableModel, JCompoundTableForm form) {
		super(parent, "Printed Records and Layout", true);
		mTableModel = tableModel;
		mCompoundTableForm = form;

		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 8} };
		p.setLayout(new TableLayout(size));
		
		mComboBoxRecords = new JComboBox(RECORD_OPTIONS);
		mComboBoxColumns = new JComboBox(COUNT_OPTIONS);
		mComboBoxRows = new JComboBox(COUNT_OPTIONS);
		mPreview = new DEPrintFormPreview();
		mInfoLabel = new JLabel("<1 page>", JLabel.CENTER);

		mComboBoxRecords.addActionListener(this);
		mComboBoxColumns.addActionListener(this);
		mComboBoxRows.addActionListener(this);
		
		p.add(new JLabel("Records:"), "1,1,3,1");
		p.add(mComboBoxRecords, "5,1,7,1");
		p.add(new JLabel("Number of columns:"), "1,3,3,3");
		p.add(mComboBoxColumns, "5,3");
		p.add(new JLabel("Number of rows:"), "1,5,3,5");
		p.add(mComboBoxRows, "5,5");
		p.add(mPreview, "7,3,7,5");
		p.add(mInfoLabel, "3,7,5,7");

		JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(this);
		p.add(bcancel, "5,9");
		JButton bok = new JButton("OK");
		bok.addActionListener(this);
		p.add(bok, "7,9");
	
		getContentPane().add(p);
		getRootPane().setDefaultButton(bok);
	
		pack();
		setLocationRelativeTo(parent);
		setVisible(true);
		}
	
    public boolean isOK() {
        return mIsOK;
    	}
    
	public void actionPerformed(ActionEvent e) {
	    if (e.getSource() == mComboBoxRecords
   	     || e.getSource() == mComboBoxColumns
	     || e.getSource() == mComboBoxRows) {
	        int columns = 1;
	        int rows = 1;
	        int records = 1;
	        
	        switch (mComboBoxRecords.getSelectedIndex()) {
	        case JCompoundTableForm.PRINT_MODE_VISIBLE_RECORDS:
		        columns = mComboBoxColumns.getSelectedIndex()+1;
        		rows = mComboBoxRows.getSelectedIndex()+1;
        		records = mTableModel.getRowCount();
	        	break;
	        case JCompoundTableForm.PRINT_MODE_ALL_RECORDS:
		        columns = mComboBoxColumns.getSelectedIndex()+1;
	        	rows = mComboBoxRows.getSelectedIndex()+1;
	        	records = mTableModel.getTotalRowCount();
	        	break;
	        	}
	        mPreview.setLayout(columns, rows, records);
	        int pages = 1+(records-1)/(columns*rows);
        	mInfoLabel.setText((pages == 1) ? "<1 page>" : "<"+pages+" pages>");
	        return;
	    	}

	    mCompoundTableForm.setPrintMode(mComboBoxRecords.getSelectedIndex(),
	            						mComboBoxColumns.getSelectedIndex()+1,
	            						mComboBoxRows.getSelectedIndex()+1);

	    mIsOK = !e.getActionCommand().equals("Cancel");
	    setVisible(false);
	    dispose();
		return;
		}
	}

class DEPrintFormPreview extends JPanel {
	private static final long serialVersionUID = 20140220L;
    private int mColumns = 1;
    private int mRows = 1;
    private int mRecords = 1;
    
    public void paint(Graphics g) {
        Dimension size = getSize();
        int hspace = Math.max(1, size.width/mColumns/4);
        int vspace = Math.max(1, size.height/mRows/4);
        int space = Math.min(hspace, vspace);
        int width = (size.width-(mColumns-1)*space)/mColumns;
        int height = (size.height-(mRows-1)*space)/mRows;
        int dx = (size.width-mColumns*(width+space)+space)/2;
        int dy = (size.height-mRows*(height+space)+space)/2;
        g.setColor(getParent().getBackground());
        g.fillRect(0, 0, size.width, size.height);
        g.setColor(Color.GRAY);
        int count = 0;
        for (int r=0; r<mRows; r++) {
            for (int c=0; c<mColumns; c++) {
                if (count == mRecords)
                    g.setColor(Color.LIGHT_GRAY);
                g.fillRect(dx+c*(width+space), dy+r*(height+space), width, height);
                count++;
            	}
        	}
    	}
    
    public void setLayout(int columns, int rows, int records) {
        mColumns = columns;
        mRows = rows;
        mRecords = records;
        repaint();
    	}
	}
