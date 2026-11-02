package tw.h39.compat;

/**
 * Pure planning layer for converting physical five-shot tubes into TaCZ rounds.
 * The inventory adapter must atomically consume tubes and commit the new count.
 * No items are consumed by this class.
 */
public final class H39ReloadPlan {
    private H39ReloadPlan() {}

    public static Plan calculate(int currentRounds, int carriedTubes) {
        H39AmmoCounter magazine = new H39AmmoCounter(currentRounds);
        H39AmmoCounter.ReloadResult result = magazine.reload(carriedTubes);
        return new Plan(currentRounds, result.magazine().rounds(), result.tubesConsumed());
    }

    public record Plan(int before, int after, int tubesToConsume) {
        public Plan {
            if (before < 0 || before > 20 || after < before || after > 20
                    || tubesToConsume < 0 || after - before != tubesToConsume * 5) {
                throw new IllegalArgumentException("Invalid reload plan");
            }
        }

        public boolean shouldReload() { return tubesToConsume > 0; }
    }
}
