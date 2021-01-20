/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-08-30 01:11:36 +0200 (dim., 30 août 2009) $
 * $Revision: 11396 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
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

import org.jmol.geodesic.Geodesic;
import org.jmol.util.BitSetUtil;

import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;
import java.util.Random;
import java.util.BitSet;

/**
 * Provides quantization of normalized vectors so that shading for
 * lighting calculations can be handled by a simple index lookup
 *<p>
 * A 'normix' is a normal index, represented as a short
 *
 * @author Miguel, miguel@jmol.org
 */
class Normix3D {

  final static int NORMIX_GEODESIC_LEVEL = Geodesic.standardLevel;

  private final static int normixCount = Geodesic.getVertexCount(NORMIX_GEODESIC_LEVEL);
  private final static Vector3f[] vertexVectors = Geodesic.getVertexVectors(); 
  private final static short[][] faceVertexesArrays = Geodesic.getFaceVertexesArrays();
  private final static short[][] neighborVertexesArrays = Geodesic.getNeighborVertexesArrays();

  private final Vector3f[] transformedVectors;
  private final byte[] intensities;
  private final byte[] intensities2Sided;

  private static short[][] faceNormixesArrays;
    // not "final" = new short[NORMIX_GEODESIC_LEVEL + 1][];

  private final static boolean TIMINGS = false;
  private final static boolean DEBUG_WITH_SEQUENTIAL_SEARCH = false;

  private final Matrix3f rotationMatrix = new Matrix3f();

  Normix3D() {
    // 12, 42, 162, 642
    intensities = new byte[normixCount];
    intensities2Sided = new byte[normixCount];
    transformedVectors = new Vector3f[normixCount];
    for (int i = normixCount; --i >= 0; )
      transformedVectors[i] = new Vector3f();

    if (TIMINGS) {
      Random rand = new Random();
      Vector3f vFoo = new Vector3f();
      Vector3f vBar = new Vector3f();
      Vector3f vSum = new Vector3f();
      
      int runCount = 100000;
      short[] neighborVertexes = neighborVertexesArrays[NORMIX_GEODESIC_LEVEL];
      for (int i = 0; i < runCount; ++i) {
        short foo = (short)(rand.nextDouble() * normixCount);
        int offsetNeighbor;
        short bar;
        do {
          offsetNeighbor = foo * 6 + (int)(rand.nextDouble() * 6);
          bar = neighborVertexes[offsetNeighbor];
        } while (bar == -1);
        vFoo.set(vertexVectors[foo]);
        vFoo.scale(rand.nextFloat());
        vBar.set(vertexVectors[bar]);
        vBar.scale(rand.nextFloat());
        vSum.add(vFoo, vBar);
        vSum.normalize();
      }
      for (int i = 0; i < runCount; ++i) {
        short foo = (short)(rand.nextDouble() * normixCount);
        int offsetNeighbor;
        short bar;
        do {
          offsetNeighbor = foo * 6 + (int)(rand.nextDouble() * 6);
          bar = neighborVertexes[offsetNeighbor];
        } while (bar == -1);
        vFoo.set(vertexVectors[foo]);
        vFoo.scale(rand.nextFloat());
        vBar.set(vertexVectors[bar]);
        vBar.scale(rand.nextFloat());
        vSum.add(vFoo, vBar);
        short sum = getNormix(vSum);
        if (sum != foo && sum != bar) {
          throw new NullPointerException();
        }
        short sum2 = getNormix(vSum);
        if (sum != sum2) {
          throw new NullPointerException();
        }
      }
    }
  }

  final BitSet bsConsidered = new BitSet();

  short getNormix(Vector3f v) {
    return getNormix(v.x, v.y, v.z, NORMIX_GEODESIC_LEVEL);
  }

  Vector3f getVector(short normix) {
    return vertexVectors[normix];
  }
  
