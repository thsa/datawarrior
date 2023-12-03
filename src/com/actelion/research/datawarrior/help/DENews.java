package com.actelion.research.datawarrior.help;

import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.JImagePanelFixedSize;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.util.BrowserControl;
import info.clearthought.layout.TableLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.net.URL;

public class DENews {
	private final static String TYPE_PERMANENT = "permanent";

	private String mTitle, mText, mImageURL, mMoreURL, mType;
	private BufferedImage mImage;
	private boolean mImageFailed;

	public DENews(String title, String text, String imageURL, String moreURL, String type) {
		mTitle = title;
		mText = text;
		mImageURL = (imageURL == null) ? null : imageURL.startsWith("http") ? imageURL : "https://".concat(imageURL);
		mMoreURL = (moreURL == null) ? null : moreURL.startsWith("http") ? moreURL : "https://".concat(moreURL);
		mType = type;
		if (isPermanent())  // if we need the image anyway, we retrieve it now
			mImage = getImage();
	}

	public boolean isPermanent() {
		return TYPE_PERMANENT.equals(mType);
	}

	public String getTitle() {
		return mTitle;
	}

	public String getMoreURL() {
		return mMoreURL;
	}

	/**
	 * @return the UI-scaled image
	 */
	public BufferedImage getImage() {
		if (mImageURL != null && mImage == null && !mImageFailed) {
			try {
				BufferedImage image = ImageIO.read(new URL(mImageURL));
				if (image != null)
					mImage = JImagePanelFixedSize.scaleImage(image,
								(int)(0.5 * HiDPIHelper.scale(image.getWidth())),
								(int)(0.5 * HiDPIHelper.scale(image.getHeight())));
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (mImage == null)
				mImageFailed = true;
		}
		return mImage;
	}

	public void show() {
		if (mText != null) {
			if (SwingUtilities.isEventDispatchThread())
				showInEDT();
			else
				try { SwingUtilities.invokeAndWait(() -> showInEDT() ); } catch (Exception e) {}
		}
		else if (mImageURL != null) {
			if (getImage() != null) {
				if (SwingUtilities.isEventDispatchThread())
					showInEDT();
				else
					try { SwingUtilities.invokeAndWait(() -> showInEDT() ); } catch (Exception e) {}
			}
		}

		if (mText == null && mImage == null && mMoreURL != null) {
			BrowserControl.displayURL(mMoreURL);
		}
	}

	private void showInEDT() {
		JDialog newsDialog = new JDialog();

		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.FILL, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap} };
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new TableLayout(size));

		if (mMoreURL != null) {
			JButton moreButton = new JButton("More...");
			moreButton.addActionListener(e -> {
				BrowserControl.displayURL(mMoreURL);
				newsDialog.dispose();
			});
			buttonPanel.add(moreButton, "1,1");
		}

		buttonPanel.add(new JLabel("News are accessible from the Help menu!", JLabel.CENTER), "3,1");

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(e -> newsDialog.dispose());
		buttonPanel.add(closeButton, "5,1");

		closeButton.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				super.keyPressed(e);
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
					newsDialog.dispose();
			}
		});

		newsDialog.setModalityType(JDialog.DEFAULT_MODALITY_TYPE);
		newsDialog.setUndecorated(true);
		newsDialog.getContentPane().setLayout(new BorderLayout());
		if (mText != null) {
			double[][] tsize = { {gap, TableLayout.PREFERRED, gap},	{gap, TableLayout.PREFERRED, gap} };
			JPanel textPanel = new JPanel();
			textPanel.setLayout(new TableLayout(tsize));
			textPanel.add(new JLabel(mText), "1,1");
			newsDialog.getContentPane().add(textPanel, BorderLayout.CENTER);
		}
		else {
			newsDialog.getContentPane().add(new JImagePanelFixedSize(mImage), BorderLayout.CENTER);
		}
		newsDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		newsDialog.pack();
		newsDialog.setLocationRelativeTo(DataWarrior.getApplication().getActiveFrame());
		newsDialog.setVisible(true);
	}
}