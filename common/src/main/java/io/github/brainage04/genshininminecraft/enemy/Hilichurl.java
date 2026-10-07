package io.github.brainage04.genshininminecraft.enemy;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.util.GeckoLibUtil;
import io.github.brainage04.genshininminecraft.client.enemy.SyncedEnemyController;
import io.github.brainage04.genshininminecraft.rules.EnemyAnimations;
import io.github.brainage04.genshininminecraft.rules.EnemyAnimations.Phase;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Server-owned club Fighter; geometry and synced presentation never drive its combat decisions. */
public final class Hilichurl extends PathfinderMob implements GeoEntity {
    public static final double AGGRO_RADIUS = 12;
    public static final double LEASH_RADIUS = 24;
    public static final double MELEE_RADIUS = 2.2;
    public static final double MELEE_VERTICAL_RANGE = 1.8;
    public static final double MELEE_ARC_COSINE = .5; // 120 degree total arc.
    public static final int WINDUP_TICKS = EnemyAnimations.WINDUP_TICKS;
    public static final int RECOVERY_TICKS = EnemyAnimations.RECOVERY_TICKS;
    public static final int PATH_REFRESH_TICKS = 10;
    public static final double MOVEMENT_SPEED = .23;
    public static final double HOME_TOLERANCE = .75;
    private static final EntityDataAccessor<Integer> GENSHIN_LEVEL = SynchedEntityData.defineId(Hilichurl.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(Hilichurl.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> PHASE_START = SynchedEntityData.defineId(Hilichurl.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> OCCURRENCE = SynchedEntityData.defineId(Hilichurl.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> FROZEN_FRAME = SynchedEntityData.defineId(Hilichurl.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> HURT_START = SynchedEntityData.defineId(Hilichurl.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> HURT_OCCURRENCE = SynchedEntityData.defineId(Hilichurl.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
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
        xpReward = 0;
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED).add(Attributes.FOLLOW_RANGE, AGGRO_RADIUS);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GENSHIN_LEVEL, HilichurlProfile.DEFAULT_CAMP_LEVEL);
        builder.define(PHASE, Phase.IDLE.ordinal());
        builder.define(PHASE_START, 0L);
        builder.define(OCCURRENCE, 0);
        builder.define(FROZEN_FRAME, -1L);
        builder.define(HURT_START, -100L);
        builder.define(HURT_OCCURRENCE, 0);
    }
    public boolean isWindingUp() { return strikeTick >= 0; }
    public boolean isReturningToCamp() { return returning; }
    public Vec3 campAnchor() { return campAnchor; }
    public Vec3 idlePosition() { return idlePosition; }
    public void setCamp(Vec3 anchor, Vec3 home) { campAnchor = anchor; idlePosition = home; }
    public int genshinLevel() { return entityData.get(GENSHIN_LEVEL); }
    public void setGenshinLevel(int level) {
        HilichurlProfile.maxHp(level); // Reject unsourced rows rather than silently interpolate.
        entityData.set(GENSHIN_LEVEL, level);
    }
    public Phase visualPhase() { return Phase.fromId(entityData.get(PHASE)); }
    public long visualStartFrame() { return entityData.get(PHASE_START); }
    public int visualOccurrence() { return entityData.get(OCCURRENCE); }
    public long frozenFrame() { return entityData.get(FROZEN_FRAME); }
    public long hurtStartFrame() { return entityData.get(HURT_START); }
    public int hurtOccurrence() { return entityData.get(HURT_OCCURRENCE); }
    private void phase(Phase phase) {
        if (visualPhase() == phase && phase != Phase.TELEGRAPH) return;
        entityData.set(PHASE, phase.ordinal());
        entityData.set(PHASE_START, level().getGameTime() * 3);
        entityData.set(OCCURRENCE, visualOccurrence() + 1);
    }
    public void visualHurt() {
        entityData.set(HURT_START, level().getGameTime() * 3);
        entityData.set(HURT_OCCURRENCE, hurtOccurrence() + 1);
    }
    public void combatHurtSound() { visualHurt(); playSound(SoundEvents.PIGLIN_HURT, .7F, .8F); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PIGLIN_HURT; }
    @Override protected SoundEvent getDeathSound() { return null; } // die owns the one broadcast for vanilla and kit damage.
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean accepted = super.hurtServer(level, source, amount);
        if (accepted && isAlive()) visualHurt();
        return accepted;
    }
    @Override public void tick() {
        if (level() instanceof ServerLevel) {
            boolean frozen = CombatRuntime.isFrozen(this);
            if (frozen && frozenFrame() < 0) entityData.set(FROZEN_FRAME, level().getGameTime() * 3);
            else if (!frozen && frozenFrame() >= 0) {
                entityData.set(FROZEN_FRAME, -1L);
                if (strikeTick < 0 && visualPhase() == Phase.TELEGRAPH) phase(Phase.RECOVERY);
            }
        }
        super.tick();
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new SyncedEnemyController<Hilichurl>(false), new SyncedEnemyController<Hilichurl>(true));
    }

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
            phase(Phase.IDLE);
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
        if (getTarget() == null && target != null) playSound(SoundEvents.PIGLIN_ANGRY, .45F, .95F);
        setTarget(target);
        if (target == null) {
            returnHome();
            return;
        }
        if (strikeTick >= 0) {
            navigation.stop();
            boolean connected = false;
            if (tickCount < strikeTick) {
                if ((tickCount & 1) == 0) level.sendParticles(TELEGRAPH, getX(), getY() + 1.8, getZ(), 3, .3, .15, .3, 0);
            } else {
                strikeTick = -1; // Consume the gameplay strike without changing the pose before earlier hitmarks drain.
                swing(InteractionHand.MAIN_HAND);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + swingX, getY() + 1, getZ() + swingZ, 1, 0, 0, 0, 0);
                for (ServerPlayer victim : level.players()) {
                    double dx = victim.getX() - getX(), dz = victim.getZ() - getZ();
                    double distance = dx * dx + dz * dz;
                    if (eligible(victim) && distance <= MELEE_RADIUS * MELEE_RADIUS
                            && Math.abs(victim.getY() - getY()) <= MELEE_VERTICAL_RANGE
                            && (distance < .01 || swingX * dx + swingZ * dz >= MELEE_ARC_COSINE * Math.sqrt(distance))
                            && hasLineOfSight(victim)) {
                        connected |= CombatRuntime.get(level.getServer()).enemyHit(victim, this, HilichurlProfile.ADAPTED_ATK,
                                HilichurlProfile.CLUB_MULTIPLIER, genshinLevel());
                    }
                }
                if (!(target instanceof ServerPlayer)) {
                    double dx = target.getX() - getX(), dz = target.getZ() - getZ();
                    double distance = dx * dx + dz * dz;
                    if (distance <= MELEE_RADIUS * MELEE_RADIUS && Math.abs(target.getY() - getY()) <= MELEE_VERTICAL_RANGE
                            && (distance < .01 || swingX * dx + swingZ * dz >= MELEE_ARC_COSINE * Math.sqrt(distance))
                            && hasLineOfSight(target)) connected |= CombatRuntime.get(level.getServer()).puppetHit(target, this);
                }
                readyTick = tickCount + RECOVERY_TICKS;
                if (isAlive() && !CombatRuntime.isFrozen(this)) {
                    phase(Phase.STRIKE);
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, .7F, .7F);
                    if (connected) playSound(SoundEvents.PLAYER_ATTACK_STRONG, .8F, .65F);
                } else if (isAlive() && frozenFrame() < 0) entityData.set(FROZEN_FRAME, level.getGameTime() * 3);
            }
            return;
        }
        if (tickCount < readyTick) {
            if (visualPhase() == Phase.STRIKE
                    && tickCount >= readyTick - RECOVERY_TICKS + EnemyAnimations.STRIKE_PRESENTATION_TICKS)
                phase(Phase.RECOVERY);
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
                phase(Phase.TELEGRAPH);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.PIGLIN_ANGRY, SoundSource.HOSTILE, .65F, .75F);
            }
        } else if (tickCount >= nextPathTick) {
            navigation.moveTo(target, 1);
            nextPathTick = tickCount + PATH_REFRESH_TICKS;
            phase(Phase.RUN);
        }
    }
    private void cancelSwing() {
        strikeTick = -1;
        if (visualPhase() == Phase.TELEGRAPH) phase(Phase.IDLE);
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
            phase(Phase.IDLE);
        } else if (tickCount >= nextPathTick) {
            // The ordinary moveTo accuracy of one block can finish outside the camp's home tolerance.
            navigation.moveTo(idlePosition.x, idlePosition.y, idlePosition.z, 0, 1);
            nextPathTick = tickCount + PATH_REFRESH_TICKS;
            phase(Phase.WALK);
        }
    }
    @Override public void die(DamageSource source) {
        if (level() instanceof ServerLevel) {
            if (!dead) playSound(SoundEvents.PIGLIN_DEATH, .8F, .8F);
            phase(Phase.DEATH);
            entityData.set(FROZEN_FRAME, -1L);
        }
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
        output.putInt("GenshinLevel", genshinLevel());
        if (campAnchor != null) {
            output.store("GenshinCamp", Vec3.CODEC, campAnchor);
            output.store("GenshinIdle", Vec3.CODEC, idlePosition);
        }
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        var savedAnchor = input.read("GenshinCamp", Vec3.CODEC);
        // Old saved camps were Lv20; fresh /summon NBT has neither a level nor a saved camp anchor.
        setGenshinLevel(input.getIntOr("GenshinLevel", savedAnchor.isPresent() ? 20 : HilichurlProfile.DEFAULT_CAMP_LEVEL));
        campAnchor = savedAnchor.orElse(position());
        idlePosition = input.read("GenshinIdle", Vec3.CODEC).orElse(campAnchor);
    }
}
