# Pinned 7.1 numeric datamine

## Pin, provenance and precision

Pinned commit **[`792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/commit/792978e5503ecfba73dcb3562ed44a0d35a2abe2)**, committed **2026-10-01 23:32:38 UTC**, commit label **`CNRELWin7.1.0_R48379043_S48511369_D48533839`**. This is a release-labelled commit, not a Git tag. Verified with the [GitLab commits API](https://gitlab.com/api/v4/projects/Dimbreath%2Fanimegamedata2/repository/commits?per_page=30&until=2026-10-02T00%3A00%3A00Z).

The requested [AnimeGameData](https://gitlab.com/Dimbreath/AnimeGameData) stopped at6.6; its [deprecation README at433284d22ef1a704e9d49ae22fb96cf7a245828e](https://gitlab.com/Dimbreath/AnimeGameData/-/raw/433284d22ef1a704e9d49ae22fb96cf7a245828e/README.md) explicitly redirects to **AnimeGameData2**, the same maintainer's continuation. No7.1 branch/tag exists in the original. The pin above is the later of the two October1 commits sharing the release label, not `main` or an assumed historical wiki revision.

Only numeric config JSON needed for this audit was fetched to `/tmp/genshin-7.1-datamine`; no dump, assets, code, text maps, audio or copied descriptions enter the mod. Tables below contain numeric facts and our arithmetic. **Exact** means the dump's exported decimal inputs and their unrounded decimal products. JSON does not establish the engine's intermediate IEEE754 rounding order; Java rules use doubles and do not claim binary-identical live-client output. All source links below are pinned to the full commit; `GGKIEIJLCJJ` is the exported curve-value field, `arith=ARITH_MULTI` means multiplication. No fitting/interpolation/extrapolation.

## Ordinary monsters

All rows: [`ExcelBinOutput/MonsterExcelConfigData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/ExcelBinOutput/MonsterExcelConfigData.json). HP/ATK/DEF use `GROW_CURVE_HP` / `GROW_CURVE_ATTACK` / `GROW_CURVE_DEFENSE` respectively. Each resistance field (`physicalSubHurt`, `fireSubHurt`, `waterSubHurt`, `elecSubHurt`, `iceSubHurt`, `grassSubHurt`, `windSubHurt`, `rockSubHurt`) is **0.1** for every listed row: matches existing hilichurl10% and the slime body's published10%. **Slime own-element immunity is separate behaviour**, not an infinite numeric RES in these rows; retain the KQM immunity qualification in [slimes.md](slimes.md#resistance-immunity-and-poise). No slime entities exist in the current runtime, so slime values here are sourced coverage, not a claim of implemented slime AI.

| Monster | ID | base HP | base ATK | base DEF | Comparison |
| --- | ---: | ---: | ---: | ---: | --- |
| Basic Hilichurl | 21010101 | 13.584 | 22.608 | 500 | HP/DEF parameters match published references; ATK now pinned |
| Hilichurl Fighter (club; runtime profile) | 21010201 | 13.584 | 22.608 | 500 | HP/DEF parameters match published references; ATK now pinned / flat120 removed |
| Uninfused Hilichurl Shooter (still substituted by club) | 21010401 | 10.8672 | 11.304 | 500 | HP/DEF parameters match published references; ATK now pinned |
| Small Anemo slime | 20010301 | 10.8672 | 15.072 | 500 | HP/DEF parameters match published references; ATK now pinned |
| Large Anemo slime | 20010401 | 27.168 | 35.168 | 500 | HP/DEF parameters match published references; ATK now pinned |
| Small Electro slime | 20010501 | 10.8672 | 7.536 | 500 | HP/DEF parameters match published references; ATK now pinned |
| Large Electro slime | 20010601 | 27.168 | 52.752 | 500 | HP/DEF parameters match published references; ATK now pinned |
| Mutant Electro slime | 20010701 | 27.168 | 52.752 | 500 | HP/DEF parameters match published references; ATK now pinned |
| Small Cryo slime | 20010801 | 10.8672 | 7.536 | 500 | HP/DEF parameters match published references; ATK now pinned |
| Large Cryo slime | 20010901 | 27.168 | 52.752 | 500 | HP/DEF parameters match published references; ATK now pinned |
| Small Hydro slime | 20011001 | 10.8672 | 7.536 | 500 | HP/DEF parameters match published references; ATK now pinned |
| Large Hydro slime | 20011101 | 27.168 | 35.168 | 500 | HP/DEF parameters match published references; ATK now pinned |
| Small Pyro slime | 20011201 | 10.8672 | 7.536 | 500 | HP/DEF parameters match published references; ATK now pinned |
| Large Pyro slime | 20011301 | 27.168 | 52.752 | 500 | HP/DEF parameters match published references; ATK now pinned |

