/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap;

import java.util.Arrays;

/**
 * Default matrix implementation backed by a square matrix.
 * @author Sean A. Irvine
 * @author Richard Littin
 */
public class DefaultMatrix extends Matrix {

  private final float[][] mData;

  /**
   * Construct a matrix backed by the given array.  Note the array is NOT copied, so that
   * any external changes to the underlying array will affect that matrix as well.
   * @param matrix matrix values
   */
  DefaultMatrix(final float[][] matrix) {
    super(matrix.length, matrix[0].length);
    mData = matrix;
  }

  /**
   * Construct a new zero matrix of specified dimensions.
   * @param rows number of rows
   * @param cols number of columns
   */
  DefaultMatrix(final int rows, final int cols) {
    this(new float[rows][cols]);
  }

  @Override
  float get(final int row, final int col) {
    return mData[row][col];
  }

  @Override
  void set(final int row, final int col, final float val) {
    mData[row][col] = val;
  }

  @Override
  boolean isFinite() {
    for (final float[] row : mData) {
      for (final float v : row) {
        if (!Float.isFinite(v)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  Matrix copy() {
    final float[][] copy = new float[mData.length][];
    for (int k = 0; k < copy.length; ++k) {
      copy[k] = Arrays.copyOf(mData[k], mData[k].length);
    }
    return new DefaultMatrix(copy);
  }

  @Override
  float[][] toArray() {
    return mData;
  }

  @Override
  float[] row(int row) {
    return mData[row];
  }

  @Override
  Matrix eliminateZeros() {
    // There is nothing to be done in this implementation (zeros cannot be removed)
    return this;
  }
}
