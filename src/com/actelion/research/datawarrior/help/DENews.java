package com.actelion.research.datawarrior.help;

import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.JImagePanelFixedSize;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.util.BrowserControl;
import info.clearthought.layout.TableLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

public class DENews {
	private final static String TYPE_PERMANENT = "permanent";

	private String mTitle, mImageURL, mMoreURL, mType;
	private BufferedImage mImage;
	private boolean mImageFailed;

	public DENews(String title, String imageURL, String moreURL, String type) {
		mTitle = title;
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
		if (mImage == null && !mImageFailed) {
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
		if (mImageURL != null) {
			getImage();
			if (mImage != null) {
				if (SwingUtilities.isEventDispatchThread())
					showInEDT();
				else
					try { SwingUtilities.invokeAndWait(() -> showInEDT() ); } catch (Exception e) {}
			}
		}

		if (mImage == null && mMoreURL != null) {
			BrowserControl.displayURL(mMoreURL);
		}
	}

	private void showInEDT() {
		JDialog imageDialog = new JDialog();

		int gap = HiDPIHelper.scale(8);
		double[][] size = {{gap, TableLayout.PREFERRED, gap, TableLayout.FILL, gap, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, gap}};
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new TableLayout(size));

		if (mMoreURL != null) {
			JButton moreButton = new JButton("More...");
			moreButton.addActionListener(e -> {
				BrowserControl.displayURL(mMoreURL);
				imageDialog.dispose();
			});
			buttonPanel.add(moreButton, "1,1");
		}

		buttonPanel.add(new JLabel("News are accessible from the Help menu!", JLabel.CENTER), "3,1");

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(e -> imageDialog.dispose());
		buttonPanel.add(closeButton, "5,1");

		imageDialog.setModalityType(JDialog.DEFAULT_MODALITY_TYPE);
		imageDialog.setUndecorated(true);
		imageDialog.getContentPane().setLayout(new BorderLayout());
		imageDialog.getContentPane().add(new JImagePanelFixedSize(mImage), BorderLayout.CENTER);
		imageDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		imageDialog.pack();
		imageDialog.setLocationRelativeTo(DataWarrior.getApplication().getActiveFrame());
		imageDialog.setVisible(true);
	}
}