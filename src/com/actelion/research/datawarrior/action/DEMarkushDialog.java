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


import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.MarkushStructure;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JFileChooserOverwrite;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.editor.GenericEditorArea;
import com.actelion.research.gui.editor.SwingEditorPanel;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.gui.swing.SwingKeyHandler;
import com.actelion.research.io.BOMSkipper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.TreeSet;

public class DEMarkushDialog extends JDialog implements ActionListener,Runnable {
    private static final long serialVersionUID = 0x20080515;

    private Frame				mParentFrame;
	private DataWarrior		    mApplication;
	private DEFrame				mTargetFrame;
	private SwingEditorPanel	mDrawPanel;
    private JProgressDialog     mProgressDialog;
    private MarkushStructure    mMarkushStructure;

	public DEMarkushDialog(Frame owner, DataWarrior application) {
		super(owner, "Enumerate Markush Structure", true);

		mParentFrame = owner;
		mApplication = application;

		StereoMolecule mol = new StereoMolecule();
		mol.setFragment(true);
		mDrawPanel = new SwingEditorPanel(mol, GenericEditorArea.MODE_MARKUSH_STRUCTURE);
		mDrawPanel.getDrawArea().setClipboardHandler(new ClipboardHandler());
		getContentPane().add(mDrawPanel, BorderLayout.CENTER);

		SwingKeyHandler keyHandler = new SwingKeyHandler(mDrawPanel.getDrawArea());
		addKeyListener(keyHandler);
		keyHandler.addListener(mDrawPanel.getDrawArea());

		JPanel bp = new JPanel();
		bp.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));
		bp.setLayout(new GridLayout(1, 8, 8, 0));
		JButton bopen = new JButton("Open...");
		bopen.addActionListener(this);
		bp.add(bopen);
		JButton bsave = new JButton("Save...");
		bsave.addActionListener(this);
		bp.add(bsave);
		bp.add(new JLabel());
		bp.add(new JLabel());
		bp.add(new JLabel());
		bp.add(new JLabel());
		JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(this);
		bp.add(bcancel);
		JButton bok = new JButton("OK");
		bok.addActionListener(this);
		bp.add(bok);

		getContentPane().add(bp, BorderLayout.SOUTH);
