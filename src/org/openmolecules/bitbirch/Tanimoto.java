package org.openmolecules.bitbirch;

/**
 * Tanimoto (Jaccard) similarity for binary fingerprints packed into long[] (64 bits per word).
 * Bit i of the fingerprint lives in word i/64, at position i%64.
 * Long.bitCount() is a JIT intrinsic (POPCNT), so these loops are very fast.
 */
public final class Tanimoto {
    private Tanimoto() {}

    /** Number of set bits. */
    public static int popcount(long[] a) {
        int c = 0;
        for (int i = 0; i < a.length; i++) c += Long.bitCount(a[i]);
        return c;
    }

    /** Number of bits set in both fingerprints. */
    public static int andCount(long[] a, long[] b) {
        int c = 0;
        for (int i = 0; i < a.length; i++) c += Long.bitCount(a[i] & b[i]);
        return c;
    }

    /** T(a,b) = |a AND b| / |a OR b|. Two all-zero fingerprints are defined as identical (1.0). */
    public static double similarity(long[] a, long[] b) {
        return similarity(a, popcount(a), b, popcount(b));
    }

    /** Same as {@link #similarity(long[], long[])} but with precomputed popcounts (the hot-path variant). */
    public static double similarity(long[] a, int popA, long[] b, int popB) {
        int inter = andCount(a, b);
        int union = popA + popB - inter;
        return union == 0 ? 1.0 : (double) inter / union;
    }
}
