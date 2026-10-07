package io.github.brainage04.genshininminecraft.client.enemy;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.network.BunnyVisualPayload;
import io.github.brainage04.genshininminecraft.rules.EnemyAnimations.Phase;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Server-retired Bunny's short inert presentation; cancelled fields never explode or leave a ghost. */
public final class BunnyVisuals {
    private record Pending(BunnyVisualPayload packet, Doll doll) {}
    public record Input(EnemyRenderInput animation, Vec3 position, float yaw, int light) {}
    public static final class State implements GeoRenderState {
        private final Map<DataTicket<?>, Object> data = new Reference2ObjectOpenHashMap<>();
        private final Input input;
        State(Input input) { this.input = input; }
        @Override public Map<DataTicket<?>, Object> getDataMap() { return data; }
    }
    public static final class Doll implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, false);
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new SyncedEnemyController<Doll>(false));
        }
    }
    private static final class Renderer extends GeoObjectRenderer<Doll, Input, State> {
        Renderer() { super(new EnemyGeoModel<>("baron_bunny")); }
        @Override public State createRenderState(Doll doll, Input input) { return new State(input); }
        @Override public long getInstanceId(Doll doll, Input input) { return 0; }
        @Override public void addRenderData(Doll doll, Input input, State state, float partialTick) {
            state.addGeckolibData(EnemyRenderInput.TICKET, input.animation());
            state.addGeckolibData(DataTickets.TICK, input.animation().seconds() * 20);
            state.addGeckolibData(DataTickets.PACKED_LIGHT, input.light());
        }
        @Override public void adjustRenderPose(RenderPassInfo<State> pass) {
            pass.poseStack().mulPose(Axis.YP.rotationDegrees(180 - pass.renderState().input.yaw()));
            // Half-sized authored doll, same scale2 as the authoritative landed Rabbit substrate.
            pass.poseStack().scale(2, 2, 2);
        }
    }
    private static final Map<UUID, Pending> pending = new HashMap<>();
    private static List<State> extracted = List.of();
    private static final Renderer renderer = new Renderer();
    private static ClientLevel level;
    private BunnyVisuals() {}
    public static void accept(BunnyVisualPayload packet) {
        var client = Minecraft.getInstance();
        if (client.level != level) { clear(); level = client.level; }
        if (client.level == null || !CombatInput.managed() || !client.level.dimension().identifier().equals(packet.dimension())) return;
        if (packet.kind() == BunnyVisualPayload.Kind.CANCEL) pending.remove(packet.owner());
        else pending.put(packet.owner(), new Pending(packet, new Doll()));
    }
    public static void clear() { pending.clear(); extracted = List.of(); level = null; }
    public static int activePresentations() { return pending.size(); }
    public static void extract(float partialTick) {
        var client = Minecraft.getInstance();
        if (client.level != level || !CombatInput.managed()) { clear(); level = client.level; return; }
        if (pending.isEmpty()) { extracted = List.of(); return; }
        double frame = (level.getGameTime() + (level.tickRateManager().isFrozen() ? 0 : partialTick)) * 3;
        var states = new ArrayList<State>(pending.size());
        var iterator = pending.values().iterator();
        while (iterator.hasNext()) {
            var event = iterator.next(); var packet = event.packet();
            var phase = packet.kind() == BunnyVisualPayload.Kind.THROW ? Phase.THROWN : Phase.EXPLODE;
            double elapsed = frame - packet.startWorldFrame();
            if (elapsed >= phase.frames()) { iterator.remove(); continue; }
            if (elapsed < 0) continue;
            Vec3 position = packet.origin();
            if (phase == Phase.THROWN) {
                double t = elapsed / phase.frames();
                position = position.lerp(packet.destination(), t).add(0, 4 * 1.1 * t * (1 - t), 0);
            }
            var animation = new EnemyRenderInput(phase, 0, elapsed / 60, 0, 1);
            BlockPos block = BlockPos.containing(position);
            int light = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, block) << 4
                    | level.getBrightness(net.minecraft.world.level.LightLayer.SKY, block) << 20;
            var input = new Input(animation, position, packet.yaw(), light);
            states.add(renderer.fillRenderState(event.doll(), input, renderer.createRenderState(event.doll(), input), partialTick));
        }
        extracted = List.copyOf(states);
    }
    public static void submit(PoseStack pose, LevelRenderState state, SubmitNodeCollector collector) {
        var camera = state.cameraRenderState.pos;
        for (var doll : extracted) {
            pose.pushPose();
            pose.translate(doll.input.position().x - camera.x, doll.input.position().y - camera.y, doll.input.position().z - camera.z);
            renderer.performRenderPass(doll, pose, collector, state.cameraRenderState);
            pose.popPose();
        }
    }
}
