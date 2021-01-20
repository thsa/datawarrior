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

import com.actelion.research.datawarrior.DEFormView;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.AbstractViewTask;
import com.actelion.research.datawarrior.task.file.JFilePathLabel;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.gui.clipboard.ImageClipboardHandler;
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.gui.form.FormModel;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.filter.JFilterPanel;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.*;
import com.actelion.research.util.AnimatedGIFWriter;
import info.clearthought.layout.TableLayout;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Properties;


public class DETaskCopyViewImage extends AbstractViewTask implements ActionListener {
	public static final String TASK_NAME = "Create View Image";

	private static final String[] DPI = { "75", "150", "300", "600" };	// need to be multiples of 75

	private static final String PROPERTY_IMAGE_WIDTH = "width";
	private static final String PROPERTY_IMAGE_HEIGHT = "height";
	private static final String PROPERTY_RESOLUTION = "dpi";
	private static final String PROPERTY_KEEP_ASPECT_RATIO = "keepAspect";
	private static final String PROPERTY_FORMAT = "format";
	private static final String PROPERTY_TARGET = "target";
	private static final String PROPERTY_FILENAME = "fileName";
	private static final String PROPERTY_TRANSPARENT_BG = "transparentBG";
	private static final String PROPERTY_ALL_VIEWS = "allViews";

	private static final String TARGET_CLIPBOARD = "clipboard";
	private static final String TARGET_FILE = "file";
	private static final String FORMAT_PNG = "png";
	private static final String FORMAT_SVG = "svg";
	private static final String FORMAT_ANIMATED_GIF = "agif";

	private DEMainPane		mMainPane;
	private JTextField		mTextFieldWidth,mTextFieldHeight;
	private JCheckBox		mCheckBoxAllViews,mCheckBoxKeepAspectRatio,mCheckBoxTransparentBG;
	private JComboBox		mComboBoxResolution;
	private JRadioButton	mRadioButtonCopy,mRadioButtonSaveAsPNG,mRadioButtonSaveAsSVG,mRadioButtonSaveAsAnimatedGIF;
	private JButton			mButtonEdit;
	private JFilePathLabel	mLabelFileName;
	private int				mCurrentResolutionFactor;
	private boolean			mDisableEvents,mCheckOverwrite;
	private volatile BufferedImage mFrameOfAnimation;

	public DETaskCopyViewImage(Frame parent, DEMainPane mainPane, CompoundTableView view) {
		super(parent, mainPane, view);
		mMainPane = mainPane;
		mCheckOverwrite = true;
		}

//	public void keyTyped(KeyEvent arg0) {}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel)
			|| (view instanceof JStructureGrid)
			|| (view instanceof DEFormView) ? null
					: "Images can be created from 2D-, 3D- and structure views only.";
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
	public String getDialogTitle() {
		return "Create Image From View";
		}

