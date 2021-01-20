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

import java.util.ArrayList;

public class FormObjectFactory {
	public static final String TYPE_STRUCTURE = "structure";
    public static final String TYPE_REACTION = "reaction";
	public static final String TYPE_SINGLE_LINE_TEXT = "textLine";
	public static final String TYPE_MULTI_LINE_TEXT = "textArea";

	private ArrayList<FormObjectSpecifier> mSupportedTypeList;

	public FormObjectFactory() {
		mSupportedTypeList = new ArrayList<FormObjectSpecifier>();
		addSupportedType(TYPE_SINGLE_LINE_TEXT, JTextFormObject.class);
		addSupportedType(TYPE_MULTI_LINE_TEXT, JTextFormObject.class);
		addSupportedType(TYPE_STRUCTURE, JStructureFormObject.class);
        addSupportedType(TYPE_REACTION, JReactionFormObject.class);
		}

	public AbstractFormObject createFormObject(String key, String type) {
		Object[] initArgs = new Object[2];
		initArgs[0] = key;
		initArgs[1] = type;
		try {
			for (int i=0; i<mSupportedTypeList.size(); i++) {
				FormObjectSpecifier objectSpecifier = mSupportedTypeList.get(i);
				if (type.equals(objectSpecifier.type)) {
					return (AbstractFormObject)objectSpecifier.formObjectClass.getConstructors()[0].newInstance(initArgs);
					}
				}
			}
		catch (Exception e) { e.printStackTrace(); }
		return null;
		}

/*	public String getTypeString(int type) {
		return SUPPORTED_TYPE[type];
		}

	public int getType(String typeString) {
		for (int i=0; i<SUPPORTED_TYPE.length; i++)
			if (typeString.equals(SUPPORTED_TYPE[i]))
				return i;
		return -1;
		}	*/

	public void addSupportedType(String typeString, Class<?> formObjectClass) {
		mSupportedTypeList.add(new FormObjectSpecifier(typeString, formObjectClass));
		}
	}

class FormObjectSpecifier {
	public String type;
	public Class<?> formObjectClass;

	public FormObjectSpecifier(String theType, Class<?> theClass) {
		type = theType;
		formObjectClass = theClass;
		}
	}
