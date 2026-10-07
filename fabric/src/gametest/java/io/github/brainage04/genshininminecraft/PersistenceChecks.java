package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.client.GenshinHud;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.rules.EventTimeline;
import io.github.brainage04.genshininminecraft.rules.Party;
import io.github.brainage04.genshininminecraft.rules.PartySave;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import io.github.brainage04.genshininminecraft.world.PartySavedData;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;

/** Real dedicated-server reconnection and explicit visual-fixture setup, never production resets. */
final class PersistenceChecks {
    private PersistenceChecks() {}
    static void resetFixture(TestDedicatedServerContext server) {
        server.runOnServer(s -> {
            if (ManagedWorld.isManaged(s.overworld())) throw new AssertionError("Seed fixtures only while managed gameplay is suspended");
            var player = s.getPlayerList().getPlayers().getFirst();
            PartySavedData.get(s).put(player.getUUID(), new Party(new EventTimeline(), (kit, hit) -> {}).save());
        });
    }
    static void reconnect(ClientGameTestContext context, TestDedicatedServerContext server) {
        server.runCommand("gamemode creative @a"); // Keep nearby camp AI from perturbing the known HP fixture.
        server.runCommand("tick freeze");
        var uuid = server.computeOnServer(s -> {
            var player = s.getPlayerList().getPlayers().getFirst();
            var session = CombatRuntime.get(s).session(player);
            if (!session.intent(Intent.SWITCH_3, session.party().frame())) throw new AssertionError("Reconnect fixture selects Kaeya");
            session.kit().state().restore(new PartySave.Member(1234.5, 17, 300, 600, 360, 900, false), session.party().frame());
            session.stamina().consume(37, session.party().frame());
            return player.getUUID();
        });
        context.waitFor(client -> CombatInput.managed() && CombatInput.state().activeSlot() == 2
                && CombatInput.state().energy() == 17);
        context.runOnClient(client -> {
            client.getConnection().getConnection().disconnect(net.minecraft.network.chat.Component.literal("Persistence reconnect test"));
            client.disconnectWithSavingScreen();
        });
        context.waitFor(client -> client.level == null && client.player == null && !CombatInput.managed());
        server.waitFor(s -> s.getPlayerList().getPlayer(uuid) == null);
        PartySave saved = server.computeOnServer(s -> PartySavedData.get(s).get(uuid));
        if (saved == null || saved.activeSlot() != 2 || saved.members().get(2).energy() != 17)
            throw new AssertionError("Actual socket logout must save the known UUID resources");
        try {
            var connection = server.connect();
            connection.waitForChunksRender();
            context.waitFor(client -> CombatInput.managed() && CombatInput.state().activeSlot() == 2 && GenshinHud.party().size() == 4);
            context.runOnClient(client -> {
                var row = GenshinHud.party().get(2);
                var state = CombatInput.state();
                if (!client.player.getUUID().equals(uuid) || !row.active() || Math.abs(row.hp() - 1234.5) > .001
                        || row.energy() != 17 || Math.abs(state.hp() - 1234.5) > .001
                        // Chunk download/render happens online: cooldowns may already have resumed.
                        || state.skillRemainingFrames() <= 0 || state.skillRemainingFrames() > saved.members().get(2).skillRemaining()
                        || state.burstRemainingFrames() <= 0 || state.burstRemainingFrames() > saved.members().get(2).burstRemaining()
                        || state.stamina() < (float) saved.stamina() || state.members().get(1).hpFraction() != 0
                        || GenshinHud.rejectionVisible())
                    throw new AssertionError("After reconnect the actual HUD must retain Kaeya HP/energy/cooldowns/stamina and fallen Amber: " + state);
                client.gui.hud.getChat().clearMessages(true);
            });
            GenshinInMinecraftClientGameTest.screenshot(context, "genshin-hud-reconnected");
        } finally {
            server.runCommand("tick unfreeze");
        }
    }
}
