package org.openmolecules.bitbirch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.IntFunction;

/**
 * BitBIRCH clustering of binary fingerprints (long[] packed) with the Tanimoto index.
 * The authors of the original code developed in Python are:
 * Kenneth López Pérez; Vicky Jung, Kate Huddleston, Ramón Alain Miranda-Quintana
 * The paper accessible from: https://pubs.rsc.org/dd/article/4/4/1042/888988/BitBIRCH-efficient-clustering-of-large-molecular.
 * The software used in the paper can be found in: https://github.com/mqcomplab/bitbirch (DOI: 10.5281/zenodo.14977624).
 * Benchmark data can be found in: https://github.com/molML/MoleculeACE/tree/main/MoleculeACE/Data/benchmark_data.
 * <p>A height-balanced tree (as in BIRCH) whose entries are {@link BitFeature}s:
 * <ul>
 *   <li>A compound descends from the root, at every node moving to the entry whose members have the highest
 *       <i>average Tanimoto</i> to it (sum of intersections / sum of unions, from the per-bit counts).</li>
 *   <li>At a leaf it is merged into the most similar entry if the merged cluster's iSIM (average pairwise
 *       Tanimoto, computed in O(1) from N, sum of counts and sum of squared counts) stays &gt;= threshold;
 *       otherwise it becomes a new entry. Overfull nodes are split around two far-apart entries.</li>
 * </ul>
 * Why not route by Tanimoto to a binary majority-vote centroid? When a subtree spans several chemotypes that
 * centroid is (nearly) empty (on the synthetic demo data the root entries had 0-1 set bits), every similarity
 * ties at ~0 and routing degenerates into arbitrary paths with tens of thousands of spurious singletons. The
 * binary centroids are still available on demand via {@link #getCentroids()}.
 *
 * <p>Memory: 4 bytes per compound for the member chains, plus 256*words bytes (8 KB for 2048-bit fingerprints)
 * for every cluster with at least 2 members (its per-bit counts), plus the fingerprints themselves, which the
 * caller owns. Singleton clusters cost only a small object.
 * Fingerprints are referenced, not copied, and must not be modified while the model is in use.
 * A single tree is not thread-safe; use {@link #fitParallel} for multicore builds.
 */
public final class BitBirch {

    private static final class Node {
        final boolean leaf;
        final BitFeature[] subs;   // leaf: cluster BFs; internal: summary BF of children[i]
        final Node[] children;     // null for leaves
        int size;

        Node(boolean leaf, int capacity) {
            this.leaf = leaf;
            this.subs = new BitFeature[capacity];
            this.children = leaf ? null : new Node[capacity];
        }

        void add(BitFeature bf, Node child) {
            subs[size] = bf;
            if (children != null) children[size] = child;
            size++;
        }
    }

    private final double threshold;
    private final int branching;
    private final int[] next;      // member chains: next[id] = following id in the same cluster, -1 = end
    private int words;             // longs per fingerprint; fixed by the first insertion
    private Node root;
    private int inserted;

    /**
     * @param threshold       minimum average pairwise Tanimoto (iSIM) a cluster must keep, in (0,1]
     * @param branchingFactor max entries per tree node (50 is a common starting point)
     * @param numCompounds    compound ids passed to {@link #insert} must be in [0, numCompounds)
     */
    public BitBirch(double threshold, int branchingFactor, int numCompounds) {
        this(threshold, branchingFactor, new int[numCompounds]);
    }

    private BitBirch(double threshold, int branchingFactor, int[] next) {
        if (!(threshold > 0.0 && threshold <= 1.0)) throw new IllegalArgumentException("threshold must be in (0,1]");
        if (branchingFactor < 2) throw new IllegalArgumentException("branchingFactor must be >= 2");
        this.threshold = threshold;
        this.branching = branchingFactor;
        this.next = next;
    }

    // ================================================================== fitting

    public static BitBirch fit(long[][] fps, double threshold, int branching) {
        return fit(fps.length, i -> fps[i], threshold, branching);
    }

    /** Sequential build. source.apply(i) must return the (unmodified, stable) fingerprint of compound i. */
    public static BitBirch fit(int n, IntFunction<long[]> source, double threshold, int branching) {
        BitBirch t = new BitBirch(threshold, branching, n);
        for (int i = 0; i < n; i++) t.insert(source.apply(i), i);
        return t;
    }

