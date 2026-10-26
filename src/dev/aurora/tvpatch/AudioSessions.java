package dev.aurora.tvpatch;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Android 10 AudioFlinger track-table reader: active tracks owned by exactly our PID. */
public final class AudioSessions {
    private static final Pattern TRACK = Pattern.compile("^\\s*(?:[A-Za-z]+\\s+)?\\d+\\s+(yes|no)\\s+(\\d+)\\s+(\\d+)\\s+\\d+\\s+[A-Za-z]\\s+.*$");
    public static Set<Integer> find(String dump, int pid) {
        Set<Integer> result = new LinkedHashSet<>();
        boolean table = false;
        for (String line : dump.split("\\r?\\n")) {
            if (line.contains("Type") && line.contains("Active") && line.contains("Client") && line.contains("Session")) {
                table = line.trim().matches("Type\\s+Id\\s+Active\\s+Client\\s+Session\\s+Port\\s+Id(?:\\s+.*)?");
                continue;
            }
            if (!table) continue;
            Matcher match = TRACK.matcher(line);
            if (!match.matches()) { if (!line.trim().isEmpty()) table = false; continue; }
            try {
                int owner = Integer.parseInt(match.group(2)), session = Integer.parseInt(match.group(3));
                if (owner == pid && session > 0 && match.group(1).equals("yes")) result.add(session);
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }
}
