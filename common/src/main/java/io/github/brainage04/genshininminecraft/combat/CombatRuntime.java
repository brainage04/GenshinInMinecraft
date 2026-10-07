package io.github.brainage04.genshininminecraft.combat;

import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import io.github.brainage04.genshininminecraft.network.DamageNumberPayload;
import io.github.brainage04.genshininminecraft.network.TargetAuraPayload;
import io.github.brainage04.genshininminecraft.network.PlayerCharacterPayload;
import io.github.brainage04.genshininminecraft.rules.*;
import io.github.brainage04.genshininminecraft.rules.kit.TravelerAnemoKit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit;
import io.github.brainage04.genshininminecraft.rules.kit.KaeyaKit;
import io.github.brainage04.genshininminecraft.rules.kit.AmberKit;
import io.github.brainage04.genshininminecraft.rules.kit.LisaKit;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.Identifier;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Hit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Kind;
import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** One simulation per server; loader hooks only forward intents, tick and send payloads. */
public final class CombatRuntime {
    public static final double SWORD_RADIUS = 3;
    public static final double SWORD_ARC_COSINE = .5; // 120 degree total arc.
    public static final double SKILL_RADIUS = 4;
    public static final double TORNADO_RADIUS = 3;
    public static final double ABSORBED_TORNADO_RADIUS = 1.5;
    public static final double TORNADO_BLOCKS_PER_SECOND = 2;
    public static final double SMALL_ENEMY_MAX_WIDTH = 1.4;
    public static final double SMALL_ENEMY_MAX_HEIGHT = 2.2;
    public static final double ADAPTED_DASH_BLOCKS_PER_TICK = .6;
    public static final int CHARACTER_SYNC_INTERVAL_TICKS = 3;
    public static final double ADAPTED_BOW_RANGE = 16;
    public static final double ADAPTED_CATALYST_RANGE = 6;
    public static final double ADAPTED_LISA_IMPACT_RADIUS = .75;
    public static final double ADAPTED_LISA_CHARGED_RADIUS = 4;
    public static final double ADAPTED_RAY_HITBOX_MARGIN = .15;
    public static final double ADAPTED_LISA_CHARGED_HEIGHT_TOLERANCE = .1;
    public static final double ADAPTED_FROSTGNAW_RANGE = 8;
    public static final double ADAPTED_FROSTGNAW_HEIGHT = 2.2;
    public static final double ADAPTED_ICICLE_ORBIT_RADIUS = 2.5;
    public static final double ADAPTED_ICICLE_CONTACT_MARGIN = .35;
    public static final double ADAPTED_BUNNY_THROW_DISTANCE = 3;
    public static final double ADAPTED_BUNNY_TAUNT_RADIUS = 8;
    public static final double ADAPTED_BUNNY_EXPLOSION_RADIUS = 4;
    public static final double ADAPTED_BUNNY_MODEL_SCALE = 2;
    public static final double ADAPTED_RAIN_RADIUS = 3.5;
    public static final double ADAPTED_RAIN_HEIGHT = 2;
    public static final double ADAPTED_RAIN_FORWARD_DISTANCE = 3;
    public static final double ADAPTED_OVERLOADED_RADIUS = 3;
    public static final double ADAPTED_OVERLOADED_KNOCKBACK = .12;
    public static final double ADAPTED_VIOLET_ACQUISITION_RADIUS = 16;
    public static final double ADAPTED_VIOLET_SPEED_PER_FRAME = .18;
    public static final double ADAPTED_VIOLET_IMPACT_RADIUS = 1;
    public static final double ADAPTED_CONDUCTIVE_BOUNCE_RADIUS = 1.5;
    public static final double ADAPTED_SUPERCONDUCT_RADIUS = 3;
    public static final double VIOLET_HOLD_RADIUS = 10;
    public static final double ADAPTED_VIOLET_HOLD_HEIGHT = 3;
    public static final double ROSE_RADIUS = 7;
    public static final double ROSE_IMPACT_RADIUS = 1;
    public static final double ADAPTED_ROSE_PLACEMENT_HEIGHT = .5;
    public static final double ADAPTED_ROSE_KNOCKBACK = .15;
    public static final int ADAPTED_ORB_SAMPLE_FRAMES = 3;
    private static final Identifier AIM_SPEED_ID = Identifier.fromNamespaceAndPath("genshininminecraft", "amber_aim");
    public static final double ADAPTED_AIM_SPEED_MULTIPLIER = .5;
    private static final AttributeModifier AIM_SPEED = new AttributeModifier(AIM_SPEED_ID,
            ADAPTED_AIM_SPEED_MULTIPLIER - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final DustParticleOptions ANEMO_DUST = new DustParticleOptions(0x80e8c0, 1.2F);
    private static final DustParticleOptions PYRO_DUST = new DustParticleOptions(0xff6622, 1.2F);
    private static final DustParticleOptions CRYO_DUST = new DustParticleOptions(0x99eeff, 1.2F);
    private static final DustParticleOptions HYDRO_DUST = new DustParticleOptions(0x3388ff, 1.2F);
    private static final DustParticleOptions ELECTRO_DUST = new DustParticleOptions(0xbb66ee, 1.2F);
    private static final Element[] ABSORPTION_PRIORITY = {Element.CRYO, Element.PYRO, Element.HYDRO, Element.ELECTRO};
    private static final Map<MinecraftServer, CombatRuntime> SERVERS = new HashMap<>();
    private static BiConsumer<ServerPlayer, CharacterStatePayload> sender;
    private final Map<UUID, Session> players = new HashMap<>();
    private final Map<UUID, CombatTarget> targets = new HashMap<>();

    private CombatRuntime() {}
    private final EventTimeline timeline = new EventTimeline();
    public static CombatRuntime get(MinecraftServer server) { return SERVERS.computeIfAbsent(server, ignored -> new CombatRuntime()); }
    public static void setSender(BiConsumer<ServerPlayer, CharacterStatePayload> value) { sender = value; }
    public static void stop(MinecraftServer server) {
        CombatRuntime runtime = SERVERS.remove(server);
        if (runtime != null) for (Session state : runtime.players.values()) state.clearFieldObjects();
    }
    public static boolean cancelsVanillaMelee(Player player) { return ManagedWorld.isManaged(player.level()); }
    public Session session(ServerPlayer player) {
        Session previous = players.get(player.getUUID());
        if (previous != null && previous.player != player) { previous.clearFieldObjects(); players.remove(player.getUUID()); }
        Session state = players.computeIfAbsent(player.getUUID(), ignored -> new Session(player));
        state.reconcileDimension();
        return state;
    }
    public CombatTarget target(LivingEntity entity) {
        if (entity instanceof Player) throw new IllegalArgumentException("Players are not kit targets");
        return targets.computeIfAbsent(entity.getUUID(), ignored -> new CombatTarget(entity));
    }
    /** Lookup only: vanilla movement must not create combat profiles or advance reaction timelines. */
    public static boolean isFrozen(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !ManagedWorld.isManaged(level) || !entity.isAlive()) return false;
        CombatRuntime runtime = SERVERS.get(level.getServer());
        CombatTarget target = runtime == null ? null : runtime.targets.get(entity.getUUID());
        return target != null && target.aura().isFrozen();
    }
    public void forget(UUID uuid) {
        Session removedPlayer = players.remove(uuid);
        if (removedPlayer != null) removedPlayer.clearFieldObjects();
        CombatTarget removed = targets.remove(uuid);
        if (removed != null) broadcast(removed, new TargetAuraPayload(removed.entity().getId(), 0, 0));
    }
    /** Replacing the profile invalidates queued EC callbacks as well as aura/ICD state. */
    public void resetTarget(LivingEntity entity) {
        forget(entity.getUUID());
        entity.setHealth(entity.getMaxHealth());
        syncAura(target(entity));
    }
    /** Nearest live puppet has priority over players; it cannot lure a camp past its leash. */
    public LivingEntity tauntTarget(Hilichurl enemy) {
        Rabbit nearest = null;
        double distance = ADAPTED_BUNNY_TAUNT_RADIUS * ADAPTED_BUNNY_TAUNT_RADIUS;
        for (Session session : players.values()) {
            Rabbit bunny = session.bunny;
            if (bunny == null || !bunny.isAlive() || bunny.isRemoved() || bunny.level() != enemy.level()
                    || !session.valid() || bunny.position().distanceToSqr(enemy.campAnchor()) > Hilichurl.LEASH_RADIUS * Hilichurl.LEASH_RADIUS)
                continue;
            double candidate = enemy.distanceToSqr(bunny);
            if (candidate < distance && enemy.hasLineOfSight(bunny)) { nearest = bunny; distance = candidate; }
        }
        return nearest;
    }
    public boolean puppetHit(LivingEntity puppet, LivingEntity enemy) {
        if (!ManagedWorld.isManaged(puppet.level())) return false;
        long frame = Math.max(timeline.frame(), Frames.atServerTick(puppet.level().getServer().getTickCount()));
        advanceTimeline(frame);
        if (!enemy.isAlive() || enemy.isRemoved() || isFrozen(enemy) || !puppet.isAlive() || puppet.isRemoved()
                || enemy.level() != puppet.level()) return false;
        for (Session session : players.values()) {
            if (session.bunny != puppet || !puppet.isAlive()) continue;
            AmberKit amber = (AmberKit) session.party.kit(1);
            // DEF snapshot chooses Amber's DEF and neutral RES; inheritance is an explicit adaptation.
            double damage = Damage.enemyDamage(HilichurlProfile.ADAPTED_ATK, HilichurlProfile.CLUB_MULTIPLIER,
                    enemy instanceof Hilichurl hilichurl ? hilichurl.genshinLevel() : TravelerAnemoKit.STARTER_LEVEL,
                    amber.stats().def(), HilichurlProfile.STARTER_PLAYER_RESISTANCE);
            amber.damagePuppet(damage, frame);
            if (session.bunny != null) session.bunny.setHealth((float) amber.puppetHp());
            return true;
        }
        return false;
    }
    private boolean isPuppet(LivingEntity entity) {
        for (Session session : players.values()) if (session.bunny == entity) return true;
        return false;
    }
    public boolean enemyHit(ServerPlayer player, LivingEntity enemy, double attack, double multiplier, int enemyLevel) {
        if (!ManagedWorld.isManaged(player.level())) return false;
        long frame = Math.max(timeline.frame(), Frames.atServerTick(player.level().getServer().getTickCount()));
        advanceTimeline(frame);
        if (!enemy.isAlive() || enemy.isRemoved() || isFrozen(enemy) || enemy.level() != player.level()
                || player.isCreative() || player.isSpectator() || !player.isAlive() || player.isRemoved()) return false;
        Session state = session(player);
        if (state.stamina().dashInvulnerable(frame)) return false;
        state.reconcileHealth();
        CharacterKit kit = state.kit();
        double amount = Damage.enemyDamage(attack, multiplier, enemyLevel, kit.stats().def(),
                HilichurlProfile.STARTER_PLAYER_RESISTANCE);
        ServerLevel level = (ServerLevel) player.level();
        var source = level.damageSources().mobAttack(enemy);
        if (player.isInvulnerableTo(level, source)) return false;
        applyCharacterLoss(state, amount, source, frame);
        return true;
    }
    /** Environmental max-HP loss bypasses DEF/RES/shields and dash protection. */
    public boolean fallDamage(ServerPlayer player, double height, DamageSource source) {
        if (!ManagedWorld.isManaged(player.level())) return false;
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) return true;
        Session state = session(player);
        long frame = Math.max(state.party.frame(), Frames.atServerTick(player.level().getServer().getTickCount()));
        advanceTimeline(frame);
        state.reconcileHealth();
        double horizontalSpeed = Math.sqrt(player.getKnownMovement().horizontalDistanceSqr()) * 20;
        double fraction = state.traversal.active() ? 0 : Traversal.fallHpFraction(height, horizontalSpeed,
                TraversalGeometry.safeWater(player));
        if (fraction > 0) applyCharacterLoss(state, state.kit().maxHp() * fraction, source, frame);
        return true;
    }
    private void applyCharacterLoss(Session state, double amount, DamageSource source, long frame) {
        ServerPlayer player = state.player;
        CharacterKit kit = state.kit();
        kit.setHp(Math.max(0, kit.hp() - amount));
        if (source.getEntity() instanceof LivingEntity enemy) player.setLastHurtByMob(enemy);
        player.getCombatTracker().recordDamage(source, (float) (amount * player.getMaxHealth() / kit.maxHp()));
        if (kit.hp() == 0) state.forceSwitch(frame);
        state.mirrorHealth();
        player.level().broadcastDamageEvent(player, source);
        if (state.kit().hp() == 0) player.die(source);
        state.sync();
    }
    /** Shared die hook also catches lethal vanilla/environmental damage before vanilla marks death. */
    public boolean handleDeath(ServerPlayer player) {
        if (!ManagedWorld.isManaged(player.level())) return false;
        Session state = session(player);
        state.kit().setHp(0);
        boolean replaced = state.forceSwitch(Math.max(state.party.frame(),
                Frames.atServerTick(player.level().getServer().getTickCount())));
        state.mirrorHealth();
        state.sync();
        return replaced;
    }
    /** Loader tracking callbacks refresh existing auras for players entering tracking range. */
    public void startTracking(ServerPlayer player, Entity entity) {
        CombatTarget target = targets.get(entity.getUUID());
        if (target != null && ManagedWorld.isManaged(entity.level()) && player.connection != null) {
            player.connection.send(new ClientboundCustomPayloadPacket(new TargetAuraPayload(entity.getId(),
                    target.auraElements(), target.conductive().stacks())));
        }
        if (entity instanceof ServerPlayer other && ManagedWorld.isManaged(entity.level()) && player.connection != null) {
            player.connection.send(new ClientboundCustomPayloadPacket(
                    new PlayerCharacterPayload(other.getId(), session(other).party.activeSlot())));
        }
    }
    private static void broadcast(CombatTarget target, CustomPacketPayload payload) {
        ((ServerLevel) target.entity().level()).getChunkSource().chunkMap.sendToTrackingPlayers(
                target.entity(), new ClientboundCustomPayloadPacket(payload));
    }
    private static void feedback(CombatTarget target, double amount, Element element, Reaction.Type reaction, boolean critical) {
        broadcast(target, new DamageNumberPayload(target.entity().getId(), (float) amount, element, reaction, critical));
    }
    private static void syncAura(CombatTarget target) {
        if (target.auraChanged()) broadcast(target, new TargetAuraPayload(target.entity().getId(),
                target.auraElements(), target.entity().isAlive() ? target.conductive().stacks() : 0));
    }
    public boolean receive(ServerPlayer player, Intent intent) {
        if (!ManagedWorld.isManaged(player.level()) || !player.isAlive() || player.isSpectator()) return false;
        var state = session(player);
        state.reconcileHealth();
        return state.intent(intent, Frames.atServerTick(player.level().getServer().getTickCount()));
    }
    /** Finite movement basis only; camera yaw never overwrites entity rotation or combat geometry. */
    public boolean receiveCameraYaw(ServerPlayer player, float yaw) {
        if (!Float.isFinite(yaw) || !ManagedWorld.isManaged(player.level()) || !player.isAlive() || player.isSpectator()) return false;
        session(player).cameraYaw = CameraMath.wrap(yaw);
        return true;
    }
    /** Entity AI runs before END_SERVER_TICK; earlier hitmarks must win before an enemy mutation. */
    private void advanceTimeline(long frame) {
        for (Session state : players.values()) state.reconcileDimension();
        timeline.advanceTo(frame);
    }
    public void tick(MinecraftServer server) {
        long frame = Frames.atServerTick(server.getTickCount());
        boolean managed = ManagedWorld.isManaged(server.overworld());
        // EC must drain before natural decay even when its owner has no ticking session.
        if (managed) advanceTimeline(frame);
        else for (Session state : players.values()) state.reconcileDimension();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!managed) {
                Session old = players.remove(player.getUUID());
                if (old != null) old.clearFieldObjects();
                if (old != null) {
                    old.broadcastCharacter(-1);
                    if (sender != null) sender.accept(player, CharacterStatePayload.UNMANAGED);
                }
                continue;
            }
            Session state = session(player);
            state.reconcileHealth();
            state.tickMovement(frame);
            state.sync();
        }
        players.values().removeIf(state -> {
            if (!state.player.isRemoved()) return false;
            state.clearFieldObjects();
            return true;
        });
        targets.values().removeIf(target -> target.entity().isRemoved());
        if (managed) {
            for (CombatTarget target : targets.values()) {
                if (frame >= target.aura().frame()) target.aura().advanceTo(frame);
                target.conductive().advanceTo(frame);
                syncAura(target);
            }
        }
        if (!managed) targets.clear();
    }

    public final class Session {
        private final ServerPlayer player;
        private ServerLevel sessionLevel;
        private long castGeneration;
        private final Party party;
        private final Random random;
        private final Random roseTargets;
        private final UUID[] combatOwners = new UUID[Party.SIZE];
        private CharacterStatePayload lastSync;
        private int rejectionSerial;
        private int rejectedIntent;
        private CharacterStatePayload.Rejection rejection = CharacterStatePayload.Rejection.NONE;
        private float mirroredHealth;
        private long skillCast = -1;
        private Element skillAbsorbed;
        private Element burstAbsorbed;
        private Vec3 tornadoOrigin;
        private Vec3 tornadoDirection;
        private ServerLevel tornadoLevel;
        private long tornadoStart = -1;
        private long lastTornadoStep = -1;
        private ServerLevel actionLevel;
        private ServerLevel icicleLevel;
        private ServerLevel puppetLevel;
        private Vec3 puppetLanding;
        private Rabbit bunny;
        private ServerLevel rainLevel;
        private Vec3 rainOrigin;
        private ServerLevel roseLevel;
        private Vec3 roseOrigin;
        private long lastRoseVisual = -1;
        private ServerLevel lastSyncLevel;
        private long lastSyncFrame;
        private long lastMovementFrame = -1;
        private boolean sprintKeyHeld;
        private float cameraYaw = Float.NaN;
        private Vec3 dashMotion;
        private ServerLevel dashLevel;
        private int displayedSlot = -1;
        private ServerLevel displayedLevel;
        private final Traversal traversal;
        private Direction climbWall;
        private boolean jumpKeyHeld;
        private boolean traversalJumpQueued;

        private Session(ServerPlayer player) {
            this.player = player;
            sessionLevel = player.level();
            random = new Random(player.getUUID().getLeastSignificantBits());
            roseTargets = new Random(player.getUUID().getMostSignificantBits());
            party = new Party(timeline, this::hit);
            traversal = new Traversal(party.stamina());
            // ICD belongs to a character+player, not to the shared Minecraft player entity.
            ByteBuffer identity = ByteBuffer.allocate(2 * Long.BYTES + Integer.BYTES);
            identity.putLong(player.getUUID().getMostSignificantBits()).putLong(player.getUUID().getLeastSignificantBits());
            for (int slot = 0; slot < Party.SIZE; slot++) {
                identity.putInt(2 * Long.BYTES, slot);
                combatOwners[party.kit(slot).state().character().ordinal()] = UUID.nameUUIDFromBytes(identity.array());
            }
            mirroredHealth = player.getHealth();
            kit().setHp(kit().maxHp() * player.getHealth() / player.getMaxHealth());
        }
        public CharacterKit kit() { return party.activeKit(); }
        public Party party() { return party; }
        public Stamina stamina() { return party.stamina(); }
        public Traversal traversal() { return traversal; }
        public Element skillAbsorbedElement() { return skillAbsorbed; }
        public Element burstAbsorbedElement() { return burstAbsorbed; }
        public boolean intent(Intent intent, long frame) {
            if (!ManagedWorld.isManaged(player.level()) || !player.isAlive() || player.isSpectator()) return false;
            // Drain prior hits before installing a new cast's absorption/origin.
            advanceTo(frame);
            if (intent == Intent.TRAVERSAL_JUMP) { traversalJumpQueued = true; return true; }
            if (traversal.active()) { reject(intent, frame); sync(); return false; }
            int slot = intent.switchSlot();
            if (slot >= 0) {
                boolean switched = party.switchTo(slot, frame);
                if (switched) { mirrorHealth(); switchEffect(); }
                else if (slot != party.activeSlot()) reject(intent, frame);
                sync();
                return switched;
            }
            boolean accepted = kit().intent(intent, frame);
            if (!accepted) reject(intent, frame);
            if (accepted && (intent == Intent.ATTACK_PRESS || intent == Intent.SKILL_PRESS)) actionLevel = (ServerLevel) player.level();
            if (accepted && kit() instanceof AmberKit) {
                if (intent == Intent.SKILL_PRESS) {
                    puppetLevel = (ServerLevel) player.level();
                    Vec3 throwPoint = player.position().add(forward(player).scale(ADAPTED_BUNNY_THROW_DISTANCE));
                    puppetLanding = puppetLevel.clip(new net.minecraft.world.level.ClipContext(
                            throwPoint.add(0, 2, 0), throwPoint.add(0, -16, 0),
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE, player)).getLocation();
                }
                if (intent == Intent.BURST_PRESS) {
                    rainLevel = (ServerLevel) player.level();
                    rainOrigin = player.position().add(forward(player).scale(ADAPTED_RAIN_FORWARD_DISTANCE));
                }
            }
            if (accepted && intent == Intent.SKILL_PRESS) { skillCast = frame; skillAbsorbed = null; }
            if (accepted && intent == Intent.BURST_PRESS && kit() instanceof TravelerAnemoKit) {
                tornadoStart = frame;
                tornadoOrigin = player.position().add(0, .8, 0);
                tornadoDirection = forward(player);
                tornadoLevel = (ServerLevel) player.level();
                burstAbsorbed = null;
            }
            if (accepted && intent == Intent.BURST_PRESS && kit() instanceof KaeyaKit) icicleLevel = (ServerLevel) player.level();
            if (accepted && intent == Intent.BURST_PRESS && kit() instanceof LisaKit) {
                roseLevel = (ServerLevel) player.level();
                roseOrigin = player.position();
            }
            sync();
            return accepted;
        }
        private void reject(Intent intent, long frame) {
            var reason = CharacterStatePayload.Rejection.NONE;
            int slot = intent.switchSlot();
            if (slot >= 0) {
                if (!party.members().get(slot).alive()) reason = CharacterStatePayload.Rejection.FALLEN;
                else if (traversal.mode() == Traversal.Mode.CLIMB) reason = CharacterStatePayload.Rejection.CLIMBING;
                else if (traversal.mode() == Traversal.Mode.GLIDE) reason = CharacterStatePayload.Rejection.GLIDING;
                else if (frame < party.switchReadyFrame()) reason = CharacterStatePayload.Rejection.SWITCH_COOLDOWN;
                else reason = CharacterStatePayload.Rejection.SWITCH_BLOCKED;
            } else if (intent == Intent.SKILL_PRESS) {
                reason = kit().skillRemaining() > 0 ? CharacterStatePayload.Rejection.SKILL_COOLDOWN
                        : CharacterStatePayload.Rejection.ACTION_BLOCKED;
            } else if (intent == Intent.BURST_PRESS) {
                reason = kit().burstRemaining() > 0 ? CharacterStatePayload.Rejection.BURST_COOLDOWN
                        : kit().energy() < kit().state().burstCost() ? CharacterStatePayload.Rejection.ENERGY
                        : CharacterStatePayload.Rejection.ACTION_BLOCKED;
            } else if (intent == Intent.ATTACK_PRESS) reason = CharacterStatePayload.Rejection.ACTION_BLOCKED;
            if (reason == CharacterStatePayload.Rejection.NONE) return;
            rejectionSerial++;
            rejectedIntent = intent.ordinal() + 1;
            rejection = reason;
        }
        public void advanceTo(long frame) {
            reconcileDimension();
            party.advanceTo(frame);
            AmberKit amber = (AmberKit) party.kit(1);
            if (bunny != null && (!bunny.isAlive() || bunny.isRemoved() || bunny.getHealth() < amber.puppetHp() - .001))
                amber.damagePuppet(Math.max(0, amber.puppetHp() - (bunny.isAlive() && !bunny.isRemoved() ? bunny.getHealth() : 0)), frame);
            boolean aiming = kit() == amber && amber.aiming();
            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (aiming && !speed.hasModifier(AIM_SPEED_ID)) speed.addTransientModifier(AIM_SPEED);
            else if (!aiming) speed.removeModifier(AIM_SPEED_ID);
            if (tornadoStart >= 0 && frame != lastTornadoStep && frame - tornadoStart <= TravelerAnemoKit.BURST_DURATION_FRAMES
                    && party.kit(0).hp() > 0 && valid() && player.level() == tornadoLevel) {
                lastTornadoStep = frame;
                Vec3 center = tornadoCenter(frame);
                var nearby = nearby(tornadoLevel, center, TORNADO_RADIUS);
                if (burstAbsorbed == null) burstAbsorbed = absorb(nearby, frame);
                for (LivingEntity enemy : nearby) {
                    if (enemy.getBbWidth() <= SMALL_ENEMY_MAX_WIDTH && enemy.getBbHeight() <= SMALL_ENEMY_MAX_HEIGHT) {
                        Vec3 pull = center.subtract(enemy.position()).normalize().scale(.12);
                        enemy.setDeltaMovement(enemy.getDeltaMovement().add(pull.x, Math.min(.08, pull.y), pull.z));
                    }
                }
                particles(tornadoLevel, center, burstAbsorbed, false);
            }
            if (frame != lastRoseVisual && roseOrigin != null && player.level() == roseLevel && valid()
                    && ((LisaKit) party.kit(3)).roseActive(frame)) {
                lastRoseVisual = frame;
                roseParticles();
            }
        }
        /** Vanilla input supplies the Sprint edge; managed camera intent supplies its movement basis. */
        public void tickMovement(long frame) {
            advanceTo(frame);
            if (lastMovementFrame == frame) return;
            lastMovementFrame = frame;
            var input = player.getLastClientInput();
            // Mirror client aim cancellation even when stationary, airborne or stamina-exhausted.
            if ((input.jump() || input.sprint()) && kit() instanceof AmberKit amber && amber.aiming()) {
                amber.leaveField(frame);
                player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(AIM_SPEED_ID);
            }
            boolean held = input.sprint();
            boolean moving = input.forward() != input.backward() || input.left() != input.right();
            // A quick real key click may press/release between server ticks. Preserve its explicit
            // intent; raw input edges remain available for offline server simulation fixtures.
            boolean jumped = traversalJumpQueued || (player.connection == null || !player.connection.isAcceptingMessages())
                    && input.jump() && !jumpKeyHeld;
            traversalJumpQueued = false;
            jumpKeyHeld = input.jump();
            if (tickTraversal(frame, input, moving, jumped)) {
                stamina().stopSprint(frame);
                player.setSprinting(false);
                sprintKeyHeld = held;
                dashMotion = null;
                dashLevel = null;
                return;
            }
            boolean eligible = valid() && !player.isSpectator() && !player.isPassenger()
                    && !player.getAbilities().flying && !player.isInWater() && !player.isFallFlying()
                    && !input.shift();
            if (eligible && moving && held && !sprintKeyHeld && player.onGround() && stamina().dash(frame)) {
                if (Float.isNaN(cameraYaw)) dashMotion = player.getLastClientMoveIntent().scale(ADAPTED_DASH_BLOCKS_PER_TICK);
                else {
                    int left = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
                    int forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
                    dashMotion = new Vec3(CameraMath.movementX(cameraYaw, left, forward) * ADAPTED_DASH_BLOCKS_PER_TICK,
                            0, CameraMath.movementZ(cameraYaw, left, forward) * ADAPTED_DASH_BLOCKS_PER_TICK);
                }
                dashLevel = (ServerLevel) player.level();
            }
            sprintKeyHeld = held;
            if (eligible && moving && held) {
                if (!stamina().sprinting() && !stamina().exhausted()) stamina().startSprint(frame);
            } else {
                stamina().stopSprint(frame);
            }
            player.setSprinting(eligible && moving && held && stamina().sprinting() && !stamina().exhausted());
            if (dashMotion != null) {
                boolean dashing = eligible && player.level() == dashLevel && stamina().dashing(frame);
                setHorizontalMotion(dashing ? dashMotion : Vec3.ZERO);
                if (!dashing) { dashMotion = null; dashLevel = null; }
            }
        }
        private boolean tickTraversal(long frame, net.minecraft.world.entity.player.Input input, boolean moving, boolean jumped) {
            int before = traversalFlags(frame);
            boolean eligible = valid() && !player.isSpectator() && !player.isPassenger()
                    && !player.getAbilities().flying && !player.isInWater() && !player.isFallFlying();
            float yaw = Float.isNaN(cameraYaw) ? player.getYRot() : cameraYaw;
            Traversal.Mode oldMode = traversal.mode();
            if (oldMode == Traversal.Mode.CLIMB)
                climbWall = TraversalGeometry.attachedWall(player, climbWall, input, traversal.jumping(frame), traversal.jumpSide());
            boolean adjacent = climbWall != null;
            traversal.tick(moving, eligible && (oldMode != Traversal.Mode.CLIMB || adjacent), frame);
            if (traversal.mode() == Traversal.Mode.CLIMB) {
                if (input.shift() || input.backward() && player.onGround()) endTraversal(frame);
                else if (jumped) {
                    if (input.backward() && !input.left() && !input.right()) {
                        Vec3 away = new Vec3(-climbWall.getStepX() * Traversal.ADAPTED_JUMP_AWAY_BLOCKS_PER_TICK,
                                Traversal.ADAPTED_JUMP_AWAY_BLOCKS_PER_TICK,
                                -climbWall.getStepZ() * Traversal.ADAPTED_JUMP_AWAY_BLOCKS_PER_TICK);
                        endTraversal(frame);
                        setTraversalMotion(away, true);
                    } else traversal.climbJump((input.left() ? 1 : 0) - (input.right() ? 1 : 0), frame);
                }
                if (traversal.mode() == Traversal.Mode.CLIMB && (input.forward() || traversal.jumping(frame))) {
                    Vec3 mantle = TraversalGeometry.mantle(player, climbWall);
                    if (mantle != null) {
                        endTraversal(frame);
                        player.teleportTo(mantle.x, mantle.y, mantle.z);
                        player.setOnGround(true);
                        player.resetFallDistance();
                    } else if (!player.level().noCollision(player, player.getBoundingBox()
                            .deflate(Traversal.ADAPTED_COLLISION_EPSILON).move(0,
                                    Traversal.ADAPTED_CLIMB_BLOCKS_PER_TICK, 0))) endTraversal(frame);
                }
            } else if (traversal.mode() == Traversal.Mode.GLIDE && (jumped || input.shift() || player.onGround())) {
                endTraversal(frame);
            } else if (oldMode == Traversal.Mode.FREE && eligible && !input.shift()) {
                if (jumped && !player.onGround()) traversal.openGlider(TraversalGeometry.clearance(player), frame);
                if (!traversal.active() && moving) {
                    Direction wall = TraversalGeometry.findWall(player, yaw, input);
                    if (wall != null && traversal.attach(frame)) climbWall = wall;
                }
            }
            traversal.tick(moving, eligible, frame);
            if (oldMode != Traversal.Mode.FREE && !traversal.active()) {
                player.setNoGravity(false);
                if (!jumped) setTraversalMotion(Vec3.ZERO, true);
            }
            if (traversal.active()) {
                player.setNoGravity(true);
                player.resetFallDistance();
                if (traversal.mode() == Traversal.Mode.CLIMB) {
                    float bodyYaw = CameraMath.facingYaw(climbWall.getStepX(), climbWall.getStepZ());
                    player.setYRot(bodyYaw);
                    player.setYHeadRot(bodyYaw);
                    player.setYBodyRot(bodyYaw);
                }
                setTraversalMotion(TraversalGeometry.motion(player, traversal.mode(), climbWall, yaw, input,
                        traversal.jumping(frame), traversal.jumpSide()), false);
                if (traversal.mode() == Traversal.Mode.GLIDE) gliderParticles(yaw);
            }
            if (before != traversalFlags(frame)) { lastSync = null; sync(); }
            return traversal.active() || oldMode != Traversal.Mode.FREE;
        }
        private int traversalFlags(long frame) {
            if (traversal.mode() == Traversal.Mode.GLIDE) return 2;
            if (traversal.mode() != Traversal.Mode.CLIMB) return 0;
            return 1 | (climbWall.ordinal() << 2) | (traversal.jumping(frame) ? (traversal.jumpSide() + 2) << 5 : 0);
        }
        private void endTraversal(long frame) {
            traversal.detach(frame);
            player.setNoGravity(false);
        }
        private void setTraversalMotion(Vec3 motion, boolean send) {
            player.setDeltaMovement(motion);
            if (send && player.connection != null && player.connection.isAcceptingMessages())
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
        private void gliderParticles(float yaw) {
            double angle = Math.toRadians(yaw);
            double sideX = Math.cos(angle), sideZ = Math.sin(angle);
            double backX = Math.sin(angle) * Traversal.ADAPTED_WING_BACK_OFFSET;
            double backZ = -Math.cos(angle) * Traversal.ADAPTED_WING_BACK_OFFSET;
            for (int sample = -Traversal.ADAPTED_WING_PARTICLE_SAMPLES; sample <= Traversal.ADAPTED_WING_PARTICLE_SAMPLES; sample++) {
                double side = sample * Traversal.ADAPTED_WING_HALF_SPAN / Traversal.ADAPTED_WING_PARTICLE_SAMPLES;
                player.level().sendParticles(ANEMO_DUST, player.getX() + backX + sideX * side,
                        player.getY() + Traversal.ADAPTED_WING_HEIGHT - Math.abs(side) * Traversal.ADAPTED_WING_DROOP,
                        player.getZ() + backZ + sideZ * side, 1, 0, 0, 0, 0);
            }
        }
        private void setHorizontalMotion(Vec3 motion) {
            player.setDeltaMovement(motion.x, player.getDeltaMovement().y, motion.z);
            player.hurtMarked = true;
            // ServerPlayer movement is client-simulated; explicitly send the authoritative impulse
            // to its owner (entity tracking alone does not send that player's own velocity).
            if (player.connection != null && player.connection.isAcceptingMessages())
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
        private boolean valid() {
            reconcileDimension();
            return players.get(player.getUUID()) == this && player.isAlive() && !player.isRemoved() && ManagedWorld.isManaged(player.level());
        }
        private void reconcileDimension() {
            if (sessionLevel == player.level()) return;
            sessionLevel = player.level();
            clearFieldObjects();
        }
        private void clearFieldObjects() {
            endTraversal(Math.max(stamina().frame(), timeline.frame()));
            ++castGeneration;
            for (int slot = 0; slot < Party.SIZE; slot++) party.kit(slot).cancelCasts(timeline.frame());
            if (bunny != null) { bunny.discard(); bunny = null; }
            actionLevel = null;
            puppetLevel = null;
            puppetLanding = null;
            rainLevel = null;
            rainOrigin = null;
            icicleLevel = null;
            roseLevel = null;
            roseOrigin = null;
            tornadoLevel = null;
            tornadoOrigin = null;
            tornadoDirection = null;
            tornadoStart = -1;
            skillAbsorbed = null;
            burstAbsorbed = null;
            player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(AIM_SPEED_ID);
        }
        private void reconcileHealth() {
            if (player.getHealth() != mirroredHealth) kit().setHp(kit().maxHp() * player.getHealth() / player.getMaxHealth());
            if (kit().hp() == 0) forceSwitch(Math.max(party.frame(),
                    Frames.atServerTick(player.level().getServer().getTickCount())));
            mirrorHealth();
        }
        private void mirrorHealth() {
            mirroredHealth = (float) (player.getMaxHealth() * kit().hp() / kit().maxHp());
            player.setHealth(mirroredHealth);
        }
        private boolean forceSwitch(long frame) {
            endTraversal(frame);
            boolean switched = party.forceSwitch(frame);
            if (switched) switchEffect();
            else clearFieldObjects();
            return switched;
        }
        private void broadcastCharacter(int slot) {
            ((ServerLevel) player.level()).getChunkSource().chunkMap.sendToTrackingPlayers(player,
                    new ClientboundCustomPayloadPacket(new PlayerCharacterPayload(player.getId(), slot)));
            displayedSlot = slot;
            displayedLevel = (ServerLevel) player.level();
        }
        private void switchEffect() {
            broadcastCharacter(party.activeSlot());
            DustParticleOptions dust = switch (party.activeMember().element()) {
                case ANEMO -> ANEMO_DUST; case PYRO -> PYRO_DUST; case CRYO -> CRYO_DUST; case ELECTRO -> ELECTRO_DUST;
                default -> throw new IllegalStateException("Not a starter element");
            };
            ((ServerLevel) player.level()).sendParticles(dust, player.getX(), player.getY() + 1, player.getZ(),
                    20, .4, .7, .4, .03);
        }
        private void sync() {
            if (displayedSlot != party.activeSlot() || displayedLevel != player.level()) broadcastCharacter(party.activeSlot());
            if (player.connection == null || !player.connection.isAcceptingMessages()) return;
            if (lastSyncLevel != player.level()) {
                lastSync = null;
                lastSyncLevel = (ServerLevel) player.level();
            }
            int traversalFlags = traversalFlags(party.frame());
            // Input feedback bypasses ordinary change-only throttling; never infer rejection on the client.
            if (lastSync != null && lastSync.rejectionSerial() == rejectionSerial
                    && lastSync.traversalFlags() == traversalFlags
                    && party.frame() - lastSyncFrame < Frames.atServerTick(CHARACTER_SYNC_INTERVAL_TICKS)) return;
            float stamina = (float) stamina().current();
            boolean exhausted = stamina().exhausted();
            boolean draining = stamina().draining();
            int switchRemaining = (int) Math.max(Math.max(0, party.switchReadyFrame() - party.frame()),
                    kit().switchRemaining(party.frame()));
            boolean switchBlocked = traversal.active() || !kit().canSwitch(party.frame());
            boolean actionBlocked = traversal.active() || kit().actionBlocked();
            boolean changed = lastSync == null || lastSync.activeSlot() != party.activeSlot()
                    || lastSync.stamina() != stamina || lastSync.staminaExhausted() != exhausted
                    || lastSync.staminaDraining() != draining || lastSync.traversalFlags() != traversalFlags
                    || lastSync.switchRemainingFrames() != switchRemaining || lastSync.switchBlocked() != switchBlocked
                    || lastSync.actionBlocked() != actionBlocked || lastSync.rejectionSerial() != rejectionSerial;
            for (int index = 0; !changed && index < Party.SIZE; index++) {
                var member = party.kit(index);
                var previous = lastSync.members().get(index);
                changed = previous.hpFraction() != (float) (member.hp() / member.maxHp())
                        || previous.energy() != (float) member.energy()
                        || previous.skillRemainingFrames() != member.skillRemaining()
                        || previous.burstRemainingFrames() != member.burstRemaining()
                        || previous.skillCooldownFrames() != member.state().skillCooldownFrames()
                        || previous.burstCooldownFrames() != member.state().burstCooldownFrames();
            }
            if (!changed) return;
            var members = new java.util.ArrayList<CharacterStatePayload.Member>(Party.SIZE);
            for (int index = 0; index < Party.SIZE; index++) {
                var member = party.kit(index);
                members.add(new CharacterStatePayload.Member((float) (member.hp() / member.maxHp()),
                        (float) member.energy(), (int) member.skillRemaining(), (int) member.burstRemaining(),
                        member.state().skillCooldownFrames(), member.state().burstCooldownFrames()));
            }
            lastSync = new CharacterStatePayload(true, party.activeSlot(), members, stamina, exhausted, draining,
                    traversalFlags, switchRemaining, switchBlocked, actionBlocked, rejectionSerial, rejectedIntent, rejection);
            lastSyncFrame = party.frame();
            if (sender != null) sender.accept(player, lastSync);
        }
        private Vec3 tornadoCenter(long frame) {
            return tornadoOrigin.add(tornadoDirection.scale(Frames.seconds(frame - tornadoStart) * TORNADO_BLOCKS_PER_SECOND));
        }
        private void hit(CharacterKit kit, Hit hit) {
            if (!valid()) return;
            ServerLevel level = (ServerLevel) player.level();
            if (hit.kind() == Kind.BUNNY_LAND || hit.kind() == Kind.BUNNY_EXPLODE) {
                if (level == puppetLevel) puppet((AmberKit) kit, hit);
                return;
            }
            if (hit.kind() == Kind.RAIN_INNER || hit.kind() == Kind.RAIN_OUTER) {
                if (level == rainLevel) rain((AmberKit) kit, hit);
                return;
            }
            if (hit.kind() == Kind.ICICLES || hit.kind() == Kind.ICICLES_END) {
                if (level == icicleLevel) icicles((KaeyaKit) kit, hit, level);
                return;
            }
            if (hit.kind() == Kind.VIOLET_ORB) {
                if (level == actionLevel) launchVioletOrb((LisaKit) kit, hit, level);
                return;
            }
            if (hit.kind() == Kind.VIOLET_HOLD) {
                if (level == actionLevel) violetHold((LisaKit) kit, hit, level);
                return;
            }
            if (hit.kind() == Kind.ROSE_PLACE || hit.kind() == Kind.ROSE_DISCHARGE) {
                if (level == roseLevel) rose((LisaKit) kit, hit);
                return;
            }
            boolean burst = hit.kind() == Kind.TORNADO;
            if (level != (burst ? tornadoLevel : actionLevel)) return;
            Vec3 origin = burst ? tornadoCenter(hit.frame()) : player.position().add(0, .8, 0);
            boolean basic = hit.kind() == Kind.NORMAL || hit.kind() == Kind.CHARGED;
            boolean sword = basic && kit.weapon() == CharacterKit.Weapon.SWORD;
            boolean ranged = basic && !sword && (hit.kind() == Kind.NORMAL || kit.weapon() == CharacterKit.Weapon.BOW);
            boolean catalystCharge = basic && !sword && !ranged;
            boolean frostgnaw = hit.kind() == Kind.FROSTGNAW;
            double radius = sword ? SWORD_RADIUS : catalystCharge ? ADAPTED_LISA_CHARGED_RADIUS
                    : burst ? TORNADO_RADIUS : frostgnaw ? ADAPTED_FROSTGNAW_RANGE : SKILL_RADIUS;
            List<LivingEntity> enemies = ranged ? rayTargets(level, kit.weapon()) : nearby(level, origin, radius);
            if (frostgnaw) enemies.removeIf(enemy -> !enemy.getBoundingBox().intersects(
                    player.getX() - ADAPTED_FROSTGNAW_RANGE, player.getY(), player.getZ() - ADAPTED_FROSTGNAW_RANGE,
                    player.getX() + ADAPTED_FROSTGNAW_RANGE, player.getY() + ADAPTED_FROSTGNAW_HEIGHT,
                    player.getZ() + ADAPTED_FROSTGNAW_RANGE));
            if (!burst && !ranged) {
                Vec3 facing = forward(player);
                enemies.removeIf(enemy -> {
                    double dx = enemy.getX() - player.getX();
                    double dz = enemy.getZ() - player.getZ();
                    double distanceSquared = dx * dx + dz * dz;
                    return catalystCharge && enemy.getY() > player.getY() + ADAPTED_LISA_CHARGED_HEIGHT_TOLERANCE
                            || distanceSquared > .01 && (facing.x * dx + facing.z * dz)
                            < (sword || catalystCharge || frostgnaw ? SWORD_ARC_COSINE : 0) * Math.sqrt(distanceSquared);
                });
            }
            Element absorbed = null;
            if (hit.mayAbsorb()) {
                if (burst) {
                    if (burstAbsorbed == null) burstAbsorbed = absorb(enemies, hit.frame());
                    absorbed = burstAbsorbed;
                } else if (hit.castFrame() == skillCast) {
                    if (skillAbsorbed == null) skillAbsorbed = absorb(enemies, hit.frame());
                    absorbed = skillAbsorbed;
                }
            }
            if (frostgnaw) frostgnawParticles(level);
            else particles(level, origin.add(burst ? Vec3.ZERO : forward(player).scale(1.5)),
                    basic && hit.element() != Element.PHYSICAL ? hit.element() : absorbed, sword);
            boolean enemyHit = false;
            // Existing bow raycasts hit at release: elapsed flight is zero, not the attack's wind-up.
            Stats hitStats = basic ? kit.state().normalChargedStats(hit.frame(), 0) : kit.state().stats(hit.frame());
            for (LivingEntity enemy : enemies) {
                CombatTarget target = target(enemy);
                enemyHit |= deal(kit, target, hit.element(), hit.multiplier(), hit.gauge(), hit.icdTag(), hit.frame(), false, hitStats);
                if (absorbed != null && hit.absorbedHit() && enemy.isAlive()
                        && (!burst || enemy.position().distanceToSqr(origin) <= ABSORBED_TORNADO_RADIUS * ABSORBED_TORNADO_RADIUS)) {
                    deal(kit, target, absorbed, burst ? .248 : hit.multiplier() * .25,
                            burst ? 2 : 1, hit.kind() == Kind.STORM ? null
                                    : (burst ? "Elemental Burst " : "Elemental Skill ") + absorbed, hit.frame(), false);
                }
            }
            if (enemyHit && hit.particles() > 0) party.collect(Energy.Item.PARTICLE, hit.element(), hit.particles());
        }
        private LivingEntity nearestEnemy(ServerLevel level, Vec3 center, double radius) {
            LivingEntity nearest = null;
            double distance = Double.POSITIVE_INFINITY;
            for (LivingEntity enemy : nearby(level, center, radius)) {
                double candidate = enemy.position().distanceToSqr(center);
                if (candidate < distance || candidate == distance && nearest != null && enemy.getId() < nearest.getId()) {
                    nearest = enemy;
                    distance = candidate;
                }
            }
            return nearest;
        }
        private final class VioletOrb {
            final LisaKit lisa;
            final Hit hit;
            final ServerLevel level;
            final long generation;
            final Vec3 direction;
            LivingEntity enemy;
            Vec3 position;
            long lastFrame;
            VioletOrb(LisaKit lisa, Hit hit, ServerLevel level) {
                this.lisa = lisa;
                this.hit = hit;
                this.level = level;
                generation = castGeneration;
                direction = player.getLookAngle();
                position = player.getEyePosition();
                enemy = nearestEnemy(level, position, ADAPTED_VIOLET_ACQUISITION_RADIUS);
                lastFrame = hit.frame();
            }
        }
        private void launchVioletOrb(LisaKit lisa, Hit hit, ServerLevel level) {
            VioletOrb orb = new VioletOrb(lisa, hit, level);
            sampleVioletOrb(orb, hit.frame() + LisaKit.TAP_FIRST_IMPACT_FRAME - LisaKit.TAP_LAUNCH_FRAME);
        }
        private void sampleVioletOrb(VioletOrb orb, long frame) {
            timeline.schedule(frame, at -> {
                if (!valid() || orb.generation != castGeneration || !orb.lisa.state().alive()
                        || player.level() != orb.level || at >= orb.hit.frame() + LisaKit.TAP_LIFETIME_FRAMES) return;
                if (orb.enemy == null || !orb.enemy.isAlive() || orb.enemy.isRemoved())
                    orb.enemy = nearestEnemy(orb.level, orb.position, ADAPTED_VIOLET_ACQUISITION_RADIUS);
                Vec3 destination = orb.enemy == null ? orb.position.add(orb.direction)
                        : orb.enemy.position().add(0, orb.enemy.getBbHeight() / 2, 0);
                Vec3 difference = destination.subtract(orb.position);
                double step = ADAPTED_VIOLET_SPEED_PER_FRAME * (at - orb.lastFrame);
                boolean impact = orb.enemy != null && difference.lengthSqr() <= step * step;
                Vec3 next = impact ? destination : orb.position.add(difference.normalize().scale(step));
                var obstruction = orb.level.clip(new net.minecraft.world.level.ClipContext(orb.position, next,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, player));
                if (obstruction.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) return;
                orb.position = next;
                orb.lastFrame = at;
                orb.level.sendParticles(ELECTRO_DUST, next.x, next.y, next.z, 5, .08, .08, .08, 0);
                if (impact) violetImpact(orb, at);
                else sampleVioletOrb(orb, at + ADAPTED_ORB_SAMPLE_FRAMES);
            });
        }
        private void violetImpact(VioletOrb orb, long frame) {
            var direct = nearby(orb.level, orb.position, ADAPTED_VIOLET_IMPACT_RADIUS);
            boolean stacks = party.activeKit() == orb.lisa;
            for (LivingEntity enemy : direct) {
                CombatTarget target = target(enemy);
                deal(orb.lisa, target, Element.ELECTRO, orb.hit.multiplier(), 1, LisaKit.NORMAL_ICD_TAG, frame, stacks);
                if (stacks) {
                    target.conductive().add(frame);
                    syncAura(target);
                }
            }
            // Each initially affected enemy emits one non-recursive bounce; self is not a second stack.
            if (stacks) for (LivingEntity origin : direct) {
                for (LivingEntity enemy : nearby(orb.level, origin.position(), ADAPTED_CONDUCTIVE_BOUNCE_RADIUS)) {
                    if (enemy == origin) continue;
                    CombatTarget target = target(enemy);
                    target.conductive().add(frame);
                    syncAura(target);
                }
            }
        }
        private void violetHold(LisaKit lisa, Hit hit, ServerLevel level) {
            Vec3 center = player.position();
            boolean connected = false;
            for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(center, center).inflate(VIOLET_HOLD_RADIUS, ADAPTED_VIOLET_HOLD_HEIGHT, VIOLET_HOLD_RADIUS),
                    entity -> !(entity instanceof Player) && !isPuppet(entity) && entity.isAlive() && !entity.isRemoved()
                            && horizontalDistanceSquared(entity, center) <= VIOLET_HOLD_RADIUS * VIOLET_HOLD_RADIUS)) {
                CombatTarget target = target(enemy);
                int stacks = target.conductive().consume(hit.frame());
                connected |= deal(lisa, target, Element.ELECTRO, LisaKit.holdMultiplier(stacks), 2, null, hit.frame(), false);
                syncAura(target);
            }
            for (int index = 0; index < 32; index++) {
                double angle = 2 * Math.PI * index / 32;
                level.sendParticles(ELECTRO_DUST, center.x + Math.cos(angle) * VIOLET_HOLD_RADIUS,
                        center.y + .2, center.z + Math.sin(angle) * VIOLET_HOLD_RADIUS, 2, .1, .5, .1, 0);
            }
            if (connected) party.collect(Energy.Item.PARTICLE, Element.ELECTRO, hit.particles());
        }
        private void roseParticles() {
            for (int petal = 0; petal < 6; petal++) {
                double angle = Math.PI * petal / 3;
                roseLevel.sendParticles(ELECTRO_DUST, roseOrigin.x + .45 * Math.cos(angle),
                        roseOrigin.y + 1.1, roseOrigin.z + .45 * Math.sin(angle), 1, .04, .08, .04, 0);
            }
            roseLevel.sendParticles(ELECTRO_DUST, roseOrigin.x, roseOrigin.y + .7, roseOrigin.z, 3, .05, .4, .05, 0);
        }
        private void rose(LisaKit lisa, Hit hit) {
            roseParticles();
            if (hit.kind() == Kind.ROSE_PLACE) {
                for (LivingEntity enemy : nearby(roseLevel, roseOrigin, ROSE_RADIUS)) {
                    if (Math.abs(enemy.getY() - roseOrigin.y) > ADAPTED_ROSE_PLACEMENT_HEIGHT) continue;
                    deal(lisa, target(enemy), Element.ELECTRO, hit.multiplier(), 0, null, hit.frame(), false);
                    Vec3 push = enemy.position().subtract(roseOrigin).multiply(1, 0, 1).normalize().scale(ADAPTED_ROSE_KNOCKBACK);
                    enemy.setDeltaMovement(enemy.getDeltaMovement().add(push.x, .08, push.z));
                }
                return;
            }
            var candidates = nearby(roseLevel, roseOrigin, ROSE_RADIUS);
            if (candidates.isEmpty()) return;
            // Source says random enemy priority; uniform weighting/seed are explicit adaptations.
            LivingEntity selected = candidates.get(candidates.size() == 1 ? 0 : roseTargets.nextInt(candidates.size()));
            Vec3 start = roseOrigin.add(0, 1.1, 0), end = selected.position().add(0, selected.getBbHeight() / 2, 0);
            for (int point = 0; point <= 12; point++) {
                Vec3 bolt = start.lerp(end, point / 12.0);
                roseLevel.sendParticles(ELECTRO_DUST, bolt.x, bolt.y, bolt.z, 1, .02, .02, .02, 0);
            }
            for (LivingEntity enemy : nearby(roseLevel, selected.position(), ROSE_IMPACT_RADIUS))
                deal(lisa, target(enemy), Element.ELECTRO, hit.multiplier(), 1, hit.icdTag(), hit.frame(), false);
        }
        private void puppet(AmberKit amber, Hit hit) {
            if (hit.kind() == Kind.BUNNY_LAND) {
                if (player.level() != puppetLevel) return;
                bunny = new Rabbit(GenshinEntities.BARON_BUNNY, puppetLevel);
                bunny.setNoAi(true);
                bunny.setCustomName(Component.literal("Baron Bunny"));
                bunny.setCustomNameVisible(true);
                bunny.getAttribute(Attributes.SCALE).setBaseValue(ADAPTED_BUNNY_MODEL_SCALE);
                bunny.getAttribute(Attributes.MAX_HEALTH).setBaseValue(amber.puppetHp());
                bunny.setHealth((float) amber.puppetHp());
                bunny.snapTo(puppetLanding);
                puppetLevel.addFreshEntity(bunny);
                puppetLevel.sendParticles(PYRO_DUST, bunny.getX(), bunny.getY() + .5, bunny.getZ(), 16, .4, .5, .4, 0);
                return;
            }
            if (bunny == null) return;
            Vec3 center = bunny.position().add(0, .5, 0);
            bunny.discard();
            bunny = null;
            puppetLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0);
            puppetLevel.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 45, 1.5, .6, 1.5, .04);
            boolean connected = false;
            for (LivingEntity enemy : nearby(puppetLevel, center, ADAPTED_BUNNY_EXPLOSION_RADIUS))
                connected |= deal(amber, target(enemy), hit.element(), hit.multiplier(), hit.gauge(), null, hit.frame(), false);
            if (connected) {
                puppetLevel.sendParticles(PYRO_DUST, center.x, center.y, center.z, 4, .3, .3, .3, .01);
                party.collect(Energy.Item.PARTICLE, Element.PYRO, hit.particles());
            }
        }
        private void rain(AmberKit amber, Hit hit) {
            boolean inner = hit.kind() == Kind.RAIN_INNER;
            double innerRadius = ADAPTED_RAIN_RADIUS / 2;
            // Nine inner and nine outer waves, rather than pretending all18 connect to one target.
            for (LivingEntity enemy : nearby(rainLevel, rainOrigin, ADAPTED_RAIN_RADIUS)) {
                double distance = horizontalDistanceSquared(enemy, rainOrigin);
                if ((distance <= innerRadius * innerRadius) != inner
                        || enemy.getY() > rainOrigin.y + ADAPTED_RAIN_HEIGHT || enemy.getY() < rainOrigin.y - 1) continue;
                deal(amber, target(enemy), hit.element(), hit.multiplier(), hit.gauge(), hit.icdTag(), hit.frame(), false);
            }
            for (int arrow = 0; arrow < 12; arrow++) {
                double angle = 2 * Math.PI * arrow / 12;
                double radius = inner ? innerRadius * .7 : ADAPTED_RAIN_RADIUS * .8;
                double x = rainOrigin.x + Math.cos(angle) * radius, z = rainOrigin.z + Math.sin(angle) * radius;
                rainLevel.sendParticles(ParticleTypes.FLAME, x, rainOrigin.y + 2.5, z, 2, .08, .8, .08, .015);
                rainLevel.sendParticles(PYRO_DUST, x, rainOrigin.y + .15, z, 1, .08, .1, .08, 0);
            }
        }
        private void frostgnawParticles(ServerLevel level) {
            Vec3 facing = forward(player);
            for (double distance = 1; distance <= ADAPTED_FROSTGNAW_RANGE; distance += 1.5) {
                Vec3 center = player.position().add(facing.scale(distance)).add(0, .9, 0);
                level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z, 8,
                        distance * .18, .5, distance * .18, .02);
                level.sendParticles(CRYO_DUST, center.x, center.y, center.z, 4, .3, .4, .3, 0);
            }
        }
        private void icicles(KaeyaKit kaeya, Hit hit, ServerLevel level) {
            boolean end = hit.kind() == Kind.ICICLES_END;
            double revolution = 2 * Math.PI * (hit.frame() - hit.castFrame() - KaeyaKit.BURST_FIRST_CONTACT_FRAME)
                    / KaeyaKit.ADAPTED_REVOLUTION_FRAMES;
            Vec3 origin = player.position().add(0, .8, 0);
            for (int icicle = 0; icicle < KaeyaKit.ICICLE_COUNT; icicle++) {
                double angle = revolution + 2 * Math.PI * icicle / KaeyaKit.ICICLE_COUNT;
                Vec3 point = origin.add(Math.sin(angle) * ADAPTED_ICICLE_ORBIT_RADIUS, 0,
                        Math.cos(angle) * ADAPTED_ICICLE_ORBIT_RADIUS);
                level.sendParticles(ParticleTypes.SNOWFLAKE, point.x, point.y, point.z, end ? 12 : 2, .08, .3, .08, .005);
                level.sendParticles(CRYO_DUST, point.x, point.y, point.z, end ? 8 : 2, .04, .25, .04, 0);
                var contacts = level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(point, point).inflate(ADAPTED_ICICLE_CONTACT_MARGIN),
                        entity -> !(entity instanceof Player) && !isPuppet(entity) && entity.isAlive() && !entity.isRemoved());
                if (contacts.isEmpty() || !end && !kaeya.connectIcicle(icicle, hit.frame())) continue;
                // One contact AoE spends this icicle's enemy-independent lock, not a per-target timer.
                for (LivingEntity enemy : contacts)
                    deal(kaeya, target(enemy), hit.element(), hit.multiplier(), hit.gauge(), hit.icdTag(), hit.frame(), false);
            }
        }
        /** Nearest unobstructed living target on the server eye ray. */
        private LivingEntity aimedEnemy(ServerLevel level, double range) {
            Vec3 start = player.getEyePosition();
            Vec3 end = start.add(player.getLookAngle().scale(range));
            end = level.clip(new net.minecraft.world.level.ClipContext(start, end,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, player)).getLocation();
            LivingEntity nearest = null;
            double distance = start.distanceToSqr(end);
            for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(ADAPTED_RAY_HITBOX_MARGIN),
                    entity -> !(entity instanceof Player) && !isPuppet(entity) && entity.isAlive() && !entity.isRemoved())) {
                var impact = enemy.getBoundingBox().inflate(ADAPTED_RAY_HITBOX_MARGIN).clip(start, end);
                if (impact.isPresent() && start.distanceToSqr(impact.get()) < distance) {
                    nearest = enemy;
                    distance = start.distanceToSqr(impact.get());
                }
            }
            return nearest;
        }
        private List<LivingEntity> rayTargets(ServerLevel level, CharacterKit.Weapon weapon) {
            LivingEntity nearest = aimedEnemy(level, weapon == CharacterKit.Weapon.BOW ? ADAPTED_BOW_RANGE : ADAPTED_CATALYST_RANGE);
            if (nearest == null) return List.of();
            if (weapon == CharacterKit.Weapon.BOW) {
                level.sendParticles(ParticleTypes.CRIT, nearest.getX(), nearest.getY() + 1, nearest.getZ(), 8, .1, .2, .1, .01);
                return List.of(nearest);
            }
            return nearby(level, nearest.position(), ADAPTED_LISA_IMPACT_RADIUS);
        }
        private boolean deal(CharacterKit kit, CombatTarget target, Element element, double multiplier,
                double gauge, String tag, long frame, boolean conductiveTap) {
            return deal(kit, target, element, multiplier, gauge, tag, frame, conductiveTap, kit.state().stats(frame));
        }
        private boolean deal(CharacterKit kit, CombatTarget target, Element element, double multiplier,
                double gauge, String tag, long frame, boolean conductiveTap, Stats stats) {
            target.aura().advanceTo(frame);
            target.conductive().advanceTo(frame);
            UUID owner = combatOwners[kit.state().character().ordinal()];
            List<Reaction> reactions = List.of();
            if (gauge > 0 && target.application.allowsApplication(owner, tag, target.entity().getUUID(),
                    kit instanceof AmberKit ? ApplicationIcd.Rule.AMBER : ApplicationIcd.Rule.STANDARD, frame)) {
                reactions = target.aura().applyHit(element, gauge, frame);
                if (element == Element.HYDRO || element == Element.ELECTRO) {
                    target.ecOwner = kit.stats();
                    target.ecPlayer = player;
                }
            }
            Reaction amplification = null;
            for (Reaction reaction : reactions) {
                if (reaction.amplifying()) { amplification = reaction; break; }
            }
            boolean critical = random.nextDouble() < Math.clamp(stats.critRate(), 0, 1);
            double amount = Damage.talentDamage(stats, multiplier, element, target.level(), 0, 0,
                    target.resistance(element), amplification, 0, critical ? Damage.CritMode.CRIT : Damage.CritMode.NON_CRIT, null);
            boolean applied = target.damage(player, amount);
            if (applied) feedback(target, amount, element, amplification == null ? null : amplification.type(), critical);
            for (Reaction reaction : reactions) {
                if (reaction.type() == Reaction.Type.SWIRL) target.countSwirl();
                if (reaction.amplifying()) continue;
                if (conductiveTap && (reaction.type() == Reaction.Type.OVERLOADED || reaction.type() == Reaction.Type.SUPERCONDUCT)) {
                    double radius = reaction.type() == Reaction.Type.OVERLOADED ? ADAPTED_OVERLOADED_RADIUS : ADAPTED_SUPERCONDUCT_RADIUS;
                    // Reaction-only targets get a mark, not Electro gauge from transformative damage.
                    for (LivingEntity enemy : nearby((ServerLevel) target.entity().level(), target.entity().position(), radius)) {
                        CombatTarget neighbor = target(enemy);
                        neighbor.conductive().add(frame);
                        syncAura(neighbor);
                    }
                }
                if (reaction.type() == Reaction.Type.FROZEN || reaction.type() == Reaction.Type.ELECTRO_CHARGED) {
                    feedback(target, 0, element, reaction.type(), false);
                    continue;
                }
                Element damageElement = Reaction.damageElement(reaction.type(), reaction.auraElement());
                double reactionAmount = 0;
                if (target.reactionDamage.allowsDamage(owner, target.entity().getUUID(), reaction.type(), reaction.auraElement(), frame)) {
                    double damage = Damage.transformativeDamage(reaction.type(), kit.stats().level(), kit.stats().elementalMastery(), 0,
                            target.resistance(damageElement));
                    if (target.damage(player, damage)) reactionAmount = damage;
                }
                feedback(target, reactionAmount, damageElement, reaction.type(), false);
                if (reaction.type() == Reaction.Type.OVERLOADED) overloaded(kit, owner, target, frame);
            }
            syncAura(target);
            scheduleEc(target);
            return applied;
        }
        private void overloaded(CharacterKit kit, UUID owner, CombatTarget origin, long frame) {
            Vec3 center = origin.entity().position();
            ServerLevel level = (ServerLevel) origin.entity().level();
            level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + .5, center.z, 1, 0, 0, 0, 0);
            for (LivingEntity enemy : nearby(level, center, ADAPTED_OVERLOADED_RADIUS)) {
                Vec3 push = enemy.position().subtract(center).multiply(1, 0, 1);
                if (push.lengthSqr() < .001) push = forward(player);
                enemy.setDeltaMovement(enemy.getDeltaMovement().add(push.normalize().scale(ADAPTED_OVERLOADED_KNOCKBACK)));
                if (enemy == origin.entity()) continue;
                CombatTarget neighbor = target(enemy);
                if (!neighbor.reactionDamage.allowsDamage(owner, enemy.getUUID(), Reaction.Type.OVERLOADED, Element.ELECTRO, frame)) continue;
                double damage = Damage.transformativeDamage(Reaction.Type.OVERLOADED, kit.stats().level(),
                        kit.stats().elementalMastery(), 0, neighbor.resistance(Element.PYRO));
                if (neighbor.damage(player, damage)) feedback(neighbor, damage, Element.PYRO, Reaction.Type.OVERLOADED, false);
            }
        }
        private void scheduleEc(CombatTarget target) {
            var next = target.aura().nextElectroChargedTickFrame();
            if (next.isEmpty() || target.ecOwner == null || next.getAsLong() == target.scheduledEc) return;
            long at = next.getAsLong();
            target.scheduledEc = at;
            timeline.schedule(at, frame -> {
                if (targets.get(target.entity().getUUID()) != target || target.entity().isRemoved()
                        || !ManagedWorld.isManaged(target.entity().level()) || target.scheduledEc != frame) return;
                var tick = target.aura().tickElectroCharged(frame, target.acceptsDamage(target.ecPlayer));
                if (tick.dealsDamage()) {
                    double damage = Damage.transformativeDamage(Reaction.Type.ELECTRO_CHARGED,
                            target.ecOwner.level(), target.ecOwner.elementalMastery(), 0, target.resistance(Element.ELECTRO));
                    if (target.damage(target.ecPlayer, damage)) feedback(target, damage, Element.ELECTRO, Reaction.Type.ELECTRO_CHARGED, false);
                }
                syncAura(target);
                if (target.scheduledEc == frame) target.scheduledEc = -1;
                scheduleEc(target);
            });
        }
        /** Debug applications obey the same aura/reaction rules but are not talent damage or ICD hits. */
        public LivingEntity debugAura(Element element, double gauge, long frame) {
            advanceTo(frame);
            ServerLevel level = (ServerLevel) player.level();
            LivingEntity enemy = aimedEnemy(level, ADAPTED_VIOLET_ACQUISITION_RADIUS);
            if (enemy == null) enemy = nearestEnemy(level, player.position(), ADAPTED_VIOLET_ACQUISITION_RADIUS);
            if (enemy == null) return null;
            CombatTarget target = target(enemy);
            target.aura().applyHit(element, gauge, frame);
            if (element == Element.HYDRO || element == Element.ELECTRO) {
                target.ecOwner = kit().stats();
                target.ecPlayer = player;
            }
            syncAura(target);
            scheduleEc(target);
            return enemy;
        }
    }
    private Element absorb(List<LivingEntity> enemies, long frame) {
        for (LivingEntity entity : enemies) target(entity).aura().advanceTo(frame);
        for (Element element : ABSORPTION_PRIORITY) {
            for (LivingEntity entity : enemies) {
                AuraState aura = target(entity).aura();
                if (aura.gauge(element) > 0 || element == Element.CRYO && aura.isFrozen()) return element;
            }
        }
        return null;
    }
    private static Vec3 forward(ServerPlayer player) {
        Vec3 direction = player.getLookAngle().multiply(1, 0, 1);
        return direction.lengthSqr() < .001 ? new Vec3(0, 0, 1) : direction.normalize();
    }
    private List<LivingEntity> nearby(ServerLevel level, Vec3 center, double radius) {
        double radiusSquared = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, 2, radius),
                entity -> !(entity instanceof Player) && !isPuppet(entity) && entity.isAlive() && !entity.isRemoved()
                        && horizontalDistanceSquared(entity, center) <= radiusSquared);
    }
    private static double horizontalDistanceSquared(LivingEntity entity, Vec3 point) {
        double dx = entity.getX() - point.x;
        double dz = entity.getZ() - point.z;
        return dx * dx + dz * dz;
    }
    private static void particles(ServerLevel level, Vec3 center, Element absorbed, boolean sword) {
        level.sendParticles(sword ? ParticleTypes.SWEEP_ATTACK : ParticleTypes.CLOUD, center.x, center.y, center.z,
                sword ? 1 : 12, .7, .6, .7, .025);
        if (absorbed != null) {
            DustParticleOptions dust = switch (absorbed) {
                case PYRO -> PYRO_DUST; case CRYO -> CRYO_DUST; case HYDRO -> HYDRO_DUST; case ELECTRO -> ELECTRO_DUST;
                default -> throw new IllegalArgumentException("Not an absorbable element");
            };
            level.sendParticles(dust, center.x, center.y, center.z, 12, .8, .8, .8, .02);
        }
    }
}
