# Credits and provenance

This is a private-use project for the owner and friends. Code licensing does not grant rights to reference maps, character models or other creators' work. Nothing is published or distributed until the permissions and legal review in [the brief](docs/GENSHIN_MINECRAFT_BRIEF.md) is complete.

| Material | Origin | Use and shipping status |
| --- | --- | --- |
| Mod project template | brainage04's Fabric + NeoForge multiloader template, with shared `common/` code and thin loader adapters. | Starting code/build structure for this repository. |
| Template initialization workflow and script ancestry | [nea89o](https://github.com/nea89o), [Forge1.8.9Template workflow](https://github.com/nea89o/Forge1.8.9Template/blob/master/.github/workflows/init.yml) and [make-my-own.sh](https://github.com/nea89o/Forge1.8.9Template/blob/master/make-my-own.sh). | Historical template ancestry, as credited in README; this project no longer uses the template initialization instructions. |
| Build/test convention tooling | [brainage04/FabricModdingConventions](https://github.com/brainage04/FabricModdingConventions); version pinned in `gradle.properties`. | Gradle convention plugins and GameTest/recording support, not gameplay assets. |
| GeckoLib animation/rendering library | [GeckoLib](https://github.com/bernie-g/geckolib), MIT, copyright (c) 2026 GeckoLib. Common compile API `com.geckolib:geckolib-common-26.2:5.5.5`; loader runtimes `geckolib-fabric-26.2:5.5.5`, `geckolib-neoforge-26.2:5.5.6`, from [Cloudsmith](https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/). | Required external loader-specific mod on clients and servers, not shaded. External jars retain the MIT copyright/permission notice in `LICENSE.txt`; no GeckoLib art imported. |
| Blocky Teyvat 5.1.0 map | [WanggMC / wangg_mc](https://ko-fi.com/wanggmc/shop), [map listing](https://ko-fi.com/s/5609329f72). | Owner-purchased, private reference only. Map archives/world data and bundled packs are not shipped with the mod. |
| YiFang character model set (`.ysm`) | YiFang, owner-purchased/private reference as described in the brief. | Private reference only; no YiFang models are imported into or shipped with the mod. No public listing or redistribution permission is recorded here. |
| Existing project icon | A live Minecraft screenshot of Dragonspine and the Skyfrost Nail in the owner-supplied Blocky Teyvat map, rendered with Iris, Sodium and Complementary Reimagined. Full capture/tool provenance: [docs/icon/README.md](docs/icon/README.md). | The existing screenshot is used as the project/mod icon. It is map-derived imagery, not a shipped map, model or shader pack; permissions must be reviewed before any public release. |
| Gameplay mechanics reference | Genshin Impact by HoYoverse; public research such as the [KQM Theorycrafting Library](https://library.keqingmains.com/) and owner observations. | Source links and pinned-version notes belong beside each researched value in `spec/mechanics/`. No extracted game assets, leaked code, or HoYoverse account/service access. |
| Starter weapons and encounter reference data | Public [Paimon.moe Harbinger](https://paimon.moe/weapons/harbinger_of_dawn), [Slingshot](https://paimon.moe/weapons/slingshot), [Thrilling Tales](https://paimon.moe/weapons/thrilling_tales_of_dragon_slayers) tables/passives; [public Type1 Hilichurl HP table](https://wiki3.jp/genshin_impact/page/2093), existing KQM/wiki DEF/RES research. | Published rounded numbers only, transcribed into the named starter loadout/level-indexed enemy profile; no datamine, extracted game assets or weapon artwork. Level8 default is our recorded adaptation, not sourced camp configuration. |
| Hilichurl placeholder texture | Original 64×64 RGBA procedural pixel drawing made for this repository: tan skin, dark leather wraps/sash and a bone-coloured mask with dark eye slots. | Shipped at `common/src/main/resources/assets/genshininminecraft/textures/entity/hilichurl.png`; no game artwork, downloaded texture or extracted Genshin asset used. Renderer reuses built-in Minecraft zombie humanoid geometry and a built-in wooden shovel as the placeholder club, not copied model/texture files. |
| Baron Bunny and Pyro placeholders | Built-in Minecraft rabbit model/texture (scaled at runtime), flame/explosion/dust particles, and original aim-reticle rectangles. | No texture/model files imported or copied into the mod; vanilla resources are referenced at runtime. No extracted Genshin art. |

**No imported third-party gameplay assets or character models yet.** The existing map-derived icon above is explicitly recorded rather than treating it as original artwork. Add every non-original asset or data source here when introduced; do not assume purchase grants redistribution rights.

<!-- character-assets:start -->

## Original generated character assets (20a)

`tools/models/generate.py` authors the Aether, Amber, Kaeya and Lisa cuboid geometry,
128×128 per-face shaded/pixel-painted textures, articulated weapons and original wind gliders,
and all locomotion key poses. Authored for this repository, 2026-10-08; no extracted game assets,
downloaded fan meshes, traced textures, YiFang content or copied sound files. Outputs under
`assets/genshininminecraft/geckolib/{models,animations}/character` and
`textures/entity/character` are reproducible with Python 3 (stdlib only); `--check` compares bytes.

Public visual references (silhouette/colour/signature features only):
- [Traveler](https://genshin-impact.fandom.com/wiki/Traveler)
- [Amber](https://genshin-impact.fandom.com/wiki/Amber)
- [Kaeya](https://genshin-impact.fandom.com/wiki/Kaeya)
- [Lisa](https://genshin-impact.fandom.com/wiki/Lisa)

These are placeholder-quality original adaptations, not faithful source-game meshes/poses.
All locomotion sounds refer to built-in Minecraft events; no new audio assets.

<!-- character-assets:end -->
