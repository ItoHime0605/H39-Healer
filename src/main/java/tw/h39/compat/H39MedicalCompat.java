package tw.h39.compat;

import net.neoforged.fml.common.Mod;

@Mod(H39MedicalCompat.MOD_ID)
public final class H39MedicalCompat {
    public static final String MOD_ID = "h39medical";

    public H39MedicalCompat() {
        H39MedicalHitHandler.register();
    }
}
