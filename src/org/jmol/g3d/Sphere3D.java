/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-19 07:21:18 +0200 (dim., 19 juil. 2009) $
 * $Revision: 11256 $
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

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
//import javax.vecmath.Vector4f;  !NO -- requires Vector4d in applet
import org.jmol.util.Quadric;



/**
 *<p>
 * Implements high performance rendering of shaded spheres.
 *</p>
 *<p>
 * Drawing spheres quickly is critically important to Jmol.
 * These routines implement high performance rendering of
 * spheres in 3D.
 *</p>
 *<p>
 * If you can think of a faster way to implement this, please
 * let us know.
 *</p>
 *<p>
 * There is a lot of bit-twiddling going on here, which may
 * make the code difficult to understand for non-systems programmers.
 *</p>
 * Ellipsoid code added 4/2008 -- Bob Hanson hansonr@stolaf.edu
 *
 * @author Miguel, miguel@jmol.org
 */
public class Sphere3D {

  Graphics3D g3d;
  
  Sphere3D(Graphics3D g3d) {
    this.g3d = g3d;
  }

  private int zShift;
  
  private final static int maxSphereCache = 128;
  private final static int maxOddSizeSphere = 49;
  final static int maxSphereDiameter = 1000;
  final static int maxSphereDiameter2 = maxSphereDiameter * 2;
  private final static int[][] sphereShapeCache = new int[maxSphereCache][];

  private double[] zroot = new double[2];
  private static int nOut, nIn;
  private Matrix3f mat;
  private double[] coef;
  private Matrix4f mDeriv;
  private int selectedOctant;
  private Point3i[] octantPoints;
  private int planeShade;
  private int[] zbuf;
  private int width;
  private int height;
  private int depth;
  private int slab;
  private int offsetPbufBeginLine;
  private boolean addAllPixels;

  static synchronized void flushSphereCache() {
    for (int i =  maxSphereCache; --i >= 0;)
      sphereShapeCache[i] = null;
    ellipsoidShades = null;
  }

  private static int[] getSphereShape(int diameter) {
    int[] ss;
    return ((ss = sphereShapeCache[diameter - 1]) == null ? createSphereShape(diameter): ss);
  }

  private synchronized static int[] createSphereShape(int diameter) {
    int countSE = 0;
    boolean oddDiameter = (diameter & 1) != 0;
    float radiusF = diameter / 2.0f;
    float radiusF2 = radiusF * radiusF;
    int radius = (diameter + 1) / 2;

    float y = oddDiameter ? 0 : 0.5f;
    for (int i = 0; i < radius; ++i, ++y) {
      float y2 = y * y;
      float x = oddDiameter ? 0 : 0.5f;
      for (int j = 0; j < radius; ++j, ++x) {
        float x2 = x * x;
        float z2 = radiusF2 - y2 - x2;
        if (z2 >= 0)
          ++countSE;
      }
    }
    
    int[] sphereShape = new int[countSE];
    int offset = 0;

    y = oddDiameter ? 0 : 0.5f;
    for (int i = 0; i < radius; ++i, ++y) {
      float y2 = y * y;
      float x = oddDiameter ? 0 : 0.5f;
      for (int j = 0; j < radius; ++j, ++x) {
        float x2 = x * x;
        float z2 = radiusF2 - y2 - x2;
        if (z2 >= 0) {
          float z = (float)Math.sqrt(z2);
          int height = (int)z;
          int intensitySE = Shade3D.calcDitheredNoisyIntensity( x,  y, z, radiusF);
          int intensitySW = Shade3D.calcDitheredNoisyIntensity(-x,  y, z, radiusF);
          int intensityNE = Shade3D.calcDitheredNoisyIntensity( x, -y, z, radiusF);
          int intensityNW = Shade3D.calcDitheredNoisyIntensity(-x, -y, z, radiusF);
          int packed = (height |
                        (intensitySE << 7) |
                        (intensitySW << 13) |
                        (intensityNE << 19) |
                        (intensityNW << 25));
          sphereShape[offset++] = packed;
        }
      }
      sphereShape[offset - 1] |= 0x80000000;
    }
    return sphereShapeCache[diameter - 1] = sphereShape;
  }

