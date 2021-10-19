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

package com.actelion.research.table.view;

import com.actelion.research.chem.IDCodeParserWithoutCoordinateInvention;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.gui.form.*;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;

public class JCompoundTableForm extends JFormView implements CompoundTableColorHandler.ColorListener,CompoundTableListener,Pageable {
    private static final long serialVersionUID = 0x20060921;

    public static final int PRINT_MODE_CURRENT_RECORD = 0;
    public static final int PRINT_MODE_VISIBLE_RECORDS = 1;
    public static final int PRINT_MODE_ALL_RECORDS = 2;

    private Frame				mParentFrame;
	private CompoundTableModel mTableModel;
	private CompoundTableColorHandler	mColorHandler;
	private int					mLastPageIndex;
	private int					mPrintMode,mPrintColumns,mPrintRows;
	private PageFormat			mPageFormat;

	public JCompoundTableForm(Frame parent, CompoundTableModel tableModel, CompoundTableColorHandler colorHandler) {
		super();
		addSupportedType(JImageDetailView.TYPE_IMAGE_FROM_PATH, JImageFormObject.class);
		addSupportedType(JImageDetailView.TYPE_IMAGE_JPEG, JImageFormObject.class);
		addSupportedType(JImageDetailView.TYPE_IMAGE_GIF, JImageFormObject.class);
		addSupportedType(JImageDetailView.TYPE_IMAGE_PNG, JImageFormObject.class);
		addSupportedType(JStructure3DFormObject.FORM_OBJECT_TYPE, JStructure3DFormObject.class);
		addSupportedType(JHTMLDetailView.TYPE_TEXT_PLAIN, JHTMLFormObject.class);
		addSupportedType(JHTMLDetailView.TYPE_TEXT_HTML, JHTMLFormObject.class);
		addSupportedType(JSVGDetailView.TYPE_IMAGE_SVG, JSVGFormObject.class);
		addSupportedType(JFXPDBDetailView.TYPE_CHEMICAL_PDB, JFXPDBFormObject.class);

		mParentFrame = parent;
		mTableModel = tableModel;
		mColorHandler = colorHandler;

		CompoundRecord record = mTableModel.getActiveRow();
		if (record != null && !mTableModel.isVisible(record))
		    record = null;
		setModel(new CompoundTableFormModel(mTableModel, record));

		mTableModel.addCompoundTableListener(this);
		mColorHandler.addColorListener(this);

	    mPrintMode = PRINT_MODE_CURRENT_RECORD;
	    mPrintColumns = 1;
	    mPrintRows = 1;
	    mLastPageIndex = -1;
		}

	public void cleanup() {
		mTableModel.removeCompoundTableListener(this);
		mColorHandler.removeColorListener(this);
		}

