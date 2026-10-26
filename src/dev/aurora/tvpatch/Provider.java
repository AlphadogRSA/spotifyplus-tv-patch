package dev.aurora.tvpatch;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Independently implemented from LRCLIB's public API contract; no Eevee source copied. */
public final class Provider {
    private static final ScheduledExecutorService DEADLINES = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "lyrics-deadline"); thread.setDaemon(true); return thread;
    });
    public static final class Result {
        public final String synced, plain;
        public final boolean instrumental;
        Result(String synced, String plain, boolean instrumental) {
            this.synced = synced; this.plain = plain; this.instrumental = instrumental;
        }
    }
    public static Result parse(String raw) throws Exception {
        JSONObject value = new JSONObject(raw);
        return new Result(value.isNull("syncedLyrics") ? "" : value.optString("syncedLyrics", ""),
            value.isNull("plainLyrics") ? "" : value.optString("plainLyrics", ""), value.optBoolean("instrumental", false));
    }
    public static String query(String title, String artist, String album, long durationMs) throws Exception {
        if (title == null || title.trim().isEmpty() || artist == null || artist.trim().isEmpty())
            throw new IllegalArgumentException("Start a track with a title and artist first.");
        String query = "track_name=" + URLEncoder.encode(title, "UTF-8") + "&artist_name=" + URLEncoder.encode(artist, "UTF-8");
        if (album != null && !album.trim().isEmpty()) query += "&album_name=" + URLEncoder.encode(album, "UTF-8");
        long seconds = Math.round(durationMs / 1000.0);
        if (seconds >= 1 && seconds <= 3600) query += "&duration=" + seconds;
        return query;
    }
    public static Result fetch(String title, String artist, String album, long durationMs) throws Exception {
        String query = query(title, artist, album, durationMs);
        HttpURLConnection connection = (HttpURLConnection) new URL("https://lrclib.net/api/get?" + query).openConnection();
        final long deadlineAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        ScheduledFuture<?> deadline = DEADLINES.schedule(connection::disconnect, 30, TimeUnit.SECONDS);
        try {
            connection.setConnectTimeout(10000); connection.setReadTimeout(20000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "SpotifyPlusTV-LocalPatch/0.2");
            int status = connection.getResponseCode();
            if (status == 404) throw new IllegalStateException("LRCLIB has no matching lyrics for this track.");
            if (status == 429) throw new IllegalStateException("LRCLIB rate limit. Wait before fetching again.");
            if (status != 200) throw new IllegalStateException("LRCLIB request failed (" + status + ").");
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] bytes = new byte[4096]; int count;
                while (true) {
                    if (System.nanoTime() >= deadlineAt) throw new SocketTimeoutException("Lyrics fetch deadline exceeded");
                    count = input.read(bytes);
                    if (count == -1) break;
                    if (out.size() + count > 524288) throw new IllegalStateException("Lyrics response is too large.");
                    out.write(bytes, 0, count);
                }
                return parse(new String(out.toByteArray(), "UTF-8"));
            }
        } finally { deadline.cancel(false); connection.disconnect(); }
    }
}
