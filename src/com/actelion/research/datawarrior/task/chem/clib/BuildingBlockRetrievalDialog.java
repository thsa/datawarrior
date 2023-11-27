package com.actelion.research.datawarrior.task.chem.clib;

import com.actelion.research.chem.StructureSearchSpecification;
import com.actelion.research.datawarrior.task.db.BBCommunicator;
import com.actelion.research.datawarrior.task.db.BBProviderDialog;
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

import static org.openmolecules.bb.BBServerConstants.*;

public class BuildingBlockRetrievalDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 0x20080507;

	private static final String[] sPruningModes = {"random", "cheapest", "diverse"};

	private static final String ITEM_ANY_PROVIDER = "<Any Provider>";
	private static final String ITEM_CUSTOM_LIST = "Custom List...";
	private static final String[] DEFAULT_ITEMS = { ITEM_ANY_PROVIDER, ITEM_CUSTOM_LIST };

	private static float sAmount = 0.1f;
	private static float sPrice = 80f;
	private static int sMaxCount = 50;
	private static boolean sSingleMatchOnly = true;
	private static String sPruningMode = sPruningModes[0];
	private static String sProviders;

	private volatile JProgressDialog mProgressDialog;

	private Frame		mParentFrame;
	private JTextField	mTextFieldAmount,mTextFieldPrice,mTextFieldMax;
	private JComboBox	mComboBoxPruningMode,mComboBoxProviders;
	private JCheckBox   mCheckBoxSingleMatchOnly;
	private JLabel      mLabelCustomProviders;
	private String		mIDCode;
	private String[]    mProviderList;
	private CompoundCollectionModel<String[]> mModel;

	public BuildingBlockRetrievalDialog(Frame parent, String idcode, CompoundCollectionModel<String[]> model) {
		super(parent, "Suggest Commercially Available Building Blocks", true);
		mParentFrame = parent;
		mIDCode = idcode;
		mModel = model;

		int gap = HiDPIHelper.scale(8);
		JPanel p = new JPanel();
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, HiDPIHelper.scale(160), gap},
							{gap, TableLayout.PREFERRED, gap*2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2,
									TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap } };
		p.setLayout(new TableLayout(size));

		mProviderList = new BBCommunicator(null, "datawarrior").getProviderList();
		mComboBoxProviders = new JComboBox<>(DEFAULT_ITEMS);
		for (String provider:mProviderList)
			mComboBoxProviders.addItem(provider);
		mComboBoxProviders.addActionListener(e -> {
			if (e.getSource() == mComboBoxProviders) {
				boolean isCustom = mComboBoxProviders.getSelectedItem().equals(ITEM_CUSTOM_LIST);
				if (isCustom) {
					String selectedProviders = new BBProviderDialog(this, mProviderList).getSelectedProviders();
					if (selectedProviders != null)
						mLabelCustomProviders.setText(selectedProviders);
					}
				mLabelCustomProviders.setEnabled(isCustom);
				}
			});
		mLabelCustomProviders = new JLabel();
		if (sProviders != null)
			mLabelCustomProviders.setText(sProviders);
		p.add(new JLabel("Provider(s):", JLabel.RIGHT), "1,1");
		p.add(mComboBoxProviders, "3,1");
		p.add(mLabelCustomProviders, "5,1");

		mTextFieldAmount = new JTextField(DoubleFormat.toString(sAmount), 6);
		p.add(new JLabel("Required minimum amount:"), "1,3");
		p.add(mTextFieldAmount, "3,3");
		p.add(new JLabel("g"), "5,3");

		mTextFieldPrice = new JTextField(DoubleFormat.toString(sPrice), 6);
		p.add(new JLabel("Maximum price for amount:"), "1,5");
		p.add(mTextFieldPrice, "3,5");
		p.add(new JLabel("USD"), "5,5");

		mTextFieldMax = new JTextField(DoubleFormat.toString(sMaxCount), 6);
		p.add(new JLabel("Maximum building block count:"), "1,7");
		p.add(mTextFieldMax, "3,7");

		mComboBoxPruningMode = new JComboBox(sPruningModes);
		mComboBoxPruningMode.setSelectedItem(sPruningMode);
		p.add(new JLabel("If maximum count is exceeded get"), "1,9");
		p.add(mComboBoxPruningMode, "3,9");
		p.add(new JLabel("subset"), "5,9");

		mCheckBoxSingleMatchOnly = new JCheckBox("Avoid multiple substructure matches", sSingleMatchOnly);
		p.add(mCheckBoxSingleMatchOnly, "3,11,5,11");

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

			sSingleMatchOnly = mCheckBoxSingleMatchOnly.isSelected();
			sAmount = amount;
			sPrice = price;
			sMaxCount = maxbb;
			sPruningMode = (String)mComboBoxPruningMode.getSelectedItem();
			sProviders = ITEM_ANY_PROVIDER.equals(mComboBoxProviders.getSelectedItem()) ? QUERY_PROVIDERS_VALUE_ANY
					   : ITEM_CUSTOM_LIST.equals(mComboBoxProviders.getSelectedItem()) ? mLabelCustomProviders.getText()
					   : (String)mComboBoxProviders.getSelectedItem();

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

		if (sProviders != null)
			query.put(QUERY_PROVIDERS, sProviders);
		if (sAmount != 0)
			query.put(QUERY_AMOUNT, Float.toString(sAmount));
		if (sPrice != 0)
			query.put(QUERY_PRICE_LIMIT, Float.toString(sPrice));
		if (sMaxCount != 0)
			query.put(QUERY_MAX_ROWS, Integer.toString(sMaxCount));
		if (sPruningMode != null)
			query.put(QUERY_PRUNING_MODE, sPruningMode);

		query.put(QUERY_ONE_PRODUCT_ONLY, "true");
		query.put(QUERY_RESULT_FORMAT, QUERY_RESULT_FORMAT_VALUE_SHORT);

		int type = StructureSearchSpecification.TYPE_SUBSTRUCTURE + (sSingleMatchOnly ? StructureSearchSpecification.MODE_SINGLE_MATCH_ONLY : 0 );
		byte[][] idcode = new byte[1][];
		idcode[0] = mIDCode.getBytes();
		StructureSearchSpecification ssSpec = new StructureSearchSpecification(type, idcode, null, null, 0);
		query.put("ssspec", ssSpec);

		byte[][][] resultTable = new BBCommunicator(mProgressDialog, "datawarrior").search(query);
		return resultTable;
		}
	}
