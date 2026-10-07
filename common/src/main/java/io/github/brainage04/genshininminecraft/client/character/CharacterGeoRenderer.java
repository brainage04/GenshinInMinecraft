package io.github.brainage04.genshininminecraft.client.character;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Feet-origin body pass and a separately cached camera-local arms pass over the same original art. */
public final class CharacterGeoRenderer extends GeoObjectRenderer<CharacterAnimatable, CharacterRenderInput, CharacterGeoRenderState> {
    private static final String[] HIDE_ARMS = {"head", "hair", "accessory", "braid", "right_leg", "left_leg", "cape", "glider_left", "glider_right"};
    public CharacterGeoRenderer() { super(new CharacterGeoModel()); }
    @Override public CharacterGeoRenderState createRenderState(CharacterAnimatable animatable, CharacterRenderInput input) {
        return new CharacterGeoRenderState(input);
    }
    @Override public long getInstanceId(CharacterAnimatable animatable, CharacterRenderInput input) { return input.viewId(); }
    @Override public int getRenderColor(CharacterAnimatable animatable, CharacterRenderInput input, float partialTick) {
        return input.invisible() && !input.invisibleToPlayer() ? 0x26ffffff : 0xffffffff;
    }
    @Override public int getPackedOverlay(CharacterAnimatable animatable, CharacterRenderInput input, float u, float partialTick) {
        return input.overlay();
    }
    @Override public void addRenderData(CharacterAnimatable animatable, CharacterRenderInput input, CharacterGeoRenderState state, float partialTick) {
        state.addGeckolibData(CharacterGeoRenderState.INPUT, input);
        state.addGeckolibData(DataTickets.TICK, input.age());
        state.addGeckolibData(DataTickets.PACKED_LIGHT, input.light());
    }
    @Override public RenderType getRenderType(CharacterGeoRenderState state, Identifier texture) {
        var input = state.input();
        if (!input.invisible()) return RenderTypes.entityCutout(texture);
        if (!input.invisibleToPlayer()) return RenderTypes.entityTranslucentCullItemTarget(texture);
        return input.outlineColor() != 0 ? RenderTypes.outline(texture) : null;
    }
    @Override public void adjustRenderPose(RenderPassInfo<CharacterGeoRenderState> pass) {
        var input = pass.renderState().input();
        var pose = pass.poseStack();
        if (input.arms()) {
            // The view looks down -Z. Move foot-origin geometry below the eye, hands out in front.
            pose.translate(0, -1.2, -.55);
            pose.scale(.85F, .85F, .85F);
        } else {
            pose.scale(input.scale(), input.scale(), input.scale());
            pose.mulPose(Axis.YP.rotationDegrees(180 - input.bodyYaw()));
            if (input.deathTime() > 0) pose.mulPose(Axis.ZP.rotationDegrees(
                    Math.min((float) Math.sqrt(Math.max(0, input.deathTime() - 1) / 20 * 1.6), 1) * 90));
            pose.translate(0, .01, 0);
        }
    }
    @Override public void adjustModelBonesForRender(RenderPassInfo<CharacterGeoRenderState> pass, BoneSnapshots bones) {
        var input = pass.renderState().input();
        if (input.arms()) {
            for (String name : HIDE_ARMS) bones.ifPresent(name, bone -> bone.skipRender(true).skipChildrenRender(true));
            // These are arm ancestors: hide only their cubes, not their children/transforms.
            bones.ifPresent("hips", bone -> bone.skipRender(true));
            bones.ifPresent("torso", bone -> bone.skipRender(true).setRotation(0, 0, 0));
            bones.ifPresent("root", bone -> bone.setRotation(0, 0, 0).setTranslation(0, 0, 0));
            bones.ifPresent("right_arm", bone -> bone.setRotation(1.05F, 0, -.12F));
            bones.ifPresent("left_arm", bone -> bone.setRotation(1.05F, 0, .12F));
            bones.ifPresent("right_forearm", bone -> bone.setRotation(.25F, 0, 0));
            bones.ifPresent("left_forearm", bone -> bone.setRotation(.25F, 0, 0));
        } else {
            bones.ifPresent("head", bone -> bone.setRotY(bone.getRotY() - (float) Math.toRadians(input.headYaw()))
                    .setRotX(bone.getRotX() - (float) Math.toRadians(Math.clamp(input.pitch(), -45, 45))));
            boolean glide = input.phase() == io.github.brainage04.genshininminecraft.rules.Locomotion.Phase.GLIDE_START
                    || input.phase() == io.github.brainage04.genshininminecraft.rules.Locomotion.Phase.GLIDE_LOOP
                    || input.phase() == io.github.brainage04.genshininminecraft.rules.Locomotion.Phase.GLIDE_STOP;
            if (!glide) {
                bones.ifPresent("glider_left", bone -> bone.skipRender(true).skipChildrenRender(true));
                bones.ifPresent("glider_right", bone -> bone.skipRender(true).skipChildrenRender(true));
            }
        }
    }
    @Override public void submitRenderTasks(RenderPassInfo<CharacterGeoRenderState> pass, OrderedSubmitNodeCollector collector, RenderType type) {
        super.submitRenderTasks(pass, collector, type);
        var input = pass.renderState().input();
        // Custom geo submits do not inherit LivingEntityRenderer's outline colour automatically.
        if (input.outlineColor() != 0 && !(input.invisible() && input.invisibleToPlayer())) {
            collector.submitCustomGeometry(pass.poseStack(), RenderTypes.outline(getTextureLocation(pass.renderState())), (pose, vertices) -> {
                var stack = pass.poseStack();
                stack.pushPose();
                stack.last().set(pose);
                pass.renderPosed(() -> pass.model().render(pass, vertices, pass.packedLight(), pass.packedOverlay(), input.outlineColor()));
                stack.popPose();
            });
        }
    }
}
