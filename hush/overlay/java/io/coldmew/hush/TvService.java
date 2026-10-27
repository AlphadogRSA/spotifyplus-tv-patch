// SPDX-License-Identifier: GPL-3.0-only
package io.coldmew.hush;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/** Keeps restart/recovery alive after the broadcast returns and Spotify has been stopped. */
public class TvService extends Service {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!new Prefs(TvService.this).enabled()) { stopSelf(); return; }
            TvBridge.recheck();
            ui.postDelayed(this, 2000);
        }
    };
    static void enabled(Context c, boolean on) {
        Intent intent = new Intent(c, TvService.class);
        if (on) c.startForegroundService(intent); else c.stopService(intent);
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!new Prefs(this).enabled()) { stopSelf(); return START_NOT_STICKY; }
        startForeground(Hush.NOTIF_STATUS, Hush.statusNotification(this));
        ui.removeCallbacks(tick); ui.post(tick);
        return START_STICKY;
    }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { ui.removeCallbacks(tick); super.onDestroy(); }
}