  private int minX, maxX, minY, maxY, minZ, maxZ;
  private int x, y, z, diameter;
  private boolean tScreened;
  private int[] shades;
  
  void render(int[] shades, boolean tScreened, int diameter, int x, int y,
              int z, Matrix3f mat, double[] coef, Matrix4f mDeriv,
              int selectedOctant, Point3i[] octantPoints) {
    if (z == 1)
      return;
    width = g3d.width;
    height = g3d.height;
    if (diameter > maxOddSizeSphere)
      diameter &= ~1;
    if (g3d.isClippedXY(diameter, x, y))
      return;
    int radius = (diameter + 1) >> 1;
    minX = x - radius;
    maxX = x + radius;
    minY = y - radius;
    maxY = y + radius;
    slab = g3d.slab;
    depth = g3d.depth;
    minZ = z - radius;
    maxZ = z + radius;
    if (maxZ < slab || minZ > depth)
      return;
    
    nOut = nIn = 0;
    zbuf = g3d.zbuf;
    addAllPixels = g3d.addAllPixels;
    offsetPbufBeginLine = width * y + x;
    this.x = x;
    this.y = y;
    this.z = z;
    this.diameter = diameter;
    this.tScreened = tScreened;
    this.shades = shades;
    this.mat = mat;
    if (mat != null) {
      this.coef = coef;
      this.mDeriv = mDeriv;
      this.selectedOctant = selectedOctant;
      this.octantPoints = octantPoints;
    }
    if (mat != null || diameter > maxSphereCache) {
      renderLarge();
      if (mat != null) {
        this.mat = null;
        this.coef = null;
        this.mDeriv = null;
        this.octantPoints = null;
      }
    } else {
      zShift = g3d.getZShift(z);
      int[] ss = getSphereShape(diameter);
      if (minX < 0 || maxX >= width || minY < 0 || maxY >= height
          || minZ < slab || z > depth)
        renderShapeClipped(ss);
      else
        renderShapeUnclipped(ss);
    }
    this.shades = null;
    this.zbuf = null;
    //System.out.println("sphere3d " + nIn + " " + nOut + " " + (1.0 * nIn / (nIn + nOut)));
  } 
  

  private void renderShapeUnclipped(int[] sphereShape) {
    int offsetSphere = 0;
    int evenSizeCorrection = 1 - (diameter & 1);
    int offsetSouthCenter = offsetPbufBeginLine;
    int offsetNorthCenter = offsetSouthCenter - evenSizeCorrection * width;
    int nLines = (diameter + 1) / 2;
    if (!tScreened) {
      do {
        int offsetSE = offsetSouthCenter;
        int offsetSW = offsetSouthCenter - evenSizeCorrection;
        int offsetNE = offsetNorthCenter;
        int offsetNW = offsetNorthCenter - evenSizeCorrection;
        int packed;
        do {
          packed = sphereShape[offsetSphere++];
          int zPixel = z - (packed & 0x7F);
          if (zPixel < zbuf[offsetSE])
            g3d.addPixel(offsetSE, zPixel,
                shades[((packed >> 7) & 0x3F) >> zShift]);
          if (zPixel < zbuf[offsetSW])
            g3d.addPixel(offsetSW, zPixel,
                shades[((packed >> 13) & 0x3F) >> zShift]);
          if (zPixel < zbuf[offsetNE])
            g3d.addPixel(offsetNE, zPixel,
                shades[((packed >> 19) & 0x3F) >> zShift]);
          if (zPixel < zbuf[offsetNW])
            g3d.addPixel(offsetNW, zPixel,
                shades[((packed >> 25) & 0x3F) >> zShift]);
          ++offsetSE;
          --offsetSW;
          ++offsetNE;
          --offsetNW;
        } while (packed >= 0);
        offsetSouthCenter += width;
        offsetNorthCenter -= width;
      } while (--nLines > 0);
      return;
    }
    int flipflopSouthCenter = (x ^ y) & 1;
    int flipflopNorthCenter = flipflopSouthCenter ^ evenSizeCorrection;
    int flipflopSE = flipflopSouthCenter;
    int flipflopSW = flipflopSouthCenter ^ evenSizeCorrection;
    int flipflopNE = flipflopNorthCenter;
    int flipflopNW = flipflopNorthCenter ^ evenSizeCorrection;
    int flipflopsCenter = flipflopSE | (flipflopSW << 1) | (flipflopNE << 2)
        | (flipflopNW << 3);
    do {
      int offsetSE = offsetSouthCenter;
      int offsetSW = offsetSouthCenter - evenSizeCorrection;
      int offsetNE = offsetNorthCenter;
      int offsetNW = offsetNorthCenter - evenSizeCorrection;
      int packed;
      int flipflops = (flipflopsCenter = ~flipflopsCenter);
      do {
        packed = sphereShape[offsetSphere++];
        int zPixel = z - (packed & 0x7F);
        if ((flipflops & 1) != 0 && zPixel < zbuf[offsetSE])
            g3d.addPixel(offsetSE, zPixel,
                shades[((packed >> 7) & 0x3F) >> zShift]);
        if ((flipflops & 2) != 0 && zPixel < zbuf[offsetSW])        
          g3d.addPixel(offsetSW, zPixel,
              shades[((packed >> 13) & 0x3F) >> zShift]);
        if ((flipflops & 4) != 0 && zPixel < zbuf[offsetNE])        
          g3d.addPixel(offsetNE, zPixel,
              shades[((packed >> 19) & 0x3F) >> zShift]);
        if ((flipflops & 8) != 0 && zPixel < zbuf[offsetNW])
          g3d.addPixel(offsetNW, zPixel,
              shades[((packed >> 25) & 0x3F) >> zShift]);
        ++offsetSE;
        --offsetSW;
        ++offsetNE;
        --offsetNW;
        flipflops = ~flipflops;
      } while (packed >= 0);
      offsetSouthCenter += width;
      offsetNorthCenter -= width;
    } while (--nLines > 0);
  }

