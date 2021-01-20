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

import javax.vecmath.Point3i;
import javax.vecmath.Tuple3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.Vector;

import org.jmol.api.JmolRendererInterface;


/**
 *<p>
 * Implementation of hermite curves for drawing smoothed curves
 * that pass through specified points.
 *</p>
 *<p>
 * Examples of usage in Jmol include the commands: <code>trace,
 * ribbons and cartoons</code>.
 *</p>
 *<p>
 * for some useful background info about hermite curves check out
 * <a href='http://www.cubic.org/docs/hermite.htm'>
 * http://www.cubic.org/docs/hermite.htm
 * </a>
 * 
 * Technically, Jmol implements a Cardinal spline varient of the Hermitian spline
 *</p>
 *
 * @author Miguel, miguel@jmol.org
 */
public class Hermite3D {

  /* really a private class to g3d and export3d */
  
  private JmolRendererInterface g3d;

  public Hermite3D(JmolRendererInterface g3d) {
    this.g3d = g3d;
  }

  private final Point3i[] pLeft = new Point3i[16];
  private final Point3i[] pRight = new Point3i[16];

  private final float[] sLeft = new float[16];
  private final float[] sRight = new float[16];
  int sp;

  private final Point3f[] pTopLeft = new Point3f[16];
  private final Point3f[] pTopRight = new Point3f[16];
  private final Point3f[] pBotLeft = new Point3f[16];
  private final Point3f[] pBotRight = new Point3f[16];
  private final boolean[] needToFill = new boolean[16];
  {
    for (int i = 16; --i >= 0; ) {
      pLeft[i] = new Point3i();
      pRight[i] = new Point3i();

      pTopLeft[i] = new Point3f();
      pTopRight[i] = new Point3f();
      pBotLeft[i] = new Point3f();
      pBotRight[i] = new Point3f();
    }
  }

  public void render(boolean tFill, int tension,
                     int diameterBeg, int diameterMid, int diameterEnd,
                     Point3i p0, Point3i p1, Point3i p2, Point3i p3) {
    if (p0.z == 1 ||p1.z == 1 ||p2.z == 1 ||p3.z == 1)
      return;
    if (g3d.isClippedZ(p1.z) || g3d.isClippedZ(p2.z))
      return;
    int x1 = p1.x, y1 = p1.y, z1 = p1.z;
    int x2 = p2.x, y2 = p2.y, z2 = p2.z;
    int xT1 = ((x2 - p0.x) * tension) / 8;
    int yT1 = ((y2 - p0.y) * tension) / 8;
    int zT1 = ((z2 - p0.z) * tension) / 8;
    int xT2 = ((p3.x - x1) * tension) / 8;
    int yT2 = ((p3.y - y1) * tension) / 8;
    int zT2 = ((p3.z - z1) * tension) / 8;
    sLeft[0] = 0;
    pLeft[0].set(p1);
    sRight[0] = 1;
    pRight[0].set(p2);
    sp = 0;
    int n=0;
    int dDiameterFirstHalf = 0;
    int dDiameterSecondHalf = 0;
    if (tFill) {
      dDiameterFirstHalf = 2 * (diameterMid - diameterBeg);
      dDiameterSecondHalf = 2 * (diameterEnd - diameterMid);
    }
    do {
      Point3i a = pLeft[sp];
      Point3i b = pRight[sp];
      int dx = b.x - a.x;
      if (dx >= -1 && dx <= 1) {
        int dy = b.y - a.y;
        if (dy >= -1 && dy <= 1) {
          // mth 2003 10 13
          // I tried drawing short cylinder segments here,
          // but drawing spheres was faster
          n++;
          float s = sLeft[sp];
          if (tFill) {
            int d =(s < 0.5f
                    ? diameterBeg + (int)(dDiameterFirstHalf * s)
                    : diameterMid + (int)(dDiameterSecondHalf * (s - 0.5f)));
            g3d.fillSphereCentered(d, a);
          } else {
            g3d.plotPixelClipped(a);
          }
          --sp;
          continue;
        }
      }
      double s = (sLeft[sp] + sRight[sp]) / 2;
      double s2 = s * s;
      double s3 = s2 * s;
      double h1 = 2*s3 - 3*s2 + 1;
      double h2 = -2*s3 + 3*s2;
      double h3 = s3 - 2*s2 + s;
      double h4 = s3 - s2;
      if (sp >= 15)
        break;
      Point3i pMid = pRight[sp+1];
      pMid.x = (int) (h1*x1 + h2*x2 + h3*xT1 + h4*xT2);
      pMid.y = (int) (h1*y1 + h2*y2 + h3*yT1 + h4*yT2);
      pMid.z = (int) (h1*z1 + h2*z2 + h3*zT1 + h4*zT2);
      pRight[sp+1] = pRight[sp];
      sRight[sp+1] = sRight[sp];
      pRight[sp] = pMid;
      sRight[sp] = (float)s;
      ++sp;
      pLeft[sp].set(pMid);
      sLeft[sp] = (float)s;
    } while (sp >= 0);
  }

