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

import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHelper;
import com.actelion.research.chem.descriptor.DescriptorInfo;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.action.DECorrelationDialog;
import com.actelion.research.datawarrior.action.DEFileLoader;
import com.actelion.research.datawarrior.action.DEInteractiveSARDialog;
import com.actelion.research.datawarrior.action.DEMarkushDialog;
import com.actelion.research.datawarrior.help.DEHelpFrame;
import com.actelion.research.datawarrior.help.DENews;
import com.actelion.research.datawarrior.help.DEUpdateHandler;
import com.actelion.research.datawarrior.help.FXHelpFrame;
import com.actelion.research.datawarrior.task.*;
import com.actelion.research.datawarrior.task.chem.*;
import com.actelion.research.datawarrior.task.chem.clib.DETaskEnumerateCombinatorialLibrary;
import com.actelion.research.datawarrior.task.chem.elib.DETaskBuildEvolutionaryLibrary;
import com.actelion.research.datawarrior.task.chem.ml.DETaskAssessPredictionQuality;
import com.actelion.research.datawarrior.task.chem.ml.DETaskPredictMissingValues;
import com.actelion.research.datawarrior.task.chem.rxn.*;
import com.actelion.research.datawarrior.task.data.*;
import com.actelion.research.datawarrior.task.data.fuzzy.DETaskCalculateFuzzyScore;
import com.actelion.research.datawarrior.task.db.*;
import com.actelion.research.datawarrior.task.file.*;
import com.actelion.research.datawarrior.task.filter.*;
import com.actelion.research.datawarrior.task.list.*;
import com.actelion.research.datawarrior.task.macro.DETaskCopyMacro;
import com.actelion.research.datawarrior.task.macro.DETaskExitProgram;
import com.actelion.research.datawarrior.task.macro.DETaskPasteMacro;
import com.actelion.research.datawarrior.task.macro.DETaskRunMacro;
import com.actelion.research.datawarrior.task.table.DETaskPasteIntoTable;
import com.actelion.research.datawarrior.task.view.DETaskCopyViewImage;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JScrollableMenu;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.JCompoundTableForm;
import com.actelion.research.util.BrowserControl;
import com.actelion.research.util.ColorHelper;
import com.actelion.research.util.Platform;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.awt.print.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.prefs.Preferences;

import static com.actelion.research.chem.descriptor.DescriptorConstants.DESCRIPTOR_ShapeAlign;
import static com.actelion.research.chem.descriptor.DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE;
import static com.actelion.research.chem.io.CompoundTableConstants.cReactionPartProducts;
import static com.actelion.research.chem.io.CompoundTableConstants.cReactionPartReactants;

public class StandardMenuBar extends JMenuBar implements ActionListener,
		CompoundTableListener,CompoundTableListListener,ItemListener {
	static final long serialVersionUID = 0x20060728;

	public static final boolean SUPPRESS_CHEMISTRY = false;

	private static final String MENU_NAME_FILE = "File";
	private static final String MENU_NAME_EDIT = "Edit";
	private static final String MENU_NAME_DATA = "Data";
	private static final String MENU_NAME_CHEMISTRY = "Chemistry";
	private static final String MENU_NAME_DATABASE = "Database";
	private static final String MENU_NAME_LIST = "List";
	private static final String MENU_NAME_MACRO = "Macro";
	private static final String MENU_NAME_HELP = "Help";
	private static final String DEFAULT_PLUGIN_MENU = MENU_NAME_DATABASE;

	public static final String USER_MACRO_DIR = "$HOME/datawarrior/macro";

	public static final String PREFERENCES_KEY_RECENT_FILE = "recentFile";
	public static final int MAX_RECENT_FILE_COUNT = 24;

	private static final String OPEN_FILE = "open_";
	private static final String NEW_FROM_LIST = "newFromList_";
	private static final String SET_RANGE = "range_";
	private static final String UPDATE = "update_";
	private static final String LIST_ADD = "add_";
	private static final String LIST_REMOVE = "remove_";
	private static final String LIST_SELECT = "select_";
	private static final String LIST_DESELECT = "deselect_";
	private static final String LIST_DELETE = "delete_";
	private static final String LOOK_AND_FEEL = "laf_";
	private static final String EXPORT_MACRO = "export_";
	private static final String COPY_MACRO = "copyMacro_";
	public static final String RUN_GLOBAL_MACRO = "runGlobal_";
	private static final String RUN_INTERNAL_MACRO = "runInternal_";
	private static final String SHOW_NEWS = "showNews_";

	private static final String DEFAULT_LIST_NAME = "Default List";

	private static final String MORE_DATA_URL = "http://www.openmolecules.org/datawarrior/datafiles.html";
	private static final String FORUM_URL = "http://www.openmolecules.org/forum/index.php";

	final static int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

	private DataWarrior			mApplication;
	private DEFrame				mParentFrame;
	private DEParentPane		mParentPane;
	private DEMainPane			mMainPane;
	private CompoundTableModel  mTableModel;
	private PageFormat          mPageFormat;
	private double              mMainSplitting,mRightSplitting;
	private Thread              mMessageThread;
	private TreeMap<Integer,int[]> mMacroItemCountMap;
	private TreeMap<String,DENews> mNewsMap;

	private JMenu jMenuFileNewFrom,jMenuFileOpenSpecial,jMenuFileOpenRecent,jMenuFileSaveSpecial,jMenuEditPasteSpecial,jMenuDataRemoveRows,
				  jMenuDataSelfOrganizingMap,jMenuDataSetRange,jMenuDataViewLogarithmic,jMenuChemAddMoleculeDescriptor,
				  jMenuChemAddReactionDescriptor,jMenuListCreate,jMenuMacroExport,jMenuMacroCopy,jMenuMacroRun,jMenuHelpNews,jMenuHelpLaF,jMenuHelpUpdate,jMenuChemMachineLearning;

	private JMenuItem jMenuFileNew,jMenuFileNewFromVisible,jMenuFileNewFromSelection,jMenuFileNewFromPivoting,jMenuFileNewFromReversePivoting,jMenuFileNewFromTransposition,
					  jMenuFileOpen,jMenuFileOpenMacro,jMenuFileOpenTemplate,jMenuFileOpenMDLReactions,jMenuFileMerge,
					  jMenuFileAppend,jMenuFileClose,jMenuFileCloseAll,jMenuFileSave,jMenuFileSaveAs,jMenuFileSaveText,
					  jMenuFileSaveSDF,jMenuFileSaveTemplate,jMenuFileSaveVisibleAs,jMenuFilePageFormat,
					  jMenuFilePreview,jMenuFilePrint, jMenuFileQuit,jMenuEditCut,jMenuEditCopy,jMenuEditPaste,
					  jMenuEditPasteWithHeader,jMenuEditPasteWithoutHeader,jMenuEditDelete,jMenuEditPasteAppend,jMenuEditPasteMerge,
					  jMenuEditSelectAll,jMenuEditSelectRowsRandomly,jMenuEditExtendSelection,jMenuEditInvertSelection,jMenuEditSearchAndReplace,jMenuEditDisableFilters,
					  jMenuEditEnableFilters,jMenuEditResetFilters,jMenuEditRemoveFilters,
					  jMenuEditNewFilter,jMenuDataRemoveColumns,jMenuDataRemoveSelected,jMenuDataRemoveInvisible,
					  jMenuDataRemoveDuplicate,jMenuDataRemoveUnique,jMenuDataMergeColumns,jMenuDataMergeDuplicate,jMenuDataSplitRows,
					  jMenuDataAddEmptyColumns,jMenuDataAddEmptyRows,jMenuDataAddRowNumbers,jMenuDataAddCalculatedValues,
					  jMenuDataAddBinnedColumn,jMenuDataAddFuzzyScore,jMenuDataAddPrincipalComponents,jMenuDataCreateTSNE,jMenuDataCreateUMAP,
					  jMenuDataSOMCreate,jMenuDataSOMApply,jMenuDataSOMAnalyse, jMenuChemPredictMissingValues, jMenuChemAssessPredictionQuality,
					  jMenuDataGiniScore,jMenuDataArrangeGraph,jMenuDataCorrelationMatrix,
					  jMenuChemExtractReactants,jMenuChemExtractCatalysts,jMenuChemExtractProducts,jMenuChemExtractTransformation,
					  jMenuChemCCLibrary,jMenuChemEALibrary,jMenuChemEnumerateMarkush,jMenuChemAddProperties,jMenuChemAddFormula,jMenuChemAddSmiles,
					  jMenuChemAddInchi,jMenuChemAddInchiKey,jMenuChemAddCanonicalCode,jMenuChemCreate2DCoords,jMenuChemCreate3DCoords,
					  jMenuChemSuperpose,jMenuChemDock, jMenuChemExtractFragment,
					  jMenuChemAddSubstructureCount,jMenuChemAddStructureFromName, jMenuChemDecomposeRGroups,jMenuChemInteractiveSARTable,
					  jMenuChemAnalyzeScaffolds,jMenuChemAnalyzeCliffs,jMenuChemMatchFile,jMenuChemSelectDiverse,
					  jMenuChemCluster,jMenuChemExtract3DFragments,jMenuChemMapReactions,jMenuChemCompareReactionMapping,
					  jMenuChemCreateGenericTautomers,jMenuChemCompareDescriptorSimilarityDistribution,jMenuChemGenerateRandomMolecules,
					  jMenuChemExtractPairwiseCompoundSimilarities,jMenuChemExtractPairwiseStuff,jMenuChemCountAtomTypes,jMenuChemCheckIDCodes,
					  jMenuChemRunSurfacePLS,jMenuChemClassifyReactions,jMenuDBWikipedia,jMenuDBReadChEMBL,jMenuDBFindChEMBLActives,jMenuDBPatentReactions,
					  jMenuDBSearchCOD, jMenuDBSearchBuildingBlocks,jMenuDBSearchChemSpace,jMenuDBRetrieveDataFromURL,jMenuDBRetrieveSQLData,jMenuDBGooglePatents,
					  jMenuListCreateSelected,jMenuListCreateVisible,jMenuListCreateHidden,jMenuListCreateClipboard,
					  jMenuListCreateDuplicate,jMenuListCreateUnique,jMenuListCreateDistinct,
					  jMenuListCreateMerge,jMenuListDeleteAll,jMenuListNewColumn,jMenuListListsFromColumn,jMenuListImport,
					  jMenuListExport,jMenuMacroImport,jMenuMacroPaste,jMenuMacroStartRecording,jMenuMacroContinueRecording,
					  jMenuMacroStopRecording,jMenuHelpHelp,jMenuHelpShortcuts,jMenuHelpMoreData,jMenuHelpForum,jMenuHelpAbout;

	private DEScrollableMenu jMenuFileNewFromList,jMenuListAddSelectedTo,jMenuListRemoveSelectedFrom,
			jMenuListSelectFrom,jMenuListDeselectFrom,jMenuListDelete;
	private JLabel mMessageLabel;

	public StandardMenuBar(DEFrame parentFrame) {
		mApplication = parentFrame.getApplication();
		mParentFrame = parentFrame;
		mParentPane = parentFrame.getMainFrame();
		mMainPane = parentFrame.getMainFrame().getMainPane();
		mTableModel = mParentPane.getTableModel();
		mTableModel.addCompoundTableListener(this);
		mTableModel.getListHandler().addCompoundTableListListener(this);
		buildMenu();
		addOtherActionKeys();
		}

	public DEFrame getParentFrame() {
		return mParentFrame;
		}

	public void updateNewsMenu(TreeMap<String,DENews> newsMap) {
		mNewsMap = newsMap;
		jMenuHelpNews.removeAll();
		try {
			for (String newsID : newsMap.keySet())
				addMenuItem(jMenuHelpNews, newsMap.get(newsID).getTitle(), SHOW_NEWS+newsID);

			if (jMenuHelpNews.getItemCount() == 0)
				addMenuItem(jMenuHelpNews, "<no news yet>", null);
			}
		catch (Exception e) {
			e.printStackTrace();
			}
		}

	private void addOtherActionKeys() {
		final String expandMain = "ExpandMain";
		getActionMap().put(expandMain, new AbstractAction(expandMain) {
			@Override
			public void actionPerformed(ActionEvent evt) {
				double splitting = (mParentPane.getMainSplitting() != 1.0) ? 1.0 : mMainSplitting == 1.0 ? 0.75 : mMainSplitting;
				mMainSplitting = mParentPane.getMainSplitting();
				mParentPane.setMainSplitting(splitting);
			}
		});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), expandMain);

		final String expandFilters = "ExpandFilters";
		getActionMap().put(expandFilters, new AbstractAction(expandFilters) {
			@Override
			public void actionPerformed(ActionEvent evt) {
				double splitting = (mParentPane.getRightSplitting() != 1.0) ? 1.0
						: (mRightSplitting > 0.0 && mRightSplitting < 1.0) ? mRightSplitting : 0.7;
				if (mParentPane.getRightSplitting() > 0.0 && mParentPane.getRightSplitting() < 1.0)
					mRightSplitting = mParentPane.getRightSplitting();
				mParentPane.setRightSplitting(splitting);
				}
			});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), expandFilters);

		final String expandDetail = "ExpandDetail";
		getActionMap().put(expandDetail, new AbstractAction(expandDetail) {
			@Override
			public void actionPerformed(ActionEvent evt) {
				double splitting = (mParentPane.getRightSplitting() != 0.0) ? 0.0
						: (mRightSplitting > 0.0 && mRightSplitting < 1.0) ? mRightSplitting : 0.7;
				if (mParentPane.getRightSplitting() > 0.0 && mParentPane.getRightSplitting() < 1.0)
					mRightSplitting = mParentPane.getRightSplitting();
				mParentPane.setRightSplitting(splitting);
				}
			});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), expandDetail);

		final String nextview = "nextview";
		getActionMap().put(nextview, new AbstractAction(nextview) {
			@Override
			public void actionPerformed(ActionEvent evt) {
				mMainPane.selectNextVisibleView();
				}
			});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), nextview);

		final String maximize = "maximize";
		getActionMap().put(maximize, new AbstractAction(maximize) {
			@Override
			public void actionPerformed(ActionEvent evt) {
				mMainPane.maximize(mMainPane.getSelectedViewTitle(), null);
				}
			});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), maximize);

		final String copyView = "copyView";
		getActionMap().put(copyView, new AbstractAction(copyView) {
			@Override
			public void actionPerformed(ActionEvent evt) {
				DETaskCopyViewImage task = new DETaskCopyViewImage(mParentFrame, mMainPane, mMainPane.getSelectedView());
				task.runTask(task.getRecentOrDefaultConfiguration());
				}
			});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK), copyView);

		final String animSuspend = "animSuspend";
		getActionMap().put(animSuspend, new AbstractAction(animSuspend) {
			@Override
			public void actionPerformed(ActionEvent evt) {
				mParentPane.getPruningPanel().toggleSuspendAnimations();
				}
			});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), animSuspend);

		final String animFastLeft = "animFastLeft";
		getActionMap().put(animFastLeft, new AbstractAction(animFastLeft) {
			@Override
			public void actionPerformed(ActionEvent evt) {
				mParentPane.getPruningPanel().skipAnimationFrames(-1000);
				}
			});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK), animFastLeft);

		final String animFastRight = "animFastRight";
		getActionMap().put(animFastRight, new AbstractAction(animFastRight) {
			@Override
			public void actionPerformed(ActionEvent evt) {
				mParentPane.getPruningPanel().skipAnimationFrames(1000);
				}
			});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK), animFastRight);

		final String animLeft = "animLeft";
		getActionMap().put(animLeft, new AbstractAction(animLeft) {
			@Override
			public void actionPerformed(ActionEvent evt) {
				mParentPane.getPruningPanel().skipAnimationFrames(-100);
				}
			});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK), animLeft);

		final String animRight = "animRight";
		getActionMap().put(animRight, new AbstractAction(animRight) {
			@Override
			public void actionPerformed(ActionEvent evt) {
				mParentPane.getPruningPanel().skipAnimationFrames(100);
				}
			});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK), animRight);

		final String addRefRow = "addRefRow";
		getActionMap().put(addRefRow, new AbstractAction(addRefRow) {
			@Override
			public void actionPerformed(ActionEvent evt) {
				if (mTableModel.hasSelectedRows()) {
					CompoundTableListHandler lh = mTableModel.getListHandler();
					int list = lh.getListIndex(DEFAULT_LIST_NAME);
					if (list == -1)
						list = lh.getListIndex(lh.createList(DEFAULT_LIST_NAME));
					if (list != -1) {
						showTimedMessage("Rows added to default list");
						new DETaskAddSelectionToList(mParentFrame, list).defineAndRun();
						}
					}
				}
			});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_1, KeyEvent.CTRL_MASK), addRefRow);

		final String removeRefRow = "removeRefRow";
		getActionMap().put(removeRefRow, new AbstractAction(removeRefRow) {
			@Override
			public void actionPerformed(ActionEvent evt) {
				if (mTableModel.hasSelectedRows()) {
					CompoundTableListHandler lh = mTableModel.getListHandler();
					int list = lh.getListIndex(DEFAULT_LIST_NAME);
					if (list != -1) {
						showTimedMessage("Rows removed from default list");
						new DETaskRemoveSelectionFromList(mParentFrame, list).defineAndRun();
						}
					}
				}
			});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_0, KeyEvent.CTRL_MASK), removeRefRow);
		}

	private void showTimedMessage(String msg) {
		Color fg = Color.RED;
		Color bg = UIManager.getColor("Panel.background");
		mMessageLabel.setText(msg);

		if (mMessageThread != null)
			mMessageThread.interrupt();

		mMessageThread = new Thread(() -> {
			SwingUtilities.invokeLater(() -> mMessageLabel.setForeground(fg) );
			try { Thread.sleep(300); } catch (InterruptedException ie) { return; }
			for (float i=0.9f; i>0.05f && mMessageThread==Thread.currentThread(); i-=0.1f) {
				float _i = i;
				SwingUtilities.invokeLater(() -> mMessageLabel.setForeground(ColorHelper.intermediateColor(bg, fg, _i)) );
				try { Thread.sleep(50); } catch (InterruptedException ie) { return; }
				}
			if (mMessageThread == Thread.currentThread()) {
				SwingUtilities.invokeLater(() -> {
					mMessageLabel.setText("");
					mMessageLabel.setForeground(fg);
					});
				}
			mMessageThread = null;
			} );

		mMessageThread.start();
		}

	protected void buildMenu() {
		add(buildFileMenu());
		add(buildEditMenu());
		add(buildDataMenu());
		if (!SUPPRESS_CHEMISTRY)
			add(buildChemistryMenu());
		add(buildDatabaseMenu());
		add(buildListMenu());
		add(buildMacroMenu());
		add(buildHelpMenu());

		mApplication.getPluginRegistry().addPluginMenuItems(this);

		double[][] size = {{TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL},{HiDPIHelper.scale(2), TableLayout.PREFERRED}};
		JPanel msgPanel = new JPanel();
		mMessageLabel = new JLabel();
		mMessageLabel.setForeground(Color.RED);
		msgPanel.setLayout(new TableLayout(size));
		msgPanel.add(mMessageLabel, "1,1");
		add(msgPanel);
		}

	private JMenu ensureMenu(JComponent parent, String name) {
		for (Component c:getComponents())
			if (c instanceof JMenu && ((JMenu)c).getText().equalsIgnoreCase(name))
				return (JMenu)c;

		JMenu menu = new JMenu(name);
		parent.add(menu);
		return menu;
		}

	protected JMenu buildFileMenu() {
		jMenuFileNew = new JMenuItem();
		jMenuFileNewFrom = new JMenu();
		jMenuFileNewFromVisible = new JMenuItem();
		jMenuFileNewFromSelection = new JMenuItem();
		jMenuFileNewFromList = new DEScrollableMenu();
		jMenuFileNewFromPivoting = new JMenuItem();
		jMenuFileNewFromReversePivoting = new JMenuItem();
		jMenuFileNewFromTransposition = new JMenuItem();
		jMenuFileOpen = new JMenuItem();
		jMenuFileOpenRecent = new JMenu();
		jMenuFileOpenSpecial = new JMenu();
		jMenuFileOpenMacro = new JMenuItem();
		jMenuFileOpenTemplate = new JMenuItem();
		jMenuFileOpenMDLReactions = new JMenuItem();
		jMenuFileMerge = new JMenuItem();
		jMenuFileAppend = new JMenuItem();
		jMenuFileClose = new JMenuItem();
		jMenuFileCloseAll = new JMenuItem();
		jMenuFileSave = new JMenuItem();
		jMenuFileSaveAs = new JMenuItem();
		jMenuFileSaveSpecial = new JMenu();
		jMenuFileSaveText = new JMenuItem();
		jMenuFileSaveSDF = new JMenuItem();
		jMenuFileSaveTemplate = new JMenuItem();
		jMenuFileSaveVisibleAs = new JMenuItem();
		jMenuFilePageFormat = new JMenuItem();
		jMenuFilePreview = new JMenuItem();
		jMenuFilePrint = new JMenuItem();
		jMenuFileQuit = new JMenuItem();

		jMenuFileNew.setText("New...");
		jMenuFileNew.setAccelerator(KeyStroke.getKeyStroke('N', MENU_MASK));
		jMenuFileNew.addActionListener(this);
		jMenuFileNewFrom.setText("New From");
		jMenuFileNewFromVisible.setText("Visible Rows");
		jMenuFileNewFromVisible.addActionListener(this);
		jMenuFileNewFromSelection.setText("Selected Rows");
		jMenuFileNewFromSelection.setAccelerator(KeyStroke.getKeyStroke('N', Event.SHIFT_MASK | MENU_MASK));
		jMenuFileNewFromSelection.addActionListener(this);
		jMenuFileNewFromList.setText("Row List");
		jMenuFileNewFromPivoting.setText("Pivoting...");
		jMenuFileNewFromPivoting.addActionListener(this);
		jMenuFileNewFromReversePivoting.setText("Reverse Pivoting...");
		jMenuFileNewFromReversePivoting.addActionListener(this);
		jMenuFileNewFromTransposition.setText("Transposition...");
		jMenuFileNewFromTransposition.addActionListener(this);
		jMenuFileOpen.setText("Open...");
		jMenuFileOpen.setAccelerator(KeyStroke.getKeyStroke('O', MENU_MASK));
		jMenuFileOpen.addActionListener(this);
		jMenuFileOpenRecent.setText("Open Recent");
		jMenuFileOpenSpecial.setText("Open Special");
		jMenuFileOpenMacro.setText("Run Macro...");
		jMenuFileOpenMacro.addActionListener(this);
		jMenuFileOpenTemplate.setText("Apply Template...");
		jMenuFileOpenTemplate.addActionListener(this);
		jMenuFileOpenMDLReactions.setText("IsisBase Reactions...");
		jMenuFileOpenMDLReactions.addActionListener(this);
		jMenuFileMerge.setText("Merge File...");
		jMenuFileMerge.addActionListener(this);
		jMenuFileAppend.setText("Append File...");
		jMenuFileAppend.addActionListener(this);
		jMenuFileClose.setText("Close");
		jMenuFileClose.setAccelerator(KeyStroke.getKeyStroke('W', MENU_MASK));
		jMenuFileClose.addActionListener(this);
		jMenuFileCloseAll.setText("Close All");
		jMenuFileCloseAll.setAccelerator(KeyStroke.getKeyStroke('W', Event.SHIFT_MASK | MENU_MASK));
		jMenuFileCloseAll.addActionListener(this);
		jMenuFileSave.setText("Save");
		jMenuFileSave.setAccelerator(KeyStroke.getKeyStroke('S', MENU_MASK));
		jMenuFileSave.addActionListener(this);
		jMenuFileSaveAs.setText("Save As...");
		jMenuFileSaveAs.setAccelerator(KeyStroke.getKeyStroke('S', Event.SHIFT_MASK | MENU_MASK));
		jMenuFileSaveAs.addActionListener(this);
		jMenuFileSaveSpecial.setText("Save Special");
		jMenuFileSaveText.setText("Textfile...");
		jMenuFileSaveText.addActionListener(this);
		jMenuFileSaveSDF.setText("SD-File...");
		jMenuFileSaveSDF.addActionListener(this);
		jMenuFileSaveTemplate.setText("Template...");
		jMenuFileSaveTemplate.addActionListener(this);
		jMenuFileSaveVisibleAs.setText("Save Visible As...");
		jMenuFileSaveVisibleAs.addActionListener(this);
		jMenuFilePageFormat.setText("Page Format...");
		jMenuFilePageFormat.addActionListener(this);
		jMenuFilePreview.setText("Print Preview");
		jMenuFilePreview.addActionListener(this);
		jMenuFilePrint.setText("Print...");
		jMenuFilePrint.addActionListener(this);
		if (!mApplication.isMacintosh()) {
			jMenuFileQuit.setText("Quit");
			jMenuFileQuit.setAccelerator(KeyStroke.getKeyStroke('Q', MENU_MASK));
			jMenuFileQuit.addActionListener(this);
			}

		JMenu jMenuFile = new JMenu();
		jMenuFile.setMnemonic(KeyEvent.VK_F);
		jMenuFile.setText(MENU_NAME_FILE);
		jMenuFile.add(jMenuFileNew);
		jMenuFileNewFrom.add(jMenuFileNewFromVisible);
		jMenuFileNewFrom.add(jMenuFileNewFromSelection);
		jMenuFileNewFrom.add(jMenuFileNewFromList);
		jMenuFileNewFrom.add(jMenuFileNewFromPivoting);
		jMenuFileNewFrom.add(jMenuFileNewFromReversePivoting);
		jMenuFileNewFrom.add(jMenuFileNewFromTransposition);
		jMenuFile.add(jMenuFileNewFrom);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileOpen);
		jMenuFile.add(jMenuFileOpenRecent);
		updateRecentFileMenu();
		jMenuFileOpenSpecial.add(jMenuFileOpenTemplate);
		jMenuFileOpenSpecial.add(jMenuFileOpenMacro);
		addIdorsiaOpenFileMenuOptions(jMenuFileOpenSpecial);
		jMenuFileOpenSpecial.add(jMenuFileOpenMDLReactions);
		jMenuFile.add(jMenuFileOpenSpecial);
		addResourceFileMenus(jMenuFile);
		if (!mApplication.isIdorsia()) {
			jMenuHelpMoreData = new JMenuItem();
			jMenuHelpMoreData.setText("Download More Data...");
			jMenuHelpMoreData.addActionListener(this);
			jMenuFile.addSeparator();
			jMenuFile.add(jMenuHelpMoreData);
			}
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileMerge);
		jMenuFile.add(jMenuFileAppend);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileClose);
		jMenuFile.add(jMenuFileCloseAll);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileSave);
		jMenuFile.add(jMenuFileSaveAs);
		jMenuFileSaveSpecial.add(jMenuFileSaveText);
		jMenuFileSaveSpecial.add(jMenuFileSaveSDF);
		addIdorsiaSaveFileMenuOptions(jMenuFileSaveSpecial);
		jMenuFileSaveSpecial.add(jMenuFileSaveTemplate);
		jMenuFile.add(jMenuFileSaveSpecial);
 		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileSaveVisibleAs);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFilePageFormat);