	@Override
	public JComponent createInnerDialogContent() {
		JPanel mainpanel = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap/2, TableLayout.PREFERRED, gap*2, TableLayout.PREFERRED, gap/4, TableLayout.PREFERRED,
								gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap*2, TableLayout.PREFERRED,
									TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, gap*2,
									TableLayout.PREFERRED, gap*2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap} };
		mainpanel.setLayout(new TableLayout(size));

		mCheckBoxAllViews = new JCheckBox("Copy all graphical views into one image");
		mCheckBoxAllViews.addActionListener(this);
		mainpanel.add(mCheckBoxAllViews, "1,1,3,1");

		mCurrentResolutionFactor = 2;
		mTextFieldWidth = new JTextField();
		mTextFieldHeight = new JTextField();
		mCheckBoxKeepAspectRatio = new JCheckBox("Keep aspect ratio");
		mCheckBoxKeepAspectRatio.addActionListener(this);
 
		mTextFieldWidth.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent arg0) {
				try {
					JComponent view = mCheckBoxAllViews.isSelected() ? mMainPane : getViewComponent(getInteractiveView());
					int width = Integer.parseInt(mTextFieldWidth.getText());
					int height = calculateImageHeight(view, width, mCheckBoxKeepAspectRatio.isSelected());
					if (height != -1)
						mTextFieldHeight.setText(""+height);
					}
				catch (NumberFormatException nfe) {
					mTextFieldHeight.setText("");
					}
				}
			});
		mTextFieldHeight.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent arg0) {
				try {
					JComponent view = mCheckBoxAllViews.isSelected() ? mMainPane : getViewComponent(getInteractiveView());
					int height = Integer.parseInt(mTextFieldHeight.getText());
					int width = calculateImageWidth(view, height, mCheckBoxKeepAspectRatio.isSelected());
					if (width != -1)
						mTextFieldWidth.setText(""+width);
					}
				catch (NumberFormatException nfe) {
					mTextFieldWidth.setText("");
					}
				}
			});

		mTextFieldWidth.setColumns(6);
		mTextFieldHeight.setColumns(6);
		mainpanel.add(new JLabel("Image width:", JLabel.RIGHT), "1,3");
		mainpanel.add(mTextFieldWidth, "3,3");
		mainpanel.add(new JLabel("Image height:", JLabel.RIGHT), "1,5");
		mainpanel.add(mTextFieldHeight, "3,5");
		mainpanel.add(mCheckBoxKeepAspectRatio, "1,7,3,7");

		mComboBoxResolution = new JComboBox(DPI);
		mComboBoxResolution.addActionListener(this);
		mainpanel.add(new JLabel("Image resolution in dpi:"), "1,9");
		mainpanel.add(mComboBoxResolution, "3,9");

		ButtonGroup group = new ButtonGroup();
		mRadioButtonCopy = new JRadioButton("Copy image to clipboard", true);
		mRadioButtonCopy.addActionListener(this);
		group.add(mRadioButtonCopy);
		mainpanel.add(mRadioButtonCopy, "1,11,3,11");

		mRadioButtonSaveAsPNG = new JRadioButton("Save image as PNG-file", false);
		mRadioButtonSaveAsPNG.addActionListener(this);
		group.add(mRadioButtonSaveAsPNG);
		mainpanel.add(mRadioButtonSaveAsPNG, "1,12,3,12");

		mRadioButtonSaveAsSVG = new JRadioButton("Save image as SVG-file", false);
		mRadioButtonSaveAsSVG.addActionListener(this);
		group.add(mRadioButtonSaveAsSVG);
		mainpanel.add(mRadioButtonSaveAsSVG, "1,13,3,13");

		mRadioButtonSaveAsAnimatedGIF = new JRadioButton("Save animated GIF-file", false);
		mRadioButtonSaveAsAnimatedGIF.addActionListener(this);
		group.add(mRadioButtonSaveAsAnimatedGIF);
		mainpanel.add(mRadioButtonSaveAsAnimatedGIF, "1,14,3,14");

		mCheckBoxTransparentBG = new JCheckBox("Use transparent background");
		mainpanel.add(mCheckBoxTransparentBG, "1,16,3,16");

		mainpanel.add(new JLabel("File name:"), "1,18");
		mButtonEdit = new JButton(JFilePathLabel.BUTTON_TEXT);
		mButtonEdit.addActionListener(this);
		mainpanel.add(mButtonEdit, "3,18");

		mLabelFileName = new JFilePathLabel(!isInteractive());
		mainpanel.add(mLabelFileName, "1,20,3,20");

		return mainpanel;
		}

	private boolean allowsAnimatedGif() {
		return mMainPane.getParentPane().getPruningPanel().getAnimationCount() == 1;
	}

	private void enableItems() {
		JComponent view = mCheckBoxAllViews.isSelected() ? null : getViewComponent(getInteractiveView());

		boolean aspectChoosable = mCheckBoxAllViews.isSelected()
							   || view == null
							   || !(view instanceof JStructureGrid);
		mTextFieldHeight.setEnabled(aspectChoosable);
		mCheckBoxKeepAspectRatio.setEnabled(aspectChoosable);

		mRadioButtonSaveAsSVG.setEnabled(view == null || supportsSVG(view));
		mRadioButtonSaveAsAnimatedGIF.setEnabled(allowsAnimatedGif());
		mCheckBoxTransparentBG.setEnabled(view == null || !(view instanceof VisualizationPanel3D));
		}

	/**
	 * Get target image size from configuration. If viewComponent != null and configuration
	 * is set to keep the aspect ratio and one of width and height is 0, then this value
	 * is corrected to reflect the aspect ratio.
	 * @param configuration
	 * @param viewComponent
	 * @return
	 */
	private Dimension getImageSize(Properties configuration, JComponent viewComponent) {
		boolean keepAspectRatio = "true".equals(configuration.getProperty(PROPERTY_KEEP_ASPECT_RATIO));
		int width = Integer.parseInt(configuration.getProperty(PROPERTY_IMAGE_WIDTH, "0"));
		int height = Integer.parseInt(configuration.getProperty(PROPERTY_IMAGE_HEIGHT, "0"));
		if (viewComponent != null && keepAspectRatio) {
			if (width == 0 && height != 0)
				width = viewComponent.getWidth() * height / viewComponent.getHeight();
			if (height == 0 && width != 0)
				height = viewComponent.getHeight() * width / viewComponent.getWidth();
			}
		return new Dimension(width, height);
		}

	private void ensureSizeConstraints(JComponent viewComponent, boolean keepAspectRatio) {
		if (viewComponent != null) {
			int width = -1;
			try {
				width = Integer.parseInt(mTextFieldWidth.getText());
				}
			catch (NumberFormatException nfe) {}
			int height = -1;
			try {
				height = Integer.parseInt(mTextFieldHeight.getText());
				}
			catch (NumberFormatException nfe) {}
			if (width > 0) {
				height = calculateImageHeight(viewComponent, width, keepAspectRatio);
				if (height != -1)
					mTextFieldHeight.setText(""+height);
				}
			else if (height > 0) {
				width = calculateImageWidth(viewComponent, height, keepAspectRatio);
				if (width != -1)
					mTextFieldWidth.setText(""+width);
				}
			}
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (mDisableEvents)
			return;

		if (e.getSource() == mRadioButtonCopy
		 || e.getSource() == mRadioButtonSaveAsPNG
		 || e.getSource() == mRadioButtonSaveAsSVG
		 || e.getSource() == mRadioButtonSaveAsAnimatedGIF) {
			mButtonEdit.setEnabled(!mRadioButtonCopy.isSelected());
			if (e.getSource() != mRadioButtonCopy) {	// correct file extension if needed
				String path = mLabelFileName.getPath();
				if (path != null) {
					int filetype = mRadioButtonSaveAsPNG.isSelected() ? FileHelper.cFileTypePNG
								 : mRadioButtonSaveAsSVG.isSelected() ? FileHelper.cFileTypeSVG
								 : FileHelper.cFileTypeGIF;
					if (filetype != FileHelper.getFileType(path)) {
						path = FileHelper.removeExtension(path) + FileHelper.getExtension(filetype);
						mLabelFileName.setPath(path);
						mCheckOverwrite = true;
						}
					}
				}
			if (isInteractive() && mCheckBoxAllViews.isSelected()) {
				Dimension size = suggestTargetViewSize(null, mRadioButtonSaveAsSVG.isSelected());
				mTextFieldWidth.setText(Integer.toString(size.width));
				mTextFieldHeight.setText(Integer.toString(size.height));
				}
			return;
			}
		if (e.getSource() == mButtonEdit) {
			int filetype = mRadioButtonSaveAsSVG.isSelected() ? FileHelper.cFileTypeSVG
						 : mRadioButtonSaveAsPNG.isSelected() ? FileHelper.cFileTypePNG : FileHelper.cFileTypeGIF;
			String filename = resolvePathVariables(mLabelFileName.getPath());
			if (filename == null) {
				Dockable dockable = mMainPane.getSelectedDockable();
				filename = (dockable == null) ? null : dockable.getTitle();
				}
			filename = new FileHelper(getParentFrame()).selectFileToSave("Save Image To File", filetype, filename);
			if (filename != null) {
				mLabelFileName.setPath(filename);
				mCheckOverwrite = false;
				}
			return;
			}
		if (e.getSource() == mCheckBoxKeepAspectRatio) {
			if (mCheckBoxKeepAspectRatio.isSelected()) {
				JComponent viewComponent = mCheckBoxAllViews.isSelected() ? mMainPane : getViewComponent(getInteractiveView());
			   	ensureSizeConstraints(viewComponent, true);
				}

			return;
			}
		if (e.getSource() == mCheckBoxAllViews) {
			if (isInteractive()) {
				JComponent viewComponent = mCheckBoxAllViews.isSelected() ? null : getViewComponent(getInteractiveView());
				Dimension size = suggestTargetViewSize(viewComponent, mRadioButtonSaveAsSVG.isSelected());
				mTextFieldWidth.setText(Integer.toString(size.width));
				mTextFieldHeight.setText(Integer.toString(size.height));
				}
			else {
				setViewSelectionEnabled(!mCheckBoxAllViews.isSelected());
				}
			enableItems();
			return;
			}
		if (e.getSource() == mComboBoxResolution) {
			int newResolutionFactor = Integer.parseInt((String)mComboBoxResolution.getSelectedItem())/75;
			try {
				int width = Integer.parseInt(mTextFieldWidth.getText());
				mTextFieldWidth.setText(""+(width*newResolutionFactor/mCurrentResolutionFactor));
				}
			catch (NumberFormatException nfe) {}
			try {
				int height = Integer.parseInt(mTextFieldHeight.getText());
				mTextFieldHeight.setText(""+(height*newResolutionFactor/mCurrentResolutionFactor));
				}
			catch (NumberFormatException nfe) {}
			mCurrentResolutionFactor = newResolutionFactor;
			return;
			}
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		configuration.setProperty(PROPERTY_ALL_VIEWS, mCheckBoxAllViews.isSelected()?"true":"false");
		configuration.setProperty(PROPERTY_IMAGE_WIDTH, mTextFieldWidth.getText());
		configuration.setProperty(PROPERTY_IMAGE_HEIGHT, mTextFieldHeight.getText());
		configuration.setProperty(PROPERTY_RESOLUTION, DPI[mComboBoxResolution.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_KEEP_ASPECT_RATIO, mCheckBoxKeepAspectRatio.isSelected()?"true":"false");
		configuration.setProperty(PROPERTY_TRANSPARENT_BG, mCheckBoxTransparentBG.isSelected()?"true":"false");
		if (mRadioButtonCopy.isSelected()) {
			configuration.setProperty(PROPERTY_TARGET, TARGET_CLIPBOARD);
			configuration.setProperty(PROPERTY_FORMAT, FORMAT_PNG);
			}
		else if (mRadioButtonSaveAsPNG.isSelected()) {
			configuration.setProperty(PROPERTY_TARGET, TARGET_FILE);
			configuration.setProperty(PROPERTY_FORMAT, FORMAT_PNG);
			}
		else if (mRadioButtonSaveAsAnimatedGIF.isSelected()) {
			configuration.setProperty(PROPERTY_TARGET, TARGET_FILE);
			configuration.setProperty(PROPERTY_FORMAT, FORMAT_ANIMATED_GIF);
			}
		else if (mRadioButtonSaveAsSVG.isSelected()) {
			configuration.setProperty(PROPERTY_TARGET, TARGET_FILE);
			configuration.setProperty(PROPERTY_FORMAT, FORMAT_SVG);
			}

		if (!mRadioButtonCopy.isSelected()) {
			String fileName = mLabelFileName.getPath();
			if (fileName != null)
				configuration.setProperty(PROPERTY_FILENAME, fileName);
			}

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		mDisableEvents = true;
		mCheckBoxAllViews.setSelected("true".equals(configuration.getProperty(PROPERTY_ALL_VIEWS, "false")));
		mCheckBoxKeepAspectRatio.setSelected("true".equals(configuration.getProperty(PROPERTY_KEEP_ASPECT_RATIO, "true")));
		mCheckBoxTransparentBG.setSelected(mCheckBoxTransparentBG.isEnabled() && "true".equals(configuration.getProperty(PROPERTY_TRANSPARENT_BG)));
		mComboBoxResolution.setSelectedItem(configuration.getProperty(PROPERTY_RESOLUTION, "300"));
		mCurrentResolutionFactor = Integer.parseInt((String)mComboBoxResolution.getSelectedItem())/75;
		JComponent viewComponent = mCheckBoxAllViews.isSelected() ? mMainPane : getViewComponent(getInteractiveView());
		if (hasInteractiveView() && getInteractiveView() != getConfiguredView(configuration)) {
			// if we interactively selected a view and the configuration refers to another view, then don't use the other views's size
			setImageSizeTextFieldsToDefault();
			}
		else {
			mTextFieldWidth.setText(configuration.getProperty(PROPERTY_IMAGE_WIDTH));
			mTextFieldHeight.setText(configuration.getProperty(PROPERTY_IMAGE_HEIGHT));
			}
	   	ensureSizeConstraints(viewComponent, mCheckBoxKeepAspectRatio.isSelected());
	   	if (TARGET_CLIPBOARD.equals(configuration.getProperty(PROPERTY_TARGET)))
	   		mRadioButtonCopy.setSelected(true);
	   	else if (FORMAT_SVG.equals(configuration.getProperty(PROPERTY_FORMAT)))
	   		mRadioButtonSaveAsSVG.setSelected(true);
	    else if (FORMAT_ANIMATED_GIF.equals(configuration.getProperty(PROPERTY_FORMAT)) && allowsAnimatedGif())
		    mRadioButtonSaveAsAnimatedGIF.setSelected(true);
	   	else
	   		mRadioButtonSaveAsPNG.setSelected(true);
	   	mButtonEdit.setEnabled(!mRadioButtonCopy.isSelected());

		String filename = configuration.getProperty(PROPERTY_FILENAME);
		if (filename != null && !new File(filename).exists())
			filename = null;
		mLabelFileName.setPath(filename);
		enableItems();
		mDisableEvents = false;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mDisableEvents = true;
		mCheckBoxAllViews.setSelected(false);
		mCurrentResolutionFactor = 2;	// reflects 150 dpi set below
		setImageSizeTextFieldsToDefault();
		ensureSizeConstraints(getViewComponent(getInteractiveView()), true);
		mCheckBoxKeepAspectRatio.setSelected(true);
		mCheckBoxTransparentBG.setSelected(false);
		mComboBoxResolution.setSelectedItem("150");	// reflects resolution factor 2 above
   		mRadioButtonCopy.setSelected(true);
	   	mButtonEdit.setEnabled(false);
		mLabelFileName.setPath(null);
		enableItems();
		mDisableEvents = false;
		}

	public Properties getRecentOrDefaultConfiguration() {
		Properties configuration = getRecentConfiguration();
		if (configuration != null) {
			if (getInteractiveView() != getConfiguredView(configuration)) {
				int dpi = 300;
				try { dpi = Integer.parseInt(configuration.getProperty(PROPERTY_RESOLUTION)); } catch (NumberFormatException nfe) {}
				int resolutionFactor = dpi / 75;
				JComponent viewComponent = getViewComponent(getInteractiveView());
				setConfiguredView(configuration);
				configuration.setProperty(PROPERTY_IMAGE_WIDTH, ""+(resolutionFactor*(viewComponent == null ? 640 : viewComponent.getWidth())));
				configuration.setProperty(PROPERTY_IMAGE_HEIGHT, ""+(resolutionFactor*(viewComponent == null ? 640 : viewComponent.getHeight())));
				if (FORMAT_ANIMATED_GIF.equals(configuration.getProperty(PROPERTY_FORMAT)) && !allowsAnimatedGif())
					configuration.setProperty(PROPERTY_FORMAT, FORMAT_PNG);
				}
			return configuration;
			}

		configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_ALL_VIEWS, "false");
		JComponent viewComponent = getViewComponent(getInteractiveView());
		configuration.setProperty(PROPERTY_IMAGE_WIDTH, ""+(2*(viewComponent == null ? 640 : viewComponent.getWidth())));
		configuration.setProperty(PROPERTY_IMAGE_HEIGHT, ""+(2*(viewComponent == null ? 640 : viewComponent.getHeight())));
		configuration.setProperty(PROPERTY_RESOLUTION, "150");
		configuration.setProperty(PROPERTY_KEEP_ASPECT_RATIO, "true");
		configuration.setProperty(PROPERTY_TRANSPARENT_BG, "false");
		configuration.setProperty(PROPERTY_TARGET, TARGET_CLIPBOARD);
		configuration.setProperty(PROPERTY_FORMAT, FORMAT_PNG);
		return configuration;
		}

	private void setImageSizeTextFieldsToDefault() {
		JComponent viewComponent = mCheckBoxAllViews.isSelected() ? mMainPane : getViewComponent(getInteractiveView());
		mTextFieldWidth.setText(""+(mCurrentResolutionFactor*(viewComponent == null ? 640 : viewComponent.getWidth())));
		mTextFieldHeight.setText(""+(mCurrentResolutionFactor*(viewComponent == null ? 640 : viewComponent.getHeight())));
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		boolean isAllViews = "true".equals(configuration.getProperty(PROPERTY_ALL_VIEWS, "false"));
		if (isLive && isAllViews) {
			boolean graphicalViewFound = false;
			for (Dockable d:mMainPane.getDockables()) {
				if (d.isVisibleDockable()
				 && (d.getContent() instanceof VisualizationPanel2D
				  || d.getContent() instanceof VisualizationPanel3D
				  || d.getContent() instanceof DEFormView)) {
					if (FORMAT_SVG.equals(configuration.getProperty(PROPERTY_FORMAT))
					 && !supportsSVG(getViewComponent((CompoundTableView)d.getContent()))) {
						showErrorMessage("3D-views and form views cannot be copied as SVG.");
						return false;
						}
					if (d.getContent() instanceof VisualizationPanel3D) {
						if ("true".equals(configuration.getProperty(PROPERTY_TRANSPARENT_BG))) {
							showErrorMessage("3D-views do not support a transparent background.");
							return false;
							}
						}
					graphicalViewFound = true;
					break;
					}
				}
			if (!graphicalViewFound) {
				showErrorMessage("No graphical view nor form view found.");
				return false;
				}
			}

		if (!isAllViews && !super.isConfigurationValid(configuration, isLive))
			return false;

		JComponent viewComponent = isAllViews ? mMainPane : getViewComponent(getConfiguredView(configuration));
		try {
			Dimension size = getImageSize(configuration, viewComponent);
			if (size.width < 32 || size.height < 32) {
				showErrorMessage("Width or height is too small.");
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Width or height is not numerical.");
			return false;
			}

		if (!isAllViews) {
			if (isLive && !supportsSVG(viewComponent) && FORMAT_SVG.equals(configuration.getProperty(PROPERTY_FORMAT))) {
				showErrorMessage("Only structure-, form-, and 2D-views can be saved as SVG.");
				return false;
				}
	
			if (isLive && viewComponent instanceof JVisualization3D && "true".equals(configuration.getProperty(PROPERTY_TRANSPARENT_BG))) {
				showErrorMessage("The 3D-view does not support a transparent background.");
				return false;
				}
			}

		if (isLive) {
			String target = configuration.getProperty(PROPERTY_TARGET, TARGET_CLIPBOARD);
			if (target.equals(TARGET_FILE)
			 && !isFileAndPathValid(configuration.getProperty(PROPERTY_FILENAME), true, mCheckOverwrite))
				return false;

			if (FORMAT_ANIMATED_GIF.equals(configuration.getProperty(PROPERTY_FORMAT)) && !allowsAnimatedGif()) {
				showErrorMessage("An animated GIF can only be generated, if exactly one filter animation is running.");
				return false;
				}
			}

		return true;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public void runTask(Properties configuration) {
		boolean isAllViews = "true".equals(configuration.getProperty(PROPERTY_ALL_VIEWS, "false"));
		JComponent viewComponent = isAllViews ? mMainPane : getViewComponent(getConfiguredView(configuration));
		Dimension size = getImageSize(configuration, viewComponent);

		int _dpi = 300;
		try {
			_dpi = Integer.parseInt(configuration.getProperty(PROPERTY_RESOLUTION)); } catch (NumberFormatException nfe) {}
		try {
			boolean transparentBG = "true".equals(configuration.getProperty(PROPERTY_TRANSPARENT_BG, "false"));
			final int dpi = _dpi;
			if (FORMAT_SVG.equals(configuration.getProperty(PROPERTY_FORMAT))) {
				File file = new File(resolvePathVariables(configuration.getProperty(PROPERTY_FILENAME)));
				try {
					Writer writer = new OutputStreamWriter(new FileOutputStream(file),"UTF-8");
					SVGGraphics2D g2d = null;
					if (isAllViews) {
						Rectangle parentBounds = getCombinedGraphicalViewBounds(true);
						g2d = prepareSVG(size.width, size.height);
						for (Dockable d:mMainPane.getDockables()) {
							if (d.isVisibleDockable()
							 && viewQualifiesForMultiView(d.getContent(), true)) {
								Rectangle bounds = getScaledBounds(d, parentBounds, size);
								paintViewInSVG(getViewComponent((CompoundTableView)d.getContent()), bounds, dpi/75, transparentBG, g2d);
								}
							}
						}
					else {
						int width = size.width;
						int height = (viewComponent instanceof JStructureGrid) ?
								((JStructureGrid)viewComponent).getTotalHeight(width) : size.height;
						g2d = prepareSVG(width, height);

						paintViewInSVG(viewComponent, new Rectangle(0, 0, width, height), dpi/75, transparentBG, g2d);
						}
					// Finally, stream out SVG to the standard output using UTF-8 encoding.
					boolean useCSS = true; // we want to use CSS style attributes
//					Writer writer = new OutputStreamWriter(System.out, "UTF-8");	we have our own writer
					g2d.stream(writer, useCSS);
					writer.close();
					}
				catch (IOException ioe) {
					showErrorMessage("Couldn't write image file.");
					}
				}
			else if (FORMAT_ANIMATED_GIF.equals(configuration.getProperty(PROPERTY_FORMAT))) {
				JFilterPanel filter = mMainPane.getParentPane().getPruningPanel().getFirstAnimatingFilter();
				int totalFrames = filter.getFullFrameAnimationCount();
				int totalMillis = totalFrames * filter.getFrameMillis();

				JProgressDialog dialog = new JProgressDialog(getParentFrame());
				dialog.startProgress("Building animated image...", 0, totalMillis);
				new Thread(() -> {
					try {
						File file = new File(resolvePathVariables(configuration.getProperty(PROPERTY_FILENAME)));
						FileOutputStream os = new FileOutputStream(file);
						AnimatedGIFWriter writer = new AnimatedGIFWriter(true); // True for dither. Will need more memory and CPU
						writer.prepareForWrite(os, -1, -1);
						int frameMillis = filter.getFrameMillis();
						if (frameMillis < 200)
							frameMillis = 200;
						for (int millis=0; millis<totalMillis && !dialog.threadMustDie(); millis+=frameMillis) {
							dialog.updateProgress(millis);
							filter.getAnimator().setSuspended(true);
							filter.setAnimationFrame(totalFrames*millis/totalMillis);
							SwingUtilities.invokeAndWait(() ->
								mFrameOfAnimation = createViewImage(viewComponent, size, dpi, isAllViews, transparentBG) );
							writer.writeFrame(os, mFrameOfAnimation, frameMillis);
							}
						writer.finishWrite(os);
						filter.getAnimator().setSuspended(false);
						}
					catch (Exception e) {
						SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(getParentFrame(), "Error: "+e.getMessage()) );
						}
					dialog.close();
					}).start();
				dialog.setVisible(true);
				}
			else {	// png
				BufferedImage image = createViewImage(viewComponent, size, dpi, isAllViews, transparentBG);
				if (TARGET_FILE.equals(configuration.getProperty(PROPERTY_TARGET))) {
					File file = new File(resolvePathVariables(configuration.getProperty(PROPERTY_FILENAME)));
					try {
						/* do something like this for setting the image to 300 dpi
						PNGEncodeParam png = PNGEncodeParam.getDefaultEncodeParam((BufferedImage)image);
						png.setPhysicalDimension(11812, 11812, 1);  // 11812 dots per meter = 300dpi
						JAI.create("filestore", (BufferedImage)image, "analemma.png", "PNG");   */
						javax.imageio.ImageIO.write(image, "png", file);
						}
					catch (IOException ioe) {
						showErrorMessage("Couldn't write image file.");
						}
					}
				else {
					ImageClipboardHandler.copyImage(image);
					}
				}
			}
		catch (Exception ex) {
			showErrorMessage("Unexpected exception creating image.");
			ex.printStackTrace();
			}
		catch (OutOfMemoryError ex) {
			showErrorMessage("This exceeds your available memory. Try a smaller size.");
			}
		}

	private BufferedImage createViewImage(JComponent viewComponent, Dimension size, int dpi, boolean isAllViews, boolean transparentBG) {
		if (!isAllViews)
			return (BufferedImage)createComponentImage(null, viewComponent, new Rectangle(size), dpi/75, transparentBG);

		Rectangle parentBounds = getCombinedGraphicalViewBounds(false);
		BufferedImage image = createTransparentImage(size.width, size.height);
		for (Dockable d:mMainPane.getDockables()) {
			if (d.isVisibleDockable() && viewQualifiesForMultiView(d.getContent(), false)) {
				Rectangle bounds = getScaledBounds(d, parentBounds, size);
				createComponentImage(image, getViewComponent((CompoundTableView)d.getContent()), bounds, dpi/75, transparentBG);
				}
			}

		return image;
		}

	private BufferedImage createTransparentImage(int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);	// we need ARGB for transparency

// Some applications on Windows have trouble to work with transparency. We may need to consider to use white backgrounds for Windows?!
//		image.getGraphics().setColor(new Color(255, 255, 255, 0));
//		image.getGraphics().fillRect(0, 0, width, height);
		return image;
		}

	private boolean viewQualifiesForMultiView(Component view, boolean isSVG) {
		return view instanceof VisualizationPanel2D
			|| (view instanceof VisualizationPanel3D && !isSVG)
			|| view instanceof DEFormView;
		}

	private Rectangle getScaledBounds(Dockable d, Rectangle parent, Dimension size) {
		Rectangle bounds = getBoundsOnMainPane(d.getParent() instanceof JTabbedPane ? d.getParent() : d);
		return new Rectangle(size.width*(bounds.x-parent.x)/parent.width,
							 size.height*(bounds.y-parent.y)/parent.height,
							 bounds.width*size.width/parent.width,
							 bounds.height*size.height/parent.height);
		}

	/**
	 * Paints a high resolution representation of viewComponent at the position of bounds
	 * into the given image. Image may be empty if bounds.x and bounds.y are 0.
	 * In this case a new image of size bounds.width and bounds.height is created.
	 * @param image
	 * @param viewComponent
	 * @param bounds
	 * @param fontScaling
	 * @param transparentBG
	 * @return the image
	 */
	private Image createComponentImage(BufferedImage image, JComponent viewComponent, Rectangle bounds, float fontScaling, boolean transparentBG) {
		if (viewComponent instanceof JVisualization3D) {
			JVisualization3D v3D = (JVisualization3D)viewComponent;
			if (image == null)
				return v3D.getViewImage(bounds, fontScaling, JVisualization3D.STEREO_MODE_NONE);

			Image vi = v3D.getViewImage(new Rectangle(0, 0, bounds.width, bounds.height), fontScaling, JVisualization3D.STEREO_MODE_NONE);
			image.getGraphics().drawImage(vi, bounds.x, bounds.y, null);
			return image;
			}
		else if (viewComponent instanceof JVisualization2D) {
			JVisualization2D v2D = (JVisualization2D)viewComponent;
			if (image == null)
				image = createTransparentImage(bounds.width, bounds.height);

			Graphics2D imageG = image.createGraphics();
			v2D.paintHighResolution(imageG, bounds, fontScaling, transparentBG, false);
			return image;
			}
		else if (viewComponent instanceof JStructureGrid) {
			int height = ((JStructureGrid)viewComponent).getTotalHeight(bounds.width);
			image = createTransparentImage(bounds.width, bounds.height);
			Graphics imageG = image.getGraphics();
			if (!transparentBG) {
				imageG.setColor(Color.WHITE);
				imageG.fillRect(0, 0, bounds.width, height);
				}
			((JStructureGrid)viewComponent).paintHighResolution(imageG, new Dimension(bounds.width, height), fontScaling, transparentBG);
			}
		else if (viewComponent instanceof JCompoundTableForm) {
			image = createTransparentImage(bounds.width, bounds.height);
			Graphics imageG = image.getGraphics();
			if (!transparentBG) {
				imageG.setColor(Color.WHITE);
				imageG.fillRect(0, 0, bounds.width, bounds.height);
				}
			CompoundTableModel tableModel = mMainPane.getTableModel();
            FormModel model = new CompoundTableFormModel(tableModel, tableModel.getActiveRow());
            ((JCompoundTableForm)viewComponent).updatePrintColors(tableModel.getActiveRow());
			((JCompoundTableForm)viewComponent).print((Graphics2D)imageG, new Rectangle2D.Double(0, 0, bounds.width, bounds.height), fontScaling, model, false);
			}
		return image;
		}

	/**
	 * Calculates from mCurrentResolutionFactor and screen size of the dockable's component a suggested target size.
	 * If !isInteractive(), then it uses a default size. If d==null, then it scales the bounds
	 * of all compatible graphics views for mCurrentResolutionFactor upscaling.
	 * @param viewComponent null for all graphical views
	 * @param isSVG
	 * @return
	 */
	private Dimension suggestTargetViewSize(JComponent viewComponent, boolean isSVG) {
		Dimension size = new Dimension(600, 600);
		if (isInteractive()) {
			if (viewComponent == null) {
				Rectangle bounds = getCombinedGraphicalViewBounds(isSVG);
				if (bounds != null) {
					size.width = bounds.width;
					size.height = bounds.height;
					}
				}
			else {
				size.width = viewComponent.getWidth();
				size.height = viewComponent.getHeight();
				}
			}

		size.width *= mCurrentResolutionFactor;
		size.height *= mCurrentResolutionFactor;
			
		if (viewComponent instanceof JStructureGrid)
			size.height = ((JStructureGrid)viewComponent).getTotalHeight(size.width);

		return size;
		}

	/**
	 * @param isSVG
	 * @return
	 */
	private Rectangle getCombinedGraphicalViewBounds(boolean isSVG) {
		Rectangle parentBounds = null;
		for (Dockable d:mMainPane.getDockables()) {
			if (d.isVisibleDockable()
			 && viewQualifiesForMultiView(d.getContent(), isSVG)) {
				Rectangle bounds = getBoundsOnMainPane((d.getParent() instanceof JTabbedPane) ? d.getParent() : d);
				parentBounds = (parentBounds == null) ? bounds : parentBounds.union(bounds);
				}
			}
		return parentBounds;
		}

	private JComponent getViewComponent(CompoundTableView view) {
		return (view == null) ? null
			 : (view instanceof DEFormView) ? ((DEFormView)view).getCompoundTableForm()
			 : (view instanceof VisualizationPanel) ? ((VisualizationPanel)view).getVisualization()
			 : (JComponent)view;
		}

	private Rectangle getBoundsOnMainPane(Component c) {
		Rectangle bounds = c.getBounds();
		while (c.getParent() != mMainPane) {
			c = c.getParent();
			bounds.x += c.getX();
			bounds.y += c.getY();
			}
		return bounds;
		}

	private int calculateImageHeight(JComponent viewComponent, int width, boolean keepAspectRatio) {
		if (viewComponent == null)
			return -1;
		if (viewComponent instanceof JStructureGrid)
			return ((JStructureGrid)viewComponent).getTotalHeight(width);
		if (keepAspectRatio) {
			if (viewComponent == mMainPane) {
				Rectangle combinedBounds = getCombinedGraphicalViewBounds(mRadioButtonSaveAsSVG.isSelected());
				return width*combinedBounds.height/combinedBounds.width;
				}
			else {
				return width*viewComponent.getHeight()/viewComponent.getWidth();
				}
			}
		return -1;
		}

	private int calculateImageWidth(JComponent viewComponent, int height, boolean keepAspectRatio) {
		if (viewComponent == null)
			return -1;
		if (keepAspectRatio) {
			if (viewComponent == mMainPane) {
				Rectangle combinedBounds = getCombinedGraphicalViewBounds(mRadioButtonSaveAsSVG.isSelected());
				return height*combinedBounds.width/combinedBounds.height;
				}
			else {
				return height*viewComponent.getWidth()/viewComponent.getHeight();
				}
			}
		return -1;
		}

	private boolean supportsSVG(JComponent viewComponent) {
		return (viewComponent instanceof JVisualization2D
			|| (viewComponent instanceof JCompoundTableForm)
			|| (viewComponent instanceof JStructureGrid));
		}

	/**
	 * This creates and writes an SVG to the given writer using the JFreeSVG library.
	 * (not used because the tested version 2.1 of JFreeSVG seems to create larger files
	 * and converts non-filled rectangles into 4 lines which do not touch at the corners)
	 * @param v2D
	 * @param width
	 * @param height
	 * @param fontScaling
	 * @param transparentBG
	 * @param writer
	 * @throws IOException
	 *
	private void writeSVG(JVisualization2D v2D, int width, int height, int fontScaling, boolean transparentBG, Writer writer) throws IOException {
		SVGGraphics2D g2d = new SVGGraphics2D(width, height);

		Map<SVGHints.Key,Object> hints = new HashMap<SVGHints.Key,Object>();
		hints.put(SVGHints.KEY_IMAGE_HANDLING, SVGHints.VALUE_IMAGE_HANDLING_EMBED);
		g2d.addRenderingHints(hints);

		v2D.paintHighResolution(g2d, new Rectangle(0, 0, width, height), fontScaling, transparentBG, false);
		String svgDoc = g2d.getSVGDocument();
		writer.write(svgDoc);
		}*/

	/**
	 * This creates an empty SVG using the Batik library.
     * @param width
     * @param height
	 */
	private SVGGraphics2D prepareSVG(int width, int height) {
		// example from: http://xmlgraphics.apache.org/batik/using/svg-generator.html

		// Get a DOMImplementation.
		DOMImplementation impl = GenericDOMImplementation.getDOMImplementation();

		// Create an instance of org.w3c.dom.Document.
		String svgNS = "http://www.w3.org/2000/svg";
		Document myFactory = impl.createDocument(svgNS, "svg", null);

		SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(myFactory);
		ctx.setEmbeddedFontsOn(true);
		ctx.setComment("Visualization generated by DataWarrior with Batik SVG Generator");
		SVGGraphics2D g2d = new SVGGraphics2D(ctx, false);

		g2d.setSVGCanvasSize(new Dimension(width,height));

		return g2d;
		}

	/*
	 * This paints a view into an SVG using the Batik library.
	 */
	private void paintViewInSVG(JComponent viewComponent, Rectangle bounds, float fontScaling,
								boolean transparentBG, SVGGraphics2D g2d) {
		if (viewComponent instanceof JVisualization2D) {
			((JVisualization2D)viewComponent).paintHighResolution(g2d, bounds, fontScaling, transparentBG, false);
			}
		else if (viewComponent instanceof JCompoundTableForm) {
			if (!transparentBG) {
				g2d.setColor(Color.WHITE);
				g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
				}
			CompoundTableModel tableModel = mMainPane.getTableModel();
            FormModel model = new CompoundTableFormModel(tableModel, tableModel.getActiveRow());
            ((JCompoundTableForm)viewComponent).updatePrintColors(tableModel.getActiveRow());
            ((JCompoundTableForm)viewComponent).print(g2d, new Rectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height), fontScaling, model, false);
			}
		else if (viewComponent instanceof JStructureGrid) {
			if (!transparentBG) {
				g2d.setColor(Color.WHITE);
				g2d.fillRect(0, 0, bounds.width, bounds.height);
				}
			((JStructureGrid)viewComponent).paintHighResolution(g2d, new Dimension(bounds.width, bounds.height), fontScaling, transparentBG);
			}
		}
	}
