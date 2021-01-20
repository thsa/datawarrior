/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-19 00:49:32 +0200 (dim., 19 juil. 2009) $
 * $Revision: 11245 $
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
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
 * Draws shaded cylinders in 3D.
 *</p>
 *<p>
 * Cylinders are used to draw bonds.
 *</p>
 *
 * @author Miguel, miguel@jmol.org
 */
class Cylinder3D {

  final Graphics3D g3d;
  final Line3D line3d;

  Cylinder3D(Graphics3D g3d) {
    this.g3d = g3d;
    line3d = g3d.line3d;
  }

  private short colixA, colixB;
  private int[] shadesA;
  private boolean isScreenedA;
  private int[] shadesB;
  private boolean isScreenedB;
  private int xA, yA, zA;
  private int dxB, dyB, dzB;
  private float xAf, yAf, zAf;
  private float dxBf, dyBf, dzBf;
  private boolean tEvenDiameter;
  //private int evenCorrection;
  private int diameter;
  private byte endcaps;
  private boolean tEndcapOpen;
  private int xEndcap, yEndcap, zEndcap;
  private int argbEndcap;
  private short colixEndcap;
  private int intensityEndcap;

  private float radius, radius2, cosTheta, cosPhi, sinPhi;

  boolean clipped;
  private boolean drawBackside;

  private int zShift;

  void render(short colixA, short colixB, boolean isScreenedA,
              boolean isScreenedB, byte endcaps, int diameter, int xA, int yA,
              int zA, int xB, int yB, int zB) {
    //0 for colixA or colixB means ignore for this pass
    if (diameter > g3d.height * 3)
      return;
    this.isScreenedA = isScreenedA;
    this.isScreenedB = isScreenedB;
    int r = diameter / 2 + 1;
    int codeMinA = g3d.clipCode(xA - r, yA - r, zA - r);
    int codeMaxA = g3d.clipCode(xA + r, yA + r, zA + r);
    int codeMinB = g3d.clipCode(xB - r, yB - r, zB - r);
    int codeMaxB = g3d.clipCode(xB + r, yB + r, zB + r);
    //all bits 0 --> no clipping
    clipped = ((codeMinA | codeMaxA | codeMinB | codeMaxB) != 0);
    //any two bits same in all cases --> fully clipped
    if ((codeMinA & codeMaxB & codeMaxA & codeMinB) != 0)
      return; // fully clipped;
    dxB = xB - xA;
    dyB = yB - yA;
    dzB = zB - zA;
    zShift = g3d.getZShift((zA + zB) >> 1);

    if (diameter <= 1) {
      line3d.plotLineDelta(g3d.getColixArgb(colixA), isScreenedA, g3d
          .getColixArgb(colixB), isScreenedB, xA, yA, zA, dxB, dyB, dzB,
          clipped);
      return;
    }
    drawBackside = (clipped 
        || endcaps == Graphics3D.ENDCAPS_FLAT || endcaps == Graphics3D.ENDCAPS_NONE);
    this.diameter = diameter;
    this.xA = xA;
    this.yA = yA;
    this.zA = zA;
    this.endcaps = endcaps;
    shadesA = g3d.getShades(this.colixA = colixA);
    shadesB = g3d.getShades(this.colixB = colixB);
    calcArgbEndcap(true, false);

    generateBaseEllipse();

    if (endcaps == Graphics3D.ENDCAPS_FLAT || endcaps == Graphics3D.ENDCAPS_OPENEND)
      renderFlatEndcap(true);
    g3d.setZMargin(5);
    for (int i = rasterCount; --i >= 0;)
      plotRaster(i);
    g3d.setZMargin(0);
    if (endcaps == Graphics3D.ENDCAPS_SPHERICAL)
      renderSphericalEndcaps();
  }

