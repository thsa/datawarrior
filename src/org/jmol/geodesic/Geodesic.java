/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-06 09:49:20 -0500 (Fri, 06 Apr 2007) $
 * $Revision: 7338 $
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

package org.jmol.geodesic;

import javax.vecmath.Vector3f;
import java.util.Hashtable;

  /**
   * 
   * Consolidated Geodesic from dotsRenderer 3/19/07 Bob Hanson
   * 
   * Constructs a canonical geodesic sphere of unit radius.
   *<p>
   * The Normix3D code quantizes arbitrary vectors to the vectors
   * of this unit sphere. normix values are then used for
   * high performance surface normal lighting
   *<p>
   * The vertices of the geodesic sphere can be used for constructing
   * vanderWaals and Connolly dot surfaces.
   *<p>
   * One geodesic sphere is constructed. It is a unit sphere
   * with radius of 1.0
   *<p>
   * Many times a sphere is constructed with lines of latitude and
   * longitude. With this type of rendering, the atom has north and
   * south poles. And the faces are not regularly shaped ... at the
   * poles they are triangles but elsewhere they are quadrilaterals.
   *<p>
   * A geodesic sphere is more appropriate for this type
   * of application. The geodesic sphere does not have poles and 
   * looks the same in all orientations ... as a sphere should. All
   * faces are equilateral triangles.
   *<p>
   * The geodesic sphere is constructed by starting with an icosohedron, 
   * a platonic solid with 12 vertices and 20 equilateral triangles
   * for faces. The internal call to the private
   * method <code>quadruple</code> will
   * split each triangular face into 4 faces by creating a new vertex
   * at the midpoint of each edge. These midpoints are still in the
   * plane, so they are then 'pushed out' to the surface of the
   * enclosing sphere by normalizing their length back to 1.0
   *<p>
   * The sequence of vertex counts is 12, 42, 162, 642, 2562.
   * These are identified by 'levels', that run from 0 through 4;
   * The vertices
   * are stored so that when spheres are small they can choose to display
   * only the first n bits where n is one of the above vertex counts.
   *<code>
   * Faces + Vertices = Edges + 2
   *   Faces: 20, 80, 320, 1280, 5120, 20480
   *     start with 20 faces ... at each level multiply by 4
   *   Edges: 30, 120, 480, 1920, 7680, 30720
   *     start with 30 edges ... also multipy by 4 ... strange, but true
   *   Vertices: 12, 42, 162, 642, 2562, 10242
   *     start with 12 vertices and 30 edges.
   *     when you subdivide, each edge contributes one vertex
   *     12 + 30 = 42 vertices at the next level
   *     80 faces + 42 vertices - 2 = 120 edges at the next level

   * geodesic level 0 vertexCount= 12 faceCount=20 edgeCount=30
   * geodesic level 1 vertexCount= 42 faceCount=80 edgeCount=120
   * geodesic level 2 vertexCount= 162 faceCount=320 edgeCount=480
   * geodesic level 3 vertexCount= 642 faceCount=1280 edgeCount=1920
   *
   *</code>
   *<p>
   * The vertices of the 'one true canonical sphere' are rotated to the
   * current molecular rotation at the beginning of the repaint cycle.
   * That way,
   * individual atoms only need to scale the unit vector to the vdw
   * radius for that atom.
   *<p>
   *
   * Notes 27 Sep 2005 </br>
   * If I were to switch the representation to staring with
   * a tetrahedron instead of an icosohedron we would get:
   *<code>
   * Faces: 4, 16, 64, 256, 1024
   * Edges: 6, 24, 96, 384, 1536
   * Vertices: 4, 10, 34, 130, 514
   *</code>
   * If I switched to face-centered normixes then I could efficiently
   * Regardless, I think that face-centered normixes would also reduce
   * ambiguity and would speed up the normal to normix process.
   *
   * I could also start with an octahedron that placed one triangle
   * in each 3D cartesian octant. That would push to 512 faces instead
   * of 256 faces, leaving me with shorts. But, it would be easier to quantize
   * at the first level because it would be based upon sign. And perhaps
   * it would be easier to take advantage of symmetry in the process of
   * converting from normal to normix.
   */

public class Geodesic {
  
  private final static float halfRoot5 = (float)(0.5 * Math.sqrt(5));
  private final static float oneFifth = 2 * (float)Math.PI / 5;
  private final static float oneTenth = oneFifth / 2;
  
  // miguel 2005 01 11
  // within the context of this code, the term 'vertex' is used
  // to refer to a short which is an index into the tables
  // of vertex information.
  private final static short[] faceVertexesIcosahedron = {
    0, 1, 2,
    0, 2, 3,
    0, 3, 4,
    0, 4, 5,
    0, 5, 1,

    1, 6, 2,
    2, 7, 3,
    3, 8, 4,
    4, 9, 5,
    5, 10, 1,

    6, 1, 10,
    7, 2, 6,
    8, 3, 7,
    9, 4, 8,
    10, 5, 9,

    11, 6, 10,
    11, 7, 6,
    11, 8, 7,
    11, 9, 8,
    11, 10, 9,
  };

