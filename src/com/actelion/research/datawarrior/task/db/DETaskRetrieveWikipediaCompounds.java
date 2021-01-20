package com.actelion.research.datawarrior.task.db;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.MolecularFormula;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEPruningPanel.FilterException;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Properties;

public class DETaskRetrieveWikipediaCompounds extends AbstractTaskWithoutConfiguration {
	public static final String TASK_NAME = "Retrieve Wikipedia Compounds";
	private static final String RAW_DATA_URL = "http://www.cheminfo.org/wikipedia/idcode.txt";
	private static final String[] COLUMN_NAME = {"Compound Name", "Formula", "Molweight"};
	private static final String EXPLANATION =
			"<html>\n"+
			"<head>\n"+
			"</head>\n"+
			"<body>\n"+
			"<p><big><b><font color=\"#000080\">Wikipedia Chemical Structures</font></b></big></p>\n"+
			"<p>This DataWarrior window contains all chemical structures from\n"+
			"www.wikipedia.org. It allows sub-structure and similarity search on\n"+
			"these structures. For any structure the corresponding Wikipedia page can\n"+
			"be opened in your default web browser with a right mouse click on a table row\n"+
			"and choosing '<i>Lookup in Wikipedia</i>'.</p>\n"+
			"</body>\n"+
			"</html>";

	private DataWarrior	mApplication;
	private DEFrame		mTargetFrame;

	public DETaskRetrieveWikipediaCompounds(Frame parent, DataWarrior application) {
		super(parent, true);
		mApplication = application;
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/databases.html#Wikipedia";
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
		}

	@Override
	public void runTask(Properties configuration) {
		ArrayList<byte[][]> rowList = new ArrayList<>();
		try {
			startProgress("Retrieving Wikipedia compounds...", 0, 0);

			URLConnection con = new URL(RAW_DATA_URL).openConnection();
	        con.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
	        con.setRequestProperty("Content-Type", "text/plain");

			InputStream is = con.getInputStream();
			if (is != null) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				String line = reader.readLine();
				while (line != null) {
					int index = line.indexOf('\t');
					if (index != -1) {
						byte[][] bytes = new byte[2][];
						bytes[0] = line.substring(0, index).getBytes();
						bytes[1] = line.substring(index+1).getBytes();
						rowList.add(bytes);
						}
					line = reader.readLine();
					}
				reader.close();
				}
			}
		catch (Exception e) {
			if (isInteractive()) {
				JOptionPane.showMessageDialog(getParentFrame(), "Communication error:"+e.getMessage()
						+"\nA firewall or local security software or settings may prevent contacting the server.");
				}
			}

		if (rowList.size() != 0) {
			mTargetFrame = mApplication.getEmptyFrame("Wikipedia Compounds");
			CompoundTableModel tableModel = mTargetFrame.getTableModel();
		    int columnCount = COLUMN_NAME.length + 3;
			tableModel.initializeTable(rowList.size(), columnCount);
            tableModel.prepareStructureColumns(0, "Structure", true, true);
        	tableModel.setColumnProperty(0, CompoundTableModel.cColumnPropertyRelatedIdentifierColumn, COLUMN_NAME[1]);
			for (int i=3; i<columnCount; i++)
				tableModel.setColumnName(COLUMN_NAME[i-3], i);

			tableModel.setColumnProperty(3, CompoundTableModel.cColumnPropertyLookupCount, "1");
        	tableModel.setColumnProperty(3, CompoundTableModel.cColumnPropertyLookupName+"0", "Wikipedia Entry");
        	tableModel.setColumnProperty(3, CompoundTableModel.cColumnPropertyLookupURL+"0", "http://en.wikipedia.org/wiki/%s");

        	tableModel.setExtensionData(CompoundTableModel.cExtensionNameFileExplanation, EXPLANATION);

        	startProgress("Processing Wikipedia compounds...", 0, rowList.size());
			for (int row=0; row<rowList.size(); row++) {
				if ((row & 63) == 63)
					updateProgress(row);

				try {
					byte[][] rowData = rowList.get(row);
					StereoMolecule mol = new IDCodeParser().getCompactMolecule(rowData[0]);
					Canonizer canonizer = new Canonizer(mol);
					MolecularFormula formula = new MolecularFormula(mol);
					tableModel.setTotalDataAt(canonizer.getIDCode().getBytes(), row, 0);
					tableModel.setTotalDataAt(canonizer.getEncodedCoordinates().getBytes(), row, 1);
					tableModel.setTotalDataAt(rowData[1], row, 3);
					tableModel.setTotalDataAt(formula.getFormula().getBytes(), row, 4);
					float mw = Math.round(formula.getRelativeWeight()*100)/100f;
					tableModel.setTotalDataAt(Float.toString(mw).getBytes(), row, 5);
					}
				catch (Exception e) {}
				}

			// cSpecifierNoRuntimeProperties causes DataWarrior to create a detault "Table" view only
			tableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, this);

			JTable table = mTargetFrame.getMainFrame().getMainPane().getTable();
			table.getColumnModel().getColumn(0).setPreferredWidth(180);
			table.getColumnModel().getColumn(1).setPreferredWidth(200);
			table.getColumnModel().getColumn(2).setPreferredWidth(120);
			table.getColumnModel().getColumn(3).setPreferredWidth(80);

			SwingUtilities.invokeLater(() -> {
				mTargetFrame.getMainFrame().getMainPane().addExplanationView("Explanation", "Table\ttop\t0.25");

				try {
					mTargetFrame.getMainFrame().getPruningPanel().addStructureFilter(tableModel, 0, null, null);
					mTargetFrame.getMainFrame().getPruningPanel().addTextFilter(tableModel, 3);
					}
				catch (FilterException fe) {}
				} );
			}
		}
	}