  void renderBits(short colixA, short colixB, boolean isScreenedA,
                  boolean isScreenedB, byte endcaps, int diameter, float xA,
                  float yA, float zA, float xB, float yB, float zB) {
    // dipole cross, cartoonRockets, draw mesh nofill or width = -1
    if (diameter > g3d.height * 3)
      return;
    this.isScreenedA = isScreenedA;
    this.isScreenedB = isScreenedB;
    
    // oops -- problem here if diameter < 0 is that we may have already clipped it!
    int r = diameter / 2 + 1;
    int codeMinA = g3d.clipCode((int) xA - r, (int) yA - r, (int) zA - r);
    int codeMaxA = g3d.clipCode((int) xA + r, (int) yA + r, (int) zA + r);
    int codeMinB = g3d.clipCode((int) xB - r, (int) yB - r, (int) zB - r);
    int codeMaxB = g3d.clipCode((int) xB + r, (int) yB + r, (int) zB + r);
    //all bits 0 --> no clipping
    clipped = ((codeMinA | codeMaxA | codeMinB | codeMaxB) != 0);
    //any two bits same in all cases --> fully clipped
    if ((codeMinA & codeMaxB & codeMaxA & codeMinB) != 0)
      return; // fully clipped;
    dxBf = xB - xA;
    dyBf = yB - yA;
    dzBf = zB - zA;
    if (diameter == 0 || diameter == 1) {
      line3d.plotLineDelta(g3d.getColixArgb(colixA), isScreenedA, g3d
          .getColixArgb(colixB), isScreenedB, (int) xA, (int) yA, (int) zA,
          (int) dxBf, (int) dyBf, (int) dzBf, clipped);
      return;
    }
    if (diameter > 0) {
      this.diameter = diameter;
      this.xAf = xA;
      this.yAf = yA;
      this.zAf = zA;
    }
    drawBackside = (!isScreenedA && !isScreenedB && (clipped 
        || endcaps == Graphics3D.ENDCAPS_FLAT || endcaps == Graphics3D.ENDCAPS_NONE));
    this.xA = (int) xAf;
    this.yA = (int) yAf;
    this.zA = (int) zAf;
    this.dxB = (int) dxBf;
    this.dyB = (int) dyBf;
    this.dzB = (int) dzBf;

    this.shadesA = g3d.getShades(this.colixA = colixA);
    this.shadesB = g3d.getShades(this.colixB = colixB);
    this.endcaps = endcaps;
    calcArgbEndcap(true, true);

    if (diameter > 0)
      generateBaseEllipsePrecisely();
    if (endcaps == Graphics3D.ENDCAPS_FLAT)
      renderFlatEndcapPrecisely(true);
    line3d.setLineBits(this.dxBf, this.dyBf);
    g3d.setZMargin(5);
    for (int i = rasterCount; --i >= 0;)
      plotRasterBits(i);
    g3d.setZMargin(0);
    if (endcaps == Graphics3D.ENDCAPS_SPHERICAL)
      renderSphericalEndcaps();
    this.xAf += dxBf;
    this.yAf += dyBf;
    this.zAf += dzBf;
  }

  private void plotRasterBits(int i) {
    int fpz = fp8IntensityUp[i] >> (8 + zShift);
    int fpzBack = fpz >> 1;
    int x = xRaster[i];
    int y = yRaster[i];
    int z = zRaster[i];
    if (tEndcapOpen && argbEndcap != 0) {
      if (clipped) {
        g3d.plotPixelClipped(argbEndcap, xEndcap + x, yEndcap + y, zEndcap - z
            - 1);
        g3d.plotPixelClipped(argbEndcap, xEndcap - x, yEndcap - y, zEndcap + z
            - 1);
      } else {
        g3d.plotPixelUnclipped(argbEndcap, xEndcap + x, yEndcap + y, zEndcap
            - z - 1);
        g3d.plotPixelUnclipped(argbEndcap, xEndcap - x, yEndcap - y, zEndcap
            + z - 1);
      }
    }
    line3d.plotLineDeltaBits(shadesA, isScreenedA, shadesB, isScreenedB,
        fpz, xA + x, yA + y, zA - z, dxB, dyB, dzB, clipped);
    if (drawBackside) {
      line3d.plotLineDelta(shadesA[fpzBack], isScreenedA, shadesB[fpzBack], isScreenedB, xA
          - x, yA - y, zA + z, dxB, dyB, dzB, clipped);
    }
  }

  float xTip, yTip, zTip;

