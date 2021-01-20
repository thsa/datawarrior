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

import com.actelion.research.gui.form.AbstractFormObject;
import com.actelion.research.gui.form.FormObjectFactory;
import com.actelion.research.gui.form.JFormDesigner;
import com.actelion.research.gui.hidpi.JBrowseButtons;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.DetailPopupProvider;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableListEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JCompoundTableForm;
import com.actelion.research.table.view.ViewSelectionHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DEFormView extends JComponent implements ActionListener,CompoundTableView {
    private static final long serialVersionUID = 0x20080620;

	private CompoundTableModel mTableModel;
	private JCompoundTableForm	mCompoundTableForm;
	private JLabel				mLabel;
	private JPanel				mFormView;
	private JFormDesigner		mFormDesigner;
	private JButton             mButtonNew;
	private boolean				mIsDesignMode,mIsEditMode;
	private DetailPopupProvider	mDetailPopupProvider;
	private MouseAdapter		mPopupListener;
	private ViewSelectionHelper	mViewSelectionHelper;

	public DEFormView(Frame parent, CompoundTableModel tableModel, CompoundTableColorHandler colorHandler) {
		mTableModel = tableModel;

//		if (JStructure3DFormObject.getCopyActionProvider() == null)
//			JStructure3DFormObject.setCopyActionProvider(new MolViewerActionCopy(parent));
//		if (JStructure3DFormObject.getRaytraceActionProvider() == null)
//			JStructure3DFormObject.setRaytraceActionProvider(new MolViewerActionRaytrace(parent));

		mFormView = new JPanel();
		mFormView.setLayout(new BorderLayout());

		final CompoundTableView _this = this;
		mPopupListener = new MouseAdapter() {
			@Override
		    public void mousePressed(MouseEvent e) {
				mViewSelectionHelper.setSelectedView(_this);
				if (e.isPopupTrigger())
					handlePopupTrigger(e);
				}

		    @Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					handlePopupTrigger(e);
				}
			};

		mCompoundTableForm = new JCompoundTableForm(parent, tableModel, colorHandler) {
		    private static final long serialVersionUID = 0x20130612;

		    @Override
			public AbstractFormObject addFormObject(String key, String type) {
				AbstractFormObject fo = super.addFormObject(key, type);
				if (fo != null)
					addOrRemovePopupListener(fo, true);
				return fo;
				}

			@Override
			public AbstractFormObject addFormObject(String key, String type, String constraint) {
				AbstractFormObject fo = super.addFormObject(key, type, constraint);
				if (fo != null)
					addOrRemovePopupListener(fo, true);
				return fo;
				}

			@Override
			public AbstractFormObject addFormObject(String objectDescriptor) {
				AbstractFormObject fo = super.addFormObject(objectDescriptor);
				if (fo != null)
					addOrRemovePopupListener(fo, true);
				return fo;
				}

			@Override
			public void removeFormObject(int no) {
				addOrRemovePopupListener(getFormObject(no), false);
				super.removeFormObject(no);
				}
			};
		mFormView.add(mCompoundTableForm, BorderLayout.CENTER);

		JComponent controlPanel = new JPanel();
		double[][] size = { { 8, TableLayout.FILL, TableLayout.PREFERRED , TableLayout.FILL, 8 },
							{ 4, TableLayout.FILL, TableLayout.PREFERRED, 4 } };
		controlPanel.setLayout(new TableLayout(size));

		mButtonNew = new JButton("New Row");
		mButtonNew.setVisible(false);
		mButtonNew.addActionListener(this);
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BorderLayout());
		leftPanel.add(mButtonNew, BorderLayout.WEST);
        controlPanel.add(leftPanel, "1,2");

		JBrowseButtons browseToolbar = new JBrowseButtons(false, this);
