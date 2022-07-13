/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Random projection trees.
 * @author Leland McInnes (Python)
 * @author Sean A. Irvine
 * @author Richard Littin
 */
final class RandomProjectionTree {

  private RandomProjectionTree() { }

  // Used for a floating point "nearly zero" comparison
  private static final float EPS = 1e-8F;

  /**
   * Given a set of <code>indices</code> for data points from <code>data</code>, create
   * a random hyperplane to split the data, returning two arrays indices
   * that fall on either side of the hyperplane. This is the basis for a
   * random projection tree, which simply uses this splitting recursively.
   * This particular split uses cosine distance to determine the hyperplane
   * and which side each data sample falls on.
   * @param data array of shape <code>(nSamples, nFeatures)</code>. The original data to be split
   * @param indices array of shape <code>(treeNodeSize)</code>
   * The indices of the elements in the <code>data</code> array that are to
   * be split in the current operation.
   * @param random randomness source
   * @return The elements of <code>indices</code> that fall on the "left" side of the
   * random hyperplane.
   */
  private static Object[] angularRandomProjectionSplit(final Matrix data, final int[] indices, final Random random) {
    final int dim = data.cols();

    // Select two random points, set the hyperplane between them
    final int leftIndex = random.nextInt(indices.length);
    int rightIndex = random.nextInt(indices.length);
    if (leftIndex == rightIndex && ++rightIndex == indices.length) {
      rightIndex = 0;
    }
    final int left = indices[leftIndex];
    final int right = indices[rightIndex];

    float leftNorm = Utils.norm(data.row(left));
    float rightNorm = Utils.norm(data.row(right));

    if (Math.abs(leftNorm) < EPS) {
      leftNorm = 1;
    }

    if (Math.abs(rightNorm) < EPS) {
      rightNorm = 1;
    }

    // Compute the normal vector to the hyperplane (the vector between the two points)
    final float[] hyperplaneVector = new float[dim];

    for (int d = 0; d < dim; ++d) {
      hyperplaneVector[d] = (data.get(left, d) / leftNorm) - (data.get(right, d) / rightNorm);
    }

    float hyperplaneNorm = Utils.norm(hyperplaneVector);
    if (Math.abs(hyperplaneNorm) < EPS) {
      hyperplaneNorm = 1;
    }

    for (int d = 0; d < dim; ++d) {
      hyperplaneVector[d] /= hyperplaneNorm;
    }

    // For each point compute the margin (project into normal vector)
    // If we are on lower side of the hyperplane put in one pile, otherwise
    // put it in the other pile (if we hit hyperplane on the nose, flip a coin)
    int nLeft = 0;
    int nRight = 0;
    final boolean[] side = new boolean[indices.length];
    for (int i = 0; i < indices.length; ++i) {
      float margin = 0;
      for (int d = 0; d < dim; ++d) {
        margin += hyperplaneVector[d] * data.get(indices[i], d);
      }

      if (Math.abs(margin) < EPS) {
        side[i] = random.nextBoolean();
        if (side[i]) {
          nRight += 1;
        } else {
          nLeft += 1;
        }
      } else if (margin > 0) {
        side[i] = false;
        ++nLeft;
      } else {
        side[i] = true;
        ++nRight;
      }
    }

    // Now that we have the counts allocate arrays
    final int[] indicesLeft = new int[nLeft];
    final int[] indicesRight = new int[nRight];

    // Populate the arrays with indices according to which side they fell on
    nLeft = 0;
    nRight = 0;
    for (int i = 0; i < side.length; ++i) {
      if (side[i]) {
        indicesRight[nRight++] = indices[i];
      } else {
        indicesLeft[nLeft++] = indices[i];
      }
    }

    return new Object[]{indicesLeft, indicesRight, hyperplaneVector, null};
  }


