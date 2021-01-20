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

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.file.DETaskNewFileFromCorrelationCoefficients;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.model.NumericalCompoundTableColumn;
import info.clearthought.layout.TableLayout;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.Arrays;
import javax.swing.*;

import com.actelion.research.calc.*;
import com.actelion.research.util.DoubleFormat;


public class DECorrelationDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 0x20080507;

	private static final int FONT_SIZE = 14;

	private JComboBox			mComboBoxCorrelationType;
	private DEFrame				mParentFrame;
	private CompoundTableModel mTableModel;
	private int[]				mNumericalColumn;
	private double[][][]		mMatrix;

    public DECorrelationDialog(DEFrame parent, CompoundTableModel tableModel) {
		super(parent, "Correlation Matrix", true);
		mParentFrame = parent;
		mTableModel = tableModel;

		mNumericalColumn = getNumericalColumns(mTableModel);
		if (mNumericalColumn.length < 2) {
            JOptionPane.showMessageDialog(parent, "Sorry, you need at least two numerical columns\n"
                                                 +"in order to calculate a correlation matrix.");
		    return;
		    }
		mMatrix = new double[CorrelationCalculator.TYPE_NAME.length][][];

		int gap1 = HiDPIHelper.scale(8);
		int gap2 = HiDPIHelper.scale(12);
		JPanel p = new JPanel();
        double[][] size = { {gap1, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL, gap1},
                            {gap1, TableLayout.PREFERRED, gap1, TableLayout.FILL, gap2, TableLayout.PREFERRED, gap1 } };
        p.setLayout(new TableLayout(size));

		JPanel cbp = new JPanel();
		cbp.add(new JLabel("Correlation Coefficient:"));
		mComboBoxCorrelationType = new JComboBox(CorrelationCalculator.TYPE_LONG_NAME);
		mComboBoxCorrelationType.addActionListener(this);
		cbp.add(mComboBoxCorrelationType);
		p.add(cbp, "1,1,3,1");

		JPanel matrixPanel = new JPanel() {
		    private static final long serialVersionUID = 0x20080507;

		    private final int SPACING = HiDPIHelper.scale(4);
		    private final int NUM_CELL_WIDTH = HiDPIHelper.scale(24);
		    private final int CELL_WIDTH = HiDPIHelper.scale(72);
		    private final int CELL_HEIGHT = HiDPIHelper.scale(16);
            private Dimension size;
            private int titleWidth;
			private Font font;

		    public Dimension getPreferredSize() {
		        if (size == null) {
		        	if (font == null)
			        	font = new JLabel().getFont().deriveFont((float)HiDPIHelper.scale(FONT_SIZE));
		        	FontMetrics metrics = new JLabel().getFontMetrics(font);
		            for (int i=0; i<mNumericalColumn.length; i++)
		                titleWidth = Math.max(titleWidth, metrics.stringWidth(mTableModel.getColumnTitle(mNumericalColumn[i])));

		            size = new Dimension(CELL_WIDTH * mNumericalColumn.length + titleWidth + 2*SPACING + NUM_CELL_WIDTH,
		                                 CELL_HEIGHT * mNumericalColumn.length + CELL_HEIGHT);
		            }

		        return size;
		        }

		    public void paint(Graphics g) {
		        super.paint(g);

				if (font == null)
					font = new JLabel().getFont().deriveFont((float)HiDPIHelper.scale(FONT_SIZE));
				g.setFont(font);

		        g.setColor(getBackground().darker());
                for (int i=0; i<mNumericalColumn.length; i++) {
                    g.fillRect(2*SPACING+titleWidth+1, (i+1)*CELL_HEIGHT+1, NUM_CELL_WIDTH-2, CELL_HEIGHT-2);
                    g.fillRect(NUM_CELL_WIDTH+2*SPACING+titleWidth+i*CELL_WIDTH+1, 1, CELL_WIDTH-2, CELL_HEIGHT-2);
                    }
                g.setColor(Color.WHITE);
                for (int i=0; i<mNumericalColumn.length; i++) {
                    g.fillRect(1, (i+1)*CELL_HEIGHT+1, titleWidth+2*SPACING-2, CELL_HEIGHT-2);
                    }
                g.setColor(Color.BLACK);
                for (int i=0; i<mNumericalColumn.length; i++) {
                    String s = ""+(i+1);
                    int stringWidth = g.getFontMetrics().stringWidth(s);
                    g.drawString(s, 2*SPACING+titleWidth+(NUM_CELL_WIDTH-stringWidth)/2, (i+2)*CELL_HEIGHT-3);
                    g.drawString(s, NUM_CELL_WIDTH+2*SPACING+titleWidth+i*CELL_WIDTH+(CELL_WIDTH-stringWidth)/2, CELL_HEIGHT-3);
                    g.drawString(mTableModel.getColumnTitle(mNumericalColumn[i]), SPACING, (i+2)*CELL_HEIGHT-3);
                    }

                int type = mComboBoxCorrelationType.getSelectedIndex();
                if (mMatrix[type] == null) {
                	NumericalCompoundTableColumn[] nc = new NumericalCompoundTableColumn[mNumericalColumn.length];
            		for (int i=0; i<mNumericalColumn.length; i++)
            			nc[i] = new NumericalCompoundTableColumn(mTableModel, mNumericalColumn[i]);
                    mMatrix[type] = new CorrelationCalculator().calculateMatrix(nc, type);
                	}

                int xOffset = NUM_CELL_WIDTH+2*SPACING+titleWidth;
                int yOffset = CELL_HEIGHT;
		        for (int i=1; i<mNumericalColumn.length; i++) {
		            for (int j=0; j<i; j++) {
		                g.setColor(new Color(Color.HSBtoRGB((float)0.4, (float)Math.abs(mMatrix[type][i][j]), (float)0.8)));
		                g.fillRect(xOffset+i*CELL_WIDTH+1, yOffset+j*CELL_HEIGHT+1, CELL_WIDTH-2, CELL_HEIGHT-2);
                        g.fillRect(xOffset+j*CELL_WIDTH+1, yOffset+i*CELL_HEIGHT+1, CELL_WIDTH-2, CELL_HEIGHT-2);
		                g.setColor(Color.BLACK);
		                g.drawString(DoubleFormat.toString(mMatrix[type][i][j], 3), xOffset+i*CELL_WIDTH+SPACING, yOffset+CELL_HEIGHT+j*CELL_HEIGHT-3);
                        g.drawString(DoubleFormat.toString(mMatrix[type][i][j], 3), xOffset+j*CELL_WIDTH+SPACING, yOffset+CELL_HEIGHT+i*CELL_HEIGHT-3);
		                }
		            }
		        }
		    };
		matrixPanel.setSize(matrixPanel.getPreferredSize());
        if (mNumericalColumn.length > 10) {
            JScrollPane scrollPane = new JScrollPane(matrixPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setPreferredSize(new Dimension(HiDPIHelper.scale(720), HiDPIHelper.scale(192)));
            p.add(scrollPane, "1,3,3,3");
            }
        else {
            p.add(matrixPanel, "1,3,3,3");
            }

        int bw = HiDPIHelper.scale(100);
		JPanel p2 = new JPanel();
		double[][] size2 = { {bw, gap1, bw, TableLayout.FILL, bw}, {TableLayout.PREFERRED} };
		p2.setLayout(new TableLayout(size2));

		JButton bnew = new JButton("New File");
		bnew.setActionCommand("new");
		bnew.addActionListener(this);
		p2.add(bnew, "0,0");

        JButton bcopy = new JButton("Copy Matrix");
		bcopy.setActionCommand("copy");
        bcopy.addActionListener(this);
        p2.add(bcopy, "2,0");

		JButton bok = new JButton("OK");
		bok.addActionListener(this);
		p2.add(bok, "4,0");

		p.add(p2, "1,5,3,5");

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(p, BorderLayout.CENTER);
		getRootPane().setDefaultButton(bok);
	
		pack();
		setLocationRelativeTo(parent);
		setVisible(true);
		}

    public static int[] getNumericalColumns(CompoundTableModel tableModel) {
        int count = 0;
        for (int column=0; column<tableModel.getTotalColumnCount(); column++)
            if (tableModel.isColumnTypeDouble(column))
                count++;

		String[] columnName = new String[count];
		count = 0;
		for (int column=0; column<tableModel.getTotalColumnCount(); column++)
			if (tableModel.isColumnTypeDouble(column))
				columnName[count++] = tableModel.getColumnTitle(column);

		Arrays.sort(columnName);

        int[] numericalColumn = new int[count];
        for (int i=0; i<columnName.length; i++)
            numericalColumn[i] = tableModel.findColumn(columnName[i]);

        return numericalColumn;
        }

    public void actionPerformed(ActionEvent e) {
	    if (e.getSource() == mComboBoxCorrelationType) {
	        repaint();
			return;
	    	}

	    if (e.getActionCommand().equals("copy")) {
            int type = mComboBoxCorrelationType.getSelectedIndex();

            StringBuilder buf = new StringBuilder("r ("+CorrelationCalculator.TYPE_LONG_NAME[type]+")\t");
	        for (int i=0; i<mNumericalColumn.length; i++)
	            buf.append('\t').append(""+(i+1));
	        buf.append('\n');

	        for (int i=0; i<mNumericalColumn.length; i++) {
	            buf.append(mTableModel.getColumnTitle(mNumericalColumn[i])).append('\t').append(""+(i+1));
	            for (int j=0; j<mNumericalColumn.length; j++) {
	                buf.append('\t');
	                if (i<j)
	                    buf.append(DoubleFormat.toString(mMatrix[type][j][i], 3));
	                else if (i>j)
                        buf.append(DoubleFormat.toString(mMatrix[type][i][j], 3));
	                }
	            buf.append('\n');
	            }

            StringSelection theData = new StringSelection(buf.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);

	        return;
	        }

		if (e.getActionCommand().equals("new")) {
			int type = mComboBoxCorrelationType.getSelectedIndex();

			setVisible(false);
			dispose();

			new DETaskNewFileFromCorrelationCoefficients(mParentFrame, type).defineAndRun();
			return;
			}

		setVisible(false);
	    dispose();
		return;
		}
	}
