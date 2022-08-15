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

package com.actelion.research.datawarrior.task.chem.clib;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.SSSearcher;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.chem.reaction.Reactor;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.TaskUIDelegate;
import com.actelion.research.gui.LookAndFeelHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Properties;

public class DETaskEnumerateCombinatorialLibrary extends AbstractTask implements TaskConstantsCLib {
	public static final String TASK_NAME = "Enumerate Combinatorial Library";

	private DEFrame			mParentFrame;
	private DataWarrior		mApplication;
	private DEFrame			mTargetFrame;

	public DETaskEnumerateCombinatorialLibrary(DEFrame owner, DataWarrior application) {
		super(owner, true);

		mParentFrame = owner;
		mApplication = application;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#VirtualLibraries";
	}

	@Override
	public TaskUIDelegate createUIDelegate() {
		return new UIDelegateCLib(mParentFrame);
	}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		Reaction reaction = ReactionEncoder.decode(configuration.getProperty(PROPERTY_REACTION), false);
		if (reaction == null) {
			showErrorMessage("Missing definition of generic reaction.");
			return false;
		}

		int reactantCount = (reaction == null) ? 0 : reaction.getReactants();

		boolean found = false;
		for (int i = 0; i < reactantCount; i++)
			if (configuration.getProperty(PROPERTY_REACTANT + i, "").length() != 0)
				found = true;
		if (!found) {
			String msg = "There are no structures assigned to any of the "+reactantCount+" generic reactants.";
			if (isInteractive())
				msg = msg.concat("\nPlease open the 'Reactants' panel and define reactant structures.");
			showErrorMessage(msg);
			return false;
			}

