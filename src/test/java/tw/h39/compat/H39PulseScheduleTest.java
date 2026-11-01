package tw.h39.compat;

public final class H39PulseScheduleTest {
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
        System.out.println("H39PulseSchedule: 8 checks passed");
    }
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
