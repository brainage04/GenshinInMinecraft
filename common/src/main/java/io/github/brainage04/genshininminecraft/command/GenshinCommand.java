package io.github.brainage04.genshininminecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.brainage04.genshininminecraft.combat.CombatRuntime;
import io.github.brainage04.genshininminecraft.rules.Element;
import io.github.brainage04.genshininminecraft.rules.Frames;
import io.github.brainage04.genshininminecraft.rules.HilichurlProfile;
import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import io.github.brainage04.genshininminecraft.GenshinInMinecraft;
import io.github.brainage04.genshininminecraft.world.ManagedWorldData;
import io.github.brainage04.genshininminecraft.world.TestArena;
import io.github.brainage04.genshininminecraft.world.HilichurlCamp;
import io.github.brainage04.genshininminecraft.world.OverlayDefinition;
import io.github.brainage04.genshininminecraft.world.OverlayRuntime;
import net.minecraft.world.phys.Vec3;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class GenshinCommand {
    public static final String COMMAND_NAME = "genshin";

    private GenshinCommand() {
    }

    public static int execute(CommandSourceStack source) {
        source.sendSuccess(
                () -> Component.literal(GenshinInMinecraft.MOD_NAME + " " + GenshinInMinecraft.getVersion()),
                false);
        return 1;
    }

    public static void initialize(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(COMMAND_NAME)
                .executes(context -> execute(context.getSource()))
                .then(auraCommand())
                .then(Commands.literal("overlay")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("apply").executes(context -> {
                            var source = context.getSource();
                            if (!OverlayRuntime.apply(source.getServer(), OverlayDefinition.mondstadt())) {
                                source.sendFailure(Component.literal("Enable managed mode before applying the overlay."));
                                return 0;
                            }
                            source.sendSuccess(() -> Component.literal("Mondstadt overlay installed; loaded anchors reconcile without editing blocks."), true);
                            return 1;
                        })))
                .then(Commands.literal("teleport").executes(context -> {
                    OverlayRuntime.openList(context.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("managed")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("on").executes(context -> setManaged(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> setManaged(context.getSource(), false)))
                        .then(Commands.literal("status").executes(context -> {
                            boolean managed = ManagedWorldData.get(context.getSource().getServer()).isManaged();
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Genshin managed mode: " + (managed ? "on" : "off")), false);
                            return managed ? 1 : 0;
                        })))
                .then(Commands.literal("arena")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> arena(context.getSource(), false))
                        .then(Commands.literal("camp").executes(context -> arena(context.getSource(), true))))
                .then(Commands.literal("camp")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("hilichurl")
                                .executes(context -> camp(context.getSource(), HilichurlCamp.DEFAULT_COUNT, HilichurlProfile.DEFAULT_CAMP_LEVEL))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, HilichurlCamp.MAX_COUNT))
                                        .executes(context -> camp(context.getSource(), IntegerArgumentType.getInteger(context, "count"), HilichurlProfile.DEFAULT_CAMP_LEVEL))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(HilichurlProfile.MIN_LEVEL, HilichurlProfile.MAX_LEVEL))
                                                .executes(context -> camp(context.getSource(), IntegerArgumentType.getInteger(context, "count"),
                                                        IntegerArgumentType.getInteger(context, "level"))))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> auraCommand() {
        var command = Commands.literal("aura").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        for (Element element : new Element[]{Element.PYRO, Element.CRYO, Element.ELECTRO, Element.HYDRO}) {
            command.then(Commands.literal(element.name().toLowerCase(java.util.Locale.ROOT))
                    .then(Commands.argument("gauge", DoubleArgumentType.doubleArg(Double.MIN_VALUE))
                            .executes(context -> aura(context.getSource(), element, DoubleArgumentType.getDouble(context, "gauge")))));
        }
        return command;
    }

    private static int aura(CommandSourceStack source, Element element, double gauge)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        if (!ManagedWorld.isManaged(player.level())) {
            source.sendFailure(Component.literal("Enable Genshin managed mode before applying a debug aura."));
            return 0;
        }
        var runtime = CombatRuntime.get(source.getServer());
        var session = runtime.session(player);
        long frame = Math.max(session.party().frame(), Frames.atServerTick(source.getServer().getTickCount()));
        var target = session.debugAura(element, gauge, frame);
        if (target == null) {
            source.sendFailure(Component.literal("No combat target within16 blocks."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Applied " + gauge + "U " + element.name()
                + " to " + target.getName().getString() + "."), false);
        return 1;
    }

    private static int arena(CommandSourceStack source, boolean withCamp) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        var center = TestArena.build(player);
        if (withCamp) HilichurlCamp.spawn(player.level(), Vec3.atLowerCornerOf(center).add(0, 1, HilichurlCamp.ARENA_OFFSET),
                HilichurlCamp.DEFAULT_COUNT, HilichurlProfile.DEFAULT_CAMP_LEVEL);
        source.sendSuccess(() -> Component.literal("Built Genshin arena at " + center.toShortString()
                + "; managed mode on, adventure mode enabled" + (withCamp ? ", hilichurl camp placed." : ".")), true);
        return 1;
    }

    private static int camp(CommandSourceStack source, int count, int level) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        int spawned = HilichurlCamp.spawn(player.level(), player.position(), count, level).size();
        source.sendSuccess(() -> Component.literal("Spawned hilichurl camp: " + spawned + " members, Lv. " + level + "."), true);
        return spawned;
    }

    private static int setManaged(CommandSourceStack source, boolean enabled) {
        ManagedWorldData.get(source.getServer()).setManaged(source.getServer(), enabled);
        source.sendSuccess(() -> Component.literal("Genshin managed mode: " + (enabled ? "on" : "off")), true);
        return 1;
    }
}
