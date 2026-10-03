package io.github.brainage04.genshininminecraft.enemy;

import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.rules.HilichurlProfile;
import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Server-owned ordinary club Fighter placeholder, with no vanilla attack/target goals. */
public final class Hilichurl extends PathfinderMob {
    public static final double AGGRO_RADIUS = 12;
    public static final double LEASH_RADIUS = 24;
    public static final double MELEE_RADIUS = 2.2;
    public static final double MELEE_VERTICAL_RANGE = 1.8;
    public static final double MELEE_ARC_COSINE = .5; // 120 degree total arc.
    public static final int WINDUP_TICKS = 10; // 30 reference frames, exactly 0.5 seconds at 20 TPS.
    public static final int RECOVERY_TICKS = 30;
    public static final int PATH_REFRESH_TICKS = 10;
    public static final double MOVEMENT_SPEED = .23;
    public static final double HOME_TOLERANCE = .75;
    private static final EntityDataAccessor<Boolean> WINDING_UP = SynchedEntityData.defineId(Hilichurl.class, EntityDataSerializers.BOOLEAN);
    private static final DustParticleOptions TELEGRAPH = new DustParticleOptions(0xffca65, 1.2F);
    private Vec3 campAnchor;
    private Vec3 idlePosition;
    private boolean returning;
    private int strikeTick = -1;
    private int readyTick;
    private int nextPathTick;
    private double swingX;
    private double swingZ;

