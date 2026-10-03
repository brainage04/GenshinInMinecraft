package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.command.core.ClientModCommands;
import io.github.brainage04.genshininminecraft.client.HilichurlRenderer;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import io.github.brainage04.genshininminecraft.client.CombatFeedback;
import io.github.brainage04.genshininminecraft.client.GenshinHud;
import io.github.brainage04.genshininminecraft.network.DamageNumberPayload;
import io.github.brainage04.genshininminecraft.network.TargetAuraPayload;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public class GenshinInMinecraftFabricClient implements ClientModInitializer {
    private static final RenderStateDataKey<List<CombatFeedback.WorldText>> COMBAT_TEXT = RenderStateDataKey.create();
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                ClientModCommands.register(dispatcher, FabricClientCommandSource::sendFeedback));
        GenshinInMinecraftClient.initialize();
        EntityRendererRegistry.register(GenshinEntities.HILICHURL, HilichurlRenderer::new);
        KeyMappingHelper.registerKeyMapping(CombatInput.SKILL);
        KeyMappingHelper.registerKeyMapping(CombatInput.BURST);
        CombatInput.initialize(ClientPlayNetworking::send);
        ClientPlayNetworking.registerGlobalReceiver(CharacterStatePayload.TYPE,
                (packet, context) -> CombatInput.accept(packet));
        ClientPlayNetworking.registerGlobalReceiver(DamageNumberPayload.TYPE,
                (packet, context) -> CombatFeedback.accept(packet));
        ClientPlayNetworking.registerGlobalReceiver(TargetAuraPayload.TYPE,
                (packet, context) -> CombatFeedback.accept(packet));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, GenshinHud.ID, GenshinHud::render);
        for (Identifier layer : List.of(VanillaHudElements.HEALTH_BAR, VanillaHudElements.FOOD_BAR,
                VanillaHudElements.ARMOR_BAR, VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL)) {
            HudElementRegistry.replaceElement(layer, original -> (graphics, delta) -> {
                if (!CombatInput.managed()) original.extractRenderState(graphics, delta);
            });
        }
        LevelExtractionEvents.END_EXTRACTION.register(context ->
                ((FabricRenderState) context.levelState()).setData(COMBAT_TEXT,
                        CombatFeedback.extract(context.deltaTracker().getGameTimeDeltaPartialTick(false))));
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> CombatFeedback.submit(
                ((FabricRenderState) context.levelState()).getData(COMBAT_TEXT), context.poseStack(),
                context.submitNodeCollector(), context.levelState().cameraRenderState));
    }
}
