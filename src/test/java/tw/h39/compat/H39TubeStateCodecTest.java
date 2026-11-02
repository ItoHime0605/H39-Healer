package tw.h39.compat;

import java.util.Arrays;

public final class H39TubeStateCodecTest {
    private static int checks;
    public static void main(String[] args) {
        var original = new H39TubeMagazine(3, 0, 5, 2);
        int[] encoded = H39TubeStateCodec.encode(original);
        check(Arrays.equals(encoded, new int[]{3, 0, 5, 2}), "round trip encoding");
        check(Arrays.equals(H39TubeStateCodec.decode(encoded).tubes(), original.tubes()), "round trip decoding");
        encoded[0] = 0;
        check(original.tubes()[0] == 3, "defensive copy");
        check(H39TubeStateCodec.newWeapon().totalShots() == 0, "new weapon starts empty");
        expectInvalid(null);
        expectInvalid(new int[]{5, 5, 5});
        expectInvalid(new int[]{5, 5, 5, 6});
        expectInvalid(new int[]{5, -1, 5, 5});
        System.out.println("H39TubeStateCodec: " + checks + " checks passed");
    }
    private static void expectInvalid(int[] values) {
        try {
            H39TubeStateCodec.decode(values);
            throw new AssertionError("accepted invalid tube state");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
