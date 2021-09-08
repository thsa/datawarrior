package com.actelion.research.datawarrior.task.db;

public interface PatentReactionServerConstants {
//	REQUEST_RUN_QUERY returns a list of NL separated reaction IDs; To get a full reaction table use this:
	String REQUEST_RUN_QUERY_GET_TABLE = "table";

	String REQUEST_GET_QUERYABLE_FIELDS = "getFieldNames";

	String QUERY_REACTION_SEARCH_SPEC = "rsspec";
	String QUERY_STRUCTURE_SEARCH_SPEC = "ssspec";
	String QUERY_KEY_STRUCTURE_SEARCH_TARGET = "ssTarget";
	String QUERY_KEY_VALUE_PAIRS = "fieldQuery";

	String[] STRUCTURE_SEARCH_TARGET_CODE = { "reactant", "product" };
	int STRUCTURE_SEARCH_TARGET_REACTANT = 0;
	int STRUCTURE_SEARCH_TARGET_PRODUCT = 1;

	// mandatory columns for reactions
	String[] STRUCTURE_COLUMN_TITLE = { "rxncode", "mapping", "coords" };
	int RESULT_COLUMN_RXNCODE = 0;
	int RESULT_COLUMN_MAPPING = 1;
	int RESULT_COLUMN_COORDS = 2;

	// optional encoded descriptor columns
	String[] DESCRIPTOR_COLUMN_TITLE = { "reactionFP", "reactantFFP", "productFFP" };
	int RESULT_COLUMN_REACTION_FP = 3;
	int RESULT_COLUMN_REACTANT_FFP = 4;
	int RESULT_COLUMN_PRODUCT_FFP = 5;
	}
