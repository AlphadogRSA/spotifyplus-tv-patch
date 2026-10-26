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

/** Effects are attached only to verified positive sessions of this process. Never output mix 0. */
public final class AudioDsp {
    private static final ExecutorService WORK = Executors.newSingleThreadExecutor();
    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor();
    private static final Map<Integer, Effects> effects = new HashMap<>();
    private static volatile String status = "Off";
    private static boolean busy;
    private static long nextPoll;
    private static volatile long generation;
    private static final int[] FREQUENCIES = {60, 230, 910, 3600, 14000};
    public static String status() { return status; }
    public static synchronized void refresh() { nextPoll = 0; generation++; }
    public static synchronized void stop() {
        generation++; nextPoll = 0; status = "Standby";
        WORK.execute(AudioDsp::releaseAll);
    }
    public static synchronized void tick(Context context, boolean playing) {
        SharedPreferences prefs = context.getSharedPreferences("spotifyplus_tv", Context.MODE_PRIVATE);
        final boolean enabled = prefs.getBoolean("dspEnabled", false);
        final long now = SystemClock.elapsedRealtime();
        if (busy || now < nextPoll) return;
        nextPoll = now + 3000; busy = true;
        final long revision = generation;
        final int boost = Math.max(0, Math.min(600, prefs.getInt("boostMb", 0)));
        final int[] gains = new int[5];
        for (int i = 0; i < 5; i++) gains[i] = Math.max(-6, Math.min(6, prefs.getInt("eq" + i, 0)));
        final Context app = context.getApplicationContext();
        WORK.execute(() -> {
            try {
                if (!enabled || !playing) { releaseAll(); status = enabled ? "Ready · start playback" : "Off"; return; }
                if (app.checkSelfPermission("android.permission.DUMP") != PackageManager.PERMISSION_GRANTED) {
                    releaseAll(); status = "Audio setup required · see setup guide"; return;
                }
                Set<Integer> sessions = ownSessions();
                if (revision != generation) { releaseAll(); return; }
                for (Integer id : new ArrayList<>(effects.keySet())) if (!sessions.contains(id)) effects.remove(id).release();
                if (sessions.isEmpty()) { status = "Waiting for Spotify audio"; return; }
                for (int id : sessions) {
                    if (revision != generation) { releaseAll(); return; }
                    Effects fx = effects.get(id);
                    if (fx == null) { fx = new Effects(id); effects.put(id, fx); }
                    fx.apply(gains, boost);
                }
                if (revision != generation) { releaseAll(); return; }
                status = "Active · Spotify audio only";
                Log.i("SPTV", "Spotify-session EQ/boost active");
            } catch (Exception failure) { releaseAll(); status = "Audio effect unavailable on this output"; Log.w("SPTV", "Audio effect unavailable: " + failure.getClass().getSimpleName()); }
            finally { synchronized (AudioDsp.class) { busy = false; } }
        });
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
    private static void releaseAll() { for (Effects fx : effects.values()) fx.release(); effects.clear(); }
    private static final class Effects {
        Equalizer eq; LoudnessEnhancer loudness;
        Effects(int session) {
            if (session <= 0) throw new IllegalArgumentException("Positive app session required");
            try { eq = new Equalizer(0, session); loudness = new LoudnessEnhancer(session); }
            catch (RuntimeException failure) { release(); throw failure; }
        }
        void apply(int[] gains, int boost) {
            short[] range = eq.getBandLevelRange();
            for (short band = 0; band < eq.getNumberOfBands(); band++) {
                double hz = eq.getCenterFreq(band) / 1000.0, best = Double.MAX_VALUE; int target = 0;
                for (int i = 0; i < FREQUENCIES.length; i++) {
                    double distance = Math.abs(Math.log(Math.max(1, hz) / FREQUENCIES[i]));
                    if (distance < best) { best = distance; target = i; }
                }
                eq.setBandLevel(band, (short)Math.max(range[0], Math.min(range[1], gains[target] * 100)));
            }
            eq.setEnabled(true);
            loudness.setTargetGain(boost); loudness.setEnabled(boost > 0);
        }
        void release() { if (eq != null) { try { eq.release(); } catch (Exception ignored) { } eq = null; } if (loudness != null) { try { loudness.release(); } catch (Exception ignored) { } loudness = null; } }
    }
}
