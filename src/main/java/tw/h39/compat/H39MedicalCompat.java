package tw.h39.compat;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import com.tacz.guns.api.resource.ResourceManager;

@Mod(H39MedicalCompat.MOD_ID)
public final class H39MedicalCompat {
    public static final String MOD_ID = "h39medical";

    public H39MedicalCompat(IEventBus modBus) {
        H39PneumaticAudio.register(modBus);
        ResourceManager.registerExportResource(H39MedicalCompat.class, "/assets/tacz/custom/h39_healer");
        H39MedicalHitHandler.register();
        H39RuntimeDiagnostics.register();
        H39TaCZAmmoEvents.register();
    }
}
