package com.actelion.research.table;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.ByteArrayComparator;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class LookupURLBuilder {
	private final CompoundTableModel mTableModel;
	private final int mColumn;

	public LookupURLBuilder(CompoundTableModel tableModel, int column) {
		mTableModel = tableModel;
		mColumn = column;
	}

	public boolean hasURL(int row, int index) {
		// If we have (a) standard lookup URL(s) defined, they take precedence:
		String countString = mTableModel.getColumnProperty(mColumn, CompoundTableConstants.cColumnPropertyLookupCount);
		if (countString != null)
			return Integer.parseInt(countString) > index;

		String catSpecificLookupString = mTableModel.getColumnProperty(mColumn, CompoundTableConstants.cColumnPropertyCategorySpecificLookup);
		if (catSpecificLookupString == null)
			return false;

		String[] catSpecificLookup = catSpecificLookupString.split(";");
		if (catSpecificLookup.length < 3)    // format: catColumnName;cat1;url1[;cat2;url2[;cat3;url3]...]
			return false;

		int catColumn = mTableModel.findColumn(catSpecificLookup[0]);
		if (catColumn == -1)
			return false;

		for (int i=1; i<catSpecificLookup.length; i+=2)
			if (new ByteArrayComparator().compare(catSpecificLookup[i].getBytes(), (byte[])mTableModel.getRecord(row).getData(catColumn)) == 0)
				return true;

		return false;
	}

	public String getURL(int row, int index, String entry) {
		// If we have (a) standard lookup URL(s) defined, they take precedence:
		String countString = mTableModel.getColumnProperty(mColumn, CompoundTableConstants.cColumnPropertyLookupCount);
		if (countString != null) {
			String url = mTableModel.getColumnProperty(mColumn, CompoundTableConstants.cColumnPropertyLookupURL + index);
			if (url == null)
				return null;

			if (CompoundTableConstants.cColumnPropertyLookupFilterRemoveMinus.equals(
					mTableModel.getColumnProperty(mColumn, CompoundTableConstants.cColumnPropertyLookupFilter + index)))
				entry = entry.replace("-", "");
			if (!"false".equals(mTableModel.getColumnProperty(mColumn, CompoundTableConstants.cColumnPropertyLookupEncode + "0")))
				entry = URLEncoder.encode(entry, StandardCharsets.UTF_8).replace("+", "%20");

			return url.replace("%s", entry);
		}

		String catSpecificLookupString = mTableModel.getColumnProperty(mColumn, CompoundTableConstants.cColumnPropertyCategorySpecificLookup);
		if (catSpecificLookupString == null)
			return null;

		String[] catSpecificLookup = catSpecificLookupString.split(";");
		if (catSpecificLookup.length < 3)    // format: catColumnName;cat1;url1[;cat2;url2[;cat3;url3]...]
			return null;

		int catColumn = mTableModel.findColumn(catSpecificLookup[0]);
		if (catColumn == -1)
			return null;

		for (int i=1; i<catSpecificLookup.length; i+=2) {
			if (new ByteArrayComparator().compare(catSpecificLookup[i].getBytes(), (byte[])mTableModel.getRecord(row).getData(catColumn)) == 0) {
				entry = URLEncoder.encode(entry, StandardCharsets.UTF_8).replace("+", "%20");
				return catSpecificLookup[i + 1].replace("%s", entry);
			}
		}

		return null;
	}
}
