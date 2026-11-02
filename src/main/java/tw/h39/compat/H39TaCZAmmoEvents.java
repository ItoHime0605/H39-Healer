package tw.h39.compat;

import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunFinishReloadEvent;
import com.tacz.guns.api.item.IGun;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Bridge to TaCZ's existing gun magazine, which is the single source of truth.
 *
 * IMPORTANT: TaCZ itself performs the ammo decrement and reload. Do not
 * decrement it again here or duplicate ammo. The shared H39AmmoCounter is
 * for validation/visibility, not a second independent ammunition store.
 */
public final class H39TaCZAmmoEvents {
    private static final ResourceLocation H39_GUN =
            ResourceLocation.fromNamespaceAndPath("h39", "h39");

    private H39TaCZAmmoEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.register(H39TaCZAmmoEvents.class);
    }

    /** Return null when this stack is not the H-39 or has invalid ammo data. */
    public static H39AmmoCounter read(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null || !H39_GUN.equals(gun.getGunId(stack))) return null;
        int current = gun.getCurrentAmmoCount(stack);
        if (current < 0 || current > H39AmmoCounter.CAPACITY) return null;
        return new H39AmmoCounter(current);
    }

    /** Front-to-back tube count for a client-side model adapter; -1 means not H-39. */
    public static int visibleTubes(ItemStack stack) {
        H39AmmoCounter counter = read(stack);
        return counter == null ? -1 : counter.visibleTubes();
    }

    /** Returns HUD text only for the H-39; TaCZ remains the ammo authority. */
    public static String hud(ItemStack stack) {
        H39AmmoCounter counter = read(stack);
        return counter == null ? null : counter.hud();
    }

    @SubscribeEvent
    public static void onFire(GunFireEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) return;
        // TaCZ owns the actual shot and ammo decrement. No second decrement.
        read(event.getGunItemStack());
    }

    @SubscribeEvent
    public static void onFinishReload(GunFinishReloadEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) return;
        // TaCZ owns actual reload consumption. No duplicated tube grant.
        read(event.getGunItemStack());
    }
}
