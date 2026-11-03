package tw.h39.compat;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * TaCZ post-impact medical effect for Minecraft 1.21.1 / NeoForge.
 *
 * The 0.5 HP dart impact is dealt by TaCZ first. If the target died from
 * that impact, leave it dead. Only survivors receive the healing pulses.
 *
 * Most waste-land entities are NOT H-39 patients. A weak registry prevents
 * allocating persistent NBT for every zombie on every server tick. The
 * actual pulse state remains in persistent NBT, surviving chunk unloads.
 */
public final class H39MedicalHitHandler {
    private static final ResourceLocation GUN =
            ResourceLocation.fromNamespaceAndPath("h39", "h39");
    private static final String ROOT = "H39Medical";
    private static final String[] TRAUMA = {
            "bleeding", "internal_bleeding", "blood_loss",
            "bone_fracture", "laceration", "burns"
    };

    // NeoForge entity events run on their owning level thread; values are weak
    // so a forgotten unloaded target cannot remain strongly referenced.
    private static final Set<LivingEntity> ACTIVE =
            Collections.newSetFromMap(new WeakHashMap<>());

    public static void register() {
        NeoForge.EVENT_BUS.register(new H39MedicalHitHandler());
    }

    @SubscribeEvent
    public void onHit(EntityHurtByGunEvent.Post event) {
        if (event.getLogicalSide().isClient() || !GUN.equals(event.getGunId())) return;
        if (!(event.getHurtEntity() instanceof LivingEntity target) || !target.isAlive()) return;

        boolean undead = target.isInvertedHealAndHarm();
        if (undead) {
            hurtUndead(target, 8.0F);
        } else {
            target.heal(8.0F);
            for (String id : TRAUMA) {
                ResourceLocation key = ResourceLocation.fromNamespaceAndPath("meds_and_herbs", id);
                BuiltInRegistries.MOB_EFFECT.getOptional(key).ifPresent(effect ->
                        target.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect)));
            }
        }

        // The instant 8-HP undead hit may have killed its target. Do not
        // create persistent 10-second state for a dead entity.
        if (!target.isAlive()) return;

        // Re-hitting refreshes one effect window, never stacks duplicates.
        CompoundTag root = target.getPersistentData();
        CompoundTag state = root.getCompound(ROOT);
        long now = target.level().getGameTime();
        state.putLong("next", H39PulseSchedule.nextOnHit(now));
        state.putLong("end", H39PulseSchedule.endOnHit(now));
        state.putBoolean("undead", undead);
        root.put(ROOT, state);
        ACTIVE.add(target);
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)
                || target.level().isClientSide) return;
        // Once per entity load (not once every tick), restore pending pulses
        // saved in world NBT after a server restart or chunk unload.
        if (target.getPersistentData().contains(ROOT)) ACTIVE.add(target);
    }

    @SubscribeEvent
    public void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity target) {
            ACTIVE.remove(target);
        }
    }

    /**
     * The 0.5-HP TaCZ needle hit normally starts damage invulnerability
     * frames. Applying another 8 HP magic damage on the same tick would
     * otherwise do ONLY the difference (usually 7.5), not the requested
     * additional 8 HP. Clear that brief cooldown before the therapeutic
     * anti-undead hit; Minecraft then establishes its normal new cooldown.
     *
     * The original cooldown is retained if a protection/event cancels this
     * damage; no immunity should disappear after an unsuccessful hit.
     */
    private static void hurtUndead(LivingEntity target, float hp) {
        int previous = target.invulnerableTime;
        target.invulnerableTime = 0;
        if (!target.hurt(target.damageSources().magic(), hp)) {
            target.invulnerableTime = previous;
        }
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof LivingEntity target)
                || !ACTIVE.contains(target)) return;

        CompoundTag root = target.getPersistentData();
        if (!root.contains(ROOT)) {
            ACTIVE.remove(target);
            return;
        }
        CompoundTag state = root.getCompound(ROOT);
        long now = target.level().getGameTime();
        long next = state.getLong("next");
        long end = state.getLong("end");
        if (!target.isAlive() || H39PulseSchedule.isExpired(now, next, end)) {
            root.remove(ROOT);
            ACTIVE.remove(target);
            return;
        }
        if (!H39PulseSchedule.shouldPulse(now, next, end)) return;

        if (state.getBoolean("undead")) {
            hurtUndead(target, 2.0F);
        } else {
            target.heal(2.0F);
        }

        if (!target.isAlive()) {
            root.remove(ROOT);
            ACTIVE.remove(target);
            return;
        }

        long following = H39PulseSchedule.nextAfterPulse(now);
        state.putLong("next", following);
        if (following > end) {
            root.remove(ROOT);
            ACTIVE.remove(target);
        } else {
            root.put(ROOT, state);
        }
    }
}
