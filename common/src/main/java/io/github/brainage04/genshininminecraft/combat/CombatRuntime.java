package io.github.brainage04.genshininminecraft.combat;

import io.github.brainage04.genshininminecraft.network.CharacterStatePayload;
import io.github.brainage04.genshininminecraft.network.DamageNumberPayload;
import io.github.brainage04.genshininminecraft.network.TargetAuraPayload;
import io.github.brainage04.genshininminecraft.rules.*;
import io.github.brainage04.genshininminecraft.rules.kit.TravelerAnemoKit;
import io.github.brainage04.genshininminecraft.rules.kit.TravelerAnemoKit.Hit;
import io.github.brainage04.genshininminecraft.rules.kit.TravelerAnemoKit.Intent;
import io.github.brainage04.genshininminecraft.rules.kit.TravelerAnemoKit.Kind;
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
import net.minecraft.server.MinecraftServer;
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
    public void forget(UUID uuid) { players.remove(uuid); targets.remove(uuid); }
    /** Loader tracking callbacks refresh existing auras for players entering tracking range. */
    public void startTracking(ServerPlayer player, Entity entity) {
        CombatTarget target = targets.get(entity.getUUID());
        if (target != null && ManagedWorld.isManaged(entity.level()) && player.connection != null) {
            player.connection.send(new ClientboundCustomPayloadPacket(new TargetAuraPayload(entity.getId(), target.auraElements())));
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
                if (old != null && sender != null) sender.accept(player, CharacterStatePayload.UNMANAGED);
                continue;
            }
            Session state = session(player);
            state.reconcileHealth();
            state.advanceTo(frame);
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
        private final TravelerAnemoKit kit;
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

        private Session(ServerPlayer player) {
            this.player = player;
            random = new Random(player.getUUID().getLeastSignificantBits());
            kit = new TravelerAnemoKit(timeline, this::hit);
            mirroredHealth = player.getHealth();
            kit.setHp(kit.maxHp() * player.getHealth() / player.getMaxHealth());
        }
        public TravelerAnemoKit kit() { return kit; }
        public Element skillAbsorbedElement() { return skillAbsorbed; }
        public Element burstAbsorbedElement() { return burstAbsorbed; }
        public boolean intent(Intent intent, long frame) {
            if (!ManagedWorld.isManaged(player.level()) || !player.isAlive() || player.isSpectator()) return false;
            // Drain prior hits before installing a new cast's absorption/origin.
            advanceTo(frame);
            boolean accepted = kit.intent(intent, frame);
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
            kit.advanceTo(frame);
            if (tornadoStart >= 0 && frame != lastTornadoStep && frame - tornadoStart <= TravelerAnemoKit.BURST_DURATION_FRAMES
                    && valid() && player.level() == tornadoLevel) {
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
        private boolean valid() {
            return players.get(player.getUUID()) == this && player.isAlive() && !player.isRemoved() && ManagedWorld.isManaged(player.level());
        }
        private void reconcileHealth() {
            if (player.getHealth() != mirroredHealth) kit.setHp(kit.maxHp() * player.getHealth() / player.getMaxHealth());
            mirroredHealth = (float) (player.getMaxHealth() * kit.hp() / kit.maxHp());
            player.setHealth(mirroredHealth);
        }
        private void sync() {
            // Simulated/offline server players have no channel; only connected clients receive feedback.
            if (player.connection == null || !player.connection.isAcceptingMessages()) return;
            if (lastSyncLevel != player.level()) {
                lastSync = null;
                lastSyncLevel = (ServerLevel) player.level();
            }
            float hp = (float) kit.hp();
            float maxHp = (float) kit.maxHp();
            float energy = (float) kit.energy();
            int skill = (int) kit.skillRemaining();
            int burst = (int) kit.burstRemaining();
            if (lastSync == null || lastSync.hp() != hp || lastSync.maxHp() != maxHp || lastSync.energy() != energy
                    || lastSync.skillRemainingFrames() != skill || lastSync.burstRemainingFrames() != burst) {
                lastSync = new CharacterStatePayload(true, hp, maxHp, energy, skill, burst);
                if (sender != null) sender.accept(player, lastSync);
            }
        }
        private Vec3 tornadoCenter(long frame) {
            return tornadoOrigin.add(tornadoDirection.scale(Frames.seconds(frame - tornadoStart) * TORNADO_BLOCKS_PER_SECOND));
        }
        private void hit(Hit hit) {
            if (!valid()) return;
            ServerLevel level = (ServerLevel) player.level();
            boolean burst = hit.kind() == Kind.TORNADO;
            if (level != (burst ? tornadoLevel : actionLevel)) return;
            Vec3 origin = burst ? tornadoCenter(hit.frame()) : player.position().add(0, .8, 0);
            boolean sword = hit.kind() == Kind.NORMAL || hit.kind() == Kind.CHARGED;
            double radius = sword ? SWORD_RADIUS : burst ? TORNADO_RADIUS : SKILL_RADIUS;
            List<LivingEntity> enemies = nearby(level, origin, radius);
            if (!burst) {
                Vec3 facing = forward(player);
                enemies.removeIf(enemy -> {
                    double dx = enemy.getX() - player.getX();
                    double dz = enemy.getZ() - player.getZ();
                    double distanceSquared = dx * dx + dz * dz;
                    return distanceSquared > .01 && (facing.x * dx + facing.z * dz)
                            < (sword ? SWORD_ARC_COSINE : 0) * Math.sqrt(distanceSquared);
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
            particles(level, origin.add(burst ? Vec3.ZERO : forward(player).scale(1.5)), absorbed, sword);
            boolean enemyHit = false;
            for (LivingEntity enemy : enemies) {
                CombatTarget target = target(enemy);
                enemyHit |= deal(target, hit.element(), hit.multiplier(), hit.gauge(), hit.icdTag(), hit.frame());
                if (absorbed != null && hit.absorbedHit() && enemy.isAlive()
                        && (!burst || enemy.position().distanceToSqr(origin) <= ABSORBED_TORNADO_RADIUS * ABSORBED_TORNADO_RADIUS)) {
                    deal(target, absorbed, burst ? .248 : hit.multiplier() * .25,
                            burst ? 2 : 1, hit.kind() == Kind.STORM ? null
                                    : (burst ? "Elemental Burst " : "Elemental Skill ") + absorbed, hit.frame());
                }
            }
            if (enemyHit && hit.particles() > 0) kit.grantParticles(hit.particles());
        }
        private boolean deal(CombatTarget target, Element element, double multiplier, double gauge, String tag, long frame) {
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
            kit.schedule(at, frame -> {
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
