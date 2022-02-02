package com.actelion.research.datawarrior.task.db;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class BBProviderDialog extends JDialog {
	private String mProviderText;

	public BBProviderDialog(JDialog parent, String[] providerList) {
		JPanel p1 = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size1 = { {gap, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, gap, HiDPIHelper.scale(160), gap, TableLayout.PREFERRED, gap} };
		p1.setLayout(new TableLayout(size1));

		JList<String> list = new JList<>(providerList);
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(HiDPIHelper.scale(160),HiDPIHelper.scale(160)));
		p1.add(new JLabel("Choose from these providers"), "1,1");
		p1.add(scrollPane, "1,3");
		p1.add(new JLabel("(Use Shift or Ctrl to select multiple)"), "1,5");

		JDialog dialog = new JDialog(parent, "Select Providers", true);

		ActionListener al = e -> {
			if (e.getActionCommand().equals("OK")) {
				StringBuilder sb = new StringBuilder();
				for (String provider:list.getSelectedValuesList()) {
					if (sb.length() != 0)
						sb.append(", ");
					sb.append(provider);
				}
				mProviderText = sb.toString();
			}

			dialog.setVisible(false);
			dialog.dispose();
		};

		double[][] size2 = { {TableLayout.FILL, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, gap} };

		JPanel p2 = new JPanel();
		p2.setLayout(new TableLayout(size2));
		JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(al);
		p2.add(bcancel, "1,1");
		JButton bok = new JButton("OK");
		bok.addActionListener(al);
		p2.add(bok, "3,1");

		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(p1, BorderLayout.CENTER);
		dialog.getContentPane().add(p2, BorderLayout.SOUTH);
		dialog.getRootPane().setDefaultButton(bok);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}

	public String getSelectedProviders() {
		return mProviderText;
	}
}
