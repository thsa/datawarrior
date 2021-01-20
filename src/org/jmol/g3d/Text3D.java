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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.image.PixelGrabber;
import java.util.Hashtable;

import org.jmol.api.JmolRendererInterface;


/**
 * implementation for text rendering
 *<p>
 * uses java fonts by rendering into an offscreen buffer.
 * strings are rasterized and stored as a bitmap in an int[].
 *<p>
 * needs work
 *
 *
 * @author Miguel, miguel@jmol.org
 */ 
public class Text3D {
  /*
    we have a few problems here
    a message is probably going to vary in size with z depth
    a message is probably going to be repeated by more than one atom
    fonts?
      just one?
      a restricted number?
      any font?
      if we want to support more than one then a fontindex is probably
      called for in order to prevent a hashtable lookup
    color - can be applied by the painter
    rep
      array of booleans - uncompressed
      array of bits - uncompressed - i like this
      some type of run-length, using bytes
  */
  private int height; // this height is just ascent + descent ... no reason for leading
  private int ascent;
  private int width;
  private int mapWidth;
  private int size;
  private int[] bitmap;
  private boolean isInvalid;
  
  public int getWidth() {
    return width;
  }
  
  public static int plot(int x, int y, int z, int argb, String text,
                         Font3D font3d, Graphics3D g3d,
                         JmolRendererInterface jmolRenderer, boolean antialias) {
    if (text.length() == 0)
      return 0;
    //System.out.println(x + "  " + y + " " + text);
    if (text.indexOf("<su") >= 0)
      return plotByCharacter(x, y, z, argb, text, font3d, g3d, jmolRenderer,
          antialias);
    int offset = font3d.fontMetrics.getAscent();
    //if (antialias)
      //offset += offset;
    y -= offset;

    //setColix has presumably been carried out for argb, and the two 
    //are assumed to be both the same -- translucent or not. 
    Text3D text3d = getText3D(x, y, g3d, text, font3d, antialias);
    if (text3d.isInvalid)
      return text3d.width;
    //TODO: text width/height are calculated 4x correct size here when antialiased.
    // this is wasteful, as it requires drawing larger than necessary images
    if (antialias && (argb & 0xC0C0C0) == 0) {
      // an interesting problem with antialiasing occurs if 
      // the label is black or almost black.
      argb = argb | 0x040404;
    }
    if (jmolRenderer != null
        || (x < 0 || x + text3d.width > g3d.width || y < 0 || y + text3d.height > g3d.height))
      plotClipped(x, y, z, argb, g3d, jmolRenderer, text3d.mapWidth,
          text3d.height, text3d.bitmap);
    else
      plotUnclipped(x, y, z, argb, g3d, text3d.mapWidth, text3d.height,
          text3d.bitmap);
    return text3d.width;
  }

  public static void plotImage(int x, int y, int z, Image image, Graphics3D g3d,
                               JmolRendererInterface jmolRenderer, boolean antialias,
                               int argbBackground, int width, int height) {
    boolean isBackground = (x == Integer.MIN_VALUE); 
    int width0 = image.getWidth(null);
    int height0 = image.getHeight(null);
    if (isBackground) {
      x = 0;
      z = Integer.MAX_VALUE - 1;
      width = g3d.width;
      height = g3d.height;
    }
    if (x + width <= 0 || x >= g3d.width || y + height <= 0 || y >= g3d.height)
      return;    
    g3d.platform.checkOffscreenSize(width, height);
    Graphics g = g3d.platform.gOffscreen;
    g.clearRect(0,0,width,height);
    g.drawImage(image, 0, 0, width, height, 0, 0, width0, height0, null);
    PixelGrabber pixelGrabber = new PixelGrabber(g3d.platform.imageOffscreen, 0, 0,
        width, height, true);
    try {
      pixelGrabber.grabPixels();
    } catch (InterruptedException e) {
      // impossible?
      return;
    }
    int[] buffer = (int[]) pixelGrabber.getPixels();
    int bgcolor = (isBackground ? 0 : argbBackground == 0 ? buffer[0] : argbBackground);
    if (jmolRenderer != null
        || (x < 0 || x + width > g3d.width || y < 0 || y + height > g3d.height))
      plotImageClipped(x, y, z, g3d, jmolRenderer, width, height, buffer, bgcolor);
    else
      plotImageUnClipped(x, y, z, g3d, width, height, buffer, bgcolor);
    return;
  }

