package io.github.brainage04.genshininminecraft.client.character;

import com.geckolib.constant.DataTickets;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.network.PlayerCharacterPayload;
import io.github.brainage04.genshininminecraft.rules.Locomotion;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Public network snapshots and owned per-player/view caches, confined to the client extraction thread. */
public final class PlayerVisuals {
    private static final CharacterGeoRenderer RENDERER = new CharacterGeoRenderer();
    private static final Map<Integer, PlayerCharacterPayload> snapshots = new HashMap<>();
    private static final Map<Integer, Views> views = new HashMap<>();
    private static ClientLevel level;
    private static CharacterGeoRenderState arms;
    private static long bodySubmissions;
    private static long armSubmissions;
    private static int lastBodySlot = -1;
    private static int lastArmSlot = -1;
    private record Views(UUID uuid, int slot, CharacterAnimatable body, CharacterAnimatable arms) {}
    private PlayerVisuals() {}
    public static CharacterGeoRenderer renderer() { return RENDERER; }
    public static long bodySubmissions() { return bodySubmissions; }
    public static long armSubmissions() { return armSubmissions; }
    public static int lastBodySlot() { return lastBodySlot; }
    public static int lastArmSlot() { return lastArmSlot; }
    public static Collection<PlayerCharacterPayload> snapshots() { return snapshots.values(); }
    public static PlayerCharacterPayload snapshot(int id) { return snapshots.get(id); }
    public static void reset() { snapshots.clear(); views.clear(); arms = null; level = null; }
    public static void invalidateViews() { views.clear(); arms = null; }
    private static void useLevel(ClientLevel current) {
        if (level != current) { reset(); level = current; }
    }
    public static void tick(Minecraft client) {
        useLevel(client.level);
        if (level == null) return;
        snapshots.values().removeIf(packet -> {
            var entity = level.getEntity(packet.playerId());
            // Allow the tracking snapshot to arrive just before its vanilla spawn packet.
            return entity == null ? level.getGameTime() - packet.sampleGameTime() > 40 : !entity.getUUID().equals(packet.playerUuid());
        });
        views.keySet().removeIf(id -> !snapshots.containsKey(id) || level.getEntity(id) == null);
    }
    public static void accept(PlayerCharacterPayload packet) {
        useLevel(Minecraft.getInstance().level);
        if (packet.slot() < 0 || packet.slot() >= 4) { snapshots.remove(packet.playerId()); views.remove(packet.playerId()); arms = null; return; }
        snapshots.put(packet.playerId(), packet);
        var cached = views.get(packet.playerId());
        if (cached != null && (cached.slot() != packet.slot() || !cached.uuid().equals(packet.playerUuid()))) {
            views.remove(packet.playerId());
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getId() == packet.playerId()) arms = null;
        }
    }
    public static boolean usesCharacter(AbstractClientPlayer player) {
        var packet = snapshots.get(player.getId());
        return CombatInput.managed() && !player.isSpectator() && packet != null && packet.playerUuid().equals(player.getUUID());
    }
    public static CharacterGeoRenderState extract(AbstractClientPlayer player, AvatarRenderState vanilla, float partialTick, boolean firstPerson) {
        useLevel(Minecraft.getInstance().level);
        if (!usesCharacter(player)) return null;
        var packet = snapshots.get(player.getId());
        var cached = views.get(player.getId());
        if (cached == null) {
            cached = new Views(player.getUUID(), packet.slot(), new CharacterAnimatable(packet.slot()), new CharacterAnimatable(packet.slot()));
            views.put(player.getId(), cached);
        }
        double frame = Locomotion.renderFrame(packet.sampleFrame(), packet.sampleGameTime(), level.getGameTime(), partialTick);
        float bodyYaw = vanilla == null ? Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) : vanilla.bodyRot;
        float headYaw = vanilla == null ? Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot) - bodyYaw : vanilla.yRot;
        float pitch = vanilla == null ? player.getXRot(partialTick) : vanilla.xRot;
        float scale = vanilla == null ? player.getScale() : vanilla.scale;
        int light = vanilla == null ? Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player).getPackedLightCoords(player, partialTick) : vanilla.lightCoords;
        int overlay = vanilla == null ? OverlayTexture.pack(0, OverlayTexture.v(player.hurtTime > 0 || player.deathTime > 0))
                : LivingEntityRenderer.getOverlayCoords(vanilla, 0);
        boolean invisible = vanilla == null ? player.isInvisible() : vanilla.isInvisible;
        boolean invisibleToPlayer = vanilla == null ? player.isInvisibleTo(Minecraft.getInstance().player) : vanilla.isInvisibleToPlayer;
        int outline = vanilla == null ? 0 : vanilla.outlineColor;
        var input = new CharacterRenderInput(((long) player.getId() << 1) | (firstPerson ? 1 : 0), packet.slot(),
                packet.phase(), packet.occurrence(), packet.phase().seconds(frame, packet.phaseStartFrame()),
                level.getGameTime() + partialTick, bodyYaw, headYaw, pitch, scale, light, overlay,
                invisible, invisibleToPlayer, outline, vanilla == null ? player.deathTime : vanilla.deathTime, firstPerson);
        var animatable = firstPerson ? cached.arms() : cached.body();
        return RENDERER.fillRenderState(animatable, input, RENDERER.createRenderState(animatable, input), partialTick);
    }
    public static void extractArms(float partialTick) {
        var client = Minecraft.getInstance();
        arms = client.player != null && client.options.getCameraType().isFirstPerson()
                && client.getCameraEntity() == client.player ? extract(client.player, null, partialTick, true) : null;
    }
    public static void submitBody(CharacterGeoRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        RENDERER.performRenderPass(state, pose, collector, camera);
        bodySubmissions++;
        lastBodySlot = state.input().slot();
    }
    public static boolean submitArms(PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, int light) {
        if (arms == null) return false;
        arms.addGeckolibData(DataTickets.PACKED_LIGHT, light);
        RENDERER.performRenderPass(arms, pose, collector, camera);
        armSubmissions++;
        lastArmSlot = arms.input().slot();
        return true;
    }
}
