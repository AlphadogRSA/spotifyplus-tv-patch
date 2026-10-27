// SPDX-License-Identifier: GPL-3.0-only
package io.coldmew.hush;

/** One restart attempt per ad break. A running restart cannot clear the latch. */
public final class TvSkipPolicy {
    private boolean attempted;

    public synchronized boolean shouldSkip(boolean ad, boolean active, boolean ready,
                                           boolean busy, long now, long sleepAt) {
        if (busy) return false;
        if (!ad && active) attempted = false;
        if (!ad || !active || !ready || attempted || (sleepAt > 0 && now >= sleepAt)) return false;
        attempted = true;
        return true;
    }
}
