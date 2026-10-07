# GeckoLib integration design — Minecraft 26.2 / Java 25

Design spike for `todo.md` items **20a, 20b and 20c**, researched 2026-10-07. The goal is actual original character geometry and readable animation, not four recoloured vanilla player skins. The player remains one authoritative Minecraft player entity; switching changes its visible character without creating a puppet player or moving its position.

**Evidence boundary:** dependency availability, packaged metadata, GeckoLib source/API names and Minecraft class/method names below were inspected. This was read-only research: no repository edits, Gradle tasks, Minecraft launches or rendering tests. The integration is a proposed design, **[INFERENCE: runtime behaviour has not yet been exercised]**. Main must implement and run the acceptance checks in section 7 before calling any rendering path verified.

## 1. Dependency, common module and licensing

### Exact available artifacts

Repository: **`https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/`**.

| Use | Maven coordinate | Status observed |
| --- | --- | --- |
| Fabric client and server runtime | `com.geckolib:geckolib-fabric-26.2:5.5.5` | Latest Fabric 26.2 release in Cloudsmith metadata and Modrinth |
| NeoForge client and server runtime | `com.geckolib:geckolib-neoforge-26.2:5.5.6` | Latest NeoForge 26.2 release in Cloudsmith metadata and Modrinth |
| Shared compile API | `com.geckolib:geckolib-common-26.2:5.5.5` | Real published Java 25 library, ordinary API/runtime/source variants, no POM dependencies |

