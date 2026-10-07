package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.client.*;
import io.github.brainage04.genshininminecraft.client.character.*;
import io.github.brainage04.genshininminecraft.network.CombatIntentPayload;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** Two independent Fabric client GameTest JVMs join one external dedicated development server. */
final class CoopSmokeClient {
    private CoopSmokeClient() {}
    private static String phase(Path root) {
        try {
            if (Files.exists(root.resolve("failure.txt"))) throw new AssertionError(Files.readString(root.resolve("failure.txt")));
            return Files.exists(root.resolve("phase")) ? Files.readString(root.resolve("phase")) : "joining";
        } catch (java.io.IOException exception) { throw new AssertionError(exception); }
    }
    private static void await(ClientGameTestContext context, Path root, String expected) {
        context.waitFor(client -> phase(root).equals(expected), 6000);
    }
    private static void ack(Path root, String name) {
        try { Files.writeString(root.resolve(name), "ok\n"); } catch (java.io.IOException exception) { throw new AssertionError(exception); }
    }
    private static void intent(ClientGameTestContext context, Intent intent) {
        context.runOnClient(client -> ClientPlayNetworking.send(new CombatIntentPayload(intent)));
    }
    static void run(ClientGameTestContext context) {
        Path root = Path.of(System.getProperty("genshin.coopSmoke"));
        String name = System.getProperty("genshin.coopSmoke.name");
        try { Files.writeString(root.resolve(name + "-display"), System.getenv("DISPLAY")); }
        catch (java.io.IOException exception) { throw new AssertionError(exception); }
        boolean host = name.equals("CoopHost");
        String address = "127.0.0.1:" + System.getProperty("genshin.coopSmoke.port");
        context.runOnClient(client -> ConnectScreen.startConnecting(new TitleScreen(), client, ServerAddress.parseString(address),
                new ServerData("Private two-client co-op smoke", address, ServerData.Type.OTHER), false, null));
        context.waitFor(client -> client.level != null && client.player != null && CombatInput.managed(), 6000);
        await(context, root, "models");
        int remoteSlot = host ? 3 : 2;
        context.waitFor(client -> client.level.players().stream().anyMatch(player -> player != client.player
                && PlayerVisuals.usesCharacter(player) && PlayerVisuals.snapshot(player.getId()).slot() == remoteSlot)
                && GenshinHud.coop().teammates().size() == 1, 1200);
        context.runOnClient(client -> {
            var remote = client.level.players().stream().filter(player -> player != client.player).findFirst().orElseThrow();
            var state = client.getEntityRenderDispatcher().getRenderer(remote).createRenderState(remote, 0);
            var geo = ((CharacterAvatarState) state).genshin$characterState();
            if (geo == null || geo.input().slot() != remoteSlot) throw new AssertionError("Real remote player must bake its active GeckoLib character");
            var teammate = GenshinHud.coop().teammates().getFirst();
            if (teammate.slot() != remoteSlot || teammate.hp() != teammate.maxHp()) throw new AssertionError("Co-op list must show remote active character + real full HP");
            if (Integer.bitCount(GenshinHud.coop().allocatedMask()) != 2) throw new AssertionError("Own HUD allocation must have two slots");
            client.options.renderDistance().set(5);
            client.gui.hud.getChat().clearMessages(true); ManagedCamera.setAngles(host ? -15 : 15, 15);
        });
        context.waitTicks(20);
        context.runOnClient(client -> client.gui.toastManager().clear());
        context.waitTicks(2);
        Path screenshot = context.takeScreenshot(TestScreenshotOptions.of("coop-" + (host ? "host-sees-lisa" : "guest-sees-kaeya"))
                .disableCounterPrefix().withDestinationDir(root.resolve("screenshots")));
        if (!Files.isRegularFile(screenshot)) throw new AssertionError("Remote model screenshot missing");
        ack(root, name + "-model");
        if (host) {
            await(context, root, "host-hit"); intent(context, Intent.ATTACK_PRESS); intent(context, Intent.ATTACK_RELEASE); ack(root, name + "-hit");
            await(context, root, "reaction"); intent(context, Intent.SKILL_PRESS); ack(root, name + "-reaction");
            await(context, root, "guest-wiped"); context.waitTicks(35);
            intent(context, Intent.ATTACK_PRESS); intent(context, Intent.ATTACK_RELEASE); ack(root, name + "-continue");
        } else {
            await(context, root, "guest-hit"); intent(context, Intent.ATTACK_PRESS); intent(context, Intent.ATTACK_RELEASE); ack(root, name + "-hit");
            await(context, root, "respawn");
            // The combat-kill packet opens this screen; LocalPlayer.isAlive is not that packet's contract.
            context.waitForScreen(DeathScreen.class);
            context.waitFor(client -> CombatInput.state().members().get(1).hpFraction() == 0
                    && CombatInput.state().members().get(3).hpFraction() == 0, 400);
            context.takeScreenshot(TestScreenshotOptions.of("coop-guest-wiped")
                    .disableCounterPrefix().withDestinationDir(root.resolve("screenshots")));
            context.runOnClient(client -> {
                if (!(client.gui.screen() instanceof DeathScreen)) throw new AssertionError("Vanilla death screen before own respawn");
                client.player.respawn();
            });
        }
        await(context, root, "done");
        context.waitFor(client -> client.player != null && client.player.isAlive()
                && !(client.gui.screen() instanceof DeathScreen) && CombatInput.state().hp() > 0, 400);
        context.runOnClient(client -> {
            if (!client.player.isAlive()) throw new AssertionError("Both clients alive after independent respawn");
        });
        ack(root, name + "-done");
        context.runOnClient(client -> client.disconnect(new TitleScreen(), false));
        context.waitFor(client -> client.getConnection() == null && client.level == null, 200);
    }
}
