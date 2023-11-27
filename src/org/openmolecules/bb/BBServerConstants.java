/*
 * Copyright 2019, Thomas Sander, openmolecules.org
 *
 * This file is part of COD-Structure-Server.
 *
 * BB-Structure-Server is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * BB-Structure-Server is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with COD-Structure-Server.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package org.openmolecules.bb;

public interface BBServerConstants {
	// The SERVER_URL is only used by the client
//	public static final String SERVER_URL = "http://localhost:8087";
	public static final String SERVER_URL = "http://bb.openmolecules.org";

	int RESULT_STRUCTURE_COLUMNS = 3;
	int RESULT_COLUMN_IDCODE = 0;
	int RESULT_COLUMN_COORDS2D = 1;
	int RESULT_COLUMN_FFP512 = 2;
	String RESULT_COLUMN_NAME_BB_NO = "BB Number";

	String REQUEST_HELP = "help";
	String REQUEST_TEMPLATE = "template";
	String REQUEST_PROVIDER_LIST = "providers";
	String REQUEST_MOLFILE2 = "molfile2";
	String REQUEST_MOLFILE3 = "molfile3";
	String QUERY_STRUCTURE_SEARCH_SPEC = "ssspec";
	String QUERY_AMOUNT = "amount";
	String QUERY_PRICE_LIMIT = "price";
	String QUERY_PROVIDERS = "providers";
	String QUERY_PROVIDERS_VALUE_ANY = "any";
	String QUERY_ONE_PRODUCT_ONLY = "oneProductOnly"; // if one compound is available from multiple providers
	String QUERY_MAX_ROWS = "maxrows";
	String QUERY_MOLWEIGHT = "molweight";
	String QUERY_RESULT_FORMAT = "format";
	String QUERY_RESULT_FORMAT_VALUE_SHORT = "short";
	String QUERY_PRUNING_MODE = "mode";
	String QUERY_SINGLE_MATCH_ONLY = "singlematch";

	int PRUNING_MODE_RANDOM = 0;
	int PRUNING_MODE_CHEAPEST = 1;
	int PRUNING_MODE_DIVERSE = 2;
	String[] CODE_PRUNING_MODE = {"random", "cheapest", "diverse"};

	// Molport specific query parameter names
	String QUERY_VERIFIED_AMOUNT = "verifiedAmount";

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// the following are the names of individual put/get parameters if the client doesn't use a query object //
	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	String PARAMETER_BB_ID = "id";
	String PARAMETER_SMILES = "smiles";
	String PARAMETER_SEARCH_TYPE = "searchType";
	String SEARCH_TYPE_SSS = "substructure";
	String SEARCH_TYPE_SIM = "similarity";
	String PARAMETER_THRESHOLD = "threshold";
}