  /**
   * Given a set of <code>indices</code> for data points from <code>data</code>, create
   * a random hyperplane to split the data, returning two arrays indices
   * that fall on either side of the hyperplane. This is the basis for a
   * random projection tree, which simply uses this splitting recursively.
   * This particular split uses Euclidean distance to determine the hyperplane
   * and which side each data sample falls on.
   * @param data array of shape <code>(nSamples, nFeatures)</code>. The original data to be split
   * @param indices array of shape <code>(treeNodeSize)</code>
   * The indices of the elements in the <code>data</code> array that are to
   * be split in the current operation.
   * @param random randomness source
   * @return The elements of <code>indices</code> that fall on the "left" side of the
   * random hyperplane.
   */
  private static Object[] euclideanRandomProjectionSplit(final Matrix data, final int[] indices, final Random random) {
    final int dim = data.cols();

    // Select two random points, set the hyperplane between them
    final int leftIndex = random.nextInt(indices.length);
    int rightIndex = random.nextInt(indices.length);
    if (leftIndex == rightIndex && ++rightIndex == indices.length) {
      rightIndex = 0;
    }
    final int left = indices[leftIndex];
    final int right = indices[rightIndex];

    // Compute the normal vector to the hyperplane (the vector between the two points) and the offset from the origin
    float hyperplaneOffset = 0;
    final float[] hyperplaneVector = new float[dim];

    // todo what is the matrix type here?  Getting multiple values from same row ...
    for (int d = 0; d < dim; ++d) {
      final float ld = data.get(left, d);
      final float rd = data.get(right, d);
      final float delta = ld - rd;
      hyperplaneVector[d] = delta;
      hyperplaneOffset -= delta * (ld + rd);
    }
    hyperplaneOffset /= 2;

    // For each point compute the margin (project into normal vector, add offset)
    // If we are on lower side of the hyperplane put in one pile, otherwise
    // put it in the other pile (if we hit hyperplane on the nose, flip a coin)
    int nLeft = 0;
    int nRight = 0;
    final boolean[] side = new boolean[indices.length];
    for (int i = 0; i < indices.length; ++i) {
      float margin = hyperplaneOffset;
      for (int d = 0; d < dim; ++d) {
        margin += hyperplaneVector[d] * data.get(indices[i], d);
      }
      if (margin >= EPS) {
        //side[i] = false;
        ++nLeft;
      } else if (margin <= -EPS) {
        side[i] = true;
        ++nRight;
      } else {
        // Margin is very close to 0
        side[i] = random.nextBoolean();
        if (side[i]) {
          ++nRight;
        } else {
          ++nLeft;
        }
      }
    }
    // Now that we have the counts allocate arrays
    final int[] indicesLeft = new int[nLeft];
    final int[] indicesRight = new int[nRight];

    // Populate the arrays with indices according to which side they fell on
    for (int i = 0, l = 0, r = 0; i < side.length; ++i) {
      if (side[i]) {
        indicesRight[r++] = indices[i];
      } else {
        indicesLeft[l++] = indices[i];
      }
    }
    return new Object[]{indicesLeft, indicesRight, hyperplaneVector, hyperplaneOffset};
  }

