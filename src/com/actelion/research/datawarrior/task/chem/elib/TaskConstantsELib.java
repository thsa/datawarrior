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

package com.actelion.research.datawarrior.task.chem.elib;

public interface TaskConstantsELib {
	String[] START_COMPOUND_TEXT = { "Default", "Custom", "Random", "From the clipboard", "From a file...", "From a structure column...", "From all selected rows...", "From a row list..." };
	String[] START_COMPOUND_CODE = { "default", "custom", "random", "clipboard", "file", "column", "selected", "list" };
	int DEFAULT_OPTION = 0;
	int CUSTOM_OPTION = 1;
	int RANDOM_OPTION = 2;  // first that can be deferred
	int CLIPBOARD_OPTION = 3;
	int FILE_OPTION = 4;    // first that needs detail info when deferred
	int COLUMN_OPTION = 5;
	int SELECTED_OPTION = 6;
	int LIST_OPTION = 7;

	String GENERATIONS_AUTOMATIC = "automatic";
	String GENERATIONS_UNLIMITED = "unlimited";
	String DEFAULT_CYCLE_COUNT = GENERATIONS_AUTOMATIC;
	String[] GENERATION_OPTIONS = {GENERATIONS_AUTOMATIC, "10", "25", "50", "75", "100", "150", "200", "300", "400", GENERATIONS_UNLIMITED };

	String DEFAULT_COMPOUND_COUNT = "128";
	String[] COMPOUND_OPTIONS = {"8", "16", "32", "64", "128", "256", "512", "1024", "2048", "4096"};

	String DEFAULT_SURVIVAL_COUNT = "8";
	String[] SURVIVAL_OPTIONS = {"1", "2", "4", "8", "16", "32", "64", "128", "256"};

	String DEFAULT_RUNS = "1";
	String[] RUN_OPTIONS = {"1", "2", "4", "8", "16", "32", "64"};

	String[] COMPOUND_KIND_TEXT = {"Approved drugs", "Natural products"};
	String[] COMPOUND_KIND_CODE = {"drugs", "naturalProducts"};
	String[] COMPOUND_KIND_FILE = {"drugbank_nosugar.typ", "derep.typ"};
	int COMPOUND_KIND_DRUGS = 0;
	int COMPOUND_KIND_NATURAL_PRODUCTS = 1;

	String PROPERTY_START_SET_OPTION = "startSetOption";
	String PROPERTY_START_SET_DEFERRED = "startSetDeferred";
	String PROPERTY_START_SET_FILE = "startSetFile";
	String PROPERTY_START_SET_COLUMN = "startSetColumn";
	String PROPERTY_START_SET_LIST = "startSetList";
	String PROPERTY_START_COMPOUNDS = "startSet";
	String PROPERTY_SURVIVAL_COUNT = "survivalCount";
	String PROPERTY_CYCLE_COUNT = "generationCount";
	String PROPERTY_COMPOUND_COUNT = "generationSize";
	String PROPERTY_COMPOUND_KIND = "kind";
	String PROPERTY_FITNESS_PARAM_COUNT = "paramCount";
	String PROPERTY_FITNESS_PARAM_CONFIG = "paramConfig";
	String PROPERTY_RUN_COUNT = "runs";
}