//		JBrowseToolbar browseToolbar = new JBrowseToolbar();
//		browseToolbar.addActionListener(this);
		controlPanel.add(browseToolbar, "2,1,2,2");

		mLabel = new JLabel();
		updateRecordNo(mTableModel.getActiveRowIndex());
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BorderLayout());
		rightPanel.add(mLabel, BorderLayout.EAST);
		controlPanel.add(rightPanel, "3,2");

		mFormView.add(controlPanel, BorderLayout.SOUTH);

		mFormDesigner = new JFormDesigner(mCompoundTableForm);
		mFormDesigner.setVisible(false);

		setLayout(new OverlayLayout(this));
		add(mFormView);
		add(mFormDesigner);

		MouseAdapter viewSelectionMouseAdapter = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				mViewSelectionHelper.setSelectedView(_this);
				}
			};
		addMouseListener(viewSelectionMouseAdapter);
		mCompoundTableForm.addMouseListener(viewSelectionMouseAdapter);
		mFormDesigner.addMouseListener(viewSelectionMouseAdapter);
		}

	private void addOrRemovePopupListener(AbstractFormObject fo, boolean isAdd) {
		JComponent outerComponent = fo.getComponent();	// may be a JScrollPane, which has a border and therefore must also produce the popup
		JComponent innerComponent = (outerComponent instanceof JScrollPane) ? (JComponent)((JScrollPane)outerComponent).getViewport().getView() : null;
		if (isAdd) {
			outerComponent.addMouseListener(mPopupListener);
			if (innerComponent != null)
				innerComponent.addMouseListener(mPopupListener);
			}
		else {
			outerComponent.removeMouseListener(mPopupListener);
			if (innerComponent != null)
				innerComponent.removeMouseListener(mPopupListener);
			}
		}

	public void setDetailPopupProvider(DetailPopupProvider p) {
        mDetailPopupProvider = p;
        }

	@Override
	public void setViewSelectionHelper(ViewSelectionHelper l) {
		mViewSelectionHelper = l;
		}

	@Override
	public boolean copyViewContent() {
		return false;
		}

	@Override
	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	private void handlePopupTrigger(MouseEvent e) {
        if (e.isPopupTrigger() && mDetailPopupProvider != null && !mIsDesignMode && !mIsEditMode) {
        	CompoundRecord record = mTableModel.getActiveRow();
    		if (record != null) {
            	for (int i=0; i<mCompoundTableForm.getFormObjectCount(); i++) {
            		AbstractFormObject fo = mCompoundTableForm.getFormObject(i);
            		if (fo.getComponent() == e.getSource()
					 || (fo.getComponent() instanceof JScrollPane && ((JScrollPane)fo.getComponent()).getViewport().getView() == e.getSource())) {
	            		if (FormObjectFactory.TYPE_SINGLE_LINE_TEXT.equals(fo.getType())
	            		 || FormObjectFactory.TYPE_MULTI_LINE_TEXT.equals(fo.getType())
	            		 || FormObjectFactory.TYPE_STRUCTURE.equals(fo.getType())
	            		 || FormObjectFactory.TYPE_REACTION.equals(fo.getType())) {
			        		int column = mTableModel.findColumn(fo.getKey());
			                JPopupMenu popup = mDetailPopupProvider.createPopupMenu(record, this, column, e.isControlDown());
			                if (popup != null)
			                    popup.show(e.getComponent(), e.getX(), e.getY());
	            			}
	            		break;
            			}
            		}
        		}
            }
        }

	public void cleanup() {
		if (mIsEditMode)
			saveFieldInFocus();

		mTableModel.removeCompoundTableListener(this);
		mCompoundTableForm.cleanup();
		}

	public void actionPerformed(ActionEvent e) {
		if (mIsEditMode)
			saveFieldInFocus();

		int rowCount = mTableModel.getRowCount();
		if (e.getSource() == mButtonNew) {
			int firstNewRow = mTableModel.getTotalRowCount();
		    mTableModel.addNewRows(1, true);
		    mTableModel.finalizeNewRows(firstNewRow, null);
		    CompoundRecord record = mTableModel.getTotalRecord(firstNewRow);
		    if (mTableModel.isVisible(record)) {
                mTableModel.setActiveRow(rowCount);
                updateRecordNo(rowCount);
		        }
		    else {
		        mCompoundTableForm.setCurrentRecord(record);
	            mLabel.setText("New Record");
		        }
		    }
		else if (e.getActionCommand().equals("|<")) {
			if (rowCount > 0
			 && mTableModel.getActiveRow() != mTableModel.getRecord(0)) {
				mTableModel.setActiveRow(0);
				updateRecordNo(0);
				}
			}
		else if (e.getActionCommand().equals(">|")) {
			if (rowCount > 0
			 && mTableModel.getActiveRow() != mTableModel.getRecord(rowCount-1)) {
				mTableModel.setActiveRow(rowCount-1);
				updateRecordNo(rowCount-1);
				}
			}
		else if (e.getActionCommand().equals("<")) {
			int currentIndex = mTableModel.getActiveRowIndex();
			if (currentIndex > 0) {
				mTableModel.setActiveRow(currentIndex-1);
				updateRecordNo(currentIndex-1);
				}
			}
		else if (e.getActionCommand().equals(">")) {
			int currentIndex = mTableModel.getActiveRowIndex();
			if (currentIndex < rowCount-1) {
				mTableModel.setActiveRow(currentIndex+1);
				updateRecordNo(currentIndex+1);
				}
			}
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cChangeActiveRow
		 || e.getType() == CompoundTableEvent.cChangeExcluded
		 || e.getType() == CompoundTableEvent.cChangeSortOrder)
			updateRecordNo(mTableModel.getActiveRowIndex());
		}

	public void listChanged(CompoundTableListEvent e) {}

	public JCompoundTableForm getCompoundTableForm() {
		return mCompoundTableForm;
		}

	public void createDefaultLayout() {
		mCompoundTableForm.createDefaultLayout();
		}

	public void createLayout(boolean[] includeTableColumn, boolean includeDetails, boolean includeLookups, int formColumnCount) {
		mCompoundTableForm.createLayout(includeTableColumn, includeDetails, includeLookups, formColumnCount);
		}

	public boolean isDesignMode() {
		return mIsDesignMode;
		}

    public boolean isEditMode() {
        return mIsEditMode;
        }

	public void setDesignMode(boolean isDesignMode) {
		if (isDesignMode) {
		    if (mIsEditMode)
		        setEditMode(false);
			mFormView.setVisible(false);
			mFormDesigner.getFormLayout();
			mFormDesigner.setVisible(true);
			}
		else {
			mFormDesigner.setVisible(false);
			mFormDesigner.setFormLayout();
			mFormView.setVisible(true);
			mCompoundTableForm.updateColors();
			}
		mIsDesignMode = isDesignMode;
		}

	public void setEditMode(boolean isEditMode) {
		if (isEditMode != mIsEditMode) {
			if (mIsEditMode)	// leaving edit mode
				saveFieldInFocus();
			else if (mIsDesignMode)
	            setDesignMode(false);
		    mButtonNew.setVisible(isEditMode);
		    mCompoundTableForm.setEditable(isEditMode);
		    mIsEditMode = isEditMode;
			}
		}

	private void saveFieldInFocus() {
		AbstractFormObject fo = mCompoundTableForm.getFormObjectOnFocus();
		if (fo != null)
			mCompoundTableForm.dataChanged(fo);
		}

	private void updateRecordNo(int currentRecordIndex) {
		if (currentRecordIndex == -1)
			mLabel.setText("No active row");
		else
			mLabel.setText(""+(1+currentRecordIndex)+" of "+mTableModel.getRowCount());
		}

