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

import com.actelion.research.calc.CorrelationCalculator;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.datawarrior.task.view.DETaskSetStructureDisplayMode;
import com.actelion.research.gui.JMultiPanelView;
import com.actelion.research.gui.JPruningBar;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.MarkerLabelDisplayer;
import com.actelion.research.table.RuntimeProperties;
import com.actelion.research.table.filter.JFilterPanel;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.*;
import com.actelion.research.table.view.config.CardsViewConfiguration;
import com.actelion.research.table.view.config.ViewConfiguration;
import com.actelion.research.util.DoubleFormat;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

import static com.actelion.research.table.MarkerLabelConstants.cOnePerCategoryMode;
import static com.actelion.research.table.view.JVisualization.DEFAULT_LABEL_TRANSPARENCY;
import static com.actelion.research.table.view.JVisualization.cColumnUnassigned;

public class DERuntimeProperties extends RuntimeProperties {
	private static final long serialVersionUID = 0x20061101;

	private static final String cRowTaskCount = "rowTaskCount";
	private static final String cRowTaskCode = "rowTaskCode";
	private static final String cRowTaskMenu = "rowTaskMenu";
	private static final String cRowTaskItem = "rowTaskItem";
	private static final String cRowTaskConfig = "rowTaskConfig";

	private static final String cViewTypeTable = "tableView";
	private static final String cViewType2D = "2Dview";
	private static final String cViewType3D = "3Dview";
	private static final String cViewTypeCards = "cardsView";
	private static final String cViewTypeForm = "formView";
	private static final String cViewTypeStructureGrid = "structureView";
	private static final String cViewTypeExplanation = "explanationView";
	private static final String cViewTypeMacroEditor = "macroEditor";

	private static final String cMainSplitting = "mainSplitting";
	private static final String cRightSplitting = "rightSplitting";
	private static final String cSelectedMainView = "mainView";
	private static final String cMainViewCount = "mainViewCount";
	private static final String cMainViewName = "mainViewName";
	private static final String cMainViewType = "mainViewType";
	private static final String cMainViewDockInfo = "mainViewDockInfo";
	private static final String cMainViewInFront = "mainViewInFront";
	private static final String cDetailView = "detailView";
	private static final String cTableRowHeight = "rowHeight";
	private static final String cTableHeaderLines = "headerLines";
	private static final String cTableColumnWidth = "columnWidth";
	private static final String cTableColumnWrapping = "columnWrapping";
	private static final String cTableColumnVisibility = "columnVisibility";
	private static final String cTableColumnOrder = "columnOrder";
	private static final String cTableColumnFilter = "columnFilter";
	private static final String cTableText = "Text_";				// suffix for cColor???? keys in case of table view column text color
	private static final String cTableBackground = "Background_";	// suffix for cColor???? keys in case of table view column background color
	private static final String cFastRendering = "fastRendering";
	private static final String cViewBackground = "background";
	private static final String cTitleBackground = "titleBackground";
	private static final String cDefaultLabelBackground = "labelBackground";
	private static final String cLabelTransparency = "labelTransparency";
	private static final String cFaceColor3D = "faceColor3D";
	private static final String cAxisColumn = "axisColumn";
	private static final String cAxisMin = "axisMin";
	private static final String cAxisMax = "axisMax";
	private static final String cCachedAxisMin = "cachedAxisMin";
	private static final String cCachedAxisMax = "cachedAxisMax";
	private static final String cAxisLow = "axisLow";	// used to set the visible range the old way (before 20-Feb-2017)
	private static final String cAxisHigh = "axisHigh";	// used to set the visible range the old way (before 20-Feb-2017)
	private static final String cJittering = "jittering";
	private static final String cJitterAxes = "jitterAxes";
	private static final String cFocusList = "focusHitlist";
	private static final String cLabelList = "labelHitlist";
	private static final String cListIsSelection = "selection";
	private static final String cLabelOPCCategory = "opcCategory";
	private static final String cLabelOPCValue = "opcValue";
	private static final String cLabelOPCMode = "opcMode";
	private static final String cMarkerSize = "markersize";
	private static final String cMarkerSizeMin = "markersizeMin";
	private static final String cMarkerSizeMax = "markersizeMax";
	private static final String cSizeColumn = "sizeColumn";
	private static final String cSizeInversion = "sizeInversion";
	private static final String cSizeProportional = "sizeProportional";
	private static final String cSizeAdaption = "sizeAdaption";
	private static final String cLabelSize = "labelSize";
	private static final String cLabelBackground = "_labelBG";
	private static final String cLabelColumn = "labelColumn";
	private static final String cLabelMode = "labelMode";
	private static final String cLabelShowColumnNameInTable = "labelColumnNameInTable";
	private static final String cLabelBlackOrWhite = "labelBlackOrWhite";
	private static final String cLabelPositionOptimization = "labelPositionOptimization";
	private static final String cColorColumn = "colorColumn";
	private static final String cColorCount = "colorCount";
	private static final String cColor = "color";
	private static final String cDefaultColor = "defaultColor";
	private static final String cMissingColor = "missingColor";
	private static final String cColorMin = "colorMin";
	private static final String cColorMax = "colorMax";
	private static final String cColorThresholds = "colorThresholds";
	private static final String cColorListMode = "colorListMode";
	private static final String cBackgroundColorColumn = "backgroundColorColumn";
	private static final String cBackgroundColorCount = "backgroundColorCount";
	private static final String cBackgroundColor = "backgroundColor";
	private static final String cBackgroundColorListMode = "backgroundColorListMode";
	private static final String cBackgroundColorRadius = "backgroundColorRadius";
	private static final String cBackgroundColorFading = "backgroundColorFading";
	private static final String cBackgroundColorRecords = "backgroundColorRecords";
	private static final String cBackgroundColorMin = "backgroundColorMin";
	private static final String cBackgroundColorMax = "backgroundColorMax";
	private static final String cBackgroundColorThresholds = "backgroundColorThresholds";
	private static final String cBackgroundImage = "backgroundImage";
	private static final String cSuppressGrid = "suppressGrid";
	private static final String cSuppressLegend = "suppressLegend";
	private static final String cSuppressScale = "suppressScale";
	private static final String cCrossHairMode = "crosshairMode";
	private static final String cCrossHairList = "crosshairList";
	private static final String cDynamicScale = "dynamicScale";
	private static final String cScaleStyle = "scaleStyle";
	private static final String cDrawMarkerOutline = "drawMarkerOutline";
	private static final String cDrawBoxOutline = "drawBoxOutline";
	private static final String cScatterplotMargin = "scatterplotMargin";
	private static final String cViewFontSize = "fontSize";
	private static final String cViewFontSizeMode = "fontSizeMode";
	private static final String cShapeColumn = "shapeColumn";
	private static final String cMarkerTransparency = "markertransparency";
	private static final String cMarkerLabelTransparency = "markerLabelTransparency";
	private static final String cConnectionLineTransparency = "connectionLineTransparency";
	private static final String cMultiValueMarkerMode = "multiValueMarkerMode";
	private static final String cMultiValueMarkerColumns = "multiValueMarkerColumns";
	private static final String cConnectionColumn1 = "connectionColumn";
	private static final String cConnectionColumn2 = "connectionOrderColumn";
	private static final String cConnectionLineListMode = "connectionLineListMode";
	private static final String cConnectionLineList1 = "connectionLineList1";
	private static final String cConnectionLineList2 = "connectionLineList2";
	private static final String cConnectionArrows = "connectionArrows";
	private static final String cConnectionColumnConnectAll = "<connectAll>";
	private static final String cConnectionColumnConnectCases = "<connectCases>";
	private static final String cConnectionLineWidth = "connectionLineWidth";
	private static final String cAutoZoomColumn = "autoZoomColumn";
	private static final String cAutoZoomFactor = "autoZoomFactor";
	private static final String cTreeViewMode = "treeViewMode";
	private static final String cTreeViewRadius = "treeViewRadius";
	private static final String cTreeViewShowAll = "treeViewShowAll";
	private static final String cTreeViewIsDynamic = "treeViewIsDynamic";
	private static final String cTreeViewIsInverted = "treeViewIsInverted";
	private static final String cSplitViewColumn1 = "splitViewColumn1";
	private static final String cSplitViewColumn2 = "splitViewColumn2";
	private static final String cSplitViewAspect = "splitViewAspect";
	private static final String cSplitViewShowEmpty = "splitViewShowEmpty";
	private static final String cCaseSeparationColumn = "caseSeparationColumn";
	private static final String cCaseSeparationValue = "caseSeparationValue";
	private static final String cChartType = "chartType";
	private static final String cChartMode = "chartMode";
	private static final String cChartColumn = "chartColumn";
	private static final String cBoxplotMeanMode = "boxplotMeanMode";
	private static final String cBoxplotShowMeanValues = "boxPlotShowMeanValues";
	private static final String cCurveMode = "meanLineMode";
	private static final String cCurveRowList = "curveRowList";
	private static final String cCurveStdDev = "meanLineStdDev";
	private static final String cCurveSplitByCategory = "splitCurveByCategory";
	private static final String cCurveTruncate = "truncateCurve";
	private static final String cCurveSplitCategoryColumn = "splitCurveCategoryColumn";
	private static final String cCurveExpression = "curveExpression";
	private static final String cCurveLineWidth = "curveLineWidth";
	private static final String cCurveSmoothing = "curveSmoothing";
	private static final String cShowBarOrPieSizeValue = "showBarOrPieSizeValue";
	private static final String cShowStdDev = "boxPlotShowStdDev";
	private static final String cShowConfInterval = "boxPlotShowConfInterval";
	private static final String cShowValueCount = "boxPlotShowCount";
	private static final String cShowPValue = "showPValue";
	private static final String cShowFoldChange = "showFoldChange";
	private static final String cPValueColumn = "pValueColumn";
	private static final String cPValueRefCategory = "pValueRefCategory";
	private static final String cCorrelationCoefficient = "corrCoefficient";
	private static final String cAffectGlobalExclusion = "affectGlobalExclusion";
	private static final String cIgnoreGlobalExclusion = "ignoreGlobalExclusion";
	private static final String cLocalExclusionList = "localExclusionList";
	private static final String cShowNaNValues = "showNaNValues";
	private static final String cShowLabelBackground = "showLabelBackground";
	private static final String cRotation = "rotationMatrix";
	private static final String cMasterView = "masterView";
	private static final String cUsedAsFilter = "usedAsFilter";
	private static final String cStructureGridColumn = "structureGridColumn";
	private static final String cStructureGridColumns = "structureGridColumns";
	private static final String cFilter = "filter";
	private static final String cFilterAnimation = "filterAnimation";
	private static final String cFormLayout = "formLayout";
	private static final String cFormObjectCount = "formObjectCount";
	private static final String cFormObjectInfo = "formObjectInfo";
	private static final String cStructureDisplayStereoMode = "structureDisplayStereoMode";
	private static final String cStructureDisplayColorMode = "structureDisplayStereoMode";

	private DEParentPane	mParentPane;
	private DEMainPane		mMainPane;
	private DEDetailPane	mDetailPane;
	private DEPruningPanel	mPruningPanel;

	public static DERuntimeProperties getTableOnlyProperties(DEParentPane parentPane) {
		DERuntimeProperties rtp = new DERuntimeProperties(parentPane);
		rtp.setProperty(cMainViewCount, "1");
		return rtp;
		}

