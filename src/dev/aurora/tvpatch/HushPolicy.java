package dev.aurora.tvpatch;

/** Persisted restart gate: no loops if a TV simply resumes the same ad after a cold start. */
public final class HushPolicy {
    private HushPolicy() {}
    public static boolean attempt(boolean enabled, boolean ready, boolean ad, boolean active,
                                  long now, long lastAttempt, long lastFailure, long pending, long sleepAt) {
        return enabled && ready && ad && active && pending == 0
                && (lastAttempt <= 0 || now-lastAttempt >= 30000)
                && (lastFailure <= 0 || now-lastFailure >= 120000)
                && !SleepDeadline.expired(sleepAt, now);
    }
}