  private void render2x(boolean fill, int tension,
                //top strand segment
                Point3i p0, Point3i p1, Point3i p2, Point3i p3,
                //bottom strand segment
                Point3i p4, Point3i p5, Point3i p6, Point3i p7) {
    
    Point3i[] endPoints = {p2, p1, p6, p5};
    // stores all points for top+bottom strands of 1 segment
    Vector points = new Vector(10);
    int whichPoint = 0;

    int numTopStrandPoints = 2; //first and last points automatically included
    float numPointsPerSegment = 5.0f;//use 5 for mesh

    // could make it so you can set this from script command
    if (fill)
      numPointsPerSegment = 10.0f;


    float interval = (1.0f / numPointsPerSegment);
    float currentInt = 0.0f;

    int x1 = p1.x, y1 = p1.y, z1 = p1.z;
    int x2 = p2.x, y2 = p2.y, z2 = p2.z;
    int xT1 = ( (x2 - p0.x) * tension) / 8;
    int yT1 = ( (y2 - p0.y) * tension) / 8;
    int zT1 = ( (z2 - p0.z) * tension) / 8;
    int xT2 = ( (p3.x - x1) * tension) / 8;
    int yT2 = ( (p3.y - y1) * tension) / 8;
    int zT2 = ( (p3.z - z1) * tension) / 8;
    sLeft[0] = 0;
    pLeft[0].set(p1);
    sRight[0] = 1;
    pRight[0].set(p2);
    sp = 0;

    for (int strands = 2; strands > 0; strands--) {
       if (strands == 1) {
         x1 = p5.x; y1 = p5.y; z1 = p5.z;
         x2 = p6.x; y2 = p6.y; z2 = p6.z;
         xT1 = ( (x2 - p4.x) * tension) / 8;
         yT1 = ( (y2 - p4.y) * tension) / 8;
         zT1 = ( (z2 - p4.z) * tension) / 8;
         xT2 = ( (p7.x - x1) * tension) / 8;
         yT2 = ( (p7.y - y1) * tension) / 8;
         zT2 = ( (p7.z - z1) * tension) / 8;
         sLeft[0] = 0;
         pLeft[0].set(p5);
         sRight[0] = 1;
         pRight[0].set(p6);
         sp = 0;
       }

       points.addElement(endPoints[whichPoint++]);
       currentInt = interval;
       do {
         Point3i a = pLeft[sp];
         Point3i b = pRight[sp];
         int dx = b.x - a.x;
         int dy = b.y - a.y;
         int dist2 = dx * dx + dy * dy;
         if (dist2 <= 2) {
           // mth 2003 10 13
           // I tried drawing short cylinder segments here,
           // but drawing spheres was faster
           float s = sLeft[sp];

           g3d.fillSphereCentered(3, a);
           //draw outside edges of mesh

           if (s < 1.0f - currentInt) { //if first point over the interval
             Point3i temp = new Point3i();
             temp.set(a);
             points.addElement(temp); //store it
             currentInt += interval; // increase to next interval
             if (strands == 2) {
               numTopStrandPoints++;
             }
           }
           --sp;
         }
         else {
           double s = (sLeft[sp] + sRight[sp]) / 2;
           double s2 = s * s;
           double s3 = s2 * s;
           double h1 = 2 * s3 - 3 * s2 + 1;
           double h2 = -2 * s3 + 3 * s2;
           double h3 = s3 - 2 * s2 + s;
           double h4 = s3 - s2;
           if (sp >= 15)
             break;
           Point3i pMid = pRight[sp + 1];
           pMid.x = (int) (h1 * x1 + h2 * x2 + h3 * xT1 + h4 * xT2);
           pMid.y = (int) (h1 * y1 + h2 * y2 + h3 * yT1 + h4 * yT2);
           pMid.z = (int) (h1 * z1 + h2 * z2 + h3 * zT1 + h4 * zT2);
           pRight[sp + 1] = pRight[sp];
           sRight[sp + 1] = sRight[sp];
           pRight[sp] = pMid;
           sRight[sp] = (float) s;
           ++sp;
           pLeft[sp].set(pMid);
           sLeft[sp] = (float) s;
         }
       } while (sp >= 0);
       points.addElement(endPoints[whichPoint++]);
     } //end of for loop - processed top and bottom strands
     int size = points.size();
     if (fill) {//RIBBONS
       Point3i t1 = null;
       Point3i b1 = null;
       Point3i t2 = null;
       Point3i b2 = null;
       int top = 1;
       for (;top < numTopStrandPoints && (top + numTopStrandPoints) < size; top++) {
         t1 = (Point3i) points.elementAt(top - 1);
         b1 = (Point3i) points.elementAt(numTopStrandPoints + (top - 1));
         t2 = (Point3i) points.elementAt(top);
         b2 = (Point3i) points.elementAt(numTopStrandPoints + top);

         g3d.fillTriangle(t1, b1, t2);
         g3d.fillTriangle(b2, t2, b1);
       }
       if((numTopStrandPoints*2) != size){//BUG(DC09_MAY_2004): not sure why but
         //sometimes misses triangle at very start of segment
         //temp fix - will inestigate furture
         g3d.fillTriangle(p1, p5, t2);
         g3d.fillTriangle(b2, t2, p5);
       }
     }
     else {//MESH
       for (int top = 0;
            top < numTopStrandPoints && (top + numTopStrandPoints) < size; top++) {
       g3d.drawLine((Point3i) points.elementAt(top),
                    (Point3i) points.elementAt(top + numTopStrandPoints));
     }}

  }

