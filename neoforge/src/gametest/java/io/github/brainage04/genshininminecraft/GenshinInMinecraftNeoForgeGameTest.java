package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.gametest.GenshinInMinecraftGameTests;
import io.github.brainage04.genshininminecraft.gametest.ManagedWorldGameTests;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/// Each function has a matching `data/genshininminecraft/test_instance/<name>.json`.
@EventBusSubscriber(modid = GenshinInMinecraft.MOD_ID)
public final class GenshinInMinecraftNeoForgeGameTest {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
            "genshin_command_is_registered", GenshinInMinecraftGameTests::genshinCommandIsRegistered,
            "managed_block_breaking", ManagedWorldGameTests::managedBlockBreaking,
            "managed_hunger", ManagedWorldGameTests::managedHunger,
            "managed_gamerules_restored", ManagedWorldGameTests::managedGamerulesRestored,
            "managed_saved_data_round_trip", ManagedWorldGameTests::managedSavedDataRoundTrip,
            "managed_placement_and_trampling", ManagedWorldGameTests::managedPlacementAndTrampling,
            "managed_fire_and_mob_explosion", ManagedWorldGameTests::managedFireAndMobExplosion,
            "managed_command_permissions", ManagedWorldGameTests::managedCommandPermissions,
            "arena_command_builds", ManagedWorldGameTests::arenaCommandBuilds
    );

    private GenshinInMinecraftNeoForgeGameTest() {
    }

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        TESTS.forEach((name, test) -> event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(GenshinInMinecraft.MOD_ID, name),
                () -> test
        ));
    }
}
