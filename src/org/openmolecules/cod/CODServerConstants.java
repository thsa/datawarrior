/*
 * Copyright 2016, Thomas Sander, openmolecules.org
 *
 * This file is part of COD-Structure-Server.
 *
 * COD-Structure-Server is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * COD-Structure-Server is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with COD-Structure-Server.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package org.openmolecules.cod;

public interface CODServerConstants {
	public static final int RESULT_STRUCTURE_COLUMNS = 4;
	public static final int RESULT_COLUMN_IDCODE = 0;
	public static final int RESULT_COLUMN_COORDS2D = 1;
	public static final int RESULT_COLUMN_FFP512 = 2;
	public static final int RESULT_COLUMN_COORDS3D = 3;
	public static final String RESULT_COLUMN_NAME_COD_NO = "COD Number";

	public static final String REQUEST_HELP = "help";
	public static final String REQUEST_TEMPLATE = "template";
	public static final String REQUEST_MOLFILE2 = "molfile2";
	public static final String REQUEST_MOLFILE3 = "molfile3";
	public static final String QUERY_STRUCTURE_SEARCH_SPEC = "ssspec";
	public static final String QUERY_ORGANIC_ONLY = "organicOnly";
	public static final String QUERY_AUTHOR = "author";
	public static final String QUERY_YEAR = "year";

	// these are the names of individual put/get parameters if the client doesn't use a query object
	public static final String PARAMETER_COD_ID = "id";
	public static final String PARAMETER_SMILES = "smiles";
	public static final String PARAMETER_SEARCH_TYPE = "searchType";
	public static final String PARAMETER_THRESHOLD = "threshold";
	public static final String PARAMETER_ORGANIC_ONLY = "organicOnly";
	public static final String PARAMETER_AUTHOR = "author";
	public static final String PARAMETER_YEAR = "year";
	public static final String SEARCH_TYPE_SSS = "substructure";
	public static final String SEARCH_TYPE_SIM = "similarity";
	}
