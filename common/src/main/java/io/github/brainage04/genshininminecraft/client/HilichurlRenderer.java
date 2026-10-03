package io.github.brainage04.genshininminecraft.client;

import io.github.brainage04.genshininminecraft.GenshinInMinecraft;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Vanilla humanoid geometry with an original mask/skin texture and a raised-club wind-up. */
public final class HilichurlRenderer extends HumanoidMobRenderer<Hilichurl, HilichurlRenderer.State, HilichurlRenderer.Model> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(GenshinInMinecraft.MOD_ID, "textures/entity/hilichurl.png");
    public HilichurlRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModelLayers.ZOMBIE)), .5F);
    }
    @Override public State createRenderState() { return new State(); }
    @Override public Identifier getTextureLocation(State state) { return TEXTURE; }
    @Override public void extractRenderState(Hilichurl entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.windingUp = entity.isWindingUp();
    }
    public static final class State extends HumanoidRenderState {
        public boolean windingUp;
    }
    public static final class Model extends HumanoidModel<State> {
        public Model(ModelPart root) { super(root); }
        @Override public void setupAnim(State state) {
            super.setupAnim(state);
            if (state.windingUp) {
                rightArm.xRot = -2.5F;
                rightArm.zRot = -.35F;
                leftArm.xRot = -.45F;
            }
        }
    }
}
