package com.actelion.research.table.model;

import java.util.TreeMap;

public interface IdentifierHandler {
	public TreeMap<String,String> addDefaultColumnProperties(String columnName, TreeMap<String,String> columnProperties);
	public String getSubstanceIdentifierName();
	public String getBatchIdentifierName();
	public boolean isIdentifierName(String s);
	public boolean isValidSubstanceIdentifier(String s);
	public boolean isValidBatchIdentifier(String s);
	public String normalizeIdentifierName(String identifierName);
	public void setIdentifierAliases(CompoundTableModel tableModel);
	}
