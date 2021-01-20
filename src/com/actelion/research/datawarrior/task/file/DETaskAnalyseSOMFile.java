package com.actelion.research.datawarrior.task.file;

import java.awt.Color;
import java.io.File;
import java.util.Properties;

import javax.swing.SwingUtilities;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.CompoundTableSOM;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.table.view.VisualizationPanel2D;

public class DETaskAnalyseSOMFile extends DETaskAbstractOpenFile {
	public static final String TASK_NAME = "Analyse SOM-File";

    public DETaskAnalyseSOMFile(DataWarrior application) {
		super(application, "Open SOM-File And Analyse Content", FileHelper.cFileTypeSOM);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public DEFrame openFile(File file, Properties configuration) {
		final DEFrame newFrame = getApplication().getEmptyFrame("SOM Analysis of "+file.getName());
		try {
			CompoundTableSOM som = DETaskApplySOMFile.readSOMFile(file, newFrame.getTableModel(), getProgressController());
			som.createSimilarityMap(newFrame.getTableModel());
			newFrame.setTitle("Dissimilarity Map of "+file.getName());
			}
		catch (Exception e) {
			showErrorMessage(e.getMessage());
			newFrame.getMainFrame().getTableModel().unlock();
			return null;
			}

		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
                    VisualizationPanel2D vpanel1 = newFrame.getMainFrame().getMainPane().add2DView("SOM", null);
                    vpanel1.setAxisColumnName(0, CompoundTableSOM.SOM_ANALYSIS_COLUMN_NAME[0]);
                    vpanel1.setAxisColumnName(1, CompoundTableSOM.SOM_ANALYSIS_COLUMN_NAME[1]);
                    ((JVisualization2D)vpanel1.getVisualization()).setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);
                    int colorListMode = VisualizationColor.cColorListModeHSBLong;
                    Color[] colorList = VisualizationColor.createColorWedge(Color.red, Color.blue, colorListMode, null);
                    vpanel1.getVisualization().getMarkerColor().setColor(2, colorList, colorListMode);
	                }
				} );
			}
		catch (Exception ie) {}

		return newFrame;
		}
	}
