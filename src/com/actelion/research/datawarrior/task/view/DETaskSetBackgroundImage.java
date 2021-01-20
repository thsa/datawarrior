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

package com.actelion.research.datawarrior.task.view;

import com.actelion.research.chem.io.CompoundFileHelper;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.clipboard.ImageClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.*;
import com.actelion.research.util.BinaryDecoder;
import com.actelion.research.util.BinaryEncoder;
import info.clearthought.layout.TableLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Properties;


public class DETaskSetBackgroundImage extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Background Image";

	private static final String PROPERTY_IMAGE_DATA = "imageData";
//	private static final String PROPERTY_HIDE_SCALE = "showScale";
	private static final String PROPERTY_HIDE_SCALE = "hideScale";	// allowed: x,y,true,false

	private JCheckBox		mCheckboxHideScale;
	private JPanel			mBackgroundImagePreview;
	private BufferedImage	mBackgroundImage;

	public DETaskSetBackgroundImage(Frame owner, DEMainPane mainPane, VisualizationPanel2D view) {
		super(owner, mainPane, view);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public JComponent createViewOptionContent() {
		int gap = HiDPIHelper.scale(8);
		double size2[][] = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.FILL,gap/2, TableLayout.PREFERRED, gap},
							 {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap*2,
									 HiDPIHelper.scale(160), gap*2, TableLayout.PREFERRED, gap} };
		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size2));
		p.add(new JLabel("Import from file:"), "1,1,3,1");
		p.add(new JLabel("Import from clipboard:"), "1,3,3,3");
		p.add(new JLabel("Remove background image:"), "1,5,3,5");
		JButton bOpen = new JButton("Open...");
		bOpen.addActionListener(this);
		p.add(bOpen, "5,1");
		JButton bPaste = new JButton("Paste");
		bPaste.addActionListener(this);
		p.add(bPaste, "5,3");
		JButton bClear = new JButton("Clear");
		bClear.addActionListener(this);
		p.add(bClear, "5,5");
		JLabel previewLabel = new JLabel("Preview:");
		previewLabel.setVerticalAlignment(SwingConstants.TOP);
		p.add(previewLabel, "1,7");
		mBackgroundImagePreview = new JPanel() {
			private static final long serialVersionUID = 0x20080611;
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				Dimension size = getSize();
				if (mBackgroundImage == null) {
					g.setColor(Color.LIGHT_GRAY);
					g.drawRect(0, 0, size.width-1, size.height-1);
					}
				else {
					g.drawImage(mBackgroundImage, 0, 0, size.width, size.height, null);
					}
				}
			};
		mBackgroundImagePreview.addMouseListener(new MouseAdapter() {
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

			private boolean handlePopupTrigger(MouseEvent e) {
				if (e.isPopupTrigger()) {
					if (mBackgroundImage != null) {
						JPopupMenu popup = new JPopupMenu();
						JMenuItem item1 = new JMenuItem("Copy Image");
						item1.addActionListener(event -> ImageClipboardHandler.copyImage(mBackgroundImage) );
						popup.add(item1);
						popup.show(mBackgroundImagePreview, e.getX(), e.getY());
						}
					return true;
					}
				return false;
				}
			});
		p.add(mBackgroundImagePreview, "3,7,5,7");
		mCheckboxHideScale = new JCheckBox("Hide scale and grid lines");
		mCheckboxHideScale.setHorizontalAlignment(SwingConstants.CENTER);
		mCheckboxHideScale.addActionListener(this);
		p.add(mCheckboxHideScale, "1,9,5,9");

		return p;
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel2D) ? null : "Background images can only be shown in 2D-Views.";
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Clear")) {
			mCheckboxHideScale.setSelected(false);
			mBackgroundImage = null;
			mBackgroundImagePreview.repaint();
			}

		if (e.getActionCommand().equals("Open...")) {
			FileFilter filter = CompoundFileHelper.createFileFilter(CompoundFileHelper.cFileTypeJPG
																  | CompoundFileHelper.cFileTypePNG, false);
			JFileChooser fc = new JFileChooser();

			// file chooser height does not automatically grow with UI scale factor
			if (HiDPIHelper.getUIScaleFactor() > 1)
				fc.setPreferredSize(new Dimension(fc.getPreferredSize().width, HiDPIHelper.scale(fc.getPreferredSize().height)));

			fc.setCurrentDirectory(FileHelper.getCurrentDirectory());
			fc.setFileFilter(filter);
			int option = fc.showOpenDialog(getParentFrame());
			FileHelper.setCurrentDirectory(fc.getCurrentDirectory());
			if (option != JFileChooser.APPROVE_OPTION)
				return;
			try {
				File file = fc.getSelectedFile();
				InputStream is = new FileInputStream(file);
				long length = file.length();

				if (length > 4000000) {
					JOptionPane.showMessageDialog(getParentFrame(), "Image file size exceeds limit.");
					is.close();
					return;
					}
			
				byte[] imageData = new byte[(int)length];
				int offset = 0;
				int numRead = 0;
				while (offset < imageData.length
					&& (numRead=is.read(imageData, offset, imageData.length-offset)) >= 0) {
					offset += numRead;
					}
				is.close();
			
				if (offset < imageData.length) {
					JOptionPane.showMessageDialog(getParentFrame(), "Could not completely read file "+file.getName());
					return;
					}

				mBackgroundImage = ImageIO.read(new ByteArrayInputStream(imageData));
				mBackgroundImagePreview.repaint();
				mCheckboxHideScale.setEnabled(true);
				}
			catch (IOException ioe) {
				JOptionPane.showMessageDialog(getParentFrame(), "Couldn't read file.");
				return;
				}
			}

		if (e.getActionCommand().equals("Paste")) {
			Image image = ImageClipboardHandler.pasteImage();
			if (image != null) {
				mBackgroundImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
				Graphics g = mBackgroundImage.createGraphics();
				g.drawImage(image, 0, 0, null);
				g.dispose();
				mBackgroundImagePreview.repaint();
				mCheckboxHideScale.setEnabled(true);
				}
			}

		super.actionPerformed(e);
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		JVisualization2D visualization = (JVisualization2D)((VisualizationPanel)view).getVisualization();

		byte[] image = visualization.getBackgroundImageData();
		if (image != null)
			configuration.put(PROPERTY_IMAGE_DATA, BinaryEncoder.toString(image, 8));

		configuration.setProperty(PROPERTY_HIDE_SCALE, JVisualization.SCALE_MODE_CODE[visualization.getScaleMode()]);
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		if (mBackgroundImage != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ImageIO.write(mBackgroundImage, "png", baos);
				} catch (IOException e) {}
			byte[] imageData = baos.toByteArray();
			configuration.put(PROPERTY_IMAGE_DATA, BinaryEncoder.toString(imageData, 8));
			configuration.put(PROPERTY_HIDE_SCALE, mCheckboxHideScale.isSelected() ? "true" : "false");
			}
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		String imageString = configuration.getProperty(PROPERTY_IMAGE_DATA);
		if (imageString != null) {
			try {
				byte[] bytes = BinaryDecoder.toBytes(imageString, 8);
				mBackgroundImage = ImageIO.read(new ByteArrayInputStream(bytes));
				}
			catch (IOException e) {
				mBackgroundImage = null;
				}
			}
		else {
			mBackgroundImage = null;
			}

		mBackgroundImagePreview.repaint();
		mCheckboxHideScale.setSelected("true".equals(configuration.getProperty(PROPERTY_HIDE_SCALE)));
		}

	@Override
	public void enableItems() {
		mCheckboxHideScale.setEnabled(mBackgroundImage != null);
		}

	@Override
	public void setDialogToDefault() {
		mBackgroundImage = null;
		mBackgroundImagePreview.repaint();
		mCheckboxHideScale.setSelected(false);
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		return true;
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (!(view instanceof VisualizationPanel2D))
			return;

		JVisualization2D visualization = (JVisualization2D)((VisualizationPanel2D)view).getVisualization();

		String imageString = configuration.getProperty(PROPERTY_IMAGE_DATA);
		if (imageString == null)
			visualization.setBackgroundImageData(null);
		else
			visualization.setBackgroundImageData(BinaryDecoder.toBytes(imageString, 8));

		String hideScaleString = configuration.getProperty(PROPERTY_HIDE_SCALE);
		if (hideScaleString != null)
			visualization.setScaleMode(findListIndex(hideScaleString, JVisualization.SCALE_MODE_CODE, 0));
		}
	}