### Growth, levels1–100

All rows: [`ExcelBinOutput/MonsterCurveExcelConfigData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/ExcelBinOutput/MonsterCurveExcelConfigData.json), named types above. `stat(L)=monster.baseStat×curve(L)`. DEF500×the DEF column is algebraically **5L+500** at every row1–100: **matches** the old formula, no enemy DEF balance change. The dump does not independently prove the incoming-damage formula's500-vs501 scalar; [damage.md](damage.md#incoming-enemy-talent-damage) retains that separate behavioural-source conflict.

| Level | HP curve | ATK curve | DEF curve |
| ---: | ---: | ---: | ---: |
| 1 | 5.367859 | 2.02052 | 1.01 |
| 2 | 6.818905 | 2.32577 | 1.02 |
| 3 | 8.421231 | 2.65371 | 1.03 |
| 4 | 10.17484 | 3.00456 | 1.04 |
| 5 | 12.07987 | 3.37863 | 1.05 |
| 6 | 14.13796 | 3.77619 | 1.06 |
| 7 | 16.34828 | 4.19745 | 1.07 |
| 8 | 17.50736 | 4.58077 | 1.08 |
| 9 | 19.26782 | 4.98102 | 1.09 |
| 10 | 21.09865 | 5.39831 | 1.1 |
| 11 | 24.06383 | 5.95895 | 1.11 |
| 12 | 27.14657 | 6.54609 | 1.12 |
| 13 | 30.34872 | 7.16003 | 1.13 |
| 14 | 33.90172 | 7.829 | 1.14 |
| 15 | 37.58709 | 8.5272 | 1.15 |
| 16 | 41.4068 | 9.255 | 1.16 |
| 17 | 46.00103 | 10.24456 | 1.17 |
| 18 | 50.05359 | 11.26446 | 1.18 |
| 19 | 54.18154 | 12.31476 | 1.19 |
| 20 | 65.1649 | 13.31608 | 1.2 |
| 21 | 68.61109 | 14.26676 | 1.21 |
| 22 | 72.10564 | 15.24058 | 1.22 |
| 23 | 75.35814 | 16.20382 | 1.23 |
| 24 | 79.78938 | 17.18874 | 1.24 |
| 25 | 84.30028 | 18.19528 | 1.25 |
| 26 | 88.9361 | 18.885 | 1.26 |
| 27 | 93.66512 | 19.58528 | 1.27 |
| 28 | 98.48837 | 20.29613 | 1.28 |
| 29 | 103.4069 | 21.0176 | 1.29 |
| 30 | 108.4217 | 21.74973 | 1.3 |
| 31 | 114.4719 | 22.61761 | 1.31 |
| 32 | 120.6543 | 23.50049 | 1.32 |
| 33 | 125.744 | 24.3984 | 1.33 |
| 34 | 148.7045 | 25.37062 | 1.34 |
| 35 | 153.7546 | 26.19798 | 1.35 |
| 36 | 158.886 | 27.03764 | 1.36 |
| 37 | 165.88 | 28.30728 | 1.37 |
| 38 | 173.5809 | 29.66658 | 1.38 |
| 39 | 181.4102 | 31.05228 | 1.39 |
| 40 | 198.3841 | 32.51703 | 1.4 |
| 41 | 207.9781 | 34.0855 | 1.41 |
| 42 | 217.7367 | 35.68458 | 1.42 |
| 43 | 227.0249 | 37.24761 | 1.43 |
| 44 | 236.4653 | 38.8393 | 1.44 |
| 45 | 264.514 | 40.45966 | 1.45 |
| 46 | 273.7571 | 41.71497 | 1.46 |
| 47 | 289.6475 | 43.60169 | 1.47 |
| 48 | 305.8628 | 45.52411 | 1.48 |
| 49 | 322.4075 | 47.48235 | 1.49 |
| 50 | 367.8213 | 50.14548 | 1.5 |
| 51 | 384.5919 | 51.97996 | 1.51 |
| 52 | 401.5548 | 53.83439 | 1.52 |
| 53 | 418.7124 | 55.7086 | 1.53 |
| 54 | 436.0667 | 57.60231 | 1.54 |
| 55 | 452.5111 | 59.40968 | 1.55 |
| 56 | 466.241 | 60.96815 | 1.56 |
| 57 | 483.8348 | 63.18038 | 1.57 |
| 58 | 503.1843 | 65.56533 | 1.58 |
| 59 | 522.7799 | 67.97952 | 1.59 |
| 60 | 616.9946 | 70.615 | 1.6 |
| 61 | 637.3271 | 72.87743 | 1.61 |
| 62 | 659.6903 | 75.3163 | 1.62 |
| 63 | 682.2833 | 77.77995 | 1.63 |
| 64 | 711.7287 | 80.84824 | 1.64 |
| 65 | 734.9008 | 83.39035 | 1.65 |
| 66 | 753.9569 | 85.24774 | 1.66 |
| 67 | 829.3079 | 89.40919 | 1.67 |
| 68 | 855.3966 | 92.12469 | 1.68 |
| 69 | 879.7074 | 94.70249 | 1.69 |
| 70 | 960.8343 | 97.48067 | 1.7 |
| 71 | 987.2563 | 100.11726 | 1.71 |
| 72 | 1016.308 | 102.95798 | 1.72 |
| 73 | 1037.415 | 105.19038 | 1.73 |
| 74 | 1067.75 | 108.15596 | 1.74 |
| 75 | 1098.412 | 111.14884 | 1.75 |
| 76 | 1123.775 | 113.33174 | 1.76 |
| 77 | 1153.119 | 116.23478 | 1.77 |
| 78 | 1182.76 | 119.16229 | 1.78 |
| 79 | 1210.233 | 121.92657 | 1.79 |
| 80 | 1366.735 | 125.41919 | 1.8 |
| 81 | 1394.867 | 128.0491 | 1.81 |
| 82 | 1423.247 | 130.69698 | 1.82 |
| 83 | 1440.909 | 132.59346 | 1.83 |
| 84 | 1485.468 | 134.49483 | 1.84 |
| 85 | 1503.56 | 136.40102 | 1.85 |
| 86 | 1532.91 | 138.65791 | 1.86 |
| 87 | 1563.946 | 141.59421 | 1.87 |
| 88 | 1598.809 | 144.79605 | 1.88 |
| 89 | 1634.08 | 148.02296 | 1.89 |
| 90 | 1792.851 | 151.52367 | 1.9 |
| 91 | 1835.423 | 155.44891 | 1.91 |
| 92 | 1882.428 | 159.68016 | 1.92 |
| 93 | 1930.047 | 163.96825 | 1.93 |
| 94 | 1996.661 | 169.54305 | 1.94 |
| 95 | 2042.759 | 173.8133 | 1.95 |
| 96 | 2055.588 | 175.20334 | 1.96 |
| 97 | 2069.809 | 176.75739 | 1.97 |
| 98 | 2256.937 | 189.76929 | 1.98 |
| 99 | 2272.524 | 191.44691 | 1.99 |
| 100 | 2706.502 | 196.03574 | 2 |

### Exact ordinary HP/ATK at supported camp levels1–20

Every number: [`ExcelBinOutput/MonsterExcelConfigData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/ExcelBinOutput/MonsterExcelConfigData.json) × [`ExcelBinOutput/MonsterCurveExcelConfigData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/ExcelBinOutput/MonsterCurveExcelConfigData.json). The camp default remainsLv8, already included. Small-ordinary ATK covers Pyro/Cryo/Electro/Hydro; small Anemo has its separate column. Large35.168 ATK covers Anemo/Hydro; large52.752 covers Pyro/Cryo/Electro/Mutant Electro. Shooter and all small slimes share HP10.8672; all large slimes share HP27.168. DEF is505→600 (Lv8:540). HP corrections replace the rounded references, ATK corrections replace flat120 for the club runtime. Non-hilichurl vanilla training targets retain their documented×100 adaptation, not these slime profiles.

| Lv | Hilichurl HP | Hilichurl ATK | Shooter/small HP | Shooter ATK | Small ordinary ATK | Small Anemo ATK | Large HP | Large35.168 ATK | Large52.752 ATK |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 72.916996656 | 45.67991616 | 58.3335973248 | 22.83995808 | 15.22663872 | 30.45327744 | 145.833993312 | 71.05764736 | 106.58647104 |
| 2 | 92.62800552 | 52.58100816 | 74.102404416 | 26.29050408 | 17.52700272 | 35.05400544 | 185.25601104 | 81.79267936 | 122.68901904 |
| 3 | 114.394001904 | 59.99507568 | 91.5152015232 | 29.99753784 | 19.99835856 | 39.99671712 | 228.788003808 | 93.32567328 | 139.98850992 |
| 4 | 138.21502656 | 67.92709248 | 110.572021248 | 33.96354624 | 22.64236416 | 45.28472832 | 276.43005312 | 105.66436608 | 158.49654912 |
| 5 | 164.09295408 | 76.38406704 | 131.274363264 | 38.19203352 | 25.46135568 | 50.92271136 | 328.18590816 | 118.81965984 | 178.22948976 |
| 6 | 192.05004864 | 85.37210352 | 153.640038912 | 42.68605176 | 28.45736784 | 56.91473568 | 384.10009728 | 132.80104992 | 199.20157488 |
| 7 | 222.07503552 | 94.8959496 | 177.660028416 | 47.4479748 | 31.6319832 | 63.2639664 | 444.15007104 | 147.6159216 | 221.4238824 |
| 8 | 237.81997824 | 103.56204816 | 190.255982592 | 51.78102408 | 34.52068272 | 69.04136544 | 475.63995648 | 161.09651936 | 241.64477904 |
| 9 | 261.73406688 | 112.61090016 | 209.387253504 | 56.30545008 | 37.53696672 | 75.07393344 | 523.46813376 | 175.17251136 | 262.75876704 |
| 10 | 286.6040616 | 122.04499248 | 229.28324928 | 61.02249624 | 40.68166416 | 81.36332832 | 573.2081232 | 189.84776608 | 284.77164912 |
| 11 | 326.88306672 | 134.7199416 | 261.506453376 | 67.3599708 | 44.9066472 | 89.8132944 | 653.76613344 | 209.5643536 | 314.3465304 |
| 12 | 368.75900688 | 147.99400272 | 295.007205504 | 73.99700136 | 49.33133424 | 98.66266848 | 737.51801376 | 230.21289312 | 345.31933968 |
| 13 | 412.25701248 | 161.87395824 | 329.805609984 | 80.93697912 | 53.95798608 | 107.91597216 | 824.51402496 | 251.80393504 | 377.70590256 |
| 14 | 460.52096448 | 176.998032 | 368.416771584 | 88.499016 | 58.999344 | 117.998688 | 921.04192896 | 275.330272 | 412.995408 |
| 15 | 510.58303056 | 192.7829376 | 408.466424448 | 96.3914688 | 64.2609792 | 128.5219584 | 1021.16606112 | 299.8845696 | 449.8268544 |
| 16 | 562.4699712 | 209.23704 | 449.97597696 | 104.61852 | 69.74568 | 139.49136 | 1124.9399424 | 325.47984 | 488.21976 |
| 17 | 624.87799152 | 231.60901248 | 499.902393216 | 115.80450624 | 77.20300416 | 154.40600832 | 1249.75598304 | 360.28068608 | 540.42102912 |
| 18 | 679.92796656 | 254.66691168 | 543.942373248 | 127.33345584 | 84.88897056 | 169.77794112 | 1359.85593312 | 396.14852928 | 594.22279392 |
| 19 | 736.00203936 | 278.41209408 | 588.801631488 | 139.20604704 | 92.80403136 | 185.60806272 | 1472.00407872 | 433.08547968 | 649.62821952 |
| 20 | 885.2000016 | 301.04993664 | 708.16000128 | 150.52496832 | 100.34997888 | 200.69995776 | 1770.4000032 | 468.29990144 | 702.44985216 |

### Runtime changes, every supported enemy level

| Lv | old rounded HP → new exact HP | old flat ATK → new exact ATK |
| ---: | --- | --- |
| 1 | 72.917 → 72.916996656 | 120 → 45.67991616 |
| 2 | 92.628 → 92.62800552 | 120 → 52.58100816 |
| 3 | 114.394 → 114.394001904 | 120 → 59.99507568 |
| 4 | 138.215 → 138.21502656 | 120 → 67.92709248 |
| 5 | 164.093 → 164.09295408 | 120 → 76.38406704 |
| 6 | 192.050 → 192.05004864 | 120 → 85.37210352 |
| 7 | 222.075 → 222.07503552 | 120 → 94.8959496 |
| 8 | 237.820 → 237.81997824 | 120 → 103.56204816 |
| 9 | 261.734 → 261.73406688 | 120 → 112.61090016 |
| 10 | 286.604 → 286.6040616 | 120 → 122.04499248 |
| 11 | 326.883 → 326.88306672 | 120 → 134.7199416 |
| 12 | 368.759 → 368.75900688 | 120 → 147.99400272 |
| 13 | 412.257 → 412.25701248 | 120 → 161.87395824 |
| 14 | 460.521 → 460.52096448 | 120 → 176.998032 |
| 15 | 510.583 → 510.58303056 | 120 → 192.7829376 |
| 16 | 562.470 → 562.4699712 | 120 → 209.23704 |
| 17 | 624.878 → 624.87799152 | 120 → 231.60901248 |
| 18 | 679.928 → 679.92796656 | 120 → 254.66691168 |
| 19 | 736.002 → 736.00203936 | 120 → 278.41209408 |
| 20 | 885.200 → 885.2000016 | 120 → 301.04993664 |

## Starter weapons, level20/20 unascended, R1

Stat sources: [`ExcelBinOutput/WeaponExcelConfigData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/ExcelBinOutput/WeaponExcelConfigData.json) and [`ExcelBinOutput/WeaponCurveExcelConfigData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/ExcelBinOutput/WeaponCurveExcelConfigData.json). The ascension0 rows of [`ExcelBinOutput/WeaponPromoteExcelConfigData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/ExcelBinOutput/WeaponPromoteExcelConfigData.json) have unlock cap20 and no added ATK values. `StarterLoadout` already equips **the same Harbinger of Dawn sword on Traveler and Kaeya**; there is no additional Dull Blade/training sword to preserve or audit.

