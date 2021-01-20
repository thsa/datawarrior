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

package com.actelion.research.table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableListEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.table.view.VisualizationColorListener;


public class CompoundTableColorHandler implements VisualizationColorListener {
	public static final int FOREGROUND = 1;
	public static final int BACKGROUND = 2;

	private CompoundTableModel mTableModel;
	private ArrayList<CompoundTableColorHandler.ColorListener> mListenerList;
    private TreeMap<String,VisualizationColor> mForegroundColorMap,mBackgroundColorMap;

    public CompoundTableColorHandler(CompoundTableModel tableModel) {
    	mTableModel = tableModel;
		mForegroundColorMap = new TreeMap<String,VisualizationColor>();
		mBackgroundColorMap = new TreeMap<String,VisualizationColor>();
		mListenerList = new ArrayList<CompoundTableColorHandler.ColorListener>();
    	}

    public void addColorListener(CompoundTableColorHandler.ColorListener l) {
    	mListenerList.add(l);
    	}

    public void removeColorListener(CompoundTableColorHandler.ColorListener l) {
    	mListenerList.remove(l);
    	}

    public void compoundTableChanged(CompoundTableEvent e) {
        if (e.getType() == CompoundTableEvent.cNewTable) {
    		mForegroundColorMap.clear();
    		mBackgroundColorMap.clear();
            }
        if (e.getType() == CompoundTableEvent.cRemoveColumns) {
            Iterator<String> iterator = mForegroundColorMap.keySet().iterator();
            while (iterator.hasNext())
                if (mTableModel.findColumn(iterator.next()) == -1)
                    iterator.remove();
            iterator = mBackgroundColorMap.keySet().iterator();
            while (iterator.hasNext())
                if (mTableModel.findColumn(iterator.next()) == -1)
                    iterator.remove();
            }

        for (VisualizationColor vc:mForegroundColorMap.values())
        	vc.compoundTableChanged(e);
        for (VisualizationColor vc:mBackgroundColorMap.values())
        	vc.compoundTableChanged(e);
        }

	public void hitlistChanged(CompoundTableListEvent e) {
        for (VisualizationColor vc:mForegroundColorMap.values())
        	vc.listChanged(e);
        for (VisualizationColor vc:mBackgroundColorMap.values())
        	vc.listChanged(e);
		}

	/**
	 * @param column
	 * @param type FOREGROUND or BACKGROUND
	 * @return
	 */
	public boolean hasColorAssigned(int column, int type) {
		TreeMap<String,VisualizationColor> colorMap = getColorMap(type);
		String title = mTableModel.getColumnTitleNoAlias(column);
		if (title != null) {
			VisualizationColor colorHelper = colorMap.get(title);
			if (colorHelper != null)
				return colorHelper.getColorColumn() != JVisualization.cColumnUnassigned;
			}
		return false;
		}

	/**
	 * Gets or creates if necessary a new VisualizationColor associated with column 
	 * @param column
	 * @param type FOREGROUND or BACKGROUND
	 * @return
	 */
	public VisualizationColor getVisualizationColor(int column, int type) {
		String columnName = mTableModel.getColumnTitleNoAlias(column);
		TreeMap<String,VisualizationColor> colorMap = getColorMap(type);
		VisualizationColor colorHelper = colorMap.get(columnName);
		if (colorHelper == null) {
			colorHelper = new VisualizationColor(mTableModel, this);
			colorMap.put(columnName, colorHelper);
//			colorHelper.setColor(column);
			}
		return colorHelper;
		}

	/**
	 * @param type FOREGROUND or BACKGROUND
	 * @return
	 */
	private TreeMap<String,VisualizationColor> getColorMap(int type) {
		return (type == FOREGROUND) ? mForegroundColorMap
			 : (type == BACKGROUND) ? mBackgroundColorMap
			 : null;
		}

	public void colorChanged(VisualizationColor source) {
		Iterator<String> iterator = mForegroundColorMap.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			if (source == mForegroundColorMap.get(key)) {
				for (CompoundTableColorHandler.ColorListener l:mListenerList)
					l.colorChanged(mTableModel.findColumn(key), FOREGROUND, source);
				return;
				}
			}
		iterator = mBackgroundColorMap.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			if (source == mBackgroundColorMap.get(key)) {
				for (CompoundTableColorHandler.ColorListener l:mListenerList)
					l.colorChanged(mTableModel.findColumn(key), BACKGROUND, source);
				return;
				}
			}
		}

	public interface ColorListener {
		public void colorChanged(int column, int type, VisualizationColor color);
		}
	}
