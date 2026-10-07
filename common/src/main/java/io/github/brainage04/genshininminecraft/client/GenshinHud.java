package io.github.brainage04.genshininminecraft.client;

import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Stamina;
import io.github.brainage04.genshininminecraft.rules.CharacterBaseStats;
import io.github.brainage04.genshininminecraft.ui.HudFormatting;
import io.github.brainage04.genshininminecraft.ui.ElementPalette;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;

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
    private static final int SWEEP_STEPS = 64;
    private static final int[][] SWEEP_RUNS = new int[SWEEP_STEPS + 1][];
    private static final int[] RING_X = new int[STAMINA_SEGMENTS];
    private static final int[] RING_Y = new int[STAMINA_SEGMENTS];
    static {
        for (int index = 0; index < STAMINA_SEGMENTS; index++) {
            double angle = Math.toRadians(-135 + index * 270.0 / (STAMINA_SEGMENTS - 1));
            STAMINA_X[index] = (int) Math.round(14 * Math.cos(angle));
            STAMINA_Y[index] = (int) Math.round(14 * Math.sin(angle));
            double ringAngle = -Math.PI / 2 + index * 2 * Math.PI / STAMINA_SEGMENTS;
            RING_X[index] = (int) Math.round(18 * Math.cos(ringAngle));
            RING_Y[index] = (int) Math.round(18 * Math.sin(ringAngle));
        }
        // Rasterize original circular sectors once; rendering submits scanlines, not per-pixel trigonometry.
        for (int step = 0; step <= SWEEP_STEPS; step++) {
            int[] runs = new int[36 * 6];
            int count = 0;
            for (int y = -18; y < 18; y++) {
                int start = -19;
                for (int x = -18; x <= 18; x++) {
                    boolean covered = x < 18 && (x + .5) * (x + .5) + (y + .5) * (y + .5) <= 18 * 18
                            && HudFormatting.radialCovered(x + .5, y + .5, step / (double) SWEEP_STEPS);
                    if (covered && start == -19) start = x;
                    if (!covered && start != -19) {
                        runs[count++] = start;
                        runs[count++] = y;
                        runs[count++] = x;
                        start = -19;
                    }
                }
            }
            SWEEP_RUNS[step] = java.util.Arrays.copyOf(runs, count);
        }
    }
    private static List<PartyMember> party = List.of();
    private static final String[] PARTY_NAMES = {"Traveler", "Amber", "Kaeya", "Lisa"};
    private static final float[] BURST_COSTS = {60, 40, 60, 80};
    private static String hpText = "";
    private static String skillCooldown = "";
    private static String burstCooldown = "";
    private static String traversalHint = "";
    private static String switchCooldown = "";
    private static int rejectionSerial;
    private static int feedbackTicks;
    private static int flashIntent;
    private static String rejectionText = "";

    public record PartyMember(String name, Element element, float hp, float maxHp, float energy,
            float maxEnergy, boolean active) {}

    private GenshinHud() {}
    public static List<PartyMember> party() { return party; }
    public static boolean partyUnavailable(int slot) {
        var member = party.get(slot);
        var state = CombatInput.state();
        return HudFormatting.partyUnavailable(member.active(), member.hp(), state.switchRemainingFrames(), state.switchBlocked());
    }
    public static double skillSweepFraction() {
        var state = CombatInput.state();
        return HudFormatting.sweepFraction(state.skillRemainingFrames(), state.skillCooldownFrames());
    }
    public static double burstSweepFraction() {
        var state = CombatInput.state();
        return HudFormatting.sweepFraction(state.burstRemainingFrames(), state.burstCooldownFrames());
    }
    public static boolean rejectionVisible() { return feedbackTicks > 0; }
    public static String rejectionText() { return rejectionText; }
    public static void tick() { if (feedbackTicks > 0) feedbackTicks--; }
    public static void accept(CharacterStatePayload state) {
        if (!state.managed()) {
            party = List.of();
            rejectionSerial = 0;
            feedbackTicks = 0;
            rejectionText = "";
        }
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
        switchCooldown = HudFormatting.cooldown(state.switchRemainingFrames());
        if (state.managed() && state.rejection() == CharacterStatePayload.Rejection.NONE) {
            // Reconnect starts a new server-session serial; its baseline is not a rejected input.
            rejectionSerial = state.rejectionSerial();
            feedbackTicks = 0;
            rejectionText = "";
        } else if (state.managed() && state.rejectionSerial() != rejectionSerial) {
            rejectionSerial = state.rejectionSerial();
            flashIntent = state.rejectedIntent();
            feedbackTicks = 16; // Named .8-second adaptation, not a measured Genshin timing.
            boolean normal = flashIntent == Intent.ATTACK_PRESS.ordinal() + 1;
            rejectionText = normal ? "" : Component.translatable("hud.genshininminecraft.rejection."
                    + state.rejection().name().toLowerCase(java.util.Locale.ROOT)).getString();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), .7F, .25F));
        }
        traversalHint = "";
        if (state.climbing() || state.gliding()) {
            var options = Minecraft.getInstance().options;
            String jump = options.keyJump.getTranslatedKeyMessage().getString();
            String drop = options.keyShift.getTranslatedKeyMessage().getString();
            traversalHint = state.climbing() ? "CLIMB  WASD | " + jump + " jump | S+" + jump + " away | " + drop + " drop"
                    : "GLIDE  WASD steer | " + jump + " close";
        }
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
            boolean unavailable = partyUnavailable(index);
            boolean fallen = member.hp() <= 0;
            int color = unavailable ? 0xff89929f : ElementPalette.color(member.element());
            graphics.text(font, member.name(), x + 7, y + 4, color);
            bar(graphics, x + 7, y + 23, 73, 3, member.energy(), member.maxEnergy(), color);
            if (unavailable) {
                double covered = fallen || CombatInput.state().switchBlocked() && switchCooldown.isEmpty()
                        ? 1 : HudFormatting.sweepFraction(CombatInput.state().switchRemainingFrames(), 60);
                int bottom = y + (int) Math.ceil((rowHeight - 3) * covered);
                graphics.fill(x, y, width - 10, bottom, 0x8820252d);
                if (!fallen) graphics.centeredText(font, switchCooldown.isEmpty() ? "LOCK" : switchCooldown, x + 95, y + 18, BORDER);
            }
            // Availability greys the row, not its health: retain the green HP information.
            bar(graphics, x + 7, y + 16, 73, 4, member.hp(), member.maxHp(), HP);
            if (fallen) {
                graphics.centeredText(font, "X", x + 95, y + 17, 0xffbbbbbb);
                graphics.outline(x + 88, y + 14, 15, 14, 0xff89929f);
            }
            // Rebound slot keys remain readable above the unavailable sweep.
            graphics.text(font, CombatInput.PARTY[index].getTranslatedKeyMessage().getString(), width - 20, y + 4, BORDER);
            if (feedbackTicks > 0 && flashIntent == Intent.SWITCH_1.ordinal() + index + 1)
                graphics.outline(x, y, 112, rowHeight - 3, 0xfff5f2e9);
            var memberState = CombatInput.state().members().get(index);
            if (!member.active() && !fallen && member.energy() >= member.maxEnergy() && memberState.burstRemainingFrames() == 0)
                graphics.fill(x + 76, y + 4, x + 80, y + 8, ElementPalette.color(member.element()));
        }
        if (active == null) return;
        int hpWidth = Math.min(180, width / 2 - 12);
        int hpX = (width - hpWidth) / 2;
        int hpY = height - 57;
        graphics.fill(hpX - 4, hpY - 3, hpX + hpWidth + 4, hpY + 20, PANEL);
        bar(graphics, hpX, hpY, hpWidth, 7, active.hp(), active.maxHp(), HP);
        graphics.centeredText(font, hpText, width / 2, hpY + 10, 0xfff5f2e9);
        icon(graphics, font, width - 106, height - 63, CombatInput.SKILL.getTranslatedKeyMessage().getString(),
                skillCooldown, 1, false, active.element(), skillSweepFraction(), Intent.SKILL_PRESS);
        boolean full = HudFormatting.fraction(active.energy(), active.maxEnergy()) == 1;
        boolean ready = full && CombatInput.state().burstRemainingFrames() == 0;
        icon(graphics, font, width - 57, height - 63, CombatInput.BURST.getTranslatedKeyMessage().getString(),
                burstCooldown, HudFormatting.fraction(active.energy(), active.maxEnergy()), ready, active.element(),
                burstSweepFraction(), Intent.BURST_PRESS);
        if (feedbackTicks > 0 && !rejectionText.isEmpty())
            graphics.centeredText(font, rejectionText, width / 2, height / 4, 0xfff5f2e9);
        staminaWheel(graphics, width / 2 + 35, height / 2);
        if (!traversalHint.isEmpty()) graphics.centeredText(font, traversalHint, width / 2, height - 87, BORDER);
        if (CombatInput.aiming()) {
            int x = width / 2, y = height / 2;
            if (ManagedCamera.decoupled()) {
                var camera = client.gameRenderer.mainCamera();
                var point = ManagedCamera.aimPoint(delta.getGameTimeDeltaPartialTick(false)).subtract(camera.position());
                var forward = camera.forwardVector();
                double depth = point.x * forward.x() + point.y * forward.y() + point.z * forward.z();
                if (depth > .05) {
                    double focal = height / (2 * Math.tan(Math.toRadians(camera.getFov()) / 2));
                    var left = camera.leftVector();
                    var up = camera.upVector();
                    x -= (int) Math.round((point.x * left.x() + point.y * left.y() + point.z * left.z()) * focal / depth);
                    y -= (int) Math.round((point.x * up.x() + point.y * up.y() + point.z * up.z()) * focal / depth);
                }
            }
            int color = CombatInput.fullyChargedAim() ? ElementPalette.color(Element.PYRO) : BORDER;
            graphics.outline(x - 9, y - 9, 19, 19, color);
            graphics.fill(x - 1, y - 15, x + 1, y - 11, color);
            graphics.fill(x - 1, y + 11, x + 1, y + 15, color);
        }
    }

    public static boolean staminaVisible() {
        var state = CombatInput.state();
        return state.managed() && (state.stamina() < Stamina.NEW_PLAYER_MAX || state.staminaDraining()
                || state.climbing() || state.gliding());
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
            String cooldown, double energyFraction, boolean ready, Element element, double sweep, Intent intent) {
        int color = ElementPalette.color(element);
        graphics.fill(x, y, x + 40, y + 40, PANEL);
        graphics.outline(x, y, 40, 40, ready ? 0xffffe5a0 : EMPTY);
        if (ready) graphics.outline(x - 1, y - 1, 42, 42, 0x88ffe5a0);
        // Original diamond fills bottom-up; an independent ring makes partial burst energy readable.
        int energyThreshold = 8 - (int) Math.ceil(15 * energyFraction);
        for (int offset = -7; offset <= 7; offset++) {
            int halfWidth = 7 - Math.abs(offset);
            int tint = offset >= energyThreshold ? color : 0xff89929f;
            graphics.horizontalLine(x + 20 - halfWidth, x + 20 + halfWidth, y + 20 + offset, tint);
        }
        if (intent == Intent.BURST_PRESS) {
            int filled = (int) Math.floor(STAMINA_SEGMENTS * energyFraction);
            for (int index = 0; index < STAMINA_SEGMENTS; index++) {
                int px = x + 20 + RING_X[index], py = y + 20 + RING_Y[index];
                graphics.fill(px - 1, py - 1, px + 1, py + 1, index < filled ? color : EMPTY);
            }
        }
        if (CombatInput.state().actionBlocked()) graphics.fill(x + 2, y + 2, x + 38, y + 38, 0x6620252d);
        int step = (int) Math.ceil(SWEEP_STEPS * sweep);
        int[] runs = SWEEP_RUNS[step];
        for (int index = 0; index < runs.length; index += 3)
            graphics.fill(x + 20 + runs[index], y + 20 + runs[index + 1],
                    x + 20 + runs[index + 2], y + 21 + runs[index + 1], 0xbb20252d);
        if (!cooldown.isEmpty()) graphics.centeredText(font, cooldown, x + 20, y + 16, 0xfff5f2e9);
        graphics.centeredText(font, key, x + 20, y + 43, 0xfff5f2e9);
        if (feedbackTicks > 0 && flashIntent == intent.ordinal() + 1)
            graphics.outline(x, y, 40, 40, 0xfff5f2e9);
    }
}
