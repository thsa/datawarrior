package com.actelion.research.table.view.config;

import com.actelion.research.calc.CorrelationCalculator;
import com.actelion.research.gui.JPruningBar;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.*;
import com.actelion.research.util.DoubleFormat;

import static com.actelion.research.table.view.JVisualization.DEFAULT_LABEL_TRANSPARENCY;

public abstract class VPanelConfiguration extends ViewConfiguration<VisualizationPanel> {
	public VPanelConfiguration(CompoundTableModel tableModel) {
		super(tableModel);
		}

	@Override
	public void apply(VisualizationPanel vpanel) {
	}

	@Override
	public void learn(VisualizationPanel vpanel) {/*
		JVisualization visualization = mVPanel.getVisualization();

		VisualizationPanel master = mVPanel.getSynchronizationMaster();
		if (master != null) {
			setProperty(cMasterView, mMainPane.getViewTitle(master));
		}
		else {
			int dimensions = vpanel.getDimensionCount();
			for (int j=0; j<dimensions; j++) {
				// popups assigning column to axis
				String key = cAxisColumn+"_"+j;
				setProperty(key, vpanel.getAxisColumnName(j));

				// setting visible range the new way (after 20-Feb-2017)
				JPruningBar pbar = vpanel.getPruningBar(j);
				if (pbar.getLowValue() != pbar.getMinimumValue()) {
					key = cAxisMin+"_"+j;
					setProperty(key, ""+visualization.getVisibleMin(j));
				}
				if (pbar.getHighValue() != pbar.getMaximumValue()) {
					key = cAxisMax+"_"+j;
					setProperty(key, ""+visualization.getVisibleMax(j));
				}
			}

			if (view instanceof VisualizationPanel3D) {
				float[][] rotation = ((JVisualization3D)visualization).getRotationMatrix();
				for (int j=0; j<3; j++)
					for (int k=0; k<3; k++)
						setProperty(cRotation+j+k, DoubleFormat.toString(rotation[j][k]));
			}
		}

		if (visualization.getFontSize() != 1.0)
			setProperty(cViewFontSize, ""+visualization.getFontSize());

		if (!visualization.getViewBackground().equals(Color.WHITE))
			setProperty(cViewBackground, ""+visualization.getViewBackground().getRGB());

		if (!visualization.getLabelBackground().equals(JVisualization.DEFAULT_LABEL_BACKGROUND))
			setProperty(cLabelBackground, ""+visualization.getLabelBackground().getRGB());

		if (visualization.getLabelTransparency() != DEFAULT_LABEL_TRANSPARENCY)
			setProperty(cLabelTransparency, DoubleFormat.toString(visualization.getLabelTransparency()));

		if (view instanceof VisualizationPanel3D) {
			Color faceColor = ((JVisualization3D) visualization).getGraphFaceColor();
			if (!faceColor.equals(JVisualization3D.DEFAULT_GRAPH_FACE_COLOR))
				setProperty(cFaceColor3D, "" + faceColor.getRGB());
		}

		if (visualization.isSplitViewConfigured())
			setProperty(cTitleBackground, ""+visualization.getTitleBackground().getRGB());

		if (visualization.getJittering() != 0.0) {
			setProperty(cJittering, "" + visualization.getJittering());
			setProperty(cJitterAxes, "" + visualization.getJitterAxes());
		}

		if (visualization.isFastRendering())
			setProperty(cFastRendering, "true");

		if (visualization.getMarkerSize() != 1.0)
			setProperty(cMarkerSize, ""+visualization.getMarkerSize());

		if (visualization.getMarkerSizeInversion())
			setProperty(cSizeInversion, "true");

		if (visualization.getMarkerSizeProportional())
			setProperty(cSizeProportional, "true");

		if (!visualization.isMarkerSizeZoomAdapted())
			setProperty(cSizeAdaption, "false");

		int column = visualization.getMarkerSizeColumn();
		if (column != JVisualization.cColumnUnassigned) {
			String key = cSizeColumn;
			if (CompoundTableListHandler.isListColumn(column))
				setProperty(key, "sizeByHitlist\t"
						+ mTableModel.getListHandler().getListName(
						CompoundTableListHandler.convertToListIndex(column)));
			else {
				setProperty(key, mTableModel.getColumnTitleNoAlias(column));
			}
		}

		learnViewColorProperties(viewName, visualization.getMarkerColor());

		column = visualization.getMarkerShapeColumn();
		if (column != JVisualization.cColumnUnassigned) {
			if (CompoundTableListHandler.isListColumn(column))
				setProperty(cShapeColumn, "shapeByHitlist\t"
						+ mTableModel.getListHandler().getListName(
						CompoundTableListHandler.convertToListIndex(column)));
			else
				setProperty(cShapeColumn, mTableModel.getColumnTitleNoAlias(column));
		}

		if (visualization.isCaseSeparationDone()) {
			int csColumn = visualization.getCaseSeparationColumn();
			if (csColumn != JVisualization.cColumnUnassigned) {
				if (CompoundTableListHandler.isListColumn(csColumn))
					setProperty(cCaseSeparationColumn, "splitByHitlist\t"
							+ mTableModel.getListHandler().getListName(
							CompoundTableListHandler.convertToListIndex(csColumn)));
				else
					setProperty(cCaseSeparationColumn, mTableModel.getColumnTitleNoAlias(csColumn));
				setProperty(cCaseSeparationValue, ""+visualization.getCaseSeparationValue());
			}
		}

		int[] sc = visualization.getSplittingColumns();
		if (sc[0] != JVisualization.cColumnUnassigned) {
			if (CompoundTableListHandler.isListColumn(sc[0]))
				setProperty(cSplitViewColumn1, "splitByHitlist\t"
						+ mTableModel.getListHandler().getListName(
						CompoundTableListHandler.convertToListIndex(sc[0])));
			else
				setProperty(cSplitViewColumn1, mTableModel.getColumnTitleNoAlias(sc[0]));
			setProperty(cSplitViewAspect, ""+visualization.getSplittingAspectRatio());
			setProperty(cSplitViewShowEmpty, visualization.isShowEmptyInSplitView() ? "true" : "false");
		}
		if (sc[1] != JVisualization.cColumnUnassigned) {
			if (CompoundTableListHandler.isListColumn(sc[1]))
				setProperty(cSplitViewColumn2, "splitByHitlist\t"
						+ mTableModel.getListHandler().getListName(
						CompoundTableListHandler.convertToListIndex(sc[1])));
			else
				setProperty(cSplitViewColumn2, mTableModel.getColumnTitleNoAlias(sc[1]));
		}

		learnMarkerLabelDisplayerProperties(viewName, visualization);

		int type = visualization.getChartType();
		setProperty(cChartType, JVisualization.CHART_TYPE_CODE[type]);
		if (type == JVisualization.cChartTypeBars || type == JVisualization.cChartTypePies) {
			int mode = visualization.getPreferredChartMode();
			setProperty(cChartMode, JVisualization.CHART_MODE_CODE[mode]);
			if (mode != JVisualization.cChartModeCount && mode != JVisualization.cChartModePercent) {
				column = visualization.getPreferredChartColumn();
				setProperty(cChartColumn, mTableModel.getColumnTitleNoAlias(column));
			}
		}

		column = visualization.getConnectionColumn();
		if (column != JVisualization.cColumnUnassigned) {
			setProperty(cConnectionColumn1,
					(column == JVisualization.cConnectionColumnConnectAll) ? cConnectionColumnConnectAll
							: (column == JVisualization.cConnectionColumnConnectCases) ? cConnectionColumnConnectCases
							: mTableModel.getColumnTitleNoAlias(column));
			column = visualization.getConnectionOrderColumn();
			if (column != JVisualization.cColumnUnassigned)
				setProperty(cConnectionColumn2, mTableModel.getColumnTitleNoAlias(column));
			if (visualization.isConnectionLineInverted())
				setProperty(cConnectionArrows, "inverted");
			double lineWidth = visualization.getConnectionLineWidth();
			if (lineWidth != 1.0)
				setProperty(cConnectionLineWidth, ""+lineWidth);

			if (visualization.getTreeViewMode() != JVisualization.cTreeViewModeNone) {
				setProperty(cTreeViewMode, JVisualization.TREE_VIEW_MODE_CODE[visualization.getTreeViewMode()]);
				setProperty(cTreeViewRadius, ""+visualization.getTreeViewRadius());
				setProperty(cTreeViewShowAll, visualization.isTreeViewShowAll() ? "true" : "false");
				setProperty(cTreeViewIsDynamic, visualization.isTreeViewDynamic() ? "true" : "false");
				setProperty(cTreeViewIsInverted, visualization.isTreeViewInverted() ? "true" : "false");
			}
		}

		if (visualization.isUsedAsFilter())
			setProperty(cUsedAsFilter, "true");

		if (visualization.getShowNaNValues())
			setProperty(cShowNaNValues, "true");

		if (visualization.isShowLabelBackground())
			setProperty(cShowLabelBackground, "true");

		if (!visualization.getAffectGlobalExclusion())
			setProperty(cAffectGlobalExclusion, "false");

		if (visualization.isIgnoreGlobalExclusion())
			setProperty(cIgnoreGlobalExclusion, "true");

		if (visualization.getLocalExclusionList() != -1)
			setProperty(cLocalExclusionList, mTableModel.getListHandler().getListName(visualization.getLocalExclusionList()));

		if (visualization.getGridMode() != JVisualization.cGridModeShown)
			setProperty(cSuppressGrid, JVisualization.GRID_MODE_CODE[visualization.getGridMode()]);

		if (visualization.isLegendSuppressed())
			setProperty(cSuppressLegend, "true");

		if (visualization.getScaleMode() != JVisualization.cScaleModeShown)
			setProperty(cSuppressScale, JVisualization.SCALE_MODE_CODE[visualization.getScaleMode()]);

		if (visualization.isShowStandardDeviation())
			setProperty(cShowStdDev, "true");

		if (visualization.isShowConfidenceInterval())
			setProperty(cShowConfInterval, "true");

		if (visualization.isShowValueCount())
			setProperty(cShowValueCount, "true");

		if (visualization.isShowFoldChange())
			setProperty(cShowFoldChange, "true");

		if (visualization.isShowPValue())
			setProperty(cShowPValue, "true");

		column = visualization.getPValueColumn();
		if (column != JVisualization.cColumnUnassigned) {
			setProperty(cPValueColumn, mTableModel.getColumnTitleNoAlias(column));
			setProperty(cPValueRefCategory, visualization.getPValueRefCategory());
		}

		int boxplotMeanMode = visualization.getBoxplotMeanMode();
		if (boxplotMeanMode != JVisualization.BOXPLOT_DEFAULT_MEAN_MODE)
			setProperty(cBoxplotMeanMode, JVisualization.BOXPLOT_MEAN_MODE_CODE[boxplotMeanMode]);

		if (visualization.isShowMeanAndMedianValues())
			setProperty(cBoxplotShowMeanValues, "true");

		if (view instanceof VisualizationPanel2D) {
			double transparency = ((JVisualization2D)visualization).getMarkerTransparency();
			if (transparency != 0.0) {
				setProperty(cMarkerTransparency, ""+transparency);
			}

			int[] multiValueMarkerColumn = ((JVisualization2D)visualization).getMultiValueMarkerColumns();
			if (multiValueMarkerColumn != null) {
				int multiValueMarkerMode = ((JVisualization2D)visualization).getMultiValueMarkerMode();
				setProperty(cMultiValueMarkerMode, JVisualization2D.MULTI_VALUE_MARKER_MODE_CODE[multiValueMarkerMode]);
				StringBuilder columnNames = new StringBuilder(mTableModel.getColumnTitleNoAlias(multiValueMarkerColumn[0]));
				for (int j=1; j<multiValueMarkerColumn.length; j++)
					columnNames.append('\t').append(mTableModel.getColumnTitleNoAlias(multiValueMarkerColumn[j]));
				setProperty(cMultiValueMarkerColumns, ""+columnNames.toString());
			}

			column = ((JVisualization2D)visualization).getBackgroundColor().getColorColumn();
			if (column != JVisualization.cColumnUnassigned) {
				String key = cBackgroundColorColumn;
				if (CompoundTableListHandler.isListColumn(column))
					setProperty(key, "colorByHitlist\t"
							+ mTableModel.getListHandler().getListName(
							CompoundTableListHandler.convertToListIndex(column)));
				else {
					setProperty(key, mTableModel.getColumnTitleNoAlias(column));
				}

				int mode = ((JVisualization2D)visualization).getBackgroundColor().getColorListMode();
				key = cBackgroundColorListMode;
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
					setProperty(cBackgroundColorCount, ""+colorList.length);
					for (int j=0; j<colorList.length; j++)
						setProperty(cBackgroundColor+"_"+j, ""+colorList[j].getRGB());
				}
				else {
					setProperty(cBackgroundColor+"_0", ""+colorList[0].getRGB());
					setProperty(cBackgroundColor+"_1", ""+colorList[colorList.length-1].getRGB());
				}

				int hitlist = ((JVisualization2D)visualization).getBackgroundColorConsidered();
				String value = (hitlist == JVisualization2D.BACKGROUND_VISIBLE_RECORDS) ? "visibleRecords"
						: (hitlist == JVisualization2D.BACKGROUND_ALL_RECORDS) ? "allRecords"
						: "fromHitlist\t" + mTableModel.getListHandler().getListName(hitlist);
				setProperty(cBackgroundColorRecords, value);
				setProperty(cBackgroundColorRadius, ""+((JVisualization2D)visualization).getBackgroundColorRadius());
				setProperty(cBackgroundColorFading, ""+((JVisualization2D)visualization).getBackgroundColorFading());

				if (!Double.isNaN(((JVisualization2D)visualization).getBackgroundColor().getColorMin()))
					setProperty(cBackgroundColorMin, ""+((JVisualization2D)visualization).getBackgroundColor().getColorMin());
				if (!Double.isNaN(((JVisualization2D)visualization).getBackgroundColor().getColorMax()))
					setProperty(cBackgroundColorMax, ""+((JVisualization2D)visualization).getBackgroundColor().getColorMax());
			}

			byte[] backgroundImageData = ((JVisualization2D)visualization).getBackgroundImageData();
			if (backgroundImageData != null)
				setBinary(cBackgroundImage, backgroundImageData);

			int curveMode = ((JVisualization2D)visualization).getCurveMode();
			if (curveMode != 0) {
				setProperty(cCurveMode, JVisualization2D.CURVE_MODE_CODE[curveMode]);
				if (((JVisualization2D)visualization).isShowStandardDeviationLines())
					setProperty(cCurveStdDev, "true");
				if (((JVisualization2D)visualization).isCurveSplitByCategory())
					setProperty(cCurveSplitByCategory, "true");
			}

			int correlationType = ((JVisualization2D)visualization).getShownCorrelationType();
			if (correlationType != CorrelationCalculator.TYPE_NONE) {
				setProperty(cCorrelationCoefficient, CorrelationCalculator.TYPE_NAME[correlationType]);
			}

*/		}
	}