  void renderCone(short colix, boolean isScreened, byte endcap, int diameter, float xA, float yA,
                  float zA, float xTip, float yTip, float zTip, boolean doFill) {
    if (diameter > g3d.height * 3)
      return;
    dxBf = (xTip) - (xAf = xA);
    dyBf = (yTip) - (yAf = yA);
    dzBf = (zTip) - (zAf = zA);
    this.xA = (int) Math.floor(xAf);
    this.yA = (int) Math.floor(yAf);
    this.zA = (int) Math.floor(zAf);
    this.dxB = (int) Math.floor(dxBf);
    this.dyB = (int) Math.floor(dyBf);
    this.dzB = (int) Math.floor(dzBf);
    this.xTip = xTip;
    this.yTip = yTip;
    this.zTip = zTip;
    colixA = colix;
    this.isScreenedA = isScreened;
    shadesA = g3d.getShades(colix);
    int intensityTip = Shade3D.calcIntensity(dxB, dyB, -dzB);
    g3d.plotPixelClipped(shadesA[intensityTip], isScreenedA, (int) xTip,
        (int) yTip, (int) zTip);

    this.diameter = diameter;
    if (diameter <= 1) {
      if (diameter == 1)
        line3d.plotLineDelta(colixA, isScreenedA, colixA, isScreenedA, this.xA,
            this.yA, this.zA, dxB, dyB, dzB, clipped);
      return;
    }
    //float r2 = dxB*dxB + dyB*dyB + dzB*dzB;
    //System.out.println(r2);
    this.endcaps = endcap;
    calcArgbEndcap(false, true);
    generateBaseEllipsePrecisely();
    if (endcaps == Graphics3D.ENDCAPS_FLAT)
      renderFlatEndcapPrecisely(false);
    g3d.setZMargin(5);
    for (int i = rasterCount; --i >= 0;)
      plotRasterCone(i, doFill);
    g3d.setZMargin(0);
  }

  private void generateBaseEllipse() {
    tEvenDiameter = (diameter & 1) == 0;
    //Logger.debug("diameter=" + diameter);
    radius = diameter / 2.0f;
    radius2 = radius * radius;
    int mag2d2 = dxB * dxB + dyB * dyB;
    if (mag2d2 == 0) {
      cosTheta = 1;
      cosPhi = 1;
      sinPhi = 0;
    } else {
      float mag2d = (float) Math.sqrt(mag2d2);
      float mag3d = (float) Math.sqrt(mag2d2 + dzB * dzB);
      cosTheta = dzB / mag3d;
      cosPhi = dxB / mag2d;
      sinPhi = dyB / mag2d;
    }

    calcRotatedPoint(0f, 0, false);
    calcRotatedPoint(0.5f, 1, false);
    calcRotatedPoint(1f, 2, false);
    rasterCount = 3;
    interpolate(0, 1);
    interpolate(1, 2);
  }

  private void generateBaseEllipsePrecisely() {
    tEvenDiameter = (diameter & 1) == 0;
    //Logger.debug("diameter=" + diameter);
    radius = diameter / 2.0f;
    radius2 = radius * radius;
    float mag2d2 = dxBf * dxBf + dyBf * dyBf;
    if (mag2d2 == 0) {
      cosTheta = 1;
      cosPhi = 1;
      sinPhi = 0;
    } else {
      float mag2d = (float) Math.sqrt(mag2d2);
      float mag3d = (float) Math.sqrt(mag2d2 + dzBf * dzBf);
      cosTheta = dzBf / mag3d;
      cosPhi = dxBf / mag2d;
      sinPhi = dyBf / mag2d;
    }

    calcRotatedPoint(0f, 0, true);
    calcRotatedPoint(0.5f, 1, true);
    calcRotatedPoint(1f, 2, true);
    rasterCount = 3;
    interpolatePrecisely(0, 1);
    interpolatePrecisely(1, 2);
    for (int i = 0; i < rasterCount; i++) {
      xRaster[i] = (int) Math.floor(txRaster[i]);
      yRaster[i] = (int) Math.floor(tyRaster[i]);
      zRaster[i] = (int) Math.floor(tzRaster[i]);
    }
  }

  int rasterCount;

  float[] tRaster = new float[32];
  float[] txRaster = new float[32];
  float[] tyRaster = new float[32];
  float[] tzRaster = new float[32];
  int[] xRaster = new int[32];
  int[] yRaster = new int[32];
  int[] zRaster = new int[32];
  int[] fp8IntensityUp = new int[32];