	public AbstractFormObject addFormObject(String key, String type) {
		AbstractFormObject formObject = super.addFormObject(key, type);

		if (formObject != null) {
			if (formObject.getType().equals(JImageDetailView.TYPE_IMAGE_FROM_PATH)) {
				int column = mTableModel.findColumn(formObject.getKey());
				if (column != -1) {
					JImageDetailView imageView = (JImageDetailView)formObject.getComponent();
					imageView.setImagePath(mTableModel.getColumnProperty(column,
												CompoundTableModel.cColumnPropertyImagePath));
					imageView.setUseThumbNail(mTableModel.getColumnProperty(column,
												CompoundTableModel.cColumnPropertyUseThumbNail) != null);
					}
				}
	
			if (formObject instanceof ReferenceFormObject) {
				initializeReferenceResolution(formObject);
				}
	
			if (formObject.getComponent() instanceof JResultDetailView) {
				((JResultDetailView)formObject.getComponent()).setResultDetailPopupItemProvider(mTableModel.getDetailHandler());
				}

			if (formObject instanceof JStructure3DFormObject) {
				int column = mTableModel.findColumn(formObject.getKey());
				if (column != -1) {
					String overlayIDCode = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertySuperposeMolecule);
					StereoMolecule overlayMol = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(overlayIDCode);
					if (overlayMol != null)
						((JStructure3DFormObject)formObject).setReferenceMolecule(overlayMol);

					String cavityIDCode = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyProteinCavity);
					StereoMolecule cavityMol = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(cavityIDCode);
					String ligandIDCode = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyNaturalLigand);
					StereoMolecule ligandMol = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(ligandIDCode);
					if (cavityMol != null)
						((JStructure3DFormObject)formObject).setCavityMolecule(cavityMol, ligandMol);
					}
				}
			}

		return formObject;
		}

	public void createDefaultLayout() {
		int columnCount = Math.max(1, Math.round((float)Math.sqrt((float)mTableModel.getTotalColumnCount()/3f)));
		createLayout(null, true, true, columnCount);
		}

	/**
	 * @param includeTableColumn
	 * @param includeDetails
	 * @param includeLookups
	 * @param formColumnCount -1 is automatic
	 */
	public void createLayout(boolean[] includeTableColumn, boolean includeDetails, boolean includeLookups, int formColumnCount) {
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			if (includeTableColumn == null || includeTableColumn[column]) {
				String imagePath = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyImagePath);
				String specialType = mTableModel.getColumnSpecialType(column);
				if (specialType != null) {
					if (specialType.equals(CompoundTableModel.cColumnTypeIDCode))
						addFormObject(mTableModel.getColumnTitleNoAlias(column), FormObjectFactory.TYPE_STRUCTURE);
					else if (specialType.equals(CompoundTableModel.cColumnTypeRXNCode))
						addFormObject(mTableModel.getColumnTitleNoAlias(column), FormObjectFactory.TYPE_REACTION);
					else if (specialType.equals(CompoundTableModel.cColumnType3DCoordinates))
						addFormObject(mTableModel.getColumnTitleNoAlias(column), JStructure3DFormObject.FORM_OBJECT_TYPE);
					}
				else if (imagePath != null) {
					AbstractFormObject formObject = addFormObject(mTableModel.getColumnTitleNoAlias(column),
							JImageDetailView.TYPE_IMAGE_FROM_PATH);
					JImageDetailView imageView = (JImageDetailView)formObject.getComponent();
					imageView.setImagePath(imagePath);
					imageView.setUseThumbNail(mTableModel.getColumnProperty(column,
							CompoundTableModel.cColumnPropertyUseThumbNail) != null);
					}
				else {	// default form object is single- or multi-line text
					int height = Math.max(suggestTextFormObjectHeight(column, formColumnCount),
							mTableModel.isMultiLineColumn(column) ?
							JTextFormObject.MULTI_LINE_HEIGHT : JTextFormObject.SINGLE_LINE_HEIGHT);
					AbstractFormObject fo = addFormObject(mTableModel.getColumnTitleNoAlias(column), mTableModel.isMultiLineColumn(column) ?
							FormObjectFactory.TYPE_MULTI_LINE_TEXT : FormObjectFactory.TYPE_SINGLE_LINE_TEXT);
					fo.setRelativeHeight(height);
					}

				if (includeLookups) {
					int columnLookupCount = mTableModel.getColumnLookupCount(column);
					for (int i=0; i<columnLookupCount; i++) {
						String lookupURL = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLookupDetailURL);
						if (lookupURL != null)
							addFormObject(mTableModel.getColumnTitleNoAlias(column)+CompoundTableFormModel.KEY_LOOKUP_SEPARATOR+i,
									JHTMLDetailView.TYPE_TEXT_HTML);
						}
					}

				if (includeDetails) {
					int columnDetailCount = mTableModel.getColumnDetailCount(column);
					for (int i=0; i<columnDetailCount; i++)
						addFormObject(mTableModel.getColumnTitleNoAlias(column)+CompoundTableFormModel.KEY_DETAIL_SEPARATOR+i,
								mTableModel.getColumnDetailType(column, i));
					}
				}
			}

		super.createDefaultLayout(formColumnCount);
		updateColors();
		}

	private int suggestTextFormObjectHeight(int column, int formColumnCount) {
		int avgLength = 0;
		int count = 0;
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			avgLength += mTableModel.getTotalValueAt(row, column).length();
			count++;
			}
		if (count != 0)
			avgLength /= count;

		int charsPerLine = formColumnCount == -1 ? 24 : 60 / formColumnCount;
		return Math.min(10, 1 + avgLength / charsPerLine);
		}

		/**
		 * This is to set the currently shown/edited record independent from the
		 * CompoundTableEvent based mechanism. It does not change the CompoundTable's
		 * current record. The record set by this method will be superset with the
		 * next CompoundTableEvent of types cCurrentRecordChange, cNewTable, etc.
		 * @param record
		 */
	public void setCurrentRecord(CompoundRecord record) {
        if (record != ((CompoundTableFormModel)getModel()).getCompoundRecord()) {
            ((CompoundTableFormModel)getModel()).setCompoundRecord(record);
            update();
            }
	    }

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cChangeActiveRow
		 || e.getType() == CompoundTableEvent.cNewTable
		 || e.getType() == CompoundTableEvent.cChangeExcluded)
			updateCurrentRecord();
		else if (e.getType() == CompoundTableEvent.cAddColumns) {
			boolean needsUpdate = false;
			for (int column=e.getColumn(); column<mTableModel.getTotalColumnCount(); column++) {
				String key = mTableModel.getColumnTitleNoAlias(column);
				for (int i=0; i<getFormObjectCount(); i++) {
					if (key.equals(getFormObject(i).getKey())) {
						setFormObjectTitle(mTableModel.getColumnTitle(column), i);
						getFormObject(i).setData(getModel().getValue(key));
						needsUpdate = true;
						}
					}
				if (needsUpdate)
					repaint();
				}
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			boolean needsUpdate = false;
			for (int i=getFormObjectCount()-1; i>=0; i--) {
				String key = getFormObject(i).getKey();
				int index = key.indexOf(CompoundTableFormModel.KEY_DETAIL_SEPARATOR);
				String columnName = (index == -1) ? key : key.substring(0, index);

				if (mTableModel.findColumn(columnName) == -1) {
					removeFormObject(i);
					needsUpdate = true;
					}
				}
			if (needsUpdate)
				repaint();
			}
        else if (e.getType() == CompoundTableEvent.cChangeColumnData) {
            update();
            }
		else if (e.getType() == CompoundTableEvent.cRemoveColumnDetails) {
			boolean needsUpdate = false;
			for (int i=getFormObjectCount()-1; i>=0; i--) {
				String key = getFormObject(i).getKey();
				int index = key.indexOf(CompoundTableFormModel.KEY_DETAIL_SEPARATOR);
				if (index != -1) {
					String columnName = key.substring(0, index);
					int column = mTableModel.findColumn(columnName);
					if (e.getColumn() == column) {
						try {
							int detail = Integer.parseInt(key.substring(index + CompoundTableFormModel.KEY_DETAIL_SEPARATOR.length()));
							int newDetailIndex = e.getMapping()[detail];
							if (newDetailIndex == -1) {
								removeFormObject(i);
								needsUpdate = true;
								}
							else {
								getFormObject(i).setKey(columnName+CompoundTableFormModel.KEY_DETAIL_SEPARATOR+newDetailIndex);
								}
							}
						catch (NumberFormatException nfe) {}
						}
					}
				}
			if (needsUpdate)
				repaint();
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnDetailSource) {
			for (int i=0; i<getFormObjectCount(); i++) {
			    if (getFormObject(i) instanceof ReferenceFormObject) {
			        ReferenceFormObject formObject = (ReferenceFormObject)getFormObject(i);
					String key = formObject.getKey();
					int index = key.indexOf(CompoundTableFormModel.KEY_DETAIL_SEPARATOR);
					if (index != -1) {
						String columnName = key.substring(0, index);
						int column = mTableModel.findColumn(columnName);
						if (e.getColumn() == column) {
							try {
								int detail = Integer.parseInt(key.substring(index + CompoundTableFormModel.KEY_DETAIL_SEPARATOR.length()));
								if (column == e.getColumn()
								 && detail == e.getMapping()[0])
								    formObject.setReferenceSource(new CompoundTableDetailSpecification(mTableModel, column, detail));
								}
							catch (NumberFormatException nfe) {}
							}
						}
			    	}
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
			boolean needsUpdate = false;
			String columnTitle = mTableModel.getColumnTitleNoAlias(e.getColumn());
			for (int i=0; i<getFormObjectCount(); i++) {
				String key = getFormObject(i).getKey();
				if (key.equals(columnTitle)
				 || key.startsWith(columnTitle+CompoundTableFormModel.KEY_DETAIL_SEPARATOR)) {
					getFormObject(i).setTitle(getModel().getTitle(key));
					needsUpdate = true;
					}
				}
			if (needsUpdate)
				repaint();
			}
		}

	public void colorChanged(int column, int type, VisualizationColor color) {
		CompoundRecord record = ((CompoundTableFormModel)getModel()).getCompoundRecord();
		if (record != null && !isEditable()) {
			boolean needsUpdate = false;
			String key = mTableModel.getColumnTitleNoAlias(column);
			for (int i=0; i<getFormObjectCount(); i++) {
				if (key.equals(getFormObject(i).getKey())) {
					if (type == CompoundTableColorHandler.BACKGROUND) {
						Color bg = color.getColorForBackground(record);
						getFormObject(i).setBackground(bg != null ? bg : UIManager.getColor("TextArea.inactiveBackground"));
						}
					else if (type == CompoundTableColorHandler.FOREGROUND) {
						Color fg = color.getColorForForeground(record);
						getFormObject(i).setForeground(fg != null ? fg : UIManager.getColor("TextArea.foreground"));
						}
					needsUpdate = true;
					}
				}
			if (needsUpdate)
				repaint();
			}
		}

	public void update() {
		super.update();
		updateColors();
		}

	public void updateColors() {
		if (mColorHandler != null) {
			CompoundRecord record = ((CompoundTableFormModel)getModel()).getCompoundRecord();
			for (int i=0; i<getFormObjectCount(); i++) {
				AbstractFormObject fo = getFormObject(i);
				int column = mTableModel.findColumn(fo.getKey());

				Color bg = UIManager.getColor(isEditable() ? "TextArea.background" : "TextArea.inactiveBackground");
				if (!isEditable() && record != null && mColorHandler.hasColorAssigned(column, CompoundTableColorHandler.BACKGROUND)) {
					bg = mColorHandler.getVisualizationColor(column, CompoundTableColorHandler.BACKGROUND).getColorForBackground(record);
					}
				fo.setBackground(bg);

				Color fg = UIManager.getColor("TextArea.foreground");
				if (!isEditable() && record != null && mColorHandler.hasColorAssigned(column, CompoundTableColorHandler.FOREGROUND)) {
					fg = mColorHandler.getVisualizationColor(column, CompoundTableColorHandler.FOREGROUND).getColorForForeground(record);
					}
				fo.setForeground(fg);
				}
			}
		}

	private void updateCurrentRecord() {
		CompoundRecord oldRecord = ((CompoundTableFormModel)getModel()).getCompoundRecord();
		CompoundRecord newRecord = mTableModel.getActiveRow();
		if (newRecord != null && !mTableModel.isVisible(newRecord))
		    newRecord = null;

		if (oldRecord != newRecord) {
		    ((CompoundTableFormModel)getModel()).setCompoundRecord(newRecord);
			update();
			}
		}

	private void initializeReferenceResolution(AbstractFormObject formObject) {

		// formerly we used the real detail source string as source. Now we just pass columnName,separator,detailIndex
		if (formObject instanceof ReferenceFormObject) {
			String key = formObject.getKey();
			int index = key.indexOf(CompoundTableFormModel.KEY_DETAIL_SEPARATOR);
			if (index == -1)
				index = key.indexOf(CompoundTableFormModel.KEY_LOOKUP_SEPARATOR);

			if (index != -1) {
				int column = mTableModel.findColumn(key.substring(0, index));
				if (column != -1) {
					try {
						int detail = Integer.parseInt(key.substring(index+CompoundTableFormModel.KEY_DETAIL_SEPARATOR.length()));
						((ReferenceFormObject)formObject).setReferenceResolver(mTableModel.getDetailHandler());
						((ReferenceFormObject)formObject).setReferenceSource(new CompoundTableDetailSpecification(mTableModel, column, detail));
						}
					catch (NumberFormatException nfe) {}
					}
				return;
				}
			}

/*		if (formObject instanceof ReferenceFormObject) {
			String key = formObject.getKey();
			int index = key.indexOf(CompoundTableFormModel.KEY_DETAIL_SEPARATOR);
			if (index != -1) {
				int column = mTableModel.findColumn(key.substring(0, index));
				if (column != -1) {
					try {
						int detail = Integer.parseInt(key.substring(index+CompoundTableFormModel.KEY_DETAIL_SEPARATOR.length()));
						((ReferenceFormObject)formObject).setReferenceResolver(mTableModel.getDetailHandler());
						((ReferenceFormObject)formObject).setReferenceSource(mTableModel.getColumnDetailSource(column, detail));
						}
					catch (NumberFormatException nfe) {}
					}
				return;
				}

			index = key.indexOf(CompoundTableFormModel.KEY_LOOKUP_SEPARATOR);
			if (index != -1) {
				int column = mTableModel.findColumn(key.substring(0, index));
				if (column != -1) {
					try {
						int lookup = Integer.parseInt(key.substring(index+CompoundTableFormModel.KEY_LOOKUP_SEPARATOR.length()));
						((ReferenceFormObject)formObject).setReferenceResolver(mTableModel.getDetailHandler());
						((ReferenceFormObject)formObject).setReferenceSource(CompoundTableDetailHandler.URL_RESPONSE
								+mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyLookupDetailURL+lookup));
						}
					catch (NumberFormatException nfe) {}
					}
				return;
				}
			}	*/
		return;
		}

	public int getNumberOfPages() {
	    return mTableModel.getRowCount();
		}

	public void setPageFormat(PageFormat pageFormat) {
	    mPageFormat = pageFormat;
		}

	public PageFormat getPageFormat(int pageIndex) throws java.lang.IndexOutOfBoundsException {
	    if (pageIndex < 0 || pageIndex >= mTableModel.getRowCount())
	        throw new java.lang.IndexOutOfBoundsException();

	    return mPageFormat;
		}
	
	public Printable getPrintable(int pageIndex) {
	    return this;
		}
	
	public void setPrintMode(int mode, int columns, int rows) {
	    mPrintMode = mode;
	    mPrintColumns = columns;
	    mPrintRows = rows;
	    mLastPageIndex = -1;
		}

	public int print(Graphics g, PageFormat f, int pageIndex) {
		final double FOOTER_HEIGHT = 12.0;
		final double FOOTER_FONT_SIZE = 8.0;

		if (mPrintMode == PRINT_MODE_CURRENT_RECORD)
	        return super.print(g, f, pageIndex);

	    int recordCount = (mPrintMode == PRINT_MODE_VISIBLE_RECORDS) ?
	            mTableModel.getRowCount() : mTableModel.getTotalRowCount();

	    if (recordCount == 0)
			return NO_SUCH_PAGE;
	            
	    int recordsPerPage = mPrintColumns * mPrintRows;
	    int maxPageIndex = (recordCount-1) / recordsPerPage;
	    
	    if (pageIndex > maxPageIndex)
			return NO_SUCH_PAGE;

	    if (mLastPageIndex != pageIndex) {
	        mLastPageIndex = pageIndex;
	        return PAGE_EXISTS;
	    	}

		// print footer
		double x1 = f.getImageableX();
		double x2 = f.getImageableX()+f.getImageableWidth();
		double y2 = f.getImageableY()+f.getImageableHeight()-FOOTER_FONT_SIZE/4;
		g.setColor(Color.black);
		g.setFont(g.getFont().deriveFont(Font.PLAIN, (int)FOOTER_FONT_SIZE));
	    String docTitle = mParentFrame.getTitle();
		g.drawString(docTitle, (int)x1, (int)y2);
		String page = "page "+(pageIndex+1)+" of "+(maxPageIndex+1);
		g.drawString(page, (int)x2-g.getFontMetrics().stringWidth(page), (int)y2);

	    int recordIndex = recordsPerPage * pageIndex;
	    double x = f.getImageableX();
	    double y = f.getImageableY();
	    double s = Math.min(0.1*f.getImageableWidth()/mPrintColumns,
	            			0.1*(f.getImageableHeight()-FOOTER_HEIGHT)/mPrintRows);
	    double dx = (f.getImageableWidth()-(mPrintColumns-1)*s)/mPrintColumns;
	    double dy = (f.getImageableHeight()-FOOTER_HEIGHT-(mPrintRows-1)*s)/mPrintRows;

		Graphics2D g2D = (Graphics2D)g;
		g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		for (int row=0; row<mPrintRows; row++) {
	        for (int column=0; column<mPrintColumns; column++) {
	            if (recordIndex == ((mPrintMode == PRINT_MODE_VISIBLE_RECORDS) ?
	    	            	mTableModel.getRowCount() : mTableModel.getTotalRowCount()))
	                break;
	            CompoundRecord record = (mPrintMode == PRINT_MODE_VISIBLE_RECORDS) ?
	    	            mTableModel.getRecord(recordIndex) : mTableModel.getTotalRecord(recordIndex);
	            FormModel model = new CompoundTableFormModel(mTableModel, record);
	            recordIndex++;

	            updatePrintColors(record);

	            Rectangle2D.Double bounds = new Rectangle2D.Double((float)(0.5+x+column*(dx+s)), (float)(0.5f+y+row*(dy+s)), (float)dx, (float)dy);
	            float scale = (float)Math.sqrt(1.0/recordsPerPage);
	            print(g2D, bounds, scale, model, true);
	        	}
	    	}

	    return PAGE_EXISTS;
		}

	public void updatePrintColors(CompoundRecord record) {
		if (mColorHandler != null) {
			for (int i=0; i<getFormObjectCount(); i++) {
				AbstractFormObject fo = getFormObject(i);
				int tableColumn = mTableModel.findColumn(fo.getKey());

				Color bg = Color.WHITE;
				if (record != null && mColorHandler.hasColorAssigned(tableColumn, CompoundTableColorHandler.BACKGROUND))
					bg = mColorHandler.getVisualizationColor(tableColumn, CompoundTableColorHandler.BACKGROUND).getColorForPrintBackground(record);
				fo.setPrintBackground(bg);

				Color fg = Color.BLACK;
				if (record != null && mColorHandler.hasColorAssigned(tableColumn, CompoundTableColorHandler.FOREGROUND))
					fg = mColorHandler.getVisualizationColor(tableColumn, CompoundTableColorHandler.FOREGROUND).getColorForPrintForeground(record);
				fo.setPrintForeground(fg);
				}
			}
		}
	}