  private final static int SHADE_SLAB_CLIPPED = Shade3D.shadeNormal - 5;

  private void renderShapeClipped(int[] sphereShape) {
    int offsetSphere = 0;
    int evenSizeCorrection = 1 - (diameter & 1);
    int offsetSouthCenter = offsetPbufBeginLine;
    int offsetNorthCenter = offsetSouthCenter - evenSizeCorrection * width;
    int nLines = (diameter + 1) / 2;
    int ySouth = y;
    int yNorth = y - evenSizeCorrection;
    int randu = (x << 16) + (y << 1) ^ 0x33333333;
    int flipflopSouthCenter = (x ^ y) & 1;
    int flipflopNorthCenter = flipflopSouthCenter ^ evenSizeCorrection;
    int flipflopSE = flipflopSouthCenter;
    int flipflopSW = flipflopSouthCenter ^ evenSizeCorrection;
    int flipflopNE = flipflopNorthCenter;
    int flipflopNW = flipflopNorthCenter ^ evenSizeCorrection;
    int flipflopsCenter = flipflopSE | (flipflopSW << 1) | (flipflopNE << 2)
        | (flipflopNW << 3);
    do {
      boolean tSouthVisible = ySouth >= 0 && ySouth < height;
      boolean tNorthVisible = yNorth >= 0 && yNorth < height;
      int offsetSE = offsetSouthCenter;
      int offsetSW = offsetSouthCenter - evenSizeCorrection;
      int offsetNE = offsetNorthCenter;
      int offsetNW = offsetNorthCenter - evenSizeCorrection;
      int packed;
      int flipflops = (flipflopsCenter = ~flipflopsCenter);
      int xEast = x;
      int xWest = x - evenSizeCorrection;
      do {
        boolean tWestVisible = xWest >= 0 && xWest < width;
        boolean tEastVisible = xEast >= 0 && xEast < width;
        packed = sphereShape[offsetSphere++];
        boolean isCore;
        int zOffset = packed & 0x7F;
        int zPixel;
        if (z < slab) {
          // center in front of plane -- possibly show back half
          zPixel = z + zOffset;
          isCore = (zPixel >= slab);
        } else {
          // center is behind, show front, possibly as solid core
          zPixel = z - zOffset;
          isCore = (zPixel < slab);
        }
        if (isCore)
          zPixel = slab;
        if (zPixel >= slab && zPixel <= depth) {
          if (tSouthVisible) {
            if (tEastVisible && (addAllPixels || (flipflops & 1) != 0)
                && zPixel < zbuf[offsetSE]) {
              int i = (isCore ? SHADE_SLAB_CLIPPED - 3 + ((randu >> 7) & 0x07)
                  : (packed >> 7) & 0x3F);
              g3d.addPixel(offsetSE, zPixel, shades[i >> zShift]);
            }
            if (tWestVisible && (addAllPixels || (flipflops & 2) != 0)
                && zPixel < zbuf[offsetSW]) {
              int i = (isCore ? SHADE_SLAB_CLIPPED - 3 + ((randu >> 13) & 0x07)
                  : (packed >> 13) & 0x3F);
              g3d.addPixel(offsetSW, zPixel, shades[i >> zShift]);
            }
          }
          if (tNorthVisible) {
            if (tEastVisible && (!tScreened || (flipflops & 4) != 0)
                && zPixel < zbuf[offsetNE]) {
              int i = (isCore ? SHADE_SLAB_CLIPPED - 3 + ((randu >> 19) & 0x07)
                  : (packed >> 19) & 0x3F);
              g3d.addPixel(offsetNE, zPixel, shades[i >> zShift]);
            }
            if (tWestVisible && (!tScreened || (flipflops & 8) != 0)
                && zPixel < zbuf[offsetNW]) {
              int i = (isCore ? SHADE_SLAB_CLIPPED - 3 + ((randu >> 25) & 0x07)
                  : (packed >> 25) & 0x3F);
              g3d.addPixel(offsetNW, zPixel, shades[i >> zShift]);
            }
          }
        }
        ++offsetSE;
        --offsetSW;
        ++offsetNE;
        --offsetNW;
        ++xEast;
        --xWest;
        flipflops = ~flipflops;
        if (isCore)
          randu = ((randu << 16) + (randu << 1) + randu) & 0x7FFFFFFF;
      } while (packed >= 0);
      offsetSouthCenter += width;
      offsetNorthCenter -= width;
      ++ySouth;
      --yNorth;
    } while (--nLines > 0);
  }