| Weapon / users | ID | base-ATK arithmetic | old ATK → new | secondary arithmetic | old secondary → new |
| --- | ---: | --- | --- | --- | --- |
| Harbinger of Dawn / Traveler, Kaeya | 11302 | 38.7413×2.42 (`ATTACK_101`) | 94 → 93.753946 | .102×1.767 (`CRITICAL_101`) | 18% CD → 18.0234% CD |
| Slingshot / Amber | 15304 | 37.6075×2.275 (`ATTACK_104`) | 86 → 85.5570625 | .068×1.767 (`CRITICAL_101`) | 12% CR → 12.0156% CR |
| Thrilling Tales of Dragon Slayers / Lisa | 14302 | 38.7413×2.42 (`ATTACK_101`) | 94 → 93.753946 | .0766×1.767 (`CRITICAL_101`) | 13.5% HP → 13.53522% HP |

Passive numbers: [`ExcelBinOutput/EquipAffixExcelConfigData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/ExcelBinOutput/EquipAffixExcelConfigData.json), `level` absent/0 isR1, `id` equals weapon's `skillAffix`, `affixId` appends refinement index0. Behaviour remains sourced to [damage.md](damage.md#named-starter-loadout-2026-10-07), not copied descriptions or inferred from obfuscated ability code.

| Passive | Affix ID | R1 exported parameters | Comparison |
| --- | ---: | --- | --- |
| Harbinger | 1113020 | .14 CR | **matches** +14percentage points. Strict>90% HP condition remains public-source behaviour; threshold not in this row. |
| Slingshot | 1153040 | .3seconds, .36 bonus, .1 penalty | **matches**18frames/+36%/−10%. Hitscan still uses close branch. |
| Thrilling Tales | 1143020 | .24 ATK,10seconds,20seconds | **matches**24%/600frames/1200frames. Own-player recipient and snapshot rules unchanged. |

## Character base stats, no ascension or quest boosts

Every row: [`ExcelBinOutput/AvatarExcelConfigData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/ExcelBinOutput/AvatarExcelConfigData.json) and [`ExcelBinOutput/AvatarCurveExcelConfigData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/ExcelBinOutput/AvatarCurveExcelConfigData.json). All four select `GROW_CURVE_HP_S4` for HP/DEF and `GROW_CURVE_ATTACK_S4` for ATK: Lv1 multiplier1; Lv20 multiplier**2.569**. Ascension0 rows of [`ExcelBinOutput/AvatarPromoteExcelConfigData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/ExcelBinOutput/AvatarPromoteExcelConfigData.json) (promote IDs12/21/15/4) add nothing and cap at20. Aether10000005 and Lumine10000007 have matching base parameters; runtime uses Aether. Later quest additions remain ungranted.

