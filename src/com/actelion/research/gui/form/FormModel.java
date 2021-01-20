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

public interface FormModel {
	public int getKeyCount();
	public String getKey(int no);
	public String getTitle(String key);
	public Object getValue(String key);
	public String getType(String key);
    public boolean setValue(String key, Object value); // vetoable change

    /**
     * If the model is set editable, then getValue() is supposed to return
     * original values, which may be edited and returned by setValue().
     * If the model is not set editable getValue() may return summarized or
     * decorated values derived from the original value, e.g. '2.1 (mean of 2)'.
     * @param isEditable
     */
    public void setEditable(boolean isEditable);
	}
