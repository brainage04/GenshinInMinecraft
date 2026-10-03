package io.github.brainage04.genshininminecraft.client;

import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Stamina;
import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.ui.HudFormatting;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Shared client-only drawing. Loaders own layer registration and vanilla-layer suppression. */
public final class GenshinHud {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("genshininminecraft", "combat_hud");
    private static final int PANEL = 0xb5222833;
    private static final int BORDER = 0xffd5c9a5;
    private static final int HP = 0xff8bd76c;
    private static final int EMPTY = 0xff414956;
    private static final int STAMINA_GOLD = 0xffffd76c;
    private static final int STAMINA_RED = 0xffff5555;
    private static final int STAMINA_SEGMENTS = 48;
    private static final int[] STAMINA_X = new int[STAMINA_SEGMENTS];
    private static final int[] STAMINA_Y = new int[STAMINA_SEGMENTS];
    static {
        for (int index = 0; index < STAMINA_SEGMENTS; index++) {
            double angle = Math.toRadians(-135 + index * 270.0 / (STAMINA_SEGMENTS - 1));
            STAMINA_X[index] = (int) Math.round(14 * Math.cos(angle));
            STAMINA_Y[index] = (int) Math.round(14 * Math.sin(angle));
        }
    }
    private static List<PartyMember> party = List.of();
    private static final String[] PARTY_NAMES = {"Traveler", "Amber", "Kaeya", "Lisa"};
    private static final float[] BURST_COSTS = {60, 40, 60, 80};
    private static String hpText = "";
    private static String skillCooldown = "";
    private static String burstCooldown = "";

    public record PartyMember(String name, Element element, float hp, float maxHp, float energy,
            float maxEnergy, boolean active) {}

    private GenshinHud() {}
    public static List<PartyMember> party() { return party; }
    public static void accept(CharacterStatePayload state) {
        if (!state.managed()) party = List.of();
        else {
            var rows = new java.util.ArrayList<PartyMember>(4);
            for (int index = 0; index < state.members().size(); index++) {
                var base = CharacterBaseStats.at(CharacterStatePayload.ROSTER.get(index), 20);
                var member = state.members().get(index);
                rows.add(new PartyMember(PARTY_NAMES[index], base.element(), member.hpFraction() * (float) base.hp(),
                        (float) base.hp(), member.energy(), BURST_COSTS[index], index == state.activeSlot()));
            }
            party = List.copyOf(rows);
        }
        hpText = Math.round(state.hp()) + " / " + Math.round(state.maxHp());
        skillCooldown = HudFormatting.cooldown(state.skillRemainingFrames());
        burstCooldown = HudFormatting.cooldown(state.burstRemainingFrames());
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (!CombatInput.managed() || client.gui.hud.isHidden() || client.player == null) return;
        Font font = client.font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int rowHeight = Math.min(34, Math.max(24, (height - 90) / 4));
        PartyMember active = null;
        for (int index = 0; index < Math.min(4, party.size()); index++) {
            PartyMember member = party.get(index);
            int x = width - 122;
            int y = 18 + index * rowHeight;
            graphics.fill(x, y, width - 10, y + rowHeight - 3, PANEL);
            if (member.active()) {
                active = member;
                graphics.outline(x, y, 112, rowHeight - 3, BORDER);
                graphics.fill(x, y, x + 3, y + rowHeight - 3, ElementPalette.color(member.element()));
            }
            graphics.text(font, member.name(), x + 7, y + 4, ElementPalette.color(member.element()));
            graphics.text(font, CombatInput.PARTY[index].getTranslatedKeyMessage().getString(), width - 20, y + 4, BORDER);
            bar(graphics, x + 7, y + 16, 88, 4, member.hp(), member.maxHp(), HP);
            bar(graphics, x + 7, y + 23, 88, 3, member.energy(), member.maxEnergy(), ElementPalette.color(member.element()));
        }
        if (active == null) return;
        int hpWidth = Math.min(180, width / 2 - 12);
        int hpX = (width - hpWidth) / 2;
        int hpY = height - 57;
        graphics.fill(hpX - 4, hpY - 3, hpX + hpWidth + 4, hpY + 20, PANEL);
        bar(graphics, hpX, hpY, hpWidth, 7, active.hp(), active.maxHp(), HP);
        graphics.centeredText(font, hpText, width / 2, hpY + 10, 0xfff5f2e9);
        boolean available = CombatInput.state().activeSlot() != 3;
        icon(graphics, font, width - 106, height - 63, CombatInput.SKILL.getTranslatedKeyMessage().getString(),
                available ? skillCooldown : "SOON", available, false, active.element());
        boolean full = HudFormatting.fraction(active.energy(), active.maxEnergy()) == 1;
        boolean ready = available && full && CombatInput.state().burstRemainingFrames() == 0;
        icon(graphics, font, width - 57, height - 63, CombatInput.BURST.getTranslatedKeyMessage().getString(),
                available ? burstCooldown : "SOON", available && full, ready, active.element());
        bar(graphics, width - 53, height - 29, 32, 3, active.energy(), active.maxEnergy(), ElementPalette.color(active.element()));
        staminaWheel(graphics, width / 2 + 35, height / 2);
        if (CombatInput.aiming()) {
            int x = width / 2, y = height / 2;
            int color = CombatInput.fullyChargedAim() ? ElementPalette.color(Element.PYRO) : BORDER;
            graphics.outline(x - 9, y - 9, 19, 19, color);
            graphics.fill(x - 1, y - 15, x + 1, y - 11, color);
            graphics.fill(x - 1, y + 11, x + 1, y + 15, color);
        }
    }

