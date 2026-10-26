import dev.aurora.tvpatch.AudioSessions;
import java.util.Arrays;
import java.util.LinkedHashSet;

public class AudioSessionsTest {
    public static void main(String[] args) {
        String dump = "Global session refs:\n session pid count\n 111 42 1\n"
          + " Type Id Active Client Session Port Id S Flags\n"
          + " 90 yes 42 297 54 A 0x000\n"
          + " S 91 yes 99 305 55 A 0x000\n"
          + " 92 no 42 313 56 I 0x000\n"
          + " 93 yes 42 0 57 A 0x000\n"
          + " S 94 yes 42 321 58 A 0x000\n"
          + " 10-26 22:00:00 AT::remove 95 yes 42 329 59 A\n";
        assert AudioSessions.find(dump, 42).equals(new LinkedHashSet<>(Arrays.asList(297, 321)));
        assert AudioSessions.find(dump, 99).equals(new LinkedHashSet<>(Arrays.asList(305)));
        assert AudioSessions.find(dump, 123).isEmpty();
        assert AudioSessions.find("90 yes 42 297 54 A 0x000\n", 42).isEmpty();
        assert AudioSessions.find("Type Id Active Client Session Port Id\n90 yes 42 -2 54 A 0x000\n", 42).isEmpty();
        assert AudioSessions.find("Type Id Active Client Session Port Id\n90 yes 42 99999999999999 54 A 0x000\n", 42).isEmpty();
        assert AudioSessions.find("Type Id Active Session Client Port Id S Flags\n90 yes 42 777 54 A 0x000\n", 42).isEmpty();
        System.out.println("Audio session isolation: 7 assertions passed");
    }
}