**Important corrections to old examples:** the current Java packages and Maven group are **`com.geckolib`**, not `software.bernie.geckolib`; the Minecraft version belongs in the **artifact name**, not in the Maven version. The GitHub wiki now redirects GeckoLib 5 readers to [the new wiki](https://wiki.geckolib.com/docs/geckolib5/). Its overall support table says 5.5.6 for 26.2, but Fabric's actual latest release is 5.5.5. Do not invent a Fabric 5.5.6 artifact.

Use common **5.5.5**, the lower shared API level, and NeoForge's 5.5.6 at runtime. I inspected the NeoForge binary's `GeoObjectRenderer`, `GeoEntityRenderer` and `AnimationController` signatures; the APIs used here are present. 5.5.6 fixes Molang/easing issues; avoid relying on its corrected pre/post or Catmull–Rom easing behaviour in assets that must render identically on Fabric 5.5.5. Start with numeric linear keyframes.

### Fit the existing project conventions

Add the repository, restricted to group `com.geckolib`, in each module using the same `exclusiveContent` pattern as Cloth Config:

```groovy
repositories {
    exclusiveContent {
        forRepository {
            maven { url = 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/' }
        }
        filter { includeGroup 'com.geckolib' }
    }
}
```

Dependency declarations for this repository's current ordinary-`implementation` Loom convention:

```groovy
// common/build.gradle — API only, never packaged or loaded as a second mod
compileOnly 'com.geckolib:geckolib-common-26.2:5.5.5'

// fabric/build.gradle — follow the existing loader + production-test pattern
implementation('com.geckolib:geckolib-fabric-26.2:5.5.5') {
    exclude group: 'net.fabricmc.fabric-api'
}
productionRuntimeMods('com.geckolib:geckolib-fabric-26.2:5.5.5') {
    exclude group: 'net.fabricmc.fabric-api'
}

// neoforge/build.gradle
implementation 'com.geckolib:geckolib-neoforge-26.2:5.5.6'
productionRuntimeMods 'com.geckolib:geckolib-neoforge-26.2:5.5.6'
```

The official general Fabric example uses `modImplementation`; do not mechanically introduce a different convention into this 26.2 project, which already uses `implementation` and separately provisions production test mods. Keep runtime GeckoLib out of common. Each loader jar already contains GeckoLib's common classes and its own platform service implementations. **Do not shade GeckoLib, include the common jar in the release mod, or install both common and loader jars.** Install our mod plus the matching external GeckoLib loader jar on clients **and on our dedicated servers**. Update both loader metadata, production GameTest provisioning, README/install instructions and `CREDITS.md` together.

Fabric's published POM requests Fabric API `0.158.0+26.2`, but its actual mod metadata requires **`>=0.152.1+26.2`**, Fabric Loader **`>=0.19`**, Java **`>=25`**, Minecraft **`>=26.2`**. This project's `0.156.0+26.2` API / Loader `0.19.3` meets those declared minimums. The exclusion above preserves the project's existing API pin rather than silently updating it. NeoForge 5.5.6 declares NeoForge **`[26.2.0.57,)`**; our `26.2.0.88` meets that minimum. These are metadata observations, not a runtime compatibility test.

Declare our dependency as required on BOTH sides: Fabric `depends.geckolib` at least 5.5.5 (within GeckoLib 5); NeoForge a required `geckolib` dependency with range `[5.5.6,6)` and `side="BOTH"`. Keep exact development pins; install documentation should name the exact matching jars. GeckoLib itself advertises client-only/server-optional on Modrinth, but that does **not** make it optional for our server once our registered entities implement `GeoEntity`.

**Compile-time interface injections:** GeckoLib's runtime mixin makes `EntityRenderState` implement `GeoRenderState`. The common Gradle publication also has an `interfaceInjectionDataElements` variant, and its NeoForge wiki offers `interfaceInjectionData` for **ModDevGradle**. This project uses **Architectury Loom**, not MDG: do not paste that configuration blindly. For shared rendering code, use our own explicitly typed `CharacterGeoRenderState implements GeoRenderState` with a ticket-map implementation (or composition with `GeoRenderState.Impl`), and explicitly declared enemy render states `extends LivingEntityRenderState implements GeoRenderState`. Where reading vanilla injected states, cast through `Object` to `GeoRenderState`; do not assume the common compiler sees a patched vanilla superclass. This avoids requiring a separate compile-time injection convention just for our code. The runtime mixins still come from the external loader jar.

### Licence and assets

GeckoLib's inspected licence is **MIT**, copyright `(c) 2026 GeckoLib`: private use and shipping it as a dependency are permitted; copies/substantial portions must retain the copyright and permission notice. The external jars include `LICENSE.txt`. Record its coordinate, version, source and MIT attribution in credits. No copyleft requirement forces this project to change its own licence.

This says nothing about rights to Genshin characters, the purchased map or third-party `.ysm` assets. Preserve the brief's private-use/legal-review restriction. Author original cuboid models/textures/animation here, cite public visual references, do not decrypt YiFang models or import extracted game assets. An owner-supplied model still needs provenance/permission review before inclusion.

## 2. Player replacement: shared Avatar render-state hooks plus GeoObjectRenderer

### Recommendation

Use **an attached `GeoObjectRenderer` for the character body and first-person arms**, inserted through shared client-only mixins at the actual 26.2 extraction/submission boundaries. Keep vanilla `AvatarRenderer` registered, so its player/mannequin dispatch, culling, name extraction and skin-model selection remain intact outside the managed character path. Replace the **managed player's model submission**, not the entity or the entire dispatcher.

Why not the alternatives:

- A feature/layer alone adds geometry but leaves the vanilla player underneath. It is not replacement.
- `GeoReplacedEntityRenderer<T,E,R>` is a real API and useful for ordinary non-Gecko entities. But it extends `EntityRenderer`, not `AvatarRenderer`. In 26.2, the player's renderer maps and `getPlayerRenderer(AbstractClientPlayer)` are specifically typed to `AvatarRenderer`, and first-person arm code calls its `renderRightHand`/`renderLeftHand`. Registering a generic replaced renderer for `EntityTypes.PLAYER` does not solve either player dispatch or first-person hands.
- A custom `AvatarRenderer` subclass delegating to a geo renderer could work, but replacing the player maps on each resource reload plus loader-specific player registration is more machinery than the shared state hooks, and still needs a first-person path.
- Never mix `GeoEntity` into the player merely to obtain rendering: an attached client-only `GeoAnimatable` is sufficient and leaves server player construction untouched.

### Concrete 26.2 classes and hook methods

These names were obtained from the mapped classes in **`~/.gradle/caches/fabric-loom/26.2/minecraft-client-only.jar`** with `javap -p`/`-c`. The local NeoForge `*-sources.jar` existed but was an empty 22-byte ZIP, so these are binary signature/bytecode observations, not fabricated decompiled Java. **There is no `PlayerRenderer` / `PlayerRenderState` in this inspected path: the current names are `AvatarRenderer` / `AvatarRenderState`.**

| Class | Relevant method / field | Use |
| --- | --- | --- |
| `net.minecraft.client.renderer.entity.player.AvatarRenderer<A extends Avatar & ClientAvatarEntity>` | `extractRenderState(A, AvatarRenderState, float)` | RETURN injection to attach our frozen character render state, only if the actual entity is `AbstractClientPlayer` |
| `net.minecraft.client.renderer.entity.state.AvatarRenderState` | `id`; inherited `bodyRot`, `yRot`, `xRot`, movement/death/visibility/light fields | Mix in a nullable holder for our already-extracted geo body state; no live player reference |
| `net.minecraft.client.renderer.entity.LivingEntityRenderer` | `submit(LivingEntityRenderState, PoseStack, SubmitNodeCollector, CameraRenderState)` | Cancellable HEAD injection, gated by our state holder; **AvatarRenderer inherits this method** |
| `net.minecraft.client.renderer.entity.EntityRenderer` | `submit(EntityRenderState, PoseStack, SubmitNodeCollector, CameraRenderState)` | Preserve its base name/leash submission after substituting our body |
| `net.minecraft.client.renderer.entity.EntityRenderDispatcher` | `extractEntity(Entity,float)`; `submit(EntityRenderState,CameraRenderState,double,double,double,PoseStack,SubmitNodeCollector)` | Existing extraction-to-submission pipeline; no replacement required |
| Same dispatcher | `getRenderer(Entity)`, `getRenderer(EntityRenderState)`, `getPlayerRenderer(AbstractClientPlayer)`; `playerRenderers`, `mannequinRenderers` | Explains why a generic replaced renderer is not a drop-in player registration |
| `net.minecraft.client.renderer.ItemInHandRenderer` | `submitHandsWithItems(float,PoseStack,SubmitNodeCollector,LocalPlayer,int)` | Cancellable HEAD injection for managed first-person character hands |
| Same hand renderer | private `renderPlayerArm(PoseStack,SubmitNodeCollector,int,float,float,HumanoidArm)`; private `submitArmWithItem(AbstractClientPlayer,float,float,InteractionHand,float,ItemStack,float,PoseStack,SubmitNodeCollector,int)` | Vanilla's separate arm/item paths; do not hook only the empty-hand branch |
| `AvatarRenderer` | `renderRightHand(PoseStack,SubmitNodeCollector,int,Identifier,boolean)`, `renderLeftHand(...)` | Vanilla alternatives; neither takes character/action state |
| `net.minecraft.client.renderer.GameRenderer` | `extract(DeltaTracker,boolean)`; private `renderItemInHand(CameraRenderState,float,Matrix4fc)`; public `gameRenderState()` | Extract our first-person frozen geo state at RETURN of `extract`; vanilla keeps its existing hand render pass/camera transforms |
| `net.minecraft.client.renderer.state.GameRenderState` | `levelRenderState` | Public path to current extracted frame |
| `net.minecraft.client.renderer.state.level.LevelRenderState` | `cameraRenderState` | Pass the real `CameraRenderState`, not a fabricated default camera |

Use exact non-bridge descriptors in mixins. In particular the erasure of the real Avatar extraction method's first parameter is **`net.minecraft.world.entity.Avatar`**, not `AbstractClientPlayer` and not `LivingEntity`; its descriptor is `(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V`. The inherited submit descriptor starts with `LivingEntityRenderState`. Keep all these mixins in the existing shared **client** mixin configuration.

### Third-person extraction and submission

Proposed concrete client classes, under the existing `client` package:

- `PlayerVisuals`: cache of committed public player visual state, keyed by actual player identity and cleared on disconnect/level change/unmanage/respawn; one owned animatable cache per visible player, not a singleton per character.
- `CharacterAnimatable implements com.geckolib.animatable.GeoAnimatable`: fixed character ID for one cache generation, `registerControllers(AnimatableManager.ControllerRegistrar)`, `getAnimatableInstanceCache()`. Use `GeckoLibUtil.createInstanceCache(this, false)` for this per-player object. Drop/recreate that player's object on a character change so a new skin cannot inherit an old character's cached `RawAnimation`/timeline.
- `CharacterGeoModel extends GeoModel<CharacterAnimatable>`: precomputed identifiers for model/texture/animation resources. The first two getters use the extracted state's character ID; **`getAnimationResource` takes the animatable, not the render state**, so the fixed ID in the wrapper must agree with the same extraction.
- `CharacterGeoRenderer extends GeoObjectRenderer<CharacterAnimatable, CharacterRenderInput, CharacterGeoRenderState>`: immutable related input; `getInstanceId` explicitly returns a stable player/view ID; `addRenderData` copies visual/action/movement values into tickets; `adjustRenderPose` owns character transforms.

At `AvatarRenderer.extractRenderState` RETURN:

1. Clear the holder first; decline unmanaged players, spectators and mannequins. Local players use the server-accepted active slot, not a key press; remote players use the public appearance/action snapshot.
2. Capture position-independent data: active character, action occurrence/start/phase, committed traversal mode/wall orientation, movement speed, interpolated body/head rotations, native scale, visibility/glowing, hurt/death, packed light/overlay and partial tick. Do not retain a `Player`, `ClientLevel`, or mutable global character lookup for deferred rendering.
3. Call the geo renderer's `createRenderState(animatable,input)` and **`fillRenderState(animatable,input,state,partialTick)` during extraction**, storing the resulting geo state in the Avatar holder. This performs controller extraction now, consistent with GeckoLib 5.
4. Keep vanilla name/culling/fire/shadow extraction. Active characters use one unchanged player hitbox/eye height for this slice; models fit it. This is a declared visual scaling adaptation, not new character-specific collision mechanics.

At the real `LivingEntityRenderer.submit` HEAD, if the holder is populated:

1. Submit `CharacterGeoRenderer.performRenderPass(geoState,poseStack,collector,cameraState)` **using the prefilled state overload** inherited from `GeoRenderer`.
2. Call the base **`EntityRenderer.submit`** exactly once to preserve name/leash work, then cancel the living renderer's vanilla model/layers. A mixin whose actual superclass is `EntityRenderer` can call `super.submit`; a normal virtual call to `this.submit` would recurse. The inspected base bytecode submits leash states and invokes `submitNameDisplay` virtually, so Avatar's name handling still applies.
3. Do not run the vanilla player body, armor, cape, elytra, held-item, arrow or parrot layers on the managed character. Their transforms target vanilla bones. The original character model supplies its outfit, weapon and glider bones. Nothing changes for unmanaged players or other living renderers.

Do **not** call GeoObjectRenderer's `(animatable,relatedObject,...)` convenience overload at this third-person submission point: it re-extracts from mutable objects too late. Override its default **`adjustRenderPose`**: that default adds `(0.5,0.51,0.5)` for general objects, which is wrong at a player's feet. Our model has a foot-origin and 16 model units per block; apply native scale and `Axis.YP.rotationDegrees(180-bodyRot)` as appropriate for the Bedrock forward convention, with a small explicit ground offset only if needed. Do not also apply vanilla's `scale(-1,-1,1)` / `translate(0,-1.501,0)`; those are for vanilla `PlayerModel`. Use the captured body yaw from `ManagedCamera`/vanilla state, never orbit-camera yaw. Climb faces the synced wall normal; glide uses its own pose rather than vanilla elytra transforms.

Object rendering does not automatically inherit entity visibility handling. Copy and implement the relevant policy: ordinary entity cutout, invisible-to-viewer no body, teammate-visible invisibility translucent tint, outline/glowing where required, and red hurt overlay from captured state. Preserve the existing `CombatFeedback` character nameplate rather than introducing a second character label. Culling bounds must include an opened original glider: geo `visible_bounds_*` alone does not update vanilla player culling; enlarge this managed render path's `LivingEntityRenderer.getBoundingBoxForCulling` result for the wing span without changing the authoritative hitbox.

### First-person arms

Replace the **entire managed `submitHandsWithItems` branch**, not just `AvatarRenderer.renderRightHand`. The latter alone misses occupied-hand branches and cannot show our two-handed bow poses.

At `GameRenderer.extract(DeltaTracker,boolean)` RETURN, capture a first-person `CharacterGeoRenderState` from the same committed local visual state, only for managed first-person play with a real local camera player. Store this frame's arms snapshot in an owned client render holder. At the hand renderer HEAD, use that snapshot, the provided light/pose/collector, and `Minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState`; cancel vanilla hands/items only when the character snapshot is present. No entity/network reads belong inside deferred geometry callbacks.

Reuse the **same geometry and texture** in an arms-only pass, with its own view/cache ID (or separate per-player arms animatable) and the same authoritative action clock. In `adjustModelBonesForRender(RenderPassInfo,BoneSnapshots)`, hide head/hair, legs, torso surfaces, cape and wings using `BoneSnapshot.skipRender(true)` / `skipChildrenRender(true)`. Preserve arm ancestors' transforms: if the arms descend from the torso, hide **torso cubes only**, not all torso children. Render both arm chains plus the character's weapon/catalyst bones; do not hide an arm ancestor and expect its hand to remain visible. Apply a camera-local arms placement, not body yaw or a world-foot translation; compensate the character's eye height and author first-person-specific hand-root pose offsets where a third-person swing would pass behind the camera. Existing vanilla view-bob/hurt transforms remain in `GameRenderer`'s hand pass.

Character switching updates both body and arm caches in the same accepted snapshot, including empty hotbar slots; do not depend on possessing a sword/bow/catalyst item. Vanilla block editing is performed managed-off as already documented. First-person is an original arms view of the same kit actions; it does not reveal the full local body/head. Remote players are rendered as their full characters in each client's world; each remote player's own client sees that character's arms locally.

## 3. Hilichurl and Baron Bunny entities

### Hilichurl

Keep `enemy.Hilichurl extends PathfinderMob`, AI, HP, camp/leash, same-tick combat ordering and entity ID. Add **`implements GeoEntity`**, one cached `GeckoLibUtil.createInstanceCache(this)`, and controller registration. Replace the current `HilichurlRenderer extends HumanoidMobRenderer` implementation with `GeoEntityRenderer<Hilichurl,HilichurlGeoRenderState>` plus `HilichurlGeoModel`. Override **`createRenderState(Hilichurl,Void)`**, not just the old no-argument method; GeckoLib dynamically creates states during extraction. Use `addRenderData(Hilichurl,Void,state,partialTick)` for our visual tickets. Keep the existing loader registration name `HilichurlRenderer::new` to avoid two renderer conventions.

Author original mask, horns, fur/body/limbs and **a visible club** as a bone, replacing the vanilla shovel stand-in. Keep current gameplay attributes. Remove obsolete humanoid geometry/raised-arm model code and its unused old texture only after cutover. `isWindingUp()` currently provides a Boolean, not its start phase; extend synced entity visual data to contain an action occurrence and start-world-frame. Publish telegraph at the actual AI wind-up start, strike/recovery at the actual strike decision, cancel on leash/unmanage/Freeze cancellation, and never trigger a strike merely because a local animation reaches its marker. Current telegraph is **10 ticks = 30 reference frames = 0.5 s**; current recovery is **30 ticks = 90 frames**. Match those adapted values, not invented Genshin enemy frame data. Frozen must stop locomotion/appropriate pose and cannot replay a consumed attack on thaw.

### Baron Bunny

The current registered type is `EntityType<Rabbit>` with `Rabbit::new`, `.noSave().noSummon()`, and both loaders register `RabbitRenderer`. An ordinary Rabbit cannot gain `GeoEntity` simply by choosing a new renderer.

Use a concrete **`BaronBunny extends net.minecraft.world.entity.animal.rabbit.Rabbit implements GeoEntity`**. Keeping Rabbit as the superclass preserves current health/attribute/taunt interactions while changing the factory to `BaronBunny::new`; its original cuboid model supplies the puppet silhouette instead of a real rabbit's skin. Keep no-AI, no-save, no-summon, no-loot and the same entity registry ID. The alternative of changing the superclass to PathfinderMob is unnecessary for this slice.

Migrate `GenshinEntities.BARON_BUNNY` to `EntityType<BaronBunny>`, `CombatRuntime`'s spawn/owned reference/taunt local variables, both loader attribute registrations and renderer registrations to the concrete type. Use `BaronBunnyRenderer extends GeoEntityRenderer<BaronBunny,BaronBunnyGeoRenderState>` and a `BaronBunnyGeoModel`. Update Bunny lookups in `AmberGameTests`, `CombatLifecycleGameTests` and `HilichurlGameTests` to filter the registered Bunny type/concrete class rather than accidentally including ordinary world rabbits; keep the ordinary-rabbit save/reload control as an ordinary Rabbit. Do not change HP inheritance, expiry, enemy damage, cast ownership or explosions.

Current Bunny rendering doubles the Rabbit scale with `ADAPTED_BUNNY_MODEL_SCALE=2`. Decide **one** authored size: preserve that attribute multiplier while authoring half-size geometry to match the current effective silhouette/hitbox, or remove only that visual multiplier and author geometry at the intended final size. Do not both double new full-size geometry and leave the old multiplier unnoticed. The actual registered dimensions/health remain gameplay authority.

Clips: landed idle/taunt bounce, optional walking/hopping when actually moved, hurt, and death/explode. Bunny is passive: **do not manufacture telegraph/strike gameplay** because the task's enemy animation list mentions them. Its explosive warning can be a visual telegraph near its existing expiry, driven by that expiry; explosion/death is the actual end event.

**Instant removal trap:** `CombatRuntime.puppet` currently calls `discard()` on explosion immediately; a `triggerAnim("death")` followed by discard cannot visibly play death. Preserve instant server retirement/damage. Send a terminal Bunny visual snapshot (position/yaw/texture/action start) to its current tracking clients **before** removal, and render a short client-only `GeoObjectRenderer` death/explode continuation (e.g. 12 authored frames) at that captured position. It has no entity, hitbox, AI, damage or taunt and is cleared on level change/unmanage. Ordinary cast cancellation/wipe cleanup does not spawn an explosion animation. This is a presentation continuation, not a server entity kept alive to delay gameplay. Nonlethal `puppetHit` also needs an explicit hurt visual event: it mirrors HP with `setHealth`, so a vanilla damage animation is not guaranteed.

Hilichurl's vanilla death entity remains renderable for its normal death interval; author its death clip within that interval (normally up to 20 ticks/60 frames). Override GeckoLib's default death flip if an authored collapse already rotates the whole body, to avoid double rotation.

## 4. Animation state and 60-fps timing

### Existing sync is insufficient for actions

Observed current state:

- `CharacterStatePayload` contains private local resources, active slot, traversal flags and **actionBlocked/switchBlocked Booleans**, but no action ID or start frame.
- `PlayerCharacterPayload` contains only `(playerId,slot)` and drives other players' nameplates.
- `CharacterKit.comboIndex()` is the **next** normal index after acceptance, not an animation occurrence/current N index.
- Some attacks transition later in `EventTimeline` callbacks (sword charges; Amber aim; tap/hold releases). Inferring those solely from the original input produces incorrect visuals.

Add a small plain-Java visual action record to the existing kit/server state, written at **actual accepted transitions**, not a second combat simulator. Proposed fields:

```text
characterId
currentActionId              // N1..N5 (Lisa only N1..N4), charged, aim, skill phases, burst, etc.
actionOccurrence            // increasing per-player serial, even if the same clip repeats
actionStartFrame            // actual timeline start, not packet receipt
phaseStartFrame             // release/charge phase when distinct from cast start
visualEndFrame              // finite recovery end, or explicit held phase
locomotion/traversal mode + start + wall/side basis
```

Record character-specific transitions in `TravelerAnemoKit`, `NormalAttackKit`, `AmberKit`, `KaeyaKit`, `LisaKit`, and movement transitions in `CombatRuntime`/Traversal. Keep rules layer free of GeckoLib/Minecraft/client classes. A held skill uses separate start/hold/release clips; the release phase uses its own start while retaining the original cast frame for particle/kit references. Persistent field effects do not lock the player in the casting animation for their full lifetime.

Cleanly replace/extend the existing **public** `PlayerCharacterPayload` into a public player visual snapshot carrying active slot, action record and traversal flags, with `sampleFrame` and `sampleGameTime` for clock alignment. Feed local and tracking clients from the same committed visual state. Keep private HP/energy/cooldowns/stamina in `CharacterStatePayload`; do not send the whole party to other players. Update its receivers, `CombatFeedback` nameplate storage, codec tests, tracking snapshot, self snapshot, switch broadcast and lifecycle resets together. Do not retain an obsolete appearance-only payload alias.

For players, existing authoritative kit time uses `Frames.atServerTick(server.getTickCount())`: **three reference frames per tick**. Client `tickCount` is not that global server clock. Map samples into the client world's synchronised game-time domain:

```text
renderFrame = sampleFrame + 3 * (clientWorldGameTime + partialTick - sampleGameTime)
elapsedSeconds = max(0, renderFrame - phaseStartFrame) / 60
```

Capture that result during extraction. Dimension change invalidates the old clock anchor/cache. The world game-time estimate is still subject to normal Minecraft time sync/interpolation/latency; do not claim zero-latency phase agreement. The important properties are no restart on receipt, recovery by snapshot after entering tracking range, and a common explicit time domain. Entity visual data can directly store `startWorldFrame = 3*level.getGameTime()`; do not compare that against global kit-server frames without conversion.

### Controllers, not a second trigger network

Recommend **client controllers driven by the server's committed visual snapshot**, using `AnimationTest.getData(DataTicket)`/`getDataOrDefault`, cached `RawAnimation` constants and `setAndContinue`. Do not make a render predicate read changing server/client session objects.

- Base locomotion controller: idle/walk/run, jump/fall/land, climb/climb-jump/mantle, glide start/loop/stop, dash as appropriate; mostly inferred continuous speed from extracted vanilla movement, but traversal/action transitions must come from synced state. Use short authored blends for loops.
- Action controller registered after locomotion: N1..N5/N1..N4, charged, aim, tap/hold/start/release, burst, dash and death where these override the body. Zero controller transition ticks for kit-anchored full-body actions; author the wind-up inside the clip. Key all intended full-body bones so an old walk cycle cannot leak into a sword recovery. Explicitly choose whether locomotion remains active for movement-permitted upper-body casts.
- Hurt may be a small additive recoil unless server gameplay actually interrupts the action. Do not add new poise/stagger cancellations under an animation ticket. Death/forced replacement resets old actions; the new active character appears immediately when authoritative switching occurs.

The pinned API is `new AnimationController<>("actions", 0, test -> ...)` (there is **no animatable constructor argument** in these inspected sources). Cache `RawAnimation.begin().thenPlay(...)`, `.thenLoop(...)` or appropriate held final stages; never allocate them every render frame.

**Seeking caveat, important for late tracking:** GeckoLib 5.5.5/5.5.6 expose `AnimationController.setAnimationTime(double)` and `setTimelineTime(double)` in seconds. However, source inspection shows `initializeNewAnimation` unconditionally overwrites `timelineTime` with zero (unless triggered), so calling `setAnimationTime` inside the state handler on the first frame alone does **not** correctly initialise a late animation. Also `setAnimationTime` changes the timeline time, not the baked `AnimationPoint` immediately. Repeated submissions with zero age delta can skip ordinary progression.

Use one small version-pinned client adapter **`SyncedActionController extends AnimationController<CharacterAnimatable>`** for phase-critical clips (a generic equivalent may serve enemies). Override protected `checkControllerState(...)`: run the normal handler/initialisation via `super`, then, if the snapshot says this clip is active and a timeline exists, set the absolute elapsed time and rebuild protected `animationPoint` using `timeline.createAnimationPoint(timelineTime, null, easingOverride)` before returning `isAnimatingBones()`. Clear `transitionFromPoint` for these zero-transition actions. Compute modulo the clip length for explicit loop phases; clamp one-shots to their authored final pose until the server action ends. Reset on `(character,actionOccurrence,phase)` change even if the `RawAnimation` name is unchanged. This corrects **the first extracted frame**, culling/re-entry and repeated world/arms/shadow passes without inventing catch-up speed. This deliberately touches a protected internal extension point, so pin the versions and add a focused first-frame/seek regression; do not scatter reflective access or GeckoLib mixins across renderers.

Keep damage/projectile authority entirely in the existing server `EventTimeline`. For this absolute-seek path, avoid using GeckoLib's autonomous marker traversal as a second event clock. Presentation markers can be dispatched from deduplicated authoritative action/frame crossings, keyed by action occurrence; entering tracking range must not replay historical hit sounds/projectiles. Item 19's server projectile/sound work must not acquire a second duplicate launch via animation keyframes. Gecko keyframe handlers, if used for purely local decorations, must be cosmetic and tolerate seeks/repeated render passes.

### Triggerable animations: real API, not recommended as authority here

Actual APIs inspected:

- Register: `AnimationController.triggerableAnim("strike", RawAnimation...)`.
- Entity: `GeoEntity.triggerAnim(@Nullable String controllerName, String animName)`; server calls route through GeckoLib networking, client calls trigger the cached manager.
- Stop: `GeoEntity.stopTriggeredAnim(controllerName,animName)`.
- Replaced entity: `GeoReplacedEntity.triggerAnim(Entity relatedEntity,controllerName,animName)`.
- Local attached object: use its manager/controller's trigger methods if wanted; a plain `GeoAnimatable` is **not** a `GeoEntity` and has no entity `triggerAnim` method.

Triggers are convenient for unscheduled one-off cosmetic events. Their packet event is not a durable action snapshot with our kit start frame; it adds a second network timeline and cannot by itself reconstruct an already-running held skill for a newly tracking player. Therefore **do not use server `triggerAnim` as the primary player/enemy animation transport**. Use committed snapshots and occurrence-based local controller selection. It remains available for independent cosmetics where missed history is acceptable.

### Timings to consume, not rescale

All JSON animation timestamps are seconds: **`animation_length = recoveryFrames / 60.0`**, a hit/launch key at **`hitFrame / 60.0`**. Interpolate at render FPS, not by advancing one key per 20-TPS tick. Damage still occurs when the server drains its already-authored sub-tick timeline; animation cannot make the server run at 60 TPS. Keep speed 1 for attack clips, with any visual-only locomotion speed separate.

These values were read from the current kit implementations; where they say adapted, retain that label and their existing research/fidelity references:

| Character | Normal hit/release frames | Normal recovery frames | Additional anchors |
| --- | --- | --- | --- |
| Traveler/Aether | `13,13,16,30,25` | `23,32,40,49,81` | Charged starts after N1–N4 at `28,28,36,45`, then hits `+10,+21`, recovery `55`; skill tap storm `32`, recovery `74`; held storm `release+5`, recovery `release+48`; burst first tornado `96`, cast recovery `111` |
| Kaeya | `14,9,14,23,30` | `27,27,47,46,74` | Charged starts at `36,31,55,54`, both damage components at `+16`, recovery `54`; Frostgnaw hit `28`, recovery `53`; burst first contact `52`, recovery `77` |
| Amber | `14,10,27,26,26` | `26,22,37,34,60` | Aim enters at adapted frame `9`, full charge `86`, shot release no earlier than `15`, release recovery `10`; skill cast recovery `32` but Bunny lands at adapted `45`; burst first rain `72`, cast recovery `111` |
| Lisa | `26,17,17,31` (**four normals**) | `30,20,34,57` | Charged starts after N1–N3 at `31,24,40`, adapted hit `+58`, recovery `77`; tap launch `max(cast+17,release)`, cast recovery `38`; hold threshold `114` (`132` Cryo-slowed), hit `release+3`, recovery `release+27`; burst place `56`, formation adapted `59`, recovery `85`, first field discharge `119` |

For Traveler tap, held skill, Lisa tap and other phase-sensitive cases, author the pose path against the actual conditional kit schedule, not one universal E duration. Keep fields like Kaeya Waltz/Amber rain/Lisa Rose visualised independently after the cast recovery and party switching. Dash lasts the current authoritative six ticks = 18 frames; traverse/glide animation transitions must not spend stamina or change velocity themselves. Unmeasured locomotion/land/climb/glider clip durations are original presentation choices recorded separately from researched Genshin timing.

Expose/centralise existing kit timing data as small plain-Java descriptors consumed by the generator/test oracle; do not copy private frame arrays into unrelated renderer code or rewrite kit behaviour to match attractive animation.

## 5. Deterministic asset authoring without a GUI

Yes. GeckoLib loads JSON geometry and animation plus ordinary PNG textures; Blockbench is an editor/exporter, not a required runtime authoring tool. The pinned source parsers are `com.geckolib.loading.definition.geometry.Geometry.GSON` and `...animation.ActorAnimations.GSON`.

### Runtime paths — GeckoLib 5, not GeckoLib 4

Use shared resources, for example:

```text
common/src/main/resources/assets/genshininminecraft/
  geckolib/models/character/aether.geo.json
  geckolib/models/character/amber.geo.json
  geckolib/models/character/kaeya.geo.json
  geckolib/models/character/lisa.geo.json
  geckolib/animations/character/aether.animation.json
  geckolib/animations/character/amber.animation.json
  geckolib/animations/character/kaeya.animation.json
  geckolib/animations/character/lisa.animation.json
  geckolib/models/entity/hilichurl.geo.json
  geckolib/models/entity/baron_bunny.geo.json
  geckolib/animations/entity/hilichurl.animation.json
  geckolib/animations/entity/baron_bunny.animation.json
  textures/entity/character/{aether,amber,kaeya,lisa}.png
  textures/entity/{hilichurl,baron_bunny}.png
```

`GeckoLibResources` scans **`geckolib/models` and `geckolib/animations`**, strips those prefixes and `.geo.json` / `.animation.json` suffixes. Thus our model/animation getters return **`genshininminecraft:character/aether`** (or `entity/hilichurl`), while texture getters return the full **`genshininminecraft:textures/entity/character/aether.png`**. Do not return an old `geo/aether.geo.json` location or put new assets in GeckoLib 4's old root `geo/` and `animations/` folders.

### Minimal geometry schema example

This is a schema illustration for one arm, not a proposed complete character model:

```json
{
  "format_version": "1.12.0",
  "minecraft:geometry": [{
    "description": {
      "identifier": "geometry.genshin.aether",
      "texture_width": 128,
      "texture_height": 128,
      "visible_bounds_width": 3,
      "visible_bounds_height": 3,
      "visible_bounds_offset": [0, 1.25, 0]
    },
    "bones": [
      {"name": "root", "pivot": [0, 0, 0]},
      {"name": "right_arm", "parent": "root", "pivot": [-5, 22, 0],
       "cubes": [{"origin": [-7, 10, -2], "size": [4, 12, 4], "uv": [0, 0]}]}
    ]
  }]
}
```

Bedrock geometry uses `minecraft:geometry` with a description and named parented bones, cubes with origin/size/pivot/rotation/inflate/UV, degrees for rotation and **16 model units per block**. Author a foot-origin and positive Y upwards. GeckoLib converts Bedrock axes/degree rotations internally; do not pre-convert JSON to radians. Use one geometry definition per file because the inspected baker selects `definitions[0]`. `visible_bounds_*` is not an authoritative hitbox and does not itself fix MC player culling.

Use stable shared skeleton names: `root`, `hips`, `torso`, `head`, hair/accessory bones, `right_arm`, `right_forearm`, `right_hand`, matching left chain, upper/lower leg/foot chains, `weapon`, `cape`, `glider_left`, `glider_right`. Accessories may differ by character; hand and movement contracts stay stable. Cuboid knees/elbows, hair locks, garments, hat/ears/cape and weapons provide actual silhouettes, not only colour. Original cues: Aether's blond braid/white-gold asymmetric outfit; Amber's red/brown outfit and goggles/rabbit-ear headband; Kaeya's blue asymmetry, eyepatch and fur cape; Lisa's purple brimmed hat, hair, dress and catalyst. Author own pixel detail and blocky geometry; do not claim reproduction of source game models.

### Minimal animation schema example

Illustrative Traveler N1 anchors, 23-frame recovery and 13-frame strike:

```json
{
  "format_version": "1.8.0",
  "animations": {
    "attack.n1": {
      "loop": false,
      "animation_length": 0.38333333333333336,
      "bones": {
        "right_arm": {
          "rotation": {
            "0.0": [0, 0, 0],
            "0.1": [-70, 0, -25],
            "0.21666666666666667": [35, 0, 15],
            "0.38333333333333336": [0, 0, 0]
          }
        }
      }
    }
  }
}
```

Root `format_version`, named `animations` map; each clip has explicit length, loop flag or held-loop type, and per-bone `rotation`, `position`, `scale` channels. Channels can be constants or timestamp-to-vector keyframes; timestamps are seconds and rotations are degrees. Start with ordinary linear numeric values for parity. Optional `sound_effects`, `particle_effects` and `timeline` tracks need handlers; they do not execute Minecraft gameplay automatically. Pinned parser comments explicitly say Bedrock fields such as `anim_time_update`, `blend_weight`, `override_previous_animation`, `start_delay` and `loop_delay` are **not used by GeckoLib**. Do not attempt server synchronisation by putting a Molang expression into `anim_time_update`.

### UV layout and PNG generation

Choose one original 128×128 RGBA atlas per character and a smaller 64×64 or 128×128 enemy atlas. Prefer opaque/cutout pixels; avoid broad translucency and many material passes. For a cuboid with dimensions `(w,h,d)`, box UV unfolds within a rectangle of width `2*(w+d)` and height `h+d`; allocate non-overlapping deterministic rectangles with a one-pixel gutter. Alternatively specify six per-face entries `north/south/east/west/up/down`, each with `uv:[u,v]` and **`uv_size:[width,height]`** (and optional `uv_rotation`), which makes costume-detail painting explicit. These coordinates are texture pixels; the geometry description's atlas dimensions must match the actual PNG. Check top/bottom orientation with an asymmetrical authored test patch; do not assume all cube faces orient identically. Avoid coplanar clothing by deliberate tiny inflation/offsets, not multiple duplicate body meshes.

### Proposed authoring pipeline

1. Add one durable **`tools/models/generate.py`**, compact authored numeric geometry/key-pose data and the kit timing descriptors. Python standard library `json`, `struct`, `zlib` and deterministic raster painting are enough for JSON and an RGBA PNG; Pillow is optional, not a build prerequisite. Java data generation is also viable, but the Python tool is simpler for editing cuboids/texture pixels without touching runtime code.
2. Generate all six models, their **complete required** locomotion/action clips, and procedural pixel-art textures to the shared resource paths. Stable bone/key ordering, sorted numeric timestamps, fixed seeds, no timestamps/machine paths/UUID randomness. Convert frame numbers only at serialization with `frame/60.0`, including first/last keys and animation length.
3. Commit source definitions **and generated runtime assets**. Ordinary Gradle builds must not run Python or require Blockbench. The generator's `--check` compares expected bytes to checked-in outputs; a separate Java asset test uses GeckoLib's actual parsers to bake JSON and requires every intended bone/clip to exist, rather than treating a library warning/missing model as success.
4. Validate generator inputs for parent cycles/duplicate bones, UV bounds/atlas dimensions, finite values, key ordering, required named clips, action length/hit/recovery anchors and original provenance. Exercise pixel silhouettes and motion with screenshots/short recordings before declaring 20a/b/c complete. Programmatic authorship is not an excuse for plain untextured cuboids, empty clips or a single swing reused for every attack.
5. Later accept an owner-authored/permitted **GeckoLib Blockbench `.bbmodel`** as editable source for a particular character. Export its `.geo.json`, `.animation.json` and PNG using the GeckoLib plugin into the **same runtime paths/IDs**, preserving our required skeleton/clip contract and frame anchors. `.bbmodel` itself is not a runtime format. Put it under an authored-source directory, mark that model as manual/exported in the generator's small manifest, and stop generating **that** asset; never overwrite the owner's export on the next generator run. Other characters can remain generated. A new skeleton requires a deliberate matching animation/arms-bone contract update, not a silent partial import.

## 6. Risks and mitigations

| Risk | Concrete consequence and mitigation |
| --- | --- |
| GeckoLib 5 API/documentation drift | Some current wiki examples retain old constructor shapes. Follow the pinned sources/binary signatures, `com.geckolib` imports, new resource directories, `addRenderData`, `BoneSnapshots` and submission overloads. Version-pin the seek adapter; no reflection or old `setCustomAnimations` recipes. |
| Sodium / Iris | Standard cutout `RenderTypes` and `SubmitNodeCollector.submitCustomGeometry` stay in the modern MC entity/hand pipeline, which is the correct interoperability surface **[INFERENCE, not a tested guarantee]**. No private GL state, custom shader or buffer flushing. Iris shader shadow passes may submit a player more than once; absolute immutable frame snapshots prevent phase double-advance. Test arms, body, wings, outlines and reloads with/without shaders on both loaders. Do not call a missing character model a shader compatibility fallback. |
| Shader version-pair fragility | Iris/Sodium 26.2 have their own version-specific incompatibility reports (e.g. [Iris #3265](https://github.com/IrisShaders/Iris/issues/3265), closed; historical Fabric 26.2 Iris 1.11.2/Sodium 0.9.2 alpha issue). This is not evidence of a GeckoLib defect or a current universal incompatibility. Pin a tested visual-profile pair separately; the mod must remain fully usable without either. No new shader dependency. |
| Player renderer conflicts | Other player replacement mods may also cancel `LivingEntityRenderer.submit` or hand submission. Scope strictly to our populated Avatar state, preserve unmanaged path/mannequins, use descriptor-specific hooks with required injection counts, document incompatibility rather than globally disabling another renderer. No whole-dispatcher overwrite. |
| First-person-only gaps | Replacing third-person body alone leaves Steve's hand; replacing empty-hand alone leaves item branches. Own the complete managed hand branch and test all four characters with empty and occupied vanilla hands, bow/catalyst actions and F5 transitions. |
| Render-state/thread safety | Capture active character, immutable inputs and controller output during extraction. Deferred custom geometry must not reach into entity/session maps. Bone modifications belong in per-pass snapshots, never cached shared `GeoBone` state. Do not mutate already-enqueued snapshots when network packets arrive or when switching. |
| Performance | Bake geometry/animations once at resource reload; cache resource identifiers, `RawAnimation`s and per-player/per-entity managers. Low cuboid counts and one cutout atlas/pass per character; wings/accessories are bones, not extra duplicated renderers. No JSON parsing, model generation, streams or per-frame cache creation in render predicates. Use normal entity culling with managed wing-bound expansion. Remove player/view caches on lifecycle events to prevent an entity-ID reuse leak. 4 co-op players plus bounded camps is a modest target, but actual frame cost is unmeasured. |
| Dedicated-server classloading | Registered enemy entity classes and GeoEntity/cache interfaces are common/server-safe; renderers/models/BoneSnapshots/client caches/mixins stay in client paths. Do not put a static `Minecraft` reference, renderer factory or resource reload call in entity registration. Controller creation must occur through client animation-manager use, not server `getManagerForId` calls. Keep plain rules free of GeckoLib. GeckoLib's own server network/init services are loader-provided; install it on our server as required. Dedicated production GameTests are the gate for real classloading safety. |
| Loader differences | Same mapped vanilla hooks/assets/common API; only renderer registration, payload wiring and dependency/service bootstrap differ. Fabric `EntityRendererRegistry.register`; NeoForge `EntityRenderersEvent.RegisterRenderers`. Do not depend on loader-specific render-state extension APIs in the common renderer. Keep GeckoLib-specific Fabric/NeoForge event imports out of common. NeoForge 5.5.6's Molang fixes are not a reason to ship different combat timings/JSON. |
| Bunny removal / existing regressions | No-save/no-summon and instant server discard remain. Client death continuation is not a saved entity and cannot damage/taunt. Preserve same-tick Freeze/lethal-trade ordering and dimension/wipe cancellation tests while changing only visuals/types. |
| Art/fidelity/legal | Original blocky assets can be identifiable and animated, but exact Genshin geometry/poses are not established by this spike. Record authored/adapted motions and public reference provenance. Review assets visually with the owner; encrypted licensed third-party content is not a shortcut. |

## 7. Recommended implementation sequence and acceptance

### 20a — dependency, actual four-character body/arms and locomotion

1. Add the exact common/loader coordinates, BOTH-side mod metadata and external install/production-test provisioning. Keep loader adapters thin. Record MIT and original asset provenance.
2. Add public visual state/clock contract and cleanly migrate the existing appearance-only payload/caches. Authoritative slot/traversal snapshots must work for self, existing remote players and players entering tracking range; teardown on unmanage/respawn/dimension/disconnect.
3. Build `CharacterAnimatable`, model/object renderer, frozen states and the descriptor-specific Avatar/Living/first-person hooks. First use an original Aether model to establish foot transforms, camera-independent body yaw, name/visibility/culling and hands; complete all four assets **within this item**, not as a tinted fallback.
4. Deliver distinct original Aether/Amber/Kaeya/Lisa cuboid models/textures including weapons and deployable glider bones, with complete idle/walk/run/dash/jump/fall/land/climb/climb-jump/mantle/glide-start/loop/stop motion. The arms path renders the accepted active character in first person. No vanilla body or Steve arms leak through.
5. Main checks: both dedicated-server loaders join; both clients render local and remote swaps; empty/occupied-hand first person; orbit follows movement body yaw without rotating to camera; climbing/gliding poses; death/respawn; F5; managed-off restores vanilla; resource reload; disconnect/world change; opened wing culling. Require inspected screenshots/short motion clips for **each** character, not merely a successful launch.

### 20b — four kits' normal/charged/E/Q animation

1. Publish actual accepted visual actions/occurrences at every kit transition, including timeline-driven charges/aim and variable tap/hold release. Never derive current N from the already-incremented combo index or blocked Boolean.
2. Add the pinned absolute-time action-controller adapter and tests for first-frame late seek, repeated N clip/new occurrence, cancellation, culling/re-entry, hold/release, duplicate extraction/pass, resource reload and character swap. Author full action tracks from the kit timing table: five normals for the three five-hit kits, **four for Lisa**, plus all real charged/tap/hold/burst phases.
3. Match all hit/launch/recovery anchors in both JSON and live visual capture. Keep persistent field object effects independent of the player's casting clip; do not duplicate item 19 projectile launches/sounds. Test server-rejected input produces no authoritative attack animation and swapping cancels old-body actions at the existing legal window.
4. Main checks: timing/bone/clip asset tests plus action snapshot codec/rules tests; first-person and orbit recordings of full strings/E/Q for all four, including delayed packet/late tracker scenarios. A statically posed weapon is not attack-animation acceptance.

### 20c — original hilichurl and Bunny geometry/animation

1. Add `GeoEntity`/caches to Hilichurl and concrete BaronBunny, cleanly migrate factories/runtime owned references/loader attributes/renderers/tests, preserve entity IDs and gameplay boundaries. Replace the humanoid/shovel and RabbitRenderer assets only after the geo cutover.
2. Author hilichurl idle/walk/30-frame telegraph/strike/recovery/hurt/death and Bunny landed idle/taunt/moved/hurt/expiry-warning/death-explode clips. Add synced entity phase/occurrence fields and cosmetic terminal Bunny continuation, with no new Bunny attacks or server-lifetime delay.
3. Main checks: both loader renders and dedicated-server AI/health/no-save tests; original masks/club/Bunny silhouette visible; enemy wind-up matches actual damage timing; Freeze/cancel/thaw cannot replay a club; Bunny HP/taunt/lifetime/explosion ownership and no-save regression unchanged; death animation actually visible and cancellation leaves no ghost.

After all slices are integrated, **Main**, not this read-only spike, should run the repository gate **`./gradlew --no-daemon build runAllGameTests` once**, plus the dedicated two-client/NeoForge client visual checks and shader profile matrix described above. Update `docs/decisions.md`, `spec/fidelity.md`, credits/install docs and task acceptance evidence. No verification was run by this spike.

## Sources / reproducible API evidence

- [GeckoLib Fabric 26.2 Maven metadata](https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/com/geckolib/geckolib-fabric-26.2/maven-metadata.xml), [NeoForge metadata](https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/com/geckolib/geckolib-neoforge-26.2/maven-metadata.xml), [common metadata](https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/com/geckolib/geckolib-common-26.2/maven-metadata.xml).
- [Fabric 5.5.5 POM](https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/com/geckolib/geckolib-fabric-26.2/5.5.5/geckolib-fabric-26.2-5.5.5.pom), [common 5.5.5 POM](https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/com/geckolib/geckolib-common-26.2/5.5.5/geckolib-common-26.2-5.5.5.pom), [common Gradle variants](https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/com/geckolib/geckolib-common-26.2/5.5.5/geckolib-common-26.2-5.5.5.module).
- [Modrinth's 26.2 release inventory](https://api.modrinth.com/v2/project/geckolib/version?game_versions=%5B%2226.2%22%5D): Fabric release ID `7gaQHok7`, NeoForge ID `IEGPh4CJ`; downloaded jars `/tmp/geckolib-fabric-26.2-5.5.5.jar` and `/tmp/geckolib-neoforge-26.2-5.5.6.jar`, about 1.2 MB each. Their actual `fabric.mod.json` / `META-INF/neoforge.mods.toml` provided minimum loader requirements.
- **Pinned source artifact used for API behaviour:** [common 5.5.5 sources jar](https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/com/geckolib/geckolib-common-26.2/5.5.5/geckolib-common-26.2-5.5.5-sources.jar), downloaded to `/tmp/geckolib-common-26.2-5.5.5-sources.jar` (843,103 bytes). Inspected `GeoObjectRenderer`, `GeoReplacedEntityRenderer`, `GeoEntityRenderer`, `GeoRenderer`, `GeoRendererInternals`, `GeoRenderState`, `GeoAnimatable`, `GeoEntity`, `GeoModel`, `GeckoLibUtil`, `AnimationController`, `AnimationTest`, `AnimationTimeline`, `BoneSnapshots`, `BoneSnapshot`, `GeckoLibResources`, `Geometry` and animation/UV schema parsers. NeoForge 5.5.6 core signatures were inspected in its binary separately.
- [MIT licence](https://github.com/bernie-g/geckolib/blob/26.2/LICENSE); [new GeckoLib wiki](https://wiki.geckolib.com/docs/geckolib5/); [Fabric dependency docs](https://wiki.geckolib.com/docs/geckolib5/setup/fabric/adding-the-dependency); [NeoForge dependency/interface injection docs](https://wiki.geckolib.com/docs/geckolib5/setup/neoforge/adding-the-dependency).
- [GeckoLib 5 render-state conceptual changes](https://wiki.geckolib.com/docs/geckolib5/updating/important/conceptual-changes), [controller documentation](https://wiki.geckolib.com/docs/geckolib5/concepts/animation/controller/overview), [triggerable animation documentation](https://wiki.geckolib.com/docs/geckolib5/miscellaneous/triggerable-animations), [Blockbench plugin setup](https://wiki.geckolib.com/docs/geckolib5/setup/blockbench/the-plugin). Pinned source wins where an example constructor differs.
- [Bedrock geometry schema](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/schemasreference/schemas/minecraftschema_geometry_1.21.0?view=minecraft-bedrock-experimental) and [actor animation schema](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/schemasreference/schemas/minecraftschema_actor_animation_1.8.0?view=minecraft-bedrock-stable), as referenced by GeckoLib's parsers; the supported subset was checked in the actual parser source.
- Minecraft evidence: cached 26.2 mapped `minecraft-client-only.jar`, `javap -p`/`-c` on the classes enumerated in section 2, including base renderer submit bytecode and GameRenderState public camera path. No Minecraft source or jar was changed.
- Repository evidence: `AGENTS.md`; `docs/GENSHIN_MINECRAFT_BRIEF.md` Hard problem 2; `todo.md` 20a/b/c; common/Fabric/NeoForge Gradle files and versions; `PlayerCharacterPayload`, `CharacterStatePayload`, `CombatFeedback`, both loader initializers, `Hilichurl`, `HilichurlRenderer`, `GenshinEntities`, `CombatRuntime`, `Frames`, all four kits and the shared normal lifecycle; Bunny-related GameTests. These were observations of the live repository during another agent's build, not a claim of a frozen commit.
