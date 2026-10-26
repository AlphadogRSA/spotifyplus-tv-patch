package dev.aurora.tvpatch;

import java.util.Arrays;

/** Per-session EQ layout and successful hardware-setting cache. */
public final class AudioEffectPlan {
    public interface Target {
        void band(short band, short level);
        void equalizerEnabled(boolean enabled);
        void boost(int millibels);
        void boostEnabled(boolean enabled);
    }
    private static final int[] FREQUENCIES = {60, 230, 910, 3600, 14000};
    private final short min, max;
    private final int[] targets;
    private short[] appliedLevels;
    private int[] appliedGains;
    private int appliedBoost;
    public AudioEffectPlan(short min, short max, int[] centerMilliHz) {
        if (min > max || centerMilliHz.length == 0) throw new IllegalArgumentException("Invalid EQ layout");
        this.min = min; this.max = max; targets = new int[centerMilliHz.length];
        for (int band = 0; band < centerMilliHz.length; band++) {
            double hz = Math.max(1, centerMilliHz[band] / 1000.0), best = Double.MAX_VALUE;
            for (int i = 0; i < FREQUENCIES.length; i++) {
                double distance = Math.abs(Math.log(hz / FREQUENCIES[i]));
                if (distance < best) { best = distance; targets[band] = i; }
            }
        }
    }
    public void apply(int[] gainsDb, int boostMb, Target target) {
        if (gainsDb.length != FREQUENCIES.length) throw new IllegalArgumentException("Five controls required");
        boostMb = Math.max(0, Math.min(600, boostMb));
        if (appliedLevels != null && Arrays.equals(gainsDb, appliedGains) && boostMb == appliedBoost) return;
        short[] levels = new short[targets.length];
        for (int band = 0; band < targets.length; band++) levels[band] = (short)Math.max(min, Math.min(max, gainsDb[targets[band]] * 100));
        boolean initial = appliedLevels == null;
        try {
            for (short band = 0; band < targets.length; band++) if (initial || levels[band] != appliedLevels[band]) target.band(band, levels[band]);
            if (initial) target.equalizerEnabled(true);
            if (initial || boostMb != appliedBoost) target.boost(boostMb);
            if (initial || (boostMb > 0) != (appliedBoost > 0)) target.boostEnabled(boostMb > 0);
        } catch (RuntimeException failure) { invalidate(); throw failure; }
        appliedLevels = levels; appliedGains = gainsDb.clone(); appliedBoost = boostMb;
    }
    public void invalidate() { appliedLevels = null; appliedGains = null; }
}
