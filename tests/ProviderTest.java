import dev.aurora.tvpatch.Provider;

public class ProviderTest {
    public static void main(String[] args) throws Exception {
        Provider.Result sync = Provider.parse("{\"syncedLyrics\":\"[00:01.00]Original words\",\"plainLyrics\":\"Original words\",\"instrumental\":false}");
        assert sync.synced.equals("[00:01.00]Original words");
        assert sync.plain.equals("Original words");
        assert !sync.instrumental;
        Provider.Result plain = Provider.parse("{\"syncedLyrics\":null,\"plainLyrics\":\"Original plain words\"}");
        assert plain.synced.isEmpty();
        assert plain.plain.equals("Original plain words");
        Provider.Result instrumental = Provider.parse("{\"syncedLyrics\":null,\"plainLyrics\":null,\"instrumental\":true}");
        assert instrumental.instrumental && instrumental.synced.isEmpty() && instrumental.plain.isEmpty();
        String query = Provider.query("Title & more", "Artist/Name", "Album", 123000);
        assert query.equals("track_name=Title+%26+more&artist_name=Artist%2FName&album_name=Album&duration=123");
        assert !Provider.query("Title", "Artist", "", 0).contains("duration");
        assert !Provider.query("Title", "Artist", "", 3601000).contains("duration");
        boolean rejected = false;
        try { Provider.query("", "Artist", "", 0); } catch (IllegalArgumentException expected) { rejected = true; }
        assert rejected;
        System.out.println("Provider response/query: 10 assertions passed");
    }
}
