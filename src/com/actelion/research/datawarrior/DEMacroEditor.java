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

package com.actelion.research.datawarrior;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.task.*;
import com.actelion.research.datawarrior.task.DEMacro.Task;
import com.actelion.research.datawarrior.task.view.ListTransferHandler;
import com.actelion.research.gui.LookAndFeelHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.gui.hidpi.HiDPIIconButton;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableListEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.ViewSelectionHelper;
import com.actelion.research.util.ColorHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeSet;

public class DEMacroEditor extends JSplitPane implements ActionListener,CompoundTableConstants,CompoundTableView,
														 DEMacroListener,ItemListener,ListDataListener {
    private static final long serialVersionUID = 0x20130114;

    private static final String COMMAND_DELETE_SELECTED = "delsel";
    private static final String COMMAND_DELETE = "delete";
	private static final String COMMAND_DUPLICATE = "duplicate";
    private static final String COMMAND_EDIT = "edit";
	private static final String COMMAND_RUN = "run";

 //   private static ImageIcon sPopupIcon;

    private DEFrame				mParentFrame;
    private JTree				mTree;
    private JComboBox			mComboBoxMacro;
    private JList				mList;
    private ArrayList<DEMacro>	mMacroList;
    private boolean				mItemListenerDisabled,mMacroListChangedDisabled;
    private StandardTaskFactory	mTaskFactory;
    private DEMacro				mCurrentMacro;
	private JPopupMenu			mMacroPopup;
	private JCheckBoxMenuItem	mItemAutoStart;
	private ViewSelectionHelper	mViewSelectionHelper;

	@SuppressWarnings("unchecked")
	public DEMacroEditor(DEFrame parentFrame) {
		mParentFrame = parentFrame;
		mTaskFactory = parentFrame.getApplication().getTaskFactory();
		TreeSet<TaskSpecification> taskSet = mTaskFactory.getTaskDictionary(parentFrame);

		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Available Tasks");
		int category = -1;
		DefaultMutableTreeNode categoryNode = null;
		for (TaskSpecification task:taskSet) {
			if (category != task.getCategory()) {
				category = task.getCategory();
				categoryNode = new DefaultMutableTreeNode(task.getCategoryName());
				rootNode.add(categoryNode);
				}
			DefaultMutableTreeNode taskNode = new DefaultMutableTreeNode(task.getTaskName());
		    categoryNode.add(taskNode);
			}
		mTree = new JTree(rootNode);
		mTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		mTree.setDragEnabled(true);

		JScrollPane treePane = new JScrollPane(mTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
			@Override
			public void updateUI() {
				super.updateUI();
				setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, LookAndFeelHelper.isDarkLookAndFeel() ? Color.darkGray : Color.LIGHT_GRAY));
				}
			};
		setLeftComponent(treePane);

		mComboBoxMacro = new JComboBox();
		mMacroList = (ArrayList<DEMacro>)parentFrame.getTableModel().getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
		if (mMacroList == null) {
			mMacroList = new ArrayList<DEMacro>();
			parentFrame.getTableModel().setExtensionData(CompoundTableConstants.cExtensionNameMacroList, mMacroList);
			}

		for (DEMacro m:mMacroList)
			mComboBoxMacro.addItem(m.getName());
		if (mComboBoxMacro.getItemCount() != 0)
			mComboBoxMacro.setSelectedIndex(0);
		mComboBoxMacro.addItemListener(this);
		JButton popupButton = createPopupButton();

		int scaled4 = HiDPIHelper.scale(4);
		int scaled8 = HiDPIHelper.scale(8);
		double[][] size = { {scaled8, TableLayout.PREFERRED, scaled4, TableLayout.PREFERRED, scaled8, TableLayout.PREFERRED, TableLayout.FILL},
							{scaled4, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL, scaled4} };
		JPanel bp = new JPanel() {
			@Override public void paintComponent(Graphics g) {
				super.paintComponent(g);
				Dimension size = getSize();
				if (size.width > 0 && size.height > 0 & (LookAndFeelHelper.isNewSubstance() || LookAndFeelHelper.isRadiance())) {
					g.setColor(ColorHelper.darker(getBackground(), 0.9f));
					g.fillRect(0, 0, size.width, size.height);
					}
				}
			};
		bp.setLayout(new TableLayout(size));
		bp.add(new JLabel("Current Macro:", JLabel.RIGHT), "1,1,1,3");
		bp.add(mComboBoxMacro, "3,1,3,3");
		bp.add(popupButton, "5,2");

		mList = new JList(new DefaultListModel()) {
			private static final long serialVersionUID = 20131206L;

			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				if (getModel().getSize() == 0) {
					Dimension size = getSize();
					String msg = (mMacroList.size() == 0) ?
							"<create new macro to edit>" : "<drop tasks here>";
					FontMetrics metrics = g.getFontMetrics();
					g.setColor(Color.GRAY);
					g.drawString(msg, (size.width-metrics.stringWidth(msg)) / 2, (size.height+g.getFont().getSize())/2);
					}
				}
			};
		mList.setDragEnabled(true);
		mList.setTransferHandler(new ListTransferHandler() {
			private static final long serialVersionUID = 20131209L;

			// make sure we only except Strings that represent existing task names
			@Override
			public boolean canImport(TransferSupport support) {
				if (!super.canImport(support))
					return false;

				if (mComboBoxMacro.getSelectedIndex() == -1)
					return false;

				try {
					String[] taskNames = getStringData(support).split("\\n");
					for (String taskName:taskNames)
						if (mTaskFactory.createTaskFromName(mParentFrame, taskName) == null)
							return false;

					return true;
					}
				catch (Exception e) {
					return false;
					}
				}

			// we handle the task configurations in a parallel list. Keep task names and configurations in sync.
			@Override
			public void updateListIndexes(int[] newToOldListIndexMap) {
				listOrderChanged(newToOldListIndexMap);
				mParentFrame.setDirty(true);
				}
			} );
		mList.setDropMode(DropMode.INSERT);
		mList.getModel().addListDataListener(this);
		mList.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showPopupMenu(e.getX(), e.getY(), mList.locationToIndex(e.getPoint()));
					}
				else {
					allowDropOnListForTenSeconds();
					}
				}
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showPopupMenu(e.getX(), e.getY(), mList.locationToIndex(e.getPoint()));
					}
				else {
					mList.getDropTarget().setActive(false);
					}
				}
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					editTask(mList.locationToIndex(e.getPoint()));
					}
				}
			});

		// If the drop target is constantly active, then drag&drop of the docking framework
		// is not working over the macro-JList, because drag events are consumed.
		// Therefore we enable the droptarget just for 5 seconds from starting the drag.
		mList.getDropTarget().setActive(false);
		mTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				allowDropOnListForTenSeconds();
				}
			});

		JScrollPane sp = new JScrollPane();
		sp.setBorder(BorderFactory.createEmptyBorder());
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		sp.setViewportView(mList);
		if (mComboBoxMacro.getSelectedIndex() != -1)
			resetListModel(mMacroList.get(mComboBoxMacro.getSelectedIndex()));

		JPanel macroPanel = new JPanel() {
			@Override
			public void updateUI() {
				setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, LookAndFeelHelper.isDarkLookAndFeel() ? Color.darkGray : Color.LIGHT_GRAY));
				}
			};
		macroPanel.setLayout(new BorderLayout());
		macroPanel.add(bp, BorderLayout.NORTH);
		macroPanel.add(sp, BorderLayout.CENTER);
		setRightComponent(macroPanel);

		final CompoundTableView _this = this;
		MouseAdapter viewSelectionMouseAdapter = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				mViewSelectionHelper.setSelectedView(_this);
			}
		};
		bp.addMouseListener(viewSelectionMouseAdapter);
		mTree.addMouseListener(viewSelectionMouseAdapter);
		mList.addMouseListener(viewSelectionMouseAdapter);
		mComboBoxMacro.addMouseListener(viewSelectionMouseAdapter);
		popupButton.addMouseListener(viewSelectionMouseAdapter);

		setDividerSize(8);
		setDividerLocation(0.4);
		setResizeWeight(0.4);
		setContinuousLayout(true);

		enableItems();
		}

	private void allowDropOnListForTenSeconds() {
		mList.getDropTarget().setActive(true);
		new Thread(() -> {
			try { Thread.sleep(10000); } catch (InterruptedException ie) {}
			SwingUtilities.invokeLater(() -> mList.getDropTarget().setActive(false));
			} ).start();
		}

	@Override
	public boolean copyViewContent() {
		return false;
		}

	private JButton createPopupButton() {
		JButton button = new HiDPIIconButton("popup14.png", null, "showPopup");
		button.addActionListener(this);
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
			if (mMacroPopup == null)
				showMacroPopup(e);
				}
			});
		return button;
		}

	private void showMacroPopup(MouseEvent e) {
		JButton button = (JButton)e.getSource();
		mMacroPopup = new JPopupMenu() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void setVisible(boolean b) {
				super.setVisible(b);
				mMacroPopup = null;
				}
			};
		mMacroPopup.add(createPopupItem("New Macro..."));
		if (!mMacroList.isEmpty()) {
			mMacroPopup.add(createPopupItem("Duplicate Macro"));
			mMacroPopup.addSeparator();
			mMacroPopup.add(createPopupItem("Rename Macro..."));
			mMacroPopup.addSeparator();
			mMacroPopup.add(createPopupItem("Delete Macro..."));
			mMacroPopup.addSeparator();
			mItemAutoStart = new JCheckBoxMenuItem("Auto-Starting", mCurrentMacro.isAutoStarting());
			mItemAutoStart.addActionListener(this);
			mMacroPopup.add(mItemAutoStart);
			}
		mMacroPopup.show(button.getParent(),
							 button.getBounds().x,
							 button.getBounds().y + button.getBounds().height);
		}

	private JMenuItem createPopupItem(String title) {
		JMenuItem item = new JMenuItem(title);
		item.setActionCommand(title);
		item.addActionListener(this);
		return item;
		}

	@Override
	public void cleanup() {
		mList.getModel().removeListDataListener(this);
		}

	@Override
	public void setViewSelectionHelper(ViewSelectionHelper l) {
		mViewSelectionHelper = l;
		}

	@Override
	public CompoundTableModel getTableModel() {
		return mParentFrame.getTableModel();
		}

	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cChangeExtensionData
		 && e.getSpecifier() == DECompoundTableExtensionHandler.ID_MACRO
		 && !mMacroListChangedDisabled) {
			mItemListenerDisabled = true;
			String oldSelectedItem = (String)mComboBoxMacro.getSelectedItem();
			mComboBoxMacro.removeAllItems();
			for (DEMacro m:mMacroList)
				mComboBoxMacro.addItem(m.getName());
			if (oldSelectedItem != null)
				mComboBoxMacro.setSelectedItem(oldSelectedItem);
			mItemListenerDisabled = false;
			int selectedIndex = mComboBoxMacro.getSelectedIndex();
			resetListModel(selectedIndex == -1 ? null : mMacroList.get(selectedIndex));
			enableItems();
			}
		}

	@Override
	public void listChanged(CompoundTableListEvent e) {
		}

	public DEMacro getCurrentMacro() {
		return mCurrentMacro;
		}

	private int getListIndex(Object item) {
		for (int i=0; i<mList.getModel().getSize(); i++)
			if (mList.getModel().getElementAt(i) == item)	// this is the very same and not just equal
				return i;

		return -1;
		}

	private void resetListModel(DEMacro macro) {
		if (mCurrentMacro != macro) {
			if (mCurrentMacro != null)
				mCurrentMacro.removeMacroListener(this);
	
			mCurrentMacro = macro;
	
			if (mCurrentMacro != null)
				mCurrentMacro.addMacroListener(this);
			}

		mList.setDragEnabled(macro != null);
		DefaultListModel model = (DefaultListModel)mList.getModel();
		model.clear();
		if (macro != null) {
			for (int i=0; i<macro.getTaskCount(); i++) {
				AbstractTask task = mTaskFactory.createTaskFromCode(mParentFrame, macro.getTaskCode(i));
				if (task != null)
					model.addElement(task.getTaskName());
				else
					model.addElement("<Unknown Task>");
				}
			}
		}

	private void listOrderChanged(int[] oldIndex) {
		ArrayList<Task> taskList = mCurrentMacro.changeTaskOrder(oldIndex);
		for (int i=0; i<taskList.size(); i++) {
			DEMacro.Task task = taskList.get(i);
			if (task.getCode() == null) {
				String taskName = (String)mList.getModel().getElementAt(i);
				String code = mTaskFactory.getTaskCodeFromName(taskName);
				task.setCode(code);
				createTaskConfigurationLater(task, i);
				}
			}
		}

	private void createTaskConfigurationLater(final DEMacro.Task macroTask, final int taskIndex) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				AbstractTask task = mTaskFactory.createTaskFromCode(mParentFrame, macroTask.getCode());
				if (!task.isTaskWithoutConfiguration()) {
					Properties configuation = task.showDialog(null, false);
					if (task.isStatusOK())
						macroTask.setConfiguration(configuation);
					else
						deleteTask(taskIndex);
					}
				}
			} );
		}

	private void showPopupMenu(int x, int y, int taskIndex) {
		if (taskIndex != -1) {
			JPopupMenu popup = new JPopupMenu();

			String taskName = (String)mList.getModel().getElementAt(taskIndex);
			if (! mTaskFactory.createTaskFromName(mParentFrame, taskName).isTaskWithoutConfiguration()) {
				JMenuItem item1 = new JMenuItem("Edit Task...");
		        item1.addActionListener(this);
		        item1.setActionCommand(COMMAND_EDIT+taskIndex);
		        popup.add(item1);
		        popup.addSeparator();
				}

			JMenuItem item2 = new JMenuItem("Duplicate Task");
			item2.addActionListener(this);
			item2.setActionCommand(COMMAND_DUPLICATE+taskIndex);
			popup.add(item2);

			popup.addSeparator();

			JMenuItem item3 = new JMenuItem("Delete Task");
	        item3.addActionListener(this);
	        item3.setActionCommand(COMMAND_DELETE+taskIndex);
	        popup.add(item3);

	        if (mList.getSelectedValuesList().size() > 1) {
				JMenuItem item4 = new JMenuItem("Delete Selected Tasks");
		        item4.addActionListener(this);
		        item4.setActionCommand(COMMAND_DELETE_SELECTED);
		        popup.add(item4);
	        	}

			popup.addSeparator();

			JMenu runMenu = new JMenu("Run Task In");
			popup.add(runMenu);
			ArrayList<DEFrame> frameList = mParentFrame.getApplication().getFrameList();
			for (int i=0; i<frameList.size(); i++) {
				DEFrame frame = frameList.get(i);
				JMenuItem runItem = new JMenuItem((frame == mParentFrame) ? "This Window" : frame.getTitle());
				runItem.addActionListener(this);
				runItem.setActionCommand(COMMAND_RUN+i+'_'+taskIndex);
				runMenu.add(runItem);
				}

			mParentFrame.getApplication().getFrameList();

			popup.show(mList, x, y);
			}
		}

	private void enableItems() {
		boolean enabled = (mMacroList.size() != 0);
		mComboBoxMacro.setEnabled(enabled);
		mList.repaint();
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("New Macro...")) {
			DEMacro macro = DEMacroEditor.addNewMacro(mParentFrame, null);
			if (macro != null)
				mComboBoxMacro.setSelectedItem(macro.getName());
			return;
			}
		if (e.getActionCommand().equals("Duplicate Macro")) {
			DEMacro macro = addNewMacro(mParentFrame, mCurrentMacro);
			if (macro != null)
				mComboBoxMacro.setSelectedItem(macro.getName());
			return;
			}
		if (e.getActionCommand().equals("Rename Macro...")) {
			String newName = JOptionPane.showInputDialog(mParentFrame, "New macro name:", mCurrentMacro.getName());
			
			if (newName != null && newName.length() != 0)
				mCurrentMacro.setName(newName, mMacroList);

			mParentFrame.getTableModel().setExtensionData(CompoundTableConstants.cExtensionNameMacroList, mMacroList);
			mParentFrame.setDirty(true);
			return;
			}
		if (e.getActionCommand().equals("Delete Macro...")) {
			if (mComboBoxMacro.getSelectedIndex() == -1)
				return;
			int doDelete = JOptionPane.showConfirmDialog(this,
					"Do you really want to delete the macro '"+mComboBoxMacro.getSelectedItem()+"'?",
					"Delete Macro?",
					JOptionPane.OK_CANCEL_OPTION);
			if (doDelete == JOptionPane.OK_OPTION)
				deleteCurrentMacro();
			return;
			}
		if (e.getSource() == mItemAutoStart) {
			if (mCurrentMacro != null)
				mCurrentMacro.setAutoStarting(mItemAutoStart.isSelected());
			return;
			}
		if (e.getActionCommand().equals(COMMAND_DELETE_SELECTED)) {
			deleteSelectedTasks();
			return;
			}
		if (e.getActionCommand().startsWith(COMMAND_EDIT)) {
			editTask(Integer.parseInt(e.getActionCommand().substring(COMMAND_EDIT.length())));
			return;
			}
		if (e.getActionCommand().startsWith(COMMAND_DUPLICATE)) {
			duplicateTask(Integer.parseInt(e.getActionCommand().substring(COMMAND_DUPLICATE.length())));
			return;
			}
		if (e.getActionCommand().startsWith(COMMAND_DELETE)) {
			deleteTask(Integer.parseInt(e.getActionCommand().substring(COMMAND_DELETE.length())));
			return;
			}
		if (e.getActionCommand().startsWith(COMMAND_RUN)) {
			runTask(e.getActionCommand().substring(COMMAND_RUN.length()));
			return;
			}
		}

	/**
	 * Inquires for a macro name and adds a new macro with that name to the end of the
	 * table model's macro list, causing the proper CompoundTableEvent to be fired.
	 * @param frame
	 * @param macroToBeCloned null or macro to be duplicated
	 * @return new macro or null if user cancelled new macro naming dialog
	 */
	public static DEMacro addNewMacro(DEFrame frame, DEMacro macroToBeCloned) {
		String suggestedName = (macroToBeCloned == null) ? "Untitled Macro" : "Copy of " + macroToBeCloned.getName();
		String message = (macroToBeCloned == null) ? "Name of new empty macro" : "Name of duplicated macro";
		String name = JOptionPane.showInputDialog(frame, message, suggestedName);
		if (name == null || name.length() == 0)
			return null;

		return addNewMacro(frame, name, macroToBeCloned);
		}

	/**
	 * Adds a new macro with the given name to the end of the
	 * table model's macro list, causing the proper CompoundTableEvent to be fired.
	 * @param frame
	 * @param macroToBeCloned null or macro to be duplicated
	 * @return new macro or null if user cancelled new macro naming dialog
	 */
	public static DEMacro addNewMacro(DEFrame frame, String name, DEMacro macroToBeCloned) {
		@SuppressWarnings("unchecked")
		ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)frame.getTableModel().getExtensionData(CompoundTableConstants.cExtensionNameMacroList);

		DEMacro macro = new DEMacro(name, macroList, macroToBeCloned);

		if (macroList == null)
			macroList = new ArrayList<DEMacro>();

		int index = 0;
		while (index<macroList.size() && macroList.get(index).getName().compareTo(macro.getName()) < 0)
			index++;
		macroList.add(index, macro);

		frame.getTableModel().setExtensionData(CompoundTableConstants.cExtensionNameMacroList, macroList);

		frame.setDirty(true);
		return macro;
		}

	public void selectMacro(DEMacro macro) {
		mComboBoxMacro.setSelectedItem(macro.getName());
		}

	private void deleteCurrentMacro() {
		if (mComboBoxMacro.getItemCount() != 0) {
			int index = mComboBoxMacro.getSelectedIndex();
			mMacroList.remove(index);

			// to trigger change events and update the macro lists in the menu
			mMacroListChangedDisabled = true;
			mParentFrame.getTableModel().setExtensionData(CompoundTableConstants.cExtensionNameMacroList, mMacroList);
			mMacroListChangedDisabled = false;

			mItemListenerDisabled = true;
			mComboBoxMacro.removeItemAt(index);
			if (mComboBoxMacro.getItemCount() != 0) {
				if (index >= mComboBoxMacro.getItemCount())
					index = mComboBoxMacro.getItemCount()-1;
				mComboBoxMacro.setSelectedIndex(index);
				resetListModel(mMacroList.get(index));
				}
			else {
				resetListModel(null);
				}
			mItemListenerDisabled = false;

			enableItems();

			mParentFrame.setDirty(true);
			}
		}

	private void deleteSelectedTasks() {
		int[] index = mList.getSelectedIndices();
		for (int i=index.length-1; i>=0; i--)
			mCurrentMacro.removeTask(index[i]);
		}

	private void deleteTask(int taskIndex) {
		mCurrentMacro.removeTask(taskIndex);
		}

	private void duplicateTask(int taskIndex) {
		if (taskIndex != -1) {
			DEMacro macro = mMacroList.get(mComboBoxMacro.getSelectedIndex());
			macro.duplicateTask(taskIndex);
			mParentFrame.setDirty(true);
			}
		}

	private void editTask(int taskIndex) {
		if (taskIndex != -1) {
			DEMacro macro = mMacroList.get(mComboBoxMacro.getSelectedIndex());
			DEMacro.Task macroTask = macro.getTask(taskIndex);
			AbstractTask task = mTaskFactory.createTaskFromCode(mParentFrame, macroTask.getCode());
			if (task != null && !task.isTaskWithoutConfiguration()) {
				Properties newConfiguration = task.showDialog(macroTask.getConfiguration(), false);
				if (newConfiguration != macroTask.getConfiguration()) {
					macroTask.setConfiguration(newConfiguration);
					mParentFrame.setDirty(true);
					}
				}
			}
		}

	private void runTask(String encoding) {
		int index = encoding.indexOf('_');
		int frameIndex = Integer.parseInt(encoding.substring(0, index));
		int taskIndex = Integer.parseInt(encoding.substring(index+1));
		DEMacro macro = mMacroList.get(mComboBoxMacro.getSelectedIndex());
		DEMacro.Task macroTask = macro.getTask(taskIndex);
		AbstractTask task = mTaskFactory.createTaskFromCode(mParentFrame, macroTask.getCode());
		DEMacro singleTaskMacro = new DEMacro("Single-Task Macro", new ArrayList<DEMacro>());
		singleTaskMacro.addTask(macroTask.getCode(), macroTask.getConfiguration());
		ArrayList<DEFrame> frameList = mParentFrame.getApplication().getFrameList();
		DEMacroRecorder.getInstance().runMacro(singleTaskMacro, frameList.get(frameIndex));
		}

	@Override
	public void intervalAdded(ListDataEvent e) {
		mParentFrame.setDirty(true);
		}

	@Override
	public void intervalRemoved(ListDataEvent e) {
		mParentFrame.setDirty(true);
		}

	@Override
	public void contentsChanged(ListDataEvent e) {
		mParentFrame.setDirty(true);
		}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (!mItemListenerDisabled && e.getStateChange() == ItemEvent.SELECTED) {
			int index = mComboBoxMacro.getSelectedIndex();
			if (index != -1)
				resetListModel(mMacroList.get(index));
			}
		}

	@Override
	public void macroNameChanged(DEMacro macro) {
		boolean isSelected = (macro == mCurrentMacro);
		Object selectedItem = mComboBoxMacro.getSelectedItem();
		mComboBoxMacro.removeItemListener(this);
		mComboBoxMacro.removeAllItems();
		for (DEMacro m:mMacroList)
			mComboBoxMacro.addItem(m.getName());
		if (mComboBoxMacro.getItemCount() != 0)
			mComboBoxMacro.setSelectedIndex(0);
		if (isSelected)
			mComboBoxMacro.setSelectedItem(macro.getName());
		else
			mComboBoxMacro.setSelectedItem(selectedItem);
		mComboBoxMacro.addItemListener(this);
		}

	@Override
	public void macroContentChanged(DEMacro macro) {
		if (macro == mCurrentMacro) {
			resetListModel(macro);
			mParentFrame.setDirty(true);
			}
		}
	}
