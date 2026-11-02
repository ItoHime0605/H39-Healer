package tw.h39.compat;

public final class H39ReloadPlanTest {
    private static int checks;
    public static void main(String[] args) {
        for (int rounds = 0; rounds <= 20; rounds++) {
            for (int inventory = 0; inventory <= 6; inventory++) {
                H39ReloadPlan.Plan plan = H39ReloadPlan.calculate(rounds, inventory);
                int expectedTubes = Math.min(inventory, (20 - rounds) / 5);
                check(plan.before() == rounds, "before");
                check(plan.after() == rounds + 5 * expectedTubes, "after");
                check(plan.tubesToConsume() == expectedTubes, "inventory");
                check(plan.shouldReload() == (expectedTubes > 0), "eligibility");
            }
        }
        check(H39ReloadPlan.calculate(10, 2).after() == 20, "10/20 two tubes");
        check(H39ReloadPlan.calculate(5, 1).after() == 10, "one tube only");
        check(H39ReloadPlan.calculate(19, 4).tubesToConsume() == 0, "no partial tube consumption");
        System.out.println("H39ReloadPlan: " + checks + " checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
