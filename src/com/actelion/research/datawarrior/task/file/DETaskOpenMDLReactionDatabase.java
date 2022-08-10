package com.actelion.research.datawarrior.task.file;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.ExtendedMolecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.io.NativeMDLReactionReader;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

public class DETaskOpenMDLReactionDatabase extends DETaskAbstractOpenFile {
	public static final String TASK_NAME = "Open MDL Reaction Database";

	private DataWarrior mApplication;
	private NativeMDLReactionReader mReader;

	public DETaskOpenMDLReactionDatabase(DataWarrior application) {
		super(application, "Open MDL Reaction Database", FileHelper.cFileTypeDirectory);
		mApplication = application;
		}

	public DETaskOpenMDLReactionDatabase(DataWarrior application, String filePath) {
		super(application, "Open MDL Reaction Database", FileHelper.cFileTypeDirectory, filePath);
		mApplication = application;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public DEFrame openFile(File file, Properties configuration) {
		DEFrame targetFrame = null;
		try {
			mReader = new NativeMDLReactionReader(file.getPath());
			if (mReader.getReactionCount() > 0)
				targetFrame = readData(file.getName());
			}
		catch (IOException e) {
			showErrorMessage(e.toString());
			}
		catch (OutOfMemoryError e) {
			showErrorMessage("Out of memory. Launch DataWarrior with Java option -Xms???m or -Xmx???m.");
			}

		return targetFrame;
		}

	private DEFrame readData(String frameName) {
		int rows = mReader.getReactionCount();
		Object[] fieldName = mReader.getFieldNames();

		String[][] rxnList = new String[rows][];
		byte[][] catalystList = new byte[rows][];
		byte[][] solventList = new byte[rows][];
		byte[][][] fieldDataList = new byte[rows][fieldName.length][];
		byte[][][] moleculeDataList = new byte[rows][4][];

		StereoMolecule catalysts = new StereoMolecule();
		StereoMolecule solvents = new StereoMolecule();

		final DEFrame targetFrame = mApplication.getEmptyFrame(frameName);

		int failed = 0;

		getProgressController().startProgress("Reading reactions...", 0, rows);
		for (int row=0; row<rows; row++) {
			if ((row & 0x0F) == 0x0F) {
				getProgressController().updateProgress(row);
				}

			if (getProgressController().threadMustDie()) {
				rows = row;
				break;
				}

			try {
				Reaction rxn = mReader.getReaction(row, 0);
				rxnList[row] = ReactionEncoder.encode(rxn, false);
				if (rxnList[row] == null)
					failed++;

				try {
					catalysts.deleteMolecule();
					ArrayList<ExtendedMolecule> list = mReader.getCatalysts();
					for (int i=0; i<list.size(); i++)
						catalysts.addMolecule(list.get(i));
					catalystList[row] = new Canonizer(catalysts).getIDCode().getBytes();
					}
				catch (IllegalArgumentException iae) {}
				try {
					solvents.deleteMolecule();
					ArrayList<ExtendedMolecule> list = mReader.getSolvents();
					for (int i=0; i<list.size(); i++)
						solvents.addMolecule(list.get(i));
					solventList[row] = new Canonizer(solvents).getIDCode().getBytes();
					}
				catch (IllegalArgumentException iae) {}

				String[] fieldData = mReader.getFieldData(row, 0);
				for (int i=0; i<fieldData.length; i++)
					fieldDataList[row][i] = fieldData[i] == null ? null : fieldData[i].getBytes();

				String reactantData = mReader.getReactantData();
				moleculeDataList[row][0] = reactantData == null ? null : reactantData.getBytes();
				String productData = mReader.getProductData();
				moleculeDataList[row][1] = productData == null ? null : productData.getBytes();
				String catalystData = mReader.getCatalystData();
				moleculeDataList[row][2] = catalystData == null ? null : catalystData.getBytes();
				String solventData = mReader.getSolventData();
				moleculeDataList[row][3] = solventData == null ? null : solventData.getBytes();
				}
			catch (FileNotFoundException fnfe) {
				showErrorMessage(fnfe.getMessage());
				return targetFrame;
				}
			catch (IOException e) {
				System.out.println("NativeMDLReactionReader.getReaction() failed:"+e.getMessage()+" Stacktrace:");
				e.printStackTrace();
				failed++;
				}
			}

//        mReader.printPointerStatistics();

		int pointerErrors = mReader.getPointerErrors();

		CompoundTableModel tableModel = targetFrame.getTableModel();
		String[] specialFieldName = { "Registry No", "Reaction", "mapping", "coords", "Reaction Catalysts", "rxnfp", "reactantffp",
				"productffp", "catalystffp", "Solvents", "Reactant Data", "Product Data", "Catalyst Data", "Solvent Data" };
		tableModel.initializeTable(rows-failed, specialFieldName.length+fieldName.length);
		for (int i=0; i<specialFieldName.length; i++)
			tableModel.setColumnName(specialFieldName[i], i);
		for (int i=0; i<fieldName.length; i++)
			tableModel.setColumnName((String)fieldName[i], specialFieldName.length+i);
		int targetRow = 0;
		for (int row=0; row<rows; row++) {
			if (rxnList[row] != null) {
				tableModel.setTotalValueAt(""+(row+1), targetRow, 0);
				if (rxnList[row] != null) {
					if (rxnList[row][0] != null && rxnList[row][0].length() != 0)
						tableModel.setTotalDataAt(rxnList[row][0].getBytes(), targetRow, 1);
					if (rxnList[row][1] != null && rxnList[row][1].length() != 0)
						tableModel.setTotalDataAt(rxnList[row][1].getBytes(), targetRow, 2);
					if (rxnList[row][2] != null && rxnList[row][2].length() != 0)
						tableModel.setTotalDataAt(rxnList[row][2].getBytes(), targetRow, 3);
					tableModel.setTotalDataAt(catalystList[row], targetRow, 4);
				}
				tableModel.setTotalDataAt(solventList[row], targetRow, 9);
				for (int i=0; i<4; i++)
					tableModel.setTotalDataAt(moleculeDataList[row][i], targetRow, 10+i);
				for (int i=0; i<fieldName.length; i++)
					tableModel.setTotalDataAt(fieldDataList[row][i], targetRow, specialFieldName.length+i);

				targetRow++;
				}
			}

		tableModel.prepareReactionColumns(1, "Reaction", false, true,
				true, true, true, true, true, true);
		tableModel.setColumnProperty(9, CompoundTableModel.cColumnPropertySpecialType, CompoundTableModel.cColumnTypeIDCode);
		if (failed != 0 || pointerErrors != 0) {
			String style = "<style>\n.s1 {color:red;}\n.s2 {color:green;}\n</style>\n";
			String msg1 = (failed == 0) ? "" : "<h3 class=s1>" + failed + " reactions coun't be loaded because of SEMA errors.</h3>\n";
			String msg2 = (pointerErrors == 0) ? "" : "<h3 class=s2>" + pointerErrors + " values couldn't be read because of pointer errors.</h3>\n";
			tableModel.setExtensionData(CompoundTableConstants.cExtensionNameFileExplanation, style+msg1+msg2);
			}
		tableModel.finalizeTable(CompoundTableEvent.cSpecifierDefaultFilters, getProgressController());

		return targetFrame;
		}
	}
