package tw.h39.compat;

import java.util.Arrays;

public final class H39TubeMagazineTest {
    private static int checks;
    public static void main(String[] args) {
        H39TubeMagazine mag = H39TubeMagazine.full();
        check(mag.totalShots() == 20, "capacity");
        for (int i = 0; i < 4; i++) {
            var shot = mag.fire();
            check(shot.fired() && !shot.ejectEmptyTube() && shot.slot() == 0, "first four shots");
            mag = shot.magazine();
        }
        var fifth = mag.fire();
        check(fifth.ejectEmptyTube() && fifth.slot() == 0, "eject on fifth shot");
        mag = fifth.magazine();
        check(mag.totalShots() == 15 && mag.emptySlots() == 1, "one tube spent");
        var reloaded = mag.reload(1);
        check(reloaded.tubesConsumed() == 1 && reloaded.magazine().totalShots() == 20, "replace one tube");
        check(reloaded.magazine().reload(5).tubesConsumed() == 0, "no waste on full magazine");
        var partial = new H39TubeMagazine(3, 0, 0, 5);
        var partialReload = partial.reload(2);
        check(Arrays.equals(partialReload.magazine().tubes(), new int[]{3, 5, 5, 5}), "preserve partial tube");
        check(partialReload.tubesConsumed() == 2, "consume two whole tubes");
        check(partialReload.magazine().totalShots() == 18, "partial reload total");
        mag = H39TubeMagazine.full();
        int ejects = 0;
        for (int i = 0; i < 20; i++) {
            var shot = mag.fire();
            if (shot.ejectEmptyTube()) ejects++;
            mag = shot.magazine();
        }
        check(ejects == 4 && mag.totalShots() == 0, "four eject events");
        check(!mag.fire().fired(), "cannot fire empty");
        check(mag.reload(3).magazine().totalShots() == 15, "load three tubes");
        check(H39TubeMagazine.empty().reload(1).magazine().totalShots() == 5, "load one tube");
        try { new H39TubeMagazine(6, 0, 0, 0); throw new AssertionError("accepted invalid capacity"); }
        catch (IllegalArgumentException expected) { checks++; }
        try { mag.reload(-1); throw new AssertionError("accepted negative inventory"); }
        catch (IllegalArgumentException expected) { checks++; }
        System.out.println("H39TubeMagazine: " + checks + " checks passed");
    }
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
}
