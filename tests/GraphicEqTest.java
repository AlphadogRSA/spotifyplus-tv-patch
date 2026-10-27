import dev.aurora.tvpatch.GraphicEq;
import dev.aurora.tvpatch.AudioEffectPlan;
import java.util.Arrays;

public class GraphicEqTest {
    private static class Hardware implements AudioEffectPlan.Target {
        short[] levels = new short[3]; int writes;
        public void band(short band, short gain) { levels[band] = gain; writes++; }
        public void equalizerEnabled(boolean value) {}
        public void boost(int gain) {}
        public void boostEnabled(boolean value) {}
    }
    public static void main(String[] args) {
        int[] gains = {0, 100, 200, 300, 400, 500, 600, 500, 400, 300};
        assert GraphicEq.levelAt(gains, 31) == 0;
        assert GraphicEq.levelAt(gains, 62) == 100;
        assert GraphicEq.levelAt(gains, Math.sqrt(62 * 125)) == 150;
        assert GraphicEq.levelAt(gains, 1) == 0;
        assert GraphicEq.levelAt(gains, 30000) == 300;
        assert GraphicEq.adjust(590, 1) == 600;
        assert GraphicEq.adjust(-590, -1) == -600;
        assert GraphicEq.adjust(0, -1) == -50;
        int[] flat = GraphicEq.fromLegacy(new int[]{3, 3, 3, 3, 3});
        for (int value : flat) assert value == 300;
        int[] slope = GraphicEq.fromLegacy(new int[]{6, 3, 0, -3, -6});
        assert slope[0] == 600 && slope[9] == -600;
        for (int i=1; i<slope.length; i++) assert slope[i] <= slope[i-1];
        AudioEffectPlan plan = new AudioEffectPlan((short)-300, (short)300, new int[]{31000, 1000000, 16000000});
        Hardware hardware = new Hardware();
        plan.applyGraphic(gains, 0, hardware);
        assert Arrays.equals(hardware.levels, new short[]{0, 300, 300});
        int writes = hardware.writes;
        plan.applyGraphic(gains, 0, hardware); assert hardware.writes == writes;
        gains[0] = -100; plan.applyGraphic(gains, 0, hardware);
        assert hardware.writes == writes + 1 && hardware.levels[0] == -100;
        System.out.println("GraphicEqTest passed: interpolation, migration, limits and hardware mapping/cache");
    }
}
