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

import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.IDCodeParserWithoutCoordinateInvention;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.util.ArrayUtils;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point3D;
import javafx.scene.image.WritableImage;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class JStructure3DFormObject extends AbstractFormObject {
	public static final String FORM_OBJECT_TYPE = "structure3D";

	private StereoMolecule mOverlayMol,mCavityMol,mLigandMol;

	public JStructure3DFormObject(String key, String type) {
		super(key, type);
		mComponent = new JFXConformerPanel(false, false, false);
		}

	public void setReferenceMolecule(StereoMolecule refMol) {
		((JFXConformerPanel)mComponent).setOverlayMolecule(refMol);
		mOverlayMol = refMol;
		}

	public void setCavityMolecule(StereoMolecule cavityMol, StereoMolecule ligandMol) {
		((JFXConformerPanel)mComponent).setProteinCavity(cavityMol, ligandMol, true);
		mCavityMol = cavityMol;
		mLigandMol = ligandMol;
		}

	@Override
	public Object getData() {
		ArrayList<StereoMolecule> molList = ((JFXConformerPanel)mComponent).getMolecules(null);
		return (molList.isEmpty()) ? null : molList.get(0);
		}

    @Override
	public void setData(Object data) {
		if (data == null) {
			((JFXConformerPanel) mComponent).clear();
			}
		else if (data instanceof StereoMolecule) {
			((JFXConformerPanel)mComponent).clear();
			((JFXConformerPanel)mComponent).addMolecule((StereoMolecule)data, null, null);
			if (mCavityMol == null)
				((JFXConformerPanel)mComponent).optimizeView();
			}
		else if (data instanceof String) {
			((JFXConformerPanel)mComponent).clear();

			byte[] idcode = ((String)data).getBytes();
			int index = ArrayUtils.indexOf(idcode, (byte)9);

			if (index != -1 && idcode.length > index+2) {
				StereoMolecule mol = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(idcode, idcode, 0, index+1);
				index = ArrayUtils.indexOf(idcode, (byte)32, index+1);
				if (index == -1) {
					((JFXConformerPanel)mComponent).addMolecule(mol, null, null);
					}
				else {
					int count = 2;
					for (int i=index+1; i<idcode.length; i++)
						if (idcode[i] == (byte)32)
							count++;

					Point3D cor = new Point3D(0,0,0);
					for (int i=0; i<count; i++) {
						javafx.scene.paint.Color color = javafx.scene.paint.Color.hsb(360f * i / count, 0.75, 0.6);
						((JFXConformerPanel)mComponent).addMolecule(mol, color, cor);
						mol = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(idcode, idcode, 0, index+1);
						index = ArrayUtils.indexOf(idcode, (byte)32, index+1);
						}
					}
				if (mCavityMol == null)
					((JFXConformerPanel)mComponent).optimizeView();
				}
			}
		}

    @Override
	public int getDefaultRelativeHeight() {
		return 4;
		}

    @Override
	public void printContent(Graphics2D g2D, Rectangle2D.Double r, float scale, Object data, boolean isMultipleRows) {
	    if (data != null && r.width > 1 && r.height > 1) {
		    StereoMolecule[] mols = null;
		    if (data instanceof StereoMolecule) {
		    	mols = new StereoMolecule[1];
		        mols[0] = (StereoMolecule)data;
		    	}
		    else if (data instanceof String) {
				String idcode = ((String)data);
				int index = ((String)data).indexOf('\t');
				String coords = (index == -1) ? null : idcode.substring(index+1);

			    if (index == -1) {
				    mols = new StereoMolecule[1];
				    mols[0] = new IDCodeParser().getCompactMolecule(idcode, null);
				    }
				else {
				    byte[] bytes = idcode.getBytes();
				    int count = 1;
				    for (int i=index+1; i<bytes.length; i++)
					    if (bytes[i] == 32)
						    count++;
				    mols = new StereoMolecule[count];
				    for (int i=0; i<count; i++) {
					    mols[i] = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(bytes, bytes, 0, index+1);
					    index = ArrayUtils.indexOf(bytes, (byte)32, index+1);
					    }
				    }
		    	}

		    if (mols != null) {
				JFXConformerPanel fxp = new JFXConformerPanel(false, (int)(4*r.width), (int)(4*r.height), true, false);
			    fxp.waitForCompleteConstruction();
				fxp.setBackground(Color.WHITE);
				if (mOverlayMol != null)
					fxp.setOverlayMolecule(mOverlayMol);
			    if (mCavityMol != null)
				    fxp.setProteinCavity(mCavityMol, mLigandMol, false);

			    final CountDownLatch latch = new CountDownLatch(1);
			    final StereoMolecule[] mols_ = mols;
			    Platform.runLater(() -> {
				    fxp.updateConformers(mols_, -1, null);
				    latch.countDown();
			    } );
			    try { latch.await(); } catch (InterruptedException ie) {}

				fxp.getV3DScene().getWorld().setTransform(((JFXConformerPanel)mComponent).getV3DScene().getWorld().getRotation());
				WritableImage image = fxp.getContentImage();
				if (image != null)
					g2D.drawImage(SwingFXUtils.fromFXImage(image, null), (int)(r.x), (int)(r.y),
							(int)(r.x+r.width), (int)(r.y+r.height), 0, 0, (int)(4*r.width), (int)(4*r.height), null);
		    	}
	    	}
		}
	}
