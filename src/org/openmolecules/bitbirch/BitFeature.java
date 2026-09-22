package org.openmolecules.bitbirch;

/**
 * BitFeature (BF): the summary of a set of binary fingerprints.
 *
 * <pre>
 *   n      number of compounds
 *   ls[i]  number of members having bit i set          (the "linear sum")
 *   k      sum_i ls[i]                                 (total set bits over all members)
 *   q      sum_i ls[i]^2                               (leaf BFs only)
 * </pre>
 *
 * Leaf BFs additionally own a chain of member ids (head/tail into a shared next[] array), so merging two
 * clusters is O(1) for the member bookkeeping and costs no per-cluster list objects.
 *
 * <h3>Everything is Tanimoto, and everything is a ratio of pairwise sums</h3>
 * For sets of fingerprints A and B, with dot = sum_i lsA[i]*lsB[i]:
 * <pre>
 *   sum over pairs (a in A, b in B) of |a AND b| = dot
 *   sum over pairs (a in A, b in B) of |a OR  b| = nB*kA + nA*kB - dot
 * </pre>
 * <b>Routing</b> ({@link #linkSim}): the ratio of those two sums = the average Tanimoto between all members of A
 * and all members of B. For a single compound against a single compound it is the plain Tanimoto index.
 * <br><b>Merge criterion</b> ({@link #mergedISim}): the same ratio taken over all pairs inside one set (iSIM),
 * <pre>   iSIM = (q - k) / (2*N*k - q - k)</pre>
 * which needs only N, k and q, i.e. O(1) per test instead of O(N^2). Merging BFs A and B gives
 * n = nA+nB, k = kA+kB, q = qA+qB+2*dot, so the test never has to build the merged BF.
 *
 * <p>Memory trick: a single-compound BF (ls == null) has no count array; its counts ARE its fingerprint
 * (fp, shared with the caller and never modified).
 */
final class BitFeature {
    int n;
    long k;
    long q;
    int[] ls;          // null while n == 1 on a leaf; always present on internal "summary" BFs
    long[] fp;         // the compound's fingerprint while ls == null
    int head = -1;     // first member id in the chain (leaf BFs only)
    int tail = -1;     // last member id in the chain

    private BitFeature() {}

    /** BF holding one compound. The array is referenced, not copied, and must not be modified later. */
    static BitFeature ofMolecule(long[] fp, int id) {
        BitFeature b = new BitFeature();
        int pop = Tanimoto.popcount(fp);
        b.n = 1;
        b.k = pop;
        b.q = pop;
        b.fp = fp;
        b.head = id;
        b.tail = id;
        return b;
    }

    /** Empty accumulator used by internal nodes; only n, k and ls are maintained. */
    static BitFeature emptySummary(int words) {
        BitFeature b = new BitFeature();
        b.ls = new int[words << 6];
        return b;
    }

    // ------------------------------------------------------------------ similarity

    /** Average Tanimoto between all members of this and all members of o (sum of intersections / sum of unions). */
    double linkSim(BitFeature o) {
        long d = dot(o);
        double den = (double) o.n * k + (double) n * o.k - d;
        return den <= 0 ? 1.0 : d / den;
    }

    /** iSIM of the cluster that merging this BF with o would create. Nothing is modified. */
    double mergedISim(BitFeature o) {
        long nn = (long) n + o.n;
        long kk = k + o.k;
        long qq = q + o.q + 2L * dot(o);
        return isim(nn, kk, qq);
    }

    static double isim(long n, long k, long q) {
        double den = 2.0 * n * k - (double) q - k;
        if (den <= 0) return 1.0;               // only possible if all members are all-zero fingerprints
        return ((double) q - k) / den;
    }

    /** sum_i ls_i * o.ls_i */
    long dot(BitFeature o) {
        if (ls == null && o.ls == null) return Tanimoto.andCount(fp, o.fp);
        if (ls == null) return sumAtSetBits(fp, o.ls);
        if (o.ls == null) return sumAtSetBits(o.fp, ls);
        long s = 0;
        for (int i = 0; i < ls.length; i++) s += (long) ls[i] * o.ls[i];
        return s;
    }

    // ------------------------------------------------------------------ mutation

    /**
     * Leaf-level merge: this BF takes over all members of o.
     * Caller must already have checked {@link #mergedISim}. o must not be used afterwards.
     */
    void absorb(BitFeature o, int[] next) {
        long d = dot(o);                                   // needs the pre-merge state
        if (ls == null) {                                  // leaving the singleton state
            ls = new int[fp.length << 6];
            addBits(ls, fp);
            fp = null;
        }
        o.addCountsTo(ls);
        n += o.n;
        k += o.k;
        q += o.q + 2L * d;
        next[tail] = o.head;                               // concatenate member chains
        tail = o.tail;
    }

    /** Internal-node bookkeeping: add everything o summarises. */
    void accumulate(BitFeature o) {
        o.addCountsTo(ls);
        n += o.n;
        k += o.k;
    }

    /** Binary majority-vote centroid (bit i set iff at least half of the members have it), packed like the input. */
    long[] majorityCentroid() {
        if (ls == null) return fp.clone();
        long[] c = new long[ls.length >> 6];
        for (int i = 0; i < ls.length; i++) {
            if (2L * ls[i] >= n) c[i >> 6] |= 1L << (i & 63);
        }
        return c;
    }

    // ------------------------------------------------------------------ helpers

    private void addCountsTo(int[] target) {
        if (ls != null) {
            for (int i = 0; i < target.length; i++) target[i] += ls[i];
        } else {
            addBits(target, fp);
        }
    }

    private static void addBits(int[] target, long[] bits) {
        for (int w = 0; w < bits.length; w++) {
            long x = bits[w];
            final int base = w << 6;
            while (x != 0) {
                target[base + Long.numberOfTrailingZeros(x)]++;
                x &= x - 1;
            }
        }
    }

    private static long sumAtSetBits(long[] bits, int[] counts) {
        long s = 0;
        for (int w = 0; w < bits.length; w++) {
            long x = bits[w];
            final int base = w << 6;
            while (x != 0) {
                s += counts[base + Long.numberOfTrailingZeros(x)];
                x &= x - 1;
            }
        }
        return s;
    }
}
