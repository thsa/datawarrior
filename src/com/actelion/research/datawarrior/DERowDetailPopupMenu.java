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

import com.actelion.research.chem.*;
import com.actelion.research.chem.coords.CoordinateInventor;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.datawarrior.chem.InchiCreator;
import com.actelion.research.datawarrior.task.chem.DETaskSortReactionsBySimilarity;
import com.actelion.research.datawarrior.task.chem.DETaskSortStructuresBySimilarity;
import com.actelion.research.datawarrior.task.table.DETaskCopyTableCells;
import com.actelion.research.datawarrior.task.table.DETaskPasteIntoTable;
import com.actelion.research.datawarrior.task.view.cards.*;
import com.actelion.research.gui.JScrollableMenu;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.table.filter.JMultiStructureFilterPanel;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.*;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.view.card.positioning.CardNumericalShaper1D;
import com.actelion.research.table.view.card.positioning.CardPositionerInterface;
import com.actelion.research.table.view.card.positioning.SpiralOutPositioner;
import com.actelion.research.table.view.card.positioning.XYSorter;
import com.actelion.research.util.BrowserControl;
import com.actelion.research.util.Platform;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.chem.conf.so.ConformationSelfOrganizer;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.*;
import java.util.prefs.Preferences;

public class DERowDetailPopupMenu extends JPopupMenu implements ActionListener {
	private static final long serialVersionUID = 0x20060904;

	private static final String GOOGLE_PATENT_URL = "https://patents.google.com/?q=%s&oq=%s";
	private static final String GOOGLE_SCHOLAR_URL = "https://patents.google.com/?q=%s&patents=false&scholar&oq=%s";

	private static final String SPAYA_DEFAULT = "spaya.ai";
	private static final String SPAYA_URL = "https://"+SPAYA_DEFAULT+"/retro/21f39dab-8add-4a96-98b6-fa3efaf8e471?smiles=%s";


	/**
	 * Variables needed for card actions
	 */
	private CardElement mCEMouseOver = null;

	private static final String DELIMITER = "@#|";
	private static final String COPY_STRUCTURE = "structure" + DELIMITER;
	private static final String COPY_STRUCTURE_DECORATED = "decorated" + DELIMITER;	// e.g. with pKa coloring & labels
	private static final String COPY_REACTION = "reaction" + DELIMITER;
	private static final String COPY_IDCODE = "idcode" + DELIMITER;
	private static final String COPY_SMILES = "smiles" + DELIMITER;
	private static final String COPY_INCHI = "inchi" + DELIMITER;
	private static final String COPY_INCHI_KEY = "inchikey" + DELIMITER;
	private static final String COPY_RXN_SMILES = "rxnsmiles" + DELIMITER;
	private static final String COPY_MOLFILE2 = "molfile2" + DELIMITER;
	private static final String COPY_MOLFILE3 = "molfile3" + DELIMITER;
	private static final String COPY_VALUE = "value" + DELIMITER;
	private static final String COPY_WITH_HEADER = "withHeader" + DELIMITER;
	private static final String COPY_WITHOUT_HEADER = "withoutHeader" + DELIMITER;
	private static final String PASTE_STRUCTURE = "pstructure" + DELIMITER;
	private static final String PASTE_IDCODE = "pidcode" + DELIMITER;
	private static final String PASTE_SMILES = "psmiles" + DELIMITER;
	private static final String PASTE_MOLFILE = "pmolfile" + DELIMITER;
	private static final String PASTE_INTO = "pasteInto" + DELIMITER;
	protected static final String EDIT_VALUE = "edit" + DELIMITER;
	private static final String SORT = "sort" + DELIMITER;
	private static final String ADD_TO_LIST = "add" + DELIMITER;
	private static final String REMOVE_FROM_LIST = "remove" + DELIMITER;
	private static final String PATENT_SEARCH = "patentSearch" + DELIMITER;
	private static final String SCHOLAR_SEARCH = "scholarSearch" + DELIMITER;
	private static final String SPAYA_SEARCH = "spayaSearch" + DELIMITER;
	private static final String SPAYA_CHANGE_URL = "spayaChangeURL";

	private static final String ICSYNTH_SEARCH = "icsynthSearch" + DELIMITER;
	private static final String LOOKUP = "lookup" + DELIMITER;
	private static final String LAUNCH = "launch" + DELIMITER;
	private static final String OPEN_EXTERNAL = "openExternal" + DELIMITER;
	private static final String CONFORMERS = "Explore conformers of '";
	private static final String NEW_FILTER = "filter";
	private static final String NEW_FILTER_THIS = "filterT" + DELIMITER;
	private static final String NEW_FILTER_SELECTED = "filterS" + DELIMITER;
	private static final String NEW_FILTER_VISIBLE = "filterV" + DELIMITER;
	private static final String STEREO_DISPLAY_MODE = "stereoDMode" + DELIMITER;
	private static final String COLOR_DISPLAY_MODE = "colorDMode" + DELIMITER;

	// Card view options are all context sensitive
	private static final String TEXT_CARD_STACK     = DETaskCreateStackFromSelection.TASK_NAME;
	private static final String TEXT_CARD_POSITION  = "Position Cards...";
	private static final String TEXT_CARD_SET_STACK_NAME  = "Set Stack Name...";
	private static final String TEXT_CARD_CREATE_CATEGORY_COLUMN_FROM_STACKS = "Create Category Column from Stacks";
	//private static final String TEXT_CARD_WIZARD    = "Card Wizard...";

	//private static final String TEXT_CARD_POS_1D_GRID     = "Card-Sort-1D-Grid";
	//private static final String TEXT_CARD_POS_SPIRAL      = "Card-Sort-1D-Spiral";
	//private static final String TEXT_CARD_POS_2D_GRID     = "Card-Sort-2D-Grid";
	private static final String TEXT_CARD_STACK_FOR_COLUMN = "Card-Stack-ForColumn";
	private static final String TEXT_CARD_EXPAND_STACK     = "Card-Expand-Stack";

	private static final String TEXT_CARD_FROM_SELECTION_CREATE_SUBSTACKS_FROM_CAT     = "Card-From-Selection-Create-Substacks-Cat";
	private static final String TEXT_CARD_FROM_SELECTION_CREATE_SUBSTACKS_FROM_HITLIST = "Card-From-Selection-Create-Substacks-HL";
	private static final String TEXT_CARD_FROM_SELECTION_EXPAND_STACKS                 = "Card-From-Selection-Expand-Stacks";
	private static final String TEXT_CARD_FROM_SELECTION_SORT_1D_GRID                  = "Card-From-Selection-Sort-1D-Grid";
	private static final String TEXT_CARD_FROM_SELECTION_SORT_1D_SPIRAL                = "Card-From-Selection-Sort-1D-Spiral";
	private static final String TEXT_CARD_FROM_SELECTION_SORT_2D                       = "Card-From-Selection-Sort-2D";

	private static final String TEXT_CARD_FROM_ALL_CREATE_SUBSTACKS_FROM_CAT     = "Card-From-All-Create-Substacks-Cat";
	private static final String TEXT_CARD_FROM_ALL_CREATE_SUBSTACKS_FROM_HITLIST = "Card-From-All-Create-Substacks-HL";
	private static final String TEXT_CARD_FROM_ALL_EXPAND_STACKS                 = "Card-From-All-Expand-Stacks";
	private static final String TEXT_CARD_FROM_ALL_SORT_1D_GRID                  = "Card-From-All-Sort-1D-Grid";
	private static final String TEXT_CARD_FROM_ALL_SORT_1D_SPIRAL                = "Card-From-All-Sort-1D-Spiral";
	private static final String TEXT_CARD_FROM_ALL_SORT_2D                       = "Card-From-All-Sort-2D";
	private static final String TEXT_CARD_FROM_ALL_ARRANGE                       = "Card-From-All-Arrange";

	private static final String TEXT_CARD_FROM_SELECTION_ARRANGE                 = "Card-From-Selection-Arrange";


	private DEMainPane			mMainPane;
	private CompoundTableModel	mTableModel;
	private CompoundRecord		mRecord;
	private DEPruningPanel		mPruningPanel;
	private CompoundTableView	mSource;
	private DatabaseActions		mDatabaseActions;