| Character ID / level | HP old → new | ATK old → new | DEF old → new |
| --- | --- | --- | --- |
| Traveler 10000005 / 1 | 911.79 → 911.791 | 17.81 → 17.808 | 57.23 → 57.225 |
| Traveler 10000005 / 20 | 2342.39 → 2342.391079 | 45.75 → 45.748752 | 147.01 → 147.011025 |
| Amber 10000021 / 1 | 793.26 → 793.2582 | 18.70 → 18.6984 | 50.36 → 50.358 |
| Amber 10000021 / 20 | 2037.88 → 2037.8803158 | 48.04 → 48.0361896 | 129.37 → 129.369702 |
| Kaeya 10000015 / 1 | 975.62 → 975.6164 | 18.70 → 18.6984 | 66.38 → 66.381 |
| Kaeya 10000015 / 20 | 2506.36 → 2506.3585316 | 48.04 → 48.0361896 | 170.53 → 170.532789 |
| Lisa 10000006 / 1 | 802.38 → 802.3761 | 19.41 → 19.41072 | 48.07 → 48.069 |
| Lisa 10000006 / 20 | 2061.30 → 2061.3042009 | 49.87 → 49.86613968 | 123.49 → 123.489261 |

Derived runtime totals (same stat construction; no artifacts):

