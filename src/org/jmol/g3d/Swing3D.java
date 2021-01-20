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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Image;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.SinglePixelPackedSampleModel;

/**
 *<p>
 * Implementation of Platform3D when using Swing on JVMs >= 1.2
 *</p>
 *<p>
 * Uses the BufferedImage classe to turn an int[] into an
 * Image that can be drawn.
 *</p>
 *<p>
 * This is used by everything except
 * MSFT Internet Explorer with the MSFT JVM,
 * and Netscape 4.* on both Win32 and MacOS 9.
 *</p>
 *
 * @author Miguel, miguel@jmol.org
 */
  
final class Swing3D extends Platform3D {

  private final static DirectColorModel rgbColorModel =
    new DirectColorModel(24, 0x00FF0000, 0x0000FF00, 0x000000FF, 0x00000000);

  private final static int[] sampleModelBitMasks =
  { 0x00FF0000, 0x0000FF00, 0x000000FF };

  private final static DirectColorModel rgbColorModelT =
    new DirectColorModel(32, 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000);

  private final static int[] sampleModelBitMasksT =
  { 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000 };



  Image allocateImage() {
    //backgroundTransparent not working with antialiasDisplay. I have no idea why. BH 9/24/08
    if (backgroundTransparent)
      return new BufferedImage(
          rgbColorModelT,
          Raster.createWritableRaster(
              new SinglePixelPackedSampleModel(
                  DataBuffer.TYPE_INT,
                  windowWidth,
                  windowHeight,
                  sampleModelBitMasksT), 
              new DataBufferInt(pBuffer, windowSize),
              null),
          false, 
          null);
    return new BufferedImage(
        rgbColorModel,
        Raster.createWritableRaster(
            new SinglePixelPackedSampleModel(
                DataBuffer.TYPE_INT,
                windowWidth,
                windowHeight,
                sampleModelBitMasks), 
            new DataBufferInt(pBuffer, windowSize),
            null),
        false, 
        null);
  }

  private static boolean backgroundTransparent = false;
  
  void setBackgroundTransparent(boolean tf) {
    backgroundTransparent = tf;
  }

  Image allocateOffscreenImage(int width, int height) {
    return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
  }

  Graphics getGraphics(Image image) {
    return getStaticGraphics(image);
  }
  
  static Graphics getStaticGraphics(Image image) {
    Graphics2D g2d = ((BufferedImage) image).createGraphics();
    if (backgroundTransparent) {
      // what here?
    }
    // miguel 20041122
    // we need to turn off text antialiasing on OSX when
    // running in a web browser
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                         RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    // I don't know if we need these or not, but cannot hurt to have them
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                         RenderingHints.VALUE_ANTIALIAS_OFF);
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                         RenderingHints.VALUE_RENDER_SPEED);
    return g2d;
  }
}
