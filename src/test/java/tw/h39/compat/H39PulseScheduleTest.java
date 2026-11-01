package tw.h39.compat;

public final class H39PulseScheduleTest {
    private static int checks = 0;
    public static void main(String[] args) {
        long hit = 100;
        check(H39PulseSchedule.nextOnHit(hit) == 120, "first pulse");
        check(H39PulseSchedule.endOnHit(hit) == 300, "end");
        check(!H39PulseSchedule.shouldPulse(119, 120, 300), "early");
        check(H39PulseSchedule.shouldPulse(120, 120, 300), "on time");
        check(H39PulseSchedule.shouldPulse(300, 300, 300), "last pulse");
        check(!H39PulseSchedule.shouldPulse(301, 300, 300), "expired");
        check(H39PulseSchedule.nextAfterPulse(150) == 170, "lag does not catch up");
        check(H39PulseSchedule.nextOnHit(155) == 175, "hit refresh");

        long next = H39PulseSchedule.nextOnHit(hit);
        long end = H39PulseSchedule.endOnHit(hit);
        int pulses = 0;
        for (long tick = hit; tick <= end + 20; tick++) {
            if (H39PulseSchedule.shouldPulse(tick, next, end)) {
                pulses++;
                next = H39PulseSchedule.nextAfterPulse(tick);
            }
        }
        check(pulses == 10, "exactly ten pulses");

        long refreshed = 190;
        next = H39PulseSchedule.nextOnHit(refreshed);
        end = H39PulseSchedule.endOnHit(refreshed);
        check(next == 210 && end == 390, "refresh replaces existing schedule");
        check(!H39PulseSchedule.shouldPulse(200, next, end), "old pulse not retained");
        System.out.println("H39PulseSchedule: " + checks + " checks passed");
    }
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++;
    }
}