| Quantity | old → new |
| --- | --- |
| Traveler total ATK | 139.75 → 139.502698 |
| Amber total ATK | 134.04 → 133.5932521 |
| Kaeya total ATK | 142.04 → 141.7901356 |
| Lisa total ATK | 143.87 → 143.62008568 |
| Lisa weapon-scaled max HP | 2339.5755 → 2340.30625936105698 |
| Full-HP sword-user CD | 68% → 68.0234% (CR19% unchanged) |
| Amber CR | 17% → 17.0156% (CD50% unchanged) |
| Lv20 hilichurl two/three/four-player max HP | 1327.8/1770.4/2213 → 1327.8000024/1770.4000032/2213.000004 |
| Lv20 hilichurl at half solo HP | 442.6 → 442.6000008 |
| Lv20 club damage vs Lv20 Traveler, Physical RES0 | 96.384251884178 → 241.803609235888 (new:301.04993664×600/(57.225×2.569+600)) |

## Camp spawns and world level

**Finding: no identifiable exact camp monster roster or WL0 levels were recovered. Keep the namedLv8 adaptation and the existing Shooter→club substitution.** A spatial area-level polygon is not a monster spawn, a `groupId` is not a monster ID, and a World Level table is not a per-camp offset; do not substitute any of them silently.

