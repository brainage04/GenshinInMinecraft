package io.github.brainage04.genshininminecraft.combat;

import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import io.github.brainage04.genshininminecraft.network.DamageNumberPayload;
import io.github.brainage04.genshininminecraft.network.TargetAuraPayload;
import io.github.brainage04.genshininminecraft.network.PlayerCharacterPayload;
import io.github.brainage04.genshininminecraft.rules.*;
import io.github.brainage04.genshininminecraft.rules.kit.TravelerAnemoKit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Hit;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Intent;
import io.github.brainage04.genshininminecraft.rules.kit.CharacterKit.Kind;
import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.particles.DustParticleOptions;
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
    public static void stop(MinecraftServer server) { SERVERS.remove(server); }
    public static boolean cancelsVanillaMelee(Player player) { return ManagedWorld.isManaged(player.level()); }
    public Session session(ServerPlayer player) {
        Session previous = players.get(player.getUUID());
        if (previous != null && previous.player != player) players.remove(player.getUUID());
        return players.computeIfAbsent(player.getUUID(), ignored -> new Session(player));
    }
    public CombatTarget target(LivingEntity entity) {
        if (entity instanceof Player) throw new IllegalArgumentException("Players are not kit targets");
        return targets.computeIfAbsent(entity.getUUID(), ignored -> new CombatTarget(entity));
    }
    public void forget(UUID uuid) {
        players.remove(uuid);
        CombatTarget removed = targets.remove(uuid);
        if (removed != null) broadcast(removed, new TargetAuraPayload(removed.entity().getId(), 0));
    }
    /** Replacing the profile invalidates queued EC callbacks as well as aura/ICD state. */
    public void resetTarget(LivingEntity entity) {
        forget(entity.getUUID());
        entity.setHealth(entity.getMaxHealth());
        syncAura(target(entity));
    }
    public boolean enemyHit(ServerPlayer player, LivingEntity enemy, double attack, double multiplier, int enemyLevel) {
        if (!ManagedWorld.isManaged(player.level()) || player.isCreative() || player.isSpectator() || !player.isAlive()) return false;
        Session state = session(player);
        long frame = Math.max(state.party.frame(), Frames.atServerTick(player.level().getServer().getTickCount()));
        if (state.stamina().dashInvulnerable(frame)) return false;
        state.reconcileHealth();
        CharacterKit kit = state.kit();
        double amount = Damage.enemyDamage(attack, multiplier, enemyLevel, kit.stats().def(),
                HilichurlProfile.STARTER_PLAYER_RESISTANCE);
        ServerLevel level = (ServerLevel) player.level();
        var source = level.damageSources().mobAttack(enemy);
        if (player.isInvulnerableTo(level, source)) return false;
        kit.setHp(Math.max(0, kit.hp() - amount));
        player.setLastHurtByMob(enemy);
        player.getCombatTracker().recordDamage(source, (float) (amount * player.getMaxHealth() / kit.maxHp()));
        if (kit.hp() == 0) state.forceSwitch(frame);
        state.mirrorHealth();
        level.broadcastDamageEvent(player, source);
        if (state.kit().hp() == 0) player.die(source);
        state.sync();
        return true;
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
            player.connection.send(new ClientboundCustomPayloadPacket(new TargetAuraPayload(entity.getId(), target.auraElements())));
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
        if (target.auraChanged()) broadcast(target, new TargetAuraPayload(target.entity().getId(), target.auraElements()));
    }
    public boolean receive(ServerPlayer player, Intent intent) {
        if (!ManagedWorld.isManaged(player.level()) || !player.isAlive() || player.isSpectator()) return false;
        var state = session(player);
        state.reconcileHealth();
        return state.intent(intent, Frames.atServerTick(player.level().getServer().getTickCount()));
    }
    public void tick(MinecraftServer server) {
        long frame = Frames.atServerTick(server.getTickCount());
        boolean managed = ManagedWorld.isManaged(server.overworld());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!managed) {
                Session old = players.remove(player.getUUID());
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
        players.values().removeIf(state -> state.player.isRemoved());
        targets.values().removeIf(target -> target.entity().isRemoved());
        if (managed) {
            for (CombatTarget target : targets.values()) {
                if (frame >= target.aura().frame()) target.aura().advanceTo(frame);
                syncAura(target);
            }
        }
        if (!managed) targets.clear();
    }

    public final class Session {
        private final ServerPlayer player;
        private final Party party;
        private final Random random;
        private CharacterStatePayload lastSync;
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
        private ServerLevel lastSyncLevel;
        private long lastSyncFrame;
        private long lastMovementFrame = -1;
        private boolean sprintKeyHeld;
        private Vec3 dashMotion;
        private ServerLevel dashLevel;
        private int displayedSlot = -1;
        private ServerLevel displayedLevel;

        private Session(ServerPlayer player) {
            this.player = player;
            random = new Random(player.getUUID().getLeastSignificantBits());
            party = new Party(timeline, this::hit);
            mirroredHealth = player.getHealth();
            kit().setHp(kit().maxHp() * player.getHealth() / player.getMaxHealth());
        }
        public CharacterKit kit() { return party.activeKit(); }
        public Party party() { return party; }
        public Stamina stamina() { return party.stamina(); }
        public Element skillAbsorbedElement() { return skillAbsorbed; }
        public Element burstAbsorbedElement() { return burstAbsorbed; }
        public boolean intent(Intent intent, long frame) {
            if (!ManagedWorld.isManaged(player.level()) || !player.isAlive() || player.isSpectator()) return false;
            // Drain prior hits before installing a new cast's absorption/origin.
            advanceTo(frame);
            int slot = intent.switchSlot();
            if (slot >= 0) {
                boolean switched = party.switchTo(slot, frame);
                if (switched) { mirrorHealth(); switchEffect(); }
                sync();
                return switched;
            }
            if (!kit().talentsAvailable() && (intent == Intent.SKILL_PRESS || intent == Intent.BURST_PRESS)) {
                player.sendOverlayMessage(Component.literal("This character's " +
                        (intent == Intent.SKILL_PRESS ? "skill" : "burst") + " is not available yet."));
                return false;
            }
            boolean accepted = kit().intent(intent, frame);
            if (accepted && (intent == Intent.ATTACK_PRESS || intent == Intent.SKILL_PRESS)) actionLevel = (ServerLevel) player.level();
            if (accepted && intent == Intent.SKILL_PRESS) { skillCast = frame; skillAbsorbed = null; }
            if (accepted && intent == Intent.BURST_PRESS) {
                tornadoStart = frame;
                tornadoOrigin = player.position().add(0, .8, 0);
                tornadoDirection = forward(player);
                tornadoLevel = (ServerLevel) player.level();
                burstAbsorbed = null;
            }
            sync();
            return accepted;
        }
        public void advanceTo(long frame) {
            party.advanceTo(frame);
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
        }
        /** Vanilla's input packet supplies the sprint-key edge and camera-relative WASD direction. */
        public void tickMovement(long frame) {
            advanceTo(frame);
            if (lastMovementFrame == frame) return;
            lastMovementFrame = frame;
            var input = player.getLastClientInput();
            boolean held = input.sprint();
            boolean moving = input.forward() != input.backward() || input.left() != input.right();
            boolean eligible = valid() && !player.isSpectator() && !player.isPassenger()
                    && !player.getAbilities().flying && !player.isInWater() && !player.isFallFlying()
                    && !input.shift();
            if (eligible && moving && held && !sprintKeyHeld && player.onGround() && stamina().dash(frame)) {
                dashMotion = player.getLastClientMoveIntent().scale(ADAPTED_DASH_BLOCKS_PER_TICK);
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
        private void setHorizontalMotion(Vec3 motion) {
            player.setDeltaMovement(motion.x, player.getDeltaMovement().y, motion.z);
            player.hurtMarked = true;
            // ServerPlayer movement is client-simulated; explicitly send the authoritative impulse
            // to its owner (entity tracking alone does not send that player's own velocity).
            if (player.connection != null && player.connection.isAcceptingMessages())
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
        private boolean valid() {
            return players.get(player.getUUID()) == this && player.isAlive() && !player.isRemoved() && ManagedWorld.isManaged(player.level());
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
            boolean switched = party.forceSwitch(frame);
            if (switched) switchEffect();
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
            if (lastSync != null && party.frame() - lastSyncFrame < Frames.atServerTick(CHARACTER_SYNC_INTERVAL_TICKS)) return;
            float stamina = (float) stamina().current();
            boolean exhausted = stamina().exhausted();
            boolean draining = stamina().draining();
            boolean changed = lastSync == null || lastSync.activeSlot() != party.activeSlot()
                    || lastSync.stamina() != stamina || lastSync.staminaExhausted() != exhausted
                    || lastSync.staminaDraining() != draining;
            for (int index = 0; !changed && index < Party.SIZE; index++) {
                var member = party.kit(index);
                var previous = lastSync.members().get(index);
                changed = previous.hpFraction() != (float) (member.hp() / member.maxHp())
                        || previous.energy() != (float) member.energy()
                        || previous.skillRemainingFrames() != member.skillRemaining()
                        || previous.burstRemainingFrames() != member.burstRemaining();
            }
            if (!changed) return;
            var members = new java.util.ArrayList<CharacterStatePayload.Member>(Party.SIZE);
            for (int index = 0; index < Party.SIZE; index++) {
                var member = party.kit(index);
                members.add(new CharacterStatePayload.Member((float) (member.hp() / member.maxHp()),
                        (float) member.energy(), (int) member.skillRemaining(), (int) member.burstRemaining()));
            }
            lastSync = new CharacterStatePayload(true, party.activeSlot(), members, stamina, exhausted, draining);
            lastSyncFrame = party.frame();
            if (sender != null) sender.accept(player, lastSync);
        }
        private Vec3 tornadoCenter(long frame) {
            return tornadoOrigin.add(tornadoDirection.scale(Frames.seconds(frame - tornadoStart) * TORNADO_BLOCKS_PER_SECOND));
        }
        private void hit(CharacterKit kit, Hit hit) {
            if (!valid()) return;
            ServerLevel level = (ServerLevel) player.level();
            boolean burst = hit.kind() == Kind.TORNADO;
            if (level != (burst ? tornadoLevel : actionLevel)) return;
            Vec3 origin = burst ? tornadoCenter(hit.frame()) : player.position().add(0, .8, 0);
            boolean basic = hit.kind() == Kind.NORMAL || hit.kind() == Kind.CHARGED;
            boolean sword = basic && kit.weapon() == CharacterKit.Weapon.SWORD;
            boolean ranged = basic && !sword && hit.kind() == Kind.NORMAL;
            boolean catalystCharge = basic && !sword && !ranged;
            double radius = sword ? SWORD_RADIUS : catalystCharge ? ADAPTED_LISA_CHARGED_RADIUS
                    : burst ? TORNADO_RADIUS : SKILL_RADIUS;
            List<LivingEntity> enemies = ranged ? rayTargets(level, kit.weapon()) : nearby(level, origin, radius);
            if (!burst && !ranged) {
                Vec3 facing = forward(player);
                enemies.removeIf(enemy -> {
                    double dx = enemy.getX() - player.getX();
                    double dz = enemy.getZ() - player.getZ();
                    double distanceSquared = dx * dx + dz * dz;
                    return catalystCharge && enemy.getY() > player.getY() + ADAPTED_LISA_CHARGED_HEIGHT_TOLERANCE
                            || distanceSquared > .01 && (facing.x * dx + facing.z * dz)
                            < (sword || catalystCharge ? SWORD_ARC_COSINE : 0) * Math.sqrt(distanceSquared);
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
            particles(level, origin.add(burst ? Vec3.ZERO : forward(player).scale(1.5)),
                    basic && hit.element() == Element.ELECTRO ? Element.ELECTRO : absorbed, sword);
            boolean enemyHit = false;
            for (LivingEntity enemy : enemies) {
                CombatTarget target = target(enemy);
                enemyHit |= deal(kit, target, hit.element(), hit.multiplier(), hit.gauge(), hit.icdTag(), hit.frame());
                if (absorbed != null && hit.absorbedHit() && enemy.isAlive()
                        && (!burst || enemy.position().distanceToSqr(origin) <= ABSORBED_TORNADO_RADIUS * ABSORBED_TORNADO_RADIUS)) {
                    deal(kit, target, absorbed, burst ? .248 : hit.multiplier() * .25,
                            burst ? 2 : 1, hit.kind() == Kind.STORM ? null
                                    : (burst ? "Elemental Burst " : "Elemental Skill ") + absorbed, hit.frame());
                }
            }
            if (enemyHit && hit.particles() > 0) party.collect(Energy.Item.PARTICLE, hit.element(), hit.particles());
        }
        /** Nearest unobstructed living target on the server eye ray; Amber hits at arrow release. */
        private List<LivingEntity> rayTargets(ServerLevel level, CharacterKit.Weapon weapon) {
            double range = weapon == CharacterKit.Weapon.BOW ? ADAPTED_BOW_RANGE : ADAPTED_CATALYST_RANGE;
            Vec3 start = player.getEyePosition();
            Vec3 end = start.add(player.getLookAngle().scale(range));
            end = level.clip(new net.minecraft.world.level.ClipContext(start, end,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, player)).getLocation();
            LivingEntity nearest = null;
            double distance = start.distanceToSqr(end);
            for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(ADAPTED_RAY_HITBOX_MARGIN),
                    entity -> !(entity instanceof Player) && entity.isAlive() && !entity.isRemoved())) {
                var impact = enemy.getBoundingBox().inflate(ADAPTED_RAY_HITBOX_MARGIN).clip(start, end);
                if (impact.isPresent() && start.distanceToSqr(impact.get()) < distance) {
                    nearest = enemy;
                    distance = start.distanceToSqr(impact.get());
                }
            }
            if (nearest == null) return List.of();
            if (weapon == CharacterKit.Weapon.BOW) {
                level.sendParticles(ParticleTypes.CRIT, nearest.getX(), nearest.getY() + 1, nearest.getZ(), 8, .1, .2, .1, .01);
                return List.of(nearest);
            }
            return nearby(level, nearest.position(), ADAPTED_LISA_IMPACT_RADIUS);
        }
        private boolean deal(CharacterKit kit, CombatTarget target, Element element, double multiplier, double gauge, String tag, long frame) {
            target.aura().advanceTo(frame);
            List<Reaction> reactions = List.of();
            if (gauge > 0 && target.application.allowsApplication(player.getUUID(), tag, target.entity().getUUID(), frame)) {
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
            boolean critical = random.nextDouble() < Math.clamp(kit.stats().critRate(), 0, 1);
            double amount = Damage.talentDamage(kit.stats(), multiplier, element, target.level(), 0, 0,
                    target.resistance(element), amplification, 0, critical ? Damage.CritMode.CRIT : Damage.CritMode.NON_CRIT, null);
            boolean applied = target.damage(player, amount);
            if (applied) feedback(target, amount, element, amplification == null ? null : amplification.type(), critical);
            for (Reaction reaction : reactions) {
                if (reaction.type() == Reaction.Type.SWIRL) target.countSwirl();
                if (reaction.amplifying()) continue;
                if (reaction.type() == Reaction.Type.FROZEN || reaction.type() == Reaction.Type.ELECTRO_CHARGED) {
                    feedback(target, 0, element, reaction.type(), false);
                    continue;
                }
                Element damageElement = Reaction.damageElement(reaction.type(), reaction.auraElement());
                double reactionAmount = 0;
                if (target.reactionDamage.allowsDamage(player.getUUID(), target.entity().getUUID(), reaction.type(), reaction.auraElement(), frame)) {
                    double damage = Damage.transformativeDamage(reaction.type(), kit.stats().level(), kit.stats().elementalMastery(), 0,
                            target.resistance(damageElement));
                    if (target.damage(player, damage)) reactionAmount = damage;
                }
                feedback(target, reactionAmount, damageElement, reaction.type(), false);
            }
            syncAura(target);
            scheduleEc(target);
            return applied;
        }
        private void scheduleEc(CombatTarget target) {
            var next = target.aura().nextElectroChargedTickFrame();
            if (next.isEmpty() || target.ecOwner == null || next.getAsLong() == target.scheduledEc) return;
            long at = next.getAsLong();
            target.scheduledEc = at;
            timeline.schedule(at, frame -> {
                if (targets.get(target.entity().getUUID()) != target || target.entity().isRemoved()
                        || !ManagedWorld.isManaged(target.entity().level())) return;
                var tick = target.aura().tickElectroCharged(frame, target.entity().isAlive());
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
    private static List<LivingEntity> nearby(ServerLevel level, Vec3 center, double radius) {
        double radiusSquared = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, 2, radius),
                entity -> !(entity instanceof Player) && entity.isAlive() && !entity.isRemoved()
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
