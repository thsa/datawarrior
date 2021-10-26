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
	public static final String[] START_COMPOUND_CODE = { "default", "random", "file", "custom", "onthefly", "list", "selected", "all" };

	public static final String GENERATIONS_AUTOMATIC = "automatic";
	public static final String GENERATIONS_UNLIMITED = "unlimited";
	public static final String DEFAULT_GENERATIONS = GENERATIONS_AUTOMATIC;
	public static final String[] GENERATION_OPTIONS = {GENERATIONS_AUTOMATIC, "10", "25", "50", "75", "100", "150", "200", "300", "400", GENERATIONS_UNLIMITED };

	public static final String DEFAULT_COMPOUNDS = "128";
	public static final String[] COMPOUND_OPTIONS = {"8", "16", "32", "64", "128", "256", "512", "1024", "2048", "4096"};

	public static final String DEFAULT_SURVIVALS = "8";
	public static final String[] SURVIVAL_OPTIONS = {"1", "2", "4", "8", "16", "32", "64", "128", "256"};

	public static final String[] COMPOUND_KIND_TEXT = {"Approved drugs", "Natural products"};
	public static final String[] COMPOUND_KIND_CODE = {"drugs", "naturalProducts"};
	public static final String[] COMPOUND_KIND_FILE = {"drugbank_nosugar.typ", "derep.typ"};
	public static final int COMPOUND_KIND_DRUGS = 0;
	public static final int COMPOUND_KIND_NATURAL_PRODUCTS = 1;

	public static final String PROPERTY_START_SET_OPTION = "startOption";
    public static final String PROPERTY_START_COMPOUNDS = "startSet";
    public static final String PROPERTY_SURVIVAL_COUNT = "survivalCount";
	public static final String PROPERTY_GENERATION_COUNT = "generationCount";
	public static final String PROPERTY_GENERATION_SIZE = "generationSize";
    public static final String PROPERTY_COMPOUND_KIND = "kind";
    public static final String PROPERTY_FITNESS_PARAM_COUNT = "paramCount";
    public static final String PROPERTY_FITNESS_PARAM_CONFIG = "paramConfig";

}