  /**
   * Given a set of <code>indices</code> for data points from <code>data</code>, create
   * a random hyperplane to split the data, returning two arrays indices
   * that fall on either side of the hyperplane. This is the basis for a
   * random projection tree, which simply uses this splitting recursively.
   * This particular split uses cosine distance to determine the hyperplane
   * and which side each data sample falls on.
   * @param matrix CSR format matrix
   * @param indices indices of data points
   * @param random randomness source
   * @return The elements of <code>indices</code> that fall on the "left" side of the
   * random hyperplane.
   */
  private static Object[] sparseAngularRandomProjectionSplit(final CsrMatrix matrix, final int[] indices, final Random random) {
    // Select two random points, set the hyperplane between them
    final int leftIndex = random.nextInt(indices.length);
    int rightIndex = random.nextInt(indices.length);
    if (leftIndex == rightIndex && ++rightIndex == indices.length) {
      rightIndex = 0;
    }
    final int left = indices[leftIndex];
    final int right = indices[rightIndex];

    final SparseVector leftVec = matrix.vector(left);
    final SparseVector rightVec = matrix.vector(right);

    float leftNorm = leftVec.norm();
    float rightNorm = rightVec.norm();

    if (Math.abs(leftNorm) < EPS) {
      leftNorm = 1;
    }

    if (Math.abs(rightNorm) < EPS) {
      rightNorm = 1;
    }

    leftVec.divide(leftNorm);
    rightVec.divide(rightNorm);

    // Compute the normal vector to the hyperplane (the vector between the two points)
    final SparseVector sd = leftVec.subtract(rightVec);

    float hyperplaneNorm = sd.norm();
    if (Math.abs(hyperplaneNorm) < EPS) {
      hyperplaneNorm = 1;
    }
    sd.divide(hyperplaneNorm);

    // For each point compute the margin (project into normal vector)
    // If we are on lower side of the hyperplane put in one pile, otherwise
    // put it in the other pile (if we hit hyperplane on the nose, flip a coin)
    int nLeft = 0;
    int nRight = 0;
    final boolean[] side = new boolean[indices.length];
    for (int i = 0; i < indices.length; ++i) {
      final SparseVector iVec = matrix.vector(indices[i]);
      final SparseVector spm = sd.hadamardMultiply(iVec);
      final float margin = spm.sum();
      if (Math.abs(margin) < EPS) {
        side[i] = random.nextBoolean();
        if (side[i]) {
          ++nRight;
        } else {
          ++nLeft;
        }
      } else if (margin > 0) {
        side[i] = false;
        ++nLeft;
      } else {
        side[i] = true;
        ++nRight;
      }
    }

    // Now that we have the counts allocate arrays
    final int[] indicesLeft = new int[nLeft];
    final int[] indicesRight = new int[nRight];

    // Populate the arrays with indices according to which side they fell on
    nLeft = 0;
    nRight = 0;
    for (int i = 0; i < side.length; ++i) {
      if (side[i]) {
        indicesRight[nRight++] = indices[i];
      } else {
        indicesLeft[nLeft++] = indices[i];
      }
    }

    final Hyperplane hyperplane = new Hyperplane(sd.getIndices(), sd.getData());

    return new Object[]{indicesLeft, indicesRight, hyperplane, null};
  }

  /**
   * Given a set of <code>indices</code> for data points from <code>data</code>, create
   * a random hyperplane to split the data, returning two arrays indices
   * that fall on either side of the hyperplane. This is the basis for a
   * random projection tree, which simply uses this splitting recursively.
   * This particular split uses Euclidean distance to determine the hyperplane
   * and which side each data sample falls on.
   * @param matrix Csr format matrix
   * @param random randomness source
   * @return The elements of <code>indices</code> that fall on the "left" side of the
   * random hyperplane.
   */
  private static Object[] sparseEuclideanRandomProjectionSplit(final CsrMatrix matrix, final int[] indices, final Random random) {
    // Select two random points, set the hyperplane between them
    final int leftIndex = random.nextInt(indices.length);
    int rightIndex = random.nextInt(indices.length);
    if (leftIndex == rightIndex && ++rightIndex == indices.length) {
      rightIndex = 0;
    }
    final int left = indices[leftIndex];
    final int right = indices[rightIndex];

    final SparseVector leftVec = matrix.vector(left);
    final SparseVector rightVec = matrix.vector(right);

    // Compute the normal vector to the hyperplane (the vector between
    // the two points) and the offset from the origin
    final SparseVector sd = leftVec.subtract(rightVec);
    final SparseVector ss = leftVec.add(rightVec);
    ss.divide(2);
    final SparseVector sm = sd.hadamardMultiply(ss);
    final float hyperplaneOffset = -sm.sum();

    // For each point compute the margin (project into normal vector, add offset)
    // If we are on lower side of the hyperplane put in one pile, otherwise
    // put it in the other pile (if we hit hyperplane on the nose, flip a coin)
    int nLeft = 0;
    int nRight = 0;
    final boolean[] side = new boolean[indices.length];
    for (int i = 0; i < indices.length; ++i) {
      final SparseVector iVec = matrix.vector(indices[i]);

      final SparseVector spm = sd.hadamardMultiply(iVec);
      final float margin = hyperplaneOffset + spm.sum();

      if (Math.abs(margin) < EPS) {
        side[i] = random.nextBoolean();
        if (side[i]) {
          ++nLeft;
        } else {
          ++nRight;
        }
      } else if (margin > 0) {
        side[i] = false;
        ++nLeft;
      } else {
        side[i] = true;
        ++nRight;
      }
    }

    // Now that we have the counts allocate arrays
    final int[] indicesLeft = new int[nLeft];
    final int[] indicesRight = new int[nRight];

    // Populate the arrays with indices according to which side they fell on
    nLeft = 0;
    nRight = 0;
    for (int i = 0; i < side.length; ++i) {
      if (side[i]) {
        indicesRight[nRight++] = indices[i];
      } else {
        indicesLeft[nLeft++] = indices[i];
      }
    }

    final Hyperplane hyperplane = new Hyperplane(sd.getIndices(), sd.getData());

    return new Object[]{indicesLeft, indicesRight, hyperplane, hyperplaneOffset};
  }

