import io.coldmew.hush.TvSkipPolicy;

public class TvSkipPolicyTest {
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        TvSkipPolicy p = new TvSkipPolicy();
        check(!p.shouldSkip(true, false, true, false, 1000, 0)); // paused ad
        check(!p.shouldSkip(true, true, false, false, 1000, 0)); // helper missing
        check(p.shouldSkip(true, true, true, false, 1000, 0));
        check(!p.shouldSkip(true, true, true, false, 1001, 0)); // repeated flag
        check(!p.shouldSkip(false, true, true, true, 1002, 0)); // transient music while restarting
        check(!p.shouldSkip(true, true, true, false, 1003, 0));
        check(!p.shouldSkip(false, true, true, false, 2000, 0)); // actual music resets latch
        check(!p.shouldSkip(true, true, true, false, 2001, 2000)); // timer expired
        check(p.shouldSkip(true, true, true, false, 2002, 10000));
        System.out.println("TvSkipPolicyTest passed");
    }
}