  private static void set(Point3f p3f, Point3i p3i) {
    p3f.x = p3i.x;
    p3f.y = p3i.y;
    p3f.z = p3i.z;
  }

  
  private final Point3f a1 = new Point3f();
  private final Point3f a2 = new Point3f();
  private final Point3f b1 = new Point3f();
  private final Point3f b2 = new Point3f();
  private final Point3f c1 = new Point3f();
  private final Point3f c2 = new Point3f();
  private final Point3f d1 = new Point3f();
  private final Point3f d2 = new Point3f();
  private final Vector3f depth1 = new Vector3f();
  
  public void render2(boolean fill, boolean border, int tension,
                      //top strand segment
                      Point3i p0, Point3i p1, Point3i p2, Point3i p3,
                      //bottom strand segment
                      Point3i p4, Point3i p5, Point3i p6, Point3i p7,
                      int aspectRatio) {
    if (p0.z == 1 ||p1.z == 1 ||p2.z == 1 ||p3.z == 1 ||p4.z == 1 ||p5.z == 1 ||p6.z == 1 ||p7.z == 1)
      return;
    if (!fill) {
      render2x(fill, tension, p0, p1, p2, p3, p4, p5, p6, p7);
      return;
    }
    float ratio = 1f / aspectRatio;
    int x1 = p1.x, y1 = p1.y, z1 = p1.z;
    int x2 = p2.x, y2 = p2.y, z2 = p2.z;
    int xT1 = ((x2 - p0.x) * tension) / 8;
    int yT1 = ((y2 - p0.y) * tension) / 8;
    int zT1 = ((z2 - p0.z) * tension) / 8;
    int xT2 = ((p3.x - x1) * tension) / 8;
    int yT2 = ((p3.y - y1) * tension) / 8;
    int zT2 = ((p3.z - z1) * tension) / 8;
    set(pTopLeft[0], p1);
    set(pTopRight[0], p2);

    int x5 = p5.x, y5 = p5.y, z5 = p5.z;
    int x6 = p6.x, y6 = p6.y, z6 = p6.z;
    int xT5 = ((x6 - p4.x) * tension) / 8;
    int yT5 = ((y6 - p4.y) * tension) / 8;
    int zT5 = ((z6 - p4.z) * tension) / 8;
    int xT6 = ((p7.x - x5) * tension) / 8;
    int yT6 = ((p7.y - y5) * tension) / 8;
    int zT6 = ((p7.z - z5) * tension) / 8;
    set(pBotLeft[0], p5);
    set(pBotRight[0], p6);

    sLeft[0] = 0;
    sRight[0] = 1;
    needToFill[0] = true;
    sp = 0;
    boolean closeEnd = false;
    do {
      Point3f a = pTopLeft[sp];
      Point3f b = pTopRight[sp];
      double dxTop = b.x - a.x;
      double dxTop2 = dxTop * dxTop;
      if (dxTop2 < 10) {
        double dyTop = b.y - a.y;
        double dyTop2 = dyTop * dyTop;
        if (dyTop2 < 10) {
          Point3f c = pBotLeft[sp];
          Point3f d = pBotRight[sp];
          double dxBot = d.x - c.x;
          double dxBot2 = dxBot * dxBot;
          if (dxBot2 < 8) {
            double dyBot = d.y - c.y;
            double dyBot2 = dyBot * dyBot;
            if (dyBot2 < 8) {
              if (border) {
                g3d.fillSphereCentered(3, a);
                g3d.fillSphereCentered(3, c);
              }

              if (needToFill[sp]) {
                if(aspectRatio > 0) {
                  setDepth(depth1, c, a, b, ratio);
                  setPoint(a1, a, depth1, 1);
                  setPoint(a2, a, depth1, -1);
                  setPoint(b1, b, depth1, 1);
                  setPoint(b2, b, depth1, -1);
                  setPoint(c1, c, depth1, 1);
                  setPoint(c2, c, depth1, -1);
                  setPoint(d1, d, depth1, 1);
                  setPoint(d2, d, depth1, -1);
                  g3d.fillQuadrilateral(a1, b1, d1, c1);
                  g3d.fillQuadrilateral(a2, b2, d2, c2);
                  g3d.fillQuadrilateral(a1, b1, b2, a2);
                  g3d.fillQuadrilateral(c1, d1, d2, c2);
                  closeEnd = true;
                } else {
                  g3d.fillQuadrilateral(a, b, d, c);
                }
                needToFill[sp] = false;
              }
              if (dxTop2 + dyTop2 < 2 && dxBot2 + dyBot2 < 2) {
                --sp;
                continue;
              }
            }
          }
        }
      }
      double s = (sLeft[sp] + sRight[sp]) / 2;
      double s2 = s * s;
      double s3 = s2 * s;
      double h1 = 2 * s3 - 3 * s2 + 1;
      double h2 = -2 * s3 + 3 * s2;
      double h3 = s3 - 2 * s2 + s;
      double h4 = s3 - s2;

      if (sp >= 15)
        break;
      int spNext = sp + 1;
      Point3f pMidTop = pTopRight[spNext];
      pMidTop.x = (float) (h1 * x1 + h2 * x2 + h3 * xT1 + h4 * xT2);
      pMidTop.y = (float) (h1 * y1 + h2 * y2 + h3 * yT1 + h4 * yT2);
      pMidTop.z = (float) (h1 * z1 + h2 * z2 + h3 * zT1 + h4 * zT2);
      Point3f pMidBot = pBotRight[spNext];
      pMidBot.x = (float) (h1 * x5 + h2 * x6 + h3 * xT5 + h4 * xT6);
      pMidBot.y = (float) (h1 * y5 + h2 * y6 + h3 * yT5 + h4 * yT6);
      pMidBot.z = (float) (h1 * z5 + h2 * z6 + h3 * zT5 + h4 * zT6);

      pTopRight[spNext] = pTopRight[sp];
      pTopRight[sp] = pMidTop;
      pBotRight[spNext] = pBotRight[sp];
      pBotRight[sp] = pMidBot;

      sRight[spNext] = sRight[sp];
      sRight[sp] = (float) s;
      needToFill[spNext] = needToFill[sp];
      pTopLeft[spNext].set(pMidTop);
      pBotLeft[spNext].set(pMidBot);
      sLeft[spNext] = (float) s;
      ++sp;
    } while (sp >= 0);
    if (closeEnd) {
      a1.z += 1;
      c1.z += 1;
      c2.z += 1;
      a2.z += 1;
      g3d.fillQuadrilateral(a1, c1, c2, a2);
    }
  }
 
