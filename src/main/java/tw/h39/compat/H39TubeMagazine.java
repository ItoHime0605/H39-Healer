package tw.h39.compat;

import java.util.Arrays;

/**
 * Immutable 4-slot medical tube magazine. A tube contains up to 5 shots.
 * A partial tube is never discarded or topped off during reload.
 * This is independent game logic, not yet wired into TaCZ reload events.
 */
public final class H39TubeMagazine {
    public static final int SLOTS = 4;
    public static final int SHOTS_PER_TUBE = 5;
    private final int[] tubes;

    public H39TubeMagazine(int... tubes) {
        if (tubes.length != SLOTS) throw new IllegalArgumentException("Expected four tube slots");
        this.tubes = tubes.clone();
        for (int shots : tubes) {
            if (shots < 0 || shots > SHOTS_PER_TUBE) throw new IllegalArgumentException("Invalid tube rounds");
        }
    }

    public static H39TubeMagazine full() { return new H39TubeMagazine(5, 5, 5, 5); }
    public static H39TubeMagazine empty() { return new H39TubeMagazine(0, 0, 0, 0); }

    public int[] tubes() { return tubes.clone(); }
    public int totalShots() { return Arrays.stream(tubes).sum(); }
    public int emptySlots() {
        int n = 0;
        for (int tube : tubes) if (tube == 0) n++;
        return n;
    }

    public ShotResult fire() {
        int[] next = tubes.clone();
        for (int i = 0; i < SLOTS; i++) {
            if (next[i] > 0) {
                next[i]--;
                return new ShotResult(new H39TubeMagazine(next), true, next[i] == 0, i);
            }
        }
        return new ShotResult(this, false, false, -1);
    }

    /** Loads up to available empty slots, consuming only whole five-shot tubes. */
    public ReloadResult reload(int availableFullTubes) {
        if (availableFullTubes < 0) throw new IllegalArgumentException("Negative available tubes");
        int[] next = tubes.clone();
        int used = 0;
        for (int i = 0; i < SLOTS && used < availableFullTubes; i++) {
            if (next[i] == 0) {
                next[i] = SHOTS_PER_TUBE;
                used++;
            }
        }
        return new ReloadResult(new H39TubeMagazine(next), used);
    }

    public record ShotResult(H39TubeMagazine magazine, boolean fired, boolean ejectEmptyTube, int slot) {}
    public record ReloadResult(H39TubeMagazine magazine, int tubesConsumed) {}
}
