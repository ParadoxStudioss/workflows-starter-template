package site.paradoxstudios.freight;

/**
 * Globally deterministic railway alignment shared by independently generated
 * chunks. The long-distance primary routes retain their existing junctions
 * and stations, while their open-country stretches use two broad S-curves.
 *
 * Curve endpoints and the straight track beds are coordinate-derived: there
 * is no mutable RNG and therefore no order-dependent chunk seam.
 */
public final class RailwayAlignment {
    private RailwayAlignment() {}

    public static final int FIRST_START = 120;
    public static final int FIRST_END = 184;
    public static final int SECOND_START = 216;
    public static final int SECOND_END = 280;

    /** Smaller regional spacings retain the original straight layout. */
    public static boolean curved(int span) {
        return span >= 320;
    }

    public static int section(int along, int span) {
        return Math.floorDiv(along - 8, span);
    }

    public static int phase(int along, int span) {
        return Math.floorMod(along - 8, span);
    }

    public static int line(int coordinate, int span) {
        return 8 + Math.floorDiv(coordinate - 8 + span / 2, span) * span;
    }

    public static int offset(long worldSeed, boolean eastWest, int section, int line) {
        long n = worldSeed ^ (eastWest ? 0x5EAF1288D79CL : 0xC4C7C390AEDDL);
        n ^= ((long) section) * 0x9E3779B97F4A7C15L;
        n ^= ((long) line) * 0xC2B2AE3D27D4EB4FL;
        n = (n ^ (n >>> 30)) * 0xBF58476D1CE4E5B9L;
        n = (n ^ (n >>> 27)) * 0x94D049BB133111EBL;
        n ^= (n >>> 31);
        int magnitude = 8 + (int) Math.floorMod(n, 4L) * 2;
        return (n & 16L) == 0L ? magnitude : -magnitude;
    }

    /** A block exists here only on a tangent or at a Bezier endpoint. */
    public static boolean isTrackBlock(int along, int span) {
        if (!curved(span)) return true;
        int p = phase(along, span);
        return p <= FIRST_START
            || (p >= FIRST_END && p <= SECOND_START)
            || p >= SECOND_END;
    }

    public static int offsetAt(long seed, boolean eastWest, int along, int line, int span) {
        if (!curved(span)) return 0;
        int p = phase(along, span);
        if (p < FIRST_END || p > SECOND_START) return 0;
        return offset(seed, eastWest, section(along, span), line);
    }

    /** Centerline including smoothly curved areas, used for ballast paving. */
    public static int lateralAt(long seed, boolean eastWest, int along, int line, int span) {
        if (!curved(span)) return line;
        int p = phase(along, span);
        int displacement = offset(seed, eastWest, section(along, span), line);
        if (p <= FIRST_START || p >= SECOND_END) return line;
        if (p >= FIRST_END && p <= SECOND_START) return line + displacement;
        if (p < FIRST_END) {
            double t = (p - FIRST_START) / (double) (FIRST_END - FIRST_START);
            return line + (int)Math.round(displacement * smoothstep(t));
        }
        double t = (p - SECOND_START) / (double) (SECOND_END - SECOND_START);
        return line + (int)Math.round(displacement * (1.0 - smoothstep(t)));
    }

    public static boolean onCurve(int along, int span) {
        if (!curved(span)) return false;
        int p = phase(along, span);
        return p > FIRST_START && p < FIRST_END || p > SECOND_START && p < SECOND_END;
    }

    public static int[] curveRange(int along, int span) {
        int origin = 8 + section(along, span) * span;
        int p = phase(along, span);
        if (p >= FIRST_START && p <= FIRST_END)
            return new int[]{origin + FIRST_START, origin + FIRST_END};
        if (p >= SECOND_START && p <= SECOND_END)
            return new int[]{origin + SECOND_START, origin + SECOND_END};
        return null;
    }

    private static double smoothstep(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }
}
