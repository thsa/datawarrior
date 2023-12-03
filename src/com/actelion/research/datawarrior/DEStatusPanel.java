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

import com.actelion.research.calc.ProgressListener;
import com.actelion.research.gui.JProgressPanel;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundListSelectionModel;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

/**
 * Title:		DEStatusPanel.java
 * Description: Status panel with progress bar and message area for data explorer
 * Copyright:	Copyright (c) 2001
 * Company:     Actelion Ltd.
 * @author      Thomas Sander
 * @version     1.0
 */

public class DEStatusPanel extends JPanel
		implements ListSelectionListener,ProgressListener {
	private static final long serialVersionUID = 0x20060904;

	private static final int SHOW_ERROR = 1;
	private static final int START_PROGRESS = 2;
	private static final int UPDATE_PROGRESS = 3;
	private static final int STOP_PROGRESS = 4;

	private CompoundTableModel  mTableModel;
	private int mRecords,mVisible,mSelected;
	private JProgressBar mProgressBar = new JProgressBar();
	private JLabel mMemoryLabel = new JLabel();
	private JLabel mSelectedLabel = new JLabel();
	private JLabel mVisibleLabel = new JLabel();
	private JLabel mRecordsLabel = new JLabel();
	private JLabel mProgressLabel = new JLabel();
	private JProgressPanel mMacroProgressPanel = new JProgressPanel(true);
	private volatile float mUpdateFactor;
	private volatile boolean mUpdateActionPending;
	private volatile int mUpdateStatus;
	private volatile boolean mProgressVisible;

	public DEStatusPanel(CompoundTableModel tableModel) {
		mTableModel = tableModel;
		mTableModel.addProgressListener(this);

		Font labelFont = UIManager.getFont("Label.font");
		Font font = labelFont.deriveFont(Font.BOLD, labelFont.getSize()*12/13);

		JLabel LabelSelected = new JLabel("Selected:", SwingConstants.RIGHT);
		JLabel LabelVisible = new JLabel("Visible:", SwingConstants.RIGHT);
		JLabel LabelRecords = new JLabel("Total:", SwingConstants.RIGHT);
		LabelSelected.setFont(font);
		LabelVisible.setFont(font);
		LabelRecords.setFont(font);
		LabelSelected.setForeground(Color.GRAY);
		LabelVisible.setForeground(Color.GRAY);
		LabelRecords.setForeground(Color.GRAY);

		mMemoryLabel.setFont(font);
		mProgressLabel.setFont(font);
		mSelectedLabel.setFont(font);
		mVisibleLabel.setFont(font);
		mRecordsLabel.setFont(font);

		Dimension pbSize = new Dimension(HiDPIHelper.scale(80), HiDPIHelper.scale(10));
		mProgressBar.setVisible(false);
		mProgressBar.setPreferredSize(pbSize);
		mProgressBar.setMaximumSize(pbSize);
		mProgressBar.setMinimumSize(pbSize);
		mProgressBar.setSize(pbSize);

		startMemoryLabelTimer();

		int w = HiDPIHelper.scale(68);
		int scaled4 = HiDPIHelper.scale(4);
		double[][] size = { {2*scaled4, TableLayout.PREFERRED, scaled4, TableLayout.FILL, w, w, w, w, w, w, TableLayout.FILL},
							{scaled4, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL, scaled4/2} };

		setLayout(new TableLayout(size));
		add(mMemoryLabel, "1,1,3,3");
		add(LabelSelected, "4,1,4,3");
		add(mSelectedLabel, "5,1,5,3");
		add(LabelVisible, "6,1,6,3");
		add(mVisibleLabel, "7,1,7,3");
		add(LabelRecords, "8,1,8,3");
		add(mRecordsLabel, "9,1,9,3");
		add(mMacroProgressPanel, "10,1,10,3");
		}

	private void updateMemoryLabel() {
		long total = Runtime.getRuntime().totalMemory();
		long max = Runtime.getRuntime().maxMemory();
		long used = total - Runtime.getRuntime().freeMemory();
		mMemoryLabel.setText(toMegabyte(used).concat(" of ").concat(toMegabyte(total).concat(" MB")));
		}

	private void startMemoryLabelTimer() {
		new Timer(5000, l -> updateMemoryLabel() ).start();
		}

	private String toMegabyte(long l) {
		return Long.toString((l + 524288L) / 1048574L);
		}

	private void setProgressVisible(boolean b) {
		if (mProgressVisible != b) {
			if (b) {
				remove(mMemoryLabel);
				add(mProgressBar, "1,2");
				add(mProgressLabel, "3,1,3,3");
				}
			else {
				add(mMemoryLabel, "1,1,3,3");
				remove(mProgressBar);
				remove(mProgressLabel);
				}
			mProgressVisible = b;
			validate();
			}
		}

	public JProgressPanel getMacroProgressPanel() {
		return mMacroProgressPanel;
		}

	public void setNoOfRecords(int no) {
		if (no != mRecords) {
			mRecordsLabel.setText(""+no);
			mRecords = no;
			}
		}

	public void setNoOfVisible(int no) {
		if (no != mVisible) {
			mVisibleLabel.setText(""+no);
			mVisible = no;
			}
		}

	private void setNoOfSelected(int no) {
		if (no != mSelected) {
			mSelectedLabel.setText(""+no);
			mSelected = no;
			}
		}

	public void setRecording(boolean isRecording) {
		mMacroProgressPanel.showMessage(isRecording ? "Recording..." : "");
		}

	public void startProgress(String text, int min, int max) {
		doActionThreadSafe(START_PROGRESS, text, min, max);
		}

	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			setNoOfSelected(((CompoundListSelectionModel)e.getSource()).getSelectionCount());
			}
		}

	public void updateProgress(int value) {
		doActionThreadSafe(UPDATE_PROGRESS, null, value, 0);
		}

	public void updateProgress(int value, String message) {
		doActionThreadSafe(UPDATE_PROGRESS, message, value, 0);
		}

	public void stopProgress() {
		doActionThreadSafe(STOP_PROGRESS, null, 0, 0);
		}

	public void showErrorMessage(final String message) {
		doActionThreadSafe(SHOW_ERROR, message, 0, 0);
		}		

	private void doActionThreadSafe(final int action, final String text, final int v1, final int v2) {
		if (action == START_PROGRESS) {
			mUpdateFactor = (v2 - v1 <= 10000) ? 1.0f : 10000.0f / (v2 - v1);
			mUpdateStatus = v1;
			}
		if (action == UPDATE_PROGRESS)
			mUpdateStatus = (v1 >= 0) ? v1 : mUpdateStatus - v1;

		if (SwingUtilities.isEventDispatchThread()) {
			doAction(action, text, v1, v2);
			}
		else {
			if (action == UPDATE_PROGRESS) {
				if (!mUpdateActionPending) {
					mUpdateActionPending = true;
					try {
						SwingUtilities.invokeLater(() -> {
							doAction(action, text, v1, v2);
							mUpdateActionPending = false;
							} );
						}
					catch (Exception e) {}
					}
				}
			else {
				try {
					SwingUtilities.invokeLater(() -> doAction(action, text, v1, v2) );
					}
				catch (Exception e) {}
				}
			}
		}

	private void doAction(final int action, final String text, final int v1, final int v2) {
		switch (action) {
		case SHOW_ERROR:
			Component c = this;
			while (!(c instanceof Frame))
				c = c.getParent();
			JOptionPane.showMessageDialog(c, text);
			break;
		case START_PROGRESS:
			mProgressBar.setVisible(true);
			mProgressBar.setMinimum(Math.round(mUpdateFactor*v1));
			mProgressBar.setMaximum(Math.round(mUpdateFactor*v2));
			mProgressBar.setValue(Math.round(mUpdateFactor*v1));
			mProgressLabel.setText(text);
			setProgressVisible(true);
			break;
		case UPDATE_PROGRESS:
			mProgressBar.setValue(Math.round(mUpdateFactor*mUpdateStatus));
			if (text != null)
				mProgressLabel.setText(text);
			break;
		case STOP_PROGRESS:
			mProgressLabel.setText("");
			mProgressBar.setValue(mProgressBar.getMinimum());
			mProgressBar.setVisible(false);
			setProgressVisible(false);
			break;
			}
		}
	}
