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

package org.openmolecules.datawarrior.plugin;

import javax.swing.*;
import java.awt.*;

/**
 * IUserInterfaceHelper is a class passed to the IPluginTask.createDialogContent() method.
 * It allows to create and embed DataWarrior specific GUI components into the query dialog,
 * that is constructed by a plugin. Currently, a chemical editor is the only component
 * that can be created and used.
 */
public interface IUserInterfaceHelper {
	int COLUMN_TYPE_STRUCTURE = 1;
	int COLUMN_TYPE_REACTION = 2;
	int COLUMN_TYPE_NUMERICAL = 4;
	int COLUMN_TYPE_DATE = 8;
	int COLUMN_TYPE_CATEGORIES = 16;
	int COLUMN_TYPE_TEXT = 32;
	int COLUMN_TYPE_ANY = 63;

	/**
	 * For HiDPI monitors the user may have set a scaling factor in the operating system
	 * (e.g. ControlPanel in Windows). In DataWarrior standard user interface elements as
	 * JLabels, JButtons, and JComboBoxes are scaled automatically. However, custom elements,
	 * borders and padding should be scaled by this factor.
	 * @return UI scaling factor for highly resolved screens, e.g. 4k-monitors
	 */
	float getUIScaleFactor();

	/**
	 * Macintosh computers with HiDPI (Retina) monitors use a concept different from Linux
	 * and Windows. While the coordinate system of a Retina MacBookPro is equal to a non-retina
	 * model, its physical resolution is twice as high. For placing JLabels, JButtons, etc, one
	 * uses the normal unscaled coordinate system, but when drawing images, these should be
	 * prepared in double resolution and scaled down by factor 2, when drawing.
	 * @return 1 or 2 for non-retina and retina displays, respectively
	 */
	float getRetinaScaleFactor();

	/**
	 * Creates and returns a JComponent that represents and shows a chemical 2D-structure.
	 * A popup menu or double click opens a chemical editor that allows editing the shown
	 * molecule or substructures, depending on the editor mode. In substructure mode the editor
	 * allows to edit atom and bond specific query features.
	 * @return
	 */
	IChemistryPanel getChemicalEditor();

	/**
	 * Creates and returns a JComponent that represents and shows a chemical 3D-structure.
	 * A popup menu allows to load a 3D-structure from various sources and file formats.
	 * @param mode IConformerPanel.MODE_CONFORMER or IConformerPanel.MODE_PROTEIN
	 * @return
	 */
	public IConformerPanel getConformerEditor(int mode);

	/**
	 * Creates and return a JComboBox prefilled with all columns of the current fron window
	 * that match the specified column type. If the method during macro definition, then the
	 * JComboBox is editable to allow free text column name specification.
	 * @param columnType one or more of the COLUMN_TYPE_xxx flags
	 * @return
	 */
	JComboBox<String> getComboBoxForColumnSelection(int columnType);

	/**
	 * Creates a plugin helper instance for any purpose
	 * @return
	 */
	IPluginHelper getPluginHelper();

	/**
	 * @return the parent dialog, which may be null, if dialog is not visible
	 */
	JDialog getParentDialog();

	/**
	 * @return the parent dialog, if it is visible; otherwise the DataWarrior's front most window (Dialog or Frame)
	 */
	Component getParentComponent();

	/**
	 * @param text text shown on the OK-button (default is 'OK')
	 */
	void setDefaultButtonText(String text);
	}
