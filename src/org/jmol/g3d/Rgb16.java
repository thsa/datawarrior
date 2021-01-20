/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-03-28 16:42:36 +0100 (sam., 28 mars 2009) $
 * $Revision: 10745 $
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

final class Rgb16 {
  int rScaled;
  int gScaled;
  int bScaled;
    
  Rgb16() {
  }

  Rgb16(int argb) {
    set(argb);
  }

  void set(int argb) {
    rScaled = ((argb >> 8) & 0xFF00) | 0x80;
    gScaled = ((argb     ) & 0xFF00) | 0x80;
    bScaled = ((argb << 8) & 0xFF00) | 0x80;
  }

  void set(Rgb16 other) {
    rScaled = other.rScaled;
    gScaled = other.gScaled;
    bScaled = other.bScaled;
  }

  void diffDiv(Rgb16 rgb16A, Rgb16 rgb16B, int divisor) {
    rScaled = (rgb16A.rScaled - rgb16B.rScaled) / divisor;
    gScaled = (rgb16A.gScaled - rgb16B.gScaled) / divisor;
    bScaled = (rgb16A.bScaled - rgb16B.bScaled) / divisor;
  }

  /*
  void add(Rgb16 other) {
    rScaled += other.rScaled;
    gScaled += other.gScaled;
    bScaled += other.bScaled;
  }
  */
  
  /*
  void add(Rgb16 base, Rgb16 other) {
    rScaled = base.rScaled + other.rScaled;
    gScaled = base.gScaled + other.gScaled;
    bScaled = base.bScaled + other.bScaled;
  }
  */
  
  void setAndIncrement(Rgb16 base, Rgb16 other) {
    rScaled = base.rScaled;
    base.rScaled += other.rScaled;
    gScaled = base.gScaled;
    base.gScaled += other.gScaled;
    bScaled = base.bScaled;
    base.bScaled += other.bScaled;
  }

  int getArgb() {
    return (                 0xFF000000 |
           ((rScaled << 8) & 0x00FF0000)|
           (gScaled        & 0x0000FF00)|
           (bScaled >> 8));
  }

  public String toString() {
    return (new StringBuffer("Rgb16(")).append(rScaled).append(',')
    .append(gScaled).append(',')
    .append(bScaled).append(" -> ")
    .append((rScaled >> 8) & 0xFF).append(',')
    .append((gScaled >> 8) & 0xFF).append(',')
    .append((bScaled >> 8) & 0xFF).append(')').toString();
  }
}

