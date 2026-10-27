package dev.aurora.tvpatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.session.MediaSession;
import android.os.SystemClock;

/** Independent protocol adapter; the GPL Hush implementation lives in a separate APK. */
public final class HushClient {
    private static final String PACKAGE = "dev.aurora.hushtv";
    private static final String PERMISSION = PACKAGE + ".permission.TV_BRIDGE";
    private static long lastSent;
    private HushClient() {}

    private static boolean trusted(Context c) {
        return c.getPackageManager().checkSignatures(c.getPackageName(), PACKAGE) == PackageManager.SIGNATURE_MATCH;
    }

    public static void signal(Context c, MediaSession.Token token, boolean ad, long sleepAt, boolean force) {
        if (token == null) return;
        long now = SystemClock.elapsedRealtime();
        if (!force && now - lastSent < 10000) return;
        lastSent = now;
        if (!trusted(c)) return;
        try {
            c.sendBroadcast(new Intent(PACKAGE + ".TV_STATE")
                    .setComponent(new ComponentName(PACKAGE, "io.coldmew.hush.TvReceiver"))
                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    .putExtra("session", token).putExtra("ad", ad)
                    .putExtra("stamp", now).putExtra("sleep_at", sleepAt), PERMISSION);
        } catch (RuntimeException ignored) { /* Existing per-player muting continues. */ }
    }

    public static void open(Activity a) {
        if (!trusted(a)) {
            new AlertDialog.Builder(a).setTitle("Hush companion setup")
                    .setMessage("Install the Hush-TV.apk built with this Spotify+ TV release and the same signing key. Then set up Hush’s helper or authorize Shizuku.\n\nMute detected ads remains available while Hush is not ready.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        try { a.startActivity(new Intent().setComponent(new ComponentName(PACKAGE, "io.coldmew.hush.TvActivity"))); }
        catch (RuntimeException ignored) {
            new AlertDialog.Builder(a).setMessage("Hush could not open. Reinstall the matching companion APK.").setPositiveButton("OK", null).show();
        }
    }
}