	/**
	 * Creates a context dependent popup menu presenting options for one record.
	 * @param mainPane
	 * @param record
	 * @param pruningPanel
	 * @param source
	 * @param selectedColumn if the source view provides it, otherwise -1
	 */
	public DERowDetailPopupMenu(DEMainPane mainPane, CompoundRecord record,
	                            DEPruningPanel pruningPanel, CompoundTableView source,
	                            DatabaseActions databaseActions, int selectedColumn, boolean isCtrlDown) {
		super();

		mMainPane = mainPane;
		mTableModel = mainPane.getTableModel();
		mRecord = record;
		mPruningPanel = pruningPanel;
		mSource = source;
		mDatabaseActions = databaseActions;

		if (record != null) {
			if(selectedColumn != -1 && System.getProperty("development") != null) {
				addMenuItem("Do Test Stuff", "TEST"+DELIMITER+mTableModel.getColumnTitle(selectedColumn));
				addSeparator();
				}

			ArrayList<String> rxncodeColumnList = new ArrayList<>();
			if (selectedColumn == -1) {
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
					if (mTableModel.isColumnTypeReaction(column))
						rxncodeColumnList.add(mTableModel.getColumnTitle(column));
				}
			else {
				if (mTableModel.isColumnTypeReaction(selectedColumn))
					rxncodeColumnList.add(mTableModel.getColumnTitle(selectedColumn));
				}

			ArrayList<String> idcodeColumnList = new ArrayList<>();
			if (selectedColumn == -1) {
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
					if (mTableModel.isColumnTypeStructure(column))
						idcodeColumnList.add(mTableModel.getColumnTitle(column));
				}
			else {
				if (mTableModel.isColumnTypeStructure(selectedColumn))
					idcodeColumnList.add(mTableModel.getColumnTitle(selectedColumn));
				}

			if (selectedColumn == -1) {
				JMenu copyMenu = new JScrollableMenu("Copy");
				for (String columnName:rxncodeColumnList) {
					JMenu copyReactionMenu = new JMenu(columnName+" As");
					addCopyReactionItems(copyReactionMenu, mTableModel.findColumn(columnName), columnName);
					copyMenu.add(copyReactionMenu);
					}
				for (String columnName:idcodeColumnList) {
					JMenu copyStructureMenu = new JMenu(columnName+" As");
					addCopyStructureItems(copyStructureMenu, mTableModel.findColumn(columnName), columnName);
					copyMenu.add(copyStructureMenu);
					}
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
					if (mTableModel.isColumnDisplayable(column)
					 && mTableModel.getColumnSpecialType(column) == null)
						addSubmenuItem(copyMenu, mTableModel.getColumnTitle(column), COPY_VALUE+mTableModel.getColumnTitle(column));
				add(copyMenu);
				}
			else {
				if (mTableModel.isColumnDisplayable(selectedColumn)) {
					if (mTableModel.isColumnTypeReaction(selectedColumn)) {
						JMenu copyMenu = new JMenu("Copy From Reaction");
						addCopyReactionItems(copyMenu, selectedColumn, mTableModel.getColumnTitle(selectedColumn));
						add(copyMenu);
						}
					else if (mTableModel.isColumnTypeStructure(selectedColumn)) {
						JMenu copyMenu = new JMenu("Copy Structure As");
						addCopyStructureItems(copyMenu, selectedColumn, mTableModel.getColumnTitle(selectedColumn));
						add(copyMenu);
						}
					else if (source instanceof DETableView) {
						addMenuItem("Copy Cell", COPY_VALUE+mTableModel.getColumnTitle(selectedColumn));
						}

					if (source instanceof DETableView) {
						addMenuItem("Copy Selection With Header", COPY_WITH_HEADER + mTableModel.getColumnTitle(selectedColumn));
						addMenuItem("Copy Selection Without Header", COPY_WITHOUT_HEADER + mTableModel.getColumnTitle(selectedColumn));
						}

					if (mTableModel.isColumnTypeStructure(selectedColumn)) {
						JMenu pasteMenu = new JMenu("Paste Structure From");
						addPasteStructureItems(pasteMenu, selectedColumn, mTableModel.getColumnTitle(selectedColumn));
						add(pasteMenu);
						}

					if (source instanceof DETableView) {
						addMenuItem("Paste Into Table", PASTE_INTO + mTableModel.getColumnTitle(selectedColumn));
						}
					}
				}

			addSeparator();
			if (selectedColumn == -1) {
				JMenu editMenu = new JScrollableMenu("Edit");
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
					if (mTableModel.isColumnDisplayable(column)) {
						String columnType = mTableModel.getColumnSpecialType(column);
						if (columnType == null
						 || columnType.equals(CompoundTableModel.cColumnTypeIDCode)
						 || columnType.equals(CompoundTableModel.cColumnTypeRXNCode))
							addSubmenuItem(editMenu, mTableModel.getColumnTitle(column), EDIT_VALUE+mTableModel.getColumnTitleNoAlias(column));
						}
				add(editMenu);
				}
			else {
				String columnType = mTableModel.getColumnSpecialType(selectedColumn);
				if (columnType == null
				 || columnType.equals(CompoundTableModel.cColumnTypeIDCode)
				 || columnType.equals(CompoundTableModel.cColumnTypeRXNCode))
					addMenuItem("Edit Cell", EDIT_VALUE + mTableModel.getColumnTitleNoAlias(selectedColumn));
				}

			if ((selectedColumn != -1 && mTableModel.isColumnTypeStructure(selectedColumn))
			 || source instanceof JStructureGrid) {
				int idcodeColumn = (source instanceof JStructureGrid) ? ((JStructureGrid)source).getStructureColumn() : selectedColumn;
				JMenu sortMenu = new JMenu("Sort Table Rows By");
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
					if (mTableModel.getParentColumn(column) == idcodeColumn
					 && mTableModel.isDescriptorColumn(column)
					 && mTableModel.isDescriptorAvailable(column)) {
						addSubmenuItem(sortMenu, mTableModel.getDescriptorHandler(column).getInfo().shortName + " Similarity To This Molecule",
								SORT + mTableModel.getColumnTitleNoAlias(column));
						}
					}
				addSeparator();
				add(sortMenu);
				}

			if (selectedColumn != -1 && mTableModel.isColumnTypeReaction(selectedColumn)) {
				JMenu sortMenu = new JMenu("Sort Table Rows By");
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
					if (mTableModel.getParentColumn(column) == selectedColumn
					 && mTableModel.isDescriptorColumn(column)
					 && mTableModel.isDescriptorAvailable(column)
					 && mTableModel.getDescriptorHandler(column).getInfo().type == DescriptorConstants.DESCRIPTOR_TYPE_REACTION) {
						addSubmenuItem(sortMenu, mTableModel.getDescriptorHandler(column).getInfo().shortName +" Similarity To This Reaction",
								SORT + mTableModel.getColumnTitleNoAlias(column));
						}
					}
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
					if (mTableModel.getParentColumn(column) == selectedColumn
					 && mTableModel.isDescriptorColumn(column)
					 && mTableModel.isDescriptorAvailable(column)
					 && mTableModel.getDescriptorHandler(column).getInfo().type == DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE) {
						String reactionPart = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyReactionPart);
						addSubmenuItem(sortMenu, mTableModel.getDescriptorHandler(column).getInfo().shortName +" Similarity To "+reactionPart,
								SORT + mTableModel.getColumnTitleNoAlias(column) + DELIMITER + reactionPart);
						}
					}
				addSeparator();
				add(sortMenu);
				}

			CompoundTableListHandler hh = mTableModel.getListHandler();
			if (hh.getListCount() != 0) {
				DEScrollableMenu hitlistAddMenu = null;
				DEScrollableMenu hitlistRemoveMenu = null;
				for (int i = 0; i<hh.getListCount(); i++) {
					if (record.isFlagSet(hh.getListFlagNo(i))) {
						if (hitlistRemoveMenu == null)
							hitlistRemoveMenu = new DEScrollableMenu("Remove Row From List");
						addSubmenuItem(hitlistRemoveMenu, hh.getListName(i), REMOVE_FROM_LIST+hh.getListName(i));
						}
					else {
						if (hitlistAddMenu == null)
							hitlistAddMenu = new DEScrollableMenu("Add Row To List");
						addSubmenuItem(hitlistAddMenu, hh.getListName(i), ADD_TO_LIST+hh.getListName(i));
						}
					}
				addSeparator();
				if (hitlistAddMenu != null)
					add(hitlistAddMenu);
				if (hitlistRemoveMenu != null)
					add(hitlistRemoveMenu);
				}

			if (rxncodeColumnList.size() != 0) {
				addSeparator();
				JMenu filterMenu = new JMenu("New Reaction Filter from");
				for (String columnName:rxncodeColumnList) {
					addSubmenuItem(filterMenu, "this "+columnName, NEW_FILTER_THIS+columnName);
//					addSubmenuItem(filterMenu, "selected "+columnName+"s", NEW_FILTER_SELECTED+columnName);  not supported yet
//					addSubmenuItem(filterMenu, "visible "+columnName+"s", NEW_FILTER_VISIBLE+columnName);
					}
				add(filterMenu);
				}

			if (idcodeColumnList.size() != 0) {
				addSeparator();
				JMenu filterMenu = new JMenu("New Structure Filter from");
				for (String columnName:idcodeColumnList) {
					addSubmenuItem(filterMenu, "this "+columnName, NEW_FILTER_THIS+columnName);
					addSubmenuItem(filterMenu, "selected "+columnName+"s", NEW_FILTER_SELECTED+columnName);
					addSubmenuItem(filterMenu, "visible "+columnName+"s", NEW_FILTER_VISIBLE+columnName);
					}
				add(filterMenu);

				for (String columnName : idcodeColumnList) {
					addMenuItem(CONFORMERS+columnName+"'");
					}
				}

			if (idcodeColumnList.size() != 0 || rxncodeColumnList.size() != 0) {
				addSeparator();
				if (idcodeColumnList.size() == 1 && rxncodeColumnList.size() == 0) {
					String columnName = idcodeColumnList.get(0);
					addMenuItem("Search "+columnName+" in Google Scholar", SCHOLAR_SEARCH+columnName);
					addMenuItem("Search "+columnName+" in Google Patents", PATENT_SEARCH+columnName);
					addSeparator();
					JMenu predictSynthesisMenu = new JMenu("Suggest Synthesis Route");
					addSubmenuItem(predictSynthesisMenu, "of "+columnName+" using Spaya.ai", SPAYA_SEARCH+columnName
							/* ,"Press <Ctrl> when opening menu to change Spaya server" */ );
					if (isCtrlDown)
						addSubmenuItem(predictSynthesisMenu, "Change SPAYA Server URL", SPAYA_CHANGE_URL);
//	TODO			addSubmenuItem(predictSynthesisMenu, "of "+columnName+" using ICSynth Light", ICSYNTH_SEARCH+columnName);
					add(predictSynthesisMenu);
					}
				else {
					JMenu scholarMenu = new JMenu("Search in Google Scholar");
					for (String columnName:idcodeColumnList) {
						addSubmenuItem(scholarMenu, columnName, SCHOLAR_SEARCH+columnName);
						}
					for (String columnName:rxncodeColumnList) {
						addSubmenuItem(scholarMenu, columnName+" reactants", SCHOLAR_SEARCH+columnName+DELIMITER+CompoundTableConstants.cReactionPartReactants);
						addSubmenuItem(scholarMenu, columnName+" products", SCHOLAR_SEARCH+columnName+DELIMITER+CompoundTableConstants.cReactionPartProducts);
						}
					add(scholarMenu);

					JMenu patentsMenu = new JMenu("Search in Google Patents");
					for (String columnName:idcodeColumnList) {
						addSubmenuItem(patentsMenu, columnName, PATENT_SEARCH+columnName);
						}
					for (String columnName:rxncodeColumnList) {
						addSubmenuItem(patentsMenu, columnName+" reactants", PATENT_SEARCH+columnName+DELIMITER+CompoundTableConstants.cReactionPartReactants);
						addSubmenuItem(patentsMenu, columnName+" products", PATENT_SEARCH+columnName+DELIMITER+CompoundTableConstants.cReactionPartProducts);
						}
					add(patentsMenu);

					addSeparator();
					JMenu predictSynthesisMenu = new JMenu("Suggest Synthesis");
					for (String columnName:idcodeColumnList) {
						addSubmenuItem(predictSynthesisMenu, "of "+columnName+" using Spaya.ai", SPAYA_SEARCH+columnName);
//	TODO				addSubmenuItem(predictSynthesisMenu, "of "+columnName+" using ICSynth Light", ICSYNTH_SEARCH+columnName);
						}
					for (String columnName:rxncodeColumnList) {
						addSubmenuItem(predictSynthesisMenu, "of "+columnName+" reactants using Spaya.ai", SPAYA_SEARCH+columnName+DELIMITER+CompoundTableConstants.cReactionPartReactants);
						addSubmenuItem(predictSynthesisMenu, "of "+columnName+" products using Spaya.ai", SPAYA_SEARCH+columnName+DELIMITER+CompoundTableConstants.cReactionPartProducts);
//	TODO				addSubmenuItem(predictSynthesisMenu, "of "+columnName+" reactants using ICSynth Light", ICSYNTH_SEARCH+columnName+DELIMITER+CompoundTableConstants.cReactionPartReactants);
//	TODO				addSubmenuItem(predictSynthesisMenu, "of "+columnName+" products using ICSynth Light", ICSYNTH_SEARCH+columnName+DELIMITER+CompoundTableConstants.cReactionPartProducts);
						}
					if (isCtrlDown)
						addSubmenuItem(predictSynthesisMenu, "Change SPAYA Server URL", SPAYA_CHANGE_URL);
					add(predictSynthesisMenu);
					}
				}

			boolean lookupOrLaunchFoundOrOpen = false;
			try {
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
					String lookupCount = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLookupCount);
					if (lookupCount != null && Integer.parseInt(lookupCount) != 0) {
						lookupOrLaunchFoundOrOpen = true;
						break;
						}
					String launchCount = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLaunchCount);
					if (launchCount != null && Integer.parseInt(launchCount) != 0) {
						lookupOrLaunchFoundOrOpen = true;
						break;
						}
					String openExternalName = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyOpenExternalName);
					if (openExternalName != null) {
						lookupOrLaunchFoundOrOpen = true;
						break;
						}
					}
				if (lookupOrLaunchFoundOrOpen) {
					if (getComponentCount() > 0)
						addSeparator();

					for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
						String lookupCount = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLookupCount);
						if (lookupCount != null) {
							int count = Integer.parseInt(lookupCount);
							for (int i=0; i<count; i++) {
								String[] key = mTableModel.separateUniqueEntries(mTableModel.encodeData(record, column));
								if (key != null) {
									String name = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLookupName+i);
									String url = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLookupURL+i);
									boolean removeMinus = CompoundTableModel.cColumnPropertyLookupFilterRemoveMinus.equals(
											mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLookupFilter+"0"));
									boolean encode = !"false".equalsIgnoreCase(mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLookupEncode+i));
									if (name != null && url != null) {	// just to make sure
										if (key.length == 1) {
											String what = url.equals("%s") ? key[0] : name+" of "+key[0];
											JMenuItem item = new JMenuItem("Open "+what+" in Web-Browser");
											item.addActionListener(this);
											try {
												String k = removeMinus ? key[0].replace("-", "") : key[0];
												item.setActionCommand(LOOKUP+url.replace("%s", encode ? encodeParams(k) : k));
												add(item);
												}
											catch (UnsupportedEncodingException e) {}
											}
										else {
											JMenu lookupMenu = new JMenu(url.equals("%s") ? "Open in Web-Browser" : "Open "+name+" in Web-Browser");
											for (String k:key) {
												try {
													if (removeMinus)
														k = k.replace("-", "");
													addSubmenuItem(lookupMenu, k, LOOKUP+url.replace("%s", encode ? encodeParams(k) : k));
													}
												catch (UnsupportedEncodingException e) {}
												}
											if (lookupMenu.getItemCount() != 0)
												add(lookupMenu);
											}
										}
									}
								}
							}
						}

					for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
						String lauchCount = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLaunchCount);
						if (lauchCount != null) {
							int count = Integer.parseInt(lauchCount);
							for (int i=0; i<count; i++) {
								String[] key = mTableModel.separateUniqueEntries(mTableModel.encodeData(record, column));
								if (key != null)
									addLaunchItems(key, column, i, false);

								boolean allowMultiple = "true".equals(mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLaunchAllowMultiple+i));
								if (allowMultiple) {
									ArrayList<String> keyList = new ArrayList<String>();
									for (int row=0; row<mTableModel.getRowCount(); row++) {
										if (mTableModel.isSelected(row)) {
											key = mTableModel.separateUniqueEntries(mTableModel.encodeData(mTableModel.getRecord(row), column));
											if (key != null)
												for (String k : key)
													keyList.add(k);
											}
										}
									if (keyList.size() != 0)
										addLaunchItems(keyList.toArray(new String[0]), column, i,true);
									}
								}
							}
						}

					for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
						String openExternalName = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyOpenExternalName);
						if (openExternalName != null) {
							String[] key = mTableModel.separateUniqueEntries(mTableModel.encodeData(record, column));
							if (key != null)
								addOpenExternalItems(key, column, openExternalName);
							}
						}
					}
				}
			catch (NumberFormatException e) {}

			if (idcodeColumnList.size() != 0 && mDatabaseActions != null) {
				if (getComponentCount() > 0)
					addSeparator();

				mDatabaseActions.addActionItems(this, mRecord, idcodeColumnList);
				}
			}

		if (DataWarrior.USE_CARDS_VIEW)
			addCardViewOptions(source);
		}

	private void addCopyStructureItems(JMenu copyMenu, int idcodeColumn, String structureColumnSpecifier) {
		boolean hasExplicitColor = (mTableModel.getChildColumn(idcodeColumn, CompoundTableConstants.cColumnTypeAtomColorInfo) != -1);
		if (hasExplicitColor)
			addSubmenuItem(copyMenu, "2D-Structure (as drawn)", COPY_STRUCTURE_DECORATED+structureColumnSpecifier);
		addSubmenuItem(copyMenu, hasExplicitColor ? "2D-Structure (undecorated)": "2D-Structure", COPY_STRUCTURE+structureColumnSpecifier);
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (CompoundTableModel.cColumnType3DCoordinates.equals(mTableModel.getColumnSpecialType(column))
					&& mTableModel.getParentColumn(column) == idcodeColumn)
				addSubmenuItem(copyMenu, mTableModel.getColumnTitle(column), COPY_STRUCTURE+mTableModel.getColumnTitle(column));
		addSubmenuItem(copyMenu, "ID-Code", COPY_IDCODE+structureColumnSpecifier);
		addSubmenuItem(copyMenu, "SMILES-String", COPY_SMILES+structureColumnSpecifier);
		addSubmenuItem(copyMenu, "Standard Inchi", COPY_INCHI+structureColumnSpecifier);
		addSubmenuItem(copyMenu, "Inchi-Key", COPY_INCHI_KEY+structureColumnSpecifier);
		addSubmenuItem(copyMenu, "Molfile V2", COPY_MOLFILE2+structureColumnSpecifier);
		addSubmenuItem(copyMenu, "Molfile V3", COPY_MOLFILE3+structureColumnSpecifier);
		}

	private void addPasteStructureItems(JMenu pasteMenu, int idcodeColumn, String structureColumnSpecifier) {
		addSubmenuItem(pasteMenu, "Structure", PASTE_STRUCTURE+structureColumnSpecifier);
		addSubmenuItem(pasteMenu, "ID-Code", PASTE_IDCODE+structureColumnSpecifier);
		addSubmenuItem(pasteMenu, "SMILES-String", PASTE_SMILES+structureColumnSpecifier);
		addSubmenuItem(pasteMenu, "Molfile", PASTE_MOLFILE+structureColumnSpecifier);
		}

	private void addCopyReactionItems(JMenu copyMenu, int reactionColumn, String reactionColumnTitle) {
		JMenu reactionMenu = new JMenu("Reaction as");
		addSubmenuItem(reactionMenu, "Reaction", COPY_REACTION+reactionColumnTitle);
		addSubmenuItem(reactionMenu, "RXN-Code", COPY_IDCODE+reactionColumnTitle);
		addSubmenuItem(reactionMenu, "Reaction-SMILES", COPY_RXN_SMILES+reactionColumnTitle);
		copyMenu.add(reactionMenu);

		JMenu reactantsMenu = new JMenu("Reactants as");
		addCopyStructureItems(reactantsMenu, reactionColumn, mTableModel.getColumnTitle(reactionColumn)+DELIMITER+CompoundTableConstants.cReactionPartReactants);
		copyMenu.add(reactantsMenu);

		JMenu productsMenu = new JMenu("Products as");
		addCopyStructureItems(productsMenu, reactionColumn, mTableModel.getColumnTitle(reactionColumn)+DELIMITER+CompoundTableConstants.cReactionPartProducts);
		copyMenu.add(productsMenu);

		int catalystColumn = mTableModel.findColumn(mTableModel.getColumnProperty(reactionColumn, CompoundTableConstants.cColumnPropertyRelatedCatalystColumn));
		if (catalystColumn != -1) {
			JMenu catalystsMenu = new JMenu("Catalysts as");
			addCopyStructureItems(catalystsMenu, catalystColumn, mTableModel.getColumnTitle(catalystColumn));
			copyMenu.add(catalystsMenu);
			}
		}

	private void addLaunchItems(String[] key, int column, int launchItemNo, boolean isMultiple) {
		String name = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLaunchName+launchItemNo);
		String command = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLaunchCommand+launchItemNo);
		String option = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLaunchOption+launchItemNo);
		String decoration = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLaunchDecoration+launchItemNo);
		String[] decorated = new String[key.length];
		for (int i=0; i<key.length; i++)
			decorated[i] = decorateKey(key[i], decoration);
		if (name != null && command != null) {	// just to make sure
			if (command.equals("mercury"))
				truncateELNs(decorated);
			if (command.equals("pymol")) {	// We might generalize the variable resolution
				if (option != null && option.startsWith("-L "))
					option = "-L\t"+mMainPane.getApplication().resolvePathVariables(option.substring(3));
				for (int i=0; i<decorated.length; i++)
					decorated[i] = mMainPane.getApplication().resolvePathVariables(decorated[i]);
				}

			option = (option == null) ? "" : option.concat("\t");

			if (isMultiple) {
				JMenuItem item = new JMenuItem("Open all selected "+mTableModel.getColumnTitle(column)+"s in "+name);
				StringBuilder sb = new StringBuilder(decorated[0]);
				for (int i=1; i<decorated.length; i++) {
					sb.append("\t");
					sb.append(decorated[i]);
					}
				item.addActionListener(this);
				item.setActionCommand(LAUNCH+command+"\t"+option+sb.toString());
				add(item);
				}
			else if (key.length == 1) {
				JMenuItem item = new JMenuItem("Open "+mTableModel.getColumnTitle(column)+" "+key[0]+" in "+name);
				item.addActionListener(this);
				item.setActionCommand(LAUNCH+command+"\t"+option+decorated[0]);
				add(item);
				}
			else {
				JMenu launchMenu = new JMenu("Open "+mTableModel.getColumnTitle(column)+" in "+name);
				for (int j=0; j<key.length; j++)
					addSubmenuItem(launchMenu, key[j], LAUNCH+command+"\t"+option+decorated[j]);
				if (launchMenu.getItemCount() != 0)
					add(launchMenu);
				}
			}
		}

	private void addOpenExternalItems(String[] key, int column, String name) {
		String path = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyOpenExternalPath);

		if (name != null) {	// just to make sure
			if (path != null)
				path = DataWarrior.getApplication().resolvePathVariables(path);
			else
				path = "";

			if (key.length == 1) {
				JMenuItem item = new JMenuItem("Open "+mTableModel.getColumnTitle(column)+" '"+key[0]+"' in external application");
				item.addActionListener(this);
				item.setActionCommand(OPEN_EXTERNAL+path+DataWarrior.getApplication().resolvePathVariables(key[0]));
				add(item);
				}
			else {
				JMenu openExtMenu = new JMenu("Open "+mTableModel.getColumnTitle(column)+" in external application");
				for (String k:key)
					addSubmenuItem(openExtMenu, k, OPEN_EXTERNAL+path+DataWarrior.getApplication().resolvePathVariables(k));
				if (openExtMenu.getItemCount() != 0)
					add(openExtMenu);
				}
			}
		}

	private void addCardViewOptions(CompoundTableView source) {
		if (source instanceof JCardView) {
			// create automatic 1D sorting options:
			List<Integer> columnsNumeric = new ArrayList<>();
			for (int zi = 0; zi < mTableModel.getTotalColumnCount(); zi++) {
				if (mTableModel.isColumnTypeDouble(zi)) {
					columnsNumeric.add(zi);
				}
			}
			List<Integer> columnsCategorical = new ArrayList<>();
			for (int zi = 0; zi < mTableModel.getTotalColumnCount(); zi++) {
				if (mTableModel.isColumnTypeCategory(zi)) {
					columnsCategorical.add(zi);
				}
			}

			// Find Hitlists:
			List<String> listNames = new ArrayList<>();
			try {
				listNames = Arrays.asList(mTableModel.getListHandler().getListNames());
			}
			catch(Exception e){}


			JCardView cardView = (JCardView) source;
			JCardPane cardPane = cardView.getCardPane();

			mCEMouseOver = cardPane.getHighlightedCardElement();

			this.addSeparator();

//				JMenuItem positionCardItems = new JMenuItem(TEXT_CARD_POSITION);
//				positionCardItems.addActionListener(this);
//				this.add(positionCardItems);
//
//				this.addSeparator();

			JMenuItem stackItem = new JMenuItem(TEXT_CARD_STACK);
			stackItem.addActionListener(this);
			this.add(stackItem);

//				JMenuItem itemExpandStack = new JMenuItem("Expand Stack");
//				itemExpandStack.setActionCommand(TEXT_CARD_EXPAND_STACK);
//				itemExpandStack.addActionListener(this);
//				CardElement ce_mouseOver = null;
//				if (!cardPane.isMouseOverStack()) {
//					itemExpandStack.setEnabled(false);
//				} else {
//
//				}
//				this.add(itemExpandStack);
//                this.addSeparator();

//                JMenuItem miCreateStackFromSelection = new JMenuItem("Create Stack from Selection");

			//JMenu menuCreateStacksFromAll       = new JMenu("Stack All Cards by");
			//JMenu menuCreateStacksFromSelection = new JMenu("Stack Selected Cards by");

//				JMenu menuFromSelection = new JMenu("From Selection");
//				JMenu menuFromAll       = new JMenu("From All");

			// Expand Stacks
			// A: from selection, B: from all
			JMenuItem A_ExpandSelectedStacks = new JMenuItem("Expand Selected Stacks");
			A_ExpandSelectedStacks.setActionCommand(TEXT_CARD_FROM_SELECTION_EXPAND_STACKS);
			A_ExpandSelectedStacks.addActionListener(this);

			this.add(A_ExpandSelectedStacks);

			JMenuItem B_ExpandSelectedStacks = new JMenuItem("Expand All Stacks");
			B_ExpandSelectedStacks.setActionCommand(TEXT_CARD_FROM_ALL_EXPAND_STACKS);
			B_ExpandSelectedStacks.addActionListener(this);


			JMenuItem SetStackName = new JMenuItem("Set Stack Name...");
			SetStackName.setActionCommand(TEXT_CARD_SET_STACK_NAME);
			SetStackName.addActionListener(this);

			if(!cardPane.isMouseOverStack()) {
				SetStackName.setEnabled(false);
			}
//                if(mCEMouseOver==null){
//                    SetStackName.setEnabled(false);
//                }
//                else if(! mCEMouseOver.isStack()) {
//                    SetStackName.setEnabled(false);
//                }

			this.add(SetStackName);

//				menuFromSelection.add(A_ExpandStacks);
//				menuFromAll.add(B_ExpandStacks);
//
//				menuFromSelection.addSeparator();
//				menuFromAll.addSeparator();


			this.addSeparator();

			// Create Substacks

			// From Categories
			// A: from selection, B: from all
			JMenu A_createSubstacksFromCategories = new JMenu("Stacks All Cards by");
			JMenu B_createSubstacksFromCategories = new JMenu("Stack Selected Cards by");

			for (Integer ci : columnsCategorical) {
				JMenuItem cigi = new JMenuItem(mTableModel.getColumnTitle(ci));
				cigi.setActionCommand(TEXT_CARD_FROM_ALL_CREATE_SUBSTACKS_FROM_CAT + ":" + ci.toString());
				cigi.addActionListener(this);
				A_createSubstacksFromCategories.add(cigi);
			}
			for (Integer ci : columnsCategorical) {
				JMenuItem cigi = new JMenuItem(mTableModel.getColumnTitle(ci));
				cigi.setActionCommand(TEXT_CARD_FROM_SELECTION_CREATE_SUBSTACKS_FROM_CAT + ":" + ci.toString());
				cigi.addActionListener(this);
				B_createSubstacksFromCategories.add(cigi);
			}

			JMenu A_jm_arrangeSelectedCards = new JMenu("Arrange All Cards by");
			JMenu B_jm_arrangeSelectedCards = new JMenu("Arrange Selected Cards by");

			for (Integer ci : columnsNumeric) {
				JMenuItem cigi = new JMenuItem(mTableModel.getColumnTitle(ci));
				cigi.setActionCommand(TEXT_CARD_FROM_ALL_ARRANGE+":"+ci.toString());
				cigi.addActionListener(this);
				A_jm_arrangeSelectedCards.add(cigi);
			}

			for (Integer ci : columnsNumeric) {
				JMenuItem cigi = new JMenuItem(mTableModel.getColumnTitle(ci));
				cigi.setActionCommand(TEXT_CARD_FROM_SELECTION_ARRANGE+":"+ci.toString());
				cigi.addActionListener(this);
				B_jm_arrangeSelectedCards.add(cigi);
			}


			boolean selection_empty = ((JCardView)source).getCardPane().getSelection().isEmpty();

			if(!selection_empty) {
				this.add(B_jm_arrangeSelectedCards);
			}
			else {
				this.add(A_jm_arrangeSelectedCards);
			}

			this.addSeparator();

			// From Hitlist:
			// A: from selection, B: from all
//				JMenu A_createSubstacksFromHL = new JMenu("Create Stack from Hitlist");
//				JMenu B_createSubstacksFromHL = new JMenu("Create Stack from ");
//
//				for (String si : listNames) {
//					JMenuItem cigi = new JMenuItem(si);
//					cigi.setActionCommand(TEXT_CARD_FROM_SELECTION_CREATE_SUBSTACKS_FROM_HITLIST + ":" + mTableModel.getListHandler().getListFlagNo(si)  );
//					cigi.addActionListener(this);
//					A_createSubstacksFromHL.add(cigi);
//				}
//				for (String si : listNames) {
//					JMenuItem cigi = new JMenuItem(si);
//					cigi.setActionCommand(TEXT_CARD_FROM_ALL_CREATE_SUBSTACKS_FROM_HITLIST + ":" + mTableModel.getListHandler().getListFlagNo(si));
//					cigi.addActionListener(this);
//					B_createSubstacksFromHL.add(cigi);
//				}
//
//				if(columnsCategorical.isEmpty()){ A_createSubstacksFromCategories.setEnabled(false); B_createSubstacksFromCategories.setEnabled(false);}
//				if(listNames.isEmpty()){ A_createSubstacksFromHL.setEnabled(false); B_createSubstacksFromHL.setEnabled(false);}


			// Add substack creation:
//				menuFromSelection.add(A_createSubstacksFromCategories);
//				menuFromSelection.add(A_createSubstacksFromHL);
//				menuFromAll.add(B_createSubstacksFromCategories);
//				menuFromAll.add(B_createSubstacksFromHL);

			if(!selection_empty)
				add(B_createSubstacksFromCategories);
			else
				add(A_createSubstacksFromCategories);

//				menuFromSelection.addSeparator();
//				menuFromAll.addSeparator();

//				menuFromSelection.add(A_menuSort2D);
//				menuFromAll.add(B_menuSort2D);

//                JMenu menuCreateStacks = new JMenu("Create Stacks");
//                for (Integer ci : columnsCategorical) {
//                    JMenuItem cs_xi = new JMenuItem(mTableModel.getColumnTitle(ci));
//                    cs_xi.setActionCommand(TEXT_CARD_STACK_FOR_COLUMN + ":" + ci);
//                    cs_xi.addActionListener(this);
//                    menuCreateStacks.add(cs_xi);
//                }
//                this.add(menuCreateStacks);

			//this.addSeparator();
			//this.add(menuFromSelection);
			//this.addSeparator();

			// @TODO: create action and implement!
			JMenuItem createCategoryFromStacks = new JMenuItem("Create Category Column from Stacks..");
			createCategoryFromStacks.setActionCommand(TEXT_CARD_CREATE_CATEGORY_COLUMN_FROM_STACKS);
			createCategoryFromStacks.addActionListener(this);
			this.add(createCategoryFromStacks);

			// deactivate JMenuItems in case that we have nothing selected
			if( ((JCardView)source).getCardPane().getCardPaneModel().getAllElements().stream().filter( ci -> ci.getSelectedRecords().size()>0 ).count() == 0 ) {
				B_jm_arrangeSelectedCards.setEnabled(false);
				B_createSubstacksFromCategories.setEnabled(false);
				stackItem.setEnabled(false);
				A_ExpandSelectedStacks.setEnabled(false);
				}
			}
		}

	private String decorateKey(String key, String decoration) {
		if (decoration != null)
			key = decoration.replace("%s", key);

		return key.replace(' ', '_');
		}

	// Mecury requires ELN numbers without dot-extension
	private void truncateELNs(String[] eln) {
		for (int i=0; i<eln.length; i++) {
			int index = eln[i].indexOf('.');
			if (index != -1)
				eln[i] = eln[i].substring(0, index);
			}
		}

	public String encodeParams(String params) throws UnsupportedEncodingException {
		// The URLEncoder replaces ' ' by a '+', which seems to be the old way of doing it,
		// which is not compatible with wikipedia and molecule names that contain spaces.
		// (Wikipedia detects "%20" and converts them into '_', which they use in their page names instead of spaces)
		return URLEncoder.encode(params, "UTF-8").replace("+", "%20");
		}

	private Reaction getReaction(String actionCommand) {
		int chemistryColumn = mTableModel.findColumn(getCommandColumn(actionCommand));
		if (mTableModel.isColumnTypeReaction(chemistryColumn)) {
			return mTableModel.getChemicalReaction(mRecord, chemistryColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE);
			}
		return null;
		}

	private StereoMolecule getMolecule(String actionCommand) {
		int chemistryColumn = mTableModel.findColumn(getCommandColumn(actionCommand));
		if (mTableModel.isColumnTypeStructure(chemistryColumn)
		 || mTableModel.getColumnSpecialType(chemistryColumn).equals(CompoundTableConstants.cColumnType3DCoordinates)) {
			int colorMode = actionCommand.startsWith(COPY_STRUCTURE_DECORATED) ?
					CompoundTableModel.ATOM_COLOR_MODE_EXPLICIT : CompoundTableModel.ATOM_COLOR_MODE_NONE;
			return mTableModel.getChemicalStructure(mRecord, chemistryColumn, colorMode, null);
			}
		if (mTableModel.isColumnTypeReaction(chemistryColumn)) {
			return mTableModel.getChemicalStructureFromReaction(mRecord, chemistryColumn, getCommandReactionPart(actionCommand), false);
			}
		return null;
		}

	public void actionPerformed(ActionEvent e) {

		String actionCommand = e.getActionCommand();
		if (actionCommand.startsWith("TEST")) {
			test(mTableModel.findColumn(getCommandColumn(actionCommand)));
			}
		if (actionCommand.startsWith(COPY_IDCODE)) {
			int idcodeColumn = mTableModel.findColumn(getCommandColumn(actionCommand));
			byte[] idcode = (byte[]) mRecord.getData(idcodeColumn);
			if (idcode != null) {
				StringSelection theData = new StringSelection(new String(idcode));
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
			}
		} else if (actionCommand.startsWith(COPY_RXN_SMILES)) {
			Reaction rxn = getReaction(actionCommand);
			if (rxn != null) {
				String rxnsmi = IsomericSmilesCreator.createReactionSmiles(rxn);
				StringSelection theData = new StringSelection(rxnsmi);
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
			}
		} else if (actionCommand.startsWith(COPY_SMILES)
				|| actionCommand.startsWith(COPY_INCHI)
				|| actionCommand.startsWith(COPY_INCHI_KEY)
				|| actionCommand.startsWith(COPY_MOLFILE2)
				|| actionCommand.startsWith(COPY_MOLFILE3)) {
			StereoMolecule mol = getMolecule(actionCommand);
			if (mol != null) {
				String encodedMol = null;

				if (actionCommand.startsWith(COPY_SMILES))
					encodedMol = new IsomericSmilesCreator(mol).getSmiles();
				else if (actionCommand.startsWith(COPY_INCHI))
					encodedMol = InchiCreator.createStandardInchi(mol);
				else if (actionCommand.startsWith(COPY_INCHI_KEY))
					encodedMol = InchiCreator.createInchiKey(mol);
				else if (actionCommand.startsWith(COPY_MOLFILE2))
					encodedMol = new MolfileCreator(mol).getMolfile();
				else
					encodedMol = new MolfileV3Creator(mol).getMolfile();

				if (encodedMol != null) {
					StringSelection theData = new StringSelection(encodedMol);
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
				}
			}
		} else if (actionCommand.startsWith(COPY_STRUCTURE)
				|| actionCommand.startsWith(COPY_STRUCTURE_DECORATED)) {
			StereoMolecule mol = getMolecule(actionCommand);
			if (mol != null)
				new ClipboardHandler().copyMolecule(mol);
		} else if (actionCommand.startsWith(COPY_REACTION)) {
			int reactionColumn = mTableModel.findColumn(getCommandColumn(actionCommand));
			Reaction reaction = mTableModel.getChemicalReaction(mRecord, reactionColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE);
			if (reaction != null)
				new ClipboardHandler().copyReaction(reaction);
		} else if (actionCommand.startsWith(PASTE_STRUCTURE)) {
			StereoMolecule mol = new ClipboardHandler().pasteMolecule(false);
			if (mol != null) {
				int structureColumn = mTableModel.findColumn(getCommandColumn(actionCommand));

				boolean is3D = mol.is3D();
				int coordsColumn = mTableModel.getChildColumn(structureColumn, is3D ? CompoundTableConstants.cColumnType3DCoordinates : CompoundTableConstants.cColumnType2DCoordinates);

				if (is3D && coordsColumn == -1) {
					new CoordinateInventor().invent(mol);
					coordsColumn = mTableModel.getChildColumn(structureColumn, CompoundTableConstants.cColumnType2DCoordinates);
				}

				Canonizer canonizer = new Canonizer(mol);
				String idcode = canonizer.getIDCode();
				mRecord.setData(idcode.getBytes(), structureColumn);
				if (coordsColumn != -1)
					mRecord.setData(canonizer.getEncodedCoordinates().getBytes(), coordsColumn);

				for (int childColumn=0; childColumn<mTableModel.getTotalColumnCount(); childColumn++)
					if (childColumn != coordsColumn && mTableModel.getParentColumn(childColumn) == structureColumn)

				mTableModel.finalizeChangeCell(mRecord, structureColumn);
			}
		} else if (actionCommand.startsWith(COPY_VALUE)) {
			int valueColumn = mTableModel.findColumn(getCommandColumn(actionCommand));
			byte[] value = (byte[]) mRecord.getData(valueColumn);
			if (value != null) {
				StringSelection theData = new StringSelection(new String(value));
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
			}
		} else if (actionCommand.startsWith(COPY_WITH_HEADER)) {
			new DETaskCopyTableCells(getParentFrame(), mTableModel, mMainPane.getTable(), true).defineAndRun();
		} else if (actionCommand.startsWith(COPY_WITHOUT_HEADER)) {
			new DETaskCopyTableCells(getParentFrame(), mTableModel, mMainPane.getTable(), false).defineAndRun();
		} else if (actionCommand.startsWith(PASTE_INTO)) {
			int column = mTableModel.findColumn(getCommandColumn(actionCommand));
			for (int row = 0; row < mTableModel.getRowCount(); row++) {
				if (mTableModel.getRecord(row) == mRecord) {
					new DETaskPasteIntoTable(getParentFrame(), column, row).defineAndRun();
					break;
				}
			}
		} else if (actionCommand.startsWith(EDIT_VALUE)) {
			mMainPane.getTable().editCell(mRecord, mTableModel.findColumn(getCommandColumn(actionCommand)));
		} else if (actionCommand.startsWith(SORT)) {
			int descriptorColumn = mTableModel.findColumn(getCommandColumn(actionCommand));
			String reactionPart = getCommandReactionPart(actionCommand);
			if (reactionPart != null)
				new DETaskSortReactionsBySimilarity(getParentFrame(), descriptorColumn, reactionPart, mRecord).defineAndRun();
			else
				new DETaskSortStructuresBySimilarity(getParentFrame(), descriptorColumn, mRecord).defineAndRun();
		} else if (actionCommand.startsWith(ADD_TO_LIST)) {
			String hitlistName = getCommandColumn(actionCommand);
			CompoundTableListHandler hh = mTableModel.getListHandler();
			hh.addRecord(mRecord, hh.getListIndex(hitlistName));
		} else if (actionCommand.startsWith(REMOVE_FROM_LIST)) {
			String hitlistName = getCommandColumn(actionCommand);
			CompoundTableListHandler hh = mTableModel.getListHandler();
			hh.removeRecord(mRecord, hh.getListIndex(hitlistName));
		} else if (actionCommand.startsWith(NEW_FILTER)) {
			String columnName = getCommandColumn(actionCommand);
			String reactionPart = getCommandReactionPart(actionCommand);
			int chemColumn = mTableModel.findColumn(columnName);
			if (mTableModel.hasDescriptorColumn(chemColumn, reactionPart)) {
				if (actionCommand.startsWith(NEW_FILTER_THIS)) {
					if (mTableModel.isColumnTypeReaction(chemColumn) && reactionPart == null) {
						Reaction rxn = mTableModel.getChemicalReaction(mRecord, chemColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE);
						if (rxn != null) {
							try {
								mPruningPanel.addReactionFilter(mTableModel, chemColumn, rxn);
							} catch (DEPruningPanel.FilterException fpe) {
								JOptionPane.showMessageDialog(getParentFrame(), fpe.getMessage());
							}
						}
					} else {
						StereoMolecule mol = mTableModel.getChemicalStructure(mRecord, chemColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
						if (mol != null) {
							try {
								mPruningPanel.addStructureFilter(mTableModel, chemColumn, reactionPart, mol);
							} catch (DEPruningPanel.FilterException fpe) {
								JOptionPane.showMessageDialog(getParentFrame(), fpe.getMessage());
							}
						}
					}
				} else {
					boolean selected = actionCommand.startsWith(NEW_FILTER_SELECTED);
					try {
						TreeSet<String> idcodeSet = new TreeSet<>();
						for (int row = 0; row < mTableModel.getTotalRowCount(); row++) {
							CompoundRecord record = mTableModel.getTotalRecord(row);
							if (mTableModel.isVisible(record)
									&& (!selected || mTableModel.isVisibleAndSelected(record)))
								idcodeSet.add(mTableModel.getTotalValueAt(row, chemColumn));
						}
						int count = idcodeSet.size();
						if (count < 100
								|| JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(getParentFrame(),
								"Creating a filter with " + count + " structures may take some time.\nDo you want to continue?",
								"New Structure List Filter", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)) {
							JMultiStructureFilterPanel filter = mPruningPanel.addStructureListFilter(mTableModel, chemColumn, reactionPart, false);
							filter.getCompoundCollectionPane().getModel().setCompoundList(idcodeSet);
						}
					} catch (DEPruningPanel.FilterException fpe) {
						JOptionPane.showMessageDialog(getParentFrame(), fpe.getMessage());
					}
				}
			} else {
				JOptionPane.showMessageDialog(getParentFrame(), "Please calculate a descriptor for the column '" + columnName + "' before creating a structure filter.");
			}
		} else if (actionCommand.startsWith(CONFORMERS)) {    // taken out because of unclear copyright situation with CCDC concerning torsion statistics
			int index = actionCommand.lastIndexOf('\'');
			final int idcodeColumn = mTableModel.findColumn(actionCommand.substring(CONFORMERS.length(), index));
			StereoMolecule mol = mTableModel.getChemicalStructure(mRecord, idcodeColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
			if (mol != null)
				new FXConformerDialog(getParentFrame(), mol).generateConformers();
		} else if (actionCommand.startsWith(PATENT_SEARCH)
				|| actionCommand.startsWith(SCHOLAR_SEARCH)
				|| actionCommand.startsWith(SPAYA_SEARCH)
				|| actionCommand.startsWith(ICSYNTH_SEARCH)) {
			String columnName = getCommandColumn(actionCommand);
			String reactionPart = getCommandReactionPart(actionCommand);
			int chemColumn = mTableModel.findColumn(columnName);
			StereoMolecule mol = (reactionPart != null) ? mTableModel.getChemicalStructureFromReaction(mRecord, chemColumn, reactionPart, false)
														: mTableModel.getChemicalStructure(mRecord, chemColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
			if (mol != null && mol.getAllAtoms() != 0) {
				String smiles = new SmilesCreator().generateSmiles(mol);
				if (smiles != null && smiles.length() != 0) {
					if (actionCommand.startsWith(SPAYA_SEARCH)) {
						Preferences prefs = DataWarrior.getPreferences();
						String server = prefs.get(DataWarrior.PREFERENCES_KEY_SPAYA_SERVER, "");
						String url = server.length() == 0 ? SPAYA_URL : SPAYA_URL.replace(SPAYA_DEFAULT, server);
						try {
							BrowserControl.displayURL(url.replace("%s", encodeParams(smiles)));
							}
						catch (UnsupportedEncodingException uee) {}
						}
					else if (actionCommand.startsWith(ICSYNTH_SEARCH)) {
						new ICSynthCommunicator(getParentFrame()).suggestSynthesis(smiles);
						}
					else {
						try {
							String url = actionCommand.startsWith(PATENT_SEARCH) ? GOOGLE_PATENT_URL : GOOGLE_SCHOLAR_URL;
							BrowserControl.displayURL(url.replace("%s", encodeParams("SMILES=".concat(smiles))));
							}
						catch (UnsupportedEncodingException uee) {}
						}
					}
				}
		} else if (actionCommand.equals(SPAYA_CHANGE_URL)) {
			Preferences prefs = DataWarrior.getPreferences();
			String current = prefs.get(DataWarrior.PREFERENCES_KEY_SPAYA_SERVER, "");
			String server = JOptionPane.showInputDialog(getParentFrame(), "Change Spaya Server (keep empty for '"+SPAYA_DEFAULT+"')", current);
			if (server.indexOf('/') != -1) {
				JOptionPane.showMessageDialog(getParentFrame(), "Please use the domain name only, e.g. 'myspaya.mycompany.com'");
				}
			else if (!server.equals(current)) {
				if (server.length() == 0)
					prefs.remove(DataWarrior.PREFERENCES_KEY_SPAYA_SERVER);
				else
					prefs.put(DataWarrior.PREFERENCES_KEY_SPAYA_SERVER, server);
				}
		} else if (actionCommand.startsWith(LOOKUP)) {
			BrowserControl.displayURL(getCommandColumn(actionCommand));
		} else if (actionCommand.startsWith(LAUNCH)) {
			try {
				Platform.execute(getCommandColumn(actionCommand).split("\\t"));
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} else if (actionCommand.startsWith(OPEN_EXTERNAL)) {
			try {
				Platform.openDocument(getCommandColumn(actionCommand));
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}



		} else if (actionCommand.equals(TEXT_CARD_STACK)) {
			//new DETaskCreateStackFromSelection(getParentFrame(), ((JCardView) mSource).getCardPane(), ((JCardView) mSource).getCardPane().getLastClickedPoint()).defineAndRun();
			new DETaskCreateStackFromSelection(getParentFrame(), ((JCardView) mSource).getCardPane(), ((JCardView) mSource).getCardPane().getLastClickedPoint()).runTask(new Properties());
        } else if (actionCommand.equals(TEXT_CARD_CREATE_CATEGORY_COLUMN_FROM_STACKS)) {
			new DETaskCreateCategoricalColumnFromStacks(getParentFrame(),((JCardView) this.mSource).getCardPane(),((JCardView) this.mSource).getDataWarriorLink()).defineAndRun();
		}

//			//@TODO create task..
//			JCardViewOptionsPanel jcvop = new JCardViewOptionsPanel(this.mTableModel, ((JCardView) this.mSource).getCardPane(), ((JCardView) this.mSource).getFastCardPane());
//			JDialog jdg = new JDialog();
//			jdg.getContentPane().add(jcvop);
//			jdg.setVisible(true);
//			jdg.setModal(true);
//			jdg.pack();

//		else if (actionCommand.equals(TEXT_CARD_CONFIGURE_OLD)) {
//			//@TODO create task..
//			JCardConfigurationPanel jccp = new JCardConfigurationPanel(((JCardView) this.mSource).getDataWarriorLink(), ((JCardView) this.mSource).getCardPane(), ((JCardView) this.mSource).getCardPane().getCardDrawer().getCardDrawingConfig());
//
//
//			//jccp.setConfig( ((JCardView) this.mSource).getCardPane().getCardDrawer().getCardDrawingConfig() );
//			JDialog jdg = new JDialog();
//			jdg.getContentPane().add(jccp);
//			jdg.setVisible(true);
//			jdg.setModal(true);
//			jdg.setSize(1000, 600);
//			//jdg.setLocation( new Point((int) this.getMousePosition().getX()-400,(int) this.getMousePosition().getY()-300) );
//		}

		else if (actionCommand.equals(TEXT_CARD_POSITION)) {
			new DETaskPositionCards(getParentFrame(),((JCardView) this.mSource).getDataWarriorLink(), ((JCardView) this.mSource).getCardPane() ).defineAndRun();
		}
//				JCardWizard2 jCardWizard = new JCardWizard2(((JCardView) this.mSource).getDataWarriorLink(), ((JCardView) this.mSource), ((JCardView) this.mSource).getCardPane(), false, ((JCardView) this.mSource).getCardWizardConfig());
//				JDialog jdg = new JDialog();
//				jdg.getContentPane().add(jCardWizard);
//				jCardWizard.setFullConfig(((JCardView) this.mSource).getCardWizardConfig());
//				jdg.setVisible(true);
//				jdg.setModal(true);
//				jdg.pack();
//		}
//		  else if (actionCommand.startsWith(TEXT_CARD_POS_1D_CIRCLE)) {
//			// parse coordinate:
//			String splits[] = actionCommand.split(":");
//			int col = Integer.parseInt(splits[1]);
//
//		} else if (actionCommand.startsWith(TEXT_CARD_POS_1D_GRID)) {
//			// parse coordinate:
//			String splits[] = actionCommand.split(":");
//			int col = Integer.parseInt(splits[1]);
//		} else if (actionCommand.startsWith(TEXT_CARD_POS_2D_GRID)) {
//			// parse coordinate2
//			String splits[] = actionCommand.split(":");
//			int col_x = Integer.parseInt(splits[1]);
//			int col_y = Integer.parseInt(splits[2]);
//		}
		  else if (actionCommand.startsWith(TEXT_CARD_STACK_FOR_COLUMN)) {
			String[] splits = actionCommand.split(":");
			int col = Integer.parseInt(splits[1]);

			if (!mTableModel.isColumnTypeCategory(col)) {
				System.out.println("NOT YET SUPPORTED.. :(");
			} else {
				DETaskCreateStackForCategoricalColumn task = new DETaskCreateStackForCategoricalColumn(getParentFrame(), ((JCardView) mSource).getCardPane(), ((JCardView) mSource).getCardPane().getLastClickedPoint(), col);
				task.runTask(new Properties());
			}
		} else if (actionCommand.equals(TEXT_CARD_EXPAND_STACK)) {
			DETaskExpandStack task = new DETaskExpandStack(getParentFrame(), ((JCardView) mSource).getCardPane(), mCEMouseOver);
			task.runTask(new Properties());
		}
		else if (actionCommand.equals(TEXT_CARD_SET_STACK_NAME)) {
            DETaskSetStackName task = new DETaskSetStackName(getParentFrame(),((JCardView) mSource).getCardPane(), ((JCardView) mSource).getDataWarriorLink(), mCEMouseOver.getAllRecords().get(0).getID() );
            task.defineAndRun();
        }

		// 1. check if starts with "Card-From-All-" or "Card-From-Selection-"
		//

		else if(actionCommand.startsWith("Card-From-")){

			Point2D cardPaneMousePos                      = ((JCardView) mSource).getCardPane().getLastClickedPoint();
			String cardPaneCommand                        = "";
			DECardsViewHelper.Applicability applicability = null;

			if(actionCommand.startsWith("Card-From-All-")){
				cardPaneCommand = actionCommand;
				cardPaneCommand = cardPaneCommand.substring(14);
				applicability = DECardsViewHelper.Applicability.ALL;
			}
			else if(actionCommand.startsWith("Card-From-Selection-")){
				cardPaneCommand = actionCommand;
				cardPaneCommand = cardPaneCommand.substring(20);
				applicability = DECardsViewHelper.Applicability.SELECTED;
			}

			if(cardPaneCommand.equals("Expand-Stacks")){
				DETaskExpandStacks task = new DETaskExpandStacks(getParentFrame(), ((JCardView) mSource), cardPaneMousePos,applicability);
				task.runTask(new Properties());
			}
			if(cardPaneCommand.startsWith("Create-Substacks")){
				if(cardPaneCommand.startsWith("Create-Substacks-Cat")) {
					// parse the integer:
					String[] parsedCmd = cardPaneCommand.split(":");
					int pi = Integer.parseInt(parsedCmd[1]);

					// probably here we should also check if it is category or numeric, and then init the task accordingly
					DETaskCreateSubstacks task = new DETaskCreateSubstacks(getParentFrame(), ((JCardView) mSource), cardPaneMousePos, applicability, DETaskCreateSubstacks.SubstackCriterion.CATEGORY,pi );
					task.runTask(new Properties());

				}
				if(cardPaneCommand.startsWith("Create-Substacks-HL")) {
					// parse the integer: (indicates the flagIdx)
					String[] parsedCmd = cardPaneCommand.split(":");
					int flagIdx = Integer.parseInt(parsedCmd[1]);

					DETaskCreateSubstacks task = new DETaskCreateSubstacks(getParentFrame(), ((JCardView) mSource), cardPaneMousePos, applicability, DETaskCreateSubstacks.SubstackCriterion.HITLIST, flagIdx );
					task.runTask(new Properties());
					//task.defineAndRun();
				}
			}
			if( cardPaneCommand.startsWith("Arrange") ) {
				String[] splitcol = cardPaneCommand.split(":");
				int column = Integer.parseInt(splitcol[1]);

				// configure the DETaskPositionCards correctly:
				Properties conf_pc = new Properties();

				DETaskArrangeCards task = new DETaskArrangeCards(getParentFrame(),((JCardView) this.mSource).getDataWarriorLink(), ((JCardView) this.mSource).getCardPane(),applicability, column );
				Properties prop = new Properties();
				prop.put(DETaskArrangeCards.KEY_COL,((JCardView) this.mSource).getDataWarriorLink().getCTM().getColumnTitle(column));
				//task.setDialogConfiguration(prop);
				task.defineAndRun();
			}

			if( cardPaneCommand.startsWith("Sort-1D") || cardPaneCommand.startsWith("Sort-2D") ){

				DECardsViewHelper cwhelper = new DECardsViewHelper(((JCardView) mSource));
				List<CardElement> ceListSelected = cwhelper.getSelectedCardElements();
				List<CardElement> ceListAll = cwhelper.getNonexclucdedCardElements();

				CardPositionerInterface cpX = null;



				if(cardPaneCommand.startsWith("Sort-1D")) {
					String[] splitcol = cardPaneCommand.split(":");
					int colnum = Integer.parseInt(splitcol[1]);

					if (cardPaneCommand.startsWith("Sort-1D-Grid")) {
						System.out.println("sort 1d grid : " + colnum);
						cpX = CardNumericalShaper1D.createDefaultXSorter_Grid(mTableModel, colnum);
					}
					if (cardPaneCommand.startsWith("Sort-1D-Spiral")) {
						System.out.println("sort 1d spiral : " + colnum);
						cpX = new SpiralOutPositioner();// mTableModel, colnum);
						cpX.setTableModel(mTableModel);
						((SpiralOutPositioner) cpX).setColumn(colnum);
					}
				}

				if(cardPaneCommand.startsWith("Sort-2D")){
					String[] splitcol = cardPaneCommand.split(":");
					int col_a = Integer.parseInt(splitcol[1]);
					int col_b = Integer.parseInt(splitcol[2]);
					System.out.println("sort 2d grid : "+col_a+" "+col_b);

					if(applicability == DECardsViewHelper.Applicability.ALL) {
						cpX = XYSorter.createDefaultXYSorter(mTableModel,ceListAll);
						((XYSorter) cpX).getSorterX().setColumn(col_a,ceListAll);
						((XYSorter) cpX).getSorterY().setColumn(col_b,ceListAll);
					}
					if(applicability == DECardsViewHelper.Applicability.SELECTED) {
						cpX = XYSorter.createDefaultXYSorter(mTableModel,ceListAll);
						((XYSorter) cpX).getSorterX().setColumn(col_a,ceListSelected);
						((XYSorter) cpX).getSorterY().setColumn(col_b,ceListSelected);
					}
				}

				if(cpX==null){
					// something went wrong..
					System.out.println("ERROR in DEDetailPopupMenu ActionHandler code..");
					return;
				}

				if(applicability== DECardsViewHelper.Applicability.ALL) {
					try {
						List<Point2D> pos = cpX.positionAllCards(mTableModel, ceListAll);
						for (int zi = 0; zi < pos.size(); zi++) {
							ceListAll.get(zi).setCenter(pos.get(zi).getX(), pos.get(zi).getY());
						}
						cwhelper.getCardPane().startAnimateViewportToShowAllCards();
					}
					catch(InterruptedException ex){
						System.out.println("InterrupedException in positionAllCards..");
						// that's ok, we just don't position in this case..
					}
				}
				if(applicability== DECardsViewHelper.Applicability.SELECTED) {
					if(ceListSelected.size()==0){return;}
					try {
						List<Point2D> pos = cpX.positionAllCards(mTableModel, ceListSelected);
						for (int zi = 0; zi < pos.size(); zi++) {
							ceListSelected.get(zi).setCenter(pos.get(zi).getX(), pos.get(zi).getY());
						}
						cwhelper.getCardPane().startAnimateViewportToShowAllCards();
					}
					catch(InterruptedException ex){
						System.out.println("InterrupedException in positionAllCards..");
						// that's ok, we just don't position in this case..
					}
				}
			}
		}
	}

	private String getCommandColumn(String actionCommand) {
		int index1 = actionCommand.indexOf(DELIMITER) + DELIMITER.length();
		int index2 = actionCommand.indexOf(DELIMITER, index1);
		return index2 == -1 ? actionCommand.substring(index1) : actionCommand.substring(index1, index2);
		}

	private String getCommandReactionPart(String actionCommand) {
		int index1 = actionCommand.indexOf(DELIMITER) + DELIMITER.length();
		int index2 = actionCommand.indexOf(DELIMITER, index1);
		return index2 == -1 ? null : actionCommand.substring(index2 + DELIMITER.length());
		}

	private JMenuItem addSubmenuItem(JMenu menu, String text, String actionCommand) {
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(this);
		if (actionCommand != null)
			item.setActionCommand(actionCommand);
		menu.add(item);
		return item;
		}

	private JMenuItem addSubmenuItem(JMenu menu, String text, String actionCommand, String toolTipText) {
		JMenuItem item = addSubmenuItem(menu, text, actionCommand);
		item.setToolTipText(toolTipText);
		return item;
		}

	private void addMenuItem(String text) {
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(this);
		add(item);
		}

	private void addMenuItem(String text, String actionCommand) {
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(this);
		if (actionCommand != null)
			item.setActionCommand(actionCommand);
		add(item);
		}

	private DEFrame getParentFrame() {
		Component c = (Component)mSource;
		while (c != null && !(c instanceof DEFrame))
			c = c.getParent();
		return (DEFrame)c;
		}

	private void test(int column) {
		System.out.println("********************* test conformers! *****************************");
		StereoMolecule mol = mTableModel.getChemicalStructure(mRecord, column, 0, null);
		if (mol != null) {
			ConformerGenerator.addHydrogenAtoms(mol);
			ConformationSelfOrganizer cso = new ConformationSelfOrganizer(mol, true);
			cso.initializeConformers(123L, -1);
			cso.generateOneConformerInPlace(123L);
			}
		}
	}
