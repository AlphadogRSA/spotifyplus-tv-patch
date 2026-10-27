package dev.aurora.tvpatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.os.SystemClock;
import android.util.Log;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Effects are attached only to verified positive sessions of this process. Never output mix 0. */
public final class AudioDsp {
    private static final ExecutorService WORK = Executors.newSingleThreadExecutor();
    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor();
    private static final Map<Integer, Effects> effects = new HashMap<>();
    private static volatile String status = "Off";
    private static boolean busy;
    private static long nextPoll;
    private static volatile long generation;
    private static volatile boolean effectsPresent;
    private static boolean lastPlaying, settingsDirty = true;
    private static Settings settings;
    public static String status() { return status; }
    public static synchronized void refresh() { nextPoll = 0; settingsDirty = true; generation++; }
    public static synchronized void stop() {
        generation++; nextPoll = 0; settingsDirty = true;
        setStatus("Standby");
        WORK.execute(() -> { releaseAll(); setStatus("Standby"); });
    }
    public static synchronized void tick(Context context, boolean playing) {
        // A pause invalidates in-flight discovery before it can attach new effects.
        if (playing != lastPlaying) { lastPlaying = playing; generation++; nextPoll = 0; }
        if (busy) return;
        if (settingsDirty || settings == null) {
            settings = new Settings(context.getSharedPreferences("spotifyplus_tv", Context.MODE_PRIVATE));
            settingsDirty = false;
        }
        final Settings desired = settings;
        final boolean enabled = desired.enabled;
        if ((!enabled || !playing) && !effectsPresent) {
            setStatus(enabled ? "Ready · start playback" : "Off");
            return;
        }
        final long now = SystemClock.elapsedRealtime();
        if (now < nextPoll) return;
        nextPoll = now + 3000; busy = true;
        final long revision = generation;
        final Context app = context.getApplicationContext();
        WORK.execute(() -> {
            try {
                if (!enabled || !playing) {
                    releaseAll();
                    if (revision == generation) setStatus(enabled ? "Ready · start playback" : "Off");
                    return;
                }
                if (app.checkSelfPermission("android.permission.DUMP") != PackageManager.PERMISSION_GRANTED) {
                    releaseAll(); if (revision == generation) setStatus("Audio setup required · see setup guide"); return;
                }
                Set<Integer> sessions = ownSessions();
                if (revision != generation) { releaseAll(); return; }
                for (Integer id : new ArrayList<>(effects.keySet())) if (!sessions.contains(id)) effects.remove(id).release();
                effectsPresent = !effects.isEmpty();
                if (sessions.isEmpty()) { setStatus("Waiting for Spotify audio"); return; }
                for (int id : sessions) {
                    if (revision != generation) { releaseAll(); return; }
                    Effects fx = effects.get(id);
                    if (fx == null) { fx = new Effects(id); effects.put(id, fx); effectsPresent = true; }
                    fx.apply(desired.gains, desired.boost);
                }
                if (revision != generation) { releaseAll(); return; }
                setStatus("Active · Spotify audio only");
            } catch (Exception failure) {
                releaseAll();
                if (revision == generation && setStatus("Audio effect unavailable on this output")) Log.w("SPTV", "Audio effect unavailable: " + failure.getClass().getSimpleName());
            }
            finally { synchronized (AudioDsp.class) { busy = false; } }
        });
    }
    private static synchronized boolean setStatus(String value) {
        if (value.equals(status)) return false;
        status = value; Log.i("SPTV", "Audio: " + value); return true;
    }
    private static Set<Integer> ownSessions() throws Exception {
        Process child = new ProcessBuilder("/system/bin/dumpsys", "-t", "2", "media.audio_flinger").redirectErrorStream(true).start();
        ScheduledFuture<?> deadline = WATCHDOG.schedule(child::destroy, 3, TimeUnit.SECONDS);
        try (InputStream input = child.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > 524288) throw new IllegalStateException("Audio diagnostics exceeded bound");
                output.write(buffer, 0, count);
            }
            if (!child.waitFor(1, TimeUnit.SECONDS) || child.exitValue() != 0) throw new IllegalStateException("Audio diagnostics unavailable");
            // The dump is parsed in memory, never logged, saved or sent over the network.
            return AudioSessions.find(output.toString("UTF-8"), android.os.Process.myPid());
        } finally { deadline.cancel(false); child.destroy(); }
    }
    private static void releaseAll() { for (Effects fx : effects.values()) fx.release(); effects.clear(); effectsPresent = false; }
    private static final class Settings {
        final boolean enabled;
        final int boost;
        final int[] gains;
        Settings(SharedPreferences prefs) {
            enabled = prefs.getBoolean("dspEnabled", false);
            boost = Math.max(0, Math.min(600, prefs.getInt("boostMb", 0)));
            gains = GraphicEqSettings.read(prefs);
        }
    }
    private static final class Effects implements AudioEffectPlan.Target {
        Equalizer eq; LoudnessEnhancer loudness;
        AudioEffectPlan plan;
        final AtomicBoolean controlChanged = new AtomicBoolean(true);
        Effects(int session) {
            if (session <= 0) throw new IllegalArgumentException("Positive app session required");
            try {
                eq = new Equalizer(0, session); loudness = new LoudnessEnhancer(session);
                short[] range = eq.getBandLevelRange();
                int[] centers = new int[eq.getNumberOfBands()];
                for (short band = 0; band < centers.length; band++) centers[band] = eq.getCenterFreq(band);
                plan = new AudioEffectPlan(range[0], range[1], centers);
                eq.setControlStatusListener((effect, hasControl) -> controlChanged.set(true));
                loudness.setControlStatusListener((effect, hasControl) -> controlChanged.set(true));
            }
            catch (RuntimeException failure) { release(); throw failure; }
        }
        void apply(int[] gains, int boost) {
            if (controlChanged.getAndSet(false)) plan.invalidate();
            plan.applyGraphic(gains, boost, this);
        }
        public void band(short band, short level) { eq.setBandLevel(band, level); }
        public void equalizerEnabled(boolean enabled) { eq.setEnabled(enabled); }
        public void boost(int millibels) { loudness.setTargetGain(millibels); }
        public void boostEnabled(boolean enabled) { loudness.setEnabled(enabled); }
        void release() { if (eq != null) { try { eq.release(); } catch (Exception ignored) { } eq = null; } if (loudness != null) { try { loudness.release(); } catch (Exception ignored) { } loudness = null; } }
    }
}
