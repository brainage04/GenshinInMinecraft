package io.github.brainage04.genshininminecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import io.github.brainage04.genshininminecraft.network.CombatIntentPayload;
import io.github.brainage04.genshininminecraft.rules.kit.TravelerAnemoKit.Intent;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Client intent edge detection. HP, damage, cooldown and energy never originate here. */
public final class CombatInput {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("genshininminecraft", "genshin"));
    public static final KeyMapping SKILL = new KeyMapping("key.genshininminecraft.skill", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E, CATEGORY);
    public static final KeyMapping BURST = new KeyMapping("key.genshininminecraft.burst", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Q, CATEGORY);
    private static Consumer<CombatIntentPayload> sender;
    private static CharacterStatePayload state = CharacterStatePayload.UNMANAGED;
    private static boolean attackHeld;
    private static boolean skillHeld;
    private CombatInput() {}
    public static void initialize(Consumer<CombatIntentPayload> send) { sender = send; }
    public static void accept(CharacterStatePayload payload) {
        if (payload.managed() != state.managed()) {
            attackHeld = false;
            skillHeld = false;
            drain(SKILL);
            drain(BURST);
        }
        state = payload;
    }
    public static CharacterStatePayload state() { return state; }
    public static boolean managed() { return state.managed() && Minecraft.getInstance().level != null; }
    public static void reset() { state = CharacterStatePayload.UNMANAGED; attackHeld = false; skillHeld = false; }
    private static void send(Intent intent) { if (sender != null) sender.accept(new CombatIntentPayload(intent)); }
    public static void tick(Minecraft client) {
        if (!managed()) {
            drain(SKILL);
            drain(BURST);
            attackHeld = false;
            skillHeld = false;
            return;
        }
        if (managed() && (client.gui.screen() != null || client.player == null || !client.player.isAlive())) {
            if (attackHeld) send(Intent.ATTACK_RELEASE);
            if (skillHeld) send(Intent.SKILL_RELEASE);
            attackHeld = false;
            skillHeld = false;
        }
    }
    /** Runs before vanilla inventory/drop/attack handling, on both loaders. */
    public static void beforeKeybinds(Minecraft client) {
        if (!managed() || client.player == null || client.gui.screen() != null) return;
        boolean attackClick = drain(client.options.keyAttack);
        boolean attackDown = client.options.keyAttack.isDown();
        if (!attackHeld && (attackDown || attackClick)) send(Intent.ATTACK_PRESS);
        if ((attackHeld || attackClick) && !attackDown) send(Intent.ATTACK_RELEASE);
        attackHeld = attackDown;
        boolean skillClick = drain(SKILL);
        boolean skillDown = SKILL.isDown();
        if (!skillHeld && (skillDown || skillClick)) send(Intent.SKILL_PRESS);
        if ((skillHeld || skillClick) && !skillDown) send(Intent.SKILL_RELEASE);
        skillHeld = skillDown;
        if (drain(BURST)) send(Intent.BURST_PRESS);
        if (client.options.keyInventory.same(SKILL) || client.options.keyInventory.same(BURST)) drain(client.options.keyInventory);
        if (client.options.keyDrop.same(SKILL) || client.options.keyDrop.same(BURST)) drain(client.options.keyDrop);
    }
    private static boolean drain(KeyMapping mapping) {
        boolean clicked = false;
        while (mapping.consumeClick()) clicked = true;
        return clicked;
    }
}
