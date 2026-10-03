package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.gametest.GenshinInMinecraftGameTests;
import io.github.brainage04.genshininminecraft.gametest.ManagedWorldGameTests;
import io.github.brainage04.genshininminecraft.gametest.TravelerCombatGameTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class GenshinInMinecraftGameTest {
    @GameTest
    public void genshinCommandIsRegistered(GameTestHelper context) {
        GenshinInMinecraftGameTests.genshinCommandIsRegistered(context);
    }

    @GameTest
    public void managedBlockBreaking(GameTestHelper context) {
        ManagedWorldGameTests.managedBlockBreaking(context);
    }

    @GameTest
    public void managedHunger(GameTestHelper context) {
        ManagedWorldGameTests.managedHunger(context);
    }

    @GameTest
    public void managedGamerulesRestored(GameTestHelper context) {
        ManagedWorldGameTests.managedGamerulesRestored(context);
    }

    @GameTest
    public void managedSavedDataRoundTrip(GameTestHelper context) {
        ManagedWorldGameTests.managedSavedDataRoundTrip(context);
    }

    @GameTest
    public void managedPlacementAndTrampling(GameTestHelper context) {
        ManagedWorldGameTests.managedPlacementAndTrampling(context);
    }

    @GameTest
    public void managedFireAndMobExplosion(GameTestHelper context) {
        ManagedWorldGameTests.managedFireAndMobExplosion(context);
    }

    @GameTest
    public void managedCommandPermissions(GameTestHelper context) {
        ManagedWorldGameTests.managedCommandPermissions(context);
    }

    @GameTest
    public void arenaCommandBuilds(GameTestHelper context) {
        ManagedWorldGameTests.arenaCommandBuilds(context);
    }

    @GameTest
    public void palmVortexDamageAndCooldown(GameTestHelper context) {
        TravelerCombatGameTests.palmVortexDamageAndCooldown(context);
    }
    @GameTest
    public void burstEnergyAndTornado(GameTestHelper context) {
        TravelerCombatGameTests.burstEnergyAndTornado(context);
    }
    @GameTest
    public void palmVortexSwirlsAndAbsorbsPyro(GameTestHelper context) {
        TravelerCombatGameTests.palmVortexSwirlsAndAbsorbsPyro(context);
    }
    @GameTest
    public void managedVanillaMeleeCancelled(GameTestHelper context) {
        TravelerCombatGameTests.managedVanillaMeleeCancelled(context);
    }
    @GameTest
    public void targetDeathIsAttributed(GameTestHelper context) {
        TravelerCombatGameTests.targetDeathIsAttributed(context);
    }
    @GameTest
    public void managedOffOnDiscardsQueuedElectroCharged(GameTestHelper context) {
        TravelerCombatGameTests.managedOffOnDiscardsQueuedElectroCharged(context);
    }
}
