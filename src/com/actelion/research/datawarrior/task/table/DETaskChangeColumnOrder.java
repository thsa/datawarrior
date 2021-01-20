package com.actelion.research.datawarrior.task.table;

import com.actelion.research.datawarrior.DEColumnOrder;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DETable;
import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.gui.ScrollPaneAutoScrollerWhenDragging;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.view.ListTransferHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.util.Properties;
import java.util.TooManyListenersException;

public class DETaskChangeColumnOrder extends ConfigurableTask {
	public static final String TASK_NAME = "Change Column Order";

	private static final String PROPERTY_LIST = "list";

	private CompoundTableModel  mTableModel;
	private DETable             mTable;
	private JList				mList;
	private JScrollPane			mScrollPane;
	private JTextArea			mTextArea;
	private DefaultListModel	mListModel;

	/**
	 * @param parent
	 */
	public DETaskChangeColumnOrder(Frame parent, DETableView tableView) {
		super(parent, false);
		mTable = tableView.getTable();
		mTableModel = tableView.getTableModel();
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public JComponent createDialogContent() {
		JPanel dialogPanel = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		dialogPanel.setLayout(new TableLayout(size));

		dialogPanel.add(new JLabel("Define table column order:"), "1,1");

		int scaled80 = HiDPIHelper.scale(80);

		if (isInteractive()) {
			mListModel = new DefaultListModel();
			mList = new JList(mListModel);
			mList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			mList.setDropMode(DropMode.INSERT);
			mList.setTransferHandler(new ListTransferHandler());
			mList.setDragEnabled(true);
			mList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, mList.getFont().getSize()));

			mScrollPane = new JScrollPane(mList);
			int height = Math.max(3*scaled80, Math.min(8*scaled80, (1 + mTableModel.getColumnCount()) * scaled80/4));
			mScrollPane.setPreferredSize(new Dimension(3*scaled80, height));

			// Hack to fix an issue with Swing's auto scrolling when dragging in a scroll pane
			ScrollPaneAutoScrollerWhenDragging scroller = new ScrollPaneAutoScrollerWhenDragging(mScrollPane, true);
			try {
				mList.getDropTarget().addDropTargetListener(new DropTargetAdapter() {
					@Override
					public void dragOver(DropTargetDragEvent dtde) {
						scroller.autoScroll();
						}

					@Override
					public void drop(DropTargetDropEvent dtde) {}
					});
				}
			catch (TooManyListenersException tmle) {}
			}
		else {
			mTextArea = new JTextArea();
			mScrollPane = new JScrollPane(mTextArea);
			mScrollPane.setPreferredSize(new Dimension(3*scaled80, 3*scaled80));
			}

		dialogPanel.add(mScrollPane, "1,3");

		return dialogPanel;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		if (isInteractive()) {
			StringBuilder sb = new StringBuilder((String)mListModel.elementAt(0));
			for (int i=1; i<mListModel.getSize(); i++)
				sb.append('\t').append(mTableModel.getColumnTitleNoAlias((String)mListModel.elementAt(i)));
			configuration.setProperty(PROPERTY_LIST, sb.toString());
			}
		else {
			configuration.put(PROPERTY_LIST, mTextArea.getText().replace('\n', '\t'));
			}
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		if (isInteractive()) {
			populateList();
			}
		else {
			String itemString = configuration.getProperty(PROPERTY_LIST, "");
			mTextArea.setText(itemString.replace('\t', '\n'));
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		populateList();
		}

	/**
	 * Populates the list/textArea with all displayable column titles in current order.
	 */
	private void populateList() {
		StringBuilder sb = null;
		if (isInteractive())
			mListModel.clear();
		else
			sb = new StringBuilder();

		DEColumnOrder columnOrder = mTable.getIntendedColumnOrder();
		for (int column:columnOrder) {
			String title = mTableModel.getColumnTitle(column);
			if (isInteractive()) {
				mListModel.addElement(title);
				}
			else {
				if (sb.length() != 0)
					sb.append('\n');
				sb.append(title);
				}
			}

		if (!isInteractive())
			mTextArea.setText(sb.toString());
		}

	@Override
	public boolean isConfigurable() {
		return mTableModel.getColumnCount() > 1;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		String columnList = configuration.getProperty(PROPERTY_LIST, "");
		if (columnList.length() != 0)
			mTable.setColumnOrderString(columnList);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}
}
