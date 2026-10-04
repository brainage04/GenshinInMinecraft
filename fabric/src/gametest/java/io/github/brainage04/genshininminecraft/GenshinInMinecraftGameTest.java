package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.gametest.GenshinInMinecraftGameTests;
import io.github.brainage04.genshininminecraft.gametest.ManagedWorldGameTests;
import io.github.brainage04.genshininminecraft.gametest.TravelerCombatGameTests;
import io.github.brainage04.genshininminecraft.gametest.HilichurlGameTests;
import io.github.brainage04.genshininminecraft.gametest.StaminaGameTests;
import io.github.brainage04.genshininminecraft.gametest.PartyGameTests;
import io.github.brainage04.genshininminecraft.gametest.KaeyaGameTests;
import io.github.brainage04.genshininminecraft.gametest.AmberGameTests;
import io.github.brainage04.genshininminecraft.gametest.LisaGameTests;
import io.github.brainage04.genshininminecraft.gametest.CombatLifecycleGameTests;
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
    @GameTest
    public void campCommandAndSpecDamage(GameTestHelper context) {
        HilichurlGameTests.campCommandAndSpecDamage(context);
    }
    @GameTest
    public void aggroWindupAndCharacterDamage(GameTestHelper context) {
        HilichurlGameTests.aggroWindupAndCharacterDamage(context);
    }
    @GameTest
    public void leashHealsClearsAuraAndReturns(GameTestHelper context) {
        HilichurlGameTests.leashHealsClearsAuraAndReturns(context);
    }
    @GameTest
    public void clubMissAndPlayerDeath(GameTestHelper context) {
        HilichurlGameTests.clubMissAndPlayerDeath(context);
    }
    @GameTest
    public void sprintDrainsAndStopsAtZero(GameTestHelper context) {
        StaminaGameTests.sprintDrainsAndStopsAtZero(context);
    }
    @GameTest
    public void dashCostsStaminaAndDodgesClub(GameTestHelper context) {
        StaminaGameTests.dashCostsStaminaAndDodgesClub(context);
    }
    @GameTest
    public void chargedAttackRequiresStamina(GameTestHelper context) {
        StaminaGameTests.chargedAttackRequiresStamina(context);
    }
    @GameTest
    public void lisaNormalThenTravelerSwirl(GameTestHelper context) {
        PartyGameTests.lisaNormalThenTravelerSwirl(context);
    }
    @GameTest
    public void partySwitchCooldownAndHealthMirror(GameTestHelper context) {
        PartyGameTests.partySwitchCooldownAndHealthMirror(context);
    }
    @GameTest
    public void characterDeathForcesSwitchAndPartyWipeKillsPlayer(GameTestHelper context) {
        PartyGameTests.characterDeathForcesSwitchAndPartyWipeKillsPlayer(context);
    }
    @GameTest
    public void vanillaLethalDamageAlsoForcesCharacterSwitch(GameTestHelper context) {
        PartyGameTests.vanillaLethalDamageAlsoForcesCharacterSwitch(context);
    }
    @GameTest
    public void frostgnawDamageAndCryoAura(GameTestHelper context) {
        KaeyaGameTests.frostgnawDamageAndCryoAura(context);
    }
    @GameTest
    public void frostgnawSuperconductPhysicalShred(GameTestHelper context) {
        KaeyaGameTests.frostgnawSuperconductPhysicalShred(context);
    }
    @GameTest
    public void lisaTriggersSuperconductOnCryo(GameTestHelper context) {
        KaeyaGameTests.lisaTriggersSuperconductOnCryo(context);
    }
    @GameTest
    public void frostgnawMeltsPyroAura(GameTestHelper context) {
        KaeyaGameTests.frostgnawMeltsPyroAura(context);
    }
    @GameTest
    public void travelerSwirlsKaeyasCryo(GameTestHelper context) {
        KaeyaGameTests.travelerSwirlsKaeyasCryo(context);
    }
    @GameTest
    public void glacialWaltzEnergyContactAndSwitch(GameTestHelper context) {
        KaeyaGameTests.glacialWaltzEnergyContactAndSwitch(context);
    }
    @GameTest
    public void frozenTargetCannotMove(GameTestHelper context) {
        KaeyaGameTests.frozenTargetCannotMove(context);
    }
    @GameTest
    public void simultaneousBurstsKeepCharacterIcdSeparate(GameTestHelper context) {
        KaeyaGameTests.simultaneousBurstsKeepCharacterIcdSeparate(context);
    }
    @GameTest
    public void chargedAimedShotDamageAndPyro(GameTestHelper context) { AmberGameTests.chargedAimedShotDamageAndPyro(context); }
    @GameTest
    public void jumpCancelsAimedShot(GameTestHelper context) { AmberGameTests.jumpCancelsAimedShot(context); }
    @GameTest
    public void stationarySprintCancelsAimedShot(GameTestHelper context) { AmberGameTests.stationarySprintCancelsAimedShot(context); }
    @GameTest
    public void exhaustedSprintCancelsAimedShot(GameTestHelper context) { AmberGameTests.exhaustedSprintCancelsAimedShot(context); }
    @GameTest
    public void dimensionChangePermanentlyCancelsFieldObjects(GameTestHelper context) { CombatLifecycleGameTests.dimensionChangePermanentlyCancelsFieldObjects(context); }
    @GameTest
    public void partyWipeDiscardsLandedBunny(GameTestHelper context) { CombatLifecycleGameTests.partyWipeDiscardsLandedBunny(context); }
    @GameTest
    public void sameTickLethalTradeEarlierTravelerWins(GameTestHelper context) { HilichurlGameTests.sameTickLethalTradeEarlierTravelerWins(context); }
    @GameTest
    public void sameTickEarlierTravelerHitPreventsClub(GameTestHelper context) { HilichurlGameTests.sameTickEarlierTravelerHitPreventsClub(context); }
    @GameTest
    public void bunnySaveReloadCannotOutliveCast(GameTestHelper context) { CombatLifecycleGameTests.bunnySaveReloadCannotOutliveCast(context); }
    @GameTest
    public void baronBunnyTauntsTakesClubAndExpires(GameTestHelper context) { AmberGameTests.baronBunnyTauntsTakesClubAndExpires(context); }
    @GameTest
    public void destroyedBunnyExplodesOnceAndMissGrantsNoEnergy(GameTestHelper context) { AmberGameTests.destroyedBunnyExplodesOnceAndMissGrantsNoEnergy(context); }
    @GameTest
    public void fieryRainDamageOverTimePreservesBlocks(GameTestHelper context) { AmberGameTests.fieryRainDamageOverTimePreservesBlocks(context); }
    @GameTest
    public void amberOverloadsLisasElectro(GameTestHelper context) { AmberGameTests.amberOverloadsLisasElectro(context); }
    @GameTest
    public void amberForwardMeltsKaeyasCryo(GameTestHelper context) { AmberGameTests.amberForwardMeltsKaeyasCryo(context); }
    @GameTest
    public void travelerSwirlsAmbersPyro(GameTestHelper context) { AmberGameTests.travelerSwirlsAmbersPyro(context); }
    @GameTest
    public void amberVaporizesSeededHydro(GameTestHelper context) { AmberGameTests.amberVaporizesSeededHydro(context); }
    @GameTest
    public void violetTapStacksAndUnascendedCharge(GameTestHelper context) { LisaGameTests.violetTapStacksAndUnascendedCharge(context); }
    @GameTest
    public void violetHoldConsumesThreeStacks(GameTestHelper context) { LisaGameTests.violetHoldConsumesThreeStacks(context); }
    @GameTest
    public void violetTapMultistacksAndOffFieldProjectile(GameTestHelper context) { LisaGameTests.violetTapMultistacksAndOffFieldProjectile(context); }
    @GameTest
    public void lightningRoseEnergyTimingAndSwitch(GameTestHelper context) { LisaGameTests.lightningRoseEnergyTimingAndSwitch(context); }
    @GameTest
    public void electroChargedTicksConsumeBothGauges(GameTestHelper context) { LisaGameTests.electroChargedTicksConsumeBothGauges(context); }
    @GameTest
    public void lisaOverloadsAmbersPyro(GameTestHelper context) { LisaGameTests.lisaOverloadsAmbersPyro(context); }
    @GameTest
    public void auraCommandPermissionsAndTargeting(GameTestHelper context) { LisaGameTests.auraCommandPermissionsAndTargeting(context); }
    @GameTest
    public void lisaEffectsNeverDamagePuppetsOrPlayers(GameTestHelper context) { LisaGameTests.lisaEffectsNeverDamagePuppetsOrPlayers(context); }
}
