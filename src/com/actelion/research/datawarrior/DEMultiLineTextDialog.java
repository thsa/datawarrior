package com.actelion.research.datawarrior;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class DEMultiLineTextDialog extends JDialog implements ActionListener {
	private JTextArea mTextArea;
	private String mOldValue,mNewValue;

	public DEMultiLineTextDialog(Frame owner, String title, String oldValue) {
		super(owner, title, true);

		mOldValue = oldValue;
		mTextArea = new JTextArea(oldValue);

		// Change font to allow displaying rare unicode characters
		mTextArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, mTextArea.getFont().getSize()));
		mTextArea.transferFocus();
		JScrollPane scrollPane = new JScrollPane(mTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(HiDPIHelper.scale(320), HiDPIHelper.scale(100)));

//		mTextArea.getInputMap(JTextArea.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"dialogOK");
//		mTextArea.getActionMap().put("dialogOK", new AbstractAction() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				done(true);
//				}
//			});
//		mTextArea.getInputMap(JTextArea.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK),"fakeEnter");
//		mTextArea.getActionMap().put("fakeEnter", new AbstractAction() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				int pos = mTextArea.getCaretPosition();
//				mTextArea.insert("\n", pos);
//				mTextArea.setCaretPosition(pos + 1);
//				}
//			});
		mTextArea.getInputMap(JTextArea.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"dialogCancel");
		mTextArea.getActionMap().put("dialogCancel", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				done(false);
			}
		});

		JLabel message = new JLabel("Press ESC (Cancel) or Ctrl-ENTER (OK) to close dialog!", JLabel.RIGHT);
		message.setFont(message.getFont().deriveFont(0,HiDPIHelper.scale(11)));

		JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(this);
		JButton bok = new JButton("OK");
		bok.addActionListener(this);
		getRootPane().setDefaultButton(bok);

		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap },
				{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		getContentPane().setLayout(new TableLayout(size));
		getContentPane().add(scrollPane, "1,1,5,1");
		getContentPane().add(message, "1,3,5,3");
		getContentPane().add(bcancel, "3,5");
		getContentPane().add(bok, "5,5");

		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	public String getNewValue() {
		return mNewValue;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		done("OK".equals(e.getActionCommand()));
	}

	private void done(boolean isOK) {
		if (isOK && !mTextArea.getText().equals(mOldValue))
			mNewValue = mTextArea.getText();

		setVisible(false);
	}
}
