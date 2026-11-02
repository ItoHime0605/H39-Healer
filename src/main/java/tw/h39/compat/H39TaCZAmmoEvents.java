package tw.h39.compat;

import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Server-authoritative H-39 reload adapter.
 *
 * TaCZ stores the shared current round count on the gun ItemStack.
 * For H-39 only, intercept the normal reload request, consume physical
 * medical tube ammo items (one item = five rounds), and update that count.
 * All other guns retain normal TaCZ behavior.
 *
 * The animation hook and model visibility adapter are separate TODOs.
 */
public final class H39TaCZAmmoEvents {
    private static final ResourceLocation H39_GUN =
            ResourceLocation.fromNamespaceAndPath("h39", "h39");
    private static final ResourceLocation MEDICAL_TUBE =
            ResourceLocation.fromNamespaceAndPath("h39", "medical_tube");

    private H39TaCZAmmoEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.register(H39TaCZAmmoEvents.class);
    }

    /** Return null when this is not the H-39 or when ammo state is invalid. */
    public static H39AmmoCounter read(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null || !H39_GUN.equals(gun.getGunId(stack))) return null;
        int current = gun.getCurrentAmmoCount(stack);
        if (current < 0 || current > H39AmmoCounter.CAPACITY) return null;
        return new H39AmmoCounter(current);
    }

    public static int visibleTubes(ItemStack stack) {
        H39AmmoCounter counter = read(stack);
        return counter == null ? -1 : counter.visibleTubes();
    }

    public static String hud(ItemStack stack) {
        H39AmmoCounter counter = read(stack);
        return counter == null ? null : counter.hud();
    }

    private static boolean isMedicalTube(ItemStack stack) {
        IAmmo ammo = IAmmo.getIAmmoOrNull(stack);
        return ammo != null && MEDICAL_TUBE.equals(ammo.getAmmoId(stack));
    }

    @SubscribeEvent
    public static void onReload(GunReloadEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) return;
        ItemStack weapon = event.getGunItemStack();
        H39AmmoCounter counter = read(weapon);
        if (counter == null) return; // Normal TaCZ reload for all other guns.

        // Prevent the default one-item/one-round TaCZ reload for this weapon.
        event.setCanceled(true);
        if (!(event.getEntity() instanceof Player player)) return;

        int available = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (isMedicalTube(stack)) available += stack.getCount();
        }

        H39ReloadPlan.Plan plan = H39ReloadPlan.calculate(counter.rounds(), available);
        if (!plan.shouldReload()) return;

        int remaining = plan.tubesToConsume();
        if (!player.getAbilities().instabuild) {
            for (ItemStack stack : player.getInventory().items) {
                if (remaining == 0) break;
                if (!isMedicalTube(stack)) continue;
                int consumed = Math.min(remaining, stack.getCount());
                stack.shrink(consumed);
                remaining -= consumed;
            }
            if (remaining != 0) {
                // Unexpected inventory change: do not grant free rounds.
                return;
            }
        }

        IGun gun = IGun.getIGunOrNull(weapon);
        if (gun != null) gun.setCurrentAmmoCount(weapon, plan.after());
        player.getInventory().setChanged();
    }
}