/* 	public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
		Graphics2D g2 = (Graphics2D)g;
     	g2.setColor(Color.black);
     	int fontHeight = g2.getFontMetrics().getHeight();
     	int fontDesent = g2.getFontMetrics().getDescent();

     	//leave room for page number
     	double pageHeight = pageFormat.getImageableHeight() - fontHeight;
     	double pageWidth = pageFormat.getImageableWidth();
     	double tableWidth = (double)mTable.getColumnModel().getTotalColumnWidth();
     	double scale = 1;
     	if (tableWidth >= pageWidth)
			scale = pageWidth / tableWidth;

     	double headerHeightOnPage= mTable.getTableHeader().getHeight()*scale;
     	double tableWidthOnPage = tableWidth*scale;

     	double oneRowHeight = (mTable.getRowHeight() + mTable.getRowMargin())*scale;
     	int numRowsOnAPage = (int)((pageHeight-headerHeightOnPage) / oneRowHeight);
     	double pageHeightForTable = oneRowHeight * numRowsOnAPage;
     	int totalNumPages = (int)Math.ceil(((double)mTable.getRowCount()) / numRowsOnAPage);

     	if(pageIndex >= totalNumPages)
			return NO_SUCH_PAGE;

     	g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
     	g2.drawString("Page: "+(pageIndex+1),
					  (int)pageWidth / 2-35,
					  (int)(pageHeight + fontHeight - fontDesent));

     	g2.translate(0f,headerHeightOnPage);
     	g2.translate(0f,-pageIndex*pageHeightForTable);

     	//If this piece of the table is smaller
     	//than the size available,
     	//clip to the appropriate bounds.
     	if (pageIndex + 1 == totalNumPages) {
			int lastRowPrinted = numRowsOnAPage * pageIndex;
			int numRowsLeft = mTable.getRowCount() - lastRowPrinted;
			g2.setClip(0, (int)(pageHeightForTable * pageIndex),
						  (int)Math.ceil(tableWidthOnPage),
						  (int)Math.ceil(oneRowHeight * numRowsLeft));
			}
     	//else clip to the entire area available.
     	else {
			g2.setClip(0, (int)(pageHeightForTable*pageIndex),
						  (int) Math.ceil(tableWidthOnPage),
						  (int) Math.ceil(pageHeightForTable));
			}

		g2.scale(scale,scale);
		mTable.paint(g2);
		g2.scale(1/scale,1/scale);
		g2.translate(0f,pageIndex*pageHeightForTable);
		g2.translate(0f, -headerHeightOnPage);
		g2.setClip(0, 0,
				   (int) Math.ceil(tableWidthOnPage),
				   (int)Math.ceil(headerHeightOnPage));
     	g2.scale(scale, scale);
		mTable.getTableHeader().paint(g2);

     	return Printable.PAGE_EXISTS;
		}*/
	}
