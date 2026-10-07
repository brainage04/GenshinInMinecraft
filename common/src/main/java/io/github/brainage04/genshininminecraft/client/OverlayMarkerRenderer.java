package io.github.brainage04.genshininminecraft.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import io.github.brainage04.genshininminecraft.world.OverlayMarker;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Original static stone/teal obelisks; activated markers are full-bright, without placing a light block. */
public final class OverlayMarkerRenderer extends GeoEntityRenderer<OverlayMarker, OverlayMarkerRenderer.State> {
    private static final DataTicket<Boolean> STATUE = DataTicket.create("overlay_statue", Boolean.class);
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("genshininminecraft", path); }
    private static final Identifier STATUE_MODEL = id("world/statue"), WAYPOINT_MODEL = id("world/waypoint");
    private static final Identifier STATUE_TEXTURE = id("textures/entity/world/statue.png"), WAYPOINT_TEXTURE = id("textures/entity/world/waypoint.png");
    private static final Identifier ANIMATION = id("world/obelisk");
    public OverlayMarkerRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<>() {
            @Override public Identifier getModelResource(GeoRenderState state) { return state.getGeckolibData(STATUE) ? STATUE_MODEL : WAYPOINT_MODEL; }
            @Override public Identifier getTextureResource(GeoRenderState state) { return state.getGeckolibData(STATUE) ? STATUE_TEXTURE : WAYPOINT_TEXTURE; }
            @Override public Identifier getAnimationResource(OverlayMarker entity) { return ANIMATION; }
        });
        shadowRadius = .35F;
    }
    public static final class State extends EntityRenderState implements GeoRenderState {
        private final Map<DataTicket<?>, Object> data = new Reference2ObjectOpenHashMap<>();
        @Override public Map<DataTicket<?>, Object> getDataMap() { return data; }
        @Override public <D> void addGeckolibData(DataTicket<D> ticket, D value) { data.put(ticket, value); }
        @Override public boolean hasGeckolibData(DataTicket<?> ticket) { return data.containsKey(ticket); }
    }
    @Override public State createRenderState(OverlayMarker entity, Void unused) { return new State(); }
    @Override public void addRenderData(OverlayMarker entity, Void unused, State state, float partialTick) { state.addGeckolibData(STATUE, entity.statue()); }
    @Override protected int getBlockLightLevel(OverlayMarker entity, BlockPos position) { return entity.activated() ? 15 : super.getBlockLightLevel(entity, position); }
    @Override protected int getSkyLightLevel(OverlayMarker entity, BlockPos position) { return entity.activated() ? 15 : super.getSkyLightLevel(entity, position); }
    @Override public int getRenderColor(OverlayMarker entity, Void unused, float partialTick) { return entity.activated() ? 0xffffffff : 0xff7d9298; }
}
