package tw.h39.compat;

import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunReload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative H-39 magazine adapter (one physical tube = five shots).
 *
 * A TaCZ GunReloadEvent is fired BEFORE TaCZ begins its ordinary one-item/
 * one-round reload. We cancel it only for the H-39, schedule completion to
 * match the client animation, and send TaCZ's standard reload packet to
 * observers. Nothing is consumed or added until the animation finishes.
 *
 * If a player drops/switches the weapon, dies or leaves the level, reloading
 * is aborted without consuming any tubes. A custom reload blocks firing
 * on the server so there is no free ammo or early reload completion.
 */
public final class H39TaCZAmmoEvents {
    private static final ResourceLocation H39_GUN =
            ResourceLocation.fromNamespaceAndPath("h39", "h39");
    private static final ResourceLocation MEDICAL_TUBE =
            ResourceLocation.fromNamespaceAndPath("h39", "medical_tube");

    private static final Map<UUID, PendingReload> PENDING = new HashMap<>();

    private record PendingReload(ItemStack weapon, int roundsBefore,
                                 long completeAt, Level origin) {}

    private H39TaCZAmmoEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.register(H39TaCZAmmoEvents.class);
    }

    /** Null means not the H-39 or a corrupt out-of-range round counter. */
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

    private static int inventoryTubes(Player player) {
        int total = 0;
        // Inventory.items excludes the offhand slot. Only count slots that
        // will actually be decremented when the reload finishes.
        for (ItemStack stack : player.getInventory().items) {
            if (isMedicalTube(stack)) total += stack.getCount();
        }
        return total;
    }

    private static H39ReloadPlan.Plan plan(Player player, int before) {
        int available = player.getAbilities().instabuild ? 4 : inventoryTubes(player);
        return H39ReloadPlan.calculate(before, available);
    }

    @SubscribeEvent
    public static void onReload(GunReloadEvent event) {
        ItemStack weapon = event.getGunItemStack();
        H39AmmoCounter current = read(weapon);
        if (current == null || !(event.getEntity() instanceof Player player)) return;

        if (event.getLogicalSide() == LogicalSide.CLIENT) {
            // Only validate the local player's original ItemStack; remote
            // observers receive a COPY inside TaCZ's reload network packet.
            if (player.getMainHandItem() == weapon &&
                    !plan(player, current.rounds()).shouldReload()) {
                event.setCanceled(true);
            }
            return;
        }
        if (event.getLogicalSide() != LogicalSide.SERVER) return;

        // Never allow TaCZ to consume tubes as one-round ammo in its native
        // reload code. Other guns never reach this branch.
        event.setCanceled(true);
        if (!(player instanceof ServerPlayer) || player.getMainHandItem() != weapon) return;
        if (PENDING.containsKey(player.getUUID())) return;

        H39ReloadPlan.Plan pendingPlan = plan(player, current.rounds());
        if (!pendingPlan.shouldReload()) return;

        long finish = H39ReloadTiming.completeAt(
                player.level().getGameTime(), current.rounds());
        PENDING.put(player.getUUID(),
                new PendingReload(weapon, current.rounds(), finish, player.level()));

        // TaCZ normally sends this after its non-cancelled reload event.
        // Since we cancel for five-round tubes, replicate only the notification
        // so remote clients can play the same reload animation.
        NetworkHandler.sendToTrackingEntity(
                new ServerMessageGunReload(player.getId(), weapon.copy()), player);
    }

    @SubscribeEvent
    public static void onFire(GunFireEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) return;
        if (event.getShooter() instanceof Player player &&
                read(event.getGunItemStack()) != null &&
                PENDING.containsKey(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PendingReload reload = PENDING.get(player.getUUID());
        if (reload == null) return;

        // Switching/dropping the weapon or changing worlds cancels reload.
        // Requiring the very same ItemStack avoids applying ammo to another
        // gun with the same TaCZ identifier.
        if (!player.isAlive() || reload.origin() != player.level() ||
                player.getMainHandItem() != reload.weapon()) {
            PENDING.remove(player.getUUID());
            return;
        }

        H39AmmoCounter counter = read(reload.weapon());
        if (counter == null || counter.rounds() != reload.roundsBefore()) {
            PENDING.remove(player.getUUID());
            return;
        }
        if (!H39ReloadTiming.isComplete(player.level().getGameTime(),
                reload.completeAt())) return;

        PENDING.remove(player.getUUID());
        // Recheck carried tubes at completion: inventory may have changed.
        H39ReloadPlan.Plan result = plan(player, counter.rounds());
        if (!result.shouldReload()) return;

        if (!player.getAbilities().instabuild) {
            int remaining = result.tubesToConsume();
            if (inventoryTubes(player) < remaining) return;

            for (ItemStack stack : player.getInventory().items) {
                if (remaining == 0) break;
                if (!isMedicalTube(stack)) continue;
                int count = Math.min(remaining, stack.getCount());
                stack.shrink(count);
                remaining -= count;
            }
            if (remaining != 0) return; // Defensive: never grant free ammo.
        }

        IGun gun = IGun.getIGunOrNull(reload.weapon());
        if (gun != null) {
            gun.setCurrentAmmoCount(reload.weapon(), result.after());
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }
}