		for (int i = 0; i < reactantCount; i++) {
			String reactants = configuration.getProperty(PROPERTY_REACTANT + i, "");
			if (reactants.length() == 0) {
				showErrorMessage("Missing compound structures for generic reactant " + i + ".");
				return false;
			}
			SSSearcher searcher = new SSSearcher();
			searcher.setFragment(reaction.getReactant(i));
			int count = 0;
			for (String idcode : reactants.split("\\t")) {
				StereoMolecule mol = new IDCodeParser().getCompactMolecule(idcode);
				if (mol != null) {
					searcher.setMolecule(mol);
					if (searcher.isFragmentInMolecule(SSSearcher.cMatchAromDBondToDelocalized))
						count++;
				}
				if (count == 0) {
					showErrorMessage("None of the reactants given for generic reactant " + i + " match its substructure.");
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void runTask(Properties configuration) {
		Reaction reaction = ReactionEncoder.decode(configuration.getProperty(PROPERTY_REACTION), false);

		startProgress("Parsing Reactants...", 0, 0);

		StereoMolecule[][] reactant = new StereoMolecule[reaction.getReactants()][];
		int rowCount = 1;
		for (int i=0; i<reaction.getReactants(); i++) {
			String[] idcode = configuration.getProperty(PROPERTY_REACTANT+i).split("\\t");
			String[] name = configuration.getProperty(PROPERTY_REACTANT_NAME+i, "").split("\\t");
			if (name.length != idcode.length) {
				System.out.println("WARNING: counts of idcodes and IDs don't match.");
				name = null;
			}
			rowCount *= idcode.length;
			ArrayList<StereoMolecule> reactantList = new ArrayList<>();
			for (int j=0; j<idcode.length; j++) {
				StereoMolecule mol = new IDCodeParser().getCompactMolecule(idcode[j]);
				if (mol != null) {
					reactantList.add(mol);
					if (name != null && name[j] != null && name[j].length() != 0)
						mol.setName(name[j]);
				}
			}
			reactant[i] = reactantList.toArray(new StereoMolecule[0]);
		}

		startProgress("Creating Products...", 0, rowCount);

		boolean oneProductOnly = (findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, 0) == 0);
		Reactor reactor = new Reactor(reaction, Reactor.MODE_RETAIN_COORDINATES
				+Reactor.MODE_FULLY_MAP_REACTIONS+Reactor.MODE_REMOVE_DUPLICATE_PRODUCTS+Reactor.MODE_ALLOW_CHARGE_CORRECTIONS,
				oneProductOnly ? 1 : Integer.MAX_VALUE);
		int dimensions = reaction.getReactants();
		for (int i=0; i<dimensions; i++)
			reactor.setReactant(i, reactant[i][0]);
		int[] index = new int[dimensions];
		ArrayList<Object[]> recordList = new ArrayList<Object[]>();
		int row = 0;
		BufferedWriter writer = null;

//try { writer = new BufferedWriter(new FileWriter("/home/thomas/test.rdf"));

		while (row < rowCount) {

//Reaction[] rxn = reactor.getFullyMappedReactions();
//for (Reaction r:rxn)
// new RXNFileCreator(r).writeRXNfile(writer);

			StereoMolecule[][] product = reactor.getProducts();
			for (int p=0; p<Math.max(1,product.length); p++) {
				Object[] record = new Object[2+3*reaction.getReactants()];
				if (product.length != 0 && product[p][0] != null) {
					Canonizer canonizer = new Canonizer(product[p][0]);
					record[0] = canonizer.getIDCode().getBytes();
					record[1] = canonizer.getEncodedCoordinates().getBytes();
				}
				for (int i=0; i<dimensions; i++) {
					StereoMolecule mol = reactant[i][index[i]];
					String id = mol.getName();
					record[2+i] = (id!=null) ? id.getBytes() : Integer.toString(index[i]+1).getBytes();
					Canonizer canonizer = new Canonizer(mol);
					record[2+dimensions+2*i] = canonizer.getIDCode().getBytes();
					record[3+dimensions+2*i] = canonizer.getEncodedCoordinates().getBytes();
				}
				recordList.add(record);
			}

			int currentDimension = dimensions-1;
			while (currentDimension >= 0
					&& index[currentDimension] == reactant[currentDimension].length-1) {
				index[currentDimension] = 0;
				reactor.setReactant(currentDimension, reactant[currentDimension][0]);
				currentDimension--;
			}
			if (currentDimension < 0)
				break;

			index[currentDimension]++;
			reactor.setReactant(currentDimension, reactant[currentDimension][index[currentDimension]]);

			if (threadMustDie())
				break;

			updateProgress(row++);
		}

//writer.close();
//} catch (IOException e) { e.printStackTrace(); }

		if (!threadMustDie())
			populateTable(reaction, recordList);

		if (!threadMustDie())
			setRuntimeSettings(dimensions);
	}

	private void populateTable(Reaction reaction, ArrayList<Object[]> recordList) {
		mTargetFrame = mApplication.getEmptyFrame("Combinatorial Library");
		startProgress("Populating Table...", 0, recordList.size());

		CompoundTableModel tableModel = mTargetFrame.getTableModel();
		tableModel.initializeTable(recordList.size(), 3+3*reaction.getReactants());
		tableModel.prepareStructureColumns(0, "Product", true, true);
		for (int i=0; i<reaction.getReactants(); i++) {
			tableModel.setColumnName("Reactant-ID "+(i+1), 3+i);
			tableModel.prepareStructureColumns(3+reaction.getReactants()+2*i, "Reactant "+(i+1), true, false);
		}

		int row = 0;
		for (Object[] record:recordList) {
			tableModel.setTotalDataAt(record[0], row, 0);
			tableModel.setTotalDataAt(record[1], row, 1);
			for (int column=2; column<record.length; column++)
				tableModel.setTotalDataAt(record[column], row, column+1);

			updateProgress(row++);
		}

		tableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, this);
	}

	private void setRuntimeSettings(final int dimensions) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
//				mTargetFrame.getMainFrame().getMainPane().removeAllViews();
//				mTargetFrame.getMainFrame().getMainPane().addTableView("Table", null);
				mTargetFrame.getMainFrame().getMainPane().addStructureView("Structure", "Table\tbottom\t0.5", 0);

				mTargetFrame.getMainFrame().getPruningPanel().removeAllFilters();	// this is the automatically added list filter
				mTargetFrame.getMainFrame().getPruningPanel().addDefaultFilters();

				VisualizationPanel vpanel = null;
				if (dimensions == 2)
					vpanel = mTargetFrame.getMainFrame().getMainPane().add2DView("2D-View", "Structure\tright\t0.5");
				if (dimensions == 3)
					vpanel = mTargetFrame.getMainFrame().getMainPane().add3DView("3D-View", "Structure\tright\t0.5");

				if (vpanel != null) {
					for (int i=0; i<dimensions; i++) {
						vpanel.setAxisColumnName(i, "Reactant "+(i+1));
						JVisualization visualization = vpanel.getVisualization();
						if (LookAndFeelHelper.isDarkLookAndFeel())
							visualization.setViewBackground(new Color(32,0,64));
						visualization.setMarkerSize(0.6f, false);
						visualization.setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);
						visualization.getMarkerColor().setColor(mTargetFrame.getTableModel().getChildColumn(0, DescriptorConstants.DESCRIPTOR_FFP512.shortName));
					}
				}
			}
		} );
	}
}