	public DERuntimeProperties(DEParentPane parentPane) {
		super(parentPane.getTableModel());
		mParentPane = parentPane;
		mMainPane = parentPane.getMainPane();
		mDetailPane = parentPane.getDetailPane();
		mPruningPanel = parentPane.getPruningPanel();
		}

	public void setParentPane(DEParentPane parentPane) {
		mTableModel = parentPane.getTableModel();
		mParentPane = parentPane;
		mMainPane = parentPane.getMainPane();
		mDetailPane = parentPane.getDetailPane();
		mPruningPanel = parentPane.getPruningPanel();
		}

	@Override
	public void apply() {
		apply(true);
		}

	public void apply(boolean clearAllFirst) {
		if (size() == 0)
			return;

		if (SwingUtilities.isEventDispatchThread()) {
			applyEDT(clearAllFirst);
			}
		else {
				// if we are not in the event dispatcher thread we need to use invokeAndWait
			try {
				SwingUtilities.invokeAndWait(() -> applyEDT(clearAllFirst));
				}
			catch (Exception e) {
				e.printStackTrace();
				}
			}
		}

	private void applyEDT(boolean clearAllFirst) {
		if (size() == 0)
			return;

		if (clearAllFirst) {
			mMainPane.removeAllViews();
			mPruningPanel.removeAllFilters();
			}

		super.apply();

		boolean suppressMessages = DEMacroRecorder.getInstance().isRunningMacro()
				&& (DEMacroRecorder.getInstance().getMessageMode() == DEMacroRecorder.MESSAGE_MODE_SKIP_ERRORS);

		String rowTaskCountString = getProperty(cRowTaskCount);
		if (rowTaskCountString != null) {
			int rowTaskCount = Integer.parseInt(rowTaskCountString);
			for (int i=0; i<rowTaskCount; i++) {
				String taskCode = getProperty(cRowTaskCode+i);
				String taskMenu = getProperty(cRowTaskMenu+i);
				String taskItem = getProperty(cRowTaskItem+i);
				String taskConfig = getProperty(cRowTaskConfig+i);
				DETableRowTaskDef rtd = new DETableRowTaskDef(mParentPane.getParentFrame(), taskCode, taskConfig, taskMenu, taskItem);
				if (rtd.isValid())
				    mMainPane.addRowTask(rtd);
				}
			}

		String mainSplitting = getProperty(cMainSplitting);
		if (mainSplitting != null) {
			try {
				mParentPane.setMainSplitting(Double.parseDouble(mainSplitting));
				}
			catch (NumberFormatException e) {}
			}
		String rightSplitting = getProperty(cRightSplitting);
		if (rightSplitting != null) {
			try {
				mParentPane.setRightSplitting(Double.parseDouble(rightSplitting));
				}
			catch (NumberFormatException e) {}
			}

		String viewCountString = getProperty(cMainViewCount);
		if (viewCountString == null && clearAllFirst) {	// either old file with standard views only, or applying just additional properties (clearAllFirst==false)
			applyViewProperties(mMainPane.addTableView("Table", "root"), "Table", true);
			applyViewProperties(mMainPane.add2DView("2D View", "Table\tcenter"), "2D", true);
			applyViewProperties(mMainPane.add3DView("3D View", "Table\tcenter"), "3D", true);
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				if (mTableModel.isColumnTypeStructure(column)) {
					applyViewProperties(mMainPane.addStructureView("Structures", "Table\tcenter", column), "StructureView", true);
					break;
					}
				}
			}
		else {
			int viewCount = (viewCountString == null) ? 0 : Integer.parseInt(viewCountString);
			for (int i=0; i<viewCount; i++) {
				String viewType = getProperty(cMainViewType+i);
				String tabName = getProperty(cMainViewName+i);
				String dockInfo = getProperty(cMainViewDockInfo+i);

				if (i == 0) {
					// cMainViewName, cMainViewType and cMainViewDockInfo were not defined
					// for view=0 before V3.0
					if (tabName == null && viewType == null) {
						viewType = cViewTypeTable;
						tabName = "Table";
						}
					if (dockInfo == null) {
						dockInfo = "root";
						}
					}
				else if (dockInfo == null) {
					// cMainViewDockInfo was not defined before V3.1
					String refTitle = getProperty(cMainViewName+0);
					if (refTitle == null)
						refTitle = "Table";
					dockInfo = refTitle + "\tcenter";
					}

				CompoundTableView view = (viewType == null) ? null
							   : viewType.equals(cViewTypeTable) ? mMainPane.addTableView(tabName, dockInfo)
							   : viewType.equals(cViewType2D) ? mMainPane.add2DView(tabName, dockInfo)
							   : viewType.equals(cViewType3D) ? mMainPane.add3DView(tabName, dockInfo)
							   : viewType.equals(cViewTypeCards) ? mMainPane.addCardsView(tabName, dockInfo)
							   : viewType.equals(cViewTypeForm) ? mMainPane.addFormView(tabName, dockInfo, false)
							   : viewType.equals(cViewTypeStructureGrid) ? mMainPane.addStructureView(tabName, dockInfo, -1)
							   : viewType.equals(cViewTypeExplanation) ? mMainPane.addExplanationView(tabName, dockInfo)
							   : viewType.equals(cViewTypeMacroEditor) ? mMainPane.addApplicationView(DEMainPane.VIEW_TYPE_MACRO_EDITOR, tabName, dockInfo)
							   : null;
				if (view != null)
					applyViewProperties(view, "_" + tabName, clearAllFirst);
				}

			if (!clearAllFirst) {
				// we may have additional properties to already existing views
				for (String viewName:mMainPane.getDockableTitles())
					applyViewProperties(mMainPane.getView(viewName), "_" + viewName, clearAllFirst);
				}

			for (int i=0; i<viewCount; i++)
				if ("true".equals(getProperty(cMainViewInFront + i)))
					mMainPane.setToFrontInTabbedPane(getProperty(cMainViewName+i));

			for (int i=0; i<viewCount; i++) {
				String viewType = getProperty(cMainViewType+i);
				if (viewType != null	// was not defined for view=0 before V3.0
				 && (viewType.equals(cViewType2D) || viewType.equals(cViewType3D))) {
					String tabName = getProperty(cMainViewName+i);
					String masterView = getProperty(cMasterView+"_"+tabName);
					if (masterView != null)
						((VisualizationPanel)mMainPane.getView(tabName)).setSynchronizationMaster(
								(VisualizationPanel)mMainPane.getView(masterView));
					}
				}
			}

		String property = null;
		for (int i=0; (property=getProperty(cFilter+i))!=null; i++) {
			JFilterPanel filter = mPruningPanel.createFilterFromSettings(property, suppressMessages);

			if (filter != null) {
				String settings = getProperty(cFilterAnimation+i);
				if (settings != null)
					filter.applyAnimationSettings(settings);
				}
			}

		String name = getProperty(cSelectedMainView);
		if (name != null)
			mMainPane.setSelectedView(name);