  private final Vector3f T1 = new Vector3f();
  private final Vector3f T2 = new Vector3f();
  private void setDepth(Vector3f depth, Point3f c, Point3f a, Point3f b, float ratio) {
    T1.sub(a, c);
    T1.scale(ratio);
    T2.sub(a, b);
    depth.cross(T1, T2);
    depth.scale(T1.length() / depth.length());
  }
  
  private void setPoint(Point3f a1, Point3f a, Vector3f depth, int direction) {
    a1.set(a);
    if (direction == 1)
      a1.add(depth);
    else
      a1.sub(depth);
  }
  
  static void getHermiteList(int tension, Tuple3f p0, Tuple3f p1, Tuple3f p2, Tuple3f p3, Tuple3f p4, Tuple3f[] list, int index0, int n) {
    //always deliver ONE MORE than one might expect, to provide a normal
    int nPoints = n + 1;
    float fnPoints = n - 1;
    float x1 = p1.x, y1 = p1.y, z1 = p1.z;
    float x2 = p2.x, y2 = p2.y, z2 = p2.z;    
    float xT1 = ((x2 - p0.x) * tension) / 8;
    float yT1 = ((y2 - p0.y) * tension) / 8;
    float zT1 = ((z2 - p0.z) * tension) / 8;
    float xT2 = ((p3.x - x1) * tension) / 8;
    float yT2 = ((p3.y - y1) * tension) / 8;
    float zT2 = ((p3.z - z1) * tension) / 8;
    float xT3 = ((p4.x - x2) * tension) / 8;
    float yT3 = ((p4.y - y2) * tension) / 8;
    float zT3 = ((p4.z - z2) * tension) / 8;
    list[index0] = p1;
    for (int i = 0; i < nPoints; i++) {
      double s = i / fnPoints;
      if (i == nPoints - 1) {
        x1 = x2; y1 = y2; z1 = z2;
        x2 = p3.x; y2 = p3.y; z2 = p3.z;
        xT1 = xT2; yT1 = yT2; zT1 = zT2;
        xT2 = xT3; yT2 = yT3; zT2 = zT3;
        s -= 1;
      }
      double s2 = s * s;
      double s3 = s2 * s;
      double h1 = 2*s3 - 3*s2 + 1;
      double h2 = -2*s3 + 3*s2;
      double h3 = s3 - 2*s2 + s;
      double h4 = s3 - s2;
      float x = (float)(h1*x1 + h2*x2 + h3*xT1 + h4*xT2);
      float y = (float) (h1*y1 + h2*y2 + h3*yT1 + h4*yT2);
      float z = (float) (h1*z1 + h2*z2 + h3*zT1 + h4*zT2);
      if (list instanceof Point3f[])
        list[index0 + i] = new Point3f(x, y, z);
      else
        list[index0 + i] = new Vector3f(x, y, z);
   }
  }
}
