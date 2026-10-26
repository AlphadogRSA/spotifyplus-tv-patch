package dev.aurora.tvpatch;

/** Local volume policy only. Does not skip ads, edit subscription state or change device volume. */
public final class AdMuteState {
    private boolean enabled = true;
    private boolean ad;
    private long sampledAt;
    private Integer requested;
    public synchronized int requested(int volume, long now) { requested = volume; return desired(now); }
    public synchronized void ad(boolean playing, long now) { ad = playing; sampledAt = now; }
    public synchronized void enabled(boolean value) { enabled = value; }
    public synchronized void resetPlayback() { ad = false; }
    public synchronized boolean muted(long now) { return requested != null && enabled && ad && now >= sampledAt && now - sampledAt <= 180000; }
    public synchronized Integer desired(long now) { return requested == null ? null : muted(now) ? 0 : requested; }
}
