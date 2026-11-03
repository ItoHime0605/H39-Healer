package tw.h39.compat;

/**
 * Persist only the shared H-39 round count, not four individual tube counters.
 *
 * The Minecraft/NeoForge item component adapter must call read/write on the
 * actual gun ItemStack. This class is deliberately free of game dependencies.
 */
public final class H39AmmoState {
    private H39AmmoState() {}

    /** A new H-39 begins empty; a missing tag must never grant free rounds. */
    public static H39AmmoCounter newGun() {
        return H39AmmoCounter.empty();
    }

    /** Stored value is the number of rounds, 0 through 20 inclusive. */
    public static int write(H39AmmoCounter counter) {
        return counter.rounds();
    }

    /**
     * Invalid or absent state is treated as empty rather than manufacturing
     * ammunition. This also prevents old four-tube state from being read here.
     */
    public static H39AmmoCounter read(Integer stored) {
        if (stored == null || stored < 0 || stored > H39AmmoCounter.CAPACITY) {
            return H39AmmoCounter.empty();
        }
        return new H39AmmoCounter(stored);
    }

    /** Front-to-back tube bones: emptied FRONT tubes disappear first. */
    public static boolean[] visibleFromFront(H39AmmoCounter counter) {
        boolean[] visible = new boolean[4];
        int n = counter.visibleTubes();
        for (int i = 4 - n; i < 4; i++) visible[i] = true;
        return visible;
    }
}