//		jMenuFile.add(jMenuFilePreview);
		jMenuFile.add(jMenuFilePrint);

		if (!mApplication.isMacintosh()) {
			jMenuFile.addSeparator();
			jMenuFile.add(jMenuFileQuit);
			}
		return jMenuFile;
		}

	// override to add Idorsia specific items
	protected void addIdorsiaOpenFileMenuOptions(JMenu jMenuFileOpenSpecial) {}
	protected void addIdorsiaSaveFileMenuOptions(JMenu jMenuFileOpenSpecial) {}
	protected void addIdorsiaResourceFileMenus(JMenu parentMenu) {}


		protected JMenu buildEditMenu() {
		jMenuEditCut = new JMenuItem();
		jMenuEditCopy = new JMenuItem();
		jMenuEditPaste = new JMenuItem();
		jMenuEditPasteSpecial = new JMenu();
		jMenuEditPasteWithHeader = new JMenuItem();
		jMenuEditPasteWithoutHeader = new JMenuItem();
		jMenuEditPasteAppend = new JMenuItem();
		jMenuEditPasteMerge = new JMenuItem();
		jMenuEditDelete = new JMenuItem();
		jMenuEditSelectAll = new JMenuItem();
		jMenuEditSelectRowsRandomly = new JMenuItem();
		jMenuEditExtendSelection = new JMenuItem();
		jMenuEditInvertSelection = new JMenuItem();
		jMenuEditSearchAndReplace = new JMenuItem();
		jMenuEditDisableFilters = new JMenuItem();
		jMenuEditEnableFilters = new JMenuItem();
		jMenuEditResetFilters = new JMenuItem();
		jMenuEditRemoveFilters = new JMenuItem();
		jMenuEditNewFilter = new JMenuItem();

		jMenuEditCut.setText("Cut");
		jMenuEditCut.setAccelerator(KeyStroke.getKeyStroke('X', MENU_MASK));
		jMenuEditCut.setEnabled(false);
		jMenuEditCopy.setText("Copy");
		jMenuEditCopy.setAccelerator(KeyStroke.getKeyStroke('C', MENU_MASK));
		jMenuEditCopy.addActionListener(this);
		jMenuEditPaste.setText("Paste");
		jMenuEditPaste.setAccelerator(KeyStroke.getKeyStroke('V', MENU_MASK));
		jMenuEditPaste.addActionListener(this);
		jMenuEditPasteSpecial.setText("Paste Special");
		jMenuEditPasteWithHeader.setText("New From Data With Header Row");
		jMenuEditPasteWithHeader.addActionListener(this);
		jMenuEditPasteWithoutHeader.setText("New From Data Without Header Row");
		jMenuEditPasteWithoutHeader.addActionListener(this);
		jMenuEditPasteAppend.setText("Append Data Without Header Row");
		jMenuEditPasteAppend.addActionListener(this);
		jMenuEditPasteMerge.setText("Merge Clipboard Content...");
		jMenuEditPasteMerge.addActionListener(this);
		jMenuEditDelete.setText("Delete");
		jMenuEditDelete.setEnabled(false);
		jMenuEditSelectAll.setText("Select All");
		jMenuEditSelectAll.setAccelerator(KeyStroke.getKeyStroke('A', MENU_MASK));
		jMenuEditSelectAll.addActionListener(this);
		jMenuEditSelectRowsRandomly.setText("Select Rows Randomly...");
		jMenuEditSelectRowsRandomly.addActionListener(this);
		jMenuEditExtendSelection.setText("Extend Row Selection...");
		jMenuEditExtendSelection.addActionListener(this);
		jMenuEditInvertSelection.setText("Invert Row Selection");
		jMenuEditInvertSelection.addActionListener(this);
		jMenuEditSearchAndReplace.setText("Find And Replace...");
		jMenuEditSearchAndReplace.addActionListener(this);
		jMenuEditSearchAndReplace.setAccelerator(KeyStroke.getKeyStroke('H', MENU_MASK));
		jMenuEditNewFilter.setText("New Filter...");
		jMenuEditNewFilter.addActionListener(this);
		jMenuEditDisableFilters.setText("Disable All Filters");
		jMenuEditDisableFilters.addActionListener(this);
		jMenuEditEnableFilters.setText("Enable All Filters");
		jMenuEditEnableFilters.addActionListener(this);
		jMenuEditResetFilters.setText("Reset All Filters...");
		jMenuEditResetFilters.addActionListener(this);
		jMenuEditRemoveFilters.setText("Remove All Filters");
		jMenuEditRemoveFilters.addActionListener(this);

		JMenu jMenuEdit = new JMenu();
		jMenuEdit.setMnemonic(KeyEvent.VK_E);
		jMenuEdit.setText(MENU_NAME_EDIT);
		jMenuEdit.add(jMenuEditCut);
		jMenuEdit.add(jMenuEditCopy);
		jMenuEdit.add(jMenuEditPaste);
		jMenuEditPasteSpecial.add(jMenuEditPasteWithHeader);
		jMenuEditPasteSpecial.add(jMenuEditPasteWithoutHeader);
		jMenuEditPasteSpecial.add(jMenuEditPasteAppend);
		jMenuEditPasteSpecial.add(jMenuEditPasteMerge);
		jMenuEdit.add(jMenuEditPasteSpecial);
		jMenuEdit.add(jMenuEditDelete);
 		jMenuEdit.addSeparator();
		jMenuEdit.add(jMenuEditSelectAll);
		jMenuEdit.add(jMenuEditSelectRowsRandomly);
		jMenuEdit.add(jMenuEditExtendSelection);
		jMenuEdit.add(jMenuEditInvertSelection);
 		jMenuEdit.addSeparator();
 		jMenuEdit.add(jMenuEditSearchAndReplace);
 		jMenuEdit.addSeparator();
 		jMenuEdit.add(jMenuEditNewFilter);
 		jMenuEdit.add(jMenuEditDisableFilters);
 		jMenuEdit.add(jMenuEditEnableFilters);
 		jMenuEdit.add(jMenuEditResetFilters);
		jMenuEdit.add(jMenuEditRemoveFilters);

 		return jMenuEdit;
		}

	protected JMenu buildDataMenu() {
		jMenuDataRemoveColumns = new JMenuItem();
		jMenuDataRemoveRows = new JMenu();
		jMenuDataRemoveSelected = new JMenuItem();
		jMenuDataRemoveInvisible = new JMenuItem();
		jMenuDataRemoveDuplicate = new JMenuItem();
		jMenuDataRemoveUnique = new JMenuItem();
		jMenuDataMergeColumns = new JMenuItem();
		jMenuDataMergeDuplicate = new JMenuItem();
		jMenuDataSplitRows = new JMenuItem();
		jMenuDataAddEmptyColumns = new JMenuItem();
		jMenuDataAddEmptyRows = new JMenuItem();
		jMenuDataAddRowNumbers = new JMenuItem();
		jMenuDataAddBinnedColumn = new JMenuItem();
		jMenuDataAddFuzzyScore = new JMenuItem();
		jMenuDataAddCalculatedValues = new JMenuItem();
		jMenuDataAddPrincipalComponents = new JMenuItem();
		jMenuDataCreateTSNE = new JMenuItem();
		jMenuDataCreateUMAP = new JMenuItem();
		jMenuDataSelfOrganizingMap = new JMenu();
		jMenuDataSOMCreate = new JMenuItem();
		jMenuDataSOMApply = new JMenuItem();
		jMenuDataSOMAnalyse = new JMenuItem();
		jMenuDataGiniScore = new JMenuItem();
		jMenuDataArrangeGraph = new JMenuItem();
		jMenuDataSetRange = new JScrollableMenu();
		jMenuDataViewLogarithmic = new JScrollableMenu();
		jMenuDataCorrelationMatrix = new JMenuItem();

		jMenuDataRemoveColumns.setText("Delete Columns...");
		jMenuDataRemoveColumns.addActionListener(this);
		jMenuDataRemoveRows.setText("Delete Rows");
		jMenuDataRemoveSelected.setText("Selected Rows");
		jMenuDataRemoveSelected.addActionListener(this);
		jMenuDataRemoveInvisible.setText("Invisible Rows");
		jMenuDataRemoveInvisible.addActionListener(this);
		jMenuDataRemoveDuplicate.setText("Duplicate Rows...");
		jMenuDataRemoveDuplicate.addActionListener(this);
		jMenuDataRemoveUnique.setText("Unique Rows...");
		jMenuDataRemoveUnique.addActionListener(this);
		jMenuDataMergeColumns.setText("Merge Columns...");
		jMenuDataMergeColumns.addActionListener(this);
		jMenuDataMergeDuplicate.setText("Merge Equivalent Rows...");
		jMenuDataMergeDuplicate.addActionListener(this);
		jMenuDataSplitRows.setText("Split Multiple Value Rows...");
		jMenuDataSplitRows.addActionListener(this);
		jMenuDataAddEmptyColumns.setText("Add Empty Columns...");
		jMenuDataAddEmptyColumns.addActionListener(this);
		jMenuDataAddEmptyRows.setText("Add Empty Rows...");
		jMenuDataAddEmptyRows.addActionListener(this);
		jMenuDataAddRowNumbers.setText("Add Row Numbers...");
		jMenuDataAddRowNumbers.addActionListener(this);
		jMenuDataAddBinnedColumn.setText("Add Bins From Numbers...");
		jMenuDataAddBinnedColumn.addActionListener(this);
		jMenuDataAddFuzzyScore.setText("Calculate Fuzzy Score...");
		jMenuDataAddFuzzyScore.addActionListener(this);
		jMenuDataGiniScore.setText("Calculate Selectivity Score...");
		jMenuDataGiniScore.addActionListener(this);
		jMenuDataAddCalculatedValues.setText("Add Calculated Values...");
		jMenuDataAddCalculatedValues.addActionListener(this);
		jMenuDataAddPrincipalComponents.setText("Calculate Principal Components...");
		jMenuDataAddPrincipalComponents.addActionListener(this);
		jMenuDataCreateTSNE.setText("Create t-SNE Visualization...");
		jMenuDataCreateTSNE.addActionListener(this);
		jMenuDataCreateUMAP.setText("Create UMAP Visualization...");
		jMenuDataCreateUMAP.addActionListener(this);
		jMenuDataSelfOrganizingMap.setText("Self Organizing Map");
		jMenuDataSOMCreate.setText("Create...");
		jMenuDataSOMCreate.addActionListener(this);
		jMenuDataSOMApply.setText("Open And Apply...");
		jMenuDataSOMApply.addActionListener(this);
		jMenuDataSOMAnalyse.setText("Analyse...");
		jMenuDataSOMAnalyse.addActionListener(this);
		jMenuDataArrangeGraph.setText("Arrange Graph Nodes...");
		jMenuDataArrangeGraph.addActionListener(this);
		jMenuDataViewLogarithmic.setText("Treat Logarithmically");
		jMenuDataSetRange.setText("Set Value Range");
		jMenuDataCorrelationMatrix.setText("Show Correlation Matrix...");
		jMenuDataCorrelationMatrix.addActionListener(this);

		JMenu jMenuData = new JMenu();
		jMenuData.setMnemonic(KeyEvent.VK_D);
		jMenuData.setText(MENU_NAME_DATA);
		jMenuData.add(jMenuDataRemoveColumns);
		jMenuData.add(jMenuDataRemoveRows);
		jMenuDataRemoveRows.add(jMenuDataRemoveSelected);
		jMenuDataRemoveRows.add(jMenuDataRemoveInvisible);
		jMenuDataRemoveRows.add(jMenuDataRemoveDuplicate);
		jMenuDataRemoveRows.add(jMenuDataRemoveUnique);
 		jMenuData.addSeparator();
 		jMenuData.add(jMenuDataMergeColumns);
		jMenuData.add(jMenuDataMergeDuplicate);
		jMenuData.addSeparator();
		jMenuData.add(jMenuDataSplitRows);
		jMenuData.addSeparator();
		jMenuData.add(jMenuDataAddEmptyColumns);
		jMenuData.add(jMenuDataAddEmptyRows);
		jMenuData.add(jMenuDataAddRowNumbers);
		jMenuData.add(jMenuDataAddBinnedColumn);
		jMenuData.add(jMenuDataAddCalculatedValues);
		jMenuData.addSeparator();
		jMenuData.add(jMenuDataGiniScore);
		jMenuData.add(jMenuDataAddFuzzyScore);
		jMenuData.add(jMenuDataAddPrincipalComponents);
		jMenuData.add(jMenuDataCreateTSNE);
		jMenuData.add(jMenuDataCreateUMAP);
		jMenuData.add(jMenuDataSelfOrganizingMap);
		jMenuDataSelfOrganizingMap.add(jMenuDataSOMCreate);
		jMenuDataSelfOrganizingMap.add(jMenuDataSOMApply);
		jMenuDataSelfOrganizingMap.add(jMenuDataSOMAnalyse);
		jMenuData.addSeparator();
		jMenuData.add(jMenuDataArrangeGraph);
		jMenuData.addSeparator();
		jMenuData.add(jMenuDataSetRange);
		jMenuData.add(jMenuDataViewLogarithmic);
		jMenuData.addSeparator();
		jMenuData.add(jMenuDataCorrelationMatrix);

		return jMenuData;
		}

	protected JMenu buildChemistryMenu() {
		jMenuChemCCLibrary = new JMenuItem();
		jMenuChemEALibrary = new JMenuItem();
		jMenuChemGenerateRandomMolecules = new JMenuItem();
		jMenuChemEnumerateMarkush = new JMenuItem();
		jMenuChemAddProperties = new JMenuItem();
		jMenuChemAddMoleculeDescriptor = new JMenu();
		jMenuChemAddReactionDescriptor = new JMenu();
		jMenuChemExtractReactants = new JMenuItem();
		jMenuChemExtractCatalysts = new JMenuItem();
		jMenuChemExtractProducts = new JMenuItem();
		jMenuChemExtractTransformation = new JMenuItem();
		jMenuChemClassifyReactions = new JMenuItem();
		jMenuChemAddFormula = new JMenuItem();
		jMenuChemAddSmiles = new JMenuItem();
		jMenuChemAddInchi = new JMenuItem();
		jMenuChemAddInchiKey = new JMenuItem();
		jMenuChemAddCanonicalCode = new JMenuItem();
		jMenuChemExtractFragment = new JMenuItem();
		jMenuChemAddSubstructureCount = new JMenuItem();
		jMenuChemAddStructureFromName = new JMenuItem();
		jMenuChemCreate2DCoords = new JMenuItem();
		jMenuChemCreate3DCoords = new JMenuItem();
		jMenuChemSuperpose = new JMenuItem();
		jMenuChemDock = new JMenuItem();
		jMenuChemExtract3DFragments = new JMenuItem();
		jMenuChemMachineLearning = new JMenu();
		jMenuChemAssessPredictionQuality = new JMenuItem();
		jMenuChemPredictMissingValues = new JMenuItem();
		jMenuChemDecomposeRGroups = new JMenuItem();
		jMenuChemInteractiveSARTable = new JMenuItem();
		jMenuChemAnalyzeScaffolds = new JMenuItem();
		jMenuChemAnalyzeCliffs = new JMenuItem();
		jMenuChemMatchFile = new JMenuItem();
		jMenuChemSelectDiverse = new JMenuItem();
		jMenuChemCluster = new JMenuItem();
		jMenuChemCreateGenericTautomers = new JMenuItem();
		jMenuChemCheckIDCodes = new JMenuItem();
		jMenuChemMapReactions = new JMenuItem();
		jMenuChemCompareReactionMapping = new JMenuItem();
		jMenuChemCompareDescriptorSimilarityDistribution = new JMenuItem();
		jMenuChemExtractPairwiseCompoundSimilarities = new JMenuItem();
		jMenuChemExtractPairwiseStuff = new JMenuItem();
		jMenuChemCountAtomTypes = new JMenuItem();
		jMenuChemRunSurfacePLS = new JMenuItem();

		jMenuChemCCLibrary.setText("Enumerate Combinatorial Library...");
		jMenuChemCCLibrary.addActionListener(this);
		jMenuChemEALibrary.setText("Build Evolutionary Library...");
		jMenuChemEALibrary.addActionListener(this);
		jMenuChemGenerateRandomMolecules.setText("Generate Random Molecules...");
		jMenuChemGenerateRandomMolecules.addActionListener(this);
		jMenuChemEnumerateMarkush.setText("Enumerate Markush Structure...");
		jMenuChemEnumerateMarkush.addActionListener(this);
		jMenuChemAddProperties.setText("Calculate Properties...");
		jMenuChemAddProperties.addActionListener(this);
		jMenuChemAddMoleculeDescriptor.setText("Calculate Descriptor");
		jMenuChemAddReactionDescriptor.setText("Calculate Descriptor");
		jMenuChemExtractReactants.setText("Extract Reactants");
		jMenuChemExtractReactants.addActionListener(this);
		jMenuChemExtractCatalysts.setText("Extract Catalysts");
		jMenuChemExtractCatalysts.addActionListener(this);
		jMenuChemExtractProducts.setText("Extract Products");
		jMenuChemExtractProducts.addActionListener(this);
		jMenuChemExtractTransformation.setText("Extract Transformation...");
		jMenuChemExtractTransformation.addActionListener(this);
		jMenuChemClassifyReactions.setText("Classify Reactions");
		jMenuChemClassifyReactions.addActionListener(this);
		jMenuChemAddFormula.setText("Add Molecular Formula...");
		jMenuChemAddFormula.addActionListener(this);
		jMenuChemAddSmiles.setText("Add SMILES Code...");
		jMenuChemAddSmiles.addActionListener(this);
		jMenuChemAddInchi.setText("Add Standard InChI...");
		jMenuChemAddInchi.addActionListener(this);
		jMenuChemAddInchiKey.setText("Add InChI-Key...");
		jMenuChemAddInchiKey.addActionListener(this);
		jMenuChemAddCanonicalCode.setText("Add Canonical Code...");
		jMenuChemAddCanonicalCode.addActionListener(this);
		jMenuChemAddSubstructureCount.setText("Add Substructure Count...");
		jMenuChemAddSubstructureCount.addActionListener(this);
		jMenuChemExtractFragment.setText("Extract Fragment...");
		jMenuChemExtractFragment.addActionListener(this);
		jMenuChemAddStructureFromName.setText("Add Structures From Name...");
		jMenuChemAddStructureFromName.addActionListener(this);
		jMenuChemCreate2DCoords.setText("Generate 2D Atom Coordinates...");
		jMenuChemCreate2DCoords.addActionListener(this);
		jMenuChemCreate3DCoords.setText("Generate Conformers...");
		jMenuChemCreate3DCoords.addActionListener(this);
		jMenuChemSuperpose.setText("Superpose Conformers...");
		jMenuChemSuperpose.addActionListener(this);
		jMenuChemDock.setText("Dock Structures Into Protein Cavity...");
		jMenuChemDock.addActionListener(this);
		jMenuChemExtract3DFragments.setText("Build 3D-Fragment Library...");
		jMenuChemExtract3DFragments.addActionListener(this);
		jMenuChemMachineLearning.setText("Machine Learning");
		jMenuChemAssessPredictionQuality.setText("Assess Prediction Quality...");
		jMenuChemAssessPredictionQuality.addActionListener(this);
		jMenuChemPredictMissingValues.setText("Predict Missing Values...");
		jMenuChemPredictMissingValues.addActionListener(this);
		jMenuChemDecomposeRGroups.setText("Decompose R-Groups...");
		jMenuChemDecomposeRGroups.addActionListener(this);
		jMenuChemInteractiveSARTable.setText("Interactive SAR Analysis...");
		jMenuChemInteractiveSARTable.addActionListener(this);
		jMenuChemAnalyzeScaffolds.setText("Analyse Scaffolds...");
		jMenuChemAnalyzeScaffolds.addActionListener(this);
		jMenuChemAnalyzeCliffs.setText("Analyse Similarity/Activity Cliffs...");
		jMenuChemAnalyzeCliffs.addActionListener(this);
		jMenuChemMatchFile.setText("Find Similar Compounds In File...");
		jMenuChemMatchFile.addActionListener(this);
		jMenuChemSelectDiverse.setText("Select Diverse Set...");
		jMenuChemSelectDiverse.addActionListener(this);
		jMenuChemCluster.setText("Cluster Compounds/Reactions...");
		jMenuChemCluster.addActionListener(this);
		jMenuChemCreateGenericTautomers.setText("Create Generic Tautomers");
		jMenuChemCreateGenericTautomers.addActionListener(this);
		jMenuChemCheckIDCodes.setText("Check IDCode Correctness");
		jMenuChemCheckIDCodes.addActionListener(this);
		jMenuChemMapReactions.setText("Map Reaction Atoms");
		jMenuChemMapReactions.addActionListener(this);
		jMenuChemCompareReactionMapping.setText("Compare Reaction Mapping");
		jMenuChemCompareReactionMapping.addActionListener(this);
		jMenuChemCompareDescriptorSimilarityDistribution.setText("Compare Descriptor Similarity Distribution");
		jMenuChemCompareDescriptorSimilarityDistribution.addActionListener(this);
		jMenuChemExtractPairwiseCompoundSimilarities.setText("Extract Pairwise Compound Similarities");
		jMenuChemExtractPairwiseCompoundSimilarities.addActionListener(this);
		jMenuChemExtractPairwiseStuff.setText("Extract Pairwise Similarities And Distances");
		jMenuChemExtractPairwiseStuff.addActionListener(this);
		jMenuChemCountAtomTypes.setText("Count Surface Atom Types");
		jMenuChemCountAtomTypes.addActionListener(this);
		jMenuChemRunSurfacePLS.setText("Run Surface Parameter PLS");
		jMenuChemRunSurfacePLS.addActionListener(this);
		jMenuChemClassifyReactions.setText("Classify Reactions");
		jMenuChemClassifyReactions.addActionListener(this);

		JMenu jMenuChem = new JMenu();
		jMenuChem.setMnemonic(KeyEvent.VK_C);
		jMenuChem.setText(MENU_NAME_CHEMISTRY);

		JMenu jMenuChemFromStructure = new JMenu();
		jMenuChemFromStructure.setText("From Chemical Structure");
		jMenuChemFromStructure.add(jMenuChemAddMoleculeDescriptor);
		jMenuChemFromStructure.add(jMenuChemAddProperties);
		addDescriptorItems(jMenuChemAddMoleculeDescriptor, DESCRIPTOR_TYPE_MOLECULE, null);
		jMenuChemFromStructure.add(jMenuChemAddFormula);
		jMenuChemFromStructure.add(jMenuChemAddSmiles);
		jMenuChemFromStructure.add(jMenuChemAddInchi);
		jMenuChemFromStructure.add(jMenuChemAddInchiKey);
		jMenuChemFromStructure.add(jMenuChemAddCanonicalCode);
		jMenuChemFromStructure.add(jMenuChemAddSubstructureCount);
		jMenuChemFromStructure.add(jMenuChemExtractFragment);
		addIdorsiaChemistryMenuOptions(jMenuChemFromStructure);

		JMenu jMenuChemFromReaction = new JMenu("From Chemical Reaction");
		jMenuChemFromReaction.add(jMenuChemAddReactionDescriptor);
		JMenu jMenuChemAddReactantDescriptor = new JMenu("Reactants");
		JMenu jMenuChemAddProductDescriptor = new JMenu("Products");
		JMenu jMenuChemAddCatalystDescriptor = new JMenu("Catalysts");
		addDescriptorItems(jMenuChemAddReactionDescriptor, DescriptorConstants.DESCRIPTOR_TYPE_REACTION, null);
		addDescriptorItems(jMenuChemAddReactantDescriptor, DESCRIPTOR_TYPE_MOLECULE, cReactionPartReactants);
		addDescriptorItems(jMenuChemAddProductDescriptor, DESCRIPTOR_TYPE_MOLECULE, cReactionPartProducts);
		jMenuChemAddReactionDescriptor.add(jMenuChemAddReactantDescriptor);
		jMenuChemAddReactionDescriptor.add(jMenuChemAddProductDescriptor);
		jMenuChemAddReactionDescriptor.add(jMenuChemAddCatalystDescriptor);
		jMenuChemFromReaction.add(jMenuChemExtractReactants);
		jMenuChemFromReaction.add(jMenuChemExtractCatalysts);
		jMenuChemFromReaction.add(jMenuChemExtractProducts);
		jMenuChemFromReaction.add(jMenuChemExtractTransformation);
		jMenuChemFromReaction.add(jMenuChemMapReactions);
		jMenuChemFromReaction.add(jMenuChemClassifyReactions);

		jMenuChem.add(jMenuChemFromStructure);
		jMenuChem.add(jMenuChemFromReaction);
		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemCCLibrary);
		jMenuChem.add(jMenuChemEALibrary);
		jMenuChem.add(jMenuChemGenerateRandomMolecules);