  private void calcRotatedPoint(float t, int i, boolean isPrecision) {
    tRaster[i] = t;
    double tPI = t * Math.PI;
    double xT = Math.sin(tPI) * cosTheta;
    double yT = Math.cos(tPI);
    double xR = radius * (xT * cosPhi - yT * sinPhi);
    double yR = radius * (xT * sinPhi + yT * cosPhi);
    double z2 = radius2 - (xR * xR + yR * yR);
    double zR = (z2 > 0 ? Math.sqrt(z2) : 0);

    if (isPrecision) {
      txRaster[i] = (float) xR;
      tyRaster[i] = (float) yR;
      tzRaster[i] = (float) zR;
    } else if (tEvenDiameter) {
      xRaster[i] = (int) (xR - 0.5);
      yRaster[i] = (int) (yR - 0.5);
      zRaster[i] = (int) (zR + 0.5);
    } else {
      xRaster[i] = (int) (xR);
      yRaster[i] = (int) (yR);
      zRaster[i] = (int) (zR + 0.5);
    }
    fp8IntensityUp[i] = Shade3D.calcFp8Intensity((float) xR, (float) yR,
        (float) zR);
  }

  private void interpolate(int iLower, int iUpper) {
    int dx = xRaster[iUpper] - xRaster[iLower];
    if (dx < 0)
      dx = -dx;
    int dy = yRaster[iUpper] - yRaster[iLower];
    if (dy < 0)
      dy = -dy;
    if ((dx + dy) <= 1)
      return;
    float tLower = tRaster[iLower];
    float tUpper = tRaster[iUpper];
    int iMid = allocRaster(false);
    for (int j = 4; --j >= 0;) {
      float tMid = (tLower + tUpper) / 2;
      calcRotatedPoint(tMid, iMid, false);
      if ((xRaster[iMid] == xRaster[iLower])
          && (yRaster[iMid] == yRaster[iLower])) {
        fp8IntensityUp[iLower] = (fp8IntensityUp[iLower] + fp8IntensityUp[iMid]) >>> 1;
        tLower = tMid;
      } else if ((xRaster[iMid] == xRaster[iUpper])
          && (yRaster[iMid] == yRaster[iUpper])) {
        fp8IntensityUp[iUpper] = (fp8IntensityUp[iUpper] + fp8IntensityUp[iMid]) >>> 1;
        tUpper = tMid;
      } else {
        interpolate(iLower, iMid);
        interpolate(iMid, iUpper);
        return;
      }
    }
    xRaster[iMid] = xRaster[iLower];
    yRaster[iMid] = yRaster[iUpper];
  }

  private void interpolatePrecisely(int iLower, int iUpper) {
    int dx = (int) Math.floor(txRaster[iUpper])
        - (int) Math.floor(txRaster[iLower]);
    if (dx < 0)
      dx = -dx;
    float dy = (int) Math.floor(tyRaster[iUpper])
        - (int) Math.floor(tyRaster[iLower]);
    if (dy < 0)
      dy = -dy;
    if ((dx + dy) <= 1)
      return;
    float tLower = tRaster[iLower];
    float tUpper = tRaster[iUpper];
    int iMid = allocRaster(true);
    for (int j = 4; --j >= 0;) {
      float tMid = (tLower + tUpper) / 2;
      calcRotatedPoint(tMid, iMid, true);
      if (((int) Math.floor(txRaster[iMid]) == (int) Math
          .floor(txRaster[iLower]))
          && ((int) Math.floor(tyRaster[iMid]) == (int) Math
              .floor(tyRaster[iLower]))) {
        fp8IntensityUp[iLower] = (fp8IntensityUp[iLower] + fp8IntensityUp[iMid]) >>> 1;
        tLower = tMid;
      } else if (((int) Math.floor(txRaster[iMid]) == (int) Math
          .floor(txRaster[iUpper]))
          && ((int) Math.floor(tyRaster[iMid]) == (int) Math
              .floor(tyRaster[iUpper]))) {
        fp8IntensityUp[iUpper] = (fp8IntensityUp[iUpper] + fp8IntensityUp[iMid]) >>> 1;
        tUpper = tMid;
      } else {
        interpolatePrecisely(iLower, iMid);
        interpolatePrecisely(iMid, iUpper);
        return;
      }
    }
    txRaster[iMid] = txRaster[iLower];
    tyRaster[iMid] = tyRaster[iUpper];
  }

