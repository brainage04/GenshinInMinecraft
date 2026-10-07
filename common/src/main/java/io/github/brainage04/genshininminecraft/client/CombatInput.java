package io.github.brainage04.genshininminecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import io.github.brainage04.genshininminecraft.network.CombatIntentPayload;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
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
    public static final KeyMapping MAP = new KeyMapping("key.genshininminecraft.teleport", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
    public static final KeyMapping[] PARTY = {
        new KeyMapping("key.genshininminecraft.party1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_1, CATEGORY),
        new KeyMapping("key.genshininminecraft.party2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_2, CATEGORY),
        new KeyMapping("key.genshininminecraft.party3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_3, CATEGORY),
        new KeyMapping("key.genshininminecraft.party4", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_4, CATEGORY)
    };
    private static final Intent[] SWITCH_INTENTS = {Intent.SWITCH_1, Intent.SWITCH_2, Intent.SWITCH_3, Intent.SWITCH_4};
    private static Consumer<CombatIntentPayload> sender;
    private static CharacterStatePayload state = CharacterStatePayload.UNMANAGED;
    private static boolean attackHeld;
    private static boolean skillHeld;
    private static boolean jumpHeld;
    public static final float ADAPTED_AIM_FOV_MULTIPLIER = .7F;
    private static int aimTicks;
    private static boolean aimCancelled;
    private CombatInput() {}
    public static void initialize(Consumer<CombatIntentPayload> send) { sender = send; }
    public static void accept(CharacterStatePayload payload) {
        if (payload.activeSlot() != state.activeSlot()) { aimTicks = 0; aimCancelled = false; }
        if (payload.managed() != state.managed()) {
            attackHeld = false;
            skillHeld = false;
            jumpHeld = false;
            drain(SKILL);
            drain(BURST);
            for (var mapping : PARTY) drain(mapping);
            drain(MAP);
        }
        state = payload;
        ManagedCamera.tick(Minecraft.getInstance());
        GenshinHud.accept(payload);
        if (!payload.managed()) CombatFeedback.reset();
    }
    public static CharacterStatePayload state() { return state; }
    public static boolean managed() { return state.managed() && Minecraft.getInstance().level != null; }
    public static boolean aiming() {
        Minecraft client = Minecraft.getInstance();
        return managed() && state.activeSlot() == 1 && attackHeld && aimTicks >= 3 && !aimCancelled
                && client.player != null && client.player.isAlive() && client.gui.screen() == null;
    }
    public static boolean fullyChargedAim() { return aiming() && aimTicks >= 29; }
    public static void reset() {
        state = CharacterStatePayload.UNMANAGED;
        attackHeld = false;
        skillHeld = false;
        jumpHeld = false;
        aimTicks = 0;
        aimCancelled = false;
        GenshinHud.accept(state);
        CombatFeedback.reset();
        ManagedCamera.reset();
    }
    private static void send(Intent intent) {
        if (sender == null) return;
        if (intent == Intent.ATTACK_PRESS || intent == Intent.SKILL_PRESS || intent == Intent.BURST_PRESS
                || intent == Intent.ATTACK_RELEASE && aiming()) ManagedCamera.beforeAction();
        sender.accept(new CombatIntentPayload(intent));
    }
    public static void tick(Minecraft client) {
        CombatFeedback.tick(client);
        GenshinHud.tick();
        if (attackHeld) aimTicks++;
        else { aimTicks = 0; aimCancelled = false; }
        if (aiming() && (client.options.keySprint.isDown() || client.options.keyJump.isDown())) aimCancelled = true;
        ManagedCamera.tick(client);
        if (!managed()) {
            drain(SKILL);
            drain(BURST);
            drain(MAP);
            for (var mapping : PARTY) drain(mapping);
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
        if (drain(MAP)) TeleportScreen.requestList();
        boolean jumpDown = client.options.keyJump.isDown();
        boolean jumpClick = drain(client.options.keyJump);
        if (!jumpHeld && (jumpDown || jumpClick)
                && (!client.player.onGround() || state.climbing() || state.gliding())) send(Intent.TRAVERSAL_JUMP);
        jumpHeld = jumpDown;
        while (client.options.keyTogglePerspective.consumeClick()) ManagedCamera.togglePerspective(client);
        for (int slot = 0; slot < PARTY.length; slot++) {
            if (drain(PARTY[slot])) send(SWITCH_INTENTS[slot]);
            for (var hotbar : client.options.keyHotbarSlots) if (hotbar.same(PARTY[slot])) drain(hotbar);
        }
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
