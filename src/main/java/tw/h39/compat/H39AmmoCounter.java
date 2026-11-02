package tw.h39.compat;

/**
 * H-39 simplified magazine: a single shared 0..20 ammo count.
 * Each medical tube contributes five rounds; shots always consume
 * the frontmost available tube. HUD uses rounds + "/20".
 */
public final class H39AmmoCounter {
    public static final int CAPACITY = 20;
    public static final int TUBE_ROUNDS = 5;
    private final int rounds;

    public H39AmmoCounter(int rounds) {
        if (rounds < 0 || rounds > CAPACITY) {
            throw new IllegalArgumentException("Rounds out of range: " + rounds);
        }
        this.rounds = rounds;
    }

    public static H39AmmoCounter empty() { return new H39AmmoCounter(0); }
    public static H39AmmoCounter full() { return new H39AmmoCounter(CAPACITY); }
    public int rounds() { return rounds; }
    public String hud() { return rounds + "/" + CAPACITY; }

    /** Number of non-empty tubes still visible on the weapon. */
    public int visibleTubes() {
        return (rounds + TUBE_ROUNDS - 1) / TUBE_ROUNDS;
    }

    /** Which front-to-back tube is being consumed, or -1 when empty. */
    public int activeTube() {
        return rounds == 0 ? -1 : 4 - visibleTubes();
    }

    public ShotResult fire() {
        if (rounds == 0) return new ShotResult(this, false, false);
        H39AmmoCounter next = new H39AmmoCounter(rounds - 1);
        return new ShotResult(next, true, next.visibleTubes() < visibleTubes());
    }

    /**
     * Each tube adds five rounds, up to 20. A partially consumed tube is
     * preserved; capacity is checked in whole-tube increments.
     */
    public ReloadResult reload(int carriedTubes) {
        if (carriedTubes < 0) throw new IllegalArgumentException("Negative tube count");
        int usable = Math.min(carriedTubes, (CAPACITY - rounds) / TUBE_ROUNDS);
        return new ReloadResult(new H39AmmoCounter(rounds + usable * TUBE_ROUNDS), usable);
    }

    public record ShotResult(H39AmmoCounter magazine, boolean fired, boolean tubeEjected) {}
    public record ReloadResult(H39AmmoCounter magazine, int tubesConsumed) {}
}
