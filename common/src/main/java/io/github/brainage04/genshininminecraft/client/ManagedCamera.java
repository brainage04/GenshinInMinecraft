package io.github.brainage04.genshininminecraft.client;

import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.network.CameraYawPayload;
import io.github.brainage04.genshininminecraft.rules.CameraMath;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

/** View state only. Entity rotation remains combat facing and travels in vanilla rotation packets. */
public final class ManagedCamera {
    public static final float ORBIT_DISTANCE = 4;
    public static final float AIM_DISTANCE = 2;
    public static final float AIM_RIGHT_OFFSET = .6F;
    private static Consumer<CameraYawPayload> sender;
    private static CameraType previousVanillaView;
    private static CameraType beforeAimView;
    private static LocalPlayer owner;
    private static float yaw;
    private static float pitch;
    private static float sentYaw = Float.NaN;
    private static int facingHoldThroughTick;
    private ManagedCamera() {}

    public static void initialize(Consumer<CameraYawPayload> send) { sender = send; }
    public static void tick(Minecraft client) {
        if (!CombatInput.managed() || client.player == null) { reset(); return; }
        if (previousVanillaView == null) {
            previousVanillaView = client.options.getCameraType();
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if (owner != client.player) {
            owner = client.player;
            yaw = owner.getYRot();
            pitch = owner.getXRot();
            sentYaw = Float.NaN;
            facingHoldThroughTick = -1;
        }
        if (CombatInput.aiming()) {
            if (beforeAimView == null) beforeAimView = client.options.getCameraType();
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            face(owner, yaw, pitch);
        } else if (beforeAimView != null) {
            client.options.setCameraType(beforeAimView);
            beforeAimView = null;
        }
        if (!decoupled()) { yaw = owner.getYRot(); pitch = owner.getXRot(); }
        sendYaw();
    }
    public static void reset() {
        if (previousVanillaView != null) Minecraft.getInstance().options.setCameraType(previousVanillaView);
        previousVanillaView = null;
        beforeAimView = null;
        owner = null;
        sentYaw = Float.NaN;
    }
    public static boolean decoupled() {
        Minecraft client = Minecraft.getInstance();
        return CombatInput.managed() && owner == client.player && owner != null
                && client.getCameraEntity() == owner && !owner.isSleeping()
                && !client.options.getCameraType().isFirstPerson();
    }
    public static boolean controls(Entity entity) { return decoupled() && entity == owner; }
    public static float yaw() { return yaw; }
    public static float pitch() { return pitch; }
    public static void setAngles(float newYaw, float newPitch) {
        yaw = CameraMath.wrap(newYaw);
        pitch = Math.clamp(newPitch, -89, 89);
        if (CombatInput.aiming() && owner != null) face(owner, yaw, pitch);
    }
    /** Receives vanilla sensitivity/inversion/smoothing-adjusted mouse deltas. */
    public static void turn(double dx, double dy) { setAngles(yaw + (float) (dx * .15), pitch + (float) (dy * .15)); }
    public static void togglePerspective(Minecraft client) {
        if (CombatInput.aiming()) return; // An aimed hold always has a shoulder view.
        if (client.options.getCameraType().isFirstPerson()) {
            setAngles(client.player.getYRot(), client.player.getXRot());
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else {
            face(client.player, yaw, pitch);
            client.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }
    public static void applyMovement(LocalPlayer player) {
        if (!controls(player) || player.isPassenger()) return;
        float left = player.xxa;
        float forward = player.zza;
        if (CombatInput.aiming()) face(player, yaw, pitch);
        else if (left * left + forward * forward > .0001F && player.tickCount > facingHoldThroughTick) {
            face(player, CameraMath.smoothYaw(player.getYRot(), CameraMath.movementYaw(yaw, left, forward),
                    CameraMath.TURN_DEGREES_PER_TICK), 0);
        }
        // Vanilla still applies its own sneak/use-item slowdown, acceleration, gravity and collision.
        double difference = Math.toRadians(yaw - player.getYRot());
        double sine = Math.sin(difference);
        double cosine = Math.cos(difference);
        player.xxa = (float) (left * cosine - forward * sine);
        player.zza = (float) (forward * cosine + left * sine);
        sendYaw();
    }
    /** Edge-triggered assistance, not persistent tracking or client hit authority. */
    public static void beforeAction() {
        Minecraft client = Minecraft.getInstance();
        if (!CombatInput.managed() || client.player == null) return;
        LocalPlayer player = client.player;
        if (CombatInput.aiming()) face(player, yaw, pitch);
        else {
            LivingEntity best = null;
            double bestDistance = Double.POSITIVE_INFINITY;
            float viewYaw = decoupled() ? yaw : player.getYRot();
            for (Entity entity : client.level.entitiesForRendering()) {
                if (!(entity instanceof LivingEntity enemy) || enemy instanceof Player || !enemy.isAlive()
                        || enemy.isRemoved() || enemy.isInvisible() || isPuppet(enemy)) continue;
                double distance = CameraMath.lockDistanceSquared(viewYaw, enemy.getX() - player.getX(),
                        enemy.getY() - player.getY(), enemy.getZ() - player.getZ());
                if (!CameraMath.preferTarget(distance, enemy.getId(), bestDistance, best == null ? Integer.MAX_VALUE : best.getId())
                        || !player.hasLineOfSight(enemy)) continue;
                best = enemy;
                bestDistance = distance;
            }
            if (best != null) {
                float bodyYaw = CameraMath.facingYaw(best.getX() - player.getX(), best.getZ() - player.getZ());
                float bodyPitch = CombatInput.state().activeSlot() == 1 ? (float) -Math.toDegrees(Math.atan2(
                        best.getEyeY() - player.getEyeY(), Math.hypot(best.getX() - player.getX(), best.getZ() - player.getZ()))) : 0;
                face(player, bodyYaw, bodyPitch);
                facingHoldThroughTick = player.tickCount + 3;
            }
        }
        // Key handling precedes LocalPlayer.tick/sendPosition: enqueue facing BEFORE the cast packet.
        player.connection.send(new ServerboundMovePlayerPacket.Rot(player.getYRot(), player.getXRot(),
                player.onGround(), player.horizontalCollision));
    }
    private static boolean isPuppet(LivingEntity entity) {
        return entity.getType() == GenshinEntities.BARON_BUNNY;
    }
    /** Project the same blocked eye ray as the server, not a parallel ray from the shoulder. */
    public static Vec3 aimPoint(float partialTick) {
        Vec3 start = owner.getEyePosition(partialTick);
        Vec3 end = start.add(owner.getLookAngle().scale(16));
        end = owner.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, owner)).getLocation();
        double bestDistance = start.distanceToSqr(end);
        for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity enemy) || enemy instanceof Player || !enemy.isAlive()
                    || enemy.isRemoved() || isPuppet(enemy)) continue;
            var hit = enemy.getBoundingBox().inflate(.15).clip(start, end);
            if (hit.isPresent() && hit.get().distanceToSqr(start) < bestDistance) {
                end = hit.get();
                bestDistance = start.distanceToSqr(end);
            }
        }
        return end;
    }
    private static void face(LocalPlayer player, float bodyYaw, float bodyPitch) {
        player.setYRot(bodyYaw);
        player.setYHeadRot(bodyYaw);
        player.setYBodyRot(bodyYaw);
        player.setXRot(bodyPitch);
    }
    private static void sendYaw() {
        // Spectator packets are rejected server-side; never cache their yaw as accepted.
        // Invalidating here also resends an unchanged orbit as soon as play resumes.
        if (owner.isSpectator()) { sentYaw = Float.NaN; return; }
        float movementYaw = decoupled() ? yaw : owner.getYRot();
        if (sender != null && movementYaw != sentYaw) {
            sender.accept(new CameraYawPayload(movementYaw));
            sentYaw = movementYaw;
        }
    }
}
