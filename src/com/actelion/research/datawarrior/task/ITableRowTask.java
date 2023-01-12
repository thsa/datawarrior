package com.actelion.research.datawarrior.task;

import com.actelion.research.table.model.CompoundRecord;

import java.util.Properties;

public interface ITableRowTask {
	String CONFIG_DELIMITER = "&";
	String CONFIG_KEY_COLUMN = "keyColumn";

	void setTableRow(CompoundRecord row);
	void initConfiguration(Properties configuration);
}
