package com.actelion.research.table.filter;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

public class FilterTransferable implements Transferable {
	public static final DataFlavor DF_FILTER_PANEL_OBJ = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType +";class=com.actelion.research.table.filter.JFilterPanel", "DataWarrior filter panel");
	public static final DataFlavor DF_FILTER_PANEL_DEF = new DataFlavor("application/x-openmolecules-filter;class=java.lang.String", "DataWarrior filter panel setting");
	public static final DataFlavor[] FILTER_FLAVORS = { DF_FILTER_PANEL_OBJ, DF_FILTER_PANEL_DEF };

	private JFilterPanel mFilterPanel;

	public FilterTransferable(JFilterPanel filterPanel) {
		mFilterPanel = filterPanel;
		}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return FILTER_FLAVORS;
		}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		for (DataFlavor df:FILTER_FLAVORS)
			if (df.equals(flavor))
				return true;
		return false;
		}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor.equals(DF_FILTER_PANEL_OBJ))
			return mFilterPanel;
		if (flavor.equals(DF_FILTER_PANEL_DEF))
			return mFilterPanel.getSettings();

		throw new UnsupportedFlavorException(DF_FILTER_PANEL_OBJ);
		}
	}