		String detail = getProperty(cDetailView);
		if (detail != null) {
			if (detail.startsWith(JMultiPanelView.VIEW_HEIGHT))
				mDetailPane.setProperties(detail);
			else {		// to be compatible with old JTabbedPane based DEDetailPane
				if (detail.equals("Detail"))
					mDetailPane.setProperties("height[Structure]=0.5;height[Data]=0.5");
				else
					mDetailPane.setProperties("height["+detail+"]=1.0");
				}
			}
		}

	private void applyMarkerLabelDisplayerProperties(String viewName, MarkerLabelDisplayer displayer) {
		int[] columnAtPosition = new int[MarkerLabelDisplayer.cPositionCode.length];
		for (int i=0; i<MarkerLabelDisplayer.cPositionCode.length; i++) {
			String columnKey = cLabelColumn+viewName+"_"+MarkerLabelDisplayer.cPositionCode[i];
			String columnName = getProperty(columnKey);

			// for compatibility to older encoding before Sep2013
			if (columnName == null)
				columnName = getProperty(cLabelColumn+viewName+"_"+MarkerLabelDisplayer.cPositionOption[i]);

			columnAtPosition[i] = (columnName == null) ? -1 : mTableModel.findColumn(columnName);
			}

		displayer.setMarkerLabels(columnAtPosition);

		String value = getProperty(cLabelList + viewName);
		if (value != null) {
			if (value.equals(cListIsSelection)) {
				displayer.setMarkerLabelList(MarkerLabelDisplayer.cLabelsOnSelection);
				}
			else {
				int list = mTableModel.getListHandler().getListIndex(value);
				if (list != -1)
					displayer.setMarkerLabelList(list);
				}
			}

		int opcCategory = mTableModel.findColumn(getProperty(cLabelOPCCategory + viewName));
		if (opcCategory != -1) {
			int opcValue = mTableModel.findColumn(getProperty(cLabelOPCValue + viewName));
			if (opcValue != -1) {
				String mode = getProperty(cLabelOPCMode + viewName);
				for (int i = 0; i<cOnePerCategoryMode.length; i++) {
					if (cOnePerCategoryMode[i].equals(mode)) {
						displayer.setMarkerLabelOnePerCategory(opcCategory, opcValue, i);
						break;
						}
					}
				}
			}

		String mode = getProperty(cLabelMode+viewName);
		displayer.setMarkerLabelsInTreeViewOnly("inDetailGraphOnly".equals(mode));

		String size = getProperty(cLabelSize+viewName);
		if (size != null)
			displayer.setMarkerLabelSize(Float.parseFloat(size), false);

		String columnNameInTable = getProperty(cLabelShowColumnNameInTable+viewName);
		if (columnNameInTable != null)
			displayer.setShowColumnNameInTable("true".equals(columnNameInTable));

		String optimizePositions = getProperty(cLabelPositionOptimization+viewName);
		if (optimizePositions != null)
			displayer.setOptimizeLabelPositions("true".equals(optimizePositions));

		String blackOrWhite = getProperty(cLabelBlackOrWhite+viewName);
		if (blackOrWhite != null)
			displayer.setMarkerLabelsBlackOrWhite("true".equals(blackOrWhite));
		}

	public void applyViewProperties(CompoundTableView view, String viewName, boolean isNewView) {
		ViewConfiguration config = getViewConfiguration(viewName.substring(1));
		if (config != null) {
			config.apply(view);
			return;
			}

		if (view instanceof DETableView) {
			DETable table = ((DETableView)view).getTable();
			String value = getProperty(cTableRowHeight+viewName);
			if (value != null || isNewView)
				table.setRowHeight(HiDPIHelper.scale(value == null ? 16 : Integer.parseInt(value)));
			value = getProperty(cViewFontSize+viewName);
			if (value != null)
				table.setFontSize(Integer.parseInt(value));
			value = getProperty(cTableHeaderLines+viewName);
			if (value != null)
				((DETableView)view).setHeaderLineCount(Integer.parseInt(value));
			for (int modelColumn=0; modelColumn<mTableModel.getColumnCount(); modelColumn++) {
				int column = mTableModel.convertFromDisplayableColumnIndex(modelColumn);
				value = getProperty(cTableColumnWidth+viewName+"_"+mTableModel.getColumnTitleNoAlias(column));
				if (value != null)
					table.getColumnModel().getColumn(table.convertColumnIndexToView(modelColumn)).setPreferredWidth(HiDPIHelper.scale(Integer.parseInt(value)));
				value = getProperty(cTableColumnWrapping+viewName+"_"+mTableModel.getColumnTitleNoAlias(column));
				if (value != null)
					((DETableView)view).setTextWrapping(column, value.equals("true"));
				value = getProperty(cColorColumn+cTableText+viewName+mTableModel.getColumnTitleNoAlias(column));
				if (value != null)
					applyViewColorProperties(cTableText+viewName+mTableModel.getColumnTitleNoAlias(column),
							((DETableView)view).getColorHandler().getVisualizationColor(column, CompoundTableColorHandler.FOREGROUND));
				value = getProperty(cColorColumn+cTableBackground+viewName+mTableModel.getColumnTitleNoAlias(column));
				if (value != null)
					applyViewColorProperties(cTableBackground+viewName+mTableModel.getColumnTitleNoAlias(column),
							((DETableView)view).getColorHandler().getVisualizationColor(column, CompoundTableColorHandler.BACKGROUND));
				}
			value = getProperty(cTableColumnOrder+viewName);
			if (value != null)
				table.setColumnOrderString(value);

			value = getProperty(cTableColumnFilter+viewName);
			if (value != null)
				table.setColumnFilterText(value);
			for (int modelColumn=0; modelColumn<mTableModel.getColumnCount(); modelColumn++) { // manual column visibility may override filter
				int column = mTableModel.convertFromDisplayableColumnIndex(modelColumn);
				value = getProperty(cTableColumnVisibility+viewName+"_"+mTableModel.getColumnTitleNoAlias(column));
				if (value != null)
					((DETableView)view).setColumnVisibility(column, value.equals("true"));
				}
			}
		else if (view instanceof VisualizationPanel) {
			VisualizationPanel vpanel = (VisualizationPanel)view;
			JVisualization visualization = vpanel.getVisualization();

			int chartType = -1;
			int chartMode = JVisualization2D.cChartModeCount;
			int chartColumn = JVisualization.cColumnUnassigned;

			// for compatibility up to version 3.4.2
			String value = getProperty("preferHistogram"+viewName);

			// for compatibility with version with fixed main views
			if (value == null && viewName.equals("2D"))
				value = getProperty("preferHistogram");

			if (value != null && value.equals("false")) {
				chartType = JVisualization2D.cChartTypeScatterPlot;
				}
			else {	// this is the handling after version 3.5.0
				chartType = decodeProperty(cChartType+viewName, JVisualization.CHART_TYPE_CODE);
				value = getProperty(cChartMode+viewName);
				for (int i=0; i<JVisualization.CHART_MODE_CODE.length; i++) {
					if (JVisualization.CHART_MODE_CODE[i].equals(value)) {
						chartMode = i;
						break;
						}
					}
				if (chartMode != JVisualization.cChartModeCount && chartMode != JVisualization.cChartModePercent) {
					String columnName = getProperty(cChartColumn+viewName);
					if (columnName != null)
						chartColumn = mTableModel.findColumn(columnName);
					if (chartColumn == JVisualization.cColumnUnassigned)
						chartMode = JVisualization.cChartModeCount;
					}
				}
			visualization.setPreferredChartType(chartType, chartMode, chartColumn);

			int dimensions = vpanel.getDimensionCount();
			for (int j=0; j<dimensions; j++) {
						// popups assigning column to axis
				String key = cAxisColumn + viewName + "_" + j;
				value = getProperty(key);
				if (value != null
				 && vpanel.setAxisColumnName(j, value)) {

					// setting visible range the old way (before 20-Feb-2017)
					key = cAxisLow + viewName + "_" + j;
					value = getProperty(key);
					if (value != null)
						vpanel.getPruningBar(j).setLowValue(Float.parseFloat(value));

					key = cAxisHigh + viewName + "_" + j;
					value = getProperty(key);
					if (value != null)
						vpanel.getPruningBar(j).setHighValue(Float.parseFloat(value));

					// setting visible range from the numerical values
					key = cAxisMin + viewName + "_" + j;
					float low = Float.NaN;
					value = getProperty(key);
					if (value != null)
						low = Float.parseFloat(value);

					key = cAxisMax + viewName + "_" + j;
					float high = Float.NaN;
					value = getProperty(key);
					if (value != null)
						high = Float.parseFloat(value);

					if (!Float.isNaN(low) || !Float.isNaN(high))
						vpanel.setVisibleRange(j, low, high);

					// setting cached visible min and max in case of autozooming
					key = cCachedAxisMin + viewName + "_" + j;
					value = getProperty(key);
					if (value != null)
						vpanel.setCachedPruningBarLow(j, Float.parseFloat(value));

					key = cCachedAxisMax + viewName + "_" + j;
					value = getProperty(key);
					if (value != null)
						vpanel.setCachedPruningBarHigh(j, Float.parseFloat(value));
					}
				}

			value = getProperty(cScatterplotMargin + viewName);
			if (value != null)
				visualization.setScatterPlotMargin(Float.parseFloat(value));

			value = getProperty(cViewFontSize + viewName);
			int fontSizeMode = decodeProperty(cViewFontSizeMode+viewName, JVisualization.FONT_SIZE_MODE_CODE);
			if (value != null || fontSizeMode != -1) {
				if (fontSizeMode == -1)
					fontSizeMode = JVisualization.cFontSizeModeRelative;
				visualization.setFontSize(Float.parseFloat(value), fontSizeMode, false);
				}

			value = getProperty(cViewBackground + viewName);
			if (value != null)
				visualization.setViewBackground(Color.decode(value));

			value = getProperty(cTitleBackground + viewName);
			if (value != null)
				visualization.setTitleBackground(Color.decode(value));

			value = getProperty(cDefaultLabelBackground + viewName);
			if (value != null)
				visualization.setDefaultLabelBackground(Color.decode(value));

			value = getProperty(cLabelTransparency + viewName);
			if (value != null)
				visualization.setLabelTransparency(Float.parseFloat(value), false);

			value = getProperty(cJittering + viewName);
			if (value != null) {
				float jittering = Float.parseFloat(value);
				value = getProperty(cJitterAxes + viewName);
				int axes = (value != null) ? Integer.parseInt(value) : 0;
				visualization.setJittering(jittering, axes, false);
				}

			value = getProperty(cFastRendering + viewName);
			if (value != null)
				visualization.setFastRendering("true".equals(value));

			value = getProperty(cDrawBoxOutline + viewName);
			if (value != null)
				((JVisualization2D)visualization).setDrawBoxOutline("true".equals(value));

			value = getProperty(cDrawMarkerOutline + viewName);
			if (value != null)
				((JVisualization2D)visualization).setDrawMarkerOutline("true".equals(value));

			value = getProperty(cMarkerSize + viewName);
			if (value != null)
				visualization.setMarkerSize(Float.parseFloat(value), false);

			value = getProperty(cSizeInversion + viewName);
			visualization.setMarkerSizeInversion(value != null && value.equals("true"));

			value = getProperty(cSizeProportional + viewName);
			visualization.setMarkerSizeProportional(value != null && value.equals("true"));

			value = getProperty(cSizeAdaption + viewName);
			visualization.setMarkerSizeZoomAdaption(value == null || value.equals("true"));

			value = getProperty(cSizeColumn + viewName);
			if (value != null) {
				int column = JVisualization.cColumnUnassigned;
				if (value.startsWith("sizeByHitlist")) {
					String hitlistName = value.substring(value.indexOf('\t')+1);
					int hitlistIndex = mTableModel.getListHandler().getListIndex(hitlistName);
					if (hitlistIndex != -1)
						column = CompoundTableListHandler.getColumnFromList(hitlistIndex);
					}
				else {
					column = mTableModel.findColumn(value);
					}
				if (column != JVisualization.cColumnUnassigned) {
					value = getProperty(cMarkerSizeMin + viewName);
					float min = (value == null) ? Float.NaN : Float.parseFloat(value);
					value = getProperty(cMarkerSizeMax + viewName);
					float max = (value == null) ? Float.NaN : Float.parseFloat(value);
					visualization.setMarkerSizeColumn(column, min, max);
					}
				}

			applyViewColorProperties(viewName, visualization.getMarkerColor());
			applyViewColorProperties(cLabelBackground+viewName, visualization.getLabelBackgroundColor());

			value = getProperty(cShapeColumn+viewName);
			if (value != null) {
				int column = JVisualization.cColumnUnassigned;
				if (value.startsWith("shapeByHitlist")) {
					String hitlistName = value.substring(value.indexOf('\t')+1);
					int hitlistIndex = mTableModel.getListHandler().getListIndex(hitlistName);
					if (hitlistIndex != -1)
						column = CompoundTableListHandler.getColumnFromList(hitlistIndex);
					}
				else {
					column = mTableModel.findColumn(value);
					}
				if (column != JVisualization.cColumnUnassigned)
					visualization.setMarkerShapeColumn(column);
				}

			value = getProperty(cCaseSeparationColumn+viewName);
			if (value != null) {
				int column = JVisualization.cColumnUnassigned;
				if (value.startsWith("splitByHitlist")) {
					String hitlistName = value.substring(value.indexOf('\t')+1);
					int hitlistIndex = mTableModel.getListHandler().getListIndex(hitlistName);
					if (hitlistIndex != -1)
						column = CompoundTableListHandler.getColumnFromList(hitlistIndex);
					}
				else {
					column = mTableModel.findColumn(value);
					}
				if (column != JVisualization.cColumnUnassigned) {
					value = getProperty(cCaseSeparationValue+viewName);
					visualization.setCaseSeparation(column, Float.parseFloat(value), false);
					}
				}

			value = getProperty(cSplitViewColumn1+viewName);
			if (value != null) {
				int column1 = JVisualization.cColumnUnassigned;
				if (value.startsWith("splitByHitlist")) {
					String hitlistName = value.substring(value.indexOf('\t')+1);
					int hitlistIndex = mTableModel.getListHandler().getListIndex(hitlistName);
					if (hitlistIndex != -1)
						column1 = CompoundTableListHandler.getColumnFromList(hitlistIndex);
					}
				else {
					column1 = mTableModel.findColumn(value);
					}

				int column2 = JVisualization.cColumnUnassigned;
				value = getProperty(cSplitViewColumn2+viewName);
				if (value != null) {
					column2 = JVisualization.cColumnUnassigned;
					if (value.startsWith("splitByHitlist")) {
						String hitlistName = value.substring(value.indexOf('\t')+1);
						int hitlistIndex = mTableModel.getListHandler().getListIndex(hitlistName);
						if (hitlistIndex != -1)
							column2 = CompoundTableListHandler.getColumnFromList(hitlistIndex);
						}
					else {
						column2 = mTableModel.findColumn(value);
						}
					}

				if (column1 != JVisualization.cColumnUnassigned
				 || column2 != JVisualization.cColumnUnassigned) {
					value = getProperty(cSplitViewAspect+viewName);
					float aspect = (value != null) ? Float.parseFloat(value) : 1.0f;
					value = getProperty(cSplitViewShowEmpty+viewName);
					boolean showEmpty = (value == null) ? true : "true".equals(value);
					visualization.setSplittingColumns(column1, column2, aspect, showEmpty);
					}
				}

			applyMarkerLabelDisplayerProperties(viewName, visualization);

			value = getProperty(cConnectionColumn1+viewName);
			if (value != null) {
				int column1 = value.equals(cConnectionColumnConnectAll) ? JVisualization.cConnectionColumnConnectAll
							: value.equals(cConnectionColumnConnectCases) ? JVisualization.cConnectionColumnConnectCases : mTableModel.findColumn(value);
				if (column1 != JVisualization.cColumnUnassigned) {
					int column2 = JVisualization.cColumnUnassigned;
					value = getProperty(cConnectionColumn2+viewName);
					if (value != null)
						column2 = mTableModel.findColumn(value);
					visualization.setConnectionColumns(column1, column2);
					value = getProperty(cConnectionArrows+viewName);
					visualization.setConnectionLineInversion("inverted".equals(value));
					value = getProperty(cConnectionLineWidth+viewName);
					float lineWidth = (value != null) ? Float.parseFloat(value) : 1.0f;
					visualization.setConnectionLineWidth(lineWidth, false);

					int listMode = decodeProperty(cConnectionLineListMode+viewName, JVisualization.cConnectionListModeCode);
					if (listMode != -1 && listMode != JVisualization.cConnectionListModeNone) {
						int list1 = mTableModel.getListHandler().getListIndex(getProperty(cConnectionLineList1+viewName));
						int list2 = mTableModel.getListHandler().getListIndex(getProperty(cConnectionLineList2+viewName));
						if (list1 != -1)
							visualization.setConnectionLineListMode(listMode, list1, list2);
						}

					int treeViewMode = decodeProperty(cTreeViewMode+viewName, JVisualization.TREE_VIEW_MODE_CODE);
					if (treeViewMode != -1 && treeViewMode != JVisualization.cTreeViewModeNone) {
						int radius = 5;
						value = getProperty(cTreeViewRadius+viewName);
						if (value != null)
							try { radius = Integer.parseInt(value); } catch (NumberFormatException nfe) {}
						value = getProperty(cTreeViewShowAll+viewName);
						boolean showAll = (value == null || value.equals("true"));
						value = getProperty(cTreeViewIsDynamic+viewName);
						boolean isDynamic = (value != null && value.equals("true"));
						value = getProperty(cTreeViewIsInverted+viewName);
						boolean isInverted = (value != null && value.equals("true"));
						visualization.setTreeViewMode(treeViewMode, radius, showAll, isDynamic, isInverted);
						}
					}
				}

			value = getProperty(cAutoZoomFactor+viewName);
			if (value != null) {
				float azf = Float.parseFloat(value);
				int column = JVisualization.cColumnUnassigned;
				value = getProperty(cAutoZoomColumn+viewName);
				if (value != null)
					column = mTableModel.findColumn(value);
				vpanel.setAutoZoom(azf, column, false);
				}

			value = getProperty(cUsedAsFilter+viewName);
			visualization.setUseAsFilter(value != null && value.equals("true"));

			value = getProperty(cShowNaNValues+viewName);
			visualization.setShowNaNValues(value != null && value.equals("true"));

			value = getProperty(cShowLabelBackground+viewName);
			visualization.setShowLabelBackground(value != null && value.equals("true"));

			value = getProperty(cAffectGlobalExclusion+viewName);
			visualization.setAffectGlobalExclusion(value == null || value.equals("true"));

			value = getProperty(cIgnoreGlobalExclusion+viewName);
			visualization.setIgnoreGlobalExclusion(value != null && value.equals("true"));

			int exclusionList = mTableModel.getListHandler().getListIndex(getProperty(cLocalExclusionList+viewName));
			if (exclusionList != -1)
				visualization.setLocalExclusionList(exclusionList);

			int scaleMode = decodeProperty(cSuppressScale+viewName, JVisualization.SCALE_MODE_CODE);
			if (scaleMode != -1)
				visualization.setScaleMode(scaleMode);

			int scaleStyle = decodeProperty(cScaleStyle+viewName, JVisualization.SCALE_STYLE_CODE);
			if (scaleStyle != -1)
				visualization.setScaleStyle(scaleStyle);

			int gridMode = decodeProperty(cSuppressGrid+viewName, JVisualization.GRID_MODE_CODE);
			if (gridMode != -1)
				visualization.setGridMode(gridMode);

			value = getProperty(cSuppressLegend+viewName);
			visualization.setSuppressLegend("true".equals(value));

			value = getProperty(cDynamicScale+viewName);
			visualization.setDynamicScale(!"false".equals(value));

			value = getProperty(cShowBarOrPieSizeValue+viewName);
			visualization.setShowBarOrPieSizeValue(value != null && value.equals("true"));

			value = getProperty(cShowStdDev+viewName);
			visualization.setShowStandardDeviation(value != null && value.equals("true"));

			value = getProperty(cShowConfInterval+viewName);
			visualization.setShowConfidenceInterval(value != null && value.equals("true"));

			value = getProperty(cShowValueCount+viewName);
			visualization.setShowValueCount(value != null && value.equals("true"));

			value = getProperty(cShowFoldChange+viewName);
			visualization.setShowFoldChange(value != null && value.equals("true"));
			
			value = getProperty(cShowPValue+viewName);
			visualization.setShowPValue(value != null && value.equals("true"));

			value = getProperty(cPValueColumn+viewName);
			if (value != null) {
				int column = mTableModel.findColumn(value);
				if (column != JVisualization.cColumnUnassigned)
					visualization.setPValueColumn(column, getProperty(cPValueRefCategory+viewName));
				}

			int boxplotMeanMode = decodeProperty(cBoxplotMeanMode+viewName, JVisualization.BOXPLOT_MEAN_MODE_CODE);
			if (boxplotMeanMode != -1)
				visualization.setBoxplotMeanMode(boxplotMeanMode);

			value = getProperty(cBoxplotShowMeanValues+viewName);
			boolean showMeanValues = (value != null && value.equals("true"));
			visualization.setShowMeanAndMedianValues(showMeanValues);

			if (view instanceof VisualizationPanel2D) {
				value = getProperty(cMarkerTransparency+viewName);
				if (value != null) {
					try {
						float transparency = Float.parseFloat(value);

						float transparency1 = transparency;
						String value1 = getProperty(cMarkerLabelTransparency+viewName);
						if (value1 != null)
							try { transparency1 = Float.parseFloat(value1); } catch (NumberFormatException nfe) {}

						float transparency2 = transparency;
						String value2 = getProperty(cConnectionLineTransparency+viewName);
						if (value2 != null)
							try { transparency2 = Float.parseFloat(value2); } catch (NumberFormatException nfe) {}

						((JVisualization2D)visualization).setTransparency(transparency, transparency1, transparency2);
						}
					catch (NumberFormatException nfe) {}
					}

				int crossHairMode = decodeProperty(cCrossHairMode+viewName, JVisualization2D.CROSSHAIR_MODE_CODE);
				if (crossHairMode != -1)
					((JVisualization2D)visualization).setCrossHairMode(crossHairMode);

				String crossHairList = getProperty(cCrossHairList+viewName);
				if (crossHairList != null)
					((JVisualization2D)visualization).setCrossHairList(crossHairList);

				value = getProperty(cMultiValueMarkerColumns+viewName);
				if (value != null) {
					String[] columnName = value.split("\\t");
					int[] column = new int[columnName.length];
					int foundCount = 0;
					for (int i=0; i<column.length; i++) {
						column[i] = mTableModel.findColumn(columnName[i]);
						if (column[i] != -1)
							foundCount++;
						}
					if (foundCount > 1) {
						if (foundCount < column.length) {
							int[] newColumn = new int[foundCount];
							int index = 0;
							for (int i=0; i<column.length; i++)
								if (column[i] != -1)
									newColumn[index++] = column[i];
							column = newColumn;
							}
						int mode = decodeProperty(cMultiValueMarkerMode+viewName, JVisualization2D.MULTI_VALUE_MARKER_MODE_CODE);
						if (mode != -1)
							((JVisualization2D)visualization).setMultiValueMarkerColumns(column, mode);
						}
					}

				value = getProperty(cBackgroundColorColumn+viewName);
				if (value != null) {
					try {
						int column = JVisualization.cColumnUnassigned;

							// to be compatible with format prior V2.7.0
						if (value.equals("colorBySimilarity"))
							column = mTableModel.findColumn(DescriptorConstants.DESCRIPTOR_FFP512.shortName);

						else if (value.startsWith("colorByHitlist")) {
							String hitlistName = value.substring(value.indexOf('\t')+1);
							int hitlistIndex = mTableModel.getListHandler().getListIndex(hitlistName);
							if (hitlistIndex != -1)
								column = CompoundTableListHandler.getColumnFromList(hitlistIndex);
							}
						else {
							column = mTableModel.findColumn(value);
							}

						if (column != JVisualization.cColumnUnassigned) {
							Color[] colorList = null;
							value = getProperty(cBackgroundColorCount+viewName);
							if (value != null) {
								int colorCount = Integer.parseInt(value);
								colorList = new Color[colorCount];
								for (int j=0; j<colorCount; j++) {
									value = getProperty(cBackgroundColor+viewName + "_" + j);
									colorList[j] = Color.decode(value);
									}
								}

							value = getProperty(cBackgroundColorListMode+viewName);
							int colorListMode = VisualizationColor.cColorListModeStraight;	// default
							if (value != null) {
								if (value.equals("Categories"))
									colorListMode = VisualizationColor.cColorListModeCategories;
								else if (value.equals("HSBShort"))
									colorListMode = VisualizationColor.cColorListModeHSBShort;
								else if (value.equals("HSBLong"))
									colorListMode = VisualizationColor.cColorListModeHSBLong;
								else if (value.equals("straight"))
									colorListMode = VisualizationColor.cColorListModeStraight;
								}

							if (colorList == null) {	// cColorCount is only available if mode is cColorListModeCategory
								Color color1 = Color.decode(getProperty(cBackgroundColor + viewName + "_0"));
								Color color2 = Color.decode(getProperty(cBackgroundColor + viewName + "_1"));
								colorList = VisualizationColor.createColorWedge(color1, color2, colorListMode, null);
								}

							float[] thresholds = VisualizationColor.parseCustomThresholds(
									getProperty(cBackgroundColorThresholds+viewName), mTableModel, column);

							((JVisualization2D)visualization).getBackgroundColor().setColor(column, colorList, colorListMode, thresholds);

							value = getProperty(cBackgroundColorRecords);
							if (value != null) {
								if (value.equals("visibleRecords"))
									((JVisualization2D)visualization).setBackgroundColorConsidered(
													JVisualization2D.BACKGROUND_VISIBLE_RECORDS);
								else if (value.equals("allRecords"))
									((JVisualization2D)visualization).setBackgroundColorConsidered(
													JVisualization2D.BACKGROUND_ALL_RECORDS);
								else if (value.startsWith("fromHitlist")) {
									String hitlistName = value.substring(value.indexOf('\t')+1);
									int hitlistIndex = mTableModel.getListHandler().getListIndex(hitlistName);
									if (hitlistIndex != -1)
										((JVisualization2D)visualization).setBackgroundColorConsidered(hitlistIndex);
									}
								}

							value = getProperty(cBackgroundColorRadius+viewName);
							if (value != null) {
								float radius = Float.parseFloat(value);
								((JVisualization2D)visualization).setBackgroundColorRadius(radius);
								}

							value = getProperty(cBackgroundColorFading+viewName);
							if (value != null) {
								float fading = Float.parseFloat(value);
								((JVisualization2D)visualization).setBackgroundColorFading(fading);
								}

							value = getProperty(cBackgroundColorMin + viewName);
							float min = (value == null) ? Float.NaN : Float.parseFloat(value);
							value = getProperty(cBackgroundColorMax + viewName);
							float max = (value == null) ? Float.NaN : Float.parseFloat(value);
							if (!Double.isNaN(min) || !Double.isNaN(max))
								((JVisualization2D)visualization).getBackgroundColor().setColorRange(min, max);
							}
						}
					catch (Exception e) {
//						JOptionPane.showMessageDialog(mParentFrame, "Invalid color settings");
						}
					}

				byte[] backgroundImageData = getBinary(cBackgroundImage+viewName);
				if (backgroundImageData != null)
					((JVisualization2D)visualization).setBackgroundImageData(backgroundImageData);

				int mode = decodeProperty(cCurveMode+viewName, JVisualization2D.CURVE_MODE_CODE);
				if (mode != -1) {
					String listName = getProperty(cCurveRowList+viewName);
					int rowList = listName == null ? JVisualization2D.cCurveRowListVisible
							: listName.equals(JVisualization2D.ROW_LIST_CODE_SELECTED) ? JVisualization2D.cCurveRowListSelected
							: mTableModel.getListHandler().getListIndex(listName);
					value = getProperty(cCurveStdDev+viewName);
					boolean stdDev = (value != null && value.equals("true"));
					value = getProperty(cCurveTruncate+viewName);
					boolean truncate = (value != null && value.equals("true"));
					value = getProperty(cCurveSplitByCategory+viewName);
					boolean split = (value != null && value.equals("true"));
					int column = mTableModel.findColumn(getProperty(cCurveSplitCategoryColumn+viewName));
					((JVisualization2D)visualization).setCurveMode(mode, rowList, stdDev, truncate, split, column);
					value = getProperty(cCurveLineWidth+viewName);
					float curveLineWidth = (value == null) ? JVisualization2D.DEFAULT_CURVE_LINE_WIDTH : Float.parseFloat(value);
					((JVisualization2D)visualization).setCurveLineWidth(curveLineWidth);
					value = getProperty(cCurveExpression+viewName);
					if (value != null)
						((JVisualization2D)visualization).setCurveExpression(value);
					value = getProperty(cCurveSmoothing+viewName);
					if (value != null)
						((JVisualization2D)visualization).setCurveSmoothing(Float.parseFloat(value));
					}

				int type = decodeProperty(cCorrelationCoefficient+viewName, CorrelationCalculator.TYPE_NAME);
				if (type != -1)
					((JVisualization2D)visualization).setShownCorrelationType(type);
				}

			if (view instanceof VisualizationPanel3D) {
				value = getProperty(cFaceColor3D + viewName);
				if (value != null)
					((JVisualization3D)visualization).setGraphFaceColor(Color.decode(value));

				float[][] rotation = new float[3][3];
				int count = 0;
				for (int j=0; j<3; j++) {
					for (int k=0; k<3; k++) {
						value = getProperty(cRotation+viewName+j+k);
						if (value != null) {
							rotation[j][k] = Float.parseFloat(value);
							count++;
							}
						}
					}
				if (count == 9)
					((JVisualization3D)visualization).setRotationMatrix(rotation);
				}
			}
		else if (view instanceof DEFormView) {
			JCompoundTableForm form = ((DEFormView)view).getCompoundTableForm();

			String value = getProperty(cViewFontSize+viewName);
			if (value != null)
				form.setFontSize(Integer.parseInt(value));

			form.setFormLayoutDescriptor(getProperty(cFormLayout+viewName));

			try {
				value = getProperty(cFormObjectCount+viewName);
				int objectCount = Integer.parseInt(value);
				for (int j=0; j<objectCount; j++) {
					String description = getProperty(cFormObjectInfo+viewName+"_"+j);

						// The "idcode" column of versions before 2.7 is renamed to "Structure"
						// Thus, all references have to be adapted
					if (description.startsWith("idcode\tstructure")
					 && mTableModel.findColumn("idcode") == -1)
						description = "Structure" + description.substring(6);

					form.addFormObject(updateFormObjectDescription(description));
					}
				form.updateColors();
				}
			catch (NumberFormatException e) {}
			}
		else if (view instanceof JStructureGrid) {
			applyMarkerLabelDisplayerProperties(viewName, (JStructureGrid)view);

			if (viewName.equals("StructureView"))
				viewName = "";		// to provide read compatibility to version with static views

			String value = getProperty(cStructureGridColumns+viewName);
			try {
				((JStructureGrid)view).setColumnCount((value == null) ? 6 : Integer.parseInt(value));
				}
			catch (NumberFormatException e) {}

			value = getProperty(cStructureGridColumn+viewName);
			if (value == null)  // to be compatible with prior V2.7.0 format
				value = "Structure";
			((JStructureGrid)view).setStructureColumn(mTableModel.findColumn(value));

			value = getProperty(cStructureDisplayStereoMode+viewName);
			if (value != null) {
				int stereoModeIndex = DETaskSetStructureDisplayMode.findStereoModeIndex(value, -1);
				if (stereoModeIndex != -1) {
					int currentModeWithoutStereo = (((JStructureGrid)view).getStructureDisplayMode() & ~DETaskSetStructureDisplayMode.STEREO_MODE_MASK);
					((JStructureGrid)view).setStructureDisplayMode(currentModeWithoutStereo | DETaskSetStructureDisplayMode.STEREO_MODE[stereoModeIndex]);
					}
				}

			value = getProperty(cStructureDisplayColorMode+viewName);
			if (value != null) {
				int colorModeIndex = DETaskSetStructureDisplayMode.findColorModeIndex(value, -1);
				if (colorModeIndex != -1) {
					int currentModeWithoutColor = (((JStructureGrid)view).getStructureDisplayMode() & ~DETaskSetStructureDisplayMode.COLOR_MODE_MASK);
					((JStructureGrid)view).setStructureDisplayMode(currentModeWithoutColor | DETaskSetStructureDisplayMode.COLOR_MODE[colorModeIndex]);
					}
				}
			}
		
		if (view instanceof FocusableView) {
			String value = getProperty(cFocusList + viewName);
			if (value != null) {
				if (value.equals(cListIsSelection)) {
					((FocusableView)view).setFocusList(FocusableView.cFocusOnSelection);
					}
				else {
					int hitlist = mTableModel.getListHandler().getListIndex(value);
					if (hitlist != -1)
						((FocusableView)view).setFocusList(hitlist);
					}
				}
			}
		}

	private void applyViewColorProperties(String vColorName, VisualizationColor vColor) {
		try {
			String value = getProperty(cDefaultColor+vColorName);
			if (value != null)
				vColor.setDefaultDataColor(Color.decode(value));

			value = getProperty(cMissingColor+vColorName);
			if (value != null)
				vColor.setMissingDataColor(Color.decode(value));

			value = getProperty(cColorColumn + vColorName);
			if (value != null) {
				int column = JVisualization.cColumnUnassigned;
	
					// to be compatible with format prior V2.7.0
				if (value.equals("colorBySimilarity"))
					column = mTableModel.findColumn(DescriptorConstants.DESCRIPTOR_FFP512.shortName);
	
				else if (value.startsWith("colorByHitlist")) {
					String hitlistName = value.substring(value.indexOf('\t')+1);
					int hitlistIndex = mTableModel.getListHandler().getListIndex(hitlistName);
					if (hitlistIndex != -1)
						column = CompoundTableListHandler.getColumnFromList(hitlistIndex);
					}
				else {
					column = mTableModel.findColumn(value);
					}
	
				if (column == JVisualization.cColumnUnassigned) {
					vColor.setColor(JVisualization.cColumnUnassigned);
					}
				else {
					Color[] colorList = null;
					value = getProperty(cColorCount + vColorName);
					if (value != null) {
						int colorCount = Integer.parseInt(value);
						colorList = new Color[colorCount];
						for (int j=0; j<colorCount; j++) {
							value = getProperty(cColor + vColorName + "_" + j);
							colorList[j] = Color.decode(value);
							}
						}
	
					value = getProperty(cColorListMode + vColorName);
					int colorListMode = VisualizationColor.cColorListModeStraight;	// default
					if (value != null) {
						if (value.equals("Categories"))
							colorListMode = VisualizationColor.cColorListModeCategories;
						else if (value.equals("HSBShort"))
							colorListMode = VisualizationColor.cColorListModeHSBShort;
						else if (value.equals("HSBLong"))
							colorListMode = VisualizationColor.cColorListModeHSBLong;
						else if (value.equals("straight"))
							colorListMode = VisualizationColor.cColorListModeStraight;
						}
	
					if (colorList == null) {	// cColorCount is only available if mode is cColorListModeCategory
						Color color1 = Color.decode(getProperty(cColor + vColorName + "_0"));
						Color color2 = Color.decode(getProperty(cColor + vColorName + "_1"));
						colorList = VisualizationColor.createColorWedge(color1, color2, colorListMode, null);
						}

					float[] thresholds = VisualizationColor.parseCustomThresholds(
							getProperty(cColorThresholds+vColorName), mTableModel, column);

					vColor.setColor(column, colorList, colorListMode, thresholds);
	
					value = getProperty(cColorMin + vColorName);
					float min = (value == null) ? Float.NaN : Float.parseFloat(value);
					value = getProperty(cColorMax + vColorName);
					float max = (value == null) ? Float.NaN : Float.parseFloat(value);
					if (!Double.isNaN(min) || !Double.isNaN(max))
						vColor.setColorRange(min, max);
					}
				}
			}
		catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(mParentPane.getParentFrame(), "Invalid color settings");
			}
		}

	private String updateFormObjectDescription(String description) {
			// convert form object keys referencing structure fields prior V2.7.0
		int index = description.indexOf("#structure#");
		if (index != -1)
			return description.substring(0, index)+"Structure"+description.substring(index+11);

		index = description.indexOf("#3Dstructure#");
		if (index != -1)
			return description.substring(0, index)+CompoundTableModel.cColumnType3DCoordinates+description.substring(index+11);

		if (description.startsWith("Chem Lab Journal")
		 && mTableModel.findColumn("Chem Lab Journal") == -1)
			description = "ELN/ExtRef" + description.substring(16);

		return description;
		}

	public void learn() {
		if (SwingUtilities.isEventDispatchThread()) {
			doLearn();
			}
		else {
			// if we are running macros, recently set properties (e.g. JSplitPane.deviderLocation) are not yet correct, if not queried in the EDT
			try {
				SwingUtilities.invokeAndWait(() -> doLearn() );
				}
			catch (Exception e) {
				e.printStackTrace();
				}
			}
		}

	private void doLearn() {
		super.learn();

		ArrayList<DETableRowTaskDef> rowTasks = mMainPane.getRowTaskList();
		if (!rowTasks.isEmpty()) {
			setProperty(cRowTaskCount, Integer.toString(rowTasks.size()));
			for (int i=0; i<rowTasks.size(); i++) {
				DETableRowTaskDef taskDef = rowTasks.get(i);
				setProperty(cRowTaskCode+i, taskDef.getTaskCode());
				String menu = taskDef.getParentMenu();
				if (menu != null)
					setProperty(cRowTaskMenu+i, menu);
				setProperty(cRowTaskItem+i, taskDef.getMenuItem());
				String detail = taskDef.getTaskConfig();
				if (detail != null)
					setProperty(cRowTaskConfig+i, detail);
				}
			}

		setProperty(cMainSplitting, DoubleFormat.toString(mParentPane.getMainSplitting()));
		setProperty(cRightSplitting, DoubleFormat.toString(mParentPane.getRightSplitting()));
		setProperty(cSelectedMainView, mMainPane.getSelectedViewTitle());
		setProperty(cDetailView, mDetailPane.getProperties());
		String[] dockInfo = mMainPane.getDockInfoSequence();
		setProperty(cMainViewCount, dockInfo == null ? "0" : Integer.toString(dockInfo.length));
		if (dockInfo != null) {
			for (int i=0; i<dockInfo.length; i++) {
				int dockInfoIndex = dockInfo[i].indexOf('\t');
				String title = dockInfo[i].substring(0, dockInfoIndex);
				String state = dockInfo[i].substring(dockInfoIndex+1);
				CompoundTableView view = mMainPane.getView(title);
				setProperty(cMainViewName+i, title);
				setProperty(cMainViewType+i, (view instanceof DETableView) ? cViewTypeTable
										   : (view instanceof VisualizationPanel2D) ? cViewType2D
										   : (view instanceof VisualizationPanel3D) ? cViewType3D
										   : (view instanceof JCardView) ? cViewTypeCards
										   : (view instanceof DEFormView) ? cViewTypeForm
										   : (view instanceof JStructureGrid) ? cViewTypeStructureGrid
										   : (view instanceof ExplanationView) ? cViewTypeExplanation
										   : (view instanceof DEMacroEditor) ? cViewTypeMacroEditor
										   : "UNKNOWN_VIEW");
				setProperty(cMainViewDockInfo+i, state);
				if (mMainPane.isInFrontInTabbedPane(title))
					setProperty(cMainViewInFront+i, "true");

				String viewName = "_"+title;
				if (view instanceof DETableView) {
// TODO store new format of view configurations for all views and translate from old to new configuration key names
//					mViewConfiguration[i] = new DETableConfiguration((DETableView)view, i, mTableModel);
//					mViewConfiguration[i].learn();

					DETable table = ((DETableView)view).getTable();
					setProperty(cTableRowHeight+viewName, Integer.toString(Math.round((float)table.getRowHeight()/HiDPIHelper.getUIScaleFactor())));
					int fontSize = table.getFontSize();
					if (fontSize != DETable.DEFAULT_FONT_SIZE)
						setProperty(cViewFontSize+viewName, ""+fontSize);
					int headerLines = ((DETableView)view).getHeaderLineCount();
					if (headerLines != 1)
						setProperty(cTableHeaderLines+viewName, ""+headerLines);

					// store column width for all visible columns
					for (int modelColumn=0; modelColumn<mTableModel.getColumnCount(); modelColumn++) {
						int column = mTableModel.convertFromDisplayableColumnIndex(modelColumn);
						setProperty(cTableColumnWidth+viewName+"_"+mTableModel.getColumnTitleNoAlias(column), ""+table.getColumnWidth(column));
						if (table.isTextWrapped(column))
							setProperty(cTableColumnWrapping+viewName+"_"+mTableModel.getColumnTitleNoAlias(column), "true");
						String columnVisibility = table.getExplicitColumnVisibilityString(column);
						if (columnVisibility != null)
							setProperty(cTableColumnVisibility+viewName+"_"+mTableModel.getColumnTitleNoAlias(column), columnVisibility);
						if (((DETableView)view).getColorHandler().hasColorAssigned(column, CompoundTableColorHandler.FOREGROUND))
							learnViewColorProperties(cTableText+viewName+mTableModel.getColumnTitleNoAlias(column),
									((DETableView)view).getColorHandler().getVisualizationColor(column, CompoundTableColorHandler.FOREGROUND));
						if (((DETableView)view).getColorHandler().hasColorAssigned(column, CompoundTableColorHandler.BACKGROUND))
							learnViewColorProperties(cTableBackground+viewName+mTableModel.getColumnTitleNoAlias(column),
									((DETableView)view).getColorHandler().getVisualizationColor(column, CompoundTableColorHandler.BACKGROUND));
						}

					String order = table.getColumnOrderString();
					if (order != null)
						setProperty(cTableColumnOrder+viewName, order);
					String filter = table.getColumnFilterText();
					if (filter != null)
						setProperty(cTableColumnFilter+viewName, filter);
					}
				if (view instanceof JCardView) {
					CardsViewConfiguration config = new CardsViewConfiguration(mTableModel);
					config.learn((JCardView)view);
					addViewConfiguration(title, config);
					}
				else if (view instanceof VisualizationPanel) {
					VisualizationPanel vpanel = (VisualizationPanel)view;
					JVisualization visualization = vpanel.getVisualization();

					VisualizationPanel master = vpanel.getSynchronizationMaster();
					if (master != null) {
						setProperty(cMasterView+viewName, mMainPane.getViewTitle(master));
						}
					else {
						int dimensions = vpanel.getDimensionCount();
						for (int j=0; j<dimensions; j++) {
									// popups assigning column to axis
							String key = cAxisColumn + viewName + "_" + j;
							setProperty(key, vpanel.getAxisColumnName(j));

							// setting visible range the new way (after 20-Feb-2017)
							JPruningBar pbar = vpanel.getPruningBar(j);
							if (pbar.getLowValue() != pbar.getMinimumValue()) {
								key = cAxisMin + viewName + "_" + j;
								float value = visualization.getVisibleMin(j);
								if (visualization.isLogarithmicAxis(j))
									value = (float)Math.pow(10, value);
								setProperty(key, ""+value);
								}
							if (pbar.getHighValue() != pbar.getMaximumValue()) {
								key = cAxisMax + viewName + "_" + j;
								float value = visualization.getVisibleMax(j);
								if (visualization.isLogarithmicAxis(j))
									value = (float)Math.pow(10, value);
								setProperty(key, ""+value);
								}
							if (vpanel.getAutoZoomFactor() != 0f) {
								key = cCachedAxisMin + viewName + "_" + j;
								setProperty(key, ""+vpanel.getCachedPruningBarLow(j));
								key = cCachedAxisMax + viewName + "_" + j;
								setProperty(key, ""+vpanel.getCachedPruningBarHigh(j));
								}
							}

						if (view instanceof VisualizationPanel3D) {
							float[][] rotation = ((JVisualization3D)visualization).getRotationMatrix();
							for (int j=0; j<3; j++)
								for (int k=0; k<3; k++)
									setProperty(cRotation+viewName+j+k, DoubleFormat.toString(rotation[j][k]));
							}
						}

					setProperty(cScatterplotMargin+viewName, ""+visualization.getScatterPlotMargin());

					if (visualization.getFontSize() != 1.0)
						setProperty(cViewFontSize+viewName, ""+visualization.getFontSize());

					if (visualization.getFontSizeMode() != JVisualization.cFontSizeModeRelative)
						setProperty(cViewFontSizeMode+viewName, JVisualization.FONT_SIZE_MODE_CODE[visualization.getFontSizeMode()]);

					if (!visualization.getViewBackground().equals(Color.WHITE))
						setProperty(cViewBackground+viewName, ""+visualization.getViewBackground().getRGB());

					if (!visualization.getDefaultLabelBackground().equals(JVisualization.DEFAULT_LABEL_BACKGROUND))
						setProperty(cDefaultLabelBackground +viewName, ""+visualization.getDefaultLabelBackground().getRGB());

					if (visualization.getLabelTransparency() != DEFAULT_LABEL_TRANSPARENCY)
						setProperty(cLabelTransparency+viewName, DoubleFormat.toString(visualization.getLabelTransparency()));

					if (view instanceof VisualizationPanel3D) {
						Color faceColor = ((JVisualization3D) visualization).getGraphFaceColor();
						if (!faceColor.equals(JVisualization3D.DEFAULT_GRAPH_FACE_COLOR))
							setProperty(cFaceColor3D + viewName, "" + faceColor.getRGB());
						}

					if (visualization.isSplitViewConfigured())
						setProperty(cTitleBackground+viewName, ""+visualization.getTitleBackground().getRGB());

					if (visualization.getJittering() != 0.0) {
						setProperty(cJittering + viewName, "" + visualization.getJittering());
						setProperty(cJitterAxes + viewName, "" + visualization.getJitterAxes());
						}

					setProperty(cFastRendering+viewName, visualization.isFastRendering() ? "true" : "false");

					if (visualization instanceof JVisualization2D && ((JVisualization2D)visualization).isDrawBarPieBoxOutline())
						setProperty(cDrawBoxOutline+viewName, "true");

					if (visualization instanceof JVisualization2D && !((JVisualization2D)visualization).isDrawMarkerOutline())
						setProperty(cDrawMarkerOutline+viewName, "false");

					if (visualization.getMarkerSize() != 1.0)
						setProperty(cMarkerSize+viewName, ""+visualization.getMarkerSize());

					if (visualization.getMarkerSizeInversion())
						setProperty(cSizeInversion+viewName, "true");

					if (visualization.getMarkerSizeProportional())
						setProperty(cSizeProportional+viewName, "true");

					if (!visualization.isMarkerSizeZoomAdapted())
						setProperty(cSizeAdaption+viewName, "false");

					int column = visualization.getMarkerSizeColumn();
					if (column != JVisualization.cColumnUnassigned) {
						String key = cSizeColumn+viewName;
						if (CompoundTableListHandler.isListColumn(column))
							setProperty(key, "sizeByHitlist\t"
									+ mTableModel.getListHandler().getListName(
											CompoundTableListHandler.convertToListIndex(column)));
						else {
							setProperty(key, mTableModel.getColumnTitleNoAlias(column));
							float min = visualization.getMarkerSizeMin();
							if (!Float.isNaN(min))
								setProperty(cMarkerSizeMin+viewName, Float.toString(min));
							float max = visualization.getMarkerSizeMax();
							if (!Float.isNaN(max))
								setProperty(cMarkerSizeMax+viewName, Float.toString(max));
							}
						}

					learnViewColorProperties(viewName, visualization.getMarkerColor());
					learnViewColorProperties(cLabelBackground+viewName, visualization.getLabelBackgroundColor());

					column = visualization.getMarkerShapeColumn();
					if (column != JVisualization.cColumnUnassigned) {
						if (CompoundTableListHandler.isListColumn(column))
							setProperty(cShapeColumn+viewName, "shapeByHitlist\t"
									+ mTableModel.getListHandler().getListName(
											CompoundTableListHandler.convertToListIndex(column)));
						else
							setProperty(cShapeColumn+viewName, mTableModel.getColumnTitleNoAlias(column));
						}

					if (visualization.isCaseSeparationDone()) {
						int csColumn = visualization.getCaseSeparationColumn();
						if (csColumn != JVisualization.cColumnUnassigned) {
							if (CompoundTableListHandler.isListColumn(csColumn))
								setProperty(cCaseSeparationColumn+viewName, "splitByHitlist\t"
										+ mTableModel.getListHandler().getListName(
												CompoundTableListHandler.convertToListIndex(csColumn)));
							else
								setProperty(cCaseSeparationColumn+viewName, mTableModel.getColumnTitleNoAlias(csColumn));
							setProperty(cCaseSeparationValue+viewName, ""+visualization.getCaseSeparationValue());
							}
						}

					int[] sc = visualization.getSplittingColumns();
					if (sc[0] != JVisualization.cColumnUnassigned) {
						if (CompoundTableListHandler.isListColumn(sc[0]))
							setProperty(cSplitViewColumn1+viewName, "splitByHitlist\t"
									+ mTableModel.getListHandler().getListName(
											CompoundTableListHandler.convertToListIndex(sc[0])));
						else
							setProperty(cSplitViewColumn1+viewName, mTableModel.getColumnTitleNoAlias(sc[0]));
						setProperty(cSplitViewAspect+viewName, ""+visualization.getSplittingAspectRatio());
						setProperty(cSplitViewShowEmpty+viewName, visualization.isShowEmptyInSplitView() ? "true" : "false");
						}
					if (sc[1] != JVisualization.cColumnUnassigned) {
						if (CompoundTableListHandler.isListColumn(sc[1]))
							setProperty(cSplitViewColumn2+viewName, "splitByHitlist\t"
									+ mTableModel.getListHandler().getListName(
											CompoundTableListHandler.convertToListIndex(sc[1])));
						else
							setProperty(cSplitViewColumn2+viewName, mTableModel.getColumnTitleNoAlias(sc[1]));
						}

					learnMarkerLabelDisplayerProperties(viewName, visualization);

					int type = visualization.getChartType();
					setProperty(cChartType+viewName, JVisualization.CHART_TYPE_CODE[type]);
					if (type == JVisualization.cChartTypeBars || type == JVisualization.cChartTypePies) {
						int mode = visualization.getPreferredChartMode();
						setProperty(cChartMode+viewName, JVisualization.CHART_MODE_CODE[mode]);
						if (mode != JVisualization.cChartModeCount && mode != JVisualization.cChartModePercent) {
							column = visualization.getPreferredChartColumn();
							setProperty(cChartColumn+viewName, mTableModel.getColumnTitleNoAlias(column));
							}
						}

					column = visualization.getConnectionColumn();
					if (column != JVisualization.cColumnUnassigned) {
						setProperty(cConnectionColumn1+viewName,
								(column == JVisualization.cConnectionColumnConnectAll) ? cConnectionColumnConnectAll
							  : (column == JVisualization.cConnectionColumnConnectCases) ? cConnectionColumnConnectCases
							  : mTableModel.getColumnTitleNoAlias(column));
						column = visualization.getConnectionOrderColumn();
						if (column != JVisualization.cColumnUnassigned)
							setProperty(cConnectionColumn2+viewName, mTableModel.getColumnTitleNoAlias(column));
						if (visualization.isConnectionLineInverted())
							setProperty(cConnectionArrows+viewName, "inverted");
						double lineWidth = visualization.getConnectionLineWidth();
						if (lineWidth != 1.0)
							setProperty(cConnectionLineWidth+viewName, ""+lineWidth);

						int listMode = visualization.getConnectionLineListMode();
						if (listMode != JVisualization.cConnectionListModeNone) {
							int list1 = visualization.getConnectionLineList1();
							int list2 = visualization.getConnectionLineList2();
							setProperty(cConnectionLineListMode+viewName, JVisualization.cConnectionListModeCode[listMode]);
							setProperty(cConnectionLineList1+viewName, mTableModel.getListHandler().getListName(list1));
							if (list2 != -1)
								setProperty(cConnectionLineList2+viewName, mTableModel.getListHandler().getListName(list2));
							}

						if (visualization.getTreeViewMode() != JVisualization.cTreeViewModeNone) {
							setProperty(cTreeViewMode+viewName, JVisualization.TREE_VIEW_MODE_CODE[visualization.getTreeViewMode()]);
							setProperty(cTreeViewRadius+viewName, ""+visualization.getTreeViewRadius());
							setProperty(cTreeViewShowAll+viewName, visualization.isTreeViewShowAll() ? "true" : "false");
							setProperty(cTreeViewIsDynamic+viewName, visualization.isTreeViewDynamic() ? "true" : "false");
							setProperty(cTreeViewIsInverted+viewName, visualization.isTreeViewInverted() ? "true" : "false");
							}
						}

					float azf = vpanel.getAutoZoomFactor();
					if (azf != 0) {
						setProperty(cAutoZoomFactor+viewName, Float.toString(azf));
						int azc = vpanel.getAutoZoomColumn();
						if (azc != -1)
							setProperty(cAutoZoomColumn+viewName, mTableModel.getColumnTitleNoAlias(azc));
						}

					if (visualization.isUsedAsFilter())
						setProperty(cUsedAsFilter+viewName, "true");

					if (visualization.getShowNaNValues())
						setProperty(cShowNaNValues+viewName, "true");

					if (visualization.isShowLabelBackground())
						setProperty(cShowLabelBackground+viewName, "true");

					if (!visualization.getAffectGlobalExclusion())
						setProperty(cAffectGlobalExclusion+viewName, "false");

					if (visualization.isIgnoreGlobalExclusion())
						setProperty(cIgnoreGlobalExclusion+viewName, "true");

					if (visualization.getLocalExclusionList() != -1)
						setProperty(cLocalExclusionList+viewName, mTableModel.getListHandler().getListName(visualization.getLocalExclusionList()));

					if (visualization.getGridMode() != JVisualization.cGridModeShown)
						setProperty(cSuppressGrid+viewName, JVisualization.GRID_MODE_CODE[visualization.getGridMode()]);

					if (visualization.isLegendSuppressed())
						setProperty(cSuppressLegend+viewName, "true");

					if (!visualization.isDynamicScale())
						setProperty(cDynamicScale+viewName, "false");

					if (visualization.getScaleMode() != JVisualization.cScaleModeShown)
						setProperty(cSuppressScale+viewName, JVisualization.SCALE_MODE_CODE[visualization.getScaleMode()]);

					setProperty(cScaleStyle+viewName, JVisualization.SCALE_STYLE_CODE[visualization.getScaleStyle()]);

					if (visualization.isShowBarOrPieSizeValue())
						setProperty(cShowBarOrPieSizeValue+viewName, "true");

					if (visualization.isShowStandardDeviation())
						setProperty(cShowStdDev+viewName, "true");

					if (visualization.isShowConfidenceInterval())
						setProperty(cShowConfInterval+viewName, "true");

					if (visualization.isShowValueCount())
						setProperty(cShowValueCount+viewName, "true");

					if (visualization.isShowFoldChange())
						setProperty(cShowFoldChange+viewName, "true");

					if (visualization.isShowPValue())
						setProperty(cShowPValue+viewName, "true");

					column = visualization.getPValueColumn();
					if (column != JVisualization.cColumnUnassigned) {
						setProperty(cPValueColumn+viewName, mTableModel.getColumnTitleNoAlias(column));
						setProperty(cPValueRefCategory+viewName, visualization.getPValueRefCategory());
						}

					int boxplotMeanMode = visualization.getBoxplotMeanMode();
					if (boxplotMeanMode != JVisualization.BOXPLOT_DEFAULT_MEAN_MODE)
						setProperty(cBoxplotMeanMode+viewName, JVisualization.BOXPLOT_MEAN_MODE_CODE[boxplotMeanMode]);

					if (visualization.isShowMeanAndMedianValues())
						setProperty(cBoxplotShowMeanValues+viewName, "true");

					if (view instanceof VisualizationPanel2D) {
						double transparency = ((JVisualization2D)visualization).getMarkerTransparency();
						double transparency1 = ((JVisualization2D)visualization).getMarkerLabelTransparency();
						double transparency2 = ((JVisualization2D)visualization).getConnectionLineTransparency();
						if (transparency != 0.0 || transparency1 != 0 || transparency2 != 0) {
							setProperty(cMarkerTransparency+viewName, ""+transparency);

							if (transparency1 != transparency)
								setProperty(cMarkerLabelTransparency+viewName, ""+transparency1);

							if (transparency2 != transparency)
								setProperty(cConnectionLineTransparency+viewName, ""+transparency2);
							}

						if (((JVisualization2D)visualization).getCrossHairMode() != JVisualization2D.CROSSHAIR_MODE_AUTOMATIC)
							setProperty(cCrossHairMode+viewName, JVisualization2D.CROSSHAIR_MODE_CODE[((JVisualization2D)visualization).getCrossHairMode()]);

						String crossHairList = ((JVisualization2D)visualization).getCrossHairList();
						if (crossHairList != null)
							setProperty(cCrossHairList+viewName, crossHairList);

						int[] multiValueMarkerColumn = ((JVisualization2D)visualization).getMultiValueMarkerColumns();
						if (multiValueMarkerColumn != null) {
							int multiValueMarkerMode = ((JVisualization2D)visualization).getMultiValueMarkerMode();
							setProperty(cMultiValueMarkerMode+viewName, JVisualization2D.MULTI_VALUE_MARKER_MODE_CODE[multiValueMarkerMode]);
							StringBuilder columnNames = new StringBuilder(mTableModel.getColumnTitleNoAlias(multiValueMarkerColumn[0]));
							for (int j=1; j<multiValueMarkerColumn.length; j++)
								columnNames.append('\t').append(mTableModel.getColumnTitleNoAlias(multiValueMarkerColumn[j]));
							setProperty(cMultiValueMarkerColumns+viewName, ""+columnNames.toString());
							}

						column = ((JVisualization2D)visualization).getBackgroundColor().getColorColumn();
						if (column != JVisualization.cColumnUnassigned) {
							String key = cBackgroundColorColumn+viewName;
							if (CompoundTableListHandler.isListColumn(column))
								setProperty(key, "colorByHitlist\t"
										+ mTableModel.getListHandler().getListName(
												CompoundTableListHandler.convertToListIndex(column)));
							else {
								setProperty(key, mTableModel.getColumnTitleNoAlias(column));
								}

							int mode = ((JVisualization2D)visualization).getBackgroundColor().getColorListMode();
							key = cBackgroundColorListMode+viewName;
							if (mode == VisualizationColor.cColorListModeCategories)
								setProperty(key, "Categories");
							else if (mode == VisualizationColor.cColorListModeHSBShort)
								setProperty(key, "HSBShort");
							else if (mode == VisualizationColor.cColorListModeHSBLong)
								setProperty(key, "HSBLong");
							else if (mode == VisualizationColor.cColorListModeStraight)
								setProperty(key, "straight");

							Color[] colorList = ((JVisualization2D)visualization).getBackgroundColor().getColorListWithoutDefaults();
							if (mode == VisualizationColor.cColorListModeCategories) {
								setProperty(cBackgroundColorCount+viewName, ""+colorList.length);
								for (int j=0; j<colorList.length; j++)
									setProperty(cBackgroundColor+viewName+"_"+j, ""+colorList[j].getRGB());
								}
							else {
								setProperty(cBackgroundColor+viewName+"_0", ""+colorList[0].getRGB());
								setProperty(cBackgroundColor+viewName+"_1", ""+colorList[colorList.length-1].getRGB());
								}

							int hitlist = ((JVisualization2D)visualization).getBackgroundColorConsidered();
							String value = (hitlist == JVisualization2D.BACKGROUND_VISIBLE_RECORDS) ? "visibleRecords"
										 : (hitlist == JVisualization2D.BACKGROUND_ALL_RECORDS) ? "allRecords"
										 : "fromHitlist\t" + mTableModel.getListHandler().getListName(hitlist);
							setProperty(cBackgroundColorRecords, value);
							setProperty(cBackgroundColorRadius+viewName, DoubleFormat.toString(((JVisualization2D)visualization).getBackgroundColorRadius()));
							setProperty(cBackgroundColorFading+viewName, DoubleFormat.toString(((JVisualization2D)visualization).getBackgroundColorFading()));

							if (!Double.isNaN(((JVisualization2D)visualization).getBackgroundColor().getColorMin()))
								setProperty(cBackgroundColorMin+viewName, ""+((JVisualization2D)visualization).getBackgroundColor().getColorMin());
							if (!Double.isNaN(((JVisualization2D)visualization).getBackgroundColor().getColorMax()))
								setProperty(cBackgroundColorMax+viewName, ""+((JVisualization2D)visualization).getBackgroundColor().getColorMax());

							String thresholds = ((JVisualization2D)visualization).getBackgroundColor().getColorThresholdString();
							if (thresholds != null)
								setProperty(cBackgroundColorThresholds+viewName, thresholds);
							}

						byte[] backgroundImageData = ((JVisualization2D)visualization).getBackgroundImageData();
						if (backgroundImageData != null)
							setBinary(cBackgroundImage+viewName, backgroundImageData);

						int curveMode = ((JVisualization2D)visualization).getCurveMode();
						if (curveMode != 0) {
							setProperty(cCurveMode+viewName, JVisualization2D.CURVE_MODE_CODE[curveMode]);
							int rowList = ((JVisualization2D)visualization).getCurveRowList();
							if (rowList != JVisualization2D.cCurveRowListVisible)
								setProperty(cCurveRowList+viewName, rowList == JVisualization2D.cCurveRowListSelected ?
										JVisualization2D.ROW_LIST_CODE_SELECTED : mTableModel.getListHandler().getListName(rowList));
							if (((JVisualization2D)visualization).isShowStandardDeviationArea())
								setProperty(cCurveStdDev+viewName, "true");
							if (((JVisualization2D)visualization).isCurveSplitByColorCategory())
								setProperty(cCurveSplitByCategory+viewName, "true");
							if (((JVisualization2D)visualization).isCurveAreaTruncated())
								setProperty(cCurveTruncate+viewName, "true");
							int curveSplitColumn = ((JVisualization2D)visualization).getCurveSplitSecondCategoryColumn();
							if (curveSplitColumn != cColumnUnassigned)
								setProperty(cCurveSplitCategoryColumn+viewName, mTableModel.getColumnTitleNoAlias(curveSplitColumn));
							float curveLineWidth = ((JVisualization2D)visualization).getCurveLineWidth();
							setProperty(cCurveLineWidth+viewName, ""+curveLineWidth);
							if (curveMode == JVisualization2D.cCurveModeByFormulaShow
							 || curveMode == JVisualization2D.cCurveModeByFormulaHide)
								setProperty(cCurveExpression+viewName, ((JVisualization2D)visualization).getCurveExpression());
							if (curveMode == JVisualization2D.cCurveModeSmooth)
								setProperty(cCurveSmoothing+viewName, ""+((JVisualization2D)visualization).getCurveSmoothing());
							}

						int correlationType = ((JVisualization2D)visualization).getShownCorrelationType();
						if (correlationType != CorrelationCalculator.TYPE_NONE) {
							setProperty(cCorrelationCoefficient+viewName, CorrelationCalculator.TYPE_NAME[correlationType]);
							}
						}
					}
				else if (view instanceof DEFormView) {
					JCompoundTableForm form = ((DEFormView)view).getCompoundTableForm();

					int fontSize = form.getFontSize();
					if (fontSize != JCompoundTableForm.DEFAULT_FONT_SIZE)
						setProperty(cViewFontSize+viewName, ""+fontSize);

					setProperty(cFormLayout+viewName, form.getFormLayoutDescriptor());

					setProperty(cFormObjectCount+viewName, ""+form.getFormObjectCount());
					for (int j=0; j<form.getFormObjectCount(); j++)
						setProperty(cFormObjectInfo+viewName+"_"+j, ""+form.getFormObjectDescriptor(j));
					}
				else if (view instanceof JStructureGrid) {
					learnMarkerLabelDisplayerProperties(viewName, (JStructureGrid)view);

					int structureGridColumns = ((JStructureGrid)view).getColumnCount();
					setProperty(cStructureGridColumns+viewName, ""+structureGridColumns);

					String structureGridColumn = mTableModel.getColumnTitleNoAlias(((JStructureGrid)view).getStructureColumn());
					setProperty(cStructureGridColumn+viewName, ""+structureGridColumn);

					int stereoMode = ((JStructureGrid)view).getStructureDisplayMode() & DETaskSetStructureDisplayMode.STEREO_MODE_MASK;
					if (stereoMode != DETaskSetStructureDisplayMode.DEFAULT_STEREO_MODE) {
						int stereoModeIndex = DETaskSetStructureDisplayMode.findStereoModeIndex(stereoMode);
						setProperty(cStructureDisplayStereoMode + viewName, DETaskSetStructureDisplayMode.STEREO_MODE_CODE[stereoModeIndex]);
						}

					int colorMode = ((JStructureGrid)view).getStructureDisplayMode() & DETaskSetStructureDisplayMode.COLOR_MODE_MASK;
					if (colorMode != DETaskSetStructureDisplayMode.DEFAULT_COLOR_MODE) {
						int colorModeIndex = DETaskSetStructureDisplayMode.findColorModeIndex(colorMode);
						setProperty(cStructureDisplayColorMode + viewName, DETaskSetStructureDisplayMode.COLOR_MODE_CODE[colorModeIndex]);
						}
					}

				if (view instanceof FocusableView) {
					if (((FocusableView)view).getFocusList() == FocusableView.cFocusOnSelection)
						setProperty(cFocusList +viewName, cListIsSelection);
					else if (((FocusableView)view).getFocusList() != FocusableView.cFocusNone)
						setProperty(cFocusList +viewName, mTableModel.getListHandler().getListNames()[((FocusableView)view).getFocusList()]);
					}
				}
			}

		for (int i=0; i<mPruningPanel.getFilterCount(); i++) {
			JFilterPanel filter = mPruningPanel.getFilter(i);

			String property = mPruningPanel.getFilterSettings(filter);
			setProperty(cFilter+i, property);
			}

		for (int i=0; i<mPruningPanel.getFilterCount(); i++) {
			JFilterPanel filter = mPruningPanel.getFilter(i);

			String property = filter.getAnimationSettings();
			if (property != null)
				setProperty(cFilterAnimation+i, property);
			}
		}

	private void learnViewColorProperties(String vColorName, VisualizationColor vColor) {
		if (!vColor.isDefaultDefaultDataColor())
			setProperty(cDefaultColor+vColorName, ""+vColor.getDefaultDataColor().getRGB());

		if (!vColor.isDefaultMissingDataColor())
			setProperty(cMissingColor+vColorName, ""+vColor.getMissingDataColor().getRGB());

		int column = vColor.getColorColumn();
		if (column != JVisualization.cColumnUnassigned) {
			String key = cColorColumn+vColorName;
			if (CompoundTableListHandler.isListColumn(column))
				setProperty(key, "colorByHitlist\t"
						+ mTableModel.getListHandler().getListName(
								CompoundTableListHandler.convertToListIndex(column)));
			else {
				setProperty(key, mTableModel.getColumnTitleNoAlias(column));
				}
	
			int mode = vColor.getColorListMode();
			key = cColorListMode+vColorName;
			if (mode == VisualizationColor.cColorListModeCategories)
				setProperty(key, "Categories");
			else if (mode == VisualizationColor.cColorListModeHSBShort)
				setProperty(key, "HSBShort");
			else if (mode == VisualizationColor.cColorListModeHSBLong)
				setProperty(key, "HSBLong");
			else if (mode == VisualizationColor.cColorListModeStraight)
				setProperty(key, "straight");
	
			Color[] colorList = vColor.getColorListWithoutDefaults();
			if (mode == VisualizationColor.cColorListModeCategories) {
				setProperty(cColorCount+vColorName, ""+colorList.length);
				for (int j=0; j<colorList.length; j++)
					setProperty(cColor+vColorName+"_"+j, ""+colorList[j].getRGB());
				}
			else {
				setProperty(cColor+vColorName+"_0", ""+colorList[0].getRGB());
				setProperty(cColor+vColorName+"_1", ""+colorList[colorList.length-1].getRGB());
				}
	
			if (!Double.isNaN(vColor.getColorMin()))
				setProperty(cColorMin+vColorName, ""+vColor.getColorMin());
			if (!Double.isNaN(vColor.getColorMax()))
				setProperty(cColorMax+vColorName, ""+vColor.getColorMax());
			String thresholds = vColor.getColorThresholdString();
			if (thresholds != null)
				setProperty(cColorThresholds+vColorName, thresholds);
			}
		}

	private void learnMarkerLabelDisplayerProperties(String viewName, MarkerLabelDisplayer displayer) {
		boolean labelsUsed = false;
		for (int i=0; i<MarkerLabelDisplayer.cPositionCode.length; i++) {
			int column = displayer.getMarkerLabelColumn(i);
			if (column != -1) {
				String columnKey = cLabelColumn+viewName+"_"+MarkerLabelDisplayer.cPositionCode[i];
				setProperty(columnKey, mTableModel.getColumnTitleNoAlias(column));
				labelsUsed = true;
				}
			}

		if (labelsUsed) {
			if (displayer.getMarkerLabelList() == MarkerLabelDisplayer.cLabelsOnSelection)
				setProperty(cLabelList+viewName, cListIsSelection);
			else if (displayer.getMarkerLabelList() != JVisualization.cLabelsOnAllRows)
				setProperty(cLabelList +viewName, mTableModel.getListHandler().getListNames()[displayer.getMarkerLabelList()]);

			int[] opc = displayer.getMarkerLabelOnePerCategory();
			if (opc != null && opc[0] != -1 && opc[1] != -1) {
				setProperty(cLabelOPCCategory+viewName, mTableModel.getColumnTitleNoAlias(opc[0]));
				setProperty(cLabelOPCValue+viewName, mTableModel.getColumnTitleNoAlias(opc[1]));
				setProperty(cLabelOPCMode+viewName, MarkerLabelDisplayer.cOnePerCategoryMode[opc[2]]);
				}

			if (displayer.isMarkerLabelsInTreeViewOnly())
				setProperty(cLabelMode+viewName, "inDetailGraphOnly");

			double size = displayer.getMarkerLabelSize();
			if (size != 1.0)
				setProperty(cLabelSize+viewName, ""+size);

			boolean hideColumnNameInTable = !displayer.isShowColumnNameInTable();
			if (hideColumnNameInTable)
				setProperty(cLabelShowColumnNameInTable+viewName, "false");

			boolean optimizePositions = displayer.isOptimizeLabelPositions();
			if (optimizePositions)
				setProperty(cLabelPositionOptimization+viewName, "true");

			boolean blackOrWhite = displayer.isMarkerLabelBlackOrWhite();
			if (blackOrWhite)
				setProperty(cLabelBlackOrWhite+viewName, "true");
			}
		}

	public int decodeProperty(String key, String[] option) {
		String value = getProperty(key);
		if (value != null)
			for (int i=0; i<option.length; i++)
				if (value.equals(option[i]))
					return i;
		return -1;
		}
	}