//		jMenuChem.add(jMenuChemEnumerateMarkush);

		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemAddStructureFromName);

		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemCreate2DCoords);
		jMenuChem.add(jMenuChemCreate3DCoords);
		jMenuChem.add(jMenuChemSuperpose);
		jMenuChem.add(jMenuChemDock);
		jMenuChem.addSeparator();
		if (System.getProperty("development") != null) {
			jMenuChem.add(jMenuChemExtract3DFragments);
			jMenuChem.addSeparator();
			}
		jMenuChem.add(jMenuChemMachineLearning);
		jMenuChemMachineLearning.add(jMenuChemAssessPredictionQuality);
		jMenuChemMachineLearning.add(jMenuChemPredictMissingValues);
		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemDecomposeRGroups);
		if (mApplication.isIdorsia() || System.getProperty("development") != null)
			jMenuChem.add(jMenuChemInteractiveSARTable);
		jMenuChem.add(jMenuChemAnalyzeScaffolds);
		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemAnalyzeCliffs);
		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemMatchFile);
		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemSelectDiverse);
		jMenuChem.add(jMenuChemCluster);

		if (System.getProperty("development") != null) {
			jMenuChem.addSeparator();
			jMenuChem.add(jMenuChemCheckIDCodes);
			jMenuChem.add(jMenuChemCompareReactionMapping);
			jMenuChem.add(jMenuChemCompareDescriptorSimilarityDistribution);
			jMenuChem.add(jMenuChemExtractPairwiseCompoundSimilarities);
			jMenuChem.add(jMenuChemExtractPairwiseStuff);
			jMenuChem.addSeparator();
			jMenuChem.add(jMenuChemCountAtomTypes);
			jMenuChem.add(jMenuChemRunSurfacePLS);
			jMenuChem.addSeparator();
			jMenuChem.add(jMenuChemCreateGenericTautomers);
			}

		return jMenuChem;
		}

	private void addDescriptorItems(JMenu menu, int descriptorType, String reactionPart) {
		for (int i=0; i< DescriptorConstants.DESCRIPTOR_LIST.length; i++)
			if (DescriptorConstants.DESCRIPTOR_LIST[i].type == descriptorType)
				addDescriptorItem(menu, DescriptorConstants.DESCRIPTOR_LIST[i], reactionPart);

		if (System.getProperty("development") != null && descriptorType == DESCRIPTOR_TYPE_MOLECULE)
			addDescriptorItem(menu, DESCRIPTOR_ShapeAlign, null);
		}

	private void addDescriptorItem(JMenu menu, DescriptorInfo di, String reactionPart) {
		String shortName = di.shortName;
		String command = (reactionPart == null) ? shortName : shortName + "_" + reactionPart;
		JMenuItem item = new JMenuItem();
		item.setText(shortName);
		item.setActionCommand(command);
		item.addActionListener(this);
		menu.add(item);
		}

	protected void addIdorsiaChemistryMenuOptions(JMenu jMenuChem) {}	// override to add Actelion specific items

	protected JMenu buildDatabaseMenu() {
		JMenu jMenuDB = new JMenu();
		jMenuDB.setMnemonic(KeyEvent.VK_B);
		jMenuDB.setText(MENU_NAME_DATABASE);

		jMenuDBWikipedia = new JMenuItem();
		jMenuDBWikipedia.setText("Retrieve Wikipedia Molecules");
		jMenuDBWikipedia.addActionListener(this);
		jMenuDB.add(jMenuDBWikipedia);

		jMenuDB.addSeparator();

		jMenuDBGooglePatents = new JMenuItem();
		jMenuDBGooglePatents.setText("Search Google Patents...");
		jMenuDBGooglePatents.addActionListener(this);
		jMenuDB.add(jMenuDBGooglePatents);

		jMenuDBPatentReactions = new JMenuItem();
		jMenuDBPatentReactions.setText("Search US/EU Patent Reactions...");
		jMenuDBPatentReactions.addActionListener(this);
		jMenuDB.add(jMenuDBPatentReactions);

		jMenuDB.addSeparator();

		jMenuDBReadChEMBL = new JMenuItem();
		jMenuDBReadChEMBL.setText("Search ChEMBL Database...");
		jMenuDBReadChEMBL.addActionListener(this);
		jMenuDB.add(jMenuDBReadChEMBL);

		jMenuDBFindChEMBLActives = new JMenuItem();
		jMenuDBFindChEMBLActives.setText("Get Similar Compounds From ChEMBL Actives");
		jMenuDBFindChEMBLActives.addActionListener(this);
		jMenuDB.add(jMenuDBFindChEMBLActives);

		jMenuDB.addSeparator();

		jMenuDBSearchCOD = new JMenuItem();
		jMenuDBSearchCOD.setText("Search Crystallography Open Database...");
		jMenuDBSearchCOD.addActionListener(this);
		jMenuDB.add(jMenuDBSearchCOD);

		jMenuDB.addSeparator();

		jMenuDBSearchBuildingBlocks = new JMenuItem();
		jMenuDBSearchBuildingBlocks.setText("Search Commercial Building Blocks...");
		jMenuDBSearchBuildingBlocks.addActionListener(this);
		jMenuDB.add(jMenuDBSearchBuildingBlocks);

		jMenuDBSearchChemSpace = new JMenuItem();
		jMenuDBSearchChemSpace.setText("Search ChemSpace Chemicals...");
		jMenuDBSearchChemSpace.addActionListener(this);
		jMenuDB.add(jMenuDBSearchChemSpace);

		jMenuDB.addSeparator();

		jMenuDBRetrieveDataFromURL = new JMenuItem();
		jMenuDBRetrieveDataFromURL.setText("Retrieve Data From Custom URL...");
		jMenuDBRetrieveDataFromURL.addActionListener(this);
		jMenuDB.add(jMenuDBRetrieveDataFromURL);

		jMenuDBRetrieveSQLData = new JMenuItem();
		jMenuDBRetrieveSQLData.setText("Retrieve Data From SQL-Database...");
		jMenuDBRetrieveSQLData.addActionListener(this);
		jMenuDB.add(jMenuDBRetrieveSQLData);

		return jMenuDB;
		}

	protected JMenu buildListMenu() {
		jMenuListCreate = new JMenu();
		jMenuListCreateSelected = new JMenuItem();
		jMenuListCreateVisible = new JMenuItem();
		jMenuListCreateHidden = new JMenuItem();
		jMenuListCreateDuplicate = new JMenuItem();
		jMenuListCreateUnique = new JMenuItem();
		jMenuListCreateDistinct = new JMenuItem();
		jMenuListCreateClipboard = new JMenuItem();
		jMenuListCreateMerge = new JMenuItem();
		jMenuListAddSelectedTo = new DEScrollableMenu();
		jMenuListRemoveSelectedFrom = new DEScrollableMenu();
		jMenuListSelectFrom = new DEScrollableMenu();
		jMenuListDeselectFrom = new DEScrollableMenu();
		jMenuListDelete = new DEScrollableMenu();
		jMenuListDeleteAll = new JMenuItem();
		jMenuListNewColumn = new JMenuItem();
		jMenuListListsFromColumn = new JMenuItem();
		jMenuListImport = new JMenuItem();
		jMenuListExport = new JMenuItem();

		jMenuListCreate.setText("Create Row List From");
		jMenuListCreateSelected.setText("Selected Rows...");
		jMenuListCreateSelected.addActionListener(this);
		jMenuListCreateVisible.setText("Visible Rows...");
		jMenuListCreateVisible.addActionListener(this);
		jMenuListCreateHidden.setText("Hidden Rows...");
		jMenuListCreateHidden.addActionListener(this);
		jMenuListCreateDuplicate.setText("Duplicate Rows...");
		jMenuListCreateDuplicate.addActionListener(this);
		jMenuListCreateUnique.setText("Unique Rows...");
		jMenuListCreateUnique.addActionListener(this);
		jMenuListCreateDistinct.setText("Distinct Rows...");
		jMenuListCreateDistinct.addActionListener(this);
		jMenuListCreateClipboard.setText("Clipboard...");
		jMenuListCreateClipboard.addActionListener(this);
		jMenuListCreateMerge.setText("Existing Row Lists...");
		jMenuListCreateMerge.addActionListener(this);
		jMenuListAddSelectedTo.setText("Add Selected To");
		jMenuListRemoveSelectedFrom.setText("Remove Selected From");
		jMenuListSelectFrom.setText("Select Rows From");
		jMenuListDeselectFrom.setText("Deselect Rows From");
		jMenuListDelete.setText("Delete Row List");
		jMenuListDeleteAll.setText("Delete All Row Lists");
		jMenuListDeleteAll.addActionListener(this);
		jMenuListNewColumn.setText("Add Column From Row Lists...");
		jMenuListNewColumn.addActionListener(this);
		jMenuListListsFromColumn.setText("Create Row Lists From Category Column...");
		jMenuListListsFromColumn.addActionListener(this);
		jMenuListExport.setText("Export Row List...");
		jMenuListExport.addActionListener(this);
		jMenuListImport.setText("Import Row List...");
		jMenuListImport.addActionListener(this);
		jMenuListCreate.add(jMenuListCreateSelected);
		jMenuListCreate.add(jMenuListCreateVisible);
		jMenuListCreate.add(jMenuListCreateHidden);
		jMenuListCreate.add(jMenuListCreateDuplicate);
		jMenuListCreate.add(jMenuListCreateUnique);
		jMenuListCreate.add(jMenuListCreateDistinct);
 		jMenuListCreate.addSeparator();
		jMenuListCreate.add(jMenuListCreateClipboard);
 		jMenuListCreate.addSeparator();
		jMenuListCreate.add(jMenuListCreateMerge);
		JMenu jMenuList = new JMenu();
		jMenuList.setMnemonic(KeyEvent.VK_L);
		jMenuList.setText(MENU_NAME_LIST);
		jMenuList.add(jMenuListCreate);
 		jMenuList.addSeparator();
 		jMenuList.add(jMenuListAddSelectedTo);
 		jMenuList.add(jMenuListRemoveSelectedFrom);
		jMenuList.addSeparator();
 		jMenuList.add(jMenuListSelectFrom);
 		jMenuList.add(jMenuListDeselectFrom);
 		jMenuList.addSeparator();
		jMenuList.add(jMenuListDelete);
		jMenuList.add(jMenuListDeleteAll);
		jMenuList.addSeparator();
		jMenuList.add(jMenuListNewColumn);
		jMenuList.add(jMenuListListsFromColumn);
		jMenuList.addSeparator();
		jMenuList.add(jMenuListImport);
		jMenuList.add(jMenuListExport);

		return jMenuList;
		}

	protected JMenu buildMacroMenu() {
		jMenuMacroImport = new JMenuItem();
		jMenuMacroExport = new JMenu();
		jMenuMacroCopy = new JMenu();
		jMenuMacroPaste = new JMenuItem();
		jMenuMacroRun = new JMenu();
		jMenuMacroStartRecording = new JMenuItem();
		jMenuMacroContinueRecording = new JMenuItem();
		jMenuMacroStopRecording = new JMenuItem();

		jMenuMacroImport.setText("Import Macro...");
		jMenuMacroImport.addActionListener(this);
		jMenuMacroExport.setText("Export Macro");
		jMenuMacroCopy.setText("Copy Macro");
		jMenuMacroPaste.setText("Paste Macro");
		jMenuMacroPaste.addActionListener(this);
		jMenuMacroStartRecording.setText("Start Recording...");
		jMenuMacroStartRecording.addActionListener(this);
		jMenuMacroContinueRecording.setText("Continue Recording");
		jMenuMacroContinueRecording.addActionListener(this);
		jMenuMacroStopRecording.setText("Stop Recording");
		jMenuMacroStopRecording.addActionListener(this);
		jMenuMacroRun.setText("Run Macro");
		addMenuItem(jMenuMacroExport, "<no macros defined>", null);
		addMenuItem(jMenuMacroCopy, "<no macros defined>", null);
		addMacroFileItemsLater(jMenuMacroRun);
		JMenu jMenuMacro = new JMenu();
		jMenuMacro.setMnemonic(KeyEvent.VK_M);
		jMenuMacro.setText(MENU_NAME_MACRO);
		jMenuMacro.add(jMenuMacroImport);
		jMenuMacro.add(jMenuMacroExport);
 		jMenuMacro.addSeparator();
		jMenuMacro.add(jMenuMacroCopy);
		jMenuMacro.add(jMenuMacroPaste);
		jMenuMacro.addSeparator();
		jMenuMacro.add(jMenuMacroStartRecording);
		jMenuMacro.add(jMenuMacroContinueRecording);
		jMenuMacro.add(jMenuMacroStopRecording);
 		jMenuMacro.addSeparator();
		jMenuMacro.add(jMenuMacroRun);

		enableMacroItems();
		return jMenuMacro;
		}

	protected JMenu buildHelpMenu() {
		jMenuHelpNews = new JMenu();
		jMenuHelpNews.setText("Recent News");

		jMenuHelpLaF = new JMenu();
		jMenuHelpLaF.setText("Look & Feel");
		for (DataWarrior.LookAndFeel laf:mApplication.getAvailableLAFs()) {
			JCheckBoxMenuItem item = new JCheckBoxMenuItem();
			item.setActionCommand(LOOK_AND_FEEL+laf.displayName());
			item.setText(laf.displayName());
			item.setSelected(laf.className().equals(UIManager.getLookAndFeel().getClass().getCanonicalName()));
			item.addActionListener(this);
			jMenuHelpLaF.add(item);
			}

		jMenuHelpHelp = new JMenuItem();
		jMenuHelpShortcuts = new JMenuItem();
		jMenuHelpAbout = new JMenuItem();
		if (!mApplication.isMacintosh()) {
			jMenuHelpAbout.setText("About DataWarrior...");
			jMenuHelpAbout.addActionListener(this);
			}
		jMenuHelpHelp.setText(MENU_NAME_HELP);
		jMenuHelpHelp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		jMenuHelpHelp.addActionListener(this);
		jMenuHelpShortcuts.setText("Shortcuts...");
		jMenuHelpShortcuts.addActionListener(this);

		JMenu jMenuHelp = new JMenu();
		if (!mApplication.isMacintosh()) {
			jMenuHelp.add(jMenuHelpAbout);
			jMenuHelp.addSeparator();
			}
		jMenuHelp.setText("Help");
		jMenuHelp.setMnemonic(KeyEvent.VK_H);
		jMenuHelp.add(jMenuHelpHelp);
		jMenuHelp.add(jMenuHelpShortcuts);

		jMenuHelp.addSeparator();
		jMenuHelp.add(jMenuHelpNews);

		jMenuHelpForum = new JMenuItem();
		jMenuHelpForum.setText("Open Forum in Browser...");
		jMenuHelpForum.addActionListener(this);
		jMenuHelp.add(jMenuHelpForum);

		jMenuHelp.addSeparator();
		jMenuHelp.add(jMenuHelpLaF);

		addUpdateMenu(jMenuHelp);

		return jMenuHelp;
		}

	public void addUpdateMenu(JMenu jMenuHelp) {
		jMenuHelpUpdate = new JMenu();

		Preferences prefs = DataWarrior.getPreferences();
		String modeString = prefs.get(DEUpdateHandler.PREFERENCES_KEY_UPDATE_MODE, "");
		if (modeString.length() == 0) // happens once after update, because mode is now stored in new setting
			prefs.put(DEUpdateHandler.PREFERENCES_KEY_UPDATE_MODE, DEUpdateHandler.PREFERENCES_UPDATE_MODE_CODE[DEUpdateHandler.PREFERENCES_UPDATE_MODE_ASK]);
		int mode = AbstractTask.findListIndex(modeString, DEUpdateHandler.PREFERENCES_UPDATE_MODE_CODE, DEUpdateHandler.PREFERENCES_UPDATE_MODE_ASK);

		jMenuHelpUpdate.setText("Update Mode");
		for (int i=0; i<DEUpdateHandler.PREFERENCES_UPDATE_MODE_CODE.length; i++) {
			JCheckBoxMenuItem item = new JCheckBoxMenuItem();
			item.setActionCommand(UPDATE+DEUpdateHandler.PREFERENCES_UPDATE_MODE_CODE[i]);
			item.setText(DEUpdateHandler.PREFERENCES_UPDATE_MODE_TEXT[i]);
			item.setSelected(mode == i);
			item.addActionListener(this);
			jMenuHelpUpdate.add(item);
			}

//		Preferences prefs = DataWarrior.getPreferences();
//		boolean check = prefs.getBoolean(DEUpdateHandler.PREFERENCES_KEY_LEGACY_UPDATE_CHECK, true);
//		jMenuHelpAutomaticUpdateCheck = new JCheckBoxMenuItem();
//		jMenuHelpCheckForUpdate = new JMenuItem();
//
//		jMenuHelpUpdate.setText("Update Checking");
//		jMenuHelpAutomaticUpdateCheck.setText("Automatically Check For Updates");
//		jMenuHelpAutomaticUpdateCheck.setSelected(check);
//		jMenuHelpAutomaticUpdateCheck.addActionListener(this);
//		jMenuHelpCheckForUpdate.setText("Check For Update Now...");
//		jMenuHelpCheckForUpdate.addActionListener(this);
//
//		jMenuHelpUpdate.add(jMenuHelpAutomaticUpdateCheck);
//		jMenuHelpUpdate.add(jMenuHelpCheckForUpdate);

		jMenuHelp.addSeparator();
		jMenuHelp.add(jMenuHelpUpdate);
		}

	private void ensurePageFormat(PrinterJob job) {
		if (mPageFormat == null) {
			mPageFormat = job.defaultPage();
			Paper paper = mPageFormat.getPaper();
			paper.setImageableArea(60, 30, paper.getWidth() - 90, paper.getHeight() - 60);
			mPageFormat.setPaper(paper);
			}
		}

	private void menuFilePageFormat() {
		PrinterJob job = PrinterJob.getPrinterJob();
		ensurePageFormat(job);
		mPageFormat = job.pageDialog(mPageFormat);
		}

	private void menuFilePreview() {

		}

	public void menuFilePrint() {
		if (mMainPane.getSelectedDockable() == null) {
			JOptionPane.showMessageDialog(mParentFrame, "Sorry, an empty view cannot be printed");
			return;
			}

		PrinterJob job = PrinterJob.getPrinterJob();
		ensurePageFormat(job);

		try {
			Component component = mMainPane.getSelectedDockable().getContent();
			if (component instanceof DEFormView) {
				JCompoundTableForm form = ((DEFormView)component).getCompoundTableForm();
				if (!new DEPrintFormDialog(mParentFrame, mTableModel, form).isOK())
					return;
				form.setPageFormat(mPageFormat);
				job.setPageable(form);
				}
			else { // assume Printable
				job.setPrintable((Printable)component, mPageFormat);
				}

			job.setJobName("DataWarrior:"+mParentFrame.getTitle());
			if (job.printDialog()) {
				try {
					job.print();
					}
				catch (PrinterException e) {
					JOptionPane.showMessageDialog(mParentFrame, e);
					}
				}
			}
		catch (ClassCastException e) {
			JOptionPane.showMessageDialog(mParentFrame, "Sorry, the current view cannot be printed");
			}
		catch (Exception e) {
			e.printStackTrace();
			}
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cNewTable) {
			jMenuFileNewFromList.removeAll();
			jMenuListSelectFrom.removeAll();
			jMenuListDeselectFrom.removeAll();
			jMenuListAddSelectedTo.removeAll();
			jMenuListRemoveSelectedFrom.removeAll();
			jMenuListDelete.removeAll();
			}
		if (e.getType() == CompoundTableEvent.cNewTable
		 || e.getType() == CompoundTableEvent.cAddColumns
		 || e.getType() == CompoundTableEvent.cRemoveColumns
		 || e.getType() == CompoundTableEvent.cChangeColumnName
		 || e.getType() == CompoundTableEvent.cChangeColumnData
		 || e.getType() == CompoundTableEvent.cAddRows
		 || e.getType() == CompoundTableEvent.cDeleteRows) {
			jMenuDataViewLogarithmic.removeAll();
			jMenuDataSetRange.removeAll();
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				if (mTableModel.isLogarithmicViewMode(column)
				 || (mTableModel.isColumnTypeDouble(column)
				  && !mTableModel.isColumnTypeDate(column)
				  && (CompoundTableConstants.cAllowLogModeForNegativeOrZeroValues
				   || mTableModel.getMinimumValue(column) > 0))) {
//					JCheckBoxMenuItem item = new StayOpenCheckBoxMenuItem(mTableModel.getColumnTitle(column),
//							mTableModel.isLogarithmicViewMode(column));
					JCheckBoxMenuItem item = new MyCheckBoxMenuItem(mTableModel.getColumnTitle(column),
							mTableModel.isLogarithmicViewMode(column));
					if (mTableModel.getMinimumValue(column) <= 0)
						item.setForeground(Color.red);

					item.addItemListener(this);
					jMenuDataViewLogarithmic.add(item);
					}
				if (mTableModel.isColumnTypeDouble(column)) {
					addMenuItem(jMenuDataSetRange, mTableModel.getColumnTitle(column)+"...", SET_RANGE+mTableModel.getColumnTitle(column));
					}
				} jMenuDataViewLogarithmic.updateUI();
			}
		if (e.getType() == CompoundTableEvent.cNewTable
		 || (e.getType() == CompoundTableEvent.cChangeExtensionData
		  && e.getSpecifier() == DECompoundTableExtensionHandler.ID_MACRO)) {
			SwingUtilities.invokeLater(() -> {
				jMenuMacroExport.removeAll();
				jMenuMacroCopy.removeAll();
				jMenuMacroRun.removeAll();
				@SuppressWarnings("unchecked")
				ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)mTableModel.getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
				if (macroList == null || macroList.size() == 0) {
					addMenuItem(jMenuMacroExport, "<no macros defined>", null);
					addMenuItem(jMenuMacroCopy, "<no macros defined>", null);
					}
				else {
					for (DEMacro macro : macroList) {
						addMenuItem(jMenuMacroExport, macro.getName() + "...", EXPORT_MACRO + macro.getName());
						addMenuItem(jMenuMacroCopy, macro.getName(), COPY_MACRO + macro.getName());
						addMenuItem(jMenuMacroRun, macro.getName(), RUN_INTERNAL_MACRO + macro.getName());
						}
					}
				} );
			addMacroFileItemsLater(jMenuMacroRun);
			}
		}

	public void listChanged(CompoundTableListEvent e) {
		jMenuFileNewFromList.removeAll();
		jMenuListSelectFrom.removeAll();
		jMenuListDeselectFrom.removeAll();
		jMenuListAddSelectedTo.removeAll();
		jMenuListRemoveSelectedFrom.removeAll();
		jMenuListDelete.removeAll();
		CompoundTableListHandler hitlistHandler = mTableModel.getListHandler();

		for (int hitlist = 0; hitlist<hitlistHandler.getListCount(); hitlist++) {
			addMenuItem(jMenuFileNewFromList, hitlistHandler.getListName(hitlist), NEW_FROM_LIST+hitlist);
			addMenuItem(jMenuListAddSelectedTo, hitlistHandler.getListName(hitlist), LIST_ADD+hitlist);
			addMenuItem(jMenuListRemoveSelectedFrom, hitlistHandler.getListName(hitlist), LIST_REMOVE+hitlist);
			addMenuItem(jMenuListSelectFrom, hitlistHandler.getListName(hitlist), LIST_SELECT+hitlist);
			addMenuItem(jMenuListDeselectFrom, hitlistHandler.getListName(hitlist), LIST_DESELECT+hitlist);
			addMenuItem(jMenuListDelete, hitlistHandler.getListName(hitlist), LIST_DELETE+hitlist);
			}
		}

	/**
	 * @param menu
	 * @param text
	 * @param actionCommand if null, then show menu item as disabled
	 */
	public void addMenuItem(JMenu menu, String text, String actionCommand) {
		addMenuItem(menu, text, actionCommand, null, 0, 0);
		}

	/**
	 * @param menu
	 * @param text
	 * @param actionCommand if null, then show menu item as disabled
	 * @param toolTipText may be null
	 */
	private void addMenuItem(JMenu menu, String text, String actionCommand, String toolTipText) {
		addMenuItem(menu, text, actionCommand, null, 0, 0);
	}

	/**
	 * @param menu
	 * @param text
	 * @param actionCommand if null, then show menu item as disabled
	 * @param toolTipText may be null
	 * @param keyCode != 0 if an accelerator key shall be associated
	 * @param modifiers MENU_MASK or Event.SHIFT_MASK | MENU_MASK
	 */
	private void addMenuItem(JMenu menu, String text, String actionCommand, String toolTipText, int keyCode, int modifiers) {
		JMenuItem item = new JMenuItem(text);
		if (actionCommand != null) {
			item.setActionCommand(actionCommand);
			item.addActionListener(this);
			if (toolTipText != null)
				item.setToolTipText(toolTipText);
			}
		else {
			item.setEnabled(false);
			}
		if (keyCode != 0)
			item.setAccelerator(KeyStroke.getKeyStroke(keyCode, modifiers));
		menu.add(item);
		}

	public void actionPerformed(ActionEvent e) {
		try {
			Object source = e.getSource();
			String actionCommand = e.getActionCommand();
			if (source == jMenuFilePageFormat)
				menuFilePageFormat();
			else if (source == jMenuFilePreview)
				menuFilePreview();
			else if (source == jMenuFilePrint)
				menuFilePrint();
			else if (source == jMenuFileQuit)
				new DETaskExitProgram(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuHelpAbout)
				new DEAboutDialog(mParentFrame);
			else if (source == jMenuHelpHelp)
				DEHelpFrame.showInstance(mParentFrame);
			else if (source == jMenuHelpShortcuts)
				FXHelpFrame.showResource("/html/help/shortcuts.html", getParentFrame());
			else if (source == jMenuHelpMoreData)
				BrowserControl.displayURL(MORE_DATA_URL);
			else if (source == jMenuHelpForum)
				BrowserControl.displayURL(FORUM_URL);
			else if (source == jMenuFileNew)
				new DETaskNewFile(mApplication).defineAndRun();
			else if (source == jMenuFileNewFromVisible)
				new DETaskNewFileFromVisible(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuFileNewFromSelection)
				new DETaskNewFileFromSelection(mParentFrame, mApplication).defineAndRun();
			else if (actionCommand.startsWith(NEW_FROM_LIST))
				new DETaskNewFileFromList(mParentFrame, mApplication, Integer.parseInt(actionCommand.substring(NEW_FROM_LIST.length()))).defineAndRun();
			else if (source == jMenuFileNewFromPivoting)
				new DETaskNewFileFromPivoting(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuFileNewFromReversePivoting)
				new DETaskNewFileFromReversePivoting(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuFileNewFromTransposition)
				new DETaskNewFileFromTransposition(mParentFrame, mApplication).defineAndRun();
			else if (actionCommand.startsWith(OPEN_FILE))    // these are the reference,sample,etc-files
				openFile(actionCommand.substring(OPEN_FILE.length()));
			else if (source == jMenuFileOpen)
				new DETaskOpenFile(mApplication).defineAndRun();
			else if (source == jMenuFileOpenTemplate)
				new DETaskApplyTemplateFromFile(mApplication).defineAndRun();
			else if (source == jMenuFileOpenMacro)
				new DETaskRunMacroFromFile(mApplication).defineAndRun();
			else if (source == jMenuFileOpenMDLReactions)
				new DETaskOpenMDLReactionDatabase(mApplication).defineAndRun();
			else if (source == jMenuFileMerge)
				new DETaskMergeFile(mParentFrame, false).defineAndRun();
			else if (source == jMenuFileAppend) {
				if (mParentFrame.getTableModel().getTotalRowCount() == 0)
					JOptionPane.showMessageDialog(mParentFrame, "You cannot append a file to an empty table. Use 'Open File...' instead.");
				else {
					File file = FileHelper.getFile(mParentFrame, "Append DataWarrior-, SD- or Text-File", FileHelper.cFileTypeDataWarriorCompatibleData);
					if (file != null)
						new DEFileLoader(mParentFrame, null).appendFile(file);
				}
			} else if (source == jMenuFileClose)
				new DETaskCloseWindow(mParentFrame, mApplication, mParentFrame).defineAndRun();
			else if (source == jMenuFileCloseAll)
				mApplication.closeAllFramesSafely(true);
			else if (source == jMenuFileSave) {
				if (mTableModel.getFile() == null)
					new DETaskSaveFileAs(mParentFrame).defineAndRun();
				else
					new DETaskSaveFile(mParentFrame).defineAndRun();
			} else if (source == jMenuFileSaveAs)
				new DETaskSaveFileAs(mParentFrame).defineAndRun();
			else if (source == jMenuFileSaveText)
				new DETaskSaveTextFileAs(mParentFrame).defineAndRun();
			else if (source == jMenuFileSaveSDF)
				new DETaskSaveSDFileAs(mParentFrame).defineAndRun();
			else if (source == jMenuFileSaveTemplate)
				new DETaskSaveTemplateFileAs(mParentFrame).defineAndRun();
			else if (source == jMenuFileSaveVisibleAs)
				new DETaskSaveVisibleRowsAs(mParentFrame).defineAndRun();
			else if (source == jMenuEditCopy)
				new DETaskCopyViewContent(mParentFrame).defineAndRun();
			else if (source == jMenuEditPaste)
				pasteDataOrMacro();
			else if (source == jMenuEditPasteWithHeader)
				new DETaskPaste(mParentFrame, mApplication, DETaskPaste.HEADER_WITH).defineAndRun();
			else if (source == jMenuEditPasteWithoutHeader)
				new DETaskPaste(mParentFrame, mApplication, DETaskPaste.HEADER_WITHOUT).defineAndRun();
			else if (source == jMenuEditPasteAppend)
				new DETaskPasteIntoTable(mParentFrame, DETaskPasteIntoTable.NO_COLUMN, DETaskPasteIntoTable.ROW_APPEND).defineAndRun();
			else if (source == jMenuEditPasteMerge)
				new DETaskMergeFile(mParentFrame, true).defineAndRun();
			else if (source == jMenuEditSelectAll)
				new DETaskSelectAll(mParentFrame).defineAndRun();
			else if (source == jMenuEditSelectRowsRandomly)
				new DETaskSelectRowsRandomly(mParentFrame, mTableModel).defineAndRun();
			else if (source == jMenuEditExtendSelection)
				new DETaskExtendRowSelection(mParentFrame, -1, -1).defineAndRun();
			else if (source == jMenuEditInvertSelection)
				new DETaskInvertSelection(mParentFrame).defineAndRun();
			else if (source == jMenuEditSearchAndReplace)
				new DETaskFindAndReplace(mParentFrame).defineAndRun();
			else if (source == jMenuEditNewFilter)
				new DETaskAddNewFilter(mParentFrame, mParentPane.getPruningPanel()).defineAndRun();
			else if (source == jMenuEditDisableFilters)
				new DETaskDisableAllFilters(mParentFrame).defineAndRun();
			else if (source == jMenuEditEnableFilters)
				new DETaskEnableAllFilters(mParentFrame).defineAndRun();
			else if (source == jMenuEditResetFilters) {
				if (JOptionPane.showConfirmDialog(mParentFrame,
						"Do you really want to clear all filter settings?",
						"Reset All Filters?",
						JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
					new DETaskResetAllFilters(mParentFrame).defineAndRun();
			}
			else if (source == jMenuEditRemoveFilters)
				new DETaskRemoveAllFilters(mParentFrame).defineAndRun();
			else if (source == jMenuDataRemoveColumns)
				new DETaskDeleteColumns(mParentFrame, mTableModel, null).defineAndRun();
			else if (source == jMenuDataRemoveSelected)
				new DETaskDeleteSelectedRows(mParentFrame).defineAndRun();
			else if (source == jMenuDataRemoveInvisible)
				new DETaskDeleteInvisibleRows(mParentFrame).defineAndRun();
			else if (source == jMenuDataRemoveDuplicate)
				new DETaskDeleteDuplicateRows(mParentFrame, DETaskDeleteDuplicateRows.MODE_REMOVE_DUPLICATE).defineAndRun();
			else if (source == jMenuDataRemoveUnique)
				new DETaskDeleteDuplicateRows(mParentFrame, DETaskDeleteDuplicateRows.MODE_REMOVE_UNIQUE).defineAndRun();
			else if (source == jMenuDataMergeColumns)
				new DETaskMergeColumns(mParentFrame).defineAndRun();
			else if (source == jMenuDataMergeDuplicate)
				new DETaskDeleteDuplicateRows(mParentFrame, DETaskDeleteDuplicateRows.MODE_MERGE_EQUIVALENT).defineAndRun();
			else if (source == jMenuDataSplitRows)
				new DETaskSplitRows(mParentFrame, mTableModel).defineAndRun();
			else if (source == jMenuDataAddRowNumbers)
				new DETaskAddRowNumbers(mParentFrame).defineAndRun();
			else if (source == jMenuDataAddEmptyColumns)
				new DETaskAddEmptyColumns(mApplication).defineAndRun();
			else if (source == jMenuDataAddEmptyRows)
				new DETaskAddEmptyRows(mParentFrame).defineAndRun();
			else if (source == jMenuDataAddBinnedColumn)
				new DETaskAddBinsFromNumbers(mParentFrame).defineAndRun();
			else if (source == jMenuDataAddFuzzyScore)
				new DETaskCalculateFuzzyScore(mParentFrame).defineAndRun();
			else if (source == jMenuChemAssessPredictionQuality)
				new DETaskAssessPredictionQuality(mParentFrame).defineAndRun();
			else if (source == jMenuChemPredictMissingValues)
				new DETaskPredictMissingValues(mParentFrame).defineAndRun();
			else if (source == jMenuDataAddCalculatedValues)
				new DETaskAddCalculatedValues(mParentFrame, mTableModel, -1, null, true).defineAndRun();
			else if (source == jMenuDataAddPrincipalComponents)
				new DETaskPCA(mParentFrame, true).defineAndRun();
			else if (source == jMenuDataCreateTSNE)
				new DETaskCreateTSNEVisualization(mParentFrame, true).defineAndRun();
			else if (source == jMenuDataCreateUMAP)
				new DETaskCreateUMAPVisualization(mParentFrame, true).defineAndRun();
			else if (source == jMenuDataSOMCreate)
				new DETaskCalculateSOM(mParentFrame, true).defineAndRun();
			else if (source == jMenuDataSOMApply)
				new DETaskApplySOMFile(mApplication).defineAndRun();
			else if (source == jMenuDataSOMAnalyse)
				new DETaskAnalyseSOMFile(mApplication).defineAndRun();
			else if (source == jMenuDataGiniScore)
				new DETaskCalculateSelectivityScore(mParentFrame, mTableModel).defineAndRun();
			else if (source == jMenuDataArrangeGraph)
				new DETaskArrangeGraphNodes(mParentFrame).defineAndRun();
			else if (source == jMenuDataCorrelationMatrix)
				new DECorrelationDialog(mParentFrame, mTableModel);
			else if (source == jMenuChemCCLibrary)
				new DETaskEnumerateCombinatorialLibrary(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuChemEALibrary)
				new DETaskBuildEvolutionaryLibrary(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuChemGenerateRandomMolecules)
				new DETaskGenerateRandomMolecules(mParentFrame).defineAndRun();
			else if (source == jMenuChemEnumerateMarkush)
				new DEMarkushDialog(mParentFrame, mApplication);
			else if (source == jMenuChemAddProperties)
				new DETaskCalculateChemicalProperties(mParentFrame).defineAndRun();
			else if (source == jMenuChemAddFormula)
				new DETaskAddFormula(mParentFrame).defineAndRun();
			else if (source == jMenuChemAddSmiles)
				new DETaskAddSmiles(mParentFrame).defineAndRun();
			else if (source == jMenuChemAddInchi)
				new DETaskAddStandardInchi(mParentFrame).defineAndRun();
			else if (source == jMenuChemAddInchiKey)
				new DETaskAddInchiKey(mParentFrame).defineAndRun();
			else if (source == jMenuChemAddCanonicalCode)
				new DETaskAddCanonicalCode(mParentFrame).defineAndRun();
			else if (source == jMenuChemAddSubstructureCount)
				new DETaskAddSubstructureCount(mParentFrame).defineAndRun();
			else if (source == jMenuChemExtractFragment)
				new DETaskExtractFragment(mParentFrame).defineAndRun();
			else if (source == jMenuChemAddStructureFromName)
				new DETaskAddStructureFromName(mParentFrame).defineAndRun();
			else if (source == jMenuChemExtractReactants)
				new DETaskExtractReactants(mParentFrame).defineAndRun();
			else if (source == jMenuChemExtractCatalysts)
				new DETaskExtractCatalysts(mParentFrame).defineAndRun();
			else if (source == jMenuChemExtractProducts)
				new DETaskExtractProducts(mParentFrame).defineAndRun();
			else if (source == jMenuChemExtractTransformation)
				new DETaskExtractTransformation(mParentFrame).defineAndRun();
			else if (source == jMenuChemCreate2DCoords)
				new DETaskAdd2DCoordinates(mParentFrame).defineAndRun();
			else if (source == jMenuChemCreate3DCoords)
				new DETaskAdd3DCoordinates(mParentFrame).defineAndRun();
			else if (source == jMenuChemSuperpose)
				new DETaskSuperposeConformers(mParentFrame).defineAndRun();
			else if (source == jMenuChemDock)
				new DETaskDockIntoProteinCavity(mParentFrame).defineAndRun();
			else if (source == jMenuChemExtract3DFragments)
				new DETaskBuild3DFragmentLibrary(mParentFrame).defineAndRun();
			else if (source == jMenuChemDecomposeRGroups)
				new DETaskDecomposeRGroups(mParentFrame).defineAndRun();
			else if (source == jMenuChemInteractiveSARTable) {
				JOptionPane.showMessageDialog(mParentFrame, "Nobody ever asked to complete this.\nTherefore, this functionality is not final yet.\nSuggestions and sample data are welcome.");
				int idcodeColumn = getStructureColumn(true);
				if (idcodeColumn != -1)
					new DEInteractiveSARDialog(mParentFrame, mTableModel, idcodeColumn);
			} else if (source == jMenuChemAnalyzeScaffolds)
				new DETaskAnalyseScaffolds(mParentFrame).defineAndRun();
			else if (source == jMenuChemAnalyzeCliffs)
				new DETaskAnalyseActivityCliffs(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuChemMatchFile)
				new DETaskFindSimilarCompoundsInFile(mParentFrame).defineAndRun();
			else if (source == jMenuChemSelectDiverse)
				new DETaskSelectDiverse(mParentFrame).defineAndRun();
			else if (source == jMenuChemCluster)
				new DETaskClusterCompounds(mParentFrame).defineAndRun();
/*		  else if (source == jMenuChemExtractPKATree)
				new PKATreeExtractor(mParentFrame, new PKADataWarriorAdapter(mTableModel)).extract();	*/
			else if (source == jMenuChemCheckIDCodes)
				new DETaskValidateIDCodes(mParentFrame, CompoundTableConstants.cColumnTypeIDCode).defineAndRun();
			else if (source == jMenuChemMapReactions)
				new DETaskMapReactions(getParentFrame()).defineAndRun();
			else if (source == jMenuChemCompareReactionMapping)
				new DETaskCompareReactionMapping(getParentFrame()).defineAndRun();
			else if (source == jMenuChemCompareDescriptorSimilarityDistribution)
				new DETestCompareDescriptorSimilarityDistribution(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuChemExtractPairwiseCompoundSimilarities)
				new DETestExtractPairwiseCompoundSimilarities(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuChemExtractPairwiseStuff)
				new DETestExtractPairwiseStuff(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuChemCountAtomTypes)
				new DETestCountAtomTypes(mParentFrame).defineAndRun();
			else if (source == jMenuChemRunSurfacePLS)
				new DETestRunSurfacePLS(mParentFrame).defineAndRun();
			else if (source == jMenuChemClassifyReactions)
				new DETaskClassifyReactions(mParentFrame).defineAndRun();
/*			else if (source == jMenuChemPredictPKa) {
				int idcodeColumn = getStructureColumn(true);
				if (idcodeColumn != -1)
					new UndocumentedStuff(mParentFrame, mMainPane, mTableModel, idcodeColumn).predictPKaValues();
				}*/
			else if (source == jMenuChemCreateGenericTautomers)
				new DETaskCreateGenericTautomer(mParentFrame).defineAndRun();
			else if (source == jMenuDBWikipedia)
				new DETaskRetrieveWikipediaCompounds(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuDBGooglePatents)
				new DETaskSearchGooglePatents(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuDBPatentReactions)
				new DETaskSearchPatentReactions(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuDBReadChEMBL)
				new DETaskChemblQuery(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuDBFindChEMBLActives)
				new DETaskFindSimilarActiveCompounds(mParentFrame).defineAndRun();
			else if (source == jMenuDBSearchCOD)
				new DETaskCODQuery(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuDBSearchBuildingBlocks)
				new DETaskBuildingBlockQuery(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuDBSearchChemSpace)
				new DETaskChemSpaceQuery(mParentFrame, mApplication).defineAndRun();
//			else if (source == jMenuDBSearchMolport)
//				new DETaskMolportQuery(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuDBRetrieveDataFromURL)
				new DETaskRetrieveDataFromURL(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuDBRetrieveSQLData)
				new DETaskSQLQuery(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuListCreateSelected) {
				if (checkAndAllowEmptyList(DETaskNewRowList.MODE_SELECTED))
					new DETaskNewRowList(mParentFrame, DETaskNewRowList.MODE_SELECTED).defineAndRun();
				}
			else if (source == jMenuListCreateVisible) {
				if (checkAndAllowEmptyList(DETaskNewRowList.MODE_VISIBLE))
					new DETaskNewRowList(mParentFrame, DETaskNewRowList.MODE_VISIBLE).defineAndRun();
				}
			else if (source == jMenuListCreateHidden) {
				if (checkAndAllowEmptyList(DETaskNewRowList.MODE_HIDDEN))
					new DETaskNewRowList(mParentFrame, DETaskNewRowList.MODE_HIDDEN).defineAndRun();
				}
			else if (source == jMenuListCreateDuplicate)
				new DETaskNewRowList(mParentFrame, DETaskNewRowList.MODE_DUPLICATE).defineAndRun();
			else if (source == jMenuListCreateUnique)
				new DETaskNewRowList(mParentFrame, DETaskNewRowList.MODE_UNIQUE).defineAndRun();
			else if (source == jMenuListCreateDistinct)
				new DETaskNewRowList(mParentFrame, DETaskNewRowList.MODE_DISTINCT).defineAndRun();
			else if (source == jMenuListCreateClipboard)
				new DETaskNewRowList(mParentFrame, DETaskNewRowList.MODE_CLIPBOARD).defineAndRun();
			else if (source == jMenuListCreateMerge)
				new DETaskCombineTwoRowLists(mParentFrame).defineAndRun();
			else if (actionCommand.startsWith(LIST_ADD))
				new DETaskAddSelectionToList(mParentFrame, Integer.parseInt(actionCommand.substring(LIST_ADD.length()))).defineAndRun();
			else if (actionCommand.startsWith(LIST_REMOVE))
				new DETaskRemoveSelectionFromList(mParentFrame, Integer.parseInt(actionCommand.substring(LIST_REMOVE.length()))).defineAndRun();
			else if (actionCommand.startsWith(LIST_SELECT))
				new DETaskSelectRowsFromList(mParentFrame, Integer.parseInt(actionCommand.substring(LIST_SELECT.length()))).defineAndRun();
			else if (actionCommand.startsWith(LIST_DESELECT))
				new DETaskDeselectRowsFromList(mParentFrame, Integer.parseInt(actionCommand.substring(LIST_DESELECT.length()))).defineAndRun();
			else if (actionCommand.startsWith(LIST_DELETE))
				new DETaskDeleteRowList(mParentFrame, Integer.parseInt(actionCommand.substring(LIST_DELETE.length()))).defineAndRun();
			else if (source == jMenuListDeleteAll) {
				String[] names = mTableModel.getListHandler().getListNames();
				if (names == null) {
					JOptionPane.showMessageDialog(mParentFrame, "There are no row lists to be removed.");
					}
				else {
					int doDelete = JOptionPane.showConfirmDialog(this,
									"Do you really want to delete all row lists?",
									"Delete All Row Lists?",
									JOptionPane.OK_CANCEL_OPTION);
					if (doDelete == JOptionPane.OK_OPTION)
						new DETaskDeleteAllRowLists(mParentFrame).defineAndRun();
					}
				}
			else if (source == jMenuListNewColumn)
				new DETaskNewColumnWithListNames(mParentFrame).defineAndRun();
			else if (source == jMenuListListsFromColumn)
				new DETaskCreateListsFromCategories(mParentFrame).defineAndRun();
			else if (source == jMenuListImport)
				new DETaskImportHitlist(mApplication).defineAndRun();
			else if (source == jMenuListExport)
				new DETaskExportHitlist(mParentFrame, true).defineAndRun();
			else if (source == jMenuMacroImport)
				new DETaskImportMacro(mApplication).defineAndRun();
			else if (source == jMenuMacroPaste)
				new DETaskPasteMacro(mApplication).defineAndRun();
			else if (e.getActionCommand().startsWith(EXPORT_MACRO)) {
				String macroName = e.getActionCommand().substring(EXPORT_MACRO.length());
				new DETaskExportMacro(mParentFrame, macroName).defineAndRun();
				}
			else if (e.getActionCommand().startsWith(COPY_MACRO)) {
				String macroName = e.getActionCommand().substring(COPY_MACRO.length());
				new DETaskCopyMacro(mParentFrame, macroName).defineAndRun();
				}
			else if (e.getActionCommand().startsWith(RUN_GLOBAL_MACRO)) {
				runMacro(e.getActionCommand().substring(RUN_GLOBAL_MACRO.length()));
				}
			else if (e.getActionCommand().startsWith(RUN_INTERNAL_MACRO)) {
				new DETaskRunMacro(mParentFrame, e.getActionCommand().substring(RUN_INTERNAL_MACRO.length())).defineAndRun();
				}
			else if (source == jMenuMacroStartRecording)
				new DEDialogRecordMacro(mParentFrame);
			else if (source == jMenuMacroContinueRecording) {
				DEMacroRecorder.getInstance().continueRecording();
				enableMacroItems();
				}
			else if (source == jMenuMacroStopRecording) {
				DEMacroRecorder.getInstance().stopRecording();
				enableMacroItems();
				}
			else if (actionCommand.startsWith(SHOW_NEWS)) {
				DENews news = mNewsMap.get(actionCommand.substring(SHOW_NEWS.length()));
				if (news != null)
					news.show();
				}
			else if (actionCommand.startsWith(LOOK_AND_FEEL)) {
				for (int i=0; i<jMenuHelpLaF.getItemCount(); i++) {
					JCheckBoxMenuItem item = (JCheckBoxMenuItem)jMenuHelpLaF.getItem(i);
					if (item != source)
						item.setSelected(false);
					}
				String displayName = actionCommand.substring(LOOK_AND_FEEL.length());
				mApplication.updateLookAndFeel(mApplication.getLookAndFeel(displayName));
				DEHelpFrame.updateLookAndFeel();
				FXHelpFrame.updateLookAndFeel();
				}
			else if (actionCommand.startsWith(UPDATE)) {
				for (int i=0; i<jMenuHelpUpdate.getItemCount(); i++) {
					JCheckBoxMenuItem item = (JCheckBoxMenuItem)jMenuHelpUpdate.getItem(i);
					if (item != source)
						item.setSelected(false);
					}
				int mode = AbstractTask.findListIndex(actionCommand.substring(UPDATE.length()), DEUpdateHandler.PREFERENCES_UPDATE_MODE_CODE, 0);
				Preferences prefs = DataWarrior.getPreferences();
				prefs.put(DEUpdateHandler.PREFERENCES_KEY_UPDATE_MODE, DEUpdateHandler.PREFERENCES_UPDATE_MODE_CODE[mode]);
				}
			else if (actionCommand.startsWith(SET_RANGE)) {
				int column = mTableModel.findColumn(actionCommand.substring(SET_RANGE.length()));
				new DETaskSetValueRange(mParentFrame, column).defineAndRun();
				}
			else if (!isDescriptorCreation(actionCommand)) {
				JOptionPane.showMessageDialog(mParentFrame, "This option is not supported yet.");
				}
			}
		catch (OutOfMemoryError ex) {
			JOptionPane.showMessageDialog(mParentFrame, ex);
			}
		}

	private void pasteDataOrMacro() {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor))
			return;

		try {
			String s = (String)clipboard.getData(DataFlavor.stringFlavor);
			if (s.startsWith(DEMacro.MACRO_START))
				new DETaskPasteMacro(mApplication).defineAndRun();
			else
				new DETaskPaste(mParentFrame, mApplication, DETaskPaste.HEADER_ANALYZE).defineAndRun();
			}
		catch (Exception e) {}
		}

	private boolean isDescriptorCreation(String actionCommand) {
		if (DescriptorHelper.isDescriptorShortName(actionCommand)) {
			new DETaskCalculateDescriptor(mParentFrame, actionCommand, null).defineAndRun();
			return true;
			}
		int index = actionCommand.indexOf("_");
		if (index != -1) {
			new DETaskCalculateDescriptor(mParentFrame, actionCommand.substring(0, index), actionCommand.substring(index+1)).defineAndRun();
			return true;
			}

		return false;
		}

	private boolean checkAndAllowEmptyList(int listMode) {
		if (mTableModel.getTotalRowCount() == 0) {
			JOptionPane.showMessageDialog(mParentFrame, "You cannot create a row list without any data.");
			return false;
			}

		boolean isEmpty = (listMode == DETaskNewRowList.MODE_SELECTED) ?
				mMainPane.getTable().getSelectionModel().isSelectionEmpty()
						: (listMode == DETaskNewRowList.MODE_VISIBLE) ?
				(mTableModel.getRowCount() == 0)
						: (listMode == DETaskNewRowList.MODE_HIDDEN) ?
				(mTableModel.getRowCount() == mTableModel.getTotalRowCount())
						: false;	// should not happen

		if (!isEmpty)
			return true;

		String message = (listMode == DETaskNewRowList.MODE_SELECTED) ?
				"The selection is empty."
						: (listMode == DETaskNewRowList.MODE_VISIBLE) ?
				"There are no visible rows."
						:
				"There are now hidden rows.";

		int doDelete = JOptionPane.showConfirmDialog(mParentFrame,
						message+"\nDo you really want to create an empty list?",
						"Create Empty List?",
						JOptionPane.OK_CANCEL_OPTION);
		return (doDelete == JOptionPane.OK_OPTION);
		}

	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof JCheckBoxMenuItem) {
			JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem)e.getSource();
			int[] columnList = new int[1];
			columnList[0] = mTableModel.findColumn(menuItem.getText());
			new DETaskSetLogarithmicMode(mParentFrame, mTableModel, columnList, menuItem.isSelected()).defineAndRun();
			}
		}

	protected int getStructureColumn(boolean requireFingerprint) {
		int idcodeColumn = -1;

		int[] idcodeColumnList = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode);
		if (idcodeColumnList == null) {
			JOptionPane.showMessageDialog(mParentFrame, "None of your columns contain chemical structures.");
			}
		else if (idcodeColumnList.length == 1) {
			idcodeColumn = idcodeColumnList[0];
			}
		else {
			String[] columnNameList = new String[idcodeColumnList.length];
			for (int i=0; i<idcodeColumnList.length; i++)
				columnNameList[i] = mTableModel.getColumnTitle(idcodeColumnList[i]);

			String columnName = (String)JOptionPane.showInputDialog(mParentFrame,
								"Please select a column with chemical structures!",
								"Select Structure Column",
								JOptionPane.QUESTION_MESSAGE,
								null,
								columnNameList,
								columnNameList[0]);
			idcodeColumn = mTableModel.findColumn(columnName);
			}

		if (idcodeColumn != -1 && requireFingerprint) {
			int fingerprintColumn = mTableModel.getChildColumn(idcodeColumn, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
			if (fingerprintColumn == -1) {
				JOptionPane.showMessageDialog(mParentFrame, "Please create first a chemical fingerprint for the selected structure column.");
				idcodeColumn = -1;
				}
			if (!mTableModel.isDescriptorAvailable(fingerprintColumn)) {
				JOptionPane.showMessageDialog(mParentFrame, "Please wait until the chemical fingerprint creation is completed.");
				idcodeColumn = -1;
				}
			}

		return idcodeColumn;
		}

	private int getReactionColumn() {
		int reactionColumn = -1;

		int[] reactionColumnList = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeRXNCode);
		if (reactionColumnList == null) {
			JOptionPane.showMessageDialog(mParentFrame, "None of your columns contains chemical reaction.");
			}
		else if (reactionColumnList.length == 1) {
			reactionColumn = reactionColumnList[0];
			}
		else {
			String[] columnNameList = new String[reactionColumnList.length];
			for (int i=0; i<reactionColumnList.length; i++)
				columnNameList[i] = mTableModel.getColumnTitle(reactionColumnList[i]);

			String columnName = (String)JOptionPane.showInputDialog(mParentFrame,
								"Please select a column with chemical reactions!",
								"Select Reaction Column",
								JOptionPane.QUESTION_MESSAGE,
								null,
								columnNameList,
								columnNameList[0]);
			reactionColumn = mTableModel.findColumn(columnName);
			}

		return reactionColumn;
		}

	public void updateRecentFileMenu() {
		jMenuFileOpenRecent.removeAll();
		try {
			Preferences prefs = DataWarrior.getPreferences();

			for (int i=1; i<=MAX_RECENT_FILE_COUNT; i++) {
				String path = prefs.get(PREFERENCES_KEY_RECENT_FILE + i, "");
				if (path == null || path.length() == 0)
					break;

				File file = new File(path);
				if (FileHelper.fileExists(file, 200))
					addMenuItem(jMenuFileOpenRecent, file.getName(), OPEN_FILE+path);
				}

			if (jMenuFileOpenRecent.getItemCount() == 0)
				addMenuItem(jMenuFileOpenRecent, "<no recent files>", null);
			}
		catch (Exception e) {
			e.printStackTrace();
			}
		}

	private void addResourceFileMenus(JMenu parentMenu) {
		// alternative to get location of datawarrior.jar:
		//   getClass().getProtectionDomain().getCodeSource().getLocation();

		parentMenu.addSeparator();
		addIdorsiaResourceFileMenus(parentMenu);
		for (String resDir:DataWarrior.RESOURCE_DIR) {
			File directory = mApplication.resolveResourcePath(resDir);
			if (directory != null)
				addResourceFileMenu(parentMenu, "Open "+resDir+" File", DataWarrior.makePathVariable(resDir), directory);
			}

		String dirlist = System.getProperty("datapath");
		if (dirlist != null)
			parentMenu.addSeparator();

		while (dirlist != null) {
			int index = dirlist.indexOf(File.pathSeparatorChar);
			String dirname = (index == -1) ? dirlist : dirlist.substring(0, index);
			dirlist = (index == -1) ? null : dirlist.substring(index+1);
			File directory = new File(DataWarrior.resolveOSPathVariables(dirname));
			if (directory.exists())
				addResourceFileMenu(parentMenu, "Open User File <"+directory.getName()+">", dirname, directory);
			}
		}

	/**
	 * @param parentMenu
	 * @param itemString
	 * @param dirPath should be based on a path variable if it refers to a standard resource file
	 * @param directory
	 */
	private void addResourceFileMenu(JMenu parentMenu, String itemString, String dirPath, File directory) {
		File[] file = directory.listFiles((File f) -> {
			if (f.isDirectory())
				return false;
			return (f.getName().toLowerCase().endsWith(".dwar"));
			} );
		if (file != null && file.length != 0) {
			JMenu menu = new JScrollableMenu(itemString);
			Arrays.sort(file, (File o1, File o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
			for (int i=0; i<file.length; i++)
				addMenuItem(menu, file[i].getName(), OPEN_FILE+dirPath+File.separator+file[i].getName());

			parentMenu.add(menu);
			}
		}

	private void addResourceFileMenusLater(JMenu parentMenu) {
		// alternative to get location of datawarrior.jar:
		//   getClass().getProtectionDomain().getCodeSource().getLocation();

		parentMenu.addSeparator();
		new Thread(() -> {
			for (String resDir:DataWarrior.RESOURCE_DIR) {
				File directory = mApplication.resolveResourcePath(resDir);
				if (directory != null)
					SwingUtilities.invokeLater(() ->
						addResourceFileMenuLater(parentMenu, "Open "+resDir+" File", DataWarrior.makePathVariable(resDir), directory));
				}

			String dirlist = System.getProperty("datapath");
			if (dirlist != null)
				SwingUtilities.invokeLater(() -> parentMenu.addSeparator() );

			while (dirlist != null) {
				int index = dirlist.indexOf(File.pathSeparatorChar);
				String dirname = (index == -1) ? dirlist : dirlist.substring(0, index);
				dirlist = (index == -1) ? null : dirlist.substring(index+1);
				File directory = new File(DataWarrior.resolveOSPathVariables(dirname));
				if (FileHelper.fileExists(directory))
					SwingUtilities.invokeLater(() ->
						addResourceFileMenu(parentMenu, "Open User File <"+directory.getName()+">", dirname, directory) );
				}
			} ).start();
		}

	/**
	 * @param parentMenu
	 * @param itemString
	 * @param dirPath should be based on a path variable if it refers to a standard resource file
	 * @param directory
	 */
	private void addResourceFileMenuLater(JMenu parentMenu, String itemString, String dirPath, File directory) {
		File[] file = directory.listFiles((File f) -> {
			if (f.isDirectory())
				return false;
			return (f.getName().toLowerCase().endsWith(".dwar"));
		} );
		if (file != null && file.length != 0) {
			JMenu menu = new JScrollableMenu(itemString);
			Arrays.sort(file, (File o1, File o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
			for (int i=0; i<file.length; i++)
				addMenuItem(menu, file[i].getName(), OPEN_FILE+dirPath+File.separator+file[i].getName());

			parentMenu.add(menu);
		}
	}

/*	private void addPluginItems(JMenu parentMenu) {
//		addPluginItems(parentMenu, parentMenu.getText());
//	}

	private void addPluginItems(JMenu parentMenu, String parentMenuName) {
		ArrayList<PluginTaskSpec> pluginTasks = mApplication.getPluginRegistry().getPluginTasks();
		if (pluginTasks == null || pluginTasks.size() == 0)
			return;

		boolean isSeparated = (parentMenu.getItemCount() == 0);

		for (final PluginTaskSpec pluginTask:pluginTasks) {
			String targetMenuName = pluginTask.getMenuName() == null ? DEFAULT_PLUGIN_MENU : pluginTask.getMenuName();
			if (parentMenuName.equalsIgnoreCase(targetMenuName)) {
				if (!isSeparated) {
					parentMenu.addSeparator();
					isSeparated = true;
					}
				JMenuItem item = new JMenuItem(pluginTask.getMenuItemName() + "...");
				item.addActionListener(e -> new DETaskPluginTask(mParentFrame, pluginTask.getTask()).defineAndRun());
				parentMenu.add(item);
				pluginTask.setMenuExists(true);
				}
			}
		}*/

// File access via VPN at Idorsia is so slow that we have to involve independent threads for building the macro menu tree
/*	private void addMacroFileItems(JMenu parentMenu) {
		File directory = mApplication.resolveResourcePath(DataWarrior.MACRO_DIR);
		if (directory != null)
			addMacroFileItems(parentMenu, DataWarrior.makePathVariable(DataWarrior.MACRO_DIR), directory);

		directory = new File(System.getProperty("user.home")+File.separator+"datawarrior"+File.separator+"macro");
		if (directory != null)
			addMacroFileItems(parentMenu, USER_MACRO_DIR, directory);

		if (Platform.isWindows()) {
			String userMacroPath = "C:\\Users\\".concat(System.getProperty("user.name")).concat("\\AppData\\Roaming\\DataWarrior\\Macro");
			directory = new File(userMacroPath);
			if (FileHelper.fileExists(directory))
				addMacroFileItems(parentMenu, userMacroPath, directory);
			}

		String dirlist = System.getProperty("macropath");
		while (dirlist != null) {
			int index = dirlist.indexOf(File.pathSeparatorChar);
			String dirname = (index == -1) ? dirlist : dirlist.substring(0, index);
			dirlist = (index == -1) ? null : dirlist.substring(index+1);
			directory = new File(DataWarrior.resolveOSPathVariables(dirname));
			if (FileHelper.fileExists(directory))
				addMacroFileItems(parentMenu, dirname, directory);
			}

		if (parentMenu.getItemCount() == 0) {
			JMenuItem item = new JMenuItem("<no macros defined>");
			item.setEnabled(false);
			parentMenu.add(item);
			}
		}

	/**
	 * @param parentMenu
	 * @param dirPath should be based on a path variable if it refers to a standard resource file
	 * @param parentDir
	 *
	private void addMacroFileItems(JMenu parentMenu, String dirPath, File parentDir) {
		File[] dirs = parentDir.listFiles(file -> file.isDirectory() && !file.getName().startsWith(".") );
		if (dirs != null && dirs.length != 0) {
			if (parentMenu.getItemCount() != 0)
				parentMenu.addSeparator();
			Arrays.sort(dirs);
			for (File dir:dirs) {
				JMenu subMenu = new JMenu(dir.getName());
				parentMenu.add(subMenu);
				addMacroFileItems(subMenu, dir.getPath(), dir);
				}
			}

		File[] files = parentDir.listFiles(f -> {
			if (f.isDirectory())
				return false;
			return (f.getName().toLowerCase().endsWith(".dwam"));
			} );
		if (files != null && files.length != 0) {
			if (parentMenu.getItemCount() != 0)
				parentMenu.addSeparator();
			Arrays.sort(files);
			for (File file:files) {
				try {
					DEMacro macro = new DEMacro(file, null);
					addMenuItem(parentMenu, macro.getName(), RUN_GLOBAL_MACRO + dirPath + File.separator + file.getName(), macro.getDescription());
					}
				catch (IOException ioe) {}
				}
			}
		}*/

	private void addMacroFileItemsLater(JMenu parentMenu) {
		mMacroItemCountMap = new TreeMap<>();
		new Thread(() -> {
			File directory = mApplication.resolveResourcePath(DataWarrior.MACRO_DIR);
			if (directory != null && directory.exists()) {
				SwingUtilities.invokeLater(() -> { if (parentMenu.getItemCount() != 0) parentMenu.addSeparator(); } );
				addMacroFileItemsLater(parentMenu, DataWarrior.makePathVariable(DataWarrior.MACRO_DIR), directory, 0);
				}

			directory = new File(System.getProperty("user.home")+File.separator+"datawarrior"+File.separator+"macro");
			if (directory != null && directory.exists()) {
				SwingUtilities.invokeLater(() -> { if (parentMenu.getItemCount() != 0) parentMenu.addSeparator(); } );
				addMacroFileItemsLater(parentMenu, USER_MACRO_DIR, directory, Event.SHIFT_MASK);
				}

			if (Platform.isWindows()) {
				String userMacroPath = "C:\\Users\\".concat(System.getProperty("user.name")).concat("\\AppData\\Roaming\\DataWarrior\\Macro");
				directory = new File(userMacroPath);
				if (FileHelper.fileExists(directory)) {
					SwingUtilities.invokeLater(() -> { if (parentMenu.getItemCount() != 0) parentMenu.addSeparator(); } );
					addMacroFileItemsLater(parentMenu, userMacroPath, directory, Event.SHIFT_MASK);
					}
				}

			String dirlist = System.getProperty("macropath");
			while (dirlist != null) {
				int index = dirlist.indexOf(File.pathSeparatorChar);
				String dirname = (index == -1) ? dirlist : dirlist.substring(0, index);
				dirlist = (index == -1) ? null : dirlist.substring(index+1);
				directory = new File(DataWarrior.resolveOSPathVariables(dirname));
				if (FileHelper.fileExists(directory)) {
					SwingUtilities.invokeLater(() -> { if (parentMenu.getItemCount() != 0) parentMenu.addSeparator(); } );
					addMacroFileItemsLater(parentMenu, dirname, directory, Event.ALT_MASK);
					}
				}

			SwingUtilities.invokeLater(() -> {
				if (parentMenu.getItemCount() == 0) {
					JMenuItem item = new JMenuItem("<no macros defined>");
					item.setEnabled(false);
					parentMenu.add(item);
					}
				} );
			} ).start();
		}

	/**
	 * @param parentMenu
	 * @param dirPath should be based on a path variable if it refers to a standard resource file
	 * @param parentDir
	 */
	private void addMacroFileItemsLater(JMenu parentMenu, String dirPath, File parentDir, int secondModifier) {
		File[] dirs = parentDir.listFiles(file -> file.isDirectory() && !file.getName().startsWith(".") );
		if (dirs != null && dirs.length != 0) {
			Arrays.sort(dirs);
			SwingUtilities.invokeLater(() -> {
				for (File dir:dirs) {
					JMenu subMenu = new JMenu(dir.getName());
					parentMenu.add(subMenu);
					new Thread(() -> addMacroFileItemsLater(subMenu, dir.getPath(), dir, secondModifier)).start();
					}
				} );
			}

		File[] files = parentDir.listFiles(f -> {
			if (f.isDirectory())
				return false;
			return (f.getName().toLowerCase().endsWith(".dwam"));
			} );
		if (files != null && files.length != 0) {
			Arrays.sort(files);
			for (File file:files) {
				try {
					DEMacro macro = new DEMacro(file, null);
					SwingUtilities.invokeLater(() -> {
						int[] countHolder = mMacroItemCountMap.get(secondModifier);
						if (countHolder == null) {
							countHolder = new int[1];
							countHolder[0] = 1; // we start with 2 because Ctrl-1 is 'Add selection to default list'
							mMacroItemCountMap.put(secondModifier, countHolder);
						}
						countHolder[0]++;
						int keyCode = (countHolder[0] < 10) ? '0' + countHolder[0] : 0;
						addMenuItem(parentMenu, macro.getName(),
								RUN_GLOBAL_MACRO + dirPath + File.separator + file.getName(),
								macro.getDescription(), keyCode, MENU_MASK | secondModifier);
						} );
					}
				catch (IOException ioe) {}
				}
			}
		}

	public void enableMacroItems() {
		boolean isRecording = DEMacroRecorder.getInstance().isRecording();
		jMenuMacroStartRecording.setEnabled(!isRecording);
		jMenuMacroStopRecording.setEnabled(isRecording);
		jMenuMacroContinueRecording.setEnabled(DEMacroRecorder.getInstance().canContinueRecording(mParentFrame));
		}

	public void openFile(String filePath) {
		new DETaskOpenFile(mApplication, filePath).defineAndRun();
		}

	public void runMacro(String filePath) {
		new DETaskRunMacroFromFile(mApplication, filePath).defineAndRun();
		}
	}

class MyCheckBoxMenuItem extends JCheckBoxMenuItem {
	private static final long CLOSING_DELAY = 2000;
	private static long sMostRecentItemChange;
	private static Thread sClosingThread;

	public MyCheckBoxMenuItem(String title, boolean isSelected) {
		super(title, isSelected);
		}

	@Override
	protected void processMouseEvent(MouseEvent evt) {
		if (evt.getID() == MouseEvent.MOUSE_RELEASED && contains(evt.getPoint())) {
			doClick();
			setArmed(true);
			sMostRecentItemChange = System.currentTimeMillis();
			if (sClosingThread == null) {
				sClosingThread = new Thread(() -> {
					do {
						try { Thread.sleep(500); } catch (InterruptedException ie) {}
						} while (System.currentTimeMillis() - sMostRecentItemChange < CLOSING_DELAY);
					getParent().setVisible(false);
					MenuSelectionManager.defaultManager().clearSelectedPath();
					sClosingThread = null;
					} );
				sClosingThread.start();
				}
			}
		else {
			super.processMouseEvent(evt);
			}
		}
	}