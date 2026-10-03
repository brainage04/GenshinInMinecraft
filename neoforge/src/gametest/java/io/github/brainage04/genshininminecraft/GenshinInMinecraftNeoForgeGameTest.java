package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.gametest.GenshinInMinecraftGameTests;
import io.github.brainage04.genshininminecraft.gametest.ManagedWorldGameTests;
import io.github.brainage04.genshininminecraft.gametest.TravelerCombatGameTests;
import io.github.brainage04.genshininminecraft.gametest.HilichurlGameTests;
import io.github.brainage04.genshininminecraft.gametest.StaminaGameTests;
import io.github.brainage04.genshininminecraft.gametest.PartyGameTests;
import io.github.brainage04.genshininminecraft.gametest.KaeyaGameTests;
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
            Map.entry("target_death_is_attributed", TravelerCombatGameTests::targetDeathIsAttributed),
            Map.entry("managed_off_on_discards_queued_electro_charged", TravelerCombatGameTests::managedOffOnDiscardsQueuedElectroCharged),
            Map.entry("camp_command_and_spec_damage", HilichurlGameTests::campCommandAndSpecDamage),
            Map.entry("aggro_windup_and_character_damage", HilichurlGameTests::aggroWindupAndCharacterDamage),
            Map.entry("leash_heals_clears_aura_and_returns", HilichurlGameTests::leashHealsClearsAuraAndReturns),
            Map.entry("club_miss_and_player_death", HilichurlGameTests::clubMissAndPlayerDeath),
            Map.entry("sprint_drains_and_stops_at_zero", StaminaGameTests::sprintDrainsAndStopsAtZero),
            Map.entry("dash_costs_stamina_and_dodges_club", StaminaGameTests::dashCostsStaminaAndDodgesClub),
            Map.entry("lisa_normal_then_traveler_swirl", PartyGameTests::lisaNormalThenTravelerSwirl),
            Map.entry("party_switch_cooldown_and_unavailable_talents", PartyGameTests::partySwitchCooldownAndUnavailableTalents),
            Map.entry("character_death_forces_switch_and_party_wipe_kills_player", PartyGameTests::characterDeathForcesSwitchAndPartyWipeKillsPlayer),
            Map.entry("vanilla_lethal_damage_also_forces_character_switch", PartyGameTests::vanillaLethalDamageAlsoForcesCharacterSwitch),
            Map.entry("charged_attack_requires_stamina", StaminaGameTests::chargedAttackRequiresStamina),
            Map.entry("frostgnaw_damage_and_cryo_aura", KaeyaGameTests::frostgnawDamageAndCryoAura),
            Map.entry("frostgnaw_superconduct_physical_shred", KaeyaGameTests::frostgnawSuperconductPhysicalShred),
            Map.entry("lisa_triggers_superconduct_on_cryo", KaeyaGameTests::lisaTriggersSuperconductOnCryo),
            Map.entry("frostgnaw_melts_pyro_aura", KaeyaGameTests::frostgnawMeltsPyroAura),
            Map.entry("traveler_swirls_kaeyas_cryo", KaeyaGameTests::travelerSwirlsKaeyasCryo),
            Map.entry("glacial_waltz_energy_contact_and_switch", KaeyaGameTests::glacialWaltzEnergyContactAndSwitch),
            Map.entry("frozen_target_cannot_move", KaeyaGameTests::frozenTargetCannotMove),
            Map.entry("simultaneous_bursts_keep_character_icd_separate", KaeyaGameTests::simultaneousBurstsKeepCharacterIcdSeparate)
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
