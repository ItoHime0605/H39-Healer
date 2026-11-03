package tw.h39.compat;

public final class H39ReloadTimingTest {
    private static int checks;

    public static void main(String[] args) {
        for (int rounds = 0; rounds <= H39AmmoCounter.CAPACITY; rounds++) {
            long duration = H39ReloadTiming.durationTicks(rounds);
            check(duration == (rounds == 0 ? 64 : 56), "duration rounds=" + rounds);
            for (long start : new long[] {0, 1, 100, 123456}) {
                long finish = H39ReloadTiming.completeAt(start, rounds);
                check(!H39ReloadTiming.isComplete(finish - 1, finish), "too early");
                check(H39ReloadTiming.isComplete(finish, finish), "finish");
                check(H39ReloadTiming.isComplete(finish + 1, finish), "after finish");
            }
        }
        for (int invalid : new int[]{-1, 21}) {
            boolean throwsError = false;
            try {
                H39ReloadTiming.durationTicks(invalid);
            } catch (IllegalArgumentException expected) {
                throwsError = true;
            }
            check(throwsError, "invalid ammo count");
        }
        System.out.println("H39ReloadTiming: " + checks + " checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
