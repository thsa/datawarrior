package com.tagbio.umap;

public class ProgressState {
  private final int mTotal;
  private final int mCount;

  protected ProgressState(final int total, final int count) {
    mTotal = total;
    mCount = count;
  }

  public int getTotal() {
    return mTotal;
  }

  public int getCount() {
    return mCount;
  }
}
