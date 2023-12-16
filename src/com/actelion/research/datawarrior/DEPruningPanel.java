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

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.datawarrior.task.filter.*;
import com.actelion.research.gui.LookAndFeelHelper;
import com.actelion.research.gui.ScrollPaneAutoScrollerWhenDragging;
import com.actelion.research.gui.VerticalFlowLayout;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.RuntimePropertyEvent;
import com.actelion.research.table.filter.*;
import com.actelion.research.table.model.*;
import com.actelion.research.util.ColorHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;

import static com.actelion.research.chem.descriptor.DescriptorConstants.DESCRIPTOR_FFP512;

public class DEPruningPanel extends JScrollPane
                implements CompoundTableListener,CompoundTableListListener,DropTargetListener,FilterListener {
    private static final long serialVersionUID = 0x20060904;

	private static final String cFilterTypeCategoryBrowser = "#browser#";
	private static final String cFilterTypeDouble = "#double#";
	private static final String cFilterTypeCategory = "#category#";
	private static final String cFilterTypeText = "#string#";
	private static final String cFilterTypeAllColumnText = "#allColumnText#";
	private static final String cFilterTypeHitlist = "#hitlist#";
	private static final String cFilterTypeStructure = "#structure#";
	private static final String cFilterTypeSSSList = "#sssList#";
	private static final String cFilterTypeSIMList = "#simList#";
	private static final String cFilterTypeReaction = "#reaction#";
	private static final String cFilterTypeRetron = "#retron#";

	private static final int ALLOWED_DROP_ACTIONS = DnDConstants.ACTION_COPY_OR_MOVE;
	private static final int GAP = HiDPIHelper.scale(5);

	private JPanel              mContent;
	private Frame               mOwner;
	private DEParentPane		mParentPane;
    private CompoundTableModel  mTableModel;
	private String              mRecentErrorMessage;
	private long                mRecentErrorMillis;
	private int                 mDropIndex;
	private boolean             mDisableEvents,mIsDropOK;
	private ScrollPaneAutoScrollerWhenDragging mScroller;

    public DEPruningPanel(Frame owner, DEParentPane parentPane, CompoundTableModel tableModel) {
	    mContent = new JPanel() {
		    private static final long serialVersionUID = 0x20100729;

		    @Override
		    public void paintComponent(Graphics g) {
			    super.paintComponent(g);

			    Color dividerColor = LookAndFeelHelper.isDarkLookAndFeel() ?
					    ColorHelper.brighter(getBackground(), 0.85f) : ColorHelper.darker(getBackground(), 0.85f);
			    int lines = HiDPIHelper.scale(2);
			    int width = mContent.getWidth();
			    for (int c = 0; c<=mContent.getComponentCount(); c++) {
				    int y = 0;
				    if (c != 0) {
					    JFilterPanel filter = (JFilterPanel)getComponent(c - 1);
					    y = filter.getY() + filter.getHeight();

					    if (filter.isPotentiallyDragged()) {
						    Rectangle bounds = filter.getBounds();
						    Color highlightColor = LookAndFeelHelper.isDarkLookAndFeel() ?
								    ColorHelper.brighter(getBackground(), 0.7f) : ColorHelper.darker(getBackground(), 0.7f);
						    g.setColor(highlightColor);
						    int thickness = HiDPIHelper.scale(1);
						    for (int i = 0; i<thickness; i++) {
							    bounds.grow(1, 1);
							    ((Graphics2D)g).draw(bounds);
						    }
					    }
				    }

				    // draw normal divider in background color or a stronger one if drop possible
				    if (c == mDropIndex || (c>=1 && c<mContent.getComponentCount())) {
					    g.setColor(c == mDropIndex ? new Color(HiDPIHelper.getThemeSpotRGBs()[0]) : dividerColor);
					    int pos = (GAP - lines) / 2;
					    int ex = (c == mDropIndex) ? 1 : 0;
					    for (int i = pos - ex; i<pos + lines + ex; i++)
						    g.drawLine(GAP - ex, y + i, width + 2 * ex - GAP - 1, y + i);
				    }
			    }
		    }

		    @Override
		    public void remove(Component comp) {
			    super.remove(comp);
			    if (!mDisableEvents)
				    mParentPane.fireRuntimePropertyChanged(
						    new RuntimePropertyEvent(this, RuntimePropertyEvent.TYPE_REMOVE_FILTER, -1));
		    }
	    };

	    setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	    setMinimumSize(new Dimension(100, 100));
	    setPreferredSize(new Dimension(100, 100));
	    setBorder(BorderFactory.createEmptyBorder());
	    mContent.setLayout(new VerticalFlowLayout(VerticalFlowLayout.LEFT, VerticalFlowLayout.TOP, GAP, GAP, true));
	    setViewportView(mContent);
	    getVerticalScrollBar().setUnitIncrement(HiDPIHelper.scale(16));

	    mScroller = new ScrollPaneAutoScrollerWhenDragging(this, true);

	    initializeDrop(ALLOWED_DROP_ACTIONS);
	    mDropIndex = -1;

	    mOwner = owner;
	    mParentPane = parentPane;
	    mTableModel = tableModel;
	    tableModel.addCompoundTableListener(this);
	    tableModel.getListHandler().addCompoundTableListListener(this);
        }

    public DEParentPane getParentPane() {
    	return mParentPane;
    	}

	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

    public void addDefaultFilters() {
		try {
            for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
                if (mTableModel.isColumnTypeCategory(column)) {
                    addCategoryBrowser(mTableModel);
                    break;
                    }
                }

			for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
				addDefaultFilter(i);

			if (mTableModel.getListHandler().getListCount() > 0) {
				JFilterPanel filter = new JHitlistFilterPanel(mTableModel, allocateFilterFlag());
				filter.addFilterListener(this);
				mContent.add(filter);
				}

			validate();
			repaint();
			}
		catch (FilterException fpe) {
			showErrorMessage(fpe.getMessage());
			}
		}

	/**
	 * We actually only show the message, if the same message has not been shown during the recent 5 seconds
	 */
	private void showErrorMessage(String message) {
		if (System.currentTimeMillis() - mRecentErrorMillis > 5000
		 || !message.equals(mRecentErrorMessage)) {
			JOptionPane.showMessageDialog(mOwner, message);
			mRecentErrorMessage = message;
			}
		mRecentErrorMillis = System.currentTimeMillis();
		}

    private int allocateFilterFlag() throws FilterException {
		int flag = mTableModel.getUnusedRowFlag(true);

		if (flag == -1)
			throw new FilterException("Cannot create filter because maximum number if filters/lists is reached.\nRemove row lists or other filters to allow for new filters.");

		return flag;
    	}

    public void addDefaultFilter(int column) throws FilterException {
		if (mTableModel.isColumnTypeDouble(column)) {
			if (mTableModel.isColumnDataComplete(column)
			 && mTableModel.getMinimumValue(column) != mTableModel.getMaximumValue(column)) {
				JFilterPanel filter = new JRangeFilterPanel(mTableModel, column, allocateFilterFlag());
				filter.addFilterListener(this);
				mContent.add(filter);
				}
			}
		else if (mTableModel.isColumnTypeRangeCategory(column)) {
			if (mTableModel.getCategoryCount(column) <= 9) {
				JFilterPanel filter = new JCategoryFilterPanel(mTableModel, column, allocateFilterFlag());
				filter.addFilterListener(this);
				mContent.add(filter);
				}
			else {
				JFilterPanel filter = new JRangeFilterPanel(mTableModel, column, allocateFilterFlag());
				filter.addFilterListener(this);
				mContent.add(filter);
				}
			}
		else if (mTableModel.isColumnTypeCategory(column)
			  && mTableModel.getCategoryCount(column) <= JCategoryFilterPanel.cPreferredCheckboxCount) {
			JFilterPanel filter = new JCategoryFilterPanel(mTableModel, column, allocateFilterFlag());
			filter.addFilterListener(this);
			mContent.add(filter);
			}
        else if (mTableModel.isColumnTypeStructure(column)) {
            if (mTableModel.hasDescriptorColumn(column, null)) {
                JStructureFilterPanel filter = new JSingleStructureFilterPanel(mOwner, mTableModel, column, null, allocateFilterFlag(), null);
				filter.addFilterListener(this);
                mContent.add(filter);
                }
			}
        else if (mTableModel.isColumnTypeReaction(column)) {
            if (mTableModel.getChildColumn(column, DescriptorConstants.DESCRIPTOR_ReactionFP.shortName) != -1) {
				JReactionFilterPanel filter = new JReactionFilterPanel(mOwner, mTableModel, column, allocateFilterFlag(), null);
				filter.addFilterListener(this);
                mContent.add(filter);
                }
			if (mTableModel.getChildColumn(column, DESCRIPTOR_FFP512.shortName, CompoundTableConstants.cReactionPartReactants) != -1
			 && mTableModel.getChildColumn(column, DESCRIPTOR_FFP512.shortName, CompoundTableConstants.cReactionPartProducts) != -1) {
				JRetronFilterPanel filter = new JRetronFilterPanel(mOwner, mTableModel, column, allocateFilterFlag(), null);
				filter.addFilterListener(this);
				mContent.add(filter);
				}
            }
        else if (mTableModel.isColumnTypeString(column)) {
            JFilterPanel filter = new JTextFilterPanel(mTableModel, column, allocateFilterFlag());
			filter.addFilterListener(this);
            mContent.add(filter);
            }
		}

