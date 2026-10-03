package io.github.brainage04.genshininminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.brainage04.genshininminecraft.network.DamageNumberPayload;
import io.github.brainage04.genshininminecraft.network.TargetAuraPayload;
import io.github.brainage04.genshininminecraft.network.PlayerCharacterPayload;
import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.Element;
import java.util.ArrayList;
import io.github.brainage04.genshininminecraft.ui.HudFormatting;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Main-thread synced visual state, extracted to immutable billboard submits on both loaders. */
public final class CombatFeedback {
    public static final int LIFETIME_TICKS = 20;
    private static final Element[] ELEMENTS = Element.values();
    private static final FormattedCharSequence[] AURA_TEXT = new FormattedCharSequence[ELEMENTS.length];
    static {
        for (Element element : ELEMENTS) AURA_TEXT[element.ordinal()] =
                Component.literal("[" + ElementPalette.symbol(element) + "]").getVisualOrderText();
    }
    private static final List<NumberEntry> numbers = new ArrayList<>();
    private static final Map<Integer, Integer> auras = new HashMap<>();
    private static final Map<Integer, Integer> playerCharacters = new HashMap<>();
    private static final FormattedCharSequence[] CHARACTER_TEXT = {
        Component.literal("Traveler · Anemo").getVisualOrderText(),
        Component.literal("Amber · Pyro").getVisualOrderText(),
        Component.literal("Kaeya · Cryo").getVisualOrderText(),
        Component.literal("Lisa · Electro").getVisualOrderText()
    };
    private static ClientLevel level;
    private static long tick;
    private static int sequence;

    public record NumberEntry(int targetId, Vec3 anchor, long bornTick, FormattedCharSequence amount,
            FormattedCharSequence reaction, int color, boolean critical, int pixelOffset) {}
    public record WorldText(Vec3 position, FormattedCharSequence text, float halfWidth, int color, float scale) {}

    private CombatFeedback() {}
    public static boolean hasDamageNumber(int targetId) {
        return numbers.stream().anyMatch(number -> number.targetId() == targetId && number.amount() != null);
    }
    public static int auraElements(int targetId) { return auras.getOrDefault(targetId, 0); }
    public static void reset() { numbers.clear(); auras.clear(); playerCharacters.clear(); level = null; tick = 0; sequence = 0; }
    private static void useLevel(ClientLevel current) {
        if (level != current) {
            reset();
            level = current;
        }
    }
    public static void tick(Minecraft client) {
        useLevel(client.level);
        tick++;
        numbers.removeIf(number -> tick - number.bornTick() >= LIFETIME_TICKS);
        if (level != null) auras.keySet().removeIf(id -> level.getEntity(id) == null);
        if (level != null) playerCharacters.keySet().removeIf(id -> level.getEntity(id) == null);
    }
    public static void accept(DamageNumberPayload packet) {
        ClientLevel current = Minecraft.getInstance().level;
        useLevel(current);
        if (current == null) return;
        Entity target = current.getEntity(packet.targetId());
        if (target == null) return;
        FormattedCharSequence amount = packet.amount() > 0
                ? Component.literal(Long.toString(Math.round(packet.amount()))).getVisualOrderText() : null;
        FormattedCharSequence reaction = packet.reaction() == null ? null
                : Component.literal(ElementPalette.reaction(packet.reaction())).getVisualOrderText();
        numbers.add(new NumberEntry(packet.targetId(), target.position().add(0, target.getBbHeight() + .15, 0),
                tick, amount, reaction, ElementPalette.color(packet.element()), packet.critical(), HudFormatting.damageLane(sequence++)));
    }
    public static void accept(TargetAuraPayload packet) {
        useLevel(Minecraft.getInstance().level);
        if (packet.elements() == 0) auras.remove(packet.targetId());
        else auras.put(packet.targetId(), packet.elements());
    }
    public static void accept(PlayerCharacterPayload packet) {
        useLevel(Minecraft.getInstance().level);
        if (packet.slot() < 0) playerCharacters.remove(packet.playerId());
        else playerCharacters.put(packet.playerId(), packet.slot());
    }
    public static List<WorldText> extract(float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (!CombatInput.managed() || client.gui.hud.isHidden() || level == null) return List.of();
        if (numbers.isEmpty() && auras.isEmpty() && playerCharacters.isEmpty()) return List.of();
        List<WorldText> texts = new ArrayList<>();
        Font font = client.font;
        for (NumberEntry number : numbers) {
            double progress = Math.clamp((tick - number.bornTick() + partialTick) / LIFETIME_TICKS, 0, 1);
            int alpha = (int) Math.round(255 * (1 - progress));
            if (alpha < 4) continue;
            int color = (number.color() & 0xffffff) | alpha << 24;
            Vec3 position = number.anchor().add(0, .7 * progress, 0);
            if (number.amount() != null) texts.add(new WorldText(position, number.amount(),
                    font.width(number.amount()) / 2F - number.pixelOffset(),
                    color, number.critical() ? .042F : .032F));
            if (number.reaction() != null) texts.add(new WorldText(position.add(0, .9, 0), number.reaction(),
                    font.width(number.reaction()) / 2F, color, .026F));
        }
        for (var entry : auras.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null || !entity.isAlive() || entity.isInvisible()) continue;
            Vec3 position = entity.getPosition(partialTick).add(0, entity.getBbHeight() + .55, 0);
            int count = Integer.bitCount(entry.getValue());
            int index = 0;
            for (Element element : ELEMENTS) {
                if ((entry.getValue() & 1 << element.ordinal()) == 0) continue;
                FormattedCharSequence text = AURA_TEXT[element.ordinal()];
                texts.add(new WorldText(position, text, font.width(text) / 2F - (index++ - (count - 1) / 2F) * 20,
                        ElementPalette.color(element), .025F));
            }
        }
        for (var entry : playerCharacters.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null || entity == client.player || !entity.isAlive() || entity.isInvisible()) continue;
            int slot = entry.getValue();
            var text = CHARACTER_TEXT[slot];
            Element element = CharacterBaseStats.at(CharacterStatePayload.ROSTER.get(slot), 20).element();
            texts.add(new WorldText(entity.getPosition(partialTick).add(0, entity.getBbHeight() + .85, 0),
                    text, font.width(text) / 2F, ElementPalette.color(element), .027F));
        }
        return texts;
    }
    public static void submit(List<WorldText> texts, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (texts == null || texts.isEmpty()) return;
        for (WorldText text : texts) {
            if (text.position().distanceToSqr(camera.pos) > 64 * 64) continue;
            pose.pushPose();
            pose.translate(text.position().x - camera.pos.x, text.position().y - camera.pos.y, text.position().z - camera.pos.z);
            pose.mulPose(camera.orientation);
            pose.scale(text.scale(), -text.scale(), text.scale());
            collector.submitText(pose, -text.halfWidth(), 0, text.text(), true, Font.DisplayMode.NORMAL,
                    0xf000f0, text.color(), 0, 0);
            pose.popPose();
        }
    }
}
