/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap;

import java.util.Arrays;

/**
 * A form of sparse matrix where only non-zero entries are explicitly recorded.
 * Three arrays (<code>row, col, data</code>) of equal length hold coordinates and value of non-zero entries, respectively.
 * Data is stored in a sorted order to fast searching.
 * This format is compatible with the Python scipy <code>coo_matrix</code> format.
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class CooMatrix extends Matrix {
  private final int[] mRow;
  private final int[] mCol;
  private final float[] mData;

  private static CooMatrix createWithTruncate(final float[] d, final int[] r, final int[] c, final int rows, final int cols, final int len) {
    return len == r.length
      ? new CooMatrix(d, r, c, rows, cols)
      : new CooMatrix(Arrays.copyOf(d, len), Arrays.copyOf(r, len), Arrays.copyOf(c, len), rows, cols);
  }

  CooMatrix(final float[] vals, final int[] rows, final int[] cols, final int rowCount, final int colCount) {
    super(rowCount, colCount);
    if (rows.length != cols.length || rows.length != vals.length) {
      throw new IllegalArgumentException();
    }
    mRow = rows;
    mCol = cols;
    mData = vals;
    sort(0, rows.length);
    checkDataValid();
  }

  private void checkDataValid() {
    for (int r : mRow) {
      if (r < 0 || r >= rows()) {
        throw new IllegalArgumentException("Row index out of bounds: 0 <= " + r + " < " + rows());
      }
    }
    for (int c : mCol) {
      if (c < 0 || c >= cols()) {
        throw new IllegalArgumentException("Column index out of bounds: 0 <= " + c + " < " + cols());
      }
    }
    for (int i = 1; i < mRow.length; ++i) {
      if (compare(i, i - 1) == 0) {
        throw new IllegalArgumentException("Duplicated array position: row " + mRow[i] + ", col " + mCol[i]);
      }
    }
  }

  private void swap(final int a, final int b) {
      final int t = mRow[a];
      mRow[a] = mRow[b];
      mRow[b] = t;
      final int u = mCol[a];
      mCol[a] = mCol[b];
      mCol[b] = u;
      final float v = mData[a];
      mData[a] = mData[b];
      mData[b] = v;
  }

  private int compare(final int i, final int j) {
    return compare(i, mRow[j], mCol[j]);
  }

  private int compare(final int i, final int r, final int c) {
    int res = Integer.compare(mRow[i], r);
    if (res == 0) {
      res = Integer.compare(mCol[i], c);
    }
    return res;
  }

  private void sort(final int off, final int len) {
    // Insertion sort on smallest arrays
    if (len < 7) {
      for (int i = off; i < len + off; ++i) {
        for (int j = i; j > off && compare(j - 1, j) > 0; --j) {
          swap(j, j - 1);
        }
      }
      return;
    }

    // Choose a partition element, v
    int m = off + (len >> 1); // Small arrays, middle element
    if (len != 7) {
      int l = off;
      int n = off + len - 1;
      if (len > 40) { // Big arrays, pseudomedian of 9
        final int s = len / 8;
        l = med3(l, l + s, l + 2 * s);
        m = med3(m - s, m, m + s);
        n = med3(n - 2 * s, n - s, n);
      }
      m = med3(l, m, n); // Mid-size, med of 3
    }
    final int vr = mRow[m];
    final int vc = mCol[m];

    // Establish Invariant: v* (<v)* (>v)* v*
    int a = off;
    int b = a;
    int c = off + len - 1;

    // Establish Invariant: v* (<v)* (>v)* v*
    int d = c;
    while (true) {
      while (b <= c && compare(b, vr, vc) <= 0) {
        if (compare(b, vr, vc) >= 0) {
          swap(a++, b);
        }
        ++b;
      }
      while (c >= b && compare(c, vr, vc) >= 0) {
        if (compare(c, vr, vc) <= 0) {
          swap(c, d--);
        }
        --c;
      }
      if (b > c) {
        break;
      }
      swap(b++, c--);
    }

    // Swap partition elements back to middle
    int s2;

    // Swap partition elements back to middle
    final int n2 = off + len;
    s2 = Math.min(a - off, b - a);
    vecswap(off, b - s2, s2);
    s2 = Math.min(d - c, n2 - d - 1);
    vecswap(b, n2 - s2, s2);

    // Recursively sort non-partition-elements
    if ((s2 = b - a) > 1) {
      sort(off, s2);
    }
    if ((s2 = d - c) > 1) {
      sort(n2 - s2, s2);
    }
  }

  private void vecswap(final int aa, final int bb, final int n) {
    for (int i = 0, a = aa, b = bb; i < n; ++i, ++a, ++b) {
      swap(a, b);
    }
  }

  int med3(final int a, final int b, final int c) {
    final int ab = compare(a, b);
    final int ac = compare(a, c);
    final int bc = compare(b, c);

    return ab < 0
      ? (bc < 0 ? b : ac < 0 ? c : a)
      : (bc > 0 ? b : ac > 0 ? c : a);
  }

  float[] data() {
    return Arrays.copyOf(mData, mData.length);
  }

  int[] row() {
    return Arrays.copyOf(mRow, mRow.length);
  }

  int[] col() {
    return Arrays.copyOf(mCol, mCol.length);
  }

  @Override
  float get(final int r, final int c) {
    int left = 0;
    int right = mRow.length - 1;
    while (left <= right) {
      final int mid = left + (right - left) / 2;
      // Check if x is present at mid
      if (mRow[mid] == r) {
        if (mCol[mid] == c) {
          return mData[mid];
        }
        if (mCol[mid] < c) {
          left = mid + 1;
          // If x is smaller, ignore right half
        } else {
          right = mid - 1;
        }
      } else {
        // If x greater, ignore left half
        if (mRow[mid] < r) {
          left = mid + 1;
          // If x is smaller, ignore right half
        } else {
          right = mid - 1;
        }
      }
    }
    return 0;
  }

  @Override
  void set(final int row, final int col, final float val) {
    throw new UnsupportedOperationException();
  }

  @Override
  boolean isFinite() {
    for (final float v : mData) {
      if (!Float.isFinite(v)) {
        return false;
      }
    }
    return true;
  }

  @Override
  Matrix copy() {
    return new CooMatrix(Arrays.copyOf(mData, mData.length), Arrays.copyOf(mRow, mRow.length), Arrays.copyOf(mCol, mCol.length), rows(), cols());
  }

  @Override
  Matrix transpose() {
    return new CooMatrix(Arrays.copyOf(mData, mData.length), Arrays.copyOf(mCol, mCol.length), Arrays.copyOf(mRow, mRow.length), cols(), rows());
  }

  @Override
  CooMatrix toCoo() {
    return this;
  }

  CsrMatrix toCsr() {
    final int[] indptr = new int[rows() + 1];
    int r = -1;
    for (int k = 0; k < mData.length; ++k) {
      while (mRow[k] > r) {
        indptr[++r] = k;
      }
    }
    indptr[rows()] = mData.length;
    // todo can we drop these copies?
    return new CsrMatrix(Arrays.copyOf(mData, mData.length), indptr, Arrays.copyOf(mCol, mCol.length), rows(), cols());
  }

  @Override
  Matrix eliminateZeros() {
    int zeros = 0;
    for (final float v : mData) {
      if (v == 0) {
        ++zeros;
      }
    }
    if (zeros > 0) {
      final int[] r = new int[mRow.length - zeros];
      final int[] c = new int[mRow.length - zeros];
      final float[] d = new float[mRow.length - zeros];
      for (int k = 0, j = 0; k < mData.length; ++k) {
        if (mData[k] != 0) {
          r[j] = mRow[k];
          c[j] = mCol[k];
          d[j++] = mData[k];
        }
      }
      return new CooMatrix(d, r, c, rows(), cols());
    } else {
      return this;
    }
  }

  private int getMaxNonZeroSize() {
    final long mnz = Math.min(rows() * (long) cols(), 2L * mRow.length);
    if (mnz > Integer.MAX_VALUE) {
      throw new UnsupportedOperationException("Implementation limits exceeded");
    }
    return (int) mnz;
  }

  @Override
  Matrix add(final Matrix matrix) {
    if (!(matrix instanceof CooMatrix)) {
      return super.add(matrix).toCoo();
    }
    final CooMatrix m = (CooMatrix) matrix;
    if (!isShapeSame(m)) {
      throw new IllegalArgumentException("Incompatible matrix sizes");
    }
    final int maxNonZero = getMaxNonZeroSize();
    final int[] r = new int[maxNonZero];
    final int[] c = new int[maxNonZero];
    final float[] d = new float[maxNonZero];
    int i = 0;
    int j = 0;
    int k = 0;
    while (i < mData.length && j < m.mData.length) {
      final int ri = mRow[i];
      final int rj = m.mRow[j];
      if (ri < rj) {
        r[k] = ri;
        c[k] = mCol[i];
        d[k++] = mData[i++];
      } else if (rj < ri) {
        r[k] = rj;
        c[k] = m.mCol[j];
        d[k++] = -m.mData[j++];
      } else {
        assert ri == rj;
        final int ci = mCol[i];
        final int cj = m.mCol[j];
        if (ci < cj) {
          r[k] = ri;
          c[k] = ci;
          d[k++] = mData[i++];
        } else if (cj < ci) {
          r[k] = rj;
          c[k] = cj;
          d[k++] = m.mData[j++];
        } else {
          assert ci == cj;
          final float s = mData[i++] + m.mData[j++];
          if (s != 0) {
            r[k] = rj;
            c[k] = cj;
            d[k++] = s;
          }
        }
      }
    }
    // Handle any remaining entries
    while (i < mData.length) {
      r[k] = mRow[i];
      c[k] = mCol[i];
      d[k++] = mData[i++];
    }
    while (j < m.mData.length) {
      r[k] = m.mRow[j];
      c[k] = m.mCol[j];
      d[k++] = m.mData[j++];
    }
    return createWithTruncate(d, r, c, rows(), cols(), k);
  }

  @Override
  Matrix subtract(final Matrix matrix) {
    if (!(matrix instanceof CooMatrix)) {
      return super.subtract(matrix).toCoo();
    }
    final CooMatrix m = (CooMatrix) matrix;
    if (!isShapeSame(m)) {
      throw new IllegalArgumentException("Incompatible matrix sizes");
    }
    final int maxNonZero = getMaxNonZeroSize();
    final int[] r = new int[maxNonZero];
    final int[] c = new int[maxNonZero];
    final float[] d = new float[maxNonZero];
    int i = 0;
    int j = 0;
    int k = 0;
    while (i < mData.length && j < m.mData.length) {
      final int ri = mRow[i];
      final int rj = m.mRow[j];
      if (ri < rj) {
        r[k] = ri;
        c[k] = mCol[i];
        d[k++] = mData[i++];
      } else if (rj < ri) {
        r[k] = rj;
        c[k] = m.mCol[j];
        d[k++] = -m.mData[j++];
      } else {
        assert ri == rj;
        final int ci = mCol[i];
        final int cj = m.mCol[j];
        if (ci < cj) {
          r[k] = ri;
          c[k] = ci;
          d[k++] = mData[i++];
        } else if (cj < ci) {
          r[k] = rj;
          c[k] = cj;
          d[k++] = -m.mData[j++];
        } else {
          assert ci == cj;
          final float s = mData[i++] - m.mData[j++];
          if (s != 0) {
            r[k] = rj;
            c[k] = cj;
            d[k++] = s;
          }
        }
      }
    }
    // Handle any remaining entries
    while (i < mData.length) {
      r[k] = mRow[i];
      c[k] = mCol[i];
      d[k++] = mData[i++];
    }
    while (j < m.mData.length) {
      r[k] = m.mRow[j];
      c[k] = m.mCol[j];
      d[k++] = -m.mData[j++];
    }
    return createWithTruncate(d, r, c, rows(), cols(), k);
  }

  @Override
  Matrix hadamardMultiply(final Matrix matrix) {
    if (!(matrix instanceof CooMatrix)) {
      return super.hadamardMultiply(matrix).toCoo();
    }
    final CooMatrix m = (CooMatrix) matrix;
    if (!isShapeSame(m)) {
      throw new IllegalArgumentException("Incompatible matrix sizes");
    }
    final int maxNonZero = Math.max(mRow.length, m.mRow.length);
    final int[] r = new int[maxNonZero];
    final int[] c = new int[maxNonZero];
    final float[] d = new float[maxNonZero];
    int i = 0;
    int j = 0;
    int k = 0;
    while (i < mData.length && j < m.mData.length) {
      final int ri = mRow[i];
      final int rj = m.mRow[j];
      if (ri < rj) {
        ++i;
      } else if (rj < ri) {
        ++j;
      } else {
        assert ri == rj;
        final int ci = mCol[i];
        final int cj = m.mCol[j];
        if (ci < cj) {
          ++i;
        } else if (cj < ci) {
          ++j;
        } else {
          assert ci == cj;
          final float s = mData[i++] * m.mData[j++];
          if (s != 0) {
            r[k] = rj;
            c[k] = cj;
            d[k++] = s;
          }
        }
      }
    }
    return createWithTruncate(d, r, c, rows(), cols(), k);
  }

  @Override
  Matrix hadamardMultiplyTranspose() {
    if (rows() != cols()) {
      throw new IllegalArgumentException("Incompatible matrix sizes");
    }
    // This product cannot have more non-zero entries than the input
    final int[] r = new int[mRow.length];
    final int[] c = new int[mCol.length];
    final float[] d = new float[mData.length];
    int j = 0;
    for (int k = 0; k < mRow.length; ++k) {
      final float v = mData[k] * get(mCol[k], mRow[k]);
      if (v != 0) {
        r[j] = mRow[k];
        c[j] = mCol[k];
        d[j++] = v;
      }
    }
    return createWithTruncate(d, r, c, rows(), cols(), j);
  }

  @Override
  Matrix addTranspose() {
    if (rows() != cols()) {
      throw new IllegalArgumentException("Incompatible matrix sizes");
    }
    final int maxNonZero = getMaxNonZeroSize();
    final int[] r = new int[maxNonZero];
    final int[] c = new int[maxNonZero];
    final float[] d = new float[maxNonZero];
    int j = 0;
    for (int k = 0; k < mRow.length; ++k) {
      final int rk = mRow[k];
      final int ck = mCol[k];
      final float tk = get(ck, rk);
      final float v = mData[k] + tk;
      if (v != 0) {
        r[j] = rk;
        c[j] = ck;
        d[j++] = v;
        if (rk != ck && tk == 0) {
          r[j] = ck;
          c[j] = rk;
          d[j++] = v;
        }
      }
    }
    return createWithTruncate(d, r, c, rows(), cols(), j);
  }

  @Override
  Matrix multiply(final Matrix m) {
    if (!(m instanceof CooMatrix)) {
      return super.multiply(m).toCoo();
    }
    final CooMatrix a = (CooMatrix) m;
    if (cols() != m.rows()) {
      throw new IllegalArgumentException("Incompatible matrix sizes");
    }
    final int rows = rows();
    final int cols = m.cols();
    final float[][] res = new float[rows][cols];
    for (int k = 0; k < mData.length; ++k) {
      final int r = mRow[k];
      final int c = mCol[k];
      for (int j = 0; j < a.mData.length; ++j) {
        if (a.mRow[j] == c) {
          res[r][a.mCol[j]] += mData[k] * a.mData[j];
        }
      }
    }
    return new DefaultMatrix(res).toCoo();
  }

  @Override
  Matrix multiply(final float x) {
    final float[] newData = new float[mData.length];
    for (int i = 0; i < newData.length; ++i) {
      newData[i] = mData[i] * x;
    }
    return new CooMatrix(newData, mRow, mCol, rows(), cols());
  }

  String sparseToString() {
    final StringBuilder sb = new StringBuilder();
    for (int k = 0; k < mData.length; ++k) {
      sb.append('(').append(mRow[k]).append(", ").append(mCol[k]).append(") ").append(mData[k]).append('\n');
    }
    return sb.toString();
  }

  @Override
  Matrix rowNormalize() {
    final float[] d = new float[mData.length];
    int k = 0;
    int row = -1;
    float max = 0;
    while (k < mRow.length) {
      if (mRow[k] != row) {
        // Moving to a new row, compute max
        row = mRow[k];
        max = mData[k];
        for (int j = k + 1; j < mRow.length && mRow[j] == mRow[k]; ++j) {
          max = Math.max(max, mData[j]);
        }
      }
      if (max != 0) {
        d[k] = mData[k] / max;
      }
      ++k;
    }
    return new CooMatrix(d, Arrays.copyOf(mRow, mRow.length), Arrays.copyOf(mCol, mCol.length), rows(), cols());
  }


  /**
   * Under the assumption of categorical distance for the intersecting
   * simplicial set perform a fast intersection.
   * @param target (array of shape <code>nSamples</code>) The categorical labels to use in the intersection.
   * @param unknownDist The distance an unknown label (-1) is assumed to be from any point.
   * @param farDist The distance between unmatched labels.
   */
  void fastIntersection(final float[] target, final float unknownDist, final float farDist) {
    for (int nz = 0; nz < mRow.length; ++nz) {
      final int i = mRow[nz];
      final int j = mCol[nz];
      if (target[i] == -1 || target[j] == -1) {
        mData[nz] *= Math.exp(-unknownDist);
      } else if (target[i] != target[j]) {
        mData[nz] *= Math.exp(-farDist);
      }
    }
  }

}
