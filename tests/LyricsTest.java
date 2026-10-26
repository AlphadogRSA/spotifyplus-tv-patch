import dev.aurora.tvpatch.Lyrics;
import java.util.List;

public class LyricsTest {
    public static void main(String[] args) {
        List<Lyrics.Line> lines = Lyrics.parse("[offset:-200]\n[00:01.20][00:03.456]Hello\n[00:02.5]World\n[ar:Author]\ninvalid");
        assert lines.size() == 3;
        assert lines.get(0).time == 1000;
        assert lines.get(1).time == 2300;
        assert lines.get(2).time == 3256;
        assert lines.get(2).text.equals("Hello");
        assert Lyrics.active(lines, 999) == -1;
        assert Lyrics.active(lines, 1000) == 0;
        assert Lyrics.active(lines, 2300) == 1;
        assert Lyrics.position(3000, 1000, 5000, false, 1, 10000) == 3000;
        assert Lyrics.position(3000, 1000, 5000, true, 1, 10000) == 7000;
        assert Lyrics.position(9000, 1000, 5000, true, 1, 10000) == 10000;
        assert Lyrics.position(3000, 5000, 1000, true, 1, 10000) == 3000;
        assert Lyrics.parse("[offset:99999999999999999999999]\n[00:01.0]Test").get(0).time == 1000;
        System.out.println("Lyrics parser/timing: 13 assertions passed");
    }
}
