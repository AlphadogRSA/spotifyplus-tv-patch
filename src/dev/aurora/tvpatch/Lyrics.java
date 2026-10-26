package dev.aurora.tvpatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Original line-timed LRC parser. No online lyrics provider. */
public final class Lyrics {
    public static final class Line {
        public final long time;
        public final String text;
        Line(long time, String text) { this.time = time; this.text = text; }
    }
    public static List<Line> parse(String source) {
        List<Line> result = new ArrayList<>();
        Matcher off = Pattern.compile("\\[offset:([+-]?\\d+)\\]", Pattern.CASE_INSENSITIVE).matcher(source);
        long offset = 0;
        if (off.find()) try { offset = Math.max(-86400000, Math.min(86400000, Long.parseLong(off.group(1)))); } catch (NumberFormatException ignored) { }
        Pattern stamp = Pattern.compile("\\[(\\d{1,4}):([0-5]\\d)(?:[.:](\\d{1,3}))?\\]");
        for (String row : source.split("\\r?\\n")) {
            Matcher m = stamp.matcher(row);
            String text = m.replaceAll("").trim();
            m.reset();
            while (m.find()) {
                String fraction = m.group(3) == null ? "000" : (m.group(3) + "00").substring(0, 3);
                long time = Long.parseLong(m.group(1)) * 60000 + Long.parseLong(m.group(2)) * 1000 + Long.parseLong(fraction) + offset;
                result.add(new Line(time, text));
            }
        }
        Collections.sort(result, Comparator.comparingLong(line -> line.time));
        return result;
    }
    public static int active(List<Line> lines, long position) {
        int found = -1;
        for (int i = 0; i < lines.size(); i++) { if (lines.get(i).time > position) break; found = i; }
        return found;
    }
    public static long position(long base, long updated, long now, boolean playing, float speed, long duration) {
        long value = base + (playing ? (long) (Math.max(0, now - updated) * speed) : 0);
        return Math.max(0, Math.min(duration > 0 ? duration : Long.MAX_VALUE, value));
    }
}