  private void renderLarge() {
    if (mat != null) {
      if (ellipsoidShades == null)
        createEllipsoidShades();
      if (octantPoints != null)
        setPlaneDerivatives();
    } else if (!Shade3D.sphereShadingCalculated)
      Shade3D.calcSphereShading();
    renderQuadrant(-1, -1);
    renderQuadrant(-1, 1);
    renderQuadrant(1, -1);
    renderQuadrant(1, 1);
  }

  private void renderQuadrant(int xSign, int ySign) {
    int radius = diameter / 2;
    int t = x + radius * xSign;
    int xStatus = (x < 0 ? -1 : x < width ? 0 : 1)
        + (t < 0 ? -2 : t < width ? 0 : 2);
    if (xStatus == -3 || xStatus == 3)
      return;

    t = y + radius * ySign;
    int yStatus = (y < 0 ? -1 : y < height ? 0 : 1)
        + (t < 0 ? -2 : t < height ? 0 : 2);
    if (yStatus == -3 || yStatus == 3)
      return;

    boolean unclipped = (mat == null && xStatus == 0 && yStatus == 0 
        && z - radius >= slab  && z <= depth);
    if (unclipped)
      renderQuadrantUnclipped(radius, xSign, ySign);
    else
      renderQuadrantClipped(radius, xSign, ySign);
  }