  short getNormix(double x, double y, double z, int geodesicLevel) {
    short champion;
    double t;
    if (z >= 0) {
      champion = 0;
      t = z - 1;
    } else {
      champion = 11;
      t = z - (-1);
    }
    BitSetUtil.clear(bsConsidered);
    bsConsidered.set(champion);
    double championDist2 = x*x + y*y + t*t;
    for (int lvl = 0; lvl <= geodesicLevel; ++lvl) {
      short[] neighborVertexes = neighborVertexesArrays[lvl];
      for (int offsetNeighbors = 6 * champion,
             i = offsetNeighbors + (champion < 12 ? 5 : 6);
           --i >= offsetNeighbors; ) {
        short challenger = neighborVertexes[i];
        if (bsConsidered.get(challenger))
            continue;
        bsConsidered.set(challenger);
        //Logger.debug("challenger=" + challenger);
        Vector3f v = vertexVectors[challenger];
        double d;
        // d = dist2(v, x, y, z);
        //Logger.debug("challenger d2=" + (d*d));
        d = v.x - x;
        double d2 = d * d;
        if (d2 >= championDist2)
          continue;
        d = v.y - y;
        d2 += d * d;
        if (d2 >= championDist2)
          continue;
        d = v.z - z;
        d2 += d * d;
        if (d2 >= championDist2)
          continue;
        champion = challenger;
        championDist2 = d2;
      }
    }

    if (DEBUG_WITH_SEQUENTIAL_SEARCH) {
      int champSeq = 0;
      double champSeqD2 = dist2(vertexVectors[champSeq], x, y, z);
      for (int k = Geodesic.getVertexCount(geodesicLevel); --k > 0; ) {
        double challengerD2 = dist2(vertexVectors[k], x, y, z);
        if (challengerD2 < champSeqD2) {
          champSeq = k;
          champSeqD2 = challengerD2;
        }
      }
      if (champion != champSeq) {
        if (champSeqD2 + .01 < championDist2) {
          return (short)champSeq;
        }
      }
    }
    return champion;
  }

  short[] inverseNormixes;

  void calculateInverseNormixes() {
    inverseNormixes = new short[normixCount];
    for (int n = normixCount; --n >= 0; ) {
      Vector3f v = vertexVectors[n];
      inverseNormixes[n] = getNormix(-v.x, -v.y, -v.z, NORMIX_GEODESIC_LEVEL);
      }
    // validate that everyone's inverse is themselves
    //for (int n = normixCount; --n >= 0; )
    //  if (inverseNormixes[inverseNormixes[n]] != n)
    //    throw new NullPointerException();
  }

  private static byte nullIntensity = 50;
  byte getIntensity(short normix) {
    return (normix == ~Graphics3D.NORMIX_NULL || normix == Graphics3D.NORMIX_NULL 
        ? nullIntensity : normix < 0 ? intensities2Sided[~normix] :intensities[normix]);
  }

  void setRotationMatrix(Matrix3f rotationMatrix) {
    this.rotationMatrix.set(rotationMatrix);
    for (int i = normixCount; --i >= 0; ) {
      Vector3f tv = transformedVectors[i];
      rotationMatrix.transform(vertexVectors[i], tv);
      float x = tv.x;
      float y = -tv.y;
      float z = tv.z;
      /*
        enable this code in order to allow
        lighting of the inside of surfaces.
        but they probably should not be specular
        and light source should be from another position ... like a headlamp
        
      if (z < 0) {
        x = -x;
        y = -y;
        z = -z;
      }
      */
      byte intensity = Shade3D.calcIntensityNormalized(x, y, z);
      intensities[i] = intensity;
      if (z >= 0)
        intensities2Sided[i] = intensity;
      else
        intensities2Sided[i] = Shade3D.calcIntensityNormalized(-x, -y, -z);
    }
  }

  Vector3f[] getTransformedVectors() {
    return transformedVectors;
  }

  short[] getFaceNormixes(int level) {
    if (faceNormixesArrays == null)
      faceNormixesArrays = new short[NORMIX_GEODESIC_LEVEL + 1][];
    short[] faceNormixes = faceNormixesArrays[level];
    if (faceNormixes != null)
      return faceNormixes;
    return calcFaceNormixes(level);
  }

  private static double dist2(Vector3f v1, Vector3f v2) {
    double dx = v1.x - v2.x;
    double dy = v1.y - v2.y;
    double dz = v1.z - v2.z;
    return dx*dx + dy*dy + dz*dz;
  }
    
  private static double dist2(Vector3f v1, double x, double y, double z) {
    double dx = v1.x - x;
    double dy = v1.y - y;
    double dz = v1.z - z;
    return dx*dx + dy*dy + dz*dz;
  }
    
  private synchronized short[] calcFaceNormixes(int level) {
    short[] faceNormixes = faceNormixesArrays[level];
    if (faceNormixes != null)
      return faceNormixes;
    Vector3f t = new Vector3f();
    short[] faceVertexes = faceVertexesArrays[level];
    int j = faceVertexes.length;
    int faceCount = j / 3;
    faceNormixes = new short[faceCount];
    for (int i = faceCount; --i >= 0; ) {
      Vector3f vA = vertexVectors[faceVertexes[--j]];
      Vector3f vB = vertexVectors[faceVertexes[--j]];
      Vector3f vC = vertexVectors[faceVertexes[--j]];
      t.add(vA, vB);
      t.add(vC);
      short normix = getNormix(t);
      faceNormixes[i] = normix;
    }
    faceNormixesArrays[level] = faceNormixes;
    return faceNormixes;
  }

  boolean isDirectedTowardsCamera(short normix) {
    // normix < 0 means a double sided normix, so always visible
    return (normix < 0) || (transformedVectors[normix].z > 0);
  }
}
