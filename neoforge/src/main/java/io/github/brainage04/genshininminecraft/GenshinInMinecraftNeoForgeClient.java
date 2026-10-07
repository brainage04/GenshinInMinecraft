package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.command.core.ClientModCommands;
import io.github.brainage04.genshininminecraft.client.HilichurlRenderer;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import io.github.brainage04.genshininminecraft.client.BaronBunnyRenderer;
import io.github.brainage04.genshininminecraft.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.client.ManagedCamera;
import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import io.github.brainage04.genshininminecraft.client.CombatFeedback;
import io.github.brainage04.genshininminecraft.client.GenshinHud;
import io.github.brainage04.genshininminecraft.network.DamageNumberPayload;
import io.github.brainage04.genshininminecraft.network.TargetAuraPayload;
import io.github.brainage04.genshininminecraft.network.PlayerCharacterPayload;
import io.github.brainage04.genshininminecraft.network.BunnyVisualPayload;
import io.github.brainage04.genshininminecraft.client.enemy.BunnyVisuals;
import io.github.brainage04.genshininminecraft.network.ProjectileVisualPayload;
import io.github.brainage04.genshininminecraft.client.ProjectileVisuals;
import io.github.brainage04.genshininminecraft.client.TeleportScreen;
import io.github.brainage04.genshininminecraft.client.OverlayMarkerRenderer;
import io.github.brainage04.genshininminecraft.network.TeleportListPayload;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@Mod(value = GenshinInMinecraft.MOD_ID, dist = Dist.CLIENT)
public final class GenshinInMinecraftNeoForgeClient {
    private static final ContextKey<List<CombatFeedback.WorldText>> COMBAT_TEXT = new ContextKey<>(GenshinHud.ID);
    private static final Set<Identifier> REPLACED = Set.of(VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.FOOD_LEVEL, VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND,
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR, VanillaGuiLayers.EXPERIENCE_LEVEL);
    public GenshinInMinecraftNeoForgeClient(ModContainer container, IEventBus modBus) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> AutoConfigClient.getConfigScreen(ModConfig.class, parent).get());
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
                ClientModCommands.register(event.getDispatcher(), CommandSourceStack::sendSystemMessage));
        GenshinInMinecraftClient.initialize();
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(GenshinEntities.HILICHURL, HilichurlRenderer::new);
            event.registerEntityRenderer(GenshinEntities.BARON_BUNNY, BaronBunnyRenderer::new);
            event.registerEntityRenderer(GenshinEntities.OVERLAY_MARKER, OverlayMarkerRenderer::new);
        });
        CombatInput.initialize(ClientPacketDistributor::sendToServer);
        ManagedCamera.initialize(ClientPacketDistributor::sendToServer);
        TeleportScreen.initialize(ClientPacketDistributor::sendToServer);
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(CombatInput.SKILL);
            event.register(CombatInput.BURST);
            event.register(CombatInput.MAP);
            for (var mapping : CombatInput.PARTY) event.register(mapping);
        });
        modBus.addListener((RegisterClientPayloadHandlersEvent event) -> {
            event.register(CharacterStatePayload.TYPE, (packet, context) -> CombatInput.accept(packet));
            event.register(io.github.brainage04.genshininminecraft.network.CoopStatePayload.TYPE,
                    (packet, context) -> GenshinHud.accept(packet));
            event.register(DamageNumberPayload.TYPE, (packet, context) -> CombatFeedback.accept(packet));
            event.register(TargetAuraPayload.TYPE, (packet, context) -> CombatFeedback.accept(packet));
            event.register(PlayerCharacterPayload.TYPE, (packet, context) -> CombatFeedback.accept(packet));
            event.register(BunnyVisualPayload.TYPE, (packet, context) -> BunnyVisuals.accept(packet));
            event.register(ProjectileVisualPayload.TYPE, (packet, context) -> ProjectileVisuals.accept(packet));
            event.register(TeleportListPayload.TYPE, (packet, context) -> TeleportScreen.accept(packet));
        });
        modBus.addListener((RegisterGuiLayersEvent event) ->
                event.registerAbove(VanillaGuiLayers.HOTBAR, GenshinHud.ID, GenshinHud::render));
        NeoForge.EVENT_BUS.addListener((RenderGuiLayerEvent.Pre event) -> {
            if (CombatInput.managed() && REPLACED.contains(event.getName())) event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((ExtractLevelRenderStateEvent event) ->
                event.getRenderState().setRenderData(COMBAT_TEXT,
                        CombatFeedback.extract(event.getDeltaTracker().getGameTimeDeltaPartialTick(false))));
        NeoForge.EVENT_BUS.addListener((SubmitCustomGeometryEvent event) -> CombatFeedback.submit(
                event.getLevelRenderState().getRenderData(COMBAT_TEXT), event.getPoseStack(),
                event.getSubmitNodeCollector(), event.getLevelRenderState().cameraRenderState));
    }
}
