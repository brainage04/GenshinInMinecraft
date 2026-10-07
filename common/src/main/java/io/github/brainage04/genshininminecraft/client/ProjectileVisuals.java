package io.github.brainage04.genshininminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.network.ProjectileVisualPayload;
import io.github.brainage04.genshininminecraft.network.ProjectileVisualPayload.Kind;
import io.github.brainage04.genshininminecraft.rules.kit.KaeyaKit;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.ui.ElementPalette;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;

/** Original emissive arrow/bolt/icicle meshes and spiral ribbons; independent of particle settings. */
public final class ProjectileVisuals {
    private record Key(UUID owner, long id) {}
    public record State(Kind kind, Vec3 origin, Vec3 destination, double elapsed, double progress, long id) {}
    private static final Map<Key, ProjectileVisualPayload> pending = new HashMap<>();
    private static List<State> extracted = List.of();
    private static ClientLevel level;
    private ProjectileVisuals() {}
    public static void clear() { pending.clear(); extracted = List.of(); level = null; }
    public static List<State> renderStates() { return extracted; }
    public static boolean has(Kind kind) { return extracted.stream().anyMatch(state -> state.kind() == kind); }
    public static void accept(ProjectileVisualPayload packet) {
        var current = Minecraft.getInstance().level;
        if (level != current) { clear(); level = current; }
        if (current == null || !CombatInput.managed() || !current.dimension().identifier().equals(packet.dimension())) return;
        if (packet.kind() == Kind.CANCEL) {
            if (packet.id() == 0) pending.keySet().removeIf(key -> key.owner().equals(packet.owner()));
            else pending.remove(new Key(packet.owner(), packet.id()));
        } else pending.put(new Key(packet.owner(), packet.id()), packet);
    }
    public static void extract(float partialTick) {
        var current = Minecraft.getInstance().level;
        if (level != current || !CombatInput.managed()) { clear(); level = current; return; }
        if (level == null || pending.isEmpty()) { extracted = List.of(); return; }
        double frame = (level.getGameTime() + (level.tickRateManager().isFrozen() ? 0 : partialTick)) * 3;
        var states = new ArrayList<State>(pending.size());
        var iterator = pending.values().iterator();
        while (iterator.hasNext()) {
            var packet = iterator.next();
            double elapsed = frame - packet.startWorldFrame();
            if (elapsed >= packet.durationFrames()) { iterator.remove(); continue; }
            if (elapsed < 0) continue;
            Vec3 origin = packet.origin(), destination = packet.destination();
            if (packet.kind() == Kind.WALTZ_ICICLE) {
                var owner = level.getPlayerByUUID(packet.owner());
                if (owner != null) origin = owner.position().add(0, .8, 0);
                double angle = elapsed * 2 * Math.PI / KaeyaKit.ADAPTED_REVOLUTION_FRAMES + packet.destination().x;
                origin = origin.add(Math.sin(angle) * CombatRuntime.ADAPTED_ICICLE_ORBIT_RADIUS, 0,
                        Math.cos(angle) * CombatRuntime.ADAPTED_ICICLE_ORBIT_RADIUS);
                destination = origin.add(.25 * Math.cos(angle), -.65, -.25 * Math.sin(angle));
            }
            states.add(new State(packet.kind(), origin, destination, elapsed, elapsed / packet.durationFrames(), packet.id()));
        }
        extracted = List.copyOf(states);
    }
    public static void submit(PoseStack pose, LevelRenderState levelState, SubmitNodeCollector collector) {
        var camera = levelState.cameraRenderState.pos;
        for (var state : extracted) {
            pose.pushPose();
            pose.translate(-camera.x, -camera.y, -camera.z);
            // Unlit position/colour alpha blending preserves purple; additive lightning plus
            // overlapping pale cores saturated Rose bolts to white at ordinary camera distance.
            collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (snapshot, vertices) -> draw(snapshot, vertices, state));
            pose.popPose();
        }
    }
    private static void draw(PoseStack.Pose pose, VertexConsumer vertices, State state) {
        switch (state.kind()) {
            case ARROW, PYRO_ARROW, RAIN_ARROW -> {
                Vec3 direction = state.destination().subtract(state.origin()).normalize();
                Vec3 tip = state.origin().lerp(state.destination(), state.progress());
                boolean pyro = state.kind() != Kind.ARROW;
                Vec3 tail = tip.subtract(direction.scale(1.2));
                int color = ElementPalette.color(pyro ? Element.PYRO : Element.PHYSICAL);
                beam(pose, vertices, tip.subtract(direction.scale(2.2)), tail, .15, (color & 0xffffff) | 0x90000000);
                beam(pose, vertices, tail, tip, .09, color);
                // Broad original diamond head and two crossed feather fins, not a particle-only tracer.
                diamond(pose, vertices, tip.subtract(direction.scale(.18)), tip.add(direction.scale(.25)), .22, color);
                diamond(pose, vertices, tail, tail.add(direction.scale(.35)), .18, color);
                if (pyro) {
                    beam(pose, vertices, tip.subtract(direction.scale(2.2)), tail, .22, 0x90ff6b22);
                    diamond(pose, vertices, tip.subtract(direction.scale(.4)), tip.add(direction.scale(.25)), .28, 0x70ff8c25);
                    for (int flame = 0; flame < 3; flame++) {
                        Vec3 ember = tip.subtract(direction.scale(.65 + flame * .3));
                        double lick = .18 + .07 * Math.sin(state.elapsed() * .9 + flame * 2);
                        diamond(pose, vertices, ember, ember.add(0, lick, 0), .13, 0xafffa52c);
                    }
                }
            }
            case VIOLET_ORB -> {
                Vec3 center = state.origin().lerp(state.destination(), state.progress());
                int color = ElementPalette.color(Element.ELECTRO);
                diamond(pose, vertices, center.add(0, -.38, 0), center.add(0, .38, 0), .36, color);
                spiral(pose, vertices, center.add(0, -.4, 0), .52, .8, state.elapsed() * .22, 0xb0b95aff);
                Vec3 tail = center.subtract(state.destination().subtract(state.origin()).normalize().scale(1.8));
                beam(pose, vertices, tail, center, .13, 0xa0bc66ff);
            }
            case CATALYST_BOLT, CHARGED_BOLT, ROSE_BOLT -> {
                Vec3 difference = state.destination().subtract(state.origin());
                Vec3 previous = state.origin();
                double width = state.kind() == Kind.CHARGED_BOLT ? .18 : .12;
                for (int segment = 1; segment <= 8; segment++) {
                    double fraction = segment / 8.0;
                    double jitter = segment == 8 ? 0 : Math.sin(segment * 2.7 + state.id()) * .15;
                    Vec3 next = state.origin().add(difference.scale(fraction)).add(jitter, -jitter * .7, jitter * .4);
                    beam(pose, vertices, previous, next, width * 1.8, 0x70b95aff);
                    beam(pose, vertices, previous, next, width, ElementPalette.color(Element.ELECTRO));
                    beam(pose, vertices, previous, next, width * .45, 0xffbb78ff);
                    previous = next;
                }
            }
            case FROST_ICICLE, WALTZ_ICICLE -> {
                Vec3 start = state.origin(), end = state.destination();
                if (state.kind() == Kind.FROST_ICICLE) {
                    Vec3 tip = start.lerp(end, state.progress());
                    start = tip.subtract(end.subtract(start).normalize().scale(.9)); end = tip;
                }
                diamond(pose, vertices, start, end, .23, ElementPalette.color(Element.CRYO));
                beam(pose, vertices, start.subtract(end.subtract(start).normalize().scale(.6)), start, .12, 0x90a2e8f5);
                beam(pose, vertices, start, end, .07, 0xffd0f6ff);
            }
            case PALM_VORTEX -> spiral(pose, vertices, state.origin(), 1.1, 1.8, state.elapsed() * .25, 0xb070f4cd);
            case TORNADO -> {
                Vec3 center = state.origin().lerp(state.destination(), state.progress());
                for (int ribbon = 0; ribbon < 3; ribbon++)
                    spiral(pose, vertices, center.add(0, -.6, 0), 1.7, 3.8,
                            state.elapsed() * .18 + ribbon * 2 * Math.PI / 3, 0xa075f2cb);
            }
            case CANCEL -> { }
        }
    }
    private static void spiral(PoseStack.Pose pose, VertexConsumer vertices, Vec3 center,
            double radius, double height, double rotation, int color) {
        double px = center.x + Math.cos(rotation) * radius * .25;
        double py = center.y, pz = center.z + Math.sin(rotation) * radius * .25;
        for (int point = 1; point <= 36; point++) {
            double t = point / 36.0, angle = rotation + t * 4 * Math.PI;
            double r = radius * (.25 + .75 * t);
            double x = center.x + Math.cos(angle) * r, y = center.y + height * t, z = center.z + Math.sin(angle) * r;
            beam(pose, vertices, px, py, pz, x, y, z, .055, color);
            px = x; py = y; pz = z;
        }
    }
    private static void beam(PoseStack.Pose pose, VertexConsumer vertices, Vec3 from, Vec3 to, double width, int color) {
        beam(pose, vertices, from.x, from.y, from.z, to.x, to.y, to.z, width, color);
    }
    /** Numeric basis avoids allocating vectors per ribbon segment/vertex in the render hot path. */
    private static void beam(PoseStack.Pose pose, VertexConsumer vertices,
            double x, double y, double z, double tx, double ty, double tz, double width, int color) {
        double dx = tx - x, dy = ty - y, dz = tz - z, length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < .000001) return;
        dx /= length; dy /= length; dz /= length;
        double sx = Math.abs(dy) > .9 ? 0 : -dz, sy = Math.abs(dy) > .9 ? dz : 0, sz = Math.abs(dy) > .9 ? -dy : dx;
        double scale = width / Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx *= scale; sy *= scale; sz *= scale;
        double ux = dy * sz - dz * sy, uy = dz * sx - dx * sz, uz = dx * sy - dy * sx;
        vertex(pose, vertices, x + sx, y + sy, z + sz, color); vertex(pose, vertices, tx + sx, ty + sy, tz + sz, color);
        vertex(pose, vertices, tx - sx, ty - sy, tz - sz, color); vertex(pose, vertices, x - sx, y - sy, z - sz, color);
        vertex(pose, vertices, x + ux, y + uy, z + uz, color); vertex(pose, vertices, tx + ux, ty + uy, tz + uz, color);
        vertex(pose, vertices, tx - ux, ty - uy, tz - uz, color); vertex(pose, vertices, x - ux, y - uy, z - uz, color);
    }
    private static void diamond(PoseStack.Pose pose, VertexConsumer vertices, Vec3 base, Vec3 tip, double radius, int color) {
        double dx = tip.x - base.x, dy = tip.y - base.y, dz = tip.z - base.z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < .000001) return;
        double cx = base.x + dx * .35, cy = base.y + dy * .35, cz = base.z + dz * .35;
        dx /= length; dy /= length; dz /= length;
        double sx = Math.abs(dy) > .9 ? 0 : -dz, sy = Math.abs(dy) > .9 ? dz : 0, sz = Math.abs(dy) > .9 ? -dy : dx;
        double scale = radius / Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx *= scale; sy *= scale; sz *= scale;
        double ux = dy * sz - dz * sy, uy = dz * sx - dx * sz, uz = dx * sy - dy * sx;
        facet(pose, vertices, base, tip, cx + sx, cy + sy, cz + sz, cx + ux, cy + uy, cz + uz, color);
        facet(pose, vertices, base, tip, cx + ux, cy + uy, cz + uz, cx - sx, cy - sy, cz - sz, color);
        facet(pose, vertices, base, tip, cx - sx, cy - sy, cz - sz, cx - ux, cy - uy, cz - uz, color);
        facet(pose, vertices, base, tip, cx - ux, cy - uy, cz - uz, cx + sx, cy + sy, cz + sz, color);
    }
    private static void facet(PoseStack.Pose pose, VertexConsumer vertices, Vec3 base, Vec3 tip,
            double x, double y, double z, double tx, double ty, double tz, int color) {
        vertex(pose, vertices, base.x, base.y, base.z, color); vertex(pose, vertices, x, y, z, color);
        vertex(pose, vertices, tip.x, tip.y, tip.z, color); vertex(pose, vertices, tx, ty, tz, color);
    }
    private static void vertex(PoseStack.Pose pose, VertexConsumer vertices, double x, double y, double z, int color) {
        vertices.addVertex(pose, (float) x, (float) y, (float) z).setColor(color);
    }
}