    public static BitBirch fitParallel(long[][] fps, double threshold, int branching, int threads) {
        return fitParallel(fps.length, i -> fps[i], threshold, branching, threads);
    }

    /**
     * Two-phase parallel build: every thread builds its own tree over a contiguous slice of the data, then the
     * resulting leaf clusters are inserted into a final tree with the same merge rule. Results differ slightly
     * from the sequential build (and depend on the thread count), as with any input-order-dependent BIRCH variant.
     * source.apply may be called concurrently.
     */
    public static BitBirch fitParallel(int n, IntFunction<long[]> source, double threshold, int branching, int threads) {
        if (threads < 2 || n < 4L * threads) return fit(n, source, threshold, branching);
        final int[] next = new int[n];                     // shared; each thread writes only its own ids
        final int chunk = (int) (((long) n + threads - 1) / threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<BitBirch>> parts = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                final int lo = t * chunk;
                final int hi = Math.min(n, lo + chunk);
                if (lo >= hi) break;
                Callable<BitBirch> job = () -> {
                    BitBirch part = new BitBirch(threshold, branching, next);
                    for (int i = lo; i < hi; i++) part.insert(source.apply(i), i);
                    return part;
                };
                parts.add(pool.submit(job));
            }
            BitBirch merged = new BitBirch(threshold, branching, next);
            for (Future<BitBirch> f : parts) {
                BitBirch part = f.get();
                merged.ensureInit(part.words);
                merged.inserted += part.inserted;
                for (BitFeature bf : part.leafFeatures()) merged.insertFeature(bf);
            }
            return merged;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } finally {
            pool.shutdownNow();
        }
    }

    /** Streaming insertion of one compound. fp is referenced (not copied) and must stay unmodified. */
    public void insert(long[] fp, int id) {
        ensureInit(fp.length);
        if (fp.length != words) {
            throw new IllegalArgumentException("fingerprint has " + fp.length + " longs, expected " + words);
        }
        next[id] = -1;
        insertFeature(BitFeature.ofMolecule(fp, id));
        inserted++;
    }

    private void ensureInit(int fpWords) {
        if (fpWords <= 0) throw new IllegalArgumentException("empty fingerprint array");
        if (words == 0) {
            words = fpWords;
            root = new Node(true, branching + 1);
        } else if (words != fpWords) {
            throw new IllegalArgumentException("mixed fingerprint lengths: " + words + " vs " + fpWords);
        }
    }

    // ================================================================== tree logic

    private void insertFeature(BitFeature bf) {
        if (insertInto(root, bf)) {
            // root overflowed: put it under a fresh root and split it there
            Node newRoot = new Node(false, branching + 1);
            newRoot.subs[0] = null;                        // replaced by splitChild
            newRoot.children[0] = root;
            newRoot.size = 1;
            splitChild(newRoot, 0);
            root = newRoot;
        }
    }

    /** @return true if node now holds branching+1 entries and the caller has to split it. */
    private boolean insertInto(Node node, BitFeature bf) {
        if (node.size == 0) {                              // only the very first entry of an empty root leaf
            node.add(bf, null);
            return false;
        }
        final int idx = closest(node, bf);
        if (!node.leaf) {
            boolean split = insertInto(node.children[idx], bf);
            if (!split) {
                node.subs[idx].accumulate(bf);
                return false;
            }
            splitChild(node, idx);
            return node.size > branching;
        }
        BitFeature target = node.subs[idx];
        if (target.mergedISim(bf) >= threshold) {
            target.absorb(bf, next);
            return false;
        }
        node.add(bf, null);
        return node.size > branching;
    }

    /** Index of the entry with the highest average Tanimoto to bf (first wins ties). */
    private static int closest(Node node, BitFeature bf) {
        int best = 0;
        double bestSim = -1.0;
        for (int i = 0; i < node.size; i++) {
            double sim = bf.linkSim(node.subs[i]);
            if (sim > bestSim) {
                bestSim = sim;
                best = i;
            }
        }
        return best;
    }

    /**
     * Splits parent.children[idx] in two. Seeds: the entry least similar to entry 0, then the entry least similar
     * to that one (a cheap two-pass farthest-pair heuristic, O(B) similarity evaluations instead of O(B^2)).
     * Every other entry follows the closer seed. The old entry is replaced by the first half, the second half
     * is appended to the parent.
     */
    private void splitChild(Node parent, int idx) {
        final Node old = parent.children[idx];
        final BitFeature[] e = old.subs;
        final int m = old.size;

        final double[] simA = new double[m];
        final double[] simB = new double[m];
        final int a = leastSimilarTo(e, m, 0, null);
        final int b = leastSimilarTo(e, m, a, simA);
        for (int j = 0; j < m; j++) simB[j] = (j == b) ? 1.0 : e[b].linkSim(e[j]);

        Node n1 = new Node(old.leaf, branching + 1);
        Node n2 = new Node(old.leaf, branching + 1);
        BitFeature s1 = BitFeature.emptySummary(words);
        BitFeature s2 = BitFeature.emptySummary(words);

        for (int i = 0; i < m; i++) {
            boolean first;
            if (i == a) first = true;
            else if (i == b) first = false;
            else first = simA[i] > simB[i] || (simA[i] == simB[i] && n1.size <= n2.size);   // ties: keep halves balanced
            Node child = old.leaf ? null : old.children[i];
            if (first) {
                n1.add(e[i], child);
                s1.accumulate(e[i]);
            } else {
                n2.add(e[i], child);
                s2.accumulate(e[i]);
            }
        }

        parent.subs[idx] = s1;
        parent.children[idx] = n1;
        parent.subs[parent.size] = s2;
        parent.children[parent.size] = n2;
        parent.size++;
    }

    /** Index j != from with the lowest linkSim(e[from], e[j]); optionally records all similarities in out. */
    private static int leastSimilarTo(BitFeature[] e, int m, int from, double[] out) {
        int far = -1;
        double worst = Double.MAX_VALUE;
        for (int j = 0; j < m; j++) {
            if (j == from) {
                if (out != null) out[j] = 1.0;
                continue;
            }
            double s = e[from].linkSim(e[j]);
            if (out != null) out[j] = s;
            if (s < worst) {
                worst = s;
                far = j;
            }
        }
        return far;
    }

    // ================================================================== results

    private List<BitFeature> leafFeatures() {
        List<BitFeature> out = new ArrayList<>();
        if (root != null) collect(root, out);
        return out;
    }

    private static void collect(Node node, List<BitFeature> out) {
        for (int i = 0; i < node.size; i++) {
            if (node.leaf) out.add(node.subs[i]);
            else collect(node.children[i], out);
        }
    }

    private List<BitFeature> sortedLeafFeatures() {
        List<BitFeature> f = leafFeatures();
        f.sort((x, y) -> Integer.compare(y.n, x.n));       // biggest cluster first (stable)
        return f;
    }

    public int getNumCompounds() {
        return inserted;
    }

    public int getNumClusters() {
        return leafFeatures().size();
    }

    /** Member ids of every cluster, largest cluster first. Same ordering as {@link #getCentroids()}. */
    public int[][] getClusters() {
        List<BitFeature> f = sortedLeafFeatures();
        int[][] out = new int[f.size()][];
        for (int i = 0; i < out.length; i++) {
            BitFeature bf = f.get(i);
            int[] members = new int[bf.n];
            int c = bf.head;
            for (int j = 0; j < bf.n; j++) {
                members[j] = c;
                c = next[c];
            }
            out[i] = members;
        }
        return out;
    }

    /** labels[id] = index into {@link #getClusters()}; -1 for ids that were never inserted. */
    public int[] getLabels() {
        int[] labels = new int[next.length];
        java.util.Arrays.fill(labels, -1);
        int[][] clusters = getClusters();
        for (int ci = 0; ci < clusters.length; ci++) {
            for (int id : clusters[ci]) labels[id] = ci;
        }
        return labels;
    }

    /** Binary majority-vote centroid of every cluster (packed like the input), largest cluster first. Computed on demand. */
    public long[][] getCentroids() {
        List<BitFeature> f = sortedLeafFeatures();
        long[][] out = new long[f.size()][];
        for (int i = 0; i < out.length; i++) out[i] = f.get(i).majorityCentroid();
        return out;
    }

    /** iSIM (= sum of pairwise intersections / sum of pairwise unions) of every cluster; 1.0 for singletons. */
    public double[] getClusterISims() {
        List<BitFeature> f = sortedLeafFeatures();
        double[] out = new double[f.size()];
        for (int i = 0; i < out.length; i++) {
            BitFeature bf = f.get(i);
            out[i] = bf.n < 2 ? 1.0 : BitFeature.isim(bf.n, bf.k, bf.q);
        }
        return out;
    }
}
