// SPDX-License-Identifier: GPL-3.0-only
package io.coldmew.hush;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.Collections;
import java.util.List;

/** Native TV tokens are capabilities for one Spotify session; no notification access is needed. */
final class TvBridge {
    private static final Handler UI = new Handler(Looper.getMainLooper());
    private static final TvSkipPolicy POLICY = new TvSkipPolicy();
    private static volatile MediaController current;
    private static MediaController.Callback callback;
    private static volatile boolean nativeAd;
    private static long stamp;
    private static Context app;

    private TvBridge() {}

    static void accept(Context c, MediaSession.Token token, boolean ad, long sentAt, long sleepAt) {
        if (sentAt <= 0 || sentAt < stamp || sentAt > SystemClock.elapsedRealtime()
                || SystemClock.elapsedRealtime() - sentAt > 15000) return;
        MediaController candidate = new MediaController(c, token);
        if (!Hush.SPOTIFY.equals(candidate.getPackageName())) return;
        stamp = sentAt;
        app = c.getApplicationContext();
        app.getSharedPreferences("tv_bridge", 0).edit().putLong("sleep_at", sleepAt).apply();
        nativeAd = ad;
        if (current == null || !current.getSessionToken().equals(token)) {
            if (current != null && callback != null) current.unregisterCallback(callback);
            current = candidate;
            callback = new MediaController.Callback() {
                @Override public void onPlaybackStateChanged(PlaybackState s) { recheck(); }
                @Override public void onMetadataChanged(MediaMetadata m) { recheck(); }
                @Override public void onSessionDestroyed() {
                    if (current != candidate) return;
                    candidate.unregisterCallback(this);
                    current = null;
                    nativeAd = false;
                }
            };
            candidate.registerCallback(callback, UI);
        }
        recheck();
    }

    static void acceptController(Context c, MediaController controller) {
        // The native bridge is authoritative for TV. An optional notification listener may
        // refresh metadata, but must never replace a newer native session or mute the device.
        if (current != null && current.getSessionToken().equals(controller.getSessionToken())) recheck();
    }

    static boolean isNativeAd(MediaController mc) {
        MediaController active = current;
        return nativeAd && active != null && active.getSessionToken().equals(mc.getSessionToken());
    }

    static List<MediaController> sessions(Context c) {
        MediaController active = current;
        return active == null ? Collections.emptyList() : Collections.singletonList(active);
    }

    static boolean mayResume(Context c) {
        long end = c.getSharedPreferences("tv_bridge", 0).getLong("sleep_at", 0);
        return new Prefs(c).enabled() && (end <= 0 || System.currentTimeMillis() < end);
    }

    static boolean hasSession() { return current != null; }

    static void recheck() {
        if (Looper.myLooper() != Looper.getMainLooper()) { UI.post(TvBridge::recheck); return; }
        Context c = app;
        MediaController mc = current;
        if (c == null || mc == null) return;
        Prefs p = new Prefs(c);
        if (!p.enabled() || !Prefs.MODE_RESTART.equals(p.mode())) return;
        PlaybackState ps = mc.getPlaybackState();
        boolean active = ps != null && (ps.getState() == PlaybackState.STATE_PLAYING
                || ps.getState() == PlaybackState.STATE_BUFFERING || ps.getState() == PlaybackState.STATE_CONNECTING);
        MediaMetadata m = mc.getMetadata();
        boolean ad = AdWatcher.isAd(mc);
        if (!ad && m != null && !Skipper.busy()) {
            AdWatcher.lastTrack = m.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
            AdWatcher.lastContext = m.getString(AdWatcher.KEY_CONTEXT);
        }
        long now = System.currentTimeMillis();
        long end = c.getSharedPreferences("tv_bridge", 0).getLong("sleep_at", 0);
        boolean ready = Shell.ready() && (Skipper.lastFailAt <= 0 || now - Skipper.lastFailAt >= 120000);
        if (POLICY.shouldSkip(ad, active, ready, Skipper.busy(), now, end)) Skipper.start(c, false);
    }
}
