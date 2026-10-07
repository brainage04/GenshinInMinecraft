package io.github.brainage04.genshininminecraft.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import io.github.brainage04.genshininminecraft.client.enemy.EnemyGeoModel;
import io.github.brainage04.genshininminecraft.client.enemy.EnemyGeoRenderState;
import io.github.brainage04.genshininminecraft.client.enemy.EnemyRenderInput;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Original hunched masked Fighter and jointed club; no vanilla humanoid/item layers. */
public final class HilichurlRenderer extends GeoEntityRenderer<Hilichurl, EnemyGeoRenderState> {
    public HilichurlRenderer(EntityRendererProvider.Context context) {
        super(context, new EnemyGeoModel<>("hilichurl"));
        shadowRadius = .45F;
    }
    @Override public EnemyGeoRenderState createRenderState(Hilichurl entity, Void unused) { return new EnemyGeoRenderState(); }
    @Override protected float getDeathMaxRotation(GeoRenderState state) { return 0; }
    @Override public void addRenderData(Hilichurl entity, Void unused, EnemyGeoRenderState state, float partialTick) {
        double frame = entity.frozenFrame() >= 0 ? entity.frozenFrame()
                : (entity.level().getGameTime() + (entity.level().tickRateManager().isFrozen() ? 0 : partialTick)) * 3;
        state.addGeckolibData(EnemyRenderInput.TICKET, new EnemyRenderInput(entity.visualPhase(), entity.visualOccurrence(),
                Math.max(0, frame - entity.visualStartFrame()) / 60, entity.hurtOccurrence(),
                Math.max(0, frame - entity.hurtStartFrame()) / 60));
    }
}
