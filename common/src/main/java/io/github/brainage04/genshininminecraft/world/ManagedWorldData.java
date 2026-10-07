package io.github.brainage04.genshininminecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.brainage04.genshininminecraft.GenshinInMinecraft;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** One switch for the whole save (all dimensions), stored in the overworld. */
public final class ManagedWorldData extends SavedData {
    public static final Codec<ManagedWorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("managed").forGetter(data -> data.managed),
            GameRuleMap.CODEC.fieldOf("previous_rules").forGetter(data -> data.previousRules)
    ).apply(instance, ManagedWorldData::new));
    public static final SavedDataType<ManagedWorldData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(GenshinInMinecraft.MOD_ID, "managed_world"),
            ManagedWorldData::new, CODEC, null);

    private boolean managed;
    private GameRuleMap previousRules;

    public ManagedWorldData() {
        this(false, GameRuleMap.of());
    }

    private ManagedWorldData(boolean managed, GameRuleMap previousRules) {
        this.managed = managed;
        this.previousRules = previousRules;
    }

    public static ManagedWorldData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isManaged() {
        return managed;
    }

    public void setManaged(MinecraftServer server, boolean enabled) {
        if (managed == enabled) {
            return; // Repeated "on" must not overwrite the original values.
        }
        GameRules rules = server.getGameRules();
        if (enabled) {
            previousRules = GameRuleMap.of();
            rememberAndSet(rules, GameRules.NATURAL_HEALTH_REGENERATION, false, server);
            rememberAndSet(rules, GameRules.SPAWN_MOBS, false, server);
            rememberAndSet(rules, GameRules.MOB_GRIEFING, false, server);
            // 26.2 has no doFireTick: a radius of zero disables fire/lava spread everywhere.
            rememberAndSet(rules, GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0, server);
        } else {
            CombatRuntime.suspend(server);
            rules.setAll(previousRules, server);
            previousRules = GameRuleMap.of();
        }
        managed = enabled;
        setDirty();
    }

    private <T> void rememberAndSet(GameRules rules, GameRule<T> rule, T value, MinecraftServer server) {
        previousRules.set(rule, rules.get(rule));
        rules.set(rule, value, server);
    }
}
