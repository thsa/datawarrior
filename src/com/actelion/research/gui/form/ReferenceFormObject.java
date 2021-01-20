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

package com.actelion.research.gui.form;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public abstract class ReferenceFormObject extends AbstractFormObject {
	private ReferenceResolver	mReferenceResolver;

	public ReferenceFormObject(String key, String type) {
			// overwrite to set mComponent
			// mComponent must be an instance of JResultDetailView
		super(key, type);
		}

	/**
	 * getData() of ReferenceFormObject is currently not supported.
	 * These form objects are not editable and never fire dataChanged().
	 */
	public Object getData() {
	    return null;
	    }

	public void setData(Object data) {
		if (mReferenceResolver == null)
			((JResultDetailView)mComponent).setDetailData(data);
		else
			((JResultDetailView)mComponent).setReferences((String[])data);
		}

	public void setReferences(String[] reference) {
		((JHTMLDetailView)mComponent).setReferences(reference);
		}

	public void setReferenceResolver(ReferenceResolver referenceResolver) {
		mReferenceResolver = referenceResolver;
		((JResultDetailView)mComponent).setReferenceResolver(referenceResolver);
		}

	public void setReferenceSource(RemoteDetailSource referenceSource) {
		((JResultDetailView)mComponent).setDetailSource(referenceSource);
		}

	public void printContent(Graphics2D g2D, Rectangle2D.Double r, float scale, Object data, boolean isMultipleRows) {
        if (mReferenceResolver == null)
		    ((JResultDetailView)mComponent).print(g2D, r, scale, data);
		else
		    ((JResultDetailView)mComponent).printReferences(g2D, r, scale, (String[])data, isMultipleRows);
		}
	}
