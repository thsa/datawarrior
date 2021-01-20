/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-03-28 16:42:36 +0100 (sam., 28 mars 2009) $
 * $Revision: 10745 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.g3d;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.Image;
import java.awt.image.ImageProducer;
import java.awt.image.ImageConsumer;
import java.awt.image.ColorModel;

/**
 *<p>
 * Implementation of Platform3D when using AWT on 1.1 JVMs.
 *</p>
 *<p>
 * Uses the AWT imaging routines to convert an int[] of ARGB values
 * into an Image by implementing the ImageProducer interface.
 *</p>
 *<p>
 * This is used by MSFT Internet Explorer with the MSFT JVM,
 * and Netscape 4.* on both Win32 and MacOS 9.
 *</p>
 *
 * @author Miguel, miguel@jmol.org
 */
final class Awt3D extends Platform3D implements ImageProducer {

  Component component;

  ColorModel colorModelRGB;
  ImageConsumer ic;

  Awt3D(Component component) {
    this.component = component;
    colorModelRGB = Toolkit.getDefaultToolkit().getColorModel();
  }

  Image allocateImage() {
    return component.createImage(this);
  }

  void notifyEndOfRendering() {
    if (this.ic != null)
      startProduction(ic);
  }

  Image allocateOffscreenImage(int width, int height) {
    //Logger.debug("allocateOffscreenImage(" + width + "," + height + ")");
    Image img = component.createImage(width, height);
    //Logger.debug("img=" + img);
    return img;
  }

  Graphics getGraphics(Image image) {
    return image.getGraphics();
  }

  public synchronized void addConsumer(ImageConsumer ic) {
    startProduction(ic);
  }

  public boolean isConsumer(ImageConsumer ic) {
    return (this.ic == ic);
  }

  public void removeConsumer(ImageConsumer ic) {
    if (this.ic == ic)
      this.ic = null;
  }

  public void requestTopDownLeftRightResend(ImageConsumer ic) {
  }

  public void startProduction(ImageConsumer ic) {
    if (this.ic != ic) {
      this.ic = ic;
      ic.setDimensions(windowWidth, windowHeight);
      ic.setHints(ImageConsumer.TOPDOWNLEFTRIGHT |
                  ImageConsumer.COMPLETESCANLINES |
                  ImageConsumer.SINGLEPASS);
    }
    ic.setPixels(0, 0, windowWidth, windowHeight, colorModelRGB,
                 pBuffer, 0, windowWidth);
    ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
  }
}
