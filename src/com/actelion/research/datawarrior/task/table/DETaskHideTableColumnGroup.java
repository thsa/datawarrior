package com.actelion.research.datawarrior.task.table;

import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.table.model.CompoundTableModel;

import java.awt.*;
import java.util.Properties;

public class DETaskHideTableColumnGroup extends DETaskAbstractTableColumnGroup {
	public static final String TASK_NAME = "Hide Table Column Group";

	public DETaskHideTableColumnGroup(Frame owner, DETableView tableView, CompoundTableModel tableModel) {
		super(owner, tableView, tableModel);
		}

	/**
	 * Instantiates this task interactively with a pre-defined configuration.
	 * @param owner
	 * @param tableView
	 * @param groupName
	 */
	public DETaskHideTableColumnGroup(Frame owner, DETableView tableView, CompoundTableModel tableModel, String groupName) {
		super(owner, tableView, tableModel, groupName);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void prepareColumnGroupActions(Properties configuration) {}

	@Override
	public void doColumnGroupAction(int column, boolean isGroupMember, String groupName) {
		boolean isShown = getTableView().getTable().convertTotalColumnIndexToView(column) != -1;
		if (isShown && isGroupMember)
			getTableView().setColumnVisibility(column, false);
		}
	}
