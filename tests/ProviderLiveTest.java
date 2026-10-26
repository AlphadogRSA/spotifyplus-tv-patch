import dev.aurora.tvpatch.Provider;

/** Optional online smoke test using metadata from LRCLIB's own API documentation. */
public class ProviderLiveTest {
    public static void main(String[] args) throws Exception {
        Provider.Result result = Provider.fetch("I Want to Live", "Borislav Slavov", "Baldur's Gate 3 (Original Game Soundtrack)", 233000);
        if (!result.instrumental && result.synced.isEmpty() && result.plain.isEmpty()) throw new AssertionError("Empty provider record");
        System.out.println("LRCLIB HTTPS lookup passed; timed=" + !result.synced.isEmpty() + ", plain=" + !result.plain.isEmpty() + ", instrumental=" + result.instrumental);
    }
}
