package tw.h39.compat;

/** Server-tick timing for the H-39 custom five-round physical-tube reload. */
public final class H39ReloadTiming {
    public static final long TACTICAL_TICKS = 56L; // 2.8 seconds
    public static final long EMPTY_TICKS = 64L;    // 3.2 seconds

    private H39ReloadTiming() {}

    public static long durationTicks(int roundsBefore) {
        if (roundsBefore < 0 || roundsBefore > H39AmmoCounter.CAPACITY) {
            throw new IllegalArgumentException("Invalid rounds: " + roundsBefore);
        }
        return roundsBefore == 0 ? EMPTY_TICKS : TACTICAL_TICKS;
    }

    public static long completeAt(long gameTick, int roundsBefore) {
        return gameTick + durationTicks(roundsBefore);
    }

    public static boolean isComplete(long gameTick, long completeAt) {
        return gameTick >= completeAt;
    }
}
