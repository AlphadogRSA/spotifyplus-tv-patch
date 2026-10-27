package dev.aurora.tvpatch;

/** Persisted wall-clock deadline, shared across Spotify process restarts. */
public final class SleepDeadline {
    private SleepDeadline() {}
    public static boolean expired(long deadline, long now) { return deadline > 0 && now >= deadline; }
}
