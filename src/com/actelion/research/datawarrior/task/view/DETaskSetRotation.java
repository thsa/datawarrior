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

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization3D;
import com.actelion.research.table.view.VisualizationPanel3D;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Properties;


public class DETaskSetRotation extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Rotation State";

	private static final String PROPERTY_ANGLES = "angles";

	private JTextField[] mTextField;
	
	public DETaskSetRotation(Frame owner, DEMainPane mainPane, VisualizationPanel3D view) {
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
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.FILL, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap} };
		JPanel cp = new JPanel();
		cp.setLayout(new TableLayout(size));

		cp.add(new JLabel("Euler angles (radians):"), "1,2,3,2");

		mTextField = new JTextField[3];
		for (int i=0; i<3; i++) {
			cp.add(new JLabel(Integer.toString(i+1).concat(".")), "1,".concat(Integer.toString(4+i*2)));
			mTextField[i] = new JTextField(6);
			cp.add(mTextField[i], "3,".concat(Integer.toString(4+i*2)));
			}

		try {
			final BufferedImage eulerImage = ImageIO.read(new BufferedInputStream(getClass().getResourceAsStream("/images/euler.jpeg")));
			cp.add(new JPanel() {
				private static final long serialVersionUID = 0x20131016;
	
				@Override
	            protected void paintComponent(Graphics g) {
	                super.paintComponent(g);
	                g.drawImage(eulerImage, (getWidth()-eulerImage.getWidth())/2, (getHeight()-eulerImage.getHeight())/2, this);
	            	}
	
	            @Override
	            public Dimension getPreferredSize() {
	                return new Dimension(eulerImage.getWidth(), eulerImage.getHeight());
	            	}
	 			}, "5,1,5,8");
			}
		catch (IOException ioe) {}

		return cp;
	    }

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel3D) ? null : "The rotation state can be set on 3D-views only.";
		}

	@Override
	public void setDialogToDefault() {
		for (int i=0; i<3; i++)
			mTextField[i].setText("0.0");
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		String[] angle = configuration.getProperty(PROPERTY_ANGLES, "0;0;0").split(";");
		for (int i=0; i<3; i++)
			mTextField[i].setText(angle[i]);
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		StringBuilder sb = new StringBuilder(mTextField[0].getText());
		for (int i=1; i<3; i++) {
			sb.append(';');
			sb.append(mTextField[i].getText());
			}
		configuration.setProperty(PROPERTY_ANGLES, sb.toString());
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		JVisualization3D visualization = (JVisualization3D)((VisualizationPanel3D)view).getVisualization();
		float[][] rotation = visualization.getRotationMatrix();
		double[] angle = toEulerAngles(rotation);
		
		StringBuilder sb = new StringBuilder(DoubleFormat.toString(angle[0], 5));
		for (int i=1; i<3; i++) {
			sb.append(';');
			sb.append(DoubleFormat.toString(angle[i]));
			}
		configuration.setProperty(PROPERTY_ANGLES, sb.toString());
		}

	/**
	 * From Wikipedia
	 * @param m
	 * @return
	 *
	public double[] toEulerAngles(float[][] m) {
		double[] angle = new double[3];
		double theta = Math.acos(0.5*(m[0][0]+m[1][1]+m[2][2]-1));
		double f = 2 * Math.sin(theta);
		angle[0] = (m[2][1] - m[1][2]) / f;
		angle[1] = (m[0][2] - m[2][0]) / f;
		angle[2] = (m[1][0] - m[0][1]) / f;
		return angle;
		}*/

	/** this conversion uses conventions as described on page:
	*   http://www.euclideanspace.com/maths/geometry/rotations/euler/index.htm
	*   Coordinate System: right hand
	*   Positive angle: right hand
	*   Order of euler angles: heading first, then attitude, then bank
	*   matrix row column ordering:
	*   [m00 m01 m02]
	*   [m10 m11 m12]
	*   [m20 m21 m22]*/
	public double[] toEulerAngles(float[][] m) {
		double[] angle = new double[3];
	    // Assuming the angles are in radians.
		if (m[1][0] > 0.998) { // singularity at north pole
			angle[0] = Math.atan2(m[0][2],m[2][2]);
			angle[1] = Math.PI/2;
			angle[2] = 0;
			}
		else if (m[1][0] < -0.998) { // singularity at south pole
			angle[0] = Math.atan2(m[0][2],m[2][2]);
			angle[1] = -Math.PI/2;
			angle[2] = 0;
			}
		else {
			angle[0] = Math.atan2(-m[2][0],m[0][0]);
			angle[2] = Math.atan2(-m[1][2],m[1][1]);
			angle[1] = Math.asin(m[1][0]);
//			angle[1] = Math.atan2(-m[1][2],m[1][1]);	// seems that angles 1 and 2 were confused in source; TLS 25 Sep 2015
//			angle[2] = Math.asin(m[1][0]);
			}
		return angle;
		}

	/** this conversion uses NASA standard aeroplane conventions as described on page:
	*   http://www.euclideanspace.com/maths/geometry/rotations/euler/index.htm
	*   Coordinate System: right hand
	*   Positive angle: right hand
	*   Order of euler angles: heading first, then attitude, then bank
	*   matrix row column ordering:
	*   [m00 m01 m02]
	*   [m10 m11 m12]
	*   [m20 m21 m22]*/
	public float[][] toRotationMatrix(double[] angle) {
	    // Assuming the angles are in radians.
	    float ch = (float)Math.cos(angle[0]);
	    float sh = (float)Math.sin(angle[0]);
	    float ca = (float)Math.cos(angle[1]);
	    float sa = (float)Math.sin(angle[1]);
	    float cb = (float)Math.cos(angle[2]);
	    float sb = (float)Math.sin(angle[2]);

	    float[][] m = new float[3][3];
	    m[0][0] = ch * ca;
	    m[0][1] = sh*sb - ch*sa*cb;
	    m[0][2] = ch*sa*sb + sh*cb;
	    m[1][0] = sa;
	    m[1][1] = ca*cb;
	    m[1][2] = -ca*sb;
	    m[2][0] = -sh*ca;
	    m[2][1] = sh*sa*cb + ch*sb;
	    m[2][2] = -sh*sa*sb + ch*cb;
	    return m;
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		String[] angle = configuration.getProperty(PROPERTY_ANGLES, "0;0;0").split(";");
		for (int i=0; i<3; i++) {
			if (i < angle.length) {
				try {
					Double.parseDouble(angle[i]);
					}
				catch (NumberFormatException nfe) {
					showErrorMessage("Some Euler angles are not numeric.");
					return false;
					}
				}
			else {
				showErrorMessage("Some Euler angles are missing.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void enableItems() {
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (view instanceof VisualizationPanel3D) {
			String[] text = configuration.getProperty(PROPERTY_ANGLES, "0;0;0").split(";");
			double[] angle = new double[3];
			for (int i = 0; i<3; i++)
				angle[i] = Double.parseDouble(text[i]);
			((JVisualization3D)(((VisualizationPanel3D)view).getVisualization())).setRotationMatrix(toRotationMatrix(angle));
			}
		}
	}