  private void plotRaster(int i) {
    int fpz = fp8IntensityUp[i] >> (8 + zShift);
    int fpzBack = fpz >> 1;
    int x = xRaster[i];
    int y = yRaster[i];
    int z = zRaster[i];
    if (tEndcapOpen && argbEndcap != 0) {
      if (clipped) {
        g3d.plotPixelClipped(argbEndcap, xEndcap + x, yEndcap + y, zEndcap - z
            - 1);
        g3d.plotPixelClipped(argbEndcap, xEndcap - x, yEndcap - y, zEndcap + z
            - 1);
      } else {
        g3d.plotPixelUnclipped(argbEndcap, xEndcap + x, yEndcap + y, zEndcap
            - z - 1);
        g3d.plotPixelUnclipped(argbEndcap, xEndcap - x, yEndcap - y, zEndcap
            + z - 1);
      }
    }
    line3d.plotLineDelta(shadesA, isScreenedA, shadesB, isScreenedB,
        fpz, xA + x, yA + y, zA - z, dxB, dyB, dzB, clipped);
    if (drawBackside) {
      line3d.plotLineDelta(shadesA[fpzBack], isScreenedA, shadesB[fpzBack], isScreenedB, xA
          - x, yA - y, zA + z, dxB, dyB, dzB,clipped);
    }
  }

  private int[] realloc(int[] a) {
    int[] t;
    t = new int[a.length * 2];
    System.arraycopy(a, 0, t, 0, a.length);
    return t;
  }

  private float[] realloc(float[] a) {
    float[] t;
    t = new float[a.length * 2];
    System.arraycopy(a, 0, t, 0, a.length);
    return t;
  }

  private int allocRaster(boolean isPrecision) {
    while (rasterCount >= xRaster.length) {
      xRaster = realloc(xRaster);
      yRaster = realloc(yRaster);
      zRaster = realloc(zRaster);
      tRaster = realloc(tRaster);
    }
    while (rasterCount >= fp8IntensityUp.length)
      fp8IntensityUp = realloc(fp8IntensityUp);
    if (isPrecision)
      while (rasterCount >= txRaster.length) {
        txRaster = realloc(txRaster);
        tyRaster = realloc(tyRaster);
        tzRaster = realloc(tzRaster);
      }
    return rasterCount++;
  }

  int yMin, yMax;
  int xMin, xMax;
  int zXMin, zXMax;

  private void findMinMaxY() {
    yMin = yMax = yRaster[0];
    for (int i = rasterCount; --i > 0;) {
      int y = yRaster[i];
      if (y < yMin)
        yMin = y;
      else if (y > yMax)
        yMax = y;
      else {
        y = -y;
        if (y < yMin)
          yMin = y;
        else if (y > yMax)
          yMax = y;
      }
    }
  }

  private void findMinMaxX(int y) {
    xMin = Integer.MAX_VALUE;
    xMax = Integer.MIN_VALUE;
    for (int i = rasterCount; --i >= 0;) {
      if (yRaster[i] == y) {
        int x = xRaster[i];
        if (x < xMin) {
          xMin = x;
          zXMin = zRaster[i];
        }
        if (x > xMax) {
          xMax = x;
          zXMax = zRaster[i];
        }
        //if (y == 0) {
        //}
      }
      if (yRaster[i] == -y) { // 0 will run through here too
        int x = -xRaster[i];
        if (x < xMin) {
          xMin = x;
          zXMin = -zRaster[i];
        }
        if (x > xMax) {
          xMax = x;
          zXMax = -zRaster[i];
        }
      }
    }
  }

  private void renderFlatEndcap(boolean tCylinder) {
    if (dzB == 0 || !g3d.setColix(colixEndcap))
      return;
    int xT = xA, yT = yA, zT = zA;
    if (tCylinder && dzB < 0) {
      if (endcaps == Graphics3D.ENDCAPS_OPENEND)
        return;
      xT += dxB;
      yT += dyB;
      zT += dzB;
    }
    findMinMaxY();
    for (int y = yMin; y <= yMax; ++y) {
      findMinMaxX(y);
      int count = xMax - xMin + 1;
      g3d.setColorNoisy(intensityEndcap);
      g3d.plotPixelsClipped(count, xT + xMin, yT + y, zT - zXMin - 1, zT
          - zXMax - 1, null, null);
    }
  }

  private void renderFlatEndcapPrecisely(boolean tCylinder) {
    if (dzBf == 0 || !g3d.setColix(colixEndcap))
      return;
    float xTf = xAf, yTf = yAf, zTf = zAf;
    if (tCylinder && dzBf < 0) {
      xTf += dxBf;
      yTf += dyBf;
      zTf += dzBf;
    }
    int xT = (int) xTf;
    int yT = (int) yTf;
    int zT = (int) zTf;
    
    findMinMaxY();
    for (int y = yMin; y <= yMax; ++y) {
      findMinMaxX(y);
      int count = xMax - xMin + 1;
      g3d.setColorNoisy(intensityEndcap);
      g3d.plotPixelsClipped(count, xT + xMin, yT + y, zT - zXMin - 1, zT
          - zXMax - 1, null, null);
    }
  }

