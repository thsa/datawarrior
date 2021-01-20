package com.actelion.research.datawarrior.help;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.clipboard.ImageClipboardHandler;
import com.actelion.research.gui.clipboard.TextClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

import static com.actelion.research.chem.io.CompoundTableConstants.cExtensionNameFileExplanation;

public class DETaskSetExplanationHTML extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Set Explanation HTML";

// example for embedded image:
//	String testImage = "iVBORw0KGgoAAAANSUhEUgAAAGQAAAAyCAYAAACqNX6+AAACeklEQVR42u1bHZBCURgNgiBYCINgIVhYCIKFhSBYCIIgCIKFxSBoZpsJgjAIgmAhCIIgCIKFIAiChSAIF4IgCL7d82abnWl69Xq9+7r1Dhyp93PfOff7ufd+n8/nEyF0AkmgIAQFoSDEjQgSCn1LPD6SbPZDSqWKNBqv0m5nZDh8lsnkUebziIH1OiC/d+wF/tteN50+GPfiGbVaQcrld8nnm8Y78C4K8odAYC3R6Jfkci2pVosGaYtFWDYbvynRKgDx8G4Ij7FgTBjbzQuC2ZhOd4wZCgIOzfBLYysSxooxh8OL2xAEH4KPGo3irs98pwF3CZcXi42vS5CtCPiAaxfBDLPZvRQKNUWW49CDEomBdDrpmxXBDN1uSlKprvj9m8sLgkHAx47HMU+JYObSkBmenxDYvDGTaRum63UhdoFUG9maa4IgW4KZkvzD6PVebMaYEy6GSS6XdyTcIlaroA1rsRgr6vU3zwVsp4BFZzC4ckYQBCmYH4k9D4NBwmLAP2IZFMNZUY6nxwf+rFRKJNJhYLVvSxAs9Bgz1ADcniQIzIprDLVbL+aua8+PyWSfxCkGOLYsSKuVI2mKAY4tC4LlP0lTv8ViWRAS5g4oyLUKQpelmctiUNcsqDPt1Szt5cJQs4Uht0402zrh5qKGm4tb19XvJ0mkq2ciPKC6ngOq3SNcEms/xXXsCJdFDhoWOeyWAdGFWSsDikTm7hXKwVq4VjEvlLNfWnpmKSkqGFlK+l9Kaj1WuFBs7cWKRrgmbYqtvdyOUCxW9W5HOCQOXBobdtjSxpY2J5o+L0W+55o+7bZFN5t5JW3RT0+fbIsmKAgFISgIBSHU4QdCoO0W7Xd4AwAAAABJRU5ErkJggg==";
//	String explanation = "<html><body>Local image<br><img src=\"data:image/png;charset=utf-8;base64,"+testImage+"\"></body></html>";

	private String ENCODED_NL = "<NL>";
	private String DECODED_NL = "\n";
	private String DEFAULT_HTML = "<html>"+DECODED_NL+"<body>"+DECODED_NL+DECODED_NL+"</body>"+DECODED_NL+"</html>"+DECODED_NL;

	private static final String PROPERTY_HTML = "html";

	private CompoundTableModel mTableModel;
	private JTextArea mTextArea;

	public DETaskSetExplanationHTML(Frame parent, CompoundTableModel tableModel) {
		super(parent, false);
		mTableModel = tableModel;
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}

	@Override
	public JPanel createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		int width = HiDPIHelper.scale(740);
		int height = HiDPIHelper.scale(540);
		double[][] size = { {gap, width, gap}, {gap, TableLayout.PREFERRED, gap, height, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		content.add(new JLabel("Please edit the explanation text using HTML code:"), "1,1");
		mTextArea = new JTextArea();
		mTextArea.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (!handlePopupTrigger(e))
					super.mousePressed(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				if (!handlePopupTrigger(e))
					super.mouseReleased(e);
			}
		});
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setViewportView(mTextArea);
		content.add(scrollPane, "1,3");

		return content;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Copy")) {
			String text = mTextArea.getSelectedText();
			TextClipboardHandler.copyText(text);
			return;
			}

		if (e.getActionCommand().equals("Paste")) {
			String text = TextClipboardHandler.pasteText();
			if (text != null) {
				mTextArea.insert(text, mTextArea.getCaretPosition());
				return;
				}

			Image image = ImageClipboardHandler.pasteImage();
			if (image != null) {
				BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
				Graphics g = bi.createGraphics();
				g.drawImage(image, 0, 0, null);
				g.dispose();

				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				try {
					ImageIO.write(bi, "png", bos);
					byte[] data = Base64.getEncoder().encode(bos.toByteArray());
					bos.close();
					StringBuilder sb = new StringBuilder("<img src=\"data:image/png;charset=utf-8;base64,");
					for (int i=0; i<data.length; i+=80) {
						sb.append('\n');
						sb.append(new String(data, i, Math.min(80, data.length-i)));
						}
					sb.append("\">");
					mTextArea.insert(sb.toString(), mTextArea.getCaretPosition());
					}
				catch (IOException ioe) {}
				return;
				}
			}
		}

	private boolean handlePopupTrigger(MouseEvent e) {
		if (e.isPopupTrigger()) {
			JMenuItem menuItem1 = new JMenuItem("Copy");
			menuItem1.addActionListener(this);
			if (mTextArea.getSelectedText() == null)
				menuItem1.setEnabled(false);

			JMenuItem menuItem2 = new JMenuItem("Paste");
			menuItem2.addActionListener(this);
			if (Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null) == null)
				menuItem2.setEnabled(false);

			JPopupMenu popup = new JPopupMenu();
			popup.add(menuItem1);
			popup.add(menuItem2);
			popup.show(mTextArea, e.getX(), e.getY());
			return true;
			}
		return false;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		return true;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_HTML, encode(mTextArea.getText()));
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String html = isInteractive() ? (String)mTableModel.getExtensionData(cExtensionNameFileExplanation) : configuration.getProperty(PROPERTY_HTML);
		if (html == null || html.length() == 0)
			html = DEFAULT_HTML;
		mTextArea.setText(decode(html));
	}

	@Override
	public void setDialogConfigurationToDefault() {
		String html = isInteractive() ? (String)mTableModel.getExtensionData(cExtensionNameFileExplanation) : null;
		mTextArea.setText(html != null ? html : DEFAULT_HTML);
	}

	private String encode(String s) {
		return s == null ? null : s.replaceAll(DECODED_NL, ENCODED_NL).replaceAll("\"", "\\\"");
	}

	private String decode(String s) {
		return s == null ? null : s.replaceAll(ENCODED_NL, DECODED_NL).replaceAll("\\\"", "\"");
	}

	@Override
	public void runTask(Properties configuration) {
		String html = decode(configuration.getProperty(PROPERTY_HTML, ""));
		if (html.length() == 0 || html.equals(DEFAULT_HTML))
			html = null;
		mTableModel.setExtensionData(cExtensionNameFileExplanation, html);
	}
}