    public static boolean staminaVisible() {
        var state = CombatInput.state();
        return state.managed() && (state.stamina() < Stamina.NEW_PLAYER_MAX || state.staminaDraining());
    }
    private static void staminaWheel(GuiGraphicsExtractor graphics, int x, int y) {
        if (!staminaVisible()) return;
        var state = CombatInput.state();
        int filled = (int) Math.ceil(STAMINA_SEGMENTS * HudFormatting.fraction(state.stamina(), Stamina.NEW_PLAYER_MAX));
        int color = state.staminaExhausted() ? STAMINA_RED : STAMINA_GOLD;
        for (int index = 0; index < STAMINA_SEGMENTS; index++) {
            int px = x + STAMINA_X[index], py = y + STAMINA_Y[index];
            graphics.fill(px - 1, py - 1, px + 2, py + 2, index < filled ? color : EMPTY);
        }
        // A tiny red core keeps a completely depleted wheel visibly exhausted, not just grey.
        if (state.staminaExhausted()) graphics.fill(x - 2, y - 2, x + 3, y + 3, STAMINA_RED);
    }

    private static void bar(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
            double current, double maximum, int color) {
        graphics.fill(x, y, x + width, y + height, EMPTY);
        int filled = (int) Math.round(width * HudFormatting.fraction(current, maximum));
        if (filled > 0) graphics.fill(x, y, x + filled, y + height, color);
    }
    private static void icon(GuiGraphicsExtractor graphics, Font font, int x, int y, String key,
            String cooldown, boolean energized, boolean ready, Element element) {
        int color = energized ? ElementPalette.color(element) : 0xff89929f;
        graphics.fill(x, y, x + 40, y + 40, PANEL);
        graphics.outline(x, y, 40, 40, ready ? 0xffffe5a0 : color);
        // An original, stepped diamond; E/Q labels make the two abilities unambiguous.
        for (int offset = -7; offset <= 7; offset++) {
            int halfWidth = 7 - Math.abs(offset);
            graphics.horizontalLine(x + 20 - halfWidth, x + 20 + halfWidth, y + 12 + offset, color);
        }
        graphics.centeredText(font, key, x + 20, y + 23, 0xfff5f2e9);
        if (!cooldown.isEmpty()) {
            graphics.fill(x + 1, y + 1, x + 39, y + 20, 0xbb222833);
            graphics.centeredText(font, cooldown, x + 20, y + 8, 0xfff5f2e9);
        }
        if (ready) graphics.centeredText(font, "READY", x + 20, y + 44, 0xffffe5a0);
    }
}
