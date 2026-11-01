package tw.h39.compat;

/** Pure Java timing rules; does not depend on Minecraft classes. */
public final class H39PulseSchedule {
    public static final long INTERVAL_TICKS = 20L;
    public static final long DURATION_TICKS = 200L;
    private H39PulseSchedule() {}
    public static long nextOnHit(long gameTick) { return gameTick + INTERVAL_TICKS; }
    public static long endOnHit(long gameTick) { return gameTick + DURATION_TICKS; }
    public static boolean isExpired(long now, long next, long end) {
        return now > end || next > end;
    }
    public static boolean shouldPulse(long now, long next, long end) {
        return !isExpired(now, next, end) && now >= next;
    }
    public static long nextAfterPulse(long now) { return now + INTERVAL_TICKS; }
}
