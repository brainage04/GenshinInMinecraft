package io.github.brainage04.genshininminecraft.enemy;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.util.GeckoLibUtil;
import io.github.brainage04.genshininminecraft.client.enemy.SyncedEnemyController;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.level.Level;

/** No-save session-owned puppet. Rabbit health/attributes remain the gameplay substrate. */
public final class BaronBunny extends Rabbit implements GeoEntity {
    private static final EntityDataAccessor<Long> LANDED = SynchedEntityData.defineId(BaronBunny.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> HURT_START = SynchedEntityData.defineId(BaronBunny.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> HURT_OCCURRENCE = SynchedEntityData.defineId(BaronBunny.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    public BaronBunny(EntityType<? extends BaronBunny> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LANDED, 0L);
        builder.define(HURT_START, -100L);
        builder.define(HURT_OCCURRENCE, 0);
    }
    public void landed() { entityData.set(LANDED, level().getGameTime() * 3); }
    public long landedFrame() { return entityData.get(LANDED); }
    public long hurtStartFrame() { return entityData.get(HURT_START); }
    public int hurtOccurrence() { return entityData.get(HURT_OCCURRENCE); }
    public void visualHurt() {
        entityData.set(HURT_START, level().getGameTime() * 3);
        entityData.set(HURT_OCCURRENCE, hurtOccurrence() + 1);
    }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean accepted = super.hurtServer(level, source, amount);
        if (accepted && isAlive()) visualHurt();
        return accepted;
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new SyncedEnemyController<BaronBunny>(false), new SyncedEnemyController<BaronBunny>(true));
    }
}