  // every vertex has 6 neighbors ... except at the beginning of the world
  private final static short[] neighborVertexesIcosahedron = {
    1, 2, 3, 4, 5,-1, // 0
    0, 5,10, 6, 2,-1, // 1
    0, 1, 6, 7, 3,-1, // 2
    0, 2, 7, 8, 4,-1, // 3

    0, 3, 8, 9, 5,-1, // 4
    0, 4, 9,10, 1,-1, // 5
    1,10,11, 7, 2,-1, // 6
    2, 6,11, 8, 3,-1, // 7

    3, 7,11, 9, 4,-1, // 8
    4, 8,11,10, 5,-1, // 9
    5, 9,11, 6, 1,-1, // 10
    6, 7, 8, 9,10,-1 // 11
  };

  /**
   * 5 levels, 0 through 4
   */
  
  public final static int standardLevel = 3;
  private final static int maxLevel = 4;
  private static short[] vertexCounts;
  private static Vector3f[] vertexVectors;
  private static short[][] faceVertexesArrays;
  private static short[][] neighborVertexesArrays;

  static public short[][] getFaceVertexesArrays() {
    return faceVertexesArrays;
  }
  
  static public short[][] getNeighborVertexesArrays() {
     return neighborVertexesArrays;
  }
  
  static public int getVertexCount(int level) {
    if (vertexCounts == null)
      createGeodesic();
    return vertexCounts[level];
  }
  
  static public Vector3f[] getVertexVectors() {
    return vertexVectors;
  }

  static public int getVertexVectorsCount() {
    return (vertexVectors == null ? 0 : vertexVectors.length);
  }

  static public Vector3f getVertexVector(int i) {
    return vertexVectors[i];
  }

  static public short[] getFaceVertexes(int level) {
    return faceVertexesArrays[level];
  }

  ////////////// private methods ///////////////
  
  synchronized private static void createGeodesic() {
    //only one per applet set
    if (vertexCounts != null)
      return;
    short[] v = new short[maxLevel];
    neighborVertexesArrays = new short[maxLevel][];
    faceVertexesArrays = new short[maxLevel][];
    vertexVectors = new Vector3f[12];
    vertexVectors[0] = new Vector3f(0, 0, halfRoot5);
    for (int i = 0; i < 5; ++i) {
      vertexVectors[i + 1] = new Vector3f((float) Math.cos(i * oneFifth),
          (float) Math.sin(i * oneFifth), 0.5f);
      vertexVectors[i + 6] = new Vector3f((float) Math.cos(i * oneFifth
          + oneTenth), (float) Math.sin(i * oneFifth + oneTenth), -0.5f);
    }
    vertexVectors[11] = new Vector3f(0, 0, -halfRoot5);
    for (int i = 12; --i >= 0;)
      vertexVectors[i].normalize();
    faceVertexesArrays[0] = faceVertexesIcosahedron;
    neighborVertexesArrays[0] = neighborVertexesIcosahedron;
    v[0] = 12;

    for (int i = 0; i < maxLevel - 1; ++i)
      quadruple(i, v);

    /*      for (int i = 0; i < maxLevel; ++i) {
     System.out.println("geodesic level " + i + " vertexCount= "
     + v[i] + " faceCount=" + getFaceCount(i)
     + " edgeCount=" + getEdgeCount(i));
     }
     */
    vertexCounts = v;
  }

  /*
  static int getFaceCount(int level) {
    return faceVertexesArrays[level].length / 3;
  }

  private static int getEdgeCount(int level) {
    return getVertexCount(level) + getFaceCount(level) - 2;
  }

  static short[] getNeighborVertexes(int level) {
    return neighborVertexesArrays[level];
  }
*/

  private static short vertexNext;
  private static Hashtable htVertex;
    
  private final static boolean VALIDATE = true;

