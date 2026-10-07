package io.github.brainage04.genshininminecraft.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import io.github.brainage04.genshininminecraft.client.enemy.EnemyGeoModel;
import io.github.brainage04.genshininminecraft.client.enemy.EnemyGeoRenderState;
import io.github.brainage04.genshininminecraft.client.enemy.EnemyRenderInput;
import io.github.brainage04.genshininminecraft.enemy.BaronBunny;
import io.github.brainage04.genshininminecraft.rules.EnemyAnimations.Phase;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class BaronBunnyRenderer extends GeoEntityRenderer<BaronBunny, EnemyGeoRenderState> {
    public BaronBunnyRenderer(EntityRendererProvider.Context context) {
        super(context, new EnemyGeoModel<>("baron_bunny")); shadowRadius = .3F;
    }
    @Override public EnemyGeoRenderState createRenderState(BaronBunny entity, Void unused) { return new EnemyGeoRenderState(); }
    @Override protected float getDeathMaxRotation(GeoRenderState state) { return 0; }
    @Override public void addRenderData(BaronBunny entity, Void unused, EnemyGeoRenderState state, float partialTick) {
        double frame = (entity.level().getGameTime() + (entity.level().tickRateManager().isFrozen() ? 0 : partialTick)) * 3;
        double elapsed = Math.max(0, frame - entity.landedFrame());
        var phase = elapsed < Phase.LAND.frames() ? Phase.LAND : Phase.IDLE;
        state.addGeckolibData(EnemyRenderInput.TICKET, new EnemyRenderInput(phase, 0,
                (phase == Phase.LAND ? elapsed : elapsed - Phase.LAND.frames()) / 60,
                entity.hurtOccurrence(), Math.max(0, frame - entity.hurtStartFrame()) / 60));
    }
}
