package io.github.brainage04.genshininminecraft.world;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Non-colliding, invulnerable original obelisk. The purchased geometry is never edited. */
public final class OverlayMarker extends Entity implements GeoEntity {
    private static final EntityDataAccessor<String> OVERLAY_ID = SynchedEntityData.defineId(OverlayMarker.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> STATUE = SynchedEntityData.defineId(OverlayMarker.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(OverlayMarker.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("overlay.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public OverlayMarker(EntityType<? extends OverlayMarker> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setInvulnerable(true);
        noPhysics = true;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OVERLAY_ID, "");
        builder.define(STATUE, false);
        builder.define(ACTIVE, false);
    }
    public String overlayId() { return entityData.get(OVERLAY_ID); }
    public boolean statue() { return entityData.get(STATUE); }
    public boolean activated() { return entityData.get(ACTIVE); }
    public void configure(OverlayDefinition.TravelPoint point) {
        entityData.set(OVERLAY_ID, point.id());
        entityData.set(STATUE, point.statue());
        setCustomName(net.minecraft.network.chat.Component.literal(point.name()));
    }
    public void activated(boolean value) { entityData.set(ACTIVE, value); setGlowingTag(value); }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
    @Override public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player instanceof ServerPlayer serverPlayer)
            return OverlayRuntime.interact(serverPlayer, overlayId()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        return InteractionResult.SUCCESS;
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        entityData.set(OVERLAY_ID, input.getStringOr("OverlayId", ""));
        entityData.set(STATUE, input.getBooleanOr("Statue", false));
        activated(input.getBooleanOr("Activated", false));
    }
    @Override protected void addAdditionalSaveData(ValueOutput output) {
        output.putString("OverlayId", overlayId());
        output.putBoolean("Statue", statue());
        output.putBoolean("Activated", activated());
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("idle", 0, state -> state.setAndContinue(IDLE)));
    }
}
