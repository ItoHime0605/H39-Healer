package tw.h39.compat;

import java.util.Arrays;

public final class H39AmmoStateTest {
    private static int checks;
    public static void main(String[] args) {
        for (int rounds = 0; rounds <= 20; rounds++) {
            var counter = new H39AmmoCounter(rounds);
            check(H39AmmoState.read(H39AmmoState.write(counter)).rounds() == rounds, "round trip " + rounds);
            int visible = (rounds + 4) / 5;
            boolean[] expected = new boolean[4];
            for (int i = 4 - visible; i < 4; i++) expected[i] = true;
            check(Arrays.equals(H39AmmoState.visibleFromFront(counter), expected), "visible tubes " + rounds);
        }
        check(H39AmmoState.newGun().rounds() == 0, "new gun starts empty");
        check(H39AmmoState.read(null).rounds() == 0, "missing data is empty");
        check(H39AmmoState.read(-1).rounds() == 0, "negative data is empty");
        check(H39AmmoState.read(21).rounds() == 0, "overflow data is empty");
        System.out.println("H39AmmoState: " + checks + " checks passed");
    }
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
}
