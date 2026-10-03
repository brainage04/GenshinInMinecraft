package io.github.brainage04.genshininminecraft.client;

import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.kit.TravelerAnemoKit;
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
    private static List<PartyMember> party = List.of();
    private static final String[] PARTY_KEYS = {"1", "2", "3", "4"};
    private static String hpText = "";
    private static String skillCooldown = "";
    private static String burstCooldown = "";

    public record PartyMember(String name, Element element, float hp, float maxHp, float energy,
            float maxEnergy, boolean active) {}

    private GenshinHud() {}
    public static List<PartyMember> party() { return party; }
    public static void accept(CharacterStatePayload state) {
        party = state.managed() ? List.of(new PartyMember("Traveler", Element.ANEMO, state.hp(),
                state.maxHp(), state.energy(), (float) TravelerAnemoKit.BURST_COST, true)) : List.of();
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
            graphics.text(font, PARTY_KEYS[index], width - 20, y + 4, BORDER);
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
        icon(graphics, font, width - 106, height - 63, CombatInput.SKILL.getTranslatedKeyMessage().getString(),
                skillCooldown, true, false, active.element());
        boolean full = HudFormatting.fraction(active.energy(), active.maxEnergy()) == 1;
        boolean ready = full && CombatInput.state().burstRemainingFrames() == 0;
        icon(graphics, font, width - 57, height - 63, CombatInput.BURST.getTranslatedKeyMessage().getString(),
                burstCooldown, full, ready, active.element());
        bar(graphics, width - 53, height - 29, 32, 3, active.energy(), active.maxEnergy(), ElementPalette.color(active.element()));
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
