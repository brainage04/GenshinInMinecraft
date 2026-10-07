package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.client.TeleportScreen;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.world.OverlayDefinition;
import io.github.brainage04.genshininminecraft.world.OverlayRuntime;
import io.github.brainage04.genshininminecraft.world.OverlaySavedData;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import org.lwjgl.glfw.GLFW;

/** Drives the real M binding, server-owned unlock payload, list widgets and travel request. */
final class OverlayChecks {
    private OverlayChecks() {}
    static void teleportList(ClientGameTestContext context, TestDedicatedServerContext server) {
        server.runCommand("execute as @a at @s run genshin arena");
        server.runOnServer(minecraftServer -> {
            var player = minecraftServer.getPlayerList().getPlayers().getFirst();
            var session = CombatRuntime.get(minecraftServer).session(player);
            for (var member : session.party().members()) member.setHp(member.maxHp());
            session.resourcesChanged();
            var origin = player.position();
            var arrival = new OverlayDefinition.Position(origin.x, origin.y, origin.z, 0);
            var first = new OverlayDefinition.Position(origin.x + 3, origin.y, origin.z, 0);
            var second = new OverlayDefinition.Position(origin.x + 7, origin.y, origin.z, 0);
            var locked = new OverlayDefinition.Position(origin.x - 7, origin.y, origin.z, 0);
            var definition = new OverlayDefinition(1, "client-overlay", player.level().dimension().identifier().toString(), arrival,
                    List.of(new OverlayDefinition.TravelPoint("first", "Test Waypoint", "waypoint", first, first),
                            new OverlayDefinition.TravelPoint("second", "Test Statue", "statue", second, second),
                            new OverlayDefinition.TravelPoint("locked", "Locked Waypoint", "waypoint", locked, locked)), List.of());
            if (!OverlayRuntime.apply(minecraftServer, definition)) throw new AssertionError("Managed synthetic overlay must apply");
            var data = OverlaySavedData.get(minecraftServer);
            data.activate(player.getUUID(), "first", System.currentTimeMillis());
            data.activate(player.getUUID(), "second", System.currentTimeMillis());
        });
        context.getInput().pressKey(GLFW.GLFW_KEY_M);
        context.waitForScreen(TeleportScreen.class);
        context.runOnClient(client -> {
            var screen = (TeleportScreen) client.gui.screen();
            if (screen.points().size() != 2 || !screen.points().getFirst().id().equals("first")
                    || !screen.points().getLast().id().equals("second"))
                throw new AssertionError("M opens the actual server-owned unlocked list, excluding the locked point");
        });
        GenshinInMinecraftClientGameTest.screenshot(context, "genshin-teleport-list");
        context.getInput().pressKey(GLFW.GLFW_KEY_TAB);
        context.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
        context.waitFor(client -> client.gui.screen() == null);
        server.waitFor(minecraftServer -> {
            var player = minecraftServer.getPlayerList().getPlayers().getFirst();
            var point = OverlaySavedData.get(minecraftServer).definition().point("first");
            return player.position().distanceTo(OverlayRuntime.vec(point.arrival())) < .1;
        });
        server.runOnServer(minecraftServer -> {
            var data = OverlaySavedData.get(minecraftServer);
            for (var point : data.definition().points()) {
                var entity = minecraftServer.overworld().getEntityInAnyDimension(data.definition().entityId(point.id()));
                if (entity != null) entity.discard();
            }
            minecraftServer.overworld().getDataStorage().set(OverlaySavedData.TYPE, new OverlaySavedData());
            OverlayRuntime.stop(minecraftServer);
        });
    }
}
