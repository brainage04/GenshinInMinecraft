package io.github.brainage04.genshininminecraft.gametest;

import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;

/** Real server-owned party state, hit shapes, lethal damage hook and reactions, shared by both loaders. */
public final class PartyGameTests {
    private PartyGameTests() {}
    public static void lisaNormalThenTravelerSwirl(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = new Hilichurl(GenshinEntities.HILICHURL, context.getLevel());
            enemy.snapTo(player.position().add(0, 0, 2));
            enemy.setNoAi(true);
            context.assertTrue(context.getLevel().addFreshEntity(enemy), "Reaction test hilichurl spawned");
            var session = runtime.session(player);
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            context.assertTrue(runtime.receive(player, Intent.SWITCH_4), "Lisa switch accepted");
            context.assertTrue(runtime.receive(player, Intent.ATTACK_PRESS), "Lisa normal accepted");
            runtime.receive(player, Intent.ATTACK_RELEASE);
            session.advanceTo(start + 25);
            var target = runtime.target(enemy);
            close(context, target.hp(), 885.200, "Lisa normal cannot hit before source frame26");
            session.advanceTo(start + 26);
            close(context, target.hp(), 885.200 - (49.87 + 23) * .396 * .5 * .9,
                    "Talent1 Lisa normal uses Lisa level20/20 ATK, not Traveler stats");
            context.assertTrue(target.aura().gauge(Element.ELECTRO) > 0, "Lisa normal applies 1U Electro aura to hilichurl");
            context.assertTrue(session.intent(Intent.SWITCH_1, start + 60), "Switch back after sourced one second");
            context.assertTrue(session.intent(Intent.SKILL_PRESS, start + 60), "Traveler Palm accepted");
            session.intent(Intent.SKILL_RELEASE, start + 60);
            session.advanceTo(start + 92);
            context.assertValueEqual(target.swirlCount(), 1, "Traveler skill Swirls Lisa's Electro aura");
            context.assertValueEqual(session.skillAbsorbedElement(), Element.ELECTRO, "Traveler absorbs Electro before consuming aura");
            close(context, session.party().kit(0).energy(), 6, "Active Traveler receives two same-element particles");
            for (int slot = 1; slot < 4; slot++) close(context, session.party().kit(slot).energy(), 1.2,
                    "Off-field different-element members each receive0.6 per particle");
            enemy.discard();
        });
        context.succeed();
    }
    public static void partySwitchCooldownAndHealthMirror(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            long start = Frames.atServerTick(context.getLevel().getServer().getTickCount());
            session.party().kit(1).setHp(2037.88 / 2);
            context.assertTrue(runtime.receive(player, Intent.SWITCH_2), "Switch to Amber accepted");
            close(context, player.getHealth(), 10, "Active Amber HP fraction mirrors to vanilla health");
            context.assertFalse(runtime.receive(player, Intent.SWITCH_3), "A second switch inside one second is rejected");
            context.assertValueEqual(session.party().activeSlot(), 1, "Rejected switch leaves Amber active");
            context.assertFalse(session.intent(Intent.SWITCH_3, start + 59), "One frame before sourced cooldown is still rejected");
            context.assertTrue(session.intent(Intent.SWITCH_3, start + 60), "Exact one-second boundary accepts switch");
            context.assertTrue(session.intent(Intent.SWITCH_4, start + 120), "Switch to Lisa after another full cooldown");
            close(context, player.getHealth(), 20, "Lisa's full HP mirrors after switching away from half-HP Amber");
            close(context, session.party().kit(1).hp(), 2037.88 / 2, "Off-field Amber retains her HP");
        });
        context.succeed();
    }
    public static void characterDeathForcesSwitchAndPartyWipeKillsPlayer(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var enemy = new Hilichurl(GenshinEntities.HILICHURL, context.getLevel());
            enemy.snapTo(player.position().add(0, 0, 2));
            var session = runtime.session(player);
            session.kit().grantEnergy(60);
            session.kit().setHp(1);
            context.assertTrue(runtime.receive(player, Intent.SKILL_PRESS), "Precondition: held Palm blocks manual switch");
            context.assertTrue(runtime.enemyHit(player, enemy, 120, 1, 20), "Lethal character hit accepted");
            context.assertTrue(player.isAlive(), "A surviving party member prevents vanilla death");
            context.assertValueEqual(session.party().activeSlot(), 1, "Immediate cyclic replacement selects Amber");
            close(context, session.party().kit(0).hp(), 0, "Fallen Traveler stays dead");
            close(context, session.party().kit(0).energy(), 0, "Fallen Traveler energy resets to zero");
            close(context, player.getHealth(), 20, "Vanilla health now mirrors full-HP Amber");
            for (int slot = 1; slot < 4; slot++) {
                session.kit().setHp(1);
                context.assertTrue(runtime.enemyHit(player, enemy, 120, 1, 20), "Next lethal character hit accepted");
                if (slot < 3) context.assertTrue(player.isAlive(), "Forced switch bypasses existing manual cooldown");
            }
            context.assertFalse(player.isAlive(), "All four fallen members permit ordinary vanilla death");
            context.assertValueEqual(player.getKillCredit(), enemy, "Vanilla party-wipe death retains attacker credit");
        });
        context.succeed();
    }
    public static void vanillaLethalDamageAlsoForcesCharacterSwitch(GameTestHelper context) {
        HilichurlGameTests.withManaged(context, (runtime, player) -> {
            var session = runtime.session(player);
            player.hurtServer(context.getLevel(), context.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
            context.assertTrue(player.isAlive(), "Shared lethal vanilla-damage hook preserves player when another member lives");
            context.assertValueEqual(session.party().activeSlot(), 1, "Vanilla lethal damage selects next living Amber");
            close(context, session.party().kit(0).hp(), 0, "Vanilla damage falls only active Traveler");
            close(context, player.getHealth(), 20, "Vanilla health mirrors replacement member");
        });
        context.succeed();
    }
    private static void close(GameTestHelper context, double actual, double expected, String message) {
        context.assertTrue(Math.abs(actual - expected) < .0001, message + ": expected " + expected + ", actual " + actual);
    }
}