//		getRootPane().setDefaultButton(bok);

		setSize(HiDPIHelper.scale(720), HiDPIHelper.scale(434));
		setLocationRelativeTo(owner);
		setVisible(true);
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Open...")) {
			File file = FileHelper.getFile(mParentFrame, "Please select a markush file", FileHelper.cFileTypeTextTabDelimited);
			if (file == null)
				return;

			try {
			    MarkushStructure markush = readFile(mParentFrame, file);

					// allow for query features
//				for (int i=0; i<reaction.getMolecules(); i++)
	//				reaction.getMolecule(i).setFragment(true);

				mDrawPanel.getDrawArea().setMarkushStructure(markush);
				}
			catch (Exception ex) {}
			return;
			}
		else if (e.getActionCommand().equals("Save...")) {
			MarkushStructure markush = mDrawPanel.getDrawArea().getMarkushStructure();
			if (isValid(markush)) {
                JFileChooserOverwrite fileChooser = new JFileChooserOverwrite();
                fileChooser.setCurrentDirectory(FileHelper.getCurrentDirectory());
                fileChooser.setFileFilter(FileHelper.createFileFilter(FileHelper.cFileTypeTextTabDelimited, true));
                fileChooser.setExtension(FileHelper.getExtension(FileHelper.cFileTypeTextTabDelimited));
                int option = fileChooser.showSaveDialog(mParentFrame);
                FileHelper.setCurrentDirectory(fileChooser.getCurrentDirectory());
                if (option == JFileChooser.APPROVE_OPTION)
                    writeFile(mParentFrame, fileChooser.getFile(), markush);
			    }
			}
		else if (e.getActionCommand().equals("OK")) {
		    mMarkushStructure = mDrawPanel.getDrawArea().getMarkushStructure();
		    if (isValid(mMarkushStructure)) {
                enumerate();
                setVisible(false);
                dispose();
		        }
			}
		else if (e.getActionCommand().equals("Cancel")) {
		    setVisible(false);
    	    dispose();
			}
		}

	private boolean isValid(MarkushStructure markush) {
	    try {
            String warning = markush.validate();
            return (warning == null
                 || JOptionPane.showConfirmDialog(this,
                         warning+"\nDo you want to continue anyway?",
                         "Warning",
                         JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
	        }
	    catch (Exception e) {
e.printStackTrace();
            JOptionPane.showMessageDialog(this, e);
            return false;
	        }
	    }

	private void enumerate() {
		mTargetFrame = mApplication.getEmptyFrame("Markush Enumeration");
	    mProgressDialog = new JProgressDialog(mParentFrame);

	    Thread t = new Thread(this, "MarkushEnumeration");
	    t.setPriority(Thread.MIN_PRIORITY);
	    t.start();
	    }

	public void run() {
        try {
            runEnumeration();
            }
        catch (OutOfMemoryError e) {
            final String message = "Out of memory. Launch DataWarrior with Java option -Xms???m or -Xmx???m.";
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(mParentFrame, message);
                    }
                } );
            }

        mProgressDialog.close(mTargetFrame);
        }

	private void runEnumeration() {
        mProgressDialog.startProgress("Enumerating Markush Structures...", 0, 0);
        ArrayList<Object[]> recordList = new ArrayList<Object[]>();

        TreeSet<String> idcodeSet = new TreeSet<String>();

        StereoMolecule mol = mMarkushStructure.getNextEnumeration();
        while (mol != null) {
            Canonizer canonizer = new Canonizer(mol);
            String idcode = canonizer.getIDCode();
            if (!idcodeSet.contains(idcode)) {
                idcodeSet.add(idcode);
                Object[] record = new Object[2];
                record[0] = idcode.getBytes();
                record[1] = canonizer.getEncodedCoordinates().getBytes();
                recordList.add(record);
                }

            if (mProgressDialog.threadMustDie())
                break;

            mol = mMarkushStructure.getNextEnumeration();
            }

        if (!mProgressDialog.threadMustDie())
            populateTable(recordList);
        }

    private void populateTable(ArrayList<Object[]> recordList) {
        int rowCount = recordList.size();
        int columnCount = 3;

        mProgressDialog.startProgress("Populating Table...", 0, recordList.size());

        CompoundTableModel tableModel = mTargetFrame.getTableModel();
        tableModel.initializeTable(rowCount, columnCount);
        tableModel.prepareStructureColumns(0, "Structure", true, true);

        int row = 0;
        for (Object[] record:recordList) {
        	tableModel.setTotalDataAt(record[0], row, 0);
        	tableModel.setTotalDataAt(record[1], row, 1);
            mProgressDialog.updateProgress(row++);
            }

        tableModel.finalizeTable(CompoundTableEvent.cSpecifierDefaultFiltersAndViews, mProgressDialog);
        }

	private void writeFile(Frame parent, File file, MarkushStructure markush) {
	    try {
	        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
	        writer.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>");
            writer.newLine();
	        writer.write("<markushStructure>");
	        writer.newLine();
            writer.write("\t<coreCount>"+markush.getCoreCount()+"</coreCount>");
            writer.newLine();
            for (int i=0; i<markush.getCoreCount(); i++) {
                Canonizer canonizer = new Canonizer(markush.getCoreStructure(i));
                writer.write("\t<core>");
                writer.newLine();
                writer.write("\t\t<idcode>"+canonizer.getIDCode()+"</idcode>");
                writer.newLine();
                writer.write("\t\t<idcoordinates>"+canonizer.getEncodedCoordinates(true)+"</idcoordinates>");
                writer.newLine();
                writer.write("\t</core>");
                writer.newLine();
                }
            writer.write("\t<rGroupCount>"+markush.getRGroupCount()+"</rGroupCount>");
            writer.newLine();
            for (int i=0; i<markush.getRGroupCount(); i++) {
                StereoMolecule[] substituent = markush.getRGroup(i).getFragments();
                writer.write("\t<rGroup>");
                writer.newLine();
                writer.write("\t\t<substituentCount>"+substituent.length+"</substituentCount>");
                writer.newLine();
                for (int j=0; j<substituent.length; j++) {
                    Canonizer canonizer = new Canonizer(substituent[j]);
                    writer.write("\t\t<substituent>");
                    writer.newLine();
                    writer.write("\t\t\t<idcode>"+canonizer.getIDCode()+"</idcode>");
                    writer.newLine();
                    writer.write("\t\t\t<idcoordinates>"+canonizer.getEncodedCoordinates(true)+"</idcoordinates>");
                    writer.newLine();
                    writer.write("\t\t</substituent>");
                    writer.newLine();
                    }
                writer.write("\t</rGroup>");
                writer.newLine();
                }
            writer.write("</markushStructure>");
            writer.newLine();
            writer.close();
	        }
	    catch (IOException e) {
            JOptionPane.showMessageDialog(mParentFrame, e.getMessage());
	        }
	    }

	private MarkushStructure readFile(Frame parent, File file) {
	    try {
	        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			BOMSkipper.skip(reader);
	        if (!reader.readLine().startsWith("<?xml version=")) {
	        	reader.close();
	            throw new IOException("Invalid Markush file format.");
	        	}
	        extractValue(reader, "markushStructure");
	        MarkushStructure markush = new MarkushStructure();
            int coreCount = Integer.parseInt(extractValue(reader, "coreCount"));
            for (int i=0; i<coreCount; i++) {
                extractValue(reader, "core");
                String idcode = extractValue(reader, "idcode");
                String coords = extractValue(reader, "idcoordinates");
                markush.addCore(new IDCodeParser(true).getCompactMolecule(idcode, coords));
                reader.readLine();
                }
            int rGroupCount = Integer.parseInt(extractValue(reader, "rGroupCount"));
            for (int i=0; i<rGroupCount; i++) {
                extractValue(reader, "rGroup");
                StereoMolecule substituents = new StereoMolecule();
                int substituentCount = Integer.parseInt(extractValue(reader, "substituentCount"));
                for (int j=0; j<substituentCount; j++) {
                    extractValue(reader, "substituent");
                    String idcode = extractValue(reader, "idcode");
                    String coords = extractValue(reader, "idcoordinates");
                    substituents.addMolecule(new IDCodeParser(true).getCompactMolecule(idcode, coords));
                    reader.readLine();
                    }
                markush.addRGroup(substituents);
                reader.readLine();
                }
            reader.readLine();
            return markush;
	        }
        catch (IOException e) {
            JOptionPane.showMessageDialog(mParentFrame, e.getMessage());
            return null;
            }
	    }

	private String extractValue(BufferedReader reader, String property) throws IOException {
	    String line = reader.readLine();
	    int index1 = line.indexOf("<"+property+">");
	    if (index1 == -1)
	        throw new IOException("Invalid Markush file format.");
	    int index2 = line.indexOf("</"+property+">", index1+property.length()+2);
	    if (index2 == -1)
	        return null;
	    return line.substring(index1+property.length()+2, index2).trim();
	    }
    }