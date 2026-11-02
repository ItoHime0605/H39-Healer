package tw.h39.compat;

public final class H39AmmoCounterTest {
    private static int checks;
    public static void main(String[] args) {
        H39AmmoCounter m = H39AmmoCounter.full();
        check(m.hud().equals("20/20") && m.visibleTubes() == 4 && m.activeTube() == 0, "full");
        for (int i = 0; i < 5; i++) {
            var shot = m.fire();
            check(shot.fired(), "shot " + i);
            check(shot.tubeEjected() == (i == 4), "ejection " + i);
            m = shot.magazine();
        }
        check(m.hud().equals("15/20") && m.visibleTubes() == 3 && m.activeTube() == 1, "front tube exhausted");
        m = m.fire().magazine();
        check(m.rounds() == 14 && m.activeTube() == 1, "next tube starts");
        m = new H39AmmoCounter(10);
        check(m.reload(2).magazine().rounds() == 20 && m.reload(2).tubesConsumed() == 2, "10/20 two tubes");
        check(m.reload(1).magazine().hud().equals("15/20"), "10/20 one tube");
        check(new H39AmmoCounter(5).reload(1).magazine().rounds() == 10, "5/20 one tube");
        check(new H39AmmoCounter(0).reload(1).magazine().rounds() == 5, "empty one tube");
        check(new H39AmmoCounter(0).reload(4).magazine().rounds() == 20, "empty four tubes");
        check(new H39AmmoCounter(19).reload(1).tubesConsumed() == 0, "no partial tube overfill");
        check(new H39AmmoCounter(19).visibleTubes() == 4, "partial tube remains visible");
        m = H39AmmoCounter.full();
        int ejects = 0;
        for (int i = 0; i < 20; i++) {
            var shot = m.fire();
            if (shot.tubeEjected()) ejects++;
            m = shot.magazine();
        }
        check(ejects == 4 && m.visibleTubes() == 0, "all four tube ejections");
        check(!m.fire().fired() && m.hud().equals("0/20"), "empty");
        expectInvalid(-1);
        expectInvalid(21);
        System.out.println("H39AmmoCounter: " + checks + " checks passed");
    }
    private static void expectInvalid(int n) {
        try { new H39AmmoCounter(n); throw new AssertionError("accepted invalid count"); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        checks++;
    }
}
