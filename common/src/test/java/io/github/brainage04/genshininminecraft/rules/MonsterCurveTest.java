package io.github.brainage04.genshininminecraft.rules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MonsterCurveTest {
    private static final double EPS = 1e-9;
    // Numeric sources: ExcelBinOutput/{Monster,MonsterCurve,Avatar,AvatarCurve,Weapon,WeaponCurve}ExcelConfigData.json
    // @792978e5503ecfba73dcb3562ed44a0d35a2abe2; complete transcriptions in spec/mechanics/datamine.md.
    // Independent Python Decimal arithmetic: expectedHP=Decimal(baseHP)*Decimal(curveHP[L]); likewise ATK.
    // Rows cover club21010201, shooter21010401, all small-slime families, all large/Mutant families, not Java output.
    @Test void ordinaryMonsterFamiliesUseExactExportedGrowthAtEarlyAndHighLevels() {
        double[][] golden = {
            {13.584, 22.608, 1, 72.916996656, 45.67991616},
            {13.584, 22.608, 8, 237.81997824, 103.56204816},
            {13.584, 22.608, 20, 885.2000016, 301.04993664},
            {13.584, 22.608, 100, 36765.123168, 4431.97600992},
            {10.8672, 11.304, 1, 58.3335973248, 22.83995808},
            {10.8672, 11.304, 8, 190.255982592, 51.78102408},
            {10.8672, 11.304, 20, 708.16000128, 150.52496832},
            {10.8672, 11.304, 100, 29412.0985344, 2215.98800496},
            {10.8672, 7.536, 1, 58.3335973248, 15.22663872},
            {10.8672, 7.536, 8, 190.255982592, 34.52068272},
            {10.8672, 7.536, 20, 708.16000128, 100.34997888},
            {10.8672, 7.536, 100, 29412.0985344, 1477.32533664},
            {10.8672, 15.072, 1, 58.3335973248, 30.45327744},
            {10.8672, 15.072, 8, 190.255982592, 69.04136544},
            {10.8672, 15.072, 20, 708.16000128, 200.69995776},
            {10.8672, 15.072, 100, 29412.0985344, 2954.65067328},
            {27.168, 35.168, 1, 145.833993312, 71.05764736},
            {27.168, 35.168, 8, 475.63995648, 161.09651936},
            {27.168, 35.168, 20, 1770.4000032, 468.29990144},
            {27.168, 35.168, 100, 73530.246336, 6894.18490432},
            {27.168, 52.752, 1, 145.833993312, 106.58647104},
            {27.168, 52.752, 8, 475.63995648, 241.64477904},
            {27.168, 52.752, 20, 1770.4000032, 702.44985216},
            {27.168, 52.752, 100, 73530.246336, 10341.27735648}
        };
        for (double[] row : golden) {
            int level = (int) row[2];
            assertEquals(row[3], MonsterCurve.hp(row[0], level), EPS);
            assertEquals(row[4], MonsterCurve.attack(row[1], level), EPS);
        }
        // Lv50 is an independent middle-curve anchor:13.584*367.8213;22.608*50.14548.
        assertEquals(4996.4845392, MonsterCurve.hp(13.584, 50), EPS);
        assertEquals(1133.68901184, MonsterCurve.attack(22.608, 50), EPS);
    }

    @Test void campProfileUsesSourcedAttackAndDefenseRatherThanTheOldFlatAttack() {
        int[] levels = {1, 8, 20};
        double[] hp = {72.916996656, 237.81997824, 885.2000016};
        double[] attack = {45.67991616, 103.56204816, 301.04993664};
        for (int i = 0; i < levels.length; i++) {
            assertEquals(hp[i], HilichurlProfile.maxHp(levels[i]), EPS);
            assertEquals(attack[i], HilichurlProfile.attack(levels[i]), EPS);
            assertEquals(500 + 5 * levels[i], HilichurlProfile.defense(levels[i]), EPS);
        }
        // All100 exported DEF rows are1+.01L; multiply base500, independently equal to5L+500.
        for (int level = 1; level <= 100; level++)
            assertEquals(500 + 5 * level, MonsterCurve.defense(500, level), EPS);
        for (int level : new int[]{0, 101}) {
            assertThrows(IllegalArgumentException.class, () -> MonsterCurve.hp(13.584, level));
            assertThrows(IllegalArgumentException.class, () -> MonsterCurve.attack(22.608, level));
            assertThrows(IllegalArgumentException.class, () -> MonsterCurve.defense(500, level));
        }
        for (int level : new int[]{0, 21}) {
            assertThrows(IllegalArgumentException.class, () -> HilichurlProfile.attack(level));
            assertThrows(IllegalArgumentException.class, () -> HilichurlProfile.defense(level));
        }
    }

    @Test void incomingClubDamageUsesTheEnemyLevelCurveAndEachLevel20CharactersDefense() {
        // damage.md incoming formula, Physical RES0 and Fighter multiplier1:
        // Decimal("22.608")*curveATK[L]*(500+5L)/(Decimal(characterBaseDEF)*2.569+500+5L).
        // Expected decimals rounded only for these literals to12 places; inputs never read from implementation.
        int[] levels = {1, 8, 20};
        CharacterBaseStats.Character[] characters = CharacterBaseStats.Character.values();
        double[][] golden = {
            {35.380318393849, 81.401176940938, 241.803609235888},
            {36.364217250716, 83.546515235612, 247.652132366749},
            {34.148390776040, 78.706439550955, 234.422161603820},
            {36.704457963364, 84.286979900945, 249.665021612532},
        };
        for (int slot = 0; slot < characters.length; slot++)
            for (int i = 0; i < levels.length; i++)
                assertEquals(golden[slot][i], Damage.enemyDamage(HilichurlProfile.attack(levels[i]), 1,
                        levels[i], CharacterBaseStats.at(characters[slot], 20).def(), 0), EPS);
    }

    @Test void characterAndWeaponStatsUseUnroundedBaseTimesUnascendedCurve() {
        // Avatar bases×1 atLv1 and×2.569 atLv20; ascension0 adds0 in AvatarPromoteExcelConfigData.
        double[][] level1 = {
            {911.791, 17.808, 57.225},
            {793.2582, 18.6984, 50.358},
            {975.6164, 18.6984, 66.381},
            {802.3761, 19.41072, 48.069},
        };
        double[][] level20 = {
            {2342.391079, 45.748752, 147.011025},
            {2037.8803158, 48.0361896, 129.369702},
            {2506.3585316, 48.0361896, 170.532789},
            {2061.3042009, 49.86613968, 123.489261},
        };
        for (var character : CharacterBaseStats.Character.values()) {
            int slot = character.ordinal();
            for (int level : new int[]{1, 20}) {
                var actual = CharacterBaseStats.at(character, level);
                double[] expected = level == 1 ? level1[slot] : level20[slot];
                assertEquals(expected[0], actual.hp(), EPS);
                assertEquals(expected[1], actual.atk(), EPS);
                assertEquals(expected[2], actual.def(), EPS);
                assertEquals(20, actual.levelCap());
            }
        }
        // Weapon initValue×curve20:38.7413×2.42;37.6075×2.275;.102/.068/.0766×1.767.
        assertEquals(93.753946, StarterLoadout.HARBINGER_OF_DAWN.baseAtk(), EPS);
        assertEquals(.180234, StarterLoadout.HARBINGER_OF_DAWN.critDamage(), EPS);
        assertEquals(85.5570625, StarterLoadout.SLINGSHOT.baseAtk(), EPS);
        assertEquals(.120156, StarterLoadout.SLINGSHOT.critRate(), EPS);
        assertEquals(93.753946, StarterLoadout.THRILLING_TALES.baseAtk(), EPS);
        assertEquals(.1353522, StarterLoadout.THRILLING_TALES.hpPercent(), EPS);
        // EquipAffixExcelConfigData at the same pin:1113020/1153040/1143020 (R1).
        assertEquals(.14, StarterLoadout.HARBINGER_CRIT_RATE, EPS);
        assertEquals(.36, StarterLoadout.SLINGSHOT_CLOSE_BONUS, EPS);
        assertEquals(-.1, StarterLoadout.SLINGSHOT_DISTANT_BONUS, EPS);
        assertEquals(.24, StarterLoadout.THRILLING_TALES.switchAtkPercent(), EPS);
        assertEquals(18, StarterLoadout.SLINGSHOT_CLOSE_FRAMES);
        assertEquals(600, StarterLoadout.THRILLING_TALES_DURATION_FRAMES);
        assertEquals(1200, StarterLoadout.THRILLING_TALES_COOLDOWN_FRAMES);
    }
}
