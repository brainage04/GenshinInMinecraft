package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.gametest.GenshinInMinecraftGameTests;
import io.github.brainage04.genshininminecraft.gametest.ManagedWorldGameTests;
import io.github.brainage04.genshininminecraft.gametest.TravelerCombatGameTests;
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
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("genshin_command_is_registered", GenshinInMinecraftGameTests::genshinCommandIsRegistered),
            Map.entry("managed_block_breaking", ManagedWorldGameTests::managedBlockBreaking),
            Map.entry("managed_hunger", ManagedWorldGameTests::managedHunger),
            Map.entry("managed_gamerules_restored", ManagedWorldGameTests::managedGamerulesRestored),
            Map.entry("managed_saved_data_round_trip", ManagedWorldGameTests::managedSavedDataRoundTrip),
            Map.entry("managed_placement_and_trampling", ManagedWorldGameTests::managedPlacementAndTrampling),
            Map.entry("managed_fire_and_mob_explosion", ManagedWorldGameTests::managedFireAndMobExplosion),
            Map.entry("managed_command_permissions", ManagedWorldGameTests::managedCommandPermissions),
            Map.entry("arena_command_builds", ManagedWorldGameTests::arenaCommandBuilds),
            Map.entry("palm_vortex_damage_and_cooldown", TravelerCombatGameTests::palmVortexDamageAndCooldown),
            Map.entry("burst_energy_and_tornado", TravelerCombatGameTests::burstEnergyAndTornado),
            Map.entry("palm_vortex_swirls_and_absorbs_pyro", TravelerCombatGameTests::palmVortexSwirlsAndAbsorbsPyro),
            Map.entry("managed_vanilla_melee_cancelled", TravelerCombatGameTests::managedVanillaMeleeCancelled),
            Map.entry("target_death_is_attributed", TravelerCombatGameTests::targetDeathIsAttributed)
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
