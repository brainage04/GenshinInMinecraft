package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.gametest.GenshinInMinecraftGameTests;
import io.github.brainage04.genshininminecraft.gametest.ManagedWorldGameTests;
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
}