  private void renderSphericalEndcaps() {
    if (colixA != 0 && g3d.setColix(colixA))
      g3d.fillSphereCentered(diameter, xA, yA, zA + 1);
    if (colixB != 0 && g3d.setColix(colixB))
      g3d.fillSphereCentered(diameter, xA + dxB, yA + dyB, zA + dzB + 1);
  }

  private void plotRasterCone(int i, boolean doFill) {
    float x = txRaster[i];
    float y = tyRaster[i];
    float z = tzRaster[i];
    float xUp = xAf + x, yUp = yAf + y, zUp = zAf - z;
    float xDn = xAf - x, yDn = yAf - y, zDn = zAf + z;
    int argb = shadesA[0];
    if (tEndcapOpen && argbEndcap != 0) {
      g3d.plotPixelClipped(argbEndcap, isScreenedA, (int) xUp, (int) yUp,
          (int) zUp);
      g3d.plotPixelClipped(argbEndcap, isScreenedA, (int) xDn, (int) yDn,
          (int) zDn);
    }
    int fpz = fp8IntensityUp[i] >> (8 + zShift);

    if (argb != 0) {
      line3d.plotLineDelta(shadesA, isScreenedA, shadesA, isScreenedA, fpz,
          (int) xUp, (int) yUp, (int) zUp, (int) Math.ceil(xTip - xUp),
          (int) Math.ceil(yTip - yUp), (int) Math.ceil(zTip - zUp), true);
      
      if (doFill) { //rockets, not arrows
        line3d.plotLineDelta(shadesA, isScreenedA, shadesA, isScreenedA, fpz,
          (int) xUp, (int) yUp + 1, (int) zUp, (int) Math.ceil(xTip - xUp),
          (int) Math.ceil(yTip - yUp) + 1, (int) Math.ceil(zTip - zUp), true);
        line3d.plotLineDelta(shadesA, isScreenedA, shadesA, isScreenedA, fpz,
          (int) xUp + 1, (int) yUp, (int) zUp, (int) Math.ceil(xTip - xUp) + 1,
          (int) Math.ceil(yTip - yUp), (int) Math.ceil(zTip - zUp), true);
      }    
  
      if (!(endcaps == Graphics3D.ENDCAPS_FLAT && dzB > 0)) {
        line3d.plotLineDelta(argb, isScreenedA, argb, isScreenedA, (int) xDn,
            (int) yDn, (int) zDn, (int) Math.ceil(xTip - xDn), (int) Math
                .ceil(yTip - yDn), (int) Math.ceil(zTip - zDn), true);
      }
    }
  }

  private void calcArgbEndcap(boolean tCylinder, boolean isFloat) {
    tEndcapOpen = false;
    float dzf = (isFloat ? dzBf : (float) dzB);
    if (endcaps == Graphics3D.ENDCAPS_SPHERICAL || dzf == 0)
      return;
    xEndcap = xA;
    yEndcap = yA;
    zEndcap = zA;
    int[] shadesEndcap;
    float dxf = (isFloat ? dxBf : (float) dxB);
    float dyf = (isFloat ? dyBf : (float) dyB);
    if (dzf >= 0 || !tCylinder) {
      intensityEndcap = Shade3D.calcIntensity(-dxf, -dyf, dzf);
      colixEndcap = colixA;
      shadesEndcap = shadesA;
      //Logger.debug("endcap is A");
    } else {
      intensityEndcap = Shade3D.calcIntensity(dxf, dyf, -dzf);
      colixEndcap = colixB;
      shadesEndcap = shadesB;
      xEndcap += dxB;
      yEndcap += dyB;
      zEndcap += dzB;
      //Logger.debug("endcap is B");
    }
    // limit specular glare on endcap
    if (intensityEndcap > Graphics3D.intensitySpecularSurfaceLimit)
      intensityEndcap = Graphics3D.intensitySpecularSurfaceLimit;
    argbEndcap = shadesEndcap[intensityEndcap];
    tEndcapOpen = (endcaps == Graphics3D.ENDCAPS_OPEN);
  }
}
