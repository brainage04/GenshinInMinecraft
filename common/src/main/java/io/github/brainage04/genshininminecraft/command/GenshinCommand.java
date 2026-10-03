package io.github.brainage04.genshininminecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.brainage04.genshininminecraft.GenshinInMinecraft;
import io.github.brainage04.genshininminecraft.world.ManagedWorldData;
import io.github.brainage04.genshininminecraft.world.TestArena;
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
                        .executes(context -> {
                            var center = TestArena.build(context.getSource().getPlayerOrException());
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Built Genshin arena at " + center.toShortString()
                                            + "; managed mode on, adventure mode enabled."), true);
                            return 1;
                        })));
    }

    private static int setManaged(CommandSourceStack source, boolean enabled) {
        ManagedWorldData.get(source.getServer()).setManaged(source.getServer(), enabled);
        source.sendSuccess(() -> Component.literal("Genshin managed mode: " + (enabled ? "on" : "off")), true);
        return 1;
    }
}