  private static RandomProjectionTreeNode makeEuclideanTree(final Matrix data, final int[] indices, final Random random, final int leafSize) {
    if (indices.length > leafSize) {
      final Object[] erps = euclideanRandomProjectionSplit(data, indices, random);
      final int[] leftIndices = (int[]) erps[0];
      final int[] rightIndices = (int[]) erps[1];
      final Hyperplane hyperplane = new Hyperplane((float[]) erps[2]);
      final float offset = (float) erps[3];

      final RandomProjectionTreeNode leftNode = makeEuclideanTree(data, leftIndices, random, leafSize);
      final RandomProjectionTreeNode rightNode = makeEuclideanTree(data, rightIndices, random, leafSize);

      return new RandomProjectionTreeNode(null, hyperplane, offset, leftNode, rightNode);
    } else {
      return new RandomProjectionTreeNode(indices, null, null, null, null);
    }
  }

  private static RandomProjectionTreeNode makeAngularTree(final Matrix data, final int[] indices, final Random random, final int leafSize) {
    if (indices.length > leafSize) {
      final Object[] erps = angularRandomProjectionSplit(data, indices, random);
      final int[] leftIndices = (int[]) erps[0];
      final int[] rightIndices = (int[]) erps[1];
      final Hyperplane hyperplane = new Hyperplane((float[]) erps[2]);
      final float offset = (float) erps[3];

      final RandomProjectionTreeNode leftNode = makeAngularTree(data, leftIndices, random, leafSize);
      final RandomProjectionTreeNode rightNode = makeAngularTree(data, rightIndices, random, leafSize);

      return new RandomProjectionTreeNode(null, hyperplane, offset, leftNode, rightNode);
    } else {
      return new RandomProjectionTreeNode(indices, null, null, null, null);
    }
  }

  private static RandomProjectionTreeNode makeSparseEuclideanTree(final CsrMatrix matrix, final int[] indices, final Random random, final int leafSize) {
    if (indices.length > leafSize) {
      final Object[] erps = sparseEuclideanRandomProjectionSplit(matrix, indices, random);
      final int[] leftIndices = (int[]) erps[0];
      final int[] rightIndices = (int[]) erps[1];
      final Hyperplane hyperplane = (Hyperplane) erps[2];
      final float offset = (float) erps[3];

      final RandomProjectionTreeNode leftNode = makeSparseEuclideanTree(matrix, leftIndices, random, leafSize);
      final RandomProjectionTreeNode rightNode = makeSparseEuclideanTree(matrix, rightIndices, random, leafSize);

      return new RandomProjectionTreeNode(null, hyperplane, offset, leftNode, rightNode);
    } else {
      return new RandomProjectionTreeNode(indices, null, null, null, null);
    }
  }

  private static RandomProjectionTreeNode makeSparseAngularTree(final CsrMatrix matrix, final int[] indices, final Random random, final int leafSize) {
    if (indices.length > leafSize) {
      final Object[] erps = sparseAngularRandomProjectionSplit(matrix, indices, random);
      final int[] leftIndices = (int[]) erps[0];
      final int[] rightIndices = (int[]) erps[1];
      final Hyperplane hyperplane = (Hyperplane) erps[2];
      final float offset = (float) erps[3];

      final RandomProjectionTreeNode leftNode = makeSparseAngularTree(matrix, leftIndices, random, leafSize);
      final RandomProjectionTreeNode rightNode = makeSparseAngularTree(matrix, rightIndices, random, leafSize);

      return new RandomProjectionTreeNode(null, hyperplane, offset, leftNode, rightNode);
    } else {
      return new RandomProjectionTreeNode(indices, null, null, null, null);
    }
  }

