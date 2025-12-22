package com.actelion.research.table;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.ByteArrayComparator;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class LookupURLBuilder {
	private final CompoundTableModel mTableModel;

	public LookupURLBuilder(CompoundTableModel tableModel) {
		mTableModel = tableModel;
	}

	/**
	 * Determines whether the cell contains identifiers that allow opening a lookup page in a browser.
	 * This is either because one or more URLs are defined using cColumnPropertyLookup... properties,
	 * or because category specific lookup URLs are defined with cColumnPropertyCategorySpecificLookup and
	 * the corresponding cell in the category column contains at least one category for which a URL is defined.
	 * @param row visible row index
	 * @param column CompoundTableModel's total column index
	 * @return whether the cell
	 */
	public boolean hasURL(int row, int column) {
		if (mTableModel.getRecord(row).getData(column) == null)
			return false;

		// If we have (a) standard lookup URL(s) defined, they take precedence:
		String countString = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyLookupCount);
		if (countString != null && Integer.parseInt(countString) != 0)
			return true;

		String catSpecificLookupString = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyCategorySpecificLookup);
		if (catSpecificLookupString == null)
			return false;

		String[] catSpecificLookup = catSpecificLookupString.split(";");
		if (catSpecificLookup.length < 3)    // format: catColumnName;cat1;url1[;cat2;url2[;cat3;url3]...]
			return false;

		int catColumn = mTableModel.findColumn(catSpecificLookup[0]);
		if (catColumn == -1)
			return false;

//		for (int i=1; i<catSpecificLookup.length; i+=2)
//			if (new ByteArrayComparator().compare(catSpecificLookup[i].getBytes(), (byte[])mTableModel.getRecord(row).getData(catColumn)) == 0)
//				return true;
		String[] categories = mTableModel.separateEntries(mTableModel.encodeData(mTableModel.getRecord(row), catColumn));
		for (String category : categories)
			for (int i=1; i<catSpecificLookup.length; i+=2)
				if (new ByteArrayComparator().compare(catSpecificLookup[i].getBytes(), category.getBytes()) == 0)
					return true;

		return false;
	}

	/**
	 * Build a lookup URL from entry of the defined cell that allows opening a lookup page in a browser.
	 * If one or more URLs are defined using cColumnPropertyLookup... properties, then the first is used for this.
	 * If category specific lookup URLs are defined with cColumnPropertyCategorySpecificLookup and
	 * the corresponding cell in the category column contains at least one category for which a URL is defined.
	 * @param row visible row index
	 * @param column CompoundTableModel's total column index
	 * @param entry the cell entry (identifier) for which to build a lookup URL
	 * @return the constructed lookup URL
	 */
	public String getURL(int row, int column, String entry, int entryNo) {
		// If we have (a) standard lookup URL(s) defined, they take precedence:
		// If we have multiple lookup URLs defined, then a better handling would be to ask which one to use.
		// Currently, we just use the first one:
		String countString = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyLookupCount);
		if (countString != null) {
			String url = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyLookupURL + 0);
			if (url == null)
				return null;

			if (CompoundTableConstants.cColumnPropertyLookupFilterRemoveMinus.equals(
					mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyLookupFilter + 0)))
				entry = entry.replace("-", "");
			if (!"false".equals(mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyLookupEncode + "0")))
				entry = URLEncoder.encode(entry, StandardCharsets.UTF_8).replace("+", "%20");

			return url.replace("%s", entry);
		}

		String catSpecificLookupString = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyCategorySpecificLookup);
		if (catSpecificLookupString == null)
			return null;

		String[] catSpecificLookup = catSpecificLookupString.split(";");
		if (catSpecificLookup.length < 3)    // format: catColumnName;cat1;url1[;cat2;url2[;cat3;url3]...]
			return null;

		int catColumn = mTableModel.findColumn(catSpecificLookup[0]);
		if (catColumn == -1)
			return null;

		String[] category = mTableModel.separateEntries(mTableModel.encodeData(mTableModel.getRecord(row), catColumn));
		if (entryNo >= 0 && entryNo < category.length) {
			for (int i=1; i<catSpecificLookup.length; i+=2) {
				if (new ByteArrayComparator().compare(catSpecificLookup[i].getBytes(), category[entryNo].getBytes()) == 0) {
					entry = URLEncoder.encode(entry, StandardCharsets.UTF_8).replace("+", "%20");
					return catSpecificLookup[i + 1].replace("%s", entry);
				}
			}
		}

		return null;
	}
}
