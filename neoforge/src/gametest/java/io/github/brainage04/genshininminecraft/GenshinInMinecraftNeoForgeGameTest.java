package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.gametest.GenshinInMinecraftGameTests;
import io.github.brainage04.genshininminecraft.gametest.ManagedWorldGameTests;
import io.github.brainage04.genshininminecraft.gametest.TravelerCombatGameTests;
import io.github.brainage04.genshininminecraft.gametest.HilichurlGameTests;
import io.github.brainage04.genshininminecraft.gametest.StaminaGameTests;
import io.github.brainage04.genshininminecraft.gametest.TraversalGameTests;
import io.github.brainage04.genshininminecraft.gametest.PartyGameTests;
import io.github.brainage04.genshininminecraft.gametest.KaeyaGameTests;
import io.github.brainage04.genshininminecraft.gametest.AmberGameTests;
import io.github.brainage04.genshininminecraft.gametest.LisaGameTests;
import io.github.brainage04.genshininminecraft.gametest.CombatLifecycleGameTests;
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
            Map.entry("live_normals_roll_seeded_crits_and_update_harbinger_hp_condition", HilichurlGameTests::liveNormalsRollSeededCritsAndUpdateHarbingerHpCondition),
            Map.entry("safe_fall_does_not_damage", TraversalGameTests::safeFallDoesNotDamage),
            Map.entry("damaging_fall_loses_half_max_hp", TraversalGameTests::damagingFallLosesHalfMaxHp),
            Map.entry("lethal_fall_forces_character_switch", TraversalGameTests::lethalFallForcesCharacterSwitch),
            Map.entry("water_landing_negates_fall", TraversalGameTests::waterLandingNegatesFall),
            Map.entry("climb_drains_and_exhaustion_falls", TraversalGameTests::climbDrainsAndExhaustionFalls),
            Map.entry("glide_drains_and_descends", TraversalGameTests::glideDrainsAndDescends),
            Map.entry("climb_mantles_steps_and_rejects_overhang", TraversalGameTests::climbMantlesStepsAndRejectsOverhang),
            Map.entry("climb_outside_corners", TraversalGameTests::climbOutsideCorners),
            Map.entry("climb_inside_corners", TraversalGameTests::climbInsideCorners),
            Map.entry("climb_one_block_steps", TraversalGameTests::climbOneBlockSteps),
            Map.entry("dimension_change_permanently_cancels_field_objects", CombatLifecycleGameTests::dimensionChangePermanentlyCancelsFieldObjects),
            Map.entry("party_wipe_discards_landed_bunny", CombatLifecycleGameTests::partyWipeDiscardsLandedBunny),
            Map.entry("same_tick_lethal_trade_earlier_traveler_wins", HilichurlGameTests::sameTickLethalTradeEarlierTravelerWins),
            Map.entry("same_tick_earlier_traveler_hit_prevents_club", HilichurlGameTests::sameTickEarlierTravelerHitPreventsClub),
            Map.entry("same_tick_earlier_freeze_prevents_club", HilichurlGameTests::sameTickEarlierFreezePreventsClub),
            Map.entry("same_tick_earlier_freeze_prevents_puppet_club", HilichurlGameTests::sameTickEarlierFreezePreventsPuppetClub),
            Map.entry("bunny_save_reload_cannot_outlive_cast", CombatLifecycleGameTests::bunnySaveReloadCannotOutliveCast),
            Map.entry("violet_tap_stacks_and_unascended_charge", LisaGameTests::violetTapStacksAndUnascendedCharge),
            Map.entry("violet_hold_consumes_three_stacks", LisaGameTests::violetHoldConsumesThreeStacks),
            Map.entry("violet_tap_multistacks_and_off_field_projectile", LisaGameTests::violetTapMultistacksAndOffFieldProjectile),
            Map.entry("lightning_rose_energy_timing_and_switch", LisaGameTests::lightningRoseEnergyTimingAndSwitch),
            Map.entry("electro_charged_ticks_consume_both_gauges", LisaGameTests::electroChargedTicksConsumeBothGauges),
            Map.entry("lisa_overloads_ambers_pyro", LisaGameTests::lisaOverloadsAmbersPyro),
            Map.entry("aura_command_permissions_and_targeting", LisaGameTests::auraCommandPermissionsAndTargeting),
            Map.entry("lisa_effects_never_damage_puppets_or_players", LisaGameTests::lisaEffectsNeverDamagePuppetsOrPlayers),
            Map.entry("charged_aimed_shot_damage_and_pyro", AmberGameTests::chargedAimedShotDamageAndPyro),
            Map.entry("jump_cancels_aimed_shot", AmberGameTests::jumpCancelsAimedShot),
            Map.entry("stationary_sprint_cancels_aimed_shot", AmberGameTests::stationarySprintCancelsAimedShot),
            Map.entry("exhausted_sprint_cancels_aimed_shot", AmberGameTests::exhaustedSprintCancelsAimedShot),
            Map.entry("baron_bunny_taunts_takes_club_and_expires", AmberGameTests::baronBunnyTauntsTakesClubAndExpires),
            Map.entry("destroyed_bunny_explodes_once_and_miss_grants_no_energy", AmberGameTests::destroyedBunnyExplodesOnceAndMissGrantsNoEnergy),
            Map.entry("fiery_rain_damage_over_time_preserves_blocks", AmberGameTests::fieryRainDamageOverTimePreservesBlocks),
            Map.entry("amber_overloads_lisas_electro", AmberGameTests::amberOverloadsLisasElectro),
            Map.entry("amber_forward_melts_kaeyas_cryo", AmberGameTests::amberForwardMeltsKaeyasCryo),
            Map.entry("traveler_swirls_ambers_pyro", AmberGameTests::travelerSwirlsAmbersPyro),
            Map.entry("amber_vaporizes_seeded_hydro", AmberGameTests::amberVaporizesSeededHydro),
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
            Map.entry("party_switch_cooldown_and_health_mirror", PartyGameTests::partySwitchCooldownAndHealthMirror),
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
