import dev.aurora.tvpatch.AdMuteState;

public class AdMuteStateTest {
    public static void main(String[] args) {
        AdMuteState state = new AdMuteState();
        assert state.desired(0) == null;
        assert state.requested(42000, 0) == 42000;
        state.ad(true, 10);
        assert state.desired(10) == 0;
        assert state.requested(38000, 20) == 0;
        state.ad(false, 30);
        assert state.desired(30) == 38000;
        state.ad(true, 40);
        state.enabled(false);
        assert state.desired(40) == 38000;
        state.enabled(true);
        assert state.desired(40) == 0;
        assert state.desired(180041) == 38000;
        state.ad(true, 180050);
        assert state.desired(180050) == 0;
        state.resetPlayback();
        assert state.desired(180051) == 38000;
        assert state.requested(0, 180052) == 0;
        state.ad(true, 180053); state.ad(false, 180054);
        assert state.desired(180054) == 0;
        AdMuteState unknown = new AdMuteState(); unknown.ad(true, 100);
        assert unknown.desired(100) == null;
        System.out.println("Ad-mute state: 13 assertions passed");
    }
}
