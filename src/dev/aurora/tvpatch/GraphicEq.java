package dev.aurora.tvpatch;

/** A ten-point target curve in millibels; sampled logarithmically at real hardware bands. */
public final class GraphicEq {
    public static final int BANDS = 10, LIMIT = 600, STEP = 50;
    private static final int[] HZ = {31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000};
    private static final int[] LEGACY_HZ = {60, 230, 910, 3600, 14000};
    private GraphicEq() {}
    public static int frequency(int index) { return HZ[index]; }
    public static int adjust(int value, int direction) { return clamp(value + direction * STEP); }
    public static int clamp(int value) { return Math.max(-LIMIT, Math.min(LIMIT, value)); }
    public static int levelAt(int[] gains, double hz) {
        if (gains.length != BANDS) throw new IllegalArgumentException("Ten EQ curve points required");
        return interpolate(HZ, gains, hz);
    }
    public static int[] fromLegacy(int[] gainsDb) {
        if (gainsDb.length != LEGACY_HZ.length) throw new IllegalArgumentException("Five legacy controls required");
        int[] mb = new int[gainsDb.length], result = new int[BANDS];
        for (int i = 0; i < mb.length; i++) mb[i] = clamp(gainsDb[i] * 100);
        for (int i = 0; i < result.length; i++) result[i] = interpolate(LEGACY_HZ, mb, HZ[i]);
        return result;
    }
    private static int interpolate(int[] hz, int[] gains, double value) {
        if (value <= hz[0]) return clamp(gains[0]);
        for (int i = 1; i < hz.length; i++) if (value < hz[i]) {
            double t = Math.log(value / hz[i-1]) / Math.log((double)hz[i] / hz[i-1]);
            return clamp((int)Math.round(gains[i-1] * (1-t) + gains[i] * t));
        }
        return clamp(gains[gains.length-1]);
    }
}
