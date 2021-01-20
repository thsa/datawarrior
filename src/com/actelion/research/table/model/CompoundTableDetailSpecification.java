package com.actelion.research.table.model;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.gui.form.RemoteDetailSource;

public class CompoundTableDetailSpecification extends RemoteDetailSource {
	private CompoundTableModel	mTableModel;
	private String				mColumnName;
	private int					mDetailIndex;

	public CompoundTableDetailSpecification(CompoundTableModel tableModel, int column, int detail) {
		super(null);
		mTableModel = tableModel;
		mColumnName = tableModel.getColumnTitleNoAlias(column);
		mDetailIndex = detail;
		}

	@Override
	public String getSource() {
		String source = null;

		int column = mTableModel.findColumn(mColumnName);
		if (column != -1)
			source = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyDetailSource+mDetailIndex);

		return source; 
		}

	/**
	 * This is the original Orbit mimetype deduced from the assay_attribute types (e.g. 'O_JPEG').
	 * If this is an image type 'image/...' then one can launch the Orbit client with the Orbit-ID.<br>
	 * Note: cColumnPropertyDetailType is usually 'image/png' if source is 'orbit/rdf', because
	 * the detail source in this case is the Orbit web service, which creates a PNG for any detail type. 
	 * @return mimetype or null, if the detail source is not Orbit
	 */
	public String getOrbitMimetype() {
		String mimetype = null;

		int column = mTableModel.findColumn(mColumnName);
		if (column != -1)
			mimetype = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyOrbitType+mDetailIndex);

		return mimetype; 
		}
	}