  private void renderQuadrantUnclipped(int radius, int xSign, int ySign) {
    int r2 = radius * radius;
    int dDivisor = radius * 2 + 1;
    // it will get flipped twice before use
    // so initialize it to true if it is at an even coordinate
    boolean flipflopBeginLine = ((x ^ y) & 1) == 0;
    int lineIncrement = (ySign < 0 ? -width : width);
    int ptLine = offsetPbufBeginLine;
    for (int i = 0, i2 = 0; i2 <= r2; 
        i2 += i + (++i),
        ptLine += lineIncrement) {
      int offset = ptLine;
      boolean flipflop = (flipflopBeginLine = !flipflopBeginLine);
      int s2 = r2 - i2;
      int z0 = z - radius;
      int y8 = ((i * ySign + radius) << 8) / dDivisor;
      for (int j = 0, j2 = 0; j2 <= s2;
           j2 += j + (++j),
           offset += xSign) {
        if (addAllPixels || (flipflop = !flipflop)) {
          if (zbuf[offset] <= z0)
            continue;
          int k = (int)Math.sqrt(s2 - j2);
          z0 = z - k;
          if (zbuf[offset] <= z0)
            continue;
          int x8 = ((j * xSign + radius) << 8) / dDivisor;
          g3d.addPixel(offset,z0, shades[Shade3D.sphereIntensities[((y8 << 8) + x8) >> zShift]]);
        }
      }
    }
  }

  private final Point3f ptTemp = new Point3f();
  private final int[] planeShades = new int[3];
  private final float[][] dxyz = new float[3][3];
  
  private void renderQuadrantClipped(int radius, int xSign, int ySign) {
    boolean isEllipsoid = (mat != null);
    boolean checkOctant = (selectedOctant >= 0);
    int r2 = radius * radius;
    int dDivisor = radius * 2 + 1;
    int lineIncrement = (ySign < 0 ? -width : width);
    int ptLine = offsetPbufBeginLine;
    int randu = (x << 16) + (y << 1) ^ 0x33333333;
    int yCurrent = y;
    int y8 = 0;
    int iShade = 0;
    for (int i = 0, i2 = 0; i2 <= r2;
         i2 += i + (++i),
         ptLine += lineIncrement,
         yCurrent += ySign) {
      if (yCurrent < 0) {
        if (ySign < 0)
          return;
        continue;
      }
      if (yCurrent >= height) {
        if (ySign > 0)
          return;
        continue;
      }
      int s2 = r2 - (isEllipsoid? 0 : i2);
      int xCurrent = x;
      if (!isEllipsoid) {
        y8 = ((i * ySign + radius) << 8) / dDivisor;
      }
      randu = ((randu << 16) + (randu << 1) + randu) & 0x7FFFFFFF;
      int iRoot = -1;
      int mode = 1;
      int offset = ptLine;
      for (int j = 0, j2 = 0; j2 <= s2;
          j2 += j + (++j), 
          offset += xSign,
          xCurrent += xSign) {
        if (xCurrent < 0) {
          if (xSign < 0)
            break;
          continue;
        }
        if (xCurrent >= width) {
          if (xSign > 0)
            break;
          continue;
        }
        if (tScreened && (((xCurrent ^ yCurrent) & 1) != 0))
          continue;
        int zPixel;
        if (isEllipsoid) {
          if (!Quadric.getQuardricZ(xCurrent, yCurrent, coef, zroot)) {
            if (iRoot >= 0) {
              // done for this line
              break;
            }
            continue;
          }
          iRoot = (z < slab ? 1 : 0);
          zPixel = (int) zroot[iRoot];
          if (zPixel == 0)
            zPixel = z;
          mode = 2;
          if (checkOctant) {
            ptTemp.set(xCurrent - x, yCurrent - y, zPixel - z);
            mat.transform(ptTemp);
            int thisOctant = Quadric.getOctant(ptTemp); 
            if (thisOctant == selectedOctant) {
              iShade = getPlaneShade(xCurrent, yCurrent, zroot);
              zPixel = (int) zroot[0];
              mode = 3;
                // another option: show back only
                //iRoot = 1;
                //zPixel = (int) zroot[iRoot];
            }
          }
        } else {
          int zOffset = (int)Math.sqrt(s2 - j2);
          zPixel = z + (z < slab ? zOffset : -zOffset);          
        }
        boolean isCore = (z < slab ? zPixel >= slab : zPixel < slab);
        if (isCore) {
          zPixel = slab;
          mode = 0;
        }
        if (zPixel < slab || zPixel > depth || zbuf[offset] <= zPixel)
          continue;
        switch(mode) {
        case 0: //core
          iShade = (SHADE_SLAB_CLIPPED - 3 + ((randu >> 8) & 0x07)) >> zShift;
          randu = ((randu << 16) + (randu << 1) + randu) & 0x7FFFFFFF;
          mode = 1;
          break;
        case 2: //ellipsoid
          iShade = getEllipsoidShade(xCurrent, yCurrent, (float) zroot[iRoot], radius, mDeriv);
          break;
        case 3: //ellipsoid fill
          break;
        default: //sphere
          int x8 = ((j * xSign + radius) << 8) / dDivisor;
          iShade = Shade3D.sphereIntensities[(y8 << 8) + x8] >> zShift;
          break;
        }
        g3d.addPixel(offset, zPixel, shades[iShade]);
      }
      // randu is failing me and generating moire patterns :-(
      // so throw in a little more salt
      randu = ((randu + xCurrent + yCurrent) | 1) & 0x7FFFFFFF;
    }
  }