  private static void quadruple(int level, short[] counts) {
    htVertex = new Hashtable();
    int oldVertexCount = vertexVectors.length;
    short[] oldFaceVertexes = faceVertexesArrays[level];
    int oldFaceVertexesLength = oldFaceVertexes.length;
    int oldFaceCount = oldFaceVertexesLength / 3;
    int oldEdgesCount = oldVertexCount + oldFaceCount - 2;
    int newVertexCount = oldVertexCount + oldEdgesCount;
    int newFaceCount = 4 * oldFaceCount;
    Vector3f[] newVertexVectors = new Vector3f[newVertexCount];
    System.arraycopy(vertexVectors, 0, newVertexVectors, 0, oldVertexCount);
    vertexVectors = newVertexVectors;

    short[] newFacesVertexes = new short[3 * newFaceCount];
    faceVertexesArrays[level + 1] = newFacesVertexes;
    short[] neighborVertexes = new short[6 * newVertexCount];
    neighborVertexesArrays[level + 1] = neighborVertexes;
    for (int i = neighborVertexes.length; --i >= 0; )
      neighborVertexes[i] = -1;

    counts[level + 1] = (short)newVertexCount;

    vertexNext = (short)oldVertexCount;

    int iFaceNew = 0;
    for (int i = 0; i < oldFaceVertexesLength; ) {
      short iA = oldFaceVertexes[i++];
      short iB = oldFaceVertexes[i++];
      short iC = oldFaceVertexes[i++];
      short iAB = getVertex(iA, iB);
      short iBC = getVertex(iB, iC);
      short iCA = getVertex(iC, iA);
        
      newFacesVertexes[iFaceNew++] = iA;
      newFacesVertexes[iFaceNew++] = iAB;
      newFacesVertexes[iFaceNew++] = iCA;

      newFacesVertexes[iFaceNew++] = iB;
      newFacesVertexes[iFaceNew++] = iBC;
      newFacesVertexes[iFaceNew++] = iAB;

      newFacesVertexes[iFaceNew++] = iC;
      newFacesVertexes[iFaceNew++] = iCA;
      newFacesVertexes[iFaceNew++] = iBC;

      newFacesVertexes[iFaceNew++] = iCA;
      newFacesVertexes[iFaceNew++] = iAB;
      newFacesVertexes[iFaceNew++] = iBC;

      addNeighboringVertexes(neighborVertexes, iAB, iA);
      addNeighboringVertexes(neighborVertexes, iAB, iCA);
      addNeighboringVertexes(neighborVertexes, iAB, iBC);
      addNeighboringVertexes(neighborVertexes, iAB, iB);

      addNeighboringVertexes(neighborVertexes, iBC, iB);
      addNeighboringVertexes(neighborVertexes, iBC, iCA);
      addNeighboringVertexes(neighborVertexes, iBC, iC);

      addNeighboringVertexes(neighborVertexes, iCA, iC);
      addNeighboringVertexes(neighborVertexes, iCA, iA);
    }
    if (VALIDATE) {
      int vertexCount = vertexVectors.length;
      if (iFaceNew != newFacesVertexes.length)
        throw new NullPointerException();
      if (vertexNext != newVertexCount)
        throw new NullPointerException();
      for (int i = 0; i < 12; ++i) {
        for (int j = 0; j < 5; ++j) {
          int neighbor = neighborVertexes[i * 6 + j];
          if (neighbor < 0)
            throw new NullPointerException();
          if (neighbor >= vertexCount)
            throw new NullPointerException();
        if (neighborVertexes[i * 6 + 5] != -1)
          throw new NullPointerException();
        }
      }
      for (int i = 12 * 6; i < neighborVertexes.length; ++i) {
        int neighbor = neighborVertexes[i];
        if (neighbor < 0)
          throw new NullPointerException();
        if (neighbor >= vertexCount)
          throw new NullPointerException();
      }
      for (int i = 0; i < newVertexCount; ++i) {
        int neighborCount = 0;
        for (int j = neighborVertexes.length; --j >= 0; )
          if (neighborVertexes[j] == i)
            ++neighborCount;
        if ((i < 12 && neighborCount != 5) ||
            (i >= 12 && neighborCount != 6))
          throw new NullPointerException();
        int faceCount = 0;
        for (int j = newFacesVertexes.length; --j >= 0; )
          if (newFacesVertexes[j] == i)
            ++faceCount;
        if ((i < 12 && faceCount != 5) ||
            (i >= 12 && faceCount != 6))
          throw new NullPointerException();
      }
    }
    htVertex = null;
  }

  private static void addNeighboringVertexes(short[] neighborVertexes,
                                             short v1, short v2) {
    for (int i = v1 * 6, iMax = i + 6; i < iMax; ++i) {
      if (neighborVertexes[i] == v2)
        return;
      if (neighborVertexes[i] < 0) {
        neighborVertexes[i] = v2;
        for (int j = v2 * 6, jMax = j + 6; j < jMax; ++j) {
          if (neighborVertexes[j] == v1)
            return;
          if (neighborVertexes[j] < 0) {
            neighborVertexes[j] = v1;
            return;
          }
        }
      }
    }
    throw new NullPointerException();
  }

  /*
  short getNeighborVertex(int level, short vertex, int neighborIndex) {
    short[] neighborVertexes = neighborVertexesArrays[level];
    int offset = vertex * 6 + neighborIndex;
    return neighborVertexes[offset];
  }
  */
    
  private static short getVertex(short v1, short v2) {
    if (v1 > v2) {
      short t = v1;
      v1 = v2;
      v2 = t;
    }
    Integer hashKey = Integer.valueOf((v1 << 16) + v2);
    Short iv = (Short)htVertex.get(hashKey);
    if (iv != null)
      return iv.shortValue();
    Vector3f newVertexVector = new Vector3f(vertexVectors[v1]);
    vertexVectors[vertexNext] = newVertexVector;
    newVertexVector.add(vertexVectors[v2]);
    newVertexVector.scale(0.5f);
    newVertexVector.normalize();
    htVertex.put(hashKey, Short.valueOf(vertexNext));
    return vertexNext++;
  }
}