    public Hilichurl(EntityType<? extends Hilichurl> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SHOVEL));
        setDropChance(EquipmentSlot.MAINHAND, 0);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED).add(Attributes.FOLLOW_RANGE, AGGRO_RADIUS);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WINDING_UP, false);
    }
    public boolean isWindingUp() { return entityData.get(WINDING_UP); }
    public boolean isReturningToCamp() { return returning; }
    public Vec3 campAnchor() { return campAnchor; }
    public Vec3 idlePosition() { return idlePosition; }
    public void setCamp(Vec3 anchor, Vec3 home) { campAnchor = anchor; idlePosition = home; }

    private boolean eligible(ServerPlayer player) {
        return player.level() == level() && player.isAlive() && !player.isRemoved()
                && !player.isCreative() && !player.isSpectator();
    }
    @Override protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (campAnchor == null) setCamp(position(), position()); // /summon also establishes an anchor.
        if (!ManagedWorld.isManaged(level)) {
            cancelSwing();
            setTarget(null);
            navigation.stop();
            return;
        }
        if (returning) {
            returnHome();
            return;
        }
        LivingEntity target = getTarget();
        if (target instanceof ServerPlayer player && (!eligible(player) || player.position().distanceToSqr(campAnchor) > LEASH_RADIUS * LEASH_RADIUS)
                || target != null && distanceToSqr(campAnchor) > LEASH_RADIUS * LEASH_RADIUS) {
            resetEncounter(level);
            returnHome();
            return;
        }
        LivingEntity puppet = CombatRuntime.get(level.getServer()).tauntTarget(this);
        if (puppet != null && puppet != target) { cancelSwing(); target = puppet; }
        if (target != null && (!target.isAlive() || target.isRemoved()
                || !(target instanceof ServerPlayer) && target != puppet)) { cancelSwing(); target = null; }
        if (target == null) {
            double closest = AGGRO_RADIUS * AGGRO_RADIUS;
            for (ServerPlayer candidate : level.players()) {
                double distance = distanceToSqr(candidate);
                if (eligible(candidate) && candidate.position().distanceToSqr(campAnchor) <= LEASH_RADIUS * LEASH_RADIUS
                        && distance < closest && hasLineOfSight(candidate)) {
                    target = candidate;
                    closest = distance;
                }
            }
        }
        setTarget(target);
        if (target == null) {
            returnHome();
            return;
        }
        if (strikeTick >= 0) {
            navigation.stop();
            if (tickCount < strikeTick) {
                if ((tickCount & 1) == 0) level.sendParticles(TELEGRAPH, getX(), getY() + 1.8, getZ(), 3, .3, .15, .3, 0);
            } else {
                cancelSwing();
                swing(InteractionHand.MAIN_HAND);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + swingX, getY() + 1, getZ() + swingZ, 1, 0, 0, 0, 0);
                for (ServerPlayer victim : level.players()) {
                    double dx = victim.getX() - getX(), dz = victim.getZ() - getZ();
                    double distance = dx * dx + dz * dz;
                    if (eligible(victim) && distance <= MELEE_RADIUS * MELEE_RADIUS
                            && Math.abs(victim.getY() - getY()) <= MELEE_VERTICAL_RANGE
                            && (distance < .01 || swingX * dx + swingZ * dz >= MELEE_ARC_COSINE * Math.sqrt(distance))
                            && hasLineOfSight(victim)) {
                        CombatRuntime.get(level.getServer()).enemyHit(victim, this, HilichurlProfile.ADAPTED_ATK,
                                HilichurlProfile.CLUB_MULTIPLIER, HilichurlProfile.LEVEL);
                    }
                }
                if (!(target instanceof ServerPlayer)) {
                    double dx = target.getX() - getX(), dz = target.getZ() - getZ();
                    double distance = dx * dx + dz * dz;
                    if (distance <= MELEE_RADIUS * MELEE_RADIUS && Math.abs(target.getY() - getY()) <= MELEE_VERTICAL_RANGE
                            && (distance < .01 || swingX * dx + swingZ * dz >= MELEE_ARC_COSINE * Math.sqrt(distance))
                            && hasLineOfSight(target)) CombatRuntime.get(level.getServer()).puppetHit(target, this);
                }
                readyTick = tickCount + RECOVERY_TICKS;
            }
            return;
        }
        lookControl.setLookAt(target, 30, 30);
        if (distanceToSqr(target) <= MELEE_RADIUS * MELEE_RADIUS && Math.abs(target.getY() - getY()) <= MELEE_VERTICAL_RANGE
                && hasLineOfSight(target)) {
            navigation.stop();
            if (tickCount >= readyTick) {
                double dx = target.getX() - getX(), dz = target.getZ() - getZ();
                double length = Math.sqrt(dx * dx + dz * dz);
                swingX = length > .001 ? dx / length : 0;
                swingZ = length > .001 ? dz / length : 1;
                float yaw = (float) (Mth.atan2(-swingX, swingZ) * 180 / Math.PI);
                setYRot(yaw);
                setYBodyRot(yaw);
                setYHeadRot(yaw);
                strikeTick = tickCount + WINDUP_TICKS;
                entityData.set(WINDING_UP, true);
            }
        } else if (tickCount >= nextPathTick) {
            navigation.moveTo(target, 1);
            nextPathTick = tickCount + PATH_REFRESH_TICKS;
        }
    }
    private void cancelSwing() {
        strikeTick = -1;
        entityData.set(WINDING_UP, false);
    }
    private void resetEncounter(ServerLevel level) {
        cancelSwing();
        setTarget(null);
        readyTick = 0;
        returning = true;
        navigation.stop();
        nextPathTick = 0;
        CombatRuntime.get(level.getServer()).resetTarget(this);
    }
    private void returnHome() {
        if (distanceToSqr(idlePosition) <= HOME_TOLERANCE * HOME_TOLERANCE) {
            navigation.stop();
            returning = false;
        } else if (tickCount >= nextPathTick) {
            // The ordinary moveTo accuracy of one block can finish outside the camp's home tolerance.
            navigation.moveTo(idlePosition.x, idlePosition.y, idlePosition.z, 0, 1);
            nextPathTick = tickCount + PATH_REFRESH_TICKS;
        }
    }
    @Override public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) CombatRuntime.get(level.getServer()).forget(getUUID());
    }
    @Override public void onRemoval(RemovalReason reason) {
        super.onRemoval(reason);
        if (level() instanceof ServerLevel level) CombatRuntime.get(level.getServer()).forget(getUUID());
    }
    @Override protected boolean shouldDropLoot(ServerLevel level) { return false; }
    @Override protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (campAnchor != null) {
            output.store("GenshinCamp", Vec3.CODEC, campAnchor);
            output.store("GenshinIdle", Vec3.CODEC, idlePosition);
        }
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        campAnchor = input.read("GenshinCamp", Vec3.CODEC).orElse(position());
        idlePosition = input.read("GenshinIdle", Vec3.CODEC).orElse(campAnchor);
    }
}
