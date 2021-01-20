package com.actelion.research.datawarrior.task.file;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import javax.swing.SwingUtilities;

import com.actelion.research.calc.ProgressController;
import com.actelion.research.calc.SelfOrganizedMap;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.CompoundTableSOM;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.table.view.VisualizationPanel2D;

public class DETaskApplySOMFile extends DETaskAbstractOpenFile {
	public static final String TASK_NAME = "Apply SOM-File";

    private DEFrame		mParentFrame;

    public DETaskApplySOMFile(DataWarrior application) {
		super(application, "Open SOM-File And Calculate Positions", FileHelper.cFileTypeSOM);
		mParentFrame = application.getActiveFrame();
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public DEFrame openFile(File file, Properties configuration) {
		CompoundTableSOM som = null;
		try {
			som = readSOMFile(file, mParentFrame.getTableModel(), getProgressController());
			som.checkCompatibility();
			som.positionRecords();
			}
		catch (Exception e) {
e.printStackTrace();
			showErrorMessage(e.getMessage());
			return null;
			}

		final BufferedImage background = som.createSimilarityMapImage(Math.max(som.getWidth()*4, 768), Math.max(som.getHeight()*4, 768));

		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
	                int somFitColumn = mParentFrame.getTableModel().getTotalColumnCount()-1;
	                String xColumn = mParentFrame.getTableModel().getColumnTitle(somFitColumn-2);
	                String yColumn = mParentFrame.getTableModel().getColumnTitle(somFitColumn-1);
	                if (xColumn.startsWith("SOM_X") && yColumn.startsWith("SOM_Y")) {
	                    VisualizationPanel2D vpanel1 = mParentFrame.getMainFrame().getMainPane().add2DView("SOM", null);
	                    vpanel1.setAxisColumnName(0, xColumn);
	                    vpanel1.setAxisColumnName(1, yColumn);
	                    ((JVisualization2D)vpanel1.getVisualization()).setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);
	                    int colorListMode = VisualizationColor.cColorListModeHSBLong;
	                    Color[] colorList = VisualizationColor.createColorWedge(Color.red, Color.blue, colorListMode, null);
	                    vpanel1.getVisualization().getMarkerColor().setColor(somFitColumn, colorList, colorListMode);
	                    if (background != null) {
	                        ((JVisualization2D)vpanel1.getVisualization()).setBackgroundImage(background);
	                        vpanel1.getVisualization().setScaleMode(JVisualization.cScaleModeHidden);
	                        vpanel1.getVisualization().setGridMode(JVisualization.cGridModeHidden);
	                    	}
	                    }
	                }
				} );
			}
		catch (Exception ie) {}

		return null;
		}

	public static CompoundTableSOM readSOMFile(File file, CompoundTableModel tableModel, ProgressController pc) throws Exception {
		BufferedReader theReader = new BufferedReader(new FileReader(file));
		String firstLine = theReader.readLine();
		
		int somType = -1;
        if (firstLine.equals("<datawarrior SOM>")) {  // old VectorSOMs without type declaration
            somType = CompoundTableSOM.SOM_TYPE_DOUBLE;
            }
        else if (firstLine.startsWith("<datawarriorSOM type=")) {
		    String typeString = SelfOrganizedMap.extractValue(firstLine);
		    for (int i=0; i<CompoundTableSOM.SOM_TYPE_FILE.length; i++)
		        if (typeString.equals(CompoundTableSOM.SOM_TYPE_FILE[i]))
		            somType = i;
		    if (somType == -1) {
				theReader.close();
				throw new IOException("Unknown SOM type found.");
				}
			}
		else {
			theReader.close();
			throw new IOException("Invalid SOM file format");
			}

        CompoundTableSOM som = new CompoundTableSOM(tableModel, somType);	// in case of ACTION_ANALYSE tableModel is not used, but must not be null
        som.addProgressListener(pc);
        som.setThreadMaster(pc);
        som.read(theReader);

		String lastLine = theReader.readLine();
		if (!lastLine.equals("</datawarrior SOM>")	// old VectorSOMs without type declaration
		 && !lastLine.equals("</datawarriorSOM>")) {
			theReader.close();
			throw new IOException("SOM file corrupted.");
			}

		theReader.close();
		return som;
		}
	}
