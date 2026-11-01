package tw.h39.compat;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Candidate integration for MC 1.21.1 / NeoForge / TaCZ 1.1.8.
 * NOT YET COMPILED AGAINST THE FULL MOD DEPENDENCIES.
 * TaCZ gun damage is configured as 0.5 HP; do not deal it again here.
 */
public final class H39MedicalHitHandler {
    private static final ResourceLocation GUN = ResourceLocation.fromNamespaceAndPath("h39", "h39");
    private static final String ROOT = "H39Medical";
    private static final String[] TRAUMA = {"bleeding", "internal_bleeding", "blood_loss", "bone_fracture", "laceration", "burns"};

    public static void register() {
        NeoForge.EVENT_BUS.register(new H39MedicalHitHandler());
    }

    @SubscribeEvent
    public void onHit(EntityHurtByGunEvent.Post event) {
        if (event.getLogicalSide().isClient() || !GUN.equals(event.getGunId())) return;
        if (!(event.getHurtEntity() instanceof LivingEntity target) || !target.isAlive()) return;
        boolean undead = target.isInvertedHealAndHarm();
        if (undead) {
            target.hurt(target.damageSources().magic(), 8.0F);
        } else {
            target.heal(8.0F);
            for (String id : TRAUMA) {
                ResourceLocation key = ResourceLocation.fromNamespaceAndPath("meds_and_herbs", id);
                BuiltInRegistries.MOB_EFFECT.getOptional(key).ifPresent(effect ->
                    target.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect)));
            }
        }
        CompoundTag state = target.getPersistentData().getCompound(ROOT);
        long now = target.level().getGameTime();
        state.putLong("next", H39PulseSchedule.nextOnHit(now));
        state.putLong("end", H39PulseSchedule.endOnHit(now));
        state.putBoolean("undead", undead);
        target.getPersistentData().put(ROOT, state);
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof LivingEntity target)) return;
        CompoundTag root = target.getPersistentData();
        if (!root.contains(ROOT)) return;
        CompoundTag state = root.getCompound(ROOT);
        long now = target.level().getGameTime();
        long next = state.getLong("next"), end = state.getLong("end");
        if (!target.isAlive() || H39PulseSchedule.isExpired(now, next, end)) {
            root.remove(ROOT);
            return;
        }
        if (!H39PulseSchedule.shouldPulse(now, next, end)) return;
        if (state.getBoolean("undead")) target.hurt(target.damageSources().magic(), 2.0F);
        else target.heal(2.0F);
        long following = H39PulseSchedule.nextAfterPulse(now);
        state.putLong("next", following);
        if (following > end) root.remove(ROOT);
        else root.put(ROOT, state);
    }
}
