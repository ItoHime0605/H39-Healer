package tw.h39.compat;

import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * H-39's self-contained pneumatic "pop-whoosh" gunshot.
 *
 * Do not depend on TaCZ gunpack sound extraction: the Ogg and sounds.json
 * live at the root of this NeoForge mod JAR. One server-accepted shot
 * broadcasts one vanilla SoundEvent to nearby clients (including shooter).
 * The TaCZ gunpack intentionally does not register another shoot sound:
 * never play duplicate shots when its sound loader does start working.
 */
public final class H39PneumaticAudio {
    private static final ResourceLocation GUN =
            ResourceLocation.fromNamespaceAndPath("h39", "h39");
    private static final String MOD_ID = H39MedicalCompat.MOD_ID;

    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> PNEUMATIC_SHOT =
            SOUNDS.register("pneumatic_shot",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    MOD_ID, "pneumatic_shot")));

    private H39PneumaticAudio() {}

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
        NeoForge.EVENT_BUS.register(H39PneumaticAudio.class);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerGunShot(GunShootEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER || event.isCanceled()) return;

        ItemStack stack = event.getGunItemStack();
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null || !GUN.equals(gun.getGunId(stack))) return;

        LivingEntity shooter = event.getShooter();
        if (!(shooter.level() instanceof ServerLevel level)) return;

        // Null receiver sends to every nearby player; using shooter here would
        // exclude the local player and make the rifle soundless in first person.
        level.playSound(null, shooter.getX(), shooter.getEyeY(), shooter.getZ(),
                PNEUMATIC_SHOT.get(), SoundSource.PLAYERS, 1.15F, 1.0F);
    }
}
