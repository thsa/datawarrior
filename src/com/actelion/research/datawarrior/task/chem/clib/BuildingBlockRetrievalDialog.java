package com.actelion.research.datawarrior.task.chem.clib;

import com.actelion.research.chem.StructureSearchSpecification;
import com.actelion.research.datawarrior.task.db.EnamineCommunicator;
import com.actelion.research.gui.CompoundCollectionModel;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeMap;

public class BuildingBlockRetrievalDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 0x20080507;

	private static final String[] sPruningModes = {"random", "cheapest", "diverse"};

	private static float sAmount = 0.1f;
	private static float sPrice = 80f;
	private static int sMaxCount = 50;
	private static String sPruningMode = sPruningModes[0];

	private volatile JProgressDialog mProgressDialog;

	private Frame		mParentFrame;
	private JTextField	mTextFieldAmount,mTextFieldPrice,mTextFieldMax;
	private JComboBox	mComboBoxPruningMode;
	private String		mIDCode;
	private CompoundCollectionModel<String[]> mModel;

	public BuildingBlockRetrievalDialog(Frame parent, String idcode, CompoundCollectionModel<String[]> model) {
		super(parent, "Suggest Building Blocks from Enamine", true);
		mParentFrame = parent;
		mIDCode = idcode;
		mModel = model;

		int gap = HiDPIHelper.scale(8);
		JPanel p = new JPanel();
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap } };
		p.setLayout(new TableLayout(size));

		mTextFieldAmount = new JTextField(DoubleFormat.toString(sAmount), 6);
		p.add(new JLabel("Required minimum amount:"), "1,1");
		p.add(mTextFieldAmount, "3,1");
		p.add(new JLabel("g"), "5,1");

		mTextFieldPrice = new JTextField(DoubleFormat.toString(sPrice), 6);
		p.add(new JLabel("Maximum price for amount:"), "1,3");
		p.add(mTextFieldPrice, "3,3");
		p.add(new JLabel("USD"), "5,3");

		mTextFieldMax = new JTextField(DoubleFormat.toString(sMaxCount), 6);
		p.add(new JLabel("Maximum building block count:"), "1,5");
		p.add(mTextFieldMax, "3,5");

		mComboBoxPruningMode = new JComboBox(sPruningModes);
		mComboBoxPruningMode.setSelectedItem(sPruningMode);
		p.add(new JLabel("If maximum count is exceeded get"), "1,7");
		p.add(mComboBoxPruningMode, "3,7");
		p.add(new JLabel("subset"), "5,7");

		JPanel p2 = new JPanel();
		p2.setLayout(new BorderLayout());
		p2.setBorder(BorderFactory.createEmptyBorder(gap, gap, gap, gap));
		JPanel bp = new JPanel();
		bp.setLayout(new GridLayout(1, 2, gap, gap));
		JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(this);
		bp.add(bcancel);
		JButton bok = new JButton("OK");
		bok.setEnabled(true);
		bok.addActionListener(this);
		bp.add(bok);
		p2.add(bp, BorderLayout.EAST);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(p, BorderLayout.CENTER);
		getContentPane().add(p2, BorderLayout.SOUTH);
		getRootPane().setDefaultButton(bok);

		pack();
		setLocationRelativeTo(parent);
		setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		if (!e.getActionCommand().equals("Cancel")) {
			float amount = 0;
			if (mTextFieldAmount.getText().length() != 0) {
				try {
					amount = Float.parseFloat(mTextFieldAmount.getText());
				}
				catch (NumberFormatException nfe) {
					JOptionPane.showMessageDialog(mParentFrame, "The minimum amount is not numerical."+e);
					}
				}

			float price = 0;
			if (mTextFieldPrice.getText().length() != 0) {
				try {
					price = Float.parseFloat(mTextFieldPrice.getText());
					}
				catch (NumberFormatException nfe) {
					JOptionPane.showMessageDialog(mParentFrame, "The maximum price is not numerical."+e);
					}
				}

			int maxbb = 0;
			if (mTextFieldMax.getText().length() != 0) {
				try {
					maxbb = Integer.parseInt(mTextFieldMax.getText());
					}
				catch (NumberFormatException nfe) {
					JOptionPane.showMessageDialog(mParentFrame, "The maximum building block count is not an integer."+e);
					}
				}

			sAmount = amount;
			sPrice = price;
			sMaxCount = maxbb;
			sPruningMode = (String)mComboBoxPruningMode.getSelectedItem();

			mProgressDialog = new JProgressDialog(mParentFrame);

			new Thread(() -> {
				byte[][][] resultTable = retrieveBuildingBlocks();

				mProgressDialog.setVisible(false);
				mProgressDialog.dispose();

				if (resultTable != null) {
					SwingUtilities.invokeLater(() -> {
						if (resultTable != null) {
							for (int i=1; i<resultTable.length; i++) {	// skip header line
								byte[][] result = resultTable[i];
								String[] idcodeWithName = new String[2];
								idcodeWithName[0] = (result[0] == null) ? null
												  : (result[1] == null) ? new String(result[0])
												  : new String(result[0]).concat(" ").concat(new String(result[1]));
								if (result[2] != null)
									idcodeWithName[1] = new String(result[2]);
								mModel.addCompound(idcodeWithName);
								}
							}
						});
					}
				}).start();

			mProgressDialog.setVisible(true);
			}

		setVisible(false);
		dispose();
		return;
		}

	private byte[][][] retrieveBuildingBlocks() {
		TreeMap<String,Object> query = new TreeMap<String,Object>();

		if (sAmount != 0)
			query.put("amount", Float.toString(sAmount));
		if (sPrice != 0)
			query.put("price", Float.toString(sPrice));
		if (sMaxCount != 0)
			query.put("maxrows", Integer.toString(sMaxCount));
		if (sPruningMode != null)
			query.put("mode", sPruningMode);

		query.put("format", "short");

		byte[][] idcode = new byte[1][];
		idcode[0] = mIDCode.getBytes();
		StructureSearchSpecification ssSpec = new StructureSearchSpecification(StructureSearchSpecification.TYPE_SUBSTRUCTURE, idcode, null, null, 0);
		query.put("ssspec", ssSpec);

		byte[][][] resultTable = new EnamineCommunicator(mProgressDialog, "datawarrior").search(query);
		return resultTable;
		}
	}