Files actually inspected at the pin:

- [`ExcelBinOutput/MonsterExcelConfigData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/ExcelBinOutput/MonsterExcelConfigData.json).
- [`ExcelBinOutput/WorldLevelExcelConfigData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/ExcelBinOutput/WorldLevelExcelConfigData.json).
- [`ExcelBinOutput/SceneExcelConfigData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/ExcelBinOutput/SceneExcelConfigData.json).
- [`BinOutput/Scene/LevelLayout/scene3_levelLayout.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/BinOutput/Scene/LevelLayout/scene3_levelLayout.json).
- [`BinOutput/Scene/WorldArea/scene3_worldArea.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/BinOutput/Scene/WorldArea/scene3_worldArea.json).
- [`BinOutput/LevelDesign/Meta/LevelMetaData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/BinOutput/LevelDesign/Meta/LevelMetaData.json).
- [`BinOutput/LevelDesign/Monsters/10463479770824889537.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/BinOutput/LevelDesign/Monsters/10463479770824889537.json).

`SceneExcelConfigData` identifies overworld scene3 and `BigWorld_LevelStreaming`, but supplies no camp monster IDs/levels. `scene3_levelLayout` has area polygons and `level` fields, not per-spawn monster IDs; `scene3_worldArea` has area boundaries, not spawns. `LevelMetaData` lists86 scene3 blocks with hashed monster-metadata references. The complete1959-file `BinOutput/LevelDesign/Monsters` filename listing was checked: none of the12 referenced files selected by scene3 global/Mondstadt bounding blocks (centreX0–4096, centreZ−4096–1024) could be resolved by the exported path hash. The inspected monster-metadata JSON is scene102, with group/config/template identifiers, not `monsterId`, level or position; it cannot identify a Mondstadt camp. Root and `BinOutput` trees contain no `Scripts`/Lua server spawn-group directory. This is a limitation of the checked dump/schema, **not proof that no spawn data exists anywhere in the repository**. Exact Starfell Valley, Windrise and Mondstadt-gate camp IDs/levels remain unknown.

`WorldLevelExcelConfigData` exports `monsterLevel` rather than a field explicitly labelled offset. There is **no WL0 row**; do not synthesize one. These values alone cannot prove `campLevel=WL0level+offset`:

| World Level | exported monsterLevel | other numeric field |
| ---: | ---: | --- |
| 0 | absent | absent |
| 1 | 26 | — |
| 2 | 34 | — |
| 3 | 47 | — |
| 4 | 59 | — |
| 5 | 69 | — |
| 6 | 80 | — |
| 7 | 88 | — |
| 8 | 90 | — |
| 9 | 100 | `IDLGNALCKNN=-10` (meaning unverified) |

No camp-level implementation change follows from this table. The overlay has Starfell and Windrise camps, not an independently sourced gate camp. An owner observation/server spawn export is still needed for precise roster/level mapping.

## Traversal/stamina constants found

All named constants: [`ExcelBinOutput/ConstValueExcelConfigData.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/ExcelBinOutput/ConstValueExcelConfigData.json), exported `CBOMLBFIPJM` values. Also inspected [`BinOutput/Common/ConfigGlobalCombat.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/BinOutput/Common/ConfigGlobalCombat.json) and [`BinOutput/Global/GlobalValues/ConfigGlobalValues.json` @ `792978e5503ecfba73dcb3562ed44a0d35a2abe2`](https://gitlab.com/Dimbreath/animegamedata2/-/raw/792978e5503ecfba73dcb3562ed44a0d35a2abe2/BinOutput/Global/GlobalValues/ConfigGlobalValues.json); they exposed ability names/obfuscated fields, not a defensible calibrated ordinary movement-speed mapping. Stop here rather than decode or copy game code.

| Constant | exported numbers | Current repo / conclusion |
| --- | --- | --- |
| `DASH_COST_STAMINA` | 18,18 | **matches** dash18/start18; two entries do not prove billing order or sprint rate. |
| `DASH_BS_COST_STAMINA` | 18,18 | **matches** selected18; state meaning beyond name unverified. |
| `FLY_COST_STAMINA` | 3 | **matches** glider3/s, rate unit corroborated by public research. |
| `CLIMB_MIN_STAMINA` | 5 | **matches** climb-entry5. |
| `CLIMB_JUMP_COST_STAMINA` | 25 | **matches** selected25, now numeric-sourced instead of citation-needed. |
| `CLIMB_COST_STAMINA` | 59,1,91,2,105,3 | Curve/config tuple meaning unverified; **not** a constant climb drain. Keep owner-calibrated5.36/s adaptation. |
| `CLIMB_COST_STAMINA_FORMULA` | .8,.002,.00016,.8,2 | Formula parameter order/inputs unverified; cannot derive5.36/s or an exact speed. |
| `CLIMB_JUMP_COST_STAMINA_FORMULA` | 20,.12,20,30 | Formula parameter meanings unverified; do not replace25 with20. |
| `STAMINA_RECOVER_WAIT_TIME` | 1.5 | **matches**90reference frames. |
| `STAMINA_LIMIT` | 100 | **matches**100starter pool. |
| `EXTRA_STAMINA_LIMIT` | 140,100 | First value matches100+140=240 selected cap; second-value semantics unverified. |
| `SWIM_COST_STAMINA` / `SWIM_DASH_COST_STAMINA` / `SWIM_DASH_COST_STAMINA_PER_SECOND` | 4 / 1 / 10.2 | Swimming not implemented.4 and10.2 match public references; raw dash1 differs from the public initial-cost2 in[stamina.md](stamina.md#regeneration-and-ordinary-action-costs). State/billing semantics unverified; do not silently reinterpret either or implement swimming in this task. |

Ordinary held-sprint18/s and regeneration25/s remain selected public-source values, not independently recovered datamine fields. Climb/glide/dash/sprint ground speeds, timing/billing boundaries, dash immunity, map-scale conversion and climb formula semantics remain unknown/adapted. No traversal number changes in this task.

## Golden-test derivation

`MonsterCurveTest` multiplies exported decimal base parameters by independently transcribed curve rows at1/8/20/50/100; it covers club/shooter and the slime HP/ATK families, DEF and incoming club damage against Lv20 characters. `StarterLoadoutTest` independently computes each normal hit with Decimal arithmetic from raw character/weapon inputs, published talent multipliers, DEF120/(220+enemyLevel), RES.9, and Slingshot1.36 only for Amber; expected values never come from Java rule output. All original normal-chain kill counts remain8/27,7/25,6/22,9/32 atLv8/Lv20 respectively. Incoming100% club now uses22.608×ATKcurve, not flat120. Existing regression expectations are migrated without loosening tolerances.
