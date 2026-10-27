package dev.aurora.tvpatch;

import android.content.SharedPreferences;

/** Persist the new curve once, leaving previous five-band values available for older builds. */
public final class GraphicEqSettings {
    private GraphicEqSettings() {}
    public static int[] read(SharedPreferences prefs) {
        int[] values = new int[GraphicEq.BANDS];
        if (!prefs.getBoolean("graphicEqMigrated", false)) {
            int[] old = new int[5];
            for (int i = 0; i < old.length; i++) old[i] = prefs.getInt("eq" + i, 0);
            values = GraphicEq.fromLegacy(old);
            SharedPreferences.Editor edit = prefs.edit().putBoolean("graphicEqMigrated", true);
            for (int i = 0; i < values.length; i++) edit.putInt("graphicEq" + i, values[i]);
            edit.apply();
        } else for (int i = 0; i < values.length; i++) values[i] = GraphicEq.clamp(prefs.getInt("graphicEq" + i, 0));
        return values;
    }
}
