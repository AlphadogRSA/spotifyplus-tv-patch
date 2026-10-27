// SPDX-License-Identifier: GPL-3.0-only
package io.coldmew.hush;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** A D-pad-first entry point. The upstream setup screens remain available for pairing. */
public class TvActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private TextView status;
    private Button power;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            Prefs p = new Prefs(TvActivity.this);
            power.setText(p.enabled() ? "Hush skipping: on · select to turn off" : "Hush skipping: off · select to turn on");
            boolean signed = getPackageManager().checkSignatures(getPackageName(), Hush.SPOTIFY) == PackageManager.SIGNATURE_MATCH;
            status.setText(!signed ? "Install Spotify+ TV and this companion signed with the same key."
                    : !Shell.ready() ? "Helper is not ready. Ads can still be muted by Spotify+ TV. Set up the helper below."
                    : !TvBridge.hasSession() ? "Helper ready. Open Spotify+ TV and start playback to connect its session."
                    : p.enabled() ? "Hush is enabled. TV restart skipping needs device verification."
                    : "Connected. Turn on Hush to attempt restart skipping during ads.");
            ui.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(64, 32, 64, 32);
        scroll.setBackgroundColor(0xff10191f);
        scroll.addView(body);
        TextView title = new TextView(this);
        title.setText("Hush · Spotify+ TV"); title.setTextColor(0xff91e7cf); title.setTextSize(28);
        body.addView(title);
        status = new TextView(this); status.setTextColor(0xffeeeeee); status.setTextSize(18);
        status.setPadding(0, 20, 0, 20); body.addView(status);
        power = add(body, "Hush skipping", () -> {
            Prefs p = new Prefs(this); p.mode(Prefs.MODE_RESTART);
            Hush.setEnabled(this, !p.enabled()); TvBridge.recheck();
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        });
        add(body, "Set up Hush helper · Android 11+", () -> open(new Intent(this, HelperSetupActivity.class)));
        add(body, "Authorize running Shizuku · Android 8+", () -> {
            if (Shell.running()) Shell.requestPermission();
            else open(new Intent(this, ShizukuSetupActivity.class));
        });
        add(body, "Test Spotify restart", () -> {
            if (!new Prefs(this).enabled() || !Shell.ready() || !TvBridge.hasSession() || !TvBridge.mayResume(this)) {
                Toast.makeText(this, "Enable Hush, start its helper and play Spotify first. Check the sleep timer too.", Toast.LENGTH_LONG).show();
                return;
            }
            new AlertDialog.Builder(this).setTitle("Restart Spotify now?")
                    .setMessage("This interrupts current playback to test recovery. Queue behavior must be checked on your TV.")
                    .setPositiveButton("Test restart", (d, w) -> Skipper.start(this, true))
                    .setNegativeButton("Cancel", null).show();
        });
        add(body, "Return to Spotify+ TV", () -> {
            Intent launch = getPackageManager().getLeanbackLaunchIntentForPackage(Hush.SPOTIFY);
            if (launch == null) launch = getPackageManager().getLaunchIntentForPackage(Hush.SPOTIFY);
            if (launch != null) open(launch); else Toast.makeText(this, "Spotify TV is not installed", Toast.LENGTH_LONG).show();
        });
        TextView notes = new TextView(this);
        notes.setText("Keep ‘Mute detected ads’ on in Spotify+ TV for the fallback. Hush needs Wireless debugging or Shizuku after a reboot. DNS filtering is off by default.\n\nAdapted from coldMEW/Hush 2.0.2 · GPL-3.0. Source and changes: github.com/AlphadogRSA/spotifyplus-tv-patch/tree/feat/hush-tv/hush");
        notes.setTextColor(0xffb9c7d0); notes.setTextSize(15); notes.setPadding(0, 24, 0, 12); body.addView(notes);
        setContentView(scroll); power.requestFocus();
    }

    private Button add(LinearLayout parent, String label, Runnable action) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false);
        button.setOnClickListener(v -> action.run()); parent.addView(button);
        return button;
    }
    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (RuntimeException e) { Toast.makeText(this, "This setup screen is unavailable on your TV", Toast.LENGTH_LONG).show(); }
    }
    @Override protected void onResume() { super.onResume(); ui.post(refresh); }
    @Override protected void onPause() { ui.removeCallbacks(refresh); super.onPause(); }
}
