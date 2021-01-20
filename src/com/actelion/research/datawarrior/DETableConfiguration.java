package com.actelion.research.datawarrior;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.config.ViewConfiguration;

public class DETableConfiguration extends ViewConfiguration<DETableView> {
	private static final String cTableRowHeight = "rowHeight";
	private static final String cTableColumnWidth = "columnWidth";
	private static final String cTableColumnWrapping = "columnWrapping";
	private static final String cTableColumnVisibility = "columnVisibility";
	private static final String cTableColumnOrder = "columnOrder";
	private static final String cTableColumnFilter = "columnFilter";
	private static final String cTableText = "Text_";				// suffix for cColor???? keys in case of table view column text color
	private static final String cTableBackground = "Background_";	// suffix for cColor???? keys in case of table view column background color

	public static final String VIEW_TYPE = "tableView";

	private DETableView mTableView;
	private CompoundTableModel mTableModel;

	public DETableConfiguration(DETableView tableView) {
		super(tableView.getTableModel());
		mTableView = tableView;
		mTableModel = tableView.getTableModel();
	}

	@Override
	public String getViewType() {
		return VIEW_TYPE;
	}

	public void apply(DETableView tableView) {
		DETable table = mTableView.getTable();
		String value = getProperty(cTableRowHeight);
		table.setRowHeight(HiDPIHelper.scale(value == null ? 16 : Integer.parseInt(value)));
		value = getProperty(cViewFontSize);
		if (value != null)
			table.setFontSize(Integer.parseInt(value));
		for (int modelColumn=0; modelColumn<mTableModel.getColumnCount(); modelColumn++) {
			int column = mTableModel.convertFromDisplayableColumnIndex(modelColumn);
			value = getProperty(cTableColumnWidth+"_"+mTableModel.getColumnTitleNoAlias(column));
			if (value != null)
				table.getColumnModel().getColumn(table.convertColumnIndexToView(modelColumn)).setPreferredWidth(HiDPIHelper.scale(Integer.parseInt(value)));
			value = getProperty(cTableColumnWrapping+"_"+mTableModel.getColumnTitleNoAlias(column));
			if (value != null)
				mTableView.setTextWrapping(column, value.equals("true"));
			value = getProperty(cColorColumn+cTableText+mTableModel.getColumnTitleNoAlias(column));
			if (value != null)
				applyViewColorProperties(cTableText+mTableModel.getColumnTitleNoAlias(column),
						mTableView.getColorHandler().getVisualizationColor(column, CompoundTableColorHandler.FOREGROUND));
			value = getProperty(cColorColumn+cTableBackground+mTableModel.getColumnTitleNoAlias(column));
			if (value != null)
				applyViewColorProperties(cTableBackground+mTableModel.getColumnTitleNoAlias(column),
						mTableView.getColorHandler().getVisualizationColor(column, CompoundTableColorHandler.BACKGROUND));
		}
		value = getProperty(cTableColumnOrder);
		if (value != null)
			table.setColumnOrderString(value);

		value = getProperty(cTableColumnFilter);
		if (value != null)
			table.setColumnFilterText(value);
		for (int modelColumn=0; modelColumn<mTableModel.getColumnCount(); modelColumn++) { // manual column visibility may override filter
			int column = mTableModel.convertFromDisplayableColumnIndex(modelColumn);
			value = getProperty(cTableColumnVisibility+"_"+mTableModel.getColumnTitleNoAlias(column));
			if (value != null)
				mTableView.setColumnVisibility(column, value.equals("true"));
		}
	}

	public void learn(DETableView tableView) {
		DETable table = mTableView.getTable();
		setProperty(cTableRowHeight, Integer.toString(Math.round((float)table.getRowHeight()/HiDPIHelper.getUIScaleFactor())));
		int fontSize = table.getFontSize();
		if (fontSize != DETable.DEFAULT_FONT_SIZE)
			setProperty(cViewFontSize, ""+fontSize);

		// store column width for all visible columns
		for (int modelColumn=0; modelColumn<mTableModel.getColumnCount(); modelColumn++) {
			int column = mTableModel.convertFromDisplayableColumnIndex(modelColumn);
			setProperty(cTableColumnWidth+"_"+mTableModel.getColumnTitleNoAlias(column), ""+table.getColumnWidth(column));
			if (table.isTextWrapped(column))
				setProperty(cTableColumnWrapping+"_"+mTableModel.getColumnTitleNoAlias(column), "true");
			String columnVisibility = table.getExplicitColumnVisibilityString(column);
			if (columnVisibility != null)
				setProperty(cTableColumnVisibility+"_"+mTableModel.getColumnTitleNoAlias(column), columnVisibility);
			if (mTableView.getColorHandler().hasColorAssigned(column, CompoundTableColorHandler.FOREGROUND))
				learnViewColorProperties(cTableText+mTableModel.getColumnTitleNoAlias(column),
						mTableView.getColorHandler().getVisualizationColor(column, CompoundTableColorHandler.FOREGROUND));
			if (mTableView.getColorHandler().hasColorAssigned(column, CompoundTableColorHandler.BACKGROUND))
				learnViewColorProperties(cTableBackground+mTableModel.getColumnTitleNoAlias(column),
						mTableView.getColorHandler().getVisualizationColor(column, CompoundTableColorHandler.BACKGROUND));
		}

		String order = table.getColumnOrderString();
		if (order != null)
			setProperty(cTableColumnOrder, order);
		String filter = table.getColumnFilterText();
		if (filter != null)
			setProperty(cTableColumnFilter, filter);
	}
}
