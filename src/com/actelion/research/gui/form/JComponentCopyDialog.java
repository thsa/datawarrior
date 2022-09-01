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

package com.actelion.research.gui.form;

import com.actelion.research.chem.io.CompoundFileFilter;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JFileChooserOverwrite;
import com.actelion.research.gui.clipboard.ImageClipboardHandler;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class JComponentCopyDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 0x20060904;

    private static final String[] DPI_LIST = { "75 dpi", "150 dpi", "300 dpi", "600 dpi" };
    private static final int[] FONT_FACTOR = {   1,     2,     4,    16 };

    private Frame			mParentFrame;
    private Component   	mComponent;
    private JTextField		mTextFieldWidth,mTextFieldHeight;
    private JComboBox		mComboBoxResolution;
    private JRadioButton    mRadioButtonCopy,mRadioButtonSaveAsPNG,mRadioButtonSaveAsSVG;
    
    public JComponentCopyDialog(Frame owner, final Component c, String title, boolean automaticHeight) {
		super(owner, title, true);
		mParentFrame = owner;
        mComponent = c;

		getContentPane().setLayout(new BorderLayout());

		JPanel mainpanel = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.FILL, 8},
		        			{8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 2, TableLayout.PREFERRED,
							 2, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 2, TableLayout.PREFERRED,
							 2, TableLayout.PREFERRED, 8} };
		mainpanel.setLayout(new TableLayout(size));

        mainpanel.add(new JLabel("Please define the image size"), "1,1,5,1");
		mainpanel.add(new JLabel("Image width:"), "1,3");
		mainpanel.add(new JLabel("Image height:"), "1,5");
		mainpanel.add(new JLabel("Target Resolution:"), "1,7");

        Dimension imageSize = getDefaultImageSize(c);
		mTextFieldWidth = new JTextField(""+imageSize.width);
        mTextFieldHeight = new JTextField(""+imageSize.height);
        if (automaticHeight) {
            mTextFieldHeight.setText(""+calculateImageHeight(c, imageSize.width));
            mTextFieldHeight.setEnabled(false);
            mTextFieldWidth.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent arg0) {
                    try {
                        int width = Integer.parseInt(mTextFieldWidth.getText());
                        int height = calculateImageHeight(c, width);
                        mTextFieldHeight.setText(""+height);
                        }
                    catch (NumberFormatException nfe) {
                        mTextFieldHeight.setText("");
                        }
                    }
                });
            }
        mTextFieldWidth.setColumns(6);
		mTextFieldHeight.setColumns(6);
		mainpanel.add(mTextFieldWidth, "3,3");
		mainpanel.add(mTextFieldHeight, "3,5");

		mComboBoxResolution = new JComboBox(DPI_LIST);
		mainpanel.add(mComboBoxResolution, "3,7");

	    ButtonGroup group = new ButtonGroup();
		mRadioButtonCopy = new JRadioButton("Copy image to clipboard", true);
		group.add(mRadioButtonCopy);
        mainpanel.add(mRadioButtonCopy, "1,9,3,9");

        mRadioButtonSaveAsPNG = new JRadioButton("Save image as PNG-file", false);
        group.add(mRadioButtonSaveAsPNG);
        mainpanel.add(mRadioButtonSaveAsPNG, "1,11,3,11");

        mRadioButtonSaveAsSVG = new JRadioButton("Save image as SVG-file", false);
        group.add(mRadioButtonSaveAsSVG);
        if (!supportsSVG(c))
            mRadioButtonSaveAsSVG.setEnabled(false);
        mainpanel.add(mRadioButtonSaveAsSVG, "1,13,3,13");

		JPanel buttonpanel = new JPanel();
		buttonpanel.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));
		buttonpanel.setLayout(new BorderLayout());
		JPanel ibp = new JPanel();
		ibp.setLayout(new GridLayout(1, 2, 8, 0));
		JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(this);
		ibp.add(bcancel);
		JButton bok = new JButton("OK");
		bok.addActionListener(this);
		ibp.add(bok);
		buttonpanel.add(ibp, BorderLayout.EAST);

		getContentPane().add(mainpanel, BorderLayout.CENTER);
		getContentPane().add(buttonpanel, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(bok);

		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
    	}

    /**
     * Override this if you have implemented SVG creation for this component
     * @return
     */
    public boolean supportsSVG(Component c) {
        return false;
        }

    /**
     * Override this if you have implemented SVG creation for this component
     */
    public void writeSVG(Component c, int width, int height, int fontScaling, Writer writer) throws Exception {
        }

    public void actionPerformed(ActionEvent e) {
	    if (e.getActionCommand().equals("OK")) {
			try {
			    int width = Integer.parseInt(mTextFieldWidth.getText());
			    int height = Integer.parseInt(mTextFieldHeight.getText());
			    if (mRadioButtonSaveAsSVG.isSelected()) {
			        File file = selectFile(FileHelper.cFileTypeSVG, "SVG image files");
			        if (file != null)
			            writeSVG(mComponent, width, height, FONT_FACTOR[mComboBoxResolution.getSelectedIndex()], new FileWriter(file));
			        }
			    else {
                    Image image = createComponentImage(mComponent, width, height, FONT_FACTOR[mComboBoxResolution.getSelectedIndex()]);
			        if (mRadioButtonSaveAsPNG.isSelected()) {
                        File file = selectFile(FileHelper.cFileTypePNG, "PNG image files");
                        if (file != null) {
                            try {
                                /* do something like this for setting the image to 300 dpi
                                PNGEncodeParam png = PNGEncodeParam.getDefaultEncodeParam((BufferedImage)image);
                                png.setPhysicalDimension(11812, 11812, 1);  // 11812 dots per meter = 300dpi
                                JAI.create("filestore", (BufferedImage)image, "analemma.png", "PNG");   */
                                javax.imageio.ImageIO.write((BufferedImage)image, "png", file);
                                }
                            catch (IOException ioe) {
                                JOptionPane.showMessageDialog(mParentFrame, "Couldn't write file.");
                                }
                            }
			            }
			        else {
			            ImageClipboardHandler.copyImage(image);
			            }
                    }
				}
			catch (NumberFormatException nfe) {
				JOptionPane.showMessageDialog(mParentFrame, "Width or height is not numerical.");
				return;
				}
			catch (Exception ex) {
				ex.printStackTrace();
				}
			catch (OutOfMemoryError ex) {
				JOptionPane.showMessageDialog(mParentFrame, "This exceeds your available memory. Try a smaller size.");
				return;
				}
	    	}

	    setVisible(false);
		dispose();
		}

    /**
     * Overwrite this for better quality and component specific handling
     */
    public Image createComponentImage(Component c, int width, int height, int fontScaling) {
        Image image = createImage(width, height);
        Graphics g = image.getGraphics();
        mComponent.paint(g);
        return image;
        }

    public Dimension getDefaultImageSize(Component c) {
        return new Dimension(1024, 768);
        }

    /**
     * This default implementation enforces quadratic images.
     * Overwrite this for special handling.
     */
    public int calculateImageHeight(Component c, int width) {
        return width;
        }

    private File selectFile(int filetype, String description) {
        CompoundFileFilter filter = new CompoundFileFilter();
        String[] extentions = FileHelper.getExtensions(filetype);
        for (String extension:extentions)
	        filter.addExtension(extension);
        filter.addDescription(description);

        JFileChooserOverwrite fileChooser = new JFileChooserOverwrite();
        fileChooser.setCurrentDirectory(FileHelper.getCurrentDirectory());
        fileChooser.setFileFilter(filter);
        fileChooser.setExtensions(extentions);
        int option = fileChooser.showSaveDialog(mParentFrame);
        FileHelper.setCurrentDirectory(fileChooser.getCurrentDirectory());
        return (option == JFileChooser.APPROVE_OPTION) ? fileChooser.getFile() : null;
        }
    }