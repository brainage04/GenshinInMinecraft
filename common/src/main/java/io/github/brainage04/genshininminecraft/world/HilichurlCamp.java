package io.github.brainage04.genshininminecraft.world;

import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.enemy.GenshinEntities;
import io.github.brainage04.genshininminecraft.enemy.Hilichurl;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Command-only encounter: a shared leash anchor with a separate idle position per member. */
public final class HilichurlCamp {
    public static final int DEFAULT_COUNT = 3;
    public static final int MAX_COUNT = 12;
    public static final double SPAWN_RADIUS = 2.5;
    public static final double ARENA_OFFSET = 16;
    private HilichurlCamp() {}

    public static List<Hilichurl> spawn(ServerLevel level, Vec3 anchor, int count, int enemyLevel) {
        List<Hilichurl> members = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            double angle = 2 * Math.PI * index / count;
            Vec3 home = anchor.add(count == 1 ? 0 : Math.sin(angle) * SPAWN_RADIUS, 0,
                    count == 1 ? 0 : Math.cos(angle) * SPAWN_RADIUS);
            // Ground navigation ends at block centres; matching idle points avoids an unreachable sub-block home.
            home = new Vec3(Math.floor(home.x) + .5, home.y, Math.floor(home.z) + .5);
            Hilichurl member = new Hilichurl(GenshinEntities.HILICHURL, level);
            member.setGenshinLevel(enemyLevel);
            member.snapTo(home);
            member.setCamp(anchor, home);
            if (level.addFreshEntity(member)) {
                CombatRuntime.get(level.getServer()).target(member);
                members.add(member);
            }
        }
        return members;
    }
}