  /**
   * Construct a random projection tree based on <code>data</code> with leaves
   * of size at most <code>leafSize</code>.
   * @param data array of shape <code>(nSamples, nFeatures)</code>
   * The original data to be split
   * @param random randomness source
   * @param leafSize The maximum size of any leaf node in the tree. Any node in the tree
   * with more than <code>leafSize</code> will be split further to create child
   * nodes.
   * @param angular Whether to use cosine/angular distance to create splits in the tree,
   * or Euclidean distance
   * @return A random projection tree node which links to its child nodes. This
   * provides the full tree below the returned node.
   */
  private static RandomProjectionTreeNode makeTree(final Matrix data, final Random random, final int leafSize, final boolean angular) {
    final boolean isSparse = data instanceof CsrMatrix;
    final int[] indices = MathUtils.identity(data.rows());

    // Make a tree recursively until we get below the leaf size
    if (isSparse) {
      final CsrMatrix csrData = (CsrMatrix) data;
      if (angular) {
        return makeSparseAngularTree(csrData, indices, random, leafSize);
      } else {
        return makeSparseEuclideanTree(csrData, indices, random, leafSize);
      }
    } else {
      if (angular) {
        return makeAngularTree(data, indices, random, leafSize);
      } else {
        return makeEuclideanTree(data, indices, random, leafSize);
      }
    }
  }


  /**
   * Build a random projection forest with specified number of trees.
   * @param data instances
   * @param nNeighbors number of nearest neighbours
   * @param nTrees number of trees
   * @param random randomness source
   * @param angular true for cosine metric, otherwise Euclidean
   * @return list of random projection trees
   */
  static List<FlatTree> makeForest(final Matrix data, final int nNeighbors, final int nTrees, final Random random, final boolean angular) {
    final Random[] randoms = Utils.splitRandom(random, nTrees);  // insure same set of random numbers for 1 and multiple threads

    final ArrayList<FlatTree> result = new ArrayList<>();
    final int leafSize = Math.max(10, nNeighbors);
    try {
      for (int i = 0; i < nTrees; ++i) {
        result.add(makeTree(data, randoms[i], leafSize, angular).flatten());
        UmapProgress.update();
      }
    } catch (RuntimeException e) {
      Utils.message("Random Projection forest initialisation failed due to recursion limit being reached. Something is a little strange with your data, and this may take longer than normal to compute.");
      throw e; // Python blindly continued from this point ... we die for now
    }
    return result;
  }

  static List<FlatTree> makeForest(final Matrix data, final int nNeighbors, final int nTrees, final Random random, final boolean angular, int threads) {
    if (threads == 1) {
      return makeForest(data, nNeighbors, nTrees, random, angular);
    }
    final Random[] randoms = Utils.splitRandom(random, nTrees);  // insure same set of random numbers for 1 and multiple threads

    final ExecutorService executor = Executors.newFixedThreadPool(threads);
    try {
      final List<Future<FlatTree>> futures = new ArrayList<>();

      final int leafSize = Math.max(10, nNeighbors);
      for (final Random rand : randoms) {  // randoms.length == nTrees
        futures.add(executor.submit(() -> makeTree(data, rand, leafSize, angular).flatten()));
      }

      final ArrayList<FlatTree> result = new ArrayList<>();
      try {
        for (final Future<FlatTree> future : futures) {
          result.add(future.get());
          UmapProgress.update();
        }
      } catch (final InterruptedException | ExecutionException ex) {
        Utils.message("Random Projection forest initialisation failed due to recursion limit being reached. Something is a little strange with your data, and this may take longer than normal to compute.");
        throw new RuntimeException(ex); // Python blindly continued from this point ... we die for now
      }
      return result;
    } finally {
      executor.shutdown();
    }
  }
}