  //////////  Ellipsoid Code ///////////
  //
  // Bob Hanson, 4/2008
  //
  //////////////////////////////////////
  
  private void setPlaneDerivatives() {
    planeShade = -1;
    for (int i = 0; i < 3; i ++) {
      float dx = dxyz[i][0] = octantPoints[i].x - x;
      float dy = dxyz[i][1] = octantPoints[i].y - y;
      float dz = dxyz[i][2] = octantPoints[i].z - z;
      planeShades[i] = Shade3D.calcIntensity(dx, dy, -dz);
      if (dx == 0 && dy == 0) {
        planeShade = planeShades[i];
        return;
      }
    }
  }

  private int getPlaneShade(int xCurrent, int yCurrent, double[] zroot) {
    if (planeShade >= 0)
      return planeShade;
    int iMin = 3;
    float dz;
    float zMin = Float.MAX_VALUE;
    for (int i = 0; i < 3; i ++) {
      if ((dz = dxyz[i][2]) == 0)
        continue;
      float ptz = z + (-dxyz[i][0] * (xCurrent - x)
          - dxyz[i][1]*(yCurrent - y)) / dz;
      if (ptz < zMin) {
        zMin = ptz;
        iMin = i;
      }
    }
    if (iMin == 3) {
      iMin = 0;
      zMin = z;
    }
    zroot[0] = zMin;
    return planeShades[iMin];
  }
  
  private static byte[][][] ellipsoidShades;
  
  final private static int SLIM = 20;
  final private static int SDIM = SLIM * 2;
  private static void createEllipsoidShades() {
    
    // we don't need to cache rear-directed normals (kk < 0)
    
    ellipsoidShades = new byte[SDIM][SDIM][SDIM];
    for (int ii = 0; ii < SDIM; ii++)
      for (int jj = 0; jj < SDIM; jj++)
        for (int kk = 0; kk < SDIM; kk++)
          ellipsoidShades[ii][jj][kk] = Shade3D.calcIntensity(ii - SLIM, jj
              - SLIM, kk);
  }

  private static int getEllipsoidShade(float x, float y, float z, int radius,
                                       Matrix4f mDeriv) {
    float tx = mDeriv.m00 * x + mDeriv.m01 * y + mDeriv.m02 * z + mDeriv.m03;
    float ty = mDeriv.m10 * x + mDeriv.m11 * y + mDeriv.m12 * z + mDeriv.m13;
    float tz = mDeriv.m20 * x + mDeriv.m21 * y + mDeriv.m22 * z + mDeriv.m23;
    float f = Math.min(radius/2f, 45) / 
        (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
    // optimized for about 30-100% inclusion
    int i = (int) (-tx * f);
    int j = (int) (-ty * f);
    int k = (int) (tz * f);
    boolean outside = i < -SLIM || i >= SLIM || j < -SLIM || j >= SLIM
        || k < 0 || k >= SDIM;
    if (outside) {
      while (i % 2 == 0 && j % 2 == 0 && k % 2 == 0 && i + j + k > 0) {
        i >>= 1;
        j >>= 1;
        k >>= 1;
      }
      outside = i < -SLIM || i >= SLIM || j < -SLIM || j >= SLIM || k < 0
          || k >= SDIM;
    }
    
    if (outside)
      nOut++;
    else
      nIn++;

    return (outside ? Shade3D.calcIntensity(i, j, k)
        : ellipsoidShades[i + SLIM][j + SLIM][k]);
  }
  

}
