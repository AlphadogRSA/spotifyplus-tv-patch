import dev.aurora.tvpatch.SleepDeadline;

public class SleepDeadlineTest {
    public static void main(String[] args) {
        assert !SleepDeadline.expired(0, 1000);
        assert !SleepDeadline.expired(2000, 1000);
        assert SleepDeadline.expired(2000, 2000);
        assert SleepDeadline.expired(2000, 3000);
        // Re-creating the process at a later wall-clock time keeps the same deadline.
        assert !SleepDeadline.expired(1801000, 1701000);
        assert SleepDeadline.expired(1801000, 1801001);
        System.out.println("SleepDeadlineTest passed");
    }
}
