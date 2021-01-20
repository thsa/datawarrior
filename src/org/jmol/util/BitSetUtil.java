/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
package org.jmol.util;

import java.util.BitSet;

final public class BitSetUtil {

  /**
   * length = "last set bit plus one"
   * 
   * @param bs
   * @return the index of the last set bit
   */
  public static int length(BitSet bs) {
    // there are reports of bugs in some Java compilers 
    // not properly handling size(), but that's not an issue here
    // because all we need is a lower limit of the upper limit,
    // so to speak. If we are a little over, that's not a problem.
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4213570
    int n = bs.size();
    while (--n >= 0)
      if (bs.get(n))
        break;
    return n + 1;
  }

  public static int firstSetBit(BitSet bs) {
    int n;
    if (bs == null || (n = bs.size()) == 0)
      return -1;
    for (int i = 0; i < n; i++)
      if (bs.get(i))
        return i;
    return -1;
  }

  /**
   * cardinality = "total number of set bits"
   * @param bs
   * @return number of set bits
   */
  public static int cardinalityOf(BitSet bs) {
    int n = 0;
    if (bs != null)
      for (int i = bs.size(); --i >= 0;)
        if (bs.get(i))
          n++;
    return n;
  }

  public static BitSet setAll(int n) {
    BitSet bs = new BitSet(n);
    for (int i = n; --i >= 0;)
      bs.set(i);
    return bs;
  }

  public static void andNot(BitSet a, BitSet b) {
    if (b == null)
      return;
    for (int i = b.size(); --i >= 0;)
      if (b.get(i))
        a.clear(i);
  }

  public static BitSet copy(BitSet bs) {
    return bs == null ? null : (BitSet) bs.clone();
    /*
    BitSet b = new BitSet();
    b.or(a);
    return b;
    */
  }

  public static void copy(BitSet a, BitSet b) {
    b.clear();
    b.or(a);
  }
  
  /*
   * same as BitSet.clear(), except this preserves the
   * overall number of bytes of storage (probably)
   * 
   */
  private final static BitSet bsNull = new BitSet();
  public static void clear(BitSet bs) {
    bs.and(bsNull);
  }

  public static BitSet copyInvert(BitSet bs, int n) {
    if (bs == null)
      return null;
    BitSet allButN = setAll(n);
    andNot(allButN, bs);
    return allButN;
  }
  
  /**
   * inverts the bitset bits 0 through n-1, 
   * and returns a reference to the modified bitset
   * 
   * @param bs
   * @param n
   * @return  pointer to original bitset, now inverted
   */
  public static BitSet invertInPlace(BitSet bs, int n) {
    for (int i = n; --i >= 0;) {
      if (bs.get(i))
        bs.clear(i);
      else
        bs.set(i);
    }
    return bs;
  }

  /**
   * a perhaps curious method: 
   * 
   * b is a reference set, perhaps all atoms in a certain molecule
   * a is the working set, perhaps representing all displayed atoms
   * 
   * For each set bit in b: 
   *   a) if a is also set, then clear a's bit UNLESS
   *   b) if a is not set, then add to a all set bits of b
   *   
   * In "toggle" mode, when you click on any atom of the molecule, 
   * you want either:
   * 
   * (a) all the atoms in the molecule to be displayed if not
   *     all are already displayed,
   * or
   * 
   * (b) the whole molecule to be hidden if all the atoms of the 
   *     molecule are already displayed.
   *          
   * @param a
   * @param b
   * @param n
   * @return  a handy pointer to the working set, a
   */
  public static BitSet toggleInPlace(BitSet a, BitSet b, int n) {
    for (int i = n; --i >= 0;) {
      if (!b.get(i))
        continue;
      if (a.get(i)) { //both set --> clear a
        a.clear(i);
      } else {
        a.or(b); //a is not set --> set all of b's bits on in a
        break;
      }
    }
    return a;
  }
  
  public static void deleteBits(BitSet bs, BitSet bsDelete) {
    if (bs == null || bsDelete == null)
      return;
    int ipt = firstSetBit(bsDelete);
    if (ipt < 0)
      return;
    int len = bs.size();
    int lend = Math.min(len, bsDelete.size());
    for (int i = ipt; i < lend; i++)
      if (!bsDelete.get(i))
        bs.set(ipt++, bs.get(i));
    for (; ipt < len; ipt++)
      bs.clear(ipt);
  }
  
  public static boolean compareBits(BitSet a, BitSet b) {
    if (a == null || b == null)
      return a == null && b == null;
    for (int i = Math.max(a.size(), b.size()); --i >= 0; )
      if (a.get(i) != b.get(i))
        return false;
    return true;
  }
}
