// SPDX-License-Identifier: GPL-3.0-only
package io.coldmew.hush;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.session.MediaSession;

/** Manifest signature permission protects this endpoint; the peer must also share our signer. */
public class TvReceiver extends BroadcastReceiver {
    public static final String ACTION = "dev.aurora.hushtv.TV_STATE";
    @Override public void onReceive(Context c, Intent i) {
        if (!ACTION.equals(i.getAction())) return;
        if (c.getPackageManager().checkSignatures(c.getPackageName(), Hush.SPOTIFY)
                != PackageManager.SIGNATURE_MATCH) return;
        try {
            MediaSession.Token token = i.getParcelableExtra("session");
            if (token != null) TvBridge.accept(c, token, i.getBooleanExtra("ad", false),
                    i.getLongExtra("stamp", 0), i.getLongExtra("sleep_at", 0));
        } catch (RuntimeException ignored) { /* Invalid parcel or dead session: keep local muting. */ }
    }
}