/*	public void ensureStructureFilter(int idcodeColumn) {
        if (!structureFilterExists(idcodeColumn)) {
        	try {
	        	JStructureFilterPanel filter = new JSingleStructureFilterPanel(mOwner, mTableModel, idcodeColumn, null, allocateFilterFlag(), null);
				filter.addFilterListener(this);
	        	mContent.add(filter);
				validate();
				repaint();
        		}
    		catch (FilterException fpe) {
			    showErrorMessage(fpe.getMessage());
    			}

        	}
		}*/

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cNewTable) {
			Component[] filter = mContent.getComponents();
			for (int i=0; i<filter.length; i++)
				((JFilterPanel)filter[i]).removePanel();
			if (e.getSpecifier() == CompoundTableEvent.cSpecifierDefaultFilters
			 || e.getSpecifier() == CompoundTableEvent.cSpecifierDefaultFiltersAndViews)
				addDefaultFilters();
			validate();
			repaint();
            return;
			}

        Component[] filter = mContent.getComponents();
        for (int i=0; i<filter.length; i++)
            ((JFilterPanel)filter[i]).compoundTableChanged(e);

        if (e.getType() == CompoundTableEvent.cAddColumns) {
		    assert(e.getSource() == mTableModel);
			try {
				for (int column=e.getColumn(); column<mTableModel.getTotalColumnCount(); column++)
					addDefaultFilter(column);
				}
			catch (FilterException fpe) {
				showErrorMessage(fpe.getMessage());
				}
			validate();
			}
        }

    public void listChanged(CompoundTableListEvent e) {
        CompoundTableListHandler hitlistHandler = mTableModel.getListHandler();

        if (e.getType() == CompoundTableListEvent.cDelete) {
            if (hitlistHandler.getListCount() == 0) {
                Component[] filter = mContent.getComponents();
                for (int i=0; i<filter.length; i++)
                    if (filter[i] instanceof JHitlistFilterPanel)
                        ((JFilterPanel)filter[i]).removePanel();
                }
            }

        boolean hitlistFiltersVisible = false;
        Component[] filter = mContent.getComponents();
        for (int i=0; i<filter.length; i++) {
            if (filter[i] instanceof JHitlistFilterPanel) {
                ((JHitlistFilterPanel)filter[i]).listChanged(e);
                hitlistFiltersVisible = true;
                }
            }

        if (e.getType() == CompoundTableListEvent.cAdd) {
            if (!hitlistFiltersVisible) {
                try {
                	JHitlistFilterPanel f = new JHitlistFilterPanel(mTableModel, allocateFilterFlag());
    				f.addFilterListener(this);
                    mContent.add(f);
                    }
                catch (FilterException fpe) {}
                validate();
                }
            }
        }

    public JCategoryBrowser addCategoryBrowser(CompoundTableModel tableModel) throws FilterException {
        JCategoryBrowser filter = new JCategoryBrowser(mOwner, tableModel, allocateFilterFlag());
		filter.addFilterListener(this);
        mContent.add(filter);
        validate();
        repaint();
        return filter;
        }

    public JTextFilterPanel addTextFilter(CompoundTableModel tableModel, int column) throws FilterException {
		if (column == JFilterPanel.PSEUDO_COLUMN_ALL_COLUMNS || tableModel.getColumnSpecialType(column) == null) {
			JTextFilterPanel filter = new JTextFilterPanel(tableModel, column, allocateFilterFlag());
			filter.addFilterListener(this);
			mContent.add(filter);
			validate();
			repaint();
			return filter;
			}
		return null;
		}

	public JRangeFilterPanel addDoubleFilter(CompoundTableModel tableModel, int column) throws FilterException {
		if (tableModel.isColumnTypeDouble(column)) {
			JRangeFilterPanel filter = new JRangeFilterPanel(tableModel, column, allocateFilterFlag());
			filter.addFilterListener(this);
			mContent.add(filter);
			validate();
			repaint();
			return filter;
			}
		return null;
		}

	public JCategoryFilterPanel addCategoryFilter(CompoundTableModel tableModel, int column) throws FilterException {
		if (tableModel.isColumnTypeCategory(column)) {
			JCategoryFilterPanel filter = new JCategoryFilterPanel(tableModel, column, allocateFilterFlag());
			filter.addFilterListener(this);
			mContent.add(filter);
			validate();
			repaint();
			return filter;
			}
		return null;
		}

	public JSingleStructureFilterPanel addStructureFilter(CompoundTableModel tableModel, int column, String reactionPart, StereoMolecule mol) throws FilterException {
		JSingleStructureFilterPanel filter = new JSingleStructureFilterPanel(mOwner, tableModel, column, reactionPart, allocateFilterFlag(), mol);
		filter.addFilterListener(this);
		mContent.add(filter);
		validate();
		repaint();
		return filter;
		}

	public JMultiStructureFilterPanel addStructureListFilter(CompoundTableModel tableModel, int column, String reactionPart, boolean isSSS) throws FilterException {
		JMultiStructureFilterPanel filter = new JMultiStructureFilterPanel(mOwner, tableModel, column, reactionPart, allocateFilterFlag(), isSSS);
		mContent.add(filter);
		filter.addFilterListener(this);
		validate();
		repaint();
		return filter;
		}

    public JReactionFilterPanel addReactionFilter(CompoundTableModel tableModel, int column, Reaction rxn) throws FilterException {
        JReactionFilterPanel filter = new JReactionFilterPanel(mOwner, tableModel, column, allocateFilterFlag(), rxn);
		filter.addFilterListener(this);
        mContent.add(filter);
        validate();
        repaint();
        return filter;
        }

	public JRetronFilterPanel addRetronFilter(CompoundTableModel tableModel, int column, StereoMolecule mol) throws FilterException {
		JRetronFilterPanel filter = new JRetronFilterPanel(mOwner, tableModel, column, allocateFilterFlag(), mol);
		filter.addFilterListener(this);
		mContent.add(filter);
		validate();
		repaint();
		return filter;
		}

	public JHitlistFilterPanel addHitlistFilter(CompoundTableModel tableModel) throws FilterException {
		JHitlistFilterPanel filter = new JHitlistFilterPanel(tableModel, allocateFilterFlag());
		filter.addFilterListener(this);
		mContent.add(filter);
		validate();
		repaint();
		return filter;
		}

	public void disableAllFilters() {
		Component[] filter = mContent.getComponents();
		for (int i=0; i<filter.length; i++) {
			((JFilterPanel)filter[i]).setIsUserChange(false);
			filter[i].setEnabled(false);
			((JFilterPanel)filter[i]).setIsUserChange(true);
			}
		}

	public void enableAllFilters() {
		Component[] filter = mContent.getComponents();
		for (int i=0; i<filter.length; i++) {
			((JFilterPanel)filter[i]).setIsUserChange(false);
			filter[i].setEnabled(true);
			((JFilterPanel)filter[i]).setIsUserChange(true);
			}
		}

	public void removeAllFilters() {
		Component[] filter = mContent.getComponents();
		for (int i=0; i<filter.length; i++)
			((JFilterPanel)filter[i]).removePanel();
		}

	public void resetAllFilters() {
		Component[] filter = mContent.getComponents();
		for (int i=0; i<filter.length; i++)
			((JFilterPanel)filter[i]).reset();
		}

	public int getFilterCount() {
		return mContent.getComponentCount();
		}

	public JFilterPanel getFilter(int index) {
		return (JFilterPanel)mContent.getComponent(index);
		}

	public int getFilterDuplicateIndex(JFilterPanel filterPanel, int column) {
		int index = 0;
		Component[] filter = mContent.getComponents();
		for (int i=0; i<filter.length; i++) {
			if (filter[i] == filterPanel)
				return index;
			if (filterPanel.getClass() == filter[i].getClass()
			 && filterPanel.getColumnIndex() == ((JFilterPanel)filter[i]).getColumnIndex())
				index++;
			}
		return index;
		}

	/**
	 *
	 * @param filterType
	 * @param column -1 if this filter type doesn't need to match the column
	 * @param duplicateIndex
	 * @return
	 */
	public JFilterPanel getFilter(int filterType, int column, int duplicateIndex) {
		int index = 0;
		Component[] component = mContent.getComponents();
		for (int i=0; i<component.length; i++) {
			JFilterPanel filter = (JFilterPanel)component[i];
			if (filterType == filter.getFilterType()
			 && (column == -1 || column == filter.getColumnIndex())) {
				if (index == duplicateIndex)
					return filter;
				index++;
				}
			}
		return null;
		}

	public String getFilterSettings(JFilterPanel filter) {
		int column = filter.getColumnIndex();
		String columnName = (column == JFilterPanel.PSEUDO_COLUMN_ALL_COLUMNS) ? JFilterPanel.ALL_COLUMN_CODE
				: (column < 0) ? null : mTableModel.getColumnTitleNoAlias(column);

		String reactionPart = (filter instanceof JStructureFilterPanel) ? ((JStructureFilterPanel)filter).getReactionPart() : null;
		if (reactionPart == null)
			reactionPart = "";

		String property = null;
		if (filter instanceof JCategoryBrowser)
			property = cFilterTypeCategoryBrowser;
		else if (filter instanceof JRangeFilterPanel)
			property = cFilterTypeDouble + "\t" + columnName;
		else if (filter instanceof JCategoryFilterPanel)
			property = cFilterTypeCategory + "\t" + columnName;
		else if (filter instanceof JTextFilterPanel)
			property = cFilterTypeText + "\t" + columnName;
		else if (filter instanceof JHitlistFilterPanel)
			property = cFilterTypeHitlist;
		else if (filter instanceof JSingleStructureFilterPanel)
			property = cFilterTypeStructure + reactionPart + "\t" + columnName;
		else if (filter instanceof JMultiStructureFilterPanel && ((JMultiStructureFilterPanel)filter).supportsSSS())
			property = cFilterTypeSSSList + reactionPart + "\t" + columnName;
		else if (filter instanceof JMultiStructureFilterPanel && ((JMultiStructureFilterPanel)filter).supportsSim())
			property = cFilterTypeSIMList + reactionPart + "\t" + columnName;
		else if (filter instanceof JReactionFilterPanel)
			property = cFilterTypeReaction + "\t" + columnName;
		else if (filter instanceof JRetronFilterPanel)
			property = cFilterTypeRetron + "\t" + columnName;

		String settings = filter.getSettings();
		if (settings != null)
			property = property.concat("\t" + settings);

		return property;
		}

	public JFilterPanel createFilterFromSettings(String property, boolean suppressMessages) {
		// Formats prior V2.7.0 didn't store column names for structure filters.
		if (property.equals(cFilterTypeStructure))
			property = cFilterTypeStructure + "\tStructure";

		int index1 = property.indexOf('\t');

		// Formats prior V2.7.0 didn't store column names for structure filters.
		if (property.startsWith(cFilterTypeStructure+"\t#substructure#")
				|| property.startsWith(cFilterTypeStructure+"\t#similarity#")
				|| property.startsWith(cFilterTypeStructure+"\t#inverse#\t#substructure#")
				|| property.startsWith(cFilterTypeStructure+"\t#inverse#\t#similarity#"))
			property = cFilterTypeStructure+"\tStructure"+property.substring(index1);

		JFilterPanel filter = null;
		if (property.startsWith(cFilterTypeCategoryBrowser)) {
			try {
				filter = addCategoryBrowser(mTableModel);
				if (index1 != -1)
					filter.applySettings(property.substring(index1+1), suppressMessages);
			}
			catch (DEPruningPanel.FilterException fpe) {}
		}
		else if (property.startsWith(cFilterTypeHitlist)) {
			if (mTableModel.getListHandler().getListCount() > 0) {
				try {
					filter = addHitlistFilter(mTableModel);
					if (index1 != -1)
						filter.applySettings(property.substring(index1+1), suppressMessages);
				}
				catch (DEPruningPanel.FilterException fpe) {}
			}
		}
		else if (index1 != -1) {
			int index2 = property.indexOf('\t', index1+1);
			String columnName = (index2 == -1) ? property.substring(index1+1)
					: property.substring(index1+1, index2);

			int column = (columnName.equals(JFilterPanel.ALL_COLUMN_CODE)) ?
					JFilterPanel.PSEUDO_COLUMN_ALL_COLUMNS : mTableModel.findColumn(columnName);

			if (column != -1) {
				try {
					if (property.startsWith(cFilterTypeDouble))
						filter = addDoubleFilter(mTableModel, column);
					else if (property.startsWith(cFilterTypeCategory))
						filter = addCategoryFilter(mTableModel, column);
					else if (property.startsWith(cFilterTypeText))
						filter = addTextFilter(mTableModel, column);
					else if (property.startsWith(cFilterTypeStructure))
						filter = addStructureFilter(mTableModel, column, getReactionPart(property, cFilterTypeStructure), null);
					else if (property.startsWith(cFilterTypeSSSList))
						filter = addStructureListFilter(mTableModel, column, getReactionPart(property, cFilterTypeSSSList), true);
					else if (property.startsWith(cFilterTypeSIMList))
						filter = addStructureListFilter(mTableModel, column, getReactionPart(property, cFilterTypeSIMList), false);
					else if (property.startsWith(cFilterTypeReaction))
						filter = addReactionFilter(mTableModel, column, null);
					else if (property.startsWith(cFilterTypeRetron))
						filter = addRetronFilter(mTableModel, column, null);
					if (filter != null && index2 != -1) {
						filter.applySettings(property.substring(index2 + 1), suppressMessages);
						}
					}
				catch (DEPruningPanel.FilterException fpe) {}
				}
			}
		return filter;
		}

	private String getReactionPart(String property, String filterType) {
		int index = property.indexOf('\t');
		return (index <= filterType.length()) ? null : property.substring(filterType.length(), index);
		}

    public class FilterException extends Exception {
        private static final long serialVersionUID = 0x20110325;

        public FilterException(String msg) {
    		super(msg);
    		}
    	}

	@Override
	public void filterChanged(FilterEvent e) {
		if (DEMacroRecorder.getInstance().isRecording()) {
			AbstractTask task = null;
			JFilterPanel filter = e.getSource();
			if (e.getType() == FilterEvent.FILTER_CLOSED)
				task = new DETaskCloseFilter(mOwner, this, filter);
			else if (e.getSource() instanceof JCategoryBrowser)
				task = new DETaskChangeCategoryBrowser(mOwner, this, filter);
			else if (e.getSource() instanceof JCategoryFilterPanel)
				task = new DETaskChangeCategoryFilter(mOwner, this, filter);
			else if (e.getSource() instanceof JHitlistFilterPanel)
				task = new DETaskChangeListFilter(mOwner, this, filter);
			else if (e.getSource() instanceof JRangeFilterPanel)
				task = new DETaskChangeRangeFilter(mOwner, this, filter);
			else if (e.getSource() instanceof JMultiStructureFilterPanel) {
				if (((JMultiStructureFilterPanel)e.getSource()).supportsSSS())
					task = new DETaskChangeSubstructureListFilter(mOwner, this, filter);
				else
					task = new DETaskChangeSimilarStructureListFilter(mOwner, this, filter);
				}
			else if (e.getSource() instanceof JReactionFilterPanel)
				task = new DETaskChangeReactionFilter(mOwner, this, filter);
			else if (e.getSource() instanceof JRetronFilterPanel)
				task = new DETaskChangeRetronFilter(mOwner, this, filter);
			else if (e.getSource() instanceof JSingleStructureFilterPanel)
				task = new DETaskChangeStructureFilter(mOwner, this, filter);
			else if (e.getSource() instanceof JTextFilterPanel)
				task = new DETaskChangeTextFilter(mOwner, this, filter);

			if (task != null)
				DEMacroRecorder.record(task, task.getPredefinedConfiguration());
			}
		}

	/**
	 * Gets the first filter that runs an animation
	 * @return
	 */
	public JFilterPanel getFirstAnimatingFilter() {
		for (Component component:mContent.getComponents()) {
			JFilterPanel filter = (JFilterPanel)component;
			if (filter.isAnimating())
				return filter;
			}
		return null;
		}

	public int getAnimationCount() {
		int count = 0;
		for (Component component:mContent.getComponents()) {
			JFilterPanel filter = (JFilterPanel)component;
			if (filter.isAnimating())
				count++;
			}
		return count;
		}

	public void toggleSuspendAnimations() {
		boolean isSuspended = false;
		for (Component component:mContent.getComponents()) {
			JFilterPanel filter = (JFilterPanel)component;
			if (filter.isEnabled() && filter.isAnimationSuspended()) {
				isSuspended = true;
				break;
				}
			}
		for (Component component:mContent.getComponents())
			((JFilterPanel)component).setAnimationSuspended(!isSuspended);
		}

	public void skipAnimationFrames(long millis) {
		for (Component component:mContent.getComponents()) {
			JFilterPanel filter = (JFilterPanel)component;
			if (filter.isEnabled())
				filter.skipAnimationFrames(millis);
			}
		}

	public void initializeDrop(int dropAction) {
		if (dropAction != DnDConstants.ACTION_NONE) {
			new DropTarget(mContent, dropAction, this, canDrop());
			}
		}

	private int getDropIndex(DropTargetDragEvent dtde) {
		int allowance = HiDPIHelper.scale(8);
		int y = dtde.getLocation().y;

		if (y <= allowance)
			return 0;

		for (int i=0; i<mContent.getComponentCount(); i++) {
			Component c = mContent.getComponent(i);
			if (y >= c.getY() + c.getHeight() - allowance && y <= c.getY() + c.getHeight() + allowance)
				return i+1;
			}

		return -1;
		}

	private boolean canDrop() {
		return true;
		}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		mIsDropOK = canDrop() && isDropOK(dtde) ;
		if (!mIsDropOK) {
			dtde.rejectDrag();
			}
		else {
			mDropIndex = getDropIndex(dtde);
			if (mDropIndex != -1)
				mContent.repaint();
			}
		}

	private boolean isDropOK(DropTargetDragEvent dtde) {
		if (!dtde.isDataFlavorSupported(FilterTransferable.DF_FILTER_PANEL_OBJ)
		 && !dtde.isDataFlavorSupported(FilterTransferable.DF_FILTER_PANEL_DEF))
			return false;

		return ((dtde.getDropAction() & ALLOWED_DROP_ACTIONS) != 0);
		}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		mScroller.autoScroll();

		if (mIsDropOK) {
			int dropIndex = getDropIndex(dtde);
			if (mDropIndex != dropIndex) {
				mDropIndex = dropIndex;
				mContent.repaint();
				}
			}
		}

	/**
	 * Called if the user has modified
	 * the current drop gesture.
	 * <P>
	 * @param dtde the <code>DropTargetDragEvent</code>
	 */

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {}

	@Override
	public void dragExit(DropTargetEvent dte) {
		mIsDropOK = false;
		if (mDropIndex != -1) {
			mDropIndex = -1;
			repaint();
			}
		}

	@Override
	public void drop(DropTargetDropEvent dtde) {
		boolean success = false;
		try {
			DataFlavor chosen = chooseDropFlavor(dtde);
			if (chosen != null && mDropIndex != -1) {
				dtde.acceptDrop(ALLOWED_DROP_ACTIONS);
				Transferable tr = dtde.getTransferable();

				// if we move and if it is within the same application
				if (chosen.equals(FilterTransferable.DF_FILTER_PANEL_OBJ)) {
					JFilterPanel fp = (JFilterPanel)tr.getTransferData(chosen);
					if (dtde.getDropAction() == DnDConstants.ACTION_MOVE) {
						for (int i=0; i<mContent.getComponentCount(); i++) {
							if (mContent.getComponent(i) == fp) {
								mDisableEvents = true;
								mContent.remove(fp);
								mDisableEvents = false;

								if (mDropIndex > i)
									mDropIndex--;

								mContent.add(fp, mDropIndex);
								}
							}
						}
					else {  // is COPY, i.e. we need to duplicate
						String settings = getFilterSettings(fp);
						fp = createFilterFromSettings(settings, false);
						mContent.add(fp, mDropIndex);
						}
					}
				else {
					String settings = (String)tr.getTransferData(chosen);
					JFilterPanel fp = createFilterFromSettings(settings, false);
					}

				validate();
				repaint();
				success = true;
				}
			else {
				dtde.rejectDrop();
				}
			}
		catch (Exception ex) {
			ex.printStackTrace();
			}
		mIsDropOK = false;
		mDropIndex = -1;
		dtde.dropComplete(success);
		}

	private DataFlavor chooseDropFlavor(DropTargetDropEvent dtde) {
		if (dtde.isDataFlavorSupported(FilterTransferable.DF_FILTER_PANEL_OBJ))
			return FilterTransferable.DF_FILTER_PANEL_OBJ;
		if (dtde.isDataFlavorSupported(FilterTransferable.DF_FILTER_PANEL_DEF))
			return FilterTransferable.DF_FILTER_PANEL_DEF;
		return null;
		}
	}
