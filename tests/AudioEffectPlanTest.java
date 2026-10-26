import dev.aurora.tvpatch.AudioEffectPlan;
import java.util.Arrays;

public class AudioEffectPlanTest {
    private static final class Hardware implements AudioEffectPlan.Target {
        final short[] levels;
        int calls, bandCalls, boost, eqEnables, boostEnables;
        boolean boostOn, failNextBand;
        Hardware(int bands) { levels = new short[bands]; }
        public void band(short band, short value) {
            if (failNextBand) { failNextBand = false; throw new IllegalStateException("temporary hardware failure"); }
            levels[band] = value; calls++; bandCalls++;
        }
        public void equalizerEnabled(boolean value) { assert value; calls++; eqEnables++; }
        public void boost(int value) { boost = value; calls++; }
        public void boostEnabled(boolean value) { boostOn = value; calls++; boostEnables++; }
    }
    public static void main(String[] args) {
        AudioEffectPlan plan = new AudioEffectPlan((short)-1200, (short)1200,
            new int[]{60000, 230000, 910000, 3600000, 14000000});
        Hardware hardware = new Hardware(5);
        int[] gains = {0, 0, 0, 0, 0};
        plan.apply(gains, 0, hardware);
        assert hardware.calls == 8 : "First application must initialize five bands and all effect controls";
        int initial = hardware.calls;
        for (int poll = 0; poll < 100; poll++) plan.apply(gains, 0, hardware);
        assert hardware.calls == initial : "Unchanged discoveries must cause no hardware writes";
        gains[0] = 6; plan.apply(gains, 0, hardware);
        assert hardware.calls == initial + 1 && hardware.levels[0] == 600;
        assert Arrays.equals(hardware.levels, new short[]{600, 0, 0, 0, 0});
        plan.apply(gains, 300, hardware);
        assert hardware.boost == 300 && hardware.boostOn && hardware.boostEnables == 2;
        int enabledCalls = hardware.boostEnables;
        plan.apply(gains, 400, hardware);
        assert hardware.boost == 400 && hardware.boostEnables == enabledCalls;
        plan.apply(gains, 0, hardware);
        assert !hardware.boostOn && hardware.boost == 0;
        plan.invalidate(); initial = hardware.calls; plan.apply(gains, 0, hardware);
        assert hardware.calls == initial + 8 : "Restored effect control must fully reapply settings";
        AudioEffectPlan limited = new AudioEffectPlan((short)-300, (short)300, new int[]{80000, 12000000});
        Hardware limitedHardware = new Hardware(2);
        limited.apply(new int[]{6, 0, 0, 0, -6}, 0, limitedHardware);
        assert Arrays.equals(limitedHardware.levels, new short[]{300, -300});
        AudioEffectPlan retry = new AudioEffectPlan((short)-1200, (short)1200, new int[]{60000});
        Hardware flaky = new Hardware(1); flaky.failNextBand = true;
        try { retry.apply(gains, 0, flaky); throw new AssertionError("Expected hardware failure"); } catch (IllegalStateException expected) { }
        retry.apply(gains, 0, flaky);
        assert flaky.calls == 4 && flaky.levels[0] == 600 : "Failed settings cannot become cached as applied";
        System.out.println("Audio effect settings/cache: 10 assertions passed");
    }
}
