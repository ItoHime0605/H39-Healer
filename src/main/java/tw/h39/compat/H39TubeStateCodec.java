package tw.h39.compat;

/**
 * Serializes the four individual medical tube counters as a fixed four-int array.
 * The caller must store this array on the individual gun item, not the player.
 * This class deliberately has no Minecraft dependency and can be unit tested.
 */
public final class H39TubeStateCodec {
    private H39TubeStateCodec() {}

    public static int[] encode(H39TubeMagazine magazine) {
        return magazine.tubes();
    }

    /** Rejects malformed or legacy data rather than silently granting ammunition. */
    public static H39TubeMagazine decode(int[] stored) {
        if (stored == null) throw new IllegalArgumentException("Missing tube state");
        return new H39TubeMagazine(stored);
    }

    /**
     * First-use initialization must be explicit. Never use this when the tag is
     * missing from an already-used weapon or ammo would be duplicated.
     */
    public static H39TubeMagazine newWeapon() {
        return H39TubeMagazine.empty();
    }
}
