/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-19 00:49:32 +0200 (dim., 19 juil. 2009) $
 * $Revision: 11245 $
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

/**
 *<p>
 * Implements flat circle drawing/filling routines.
 *</p>
 *
 * @author Miguel, miguel@jmol.org
 */
final class Circle3D {

  Graphics3D g3d;

  Circle3D(Graphics3D g3d) {
    this.g3d = g3d;
  }

  int xCenter, yCenter, zCenter;
  int sizeCorrection;

  void plotCircleCenteredClipped(int xCenter, int yCenter, int zCenter,
                                 int diameter) {
    if (g3d.isClippedXY(diameter, xCenter, yCenter))
      return;
    // halo only -- simple window clip
    int r = diameter / 2;
    this.sizeCorrection = 1 - (diameter & 1);
    this.xCenter = xCenter;
    this.yCenter = yCenter;
    this.zCenter = zCenter;
    int x = r;
    int y = 0;
    int xChange = 1 - 2 * r;
    int yChange = 1;
    int radiusError = 0;
    while (x >= y) {
      plot8CircleCenteredClipped(x, y);
      ++y;
      radiusError += yChange;
      yChange += 2;
      if (2 * radiusError + xChange > 0) {
        --x;
        radiusError += xChange;
        xChange += 2;
      }
    }
  }

  void plotCircleCenteredUnclipped(int xCenter, int yCenter, int zCenter,
                                   int diameter) {
    int r = diameter / 2;
    this. sizeCorrection = 1 - (diameter & 1);
    this.xCenter = xCenter;
    this.yCenter = yCenter;
    this.zCenter = zCenter;
    int x = r;
    int y = 0;
    int xChange = 1 - 2*r;
    int yChange = 1;
    int radiusError = 0;
    while (x >= y) {
      plot8CircleCenteredUnclipped(x, y);
      ++y;
      radiusError += yChange;
      yChange += 2;
      if (2*radiusError + xChange > 0) {
        --x;
        radiusError += xChange;
        xChange += 2;
      }
    }
  }

  private void plot8CircleCenteredClipped(int dx, int dy) {
    g3d.plotPixelClipped(xCenter+dx-sizeCorrection,
                          yCenter+dy-sizeCorrection, zCenter);
    g3d.plotPixelClipped(xCenter+dx-sizeCorrection, yCenter-dy, zCenter);
    g3d.plotPixelClipped(xCenter-dx, yCenter+dy-sizeCorrection, zCenter);
    g3d.plotPixelClipped(xCenter-dx, yCenter-dy, zCenter);

    g3d.plotPixelClipped(xCenter+dy-sizeCorrection,
                     yCenter+dx-sizeCorrection, zCenter);
    g3d.plotPixelClipped(xCenter+dy-sizeCorrection, yCenter-dx, zCenter);
    g3d.plotPixelClipped(xCenter-dy, yCenter+dx-sizeCorrection, zCenter);
    g3d.plotPixelClipped(xCenter-dy, yCenter-dx, zCenter);
  }

  private void plot8CircleCenteredUnclipped(int dx, int dy) {
    g3d.plotPixelUnclipped(xCenter+dx-sizeCorrection,
                            yCenter+dy-sizeCorrection, zCenter);
    g3d.plotPixelUnclipped(xCenter+dx-sizeCorrection, yCenter-dy, zCenter);
    g3d.plotPixelUnclipped(xCenter-dx, yCenter+dy-sizeCorrection, zCenter);
    g3d.plotPixelUnclipped(xCenter-dx, yCenter-dy, zCenter);

    g3d.plotPixelUnclipped(xCenter+dy-sizeCorrection,
                            yCenter+dx-sizeCorrection, zCenter);
    g3d.plotPixelUnclipped(xCenter+dy-sizeCorrection, yCenter-dx, zCenter);
    g3d.plotPixelUnclipped(xCenter-dy, yCenter+dx-sizeCorrection, zCenter);
    g3d.plotPixelUnclipped(xCenter-dy, yCenter-dx, zCenter);
  }

  private void plot8FilledCircleCenteredClipped(int dx, int dy) {
    g3d.plotPixelsClipped(2*dx+1-sizeCorrection,
                          xCenter-dx, yCenter+dy-sizeCorrection, zCenter);
    g3d.plotPixelsClipped(2*dx+1-sizeCorrection,
                          xCenter-dx, yCenter-dy, zCenter);
    g3d.plotPixelsClipped(2*dy+1-sizeCorrection,
                          xCenter-dy, yCenter+dx-sizeCorrection, zCenter);
    g3d.plotPixelsClipped(2*dy+1-sizeCorrection,
                          xCenter-dy, yCenter-dx, zCenter);
  }

  private void plot8FilledCircleCenteredUnclipped(int dx, int dy) {
    g3d.plotPixelsUnclipped(2*dx+1-sizeCorrection,
                            xCenter-dx, yCenter+dy-sizeCorrection, zCenter);
    g3d.plotPixelsUnclipped(2*dx+1-sizeCorrection,
                            xCenter-dx, yCenter-dy, zCenter);
    g3d.plotPixelsUnclipped(2*dy+1-sizeCorrection,
                            xCenter-dy, yCenter+dx-sizeCorrection, zCenter);
    g3d.plotPixelsUnclipped(2*dy+1-sizeCorrection,
                            xCenter-dy, yCenter-dx, zCenter);
  }

  void plotFilledCircleCenteredClipped(int xCenter, int yCenter, int zCenter,
                                       int diameter) {
    // for halo only
    int r = diameter / 2;
    this. sizeCorrection = 1 - (diameter & 1);
    this.xCenter = xCenter;
    this.yCenter = yCenter;
    this.zCenter = zCenter;
    int x = r;
    int y = 0;
    int xChange = 1 - 2*r;
    int yChange = 1;
    int radiusError = 0;
    while (x >= y) {
      plot8FilledCircleCenteredClipped(x, y);
      ++y;
      radiusError += yChange;
      yChange += 2;
      if (2*radiusError + xChange > 0) {
        --x;
        radiusError += xChange;
        xChange += 2;
      }
    }
  }

  void plotFilledCircleCenteredUnclipped(int xCenter, int yCenter, int zCenter,
                                       int diameter) {
    // for halo only
    int r = diameter / 2;
    this.xCenter = xCenter;
    this.yCenter = yCenter;
    this.zCenter = zCenter;
    int x = r;
    int y = 0;
    int xChange = 1 - 2*r;
    int yChange = 1;
    int radiusError = 0;
    while (x >= y) {
      plot8FilledCircleCenteredUnclipped(x, y);
      ++y;
      radiusError += yChange;
      yChange += 2;
      if (2*radiusError + xChange > 0) {
        --x;
        radiusError += xChange;
        xChange += 2;
      }
    }
  }
}

