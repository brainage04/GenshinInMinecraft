package io.github.brainage04.genshininminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.brainage04.genshininminecraft.network.DamageNumberPayload;
import io.github.brainage04.genshininminecraft.network.TargetAuraPayload;
import io.github.brainage04.genshininminecraft.network.PlayerCharacterPayload;
import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import io.github.brainage04.genshininminecraft.client.character.PlayerVisuals;
import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.rules.HilichurlProfile;
import java.util.ArrayList;
import io.github.brainage04.genshininminecraft.ui.DamageNumberAnimation;
import io.github.brainage04.genshininminecraft.ui.ElementPalette;
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
    public static final int LIFETIME_TICKS = (int) (DamageNumberAnimation.LIFETIME_SECONDS * 20);
    private static final Element[] ELEMENTS = Element.values();
    private static final FormattedCharSequence[] AURA_TEXT = new FormattedCharSequence[ELEMENTS.length];
    private static final FormattedCharSequence[] ENEMY_LEVEL_TEXT = new FormattedCharSequence[HilichurlProfile.MAX_LEVEL + 1];
    static {
        for (int enemyLevel = HilichurlProfile.MIN_LEVEL; enemyLevel <= HilichurlProfile.MAX_LEVEL; enemyLevel++)
            ENEMY_LEVEL_TEXT[enemyLevel] = Component.literal("Lv. " + enemyLevel + " · Hilichurl").getVisualOrderText();
        for (Element element : ELEMENTS) AURA_TEXT[element.ordinal()] =
                Component.literal("[" + ElementPalette.symbol(element) + "]").getVisualOrderText();
    }
    private static final List<NumberEntry> numbers = new ArrayList<>();
    private static final Map<Integer, Integer> auras = new HashMap<>();
    private static final Map<Integer, Integer> conductive = new HashMap<>();
    private static final FormattedCharSequence[] CONDUCTIVE_TEXT = {
        null, Component.literal("•").getVisualOrderText(), Component.literal("••").getVisualOrderText(),
        Component.literal("•••").getVisualOrderText()
    };
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
            FormattedCharSequence reaction, int color, int reactionColor, boolean critical, int slot,
            double drift, float amountHalfWidth, float reactionHalfWidth) {}
    public record WorldText(Vec3 position, FormattedCharSequence text, float halfWidth, int color,
            float scale, float emphasis, float pixelX, float pixelY) {}

    private CombatFeedback() {}
    public static boolean hasDamageNumber(int targetId) {
        return numbers.stream().anyMatch(number -> number.targetId() == targetId && number.amount() != null);
    }
    public static List<NumberEntry> damageNumbers() { return List.copyOf(numbers); }
    public static int auraElements(int targetId) { return auras.getOrDefault(targetId, 0); }
    public static int conductiveStacks(int targetId) { return conductive.getOrDefault(targetId, 0); }
    public static void reset() { numbers.clear(); auras.clear(); conductive.clear(); PlayerVisuals.reset(); level = null; tick = 0; sequence = 0; }
    private static void useLevel(ClientLevel current) {
        if (level != current) {
            reset();
            level = current;
        }
    }
    public static void tick(Minecraft client) {
        useLevel(client.level);
        tick = level == null ? 0 : level.getGameTime();
        numbers.removeIf(number -> tick - number.bornTick() >= LIFETIME_TICKS);
        if (level != null) auras.keySet().removeIf(id -> level.getEntity(id) == null);
        if (level != null) conductive.keySet().removeIf(id -> level.getEntity(id) == null);
        PlayerVisuals.tick(client);
    }
    public static void accept(DamageNumberPayload packet) {
        ClientLevel current = Minecraft.getInstance().level;
        useLevel(current);
        if (current == null) return;
        Entity target = current.getEntity(packet.targetId());
        if (target == null) return;
        var client = Minecraft.getInstance();
        FormattedCharSequence amount = packet.amount() > 0
                ? Component.literal(Long.toString(Math.round(packet.amount())))
                        .withStyle(packet.critical() ? net.minecraft.ChatFormatting.BOLD : net.minecraft.ChatFormatting.RESET)
                        .getVisualOrderText() : null;
        FormattedCharSequence reaction = packet.reaction() == null ? null
                : Component.literal(ElementPalette.reaction(packet.reaction())).getVisualOrderText();
        int slot = 0;
        // Reuse only unoccupied slots on this target, not a global modulo shared by unrelated hits.
        boolean occupied;
        do {
            occupied = false;
            for (NumberEntry number : numbers) if (number.targetId() == packet.targetId() && number.slot() == slot
                    && current.getGameTime() - number.bornTick() < LIFETIME_TICKS) {
                occupied = true;
                slot++;
                break;
            }
        } while (occupied);
        numbers.add(new NumberEntry(packet.targetId(), target.position().add(0, target.getBbHeight() + .65, 0),
                current.getGameTime(), amount, reaction, ElementPalette.color(packet.element()),
                packet.reaction() == null ? 0 : ElementPalette.reactionColor(packet.reaction()), packet.critical(), slot,
                DamageNumberAnimation.drift(sequence++), amount == null ? 0 : client.font.width(amount) / 2F,
                reaction == null ? 0 : client.font.width(reaction) / 2F));
    }
    public static void accept(TargetAuraPayload packet) {
        useLevel(Minecraft.getInstance().level);
        if (packet.elements() == 0) auras.remove(packet.targetId());
        else auras.put(packet.targetId(), packet.elements());
        if (packet.conductiveStacks() == 0) conductive.remove(packet.targetId());
        else conductive.put(packet.targetId(), packet.conductiveStacks());
    }
    public static void accept(PlayerCharacterPayload packet) {
        useLevel(Minecraft.getInstance().level);
        PlayerVisuals.accept(packet);
    }
    public static List<WorldText> extract(float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (!CombatInput.managed() || client.gui.hud.isHidden() || level == null) return List.of();
        List<WorldText> texts = new ArrayList<>();
        Font font = client.font;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Hilichurl hilichurl) || !entity.isAlive() || entity.isInvisible()) continue;
            var text = ENEMY_LEVEL_TEXT[hilichurl.genshinLevel()];
            texts.add(new WorldText(entity.getPosition(partialTick).add(0, entity.getBbHeight() + .2, 0),
                    text, font.width(text) / 2F, 0xffeeeeee, .025F, 1, 0, 0));
        }
        double partial = level.tickRateManager().isFrozen() ? 0 : partialTick;
        var camera = client.gameRenderer.mainCamera().position();
        for (NumberEntry number : numbers) {
            double age = Math.max(0, (level.getGameTime() - number.bornTick() + partial) / 20);
            int alpha = (int) Math.round(255 * DamageNumberAnimation.alpha(age));
            if (alpha < 4) continue;
            float pop = (float) DamageNumberAnimation.pop(age);
            float scale = DamageNumberAnimation.distanceScale(number.anchor().distanceTo(camera));
            float x = (float) (DamageNumberAnimation.stackX(number.slot()) + DamageNumberAnimation.horizontal(age, number.drift()));
            float y = (float) (DamageNumberAnimation.stackY(number.slot()) - DamageNumberAnimation.rise(age));
            if (number.amount() != null) texts.add(new WorldText(number.anchor(), number.amount(), number.amountHalfWidth(),
                    (number.color() & 0xffffff) | alpha << 24, scale,
                    pop * (float) (number.critical() ? DamageNumberAnimation.CRITICAL_SCALE : 1), x, y));
            if (number.reaction() != null) texts.add(new WorldText(number.anchor(), number.reaction(), number.reactionHalfWidth(),
                    (number.reactionColor() & 0xffffff) | alpha << 24, scale,
                    pop * (float) DamageNumberAnimation.REACTION_SCALE, x, y + (float) DamageNumberAnimation.REACTION_Y));
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
                        ElementPalette.color(element), .025F, 1, 0, 0));
            }
        }
        for (var entry : conductive.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null || !entity.isAlive() || entity.isInvisible()) continue;
            var text = CONDUCTIVE_TEXT[entry.getValue()];
            texts.add(new WorldText(entity.getPosition(partialTick).add(0, entity.getBbHeight() + .9, 0),
                    text, font.width(text) / 2F, ElementPalette.color(Element.ELECTRO), .025F, 1, 0, 0));
        }
        for (var snapshot : PlayerVisuals.snapshots()) {
            Entity entity = level.getEntity(snapshot.playerId());
            if (entity == null || entity == client.player || !entity.isAlive() || entity.isInvisible()) continue;
            int slot = snapshot.slot();
            var text = CHARACTER_TEXT[slot];
            Element element = CharacterBaseStats.at(CharacterStatePayload.ROSTER.get(slot), 20).element();
            texts.add(new WorldText(entity.getPosition(partialTick).add(0, entity.getBbHeight() + .85, 0),
                    text, font.width(text) / 2F, ElementPalette.color(element), .027F, 1, 0, 0));
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
            pose.translate(text.pixelX(), text.pixelY(), 0);
            pose.scale(text.emphasis(), text.emphasis(), text.emphasis());
            collector.submitText(pose, -text.halfWidth(), 0, text.text(), true, Font.DisplayMode.NORMAL,
                    0xf000f0, text.color(), 0, 0);
            pose.popPose();
        }
    }
}