  private static void plotImageClipped(int x, int y, int z, Graphics3D g3d,
                                       JmolRendererInterface jmolRenderer,
                                       int width, int height,
                                       int[] buffer, int bgcolor) {
    if (jmolRenderer == null)
      jmolRenderer = g3d;
    for (int i = 0, offset = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        int argb = buffer[offset++];
        if (argb != bgcolor && (argb & 0xFF000000) == 0xFF000000)
          jmolRenderer.plotPixelClippedNoSlab(argb, x + j, y + i, z);
      }
    }
  }

       private static void plotImageUnClipped(int x, int y, int z, Graphics3D g3d,
                                         int textWidth, int textHeight,
                                         int[] buffer, int bgcolor) {
    int[] zbuf = g3d.zbuf;
    int renderWidth = g3d.width;
    int pbufOffset = y * renderWidth + x;
    int i = 0;
    int j = 0;
    int offset = 0;
    while (i < textHeight) {
      while (j < textWidth) {
        if (z < zbuf[pbufOffset]) {
          int argb = buffer[offset];
          if (argb != bgcolor && (argb & 0xFF000000) == 0xFF000000)
            g3d.addPixel(pbufOffset, z, argb);
        }
        ++offset;
        ++j;
        ++pbufOffset;
      }
      ++i;
      j -= textWidth;
      pbufOffset += (renderWidth - textWidth);
    }
  }

  private static int plotByCharacter(int x, int y, int z, int argb, 
                                      String text, Font3D font3d, 
                                      Graphics3D g3d, JmolRendererInterface jmolRenderer,
                                      boolean antialias) {
    //int subscale = 1; //could be something less than that
    int w = 0;
    int len = text.length();
    int suboffset = (int)(font3d.fontMetrics.getHeight() * 0.25);
    int supoffset = -(int)(font3d.fontMetrics.getHeight() * 0.3);
    for (int i = 0; i < len; i++) {
      if (text.charAt(i) == '<') {
        if (i + 4 < len && text.substring(i, i + 5).equals("<sub>")) {
          i += 4;
          y += suboffset;
          continue;
        }
        if (i + 4 < len && text.substring(i, i + 5).equals("<sup>")) {
          i += 4;
          y += supoffset;
          continue;
        }
        if (i + 5 < len  && text.substring(i, i + 6).equals("</sub>")) {
          i += 5;
          y -= suboffset;
          continue;
        }
        if (i + 5 < len  && text.substring(i, i + 6).equals("</sup>")) {
          i += 5;
          y -= supoffset;
          continue;
        }
      }
      int width = plot(x + w, y, z, argb, text.substring(i, i + 1), font3d, 
          g3d, jmolRenderer, antialias);
      w += width;
    }
    //System.out.println("w=" + w);
    return w;
  }
  
  private static void plotUnclipped(int x, int y, int z, int argb,
                                    Graphics3D g3d, int textWidth,
                                    int textHeight, int[] bitmap) {
    int offset = 0;
    int shiftregister = 0;
    int i = 0, j = 0;
    int[] zbuf = g3d.zbuf;
    int renderWidth = g3d.width;
    int pbufOffset = y * renderWidth + x;
    while (i < textHeight) {
      while (j < textWidth) {
        if ((offset & 31) == 0)
          shiftregister = bitmap[offset >> 5];
        if (shiftregister == 0) {
          int skip = 32 - (offset & 31);
          j += skip;
          offset += skip;
          pbufOffset += skip;
          continue;
        }
        if (shiftregister < 0 && z < zbuf[pbufOffset])
          g3d.addPixel(pbufOffset, z, argb);
        shiftregister <<= 1;
        ++offset;
        ++j;
        ++pbufOffset;
      }
      while (j >= textWidth) {
        ++i;
        j -= textWidth;
        pbufOffset += (renderWidth - textWidth);
      }
    }
  }
  
  private static void plotClipped(int x, int y, int z, int argb,
                                  Graphics3D g3d,
                                  JmolRendererInterface jmolRenderer,
                                  int textWidth, int textHeight, int[] bitmap) {
    if (jmolRenderer == null)
      jmolRenderer = g3d;
    int offset = 0;
    int shiftregister = 0;
    int i = 0, j = 0;
    while (i < textHeight) {
      while (j < textWidth) {
        if ((offset & 31) == 0)
          shiftregister = bitmap[offset >> 5];
        if (shiftregister == 0) {
          int skip = 32 - (offset & 31);
          j += skip;
          offset += skip;
          continue;
        }
        if (shiftregister < 0)
          jmolRenderer.plotPixelClippedNoSlab(argb, x + j, y + i, z);
        shiftregister <<= 1;
        ++offset;
        ++j;
      }
      while (j >= textWidth) {
        ++i;
        j -= textWidth;
      }
    }
  }

  private Text3D(String text, Font3D font3d,
                 boolean antialias) {
    FontMetrics fontMetrics = font3d.fontMetrics;
    ascent = fontMetrics.getAscent();
    height = ascent + fontMetrics.getDescent();
    width = fontMetrics.stringWidth(text);
    if (width == 0)
      return;
    //System.out.println(text + " " + antialias + " "  + ascent + " " + height + " " + width );
    mapWidth = width;
    size = mapWidth * height;
  }

  private void renderOffscreen(String text, Font3D font3d, Platform3D platform,
                               boolean antialias) {
    Graphics g = platform.gOffscreen;
    g.setColor(Color.black);
    g.fillRect(0, 0, mapWidth, height);
    g.setColor(Color.white);
    g.setFont(font3d.font);
    g.drawString(text, 0, ascent);
  }

  private void rasterize(Platform3D platform, boolean antialias) {
    
    PixelGrabber pixelGrabber = new PixelGrabber(platform.imageOffscreen, 0, 0, 
                                                 mapWidth, height, true);
    try {
      pixelGrabber.grabPixels();
    } catch (InterruptedException e) {
      // impossible?
      return;
    }
    int pixels[] = (int[])pixelGrabber.getPixels();

    int bitmapSize = (size + 31) >> 5;
    bitmap = new int[bitmapSize];

    int offset, shifter;
    for (offset = shifter = 0; offset < size; ++offset, shifter <<= 1) {
      if ((pixels[offset] & 0x00FFFFFF) != 0)
        shifter |= 1;
      if ((offset & 31) == 31)
        bitmap[offset >> 5] = shifter;
    }
    if ((offset & 31) != 0) {
      shifter <<= 31 - (offset & 31);
      bitmap[offset >> 5] = shifter;
    }
    /*      // error checking
      // shifter error checking
      boolean[] bits = new boolean[size];
      for (int i = 0; i < size; ++i)
        bits[i] = (pixels[i] & 0x00FFFFFF) != 0;
      //
      for (offset = 0; offset < size; ++offset, shifter <<= 1) {
        if ((offset & 31) == 0)
          shifter = bitmap[offset >> 5];
        if (shifter < 0) {
          if (!bits[offset]) {
            Logger.debug("false positive @" + offset);
            Logger.debug("size = " + size);
          }
        } else {
          if (bits[offset]) {
            Logger.debug("false negative @" + offset);
            Logger.debug("size = " + size);
          }
        }
      }
      // error checking
    */
  }

  private final static Hashtable htFont3d = new Hashtable();
  private final static Hashtable htFont3dAntialias = new Hashtable();
  private static boolean working;
  
  public synchronized static void clearFontCache() {
    if (working)
      return;
    htFont3d.clear();
    htFont3dAntialias.clear();
  }
  
  // FIXME mth
  // we have a synchronization issue/race condition  here with multiple
  // so only one Text3D can be generated at a time

  // Jmol 11.7.8: caching and rasterization only carried out if the font is
  // valid -- that is in the rectangle to be plotted.
  
  private synchronized static Text3D getText3D(int x, int y, Graphics3D g3d,
                                               String text, Font3D font3d,
                                               boolean antialias) {
    working = true;
    Hashtable ht = (antialias ? htFont3dAntialias : htFont3d);
    Hashtable htForThisFont = (Hashtable) ht.get(font3d);
    Text3D text3d = null;
    boolean newFont = false;
    boolean newText = false;
    if (htForThisFont != null) {
      text3d = (Text3D) htForThisFont.get(text);
    } else {
      htForThisFont = new Hashtable();
      newFont = true;
    }
    if (text3d == null) {
      text3d = new Text3D(text, font3d, antialias);
      newText = true;
    }
    text3d.isInvalid = (text3d.width == 0 || x + text3d.width <= 0
        || x >= g3d.width || y + text3d.height <= 0 || y >= g3d.height);
    if (text3d.isInvalid)
      return text3d;
    if (newFont)
      ht.put(font3d, htForThisFont);
    if (newText) {
      //System.out.println(text + " " + x + " " + text3d.width + " " + g3d.width + " " + y + " " + g3d.height);
      text3d.setBitmap(text, font3d, g3d.platform, antialias);
      htForThisFont.put(text, text3d);
    }
    working = false;
    return text3d;
  }

  private void setBitmap(String text, Font3D font3d, Platform3D platform, boolean antialias) {
    //System.out.println(text + " height=" + height + " setBitmap width= " + width);
    platform.checkOffscreenSize(mapWidth, height);
    renderOffscreen(text, font3d, platform, antialias);
    rasterize(platform, antialias);
  }

}
