package io.github.brainage04.genshininminecraft.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import io.github.brainage04.genshininminecraft.world.ManagedWorldData;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Synchronous tests: world-global rules are restored before another test can tick. */
public final class ManagedWorldGameTests {
    private ManagedWorldGameTests() {
    }

    public static void managedBlockBreaking(GameTestHelper context) {
        withWorld(context, data -> {
            ServerPlayer player = player(context);
            BlockPos pos = context.absolutePos(new BlockPos(1, 1, 1));
            context.getLevel().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
            data.setManaged(context.getLevel().getServer(), true);
            context.assertFalse(player.gameMode.destroyBlock(pos), "Managed survival break must be rejected by the loader event");
            context.assertTrue(context.getLevel().getBlockState(pos).is(Blocks.DIRT), "Rejected break must leave the block intact");
            player.setGameMode(GameType.CREATIVE);
            context.assertTrue(player.gameMode.destroyBlock(pos), "Creative owner must bypass managed break protection");
            context.assertTrue(context.getLevel().getBlockState(pos).isAir(), "Creative break must actually remove the block");
            player.setGameMode(GameType.SURVIVAL);
            data.setManaged(context.getLevel().getServer(), false);
            context.getLevel().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
            context.assertTrue(player.gameMode.destroyBlock(pos), "Survival breaking must resume after managed off");
            context.assertTrue(context.getLevel().getBlockState(pos).isAir(), "Unmanaged survival break must remove the block");
        });
        context.succeed();
    }

    public static void managedHunger(GameTestHelper context) {
        var server = context.getLevel().getServer();
        Difficulty originalDifficulty = server.getWorldData().getDifficulty();
        try {
            server.setDifficulty(Difficulty.NORMAL, true);
            withWorld(context, data -> {
                ServerPlayer player = player(context);
                player.setHealth(10.0F);
                FoodData food = player.getFoodData();
                food.setFoodLevel(0);
                food.setSaturation(0.0F);
                data.setManaged(server, true);
                for (int tick = 0; tick < 120; tick++) {
                    food.addExhaustion(40.0F);
                    food.tick(player);
                }
                context.assertValueEqual(food.getFoodLevel(), 20, "Managed food level");
                context.assertValueEqual(food.getSaturationLevel(), 20.0F, "Managed saturation");
                context.assertValueEqual(player.getHealth(), 10.0F, "Managed hunger must neither starve nor heal");
                TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
                food.addAdditionalSaveData(output);
                context.assertValueEqual(output.buildResult().getFloatOr("foodExhaustionLevel", -1), 0.0F, "Managed exhaustion is discarded");
                data.setManaged(server, false);
                food.setSaturation(0.0F);
                food.tick(player);
                context.assertValueEqual(food.getFoodLevel(), 20, "Managed exhaustion must not leak into off mode");
                food.addExhaustion(5.0F);
                food.tick(player);
                context.assertValueEqual(food.getFoodLevel(), 19, "Unmanaged exhaustion must consume food");
            });
        } finally {
            server.setDifficulty(originalDifficulty, true);
        }
        context.succeed();
    }

    public static void managedGamerulesRestored(GameTestHelper context) {
        withWorld(context, data -> {
            var server = context.getLevel().getServer();
            GameRules rules = server.getGameRules();
            rules.set(GameRules.NATURAL_HEALTH_REGENERATION, true, server);
            rules.set(GameRules.SPAWN_MOBS, true, server);
            rules.set(GameRules.MOB_GRIEFING, false, server); // Preserve a non-default original value too.
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 73, server);
            boolean weather = rules.get(GameRules.ADVANCE_WEATHER);
            command(context, source(context), "genshin managed on");
            assertDisabledRules(context);
            command(context, source(context), "genshin managed on");
            command(context, source(context), "genshin managed off");
            context.assertTrue(rules.get(GameRules.NATURAL_HEALTH_REGENERATION), "Restore natural regeneration");
            context.assertTrue(rules.get(GameRules.SPAWN_MOBS), "Restore mob spawning");
            context.assertFalse(rules.get(GameRules.MOB_GRIEFING), "Restore original false mob griefing, not its default");
            context.assertValueEqual(rules.get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER), 73, "Restore exact fire radius");
            context.assertValueEqual(rules.get(GameRules.ADVANCE_WEATHER), weather, "Weather cycle is untouched");
            context.assertFalse(data.isManaged(), "Off command disables the flag");
        });
        context.succeed();
    }

    public static void managedSavedDataRoundTrip(GameTestHelper context) {
        withWorld(context, data -> {
            var server = context.getLevel().getServer();
            server.getGameRules().set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, -1, server);
            data.setManaged(server, true);
            var tag = ManagedWorldData.TYPE.codec().encodeStart(NbtOps.INSTANCE, data).getOrThrow();
            ManagedWorldData loaded = ManagedWorldData.TYPE.codec().parse(NbtOps.INSTANCE, tag).getOrThrow();
            server.overworld().getDataStorage().set(ManagedWorldData.TYPE, loaded);
            context.assertTrue(ManagedWorld.isManaged(context.getLevel()), "Persisted flag must be read from saved data");
            context.assertTrue(ManagedWorld.isManaged(server.getLevel(Level.NETHER)), "One saved-world switch covers other dimensions");
            loaded.setManaged(server, false);
            context.assertValueEqual(server.getGameRules().get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER), -1, "Original rules survive serialization");
            var offTag = ManagedWorldData.TYPE.codec().encodeStart(NbtOps.INSTANCE, loaded).getOrThrow();
            context.assertFalse(ManagedWorldData.TYPE.codec().parse(NbtOps.INSTANCE, offTag).getOrThrow().isManaged(), "Off flag survives serialization");
        });
        context.succeed();
    }

    public static void managedPlacementAndTrampling(GameTestHelper context) {
        withWorld(context, data -> {
            var level = context.getLevel();
            var server = level.getServer();
            ServerPlayer player = player(context);
            BlockPos floor = context.absolutePos(new BlockPos(1, 1, 1));
            level.setBlockAndUpdate(floor, Blocks.SMOOTH_STONE.defaultBlockState());
            level.setBlockAndUpdate(floor.above(), Blocks.AIR.defaultBlockState());
            data.setManaged(server, true);
            context.assertValueEqual(place(player, floor), InteractionResult.FAIL, "Managed survival placement is refused");
            context.assertTrue(level.getBlockState(floor.above()).isAir(), "No survival block was placed");
            player.setGameMode(GameType.ADVENTURE);
            context.assertValueEqual(place(player, floor), InteractionResult.FAIL, "Managed adventure placement is refused");
            context.assertTrue(ManagedWorld.preventsBlockModification(level, player), "Adventure shares the break policy");
            player.setGameMode(GameType.CREATIVE);
            context.assertTrue(place(player, floor) instanceof InteractionResult.Success, "Creative placement bypass");
            context.assertTrue(level.getBlockState(floor.above()).is(Blocks.STONE), "Creative must actually place a block");
            for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.ADVENTURE, GameType.CREATIVE}) {
                player.setGameMode(mode);
                level.setBlockAndUpdate(floor.above(), Blocks.AIR.defaultBlockState());
                BucketItem waterBucket = (BucketItem) Items.WATER_BUCKET;
                boolean allowed = mode == GameType.CREATIVE;
                context.assertValueEqual(waterBucket.emptyContents(player, level, floor.above(), null), allowed,
                        "Fluid placement protection/bypass for " + mode);
                context.assertTrue(level.getBlockState(floor.above()).is(allowed ? Blocks.WATER : Blocks.AIR),
                        "Fluid placement world result for " + mode);
                if (!allowed) {
                    context.assertValueEqual(Items.BUCKET.use(level, player, InteractionHand.MAIN_HAND), InteractionResult.FAIL,
                            "Bucket pickup must not remove managed source blocks");
                }
                for (var item : new net.minecraft.world.item.Item[]{Items.FLINT_AND_STEEL, Items.FIRE_CHARGE}) {
                    level.setBlockAndUpdate(floor.above(), Blocks.AIR.defaultBlockState());
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                    InteractionResult result = item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
                    context.assertTrue(allowed ? result instanceof InteractionResult.Success : result == InteractionResult.FAIL,
                            "Fire placement protection/bypass for " + mode);
                    context.assertTrue(level.getBlockState(floor.above()).is(allowed ? Blocks.FIRE : Blocks.AIR),
                            "Fire placement world result for " + mode);
                }
            }
            BlockPos farmland = context.absolutePos(new BlockPos(2, 1, 1));
            for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.ADVENTURE, GameType.CREATIVE}) {
                player.setGameMode(mode);
                level.setBlockAndUpdate(farmland, Blocks.FARMLAND.defaultBlockState());
                Blocks.FARMLAND.fallOn(level, level.getBlockState(farmland), farmland, player, 1.6);
                context.assertTrue(level.getBlockState(farmland).is(mode == GameType.CREATIVE ? Blocks.DIRT : Blocks.FARMLAND),
                        "Farmland trample protection/bypass for " + mode);
            }
            data.setManaged(server, false);
            player.setGameMode(GameType.SURVIVAL);
            level.setBlockAndUpdate(floor.above(), Blocks.AIR.defaultBlockState());
            context.assertTrue(place(player, floor) instanceof InteractionResult.Success, "Unmanaged placement resumes");
            level.setBlockAndUpdate(farmland, Blocks.FARMLAND.defaultBlockState());
            Blocks.FARMLAND.fallOn(level, level.getBlockState(farmland), farmland, player, 1.6);
            context.assertTrue(level.getBlockState(farmland).is(Blocks.DIRT), "Unmanaged trampling resumes");
        });
        context.succeed();
    }

    public static void managedFireAndMobExplosion(GameTestHelper context) {
        withWorld(context, data -> {
            var level = context.getLevel();
            data.setManaged(level.getServer(), true);
            BlockPos pos = context.absolutePos(new BlockPos(1, 1, 1));
            context.assertFalse(level.canSpreadFireAround(pos), "Zero fire radius denies both fire and lava spread");
            level.setBlockAndUpdate(pos, Blocks.OAK_PLANKS.defaultBlockState());
            level.setBlockAndUpdate(pos.above(), Blocks.FIRE.defaultBlockState());
            for (int tick = 0; tick < 80; tick++) {
                context.tickBlock(context.relativePos(pos.above()));
            }
            context.assertTrue(level.getBlockState(pos).is(Blocks.OAK_PLANKS), "Environmental fire must not consume flammable map blocks");
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4.0F, Level.ExplosionInteraction.MOB);
            context.assertTrue(level.getBlockState(pos).is(Blocks.OAK_PLANKS), "Mob explosion must preserve blocks");
            data.setManaged(level.getServer(), false);
            level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4.0F, Level.ExplosionInteraction.MOB);
            context.assertTrue(level.getBlockState(pos).isAir(), "Control explosion must destroy the same block with griefing enabled");
        });
        context.succeed();
    }

    public static void managedCommandPermissions(GameTestHelper context) {
        withWorld(context, data -> {
            // A player source prevents a missing-player error from masking a broken arena permission check.
            CommandSourceStack source = source(context).withEntity(player(context));
            for (String command : new String[]{"genshin managed on", "genshin managed off", "genshin managed status", "genshin arena"}) {
                expectDenied(context, source.withPermission(LevelBasedPermissionSet.ALL), command);
                expectDenied(context, source.withPermission(LevelBasedPermissionSet.MODERATOR), command);
            }
            command(context, source.withPermission(LevelBasedPermissionSet.GAMEMASTER), "genshin managed on");
            context.assertTrue(data.isManaged(), "Operator level 2 can enable managed mode");
            context.assertValueEqual(command(context, source, "genshin managed status"), 1, "Status reports enabled");
            command(context, source, "genshin managed off");
            context.assertValueEqual(command(context, source, "genshin managed status"), 0, "Status reports disabled");
        });
        context.succeed();
    }

    public static void arenaCommandBuilds(GameTestHelper context) {
        withWorld(context, data -> {
            var level = context.getLevel();
            ServerPlayer player = player(context);
            BlockPos center = context.absolutePos(new BlockPos(0, 1, 256));
            player.snapTo(center.getX() + 0.3, center.getY() + 1, center.getZ() + 0.8);
            BlockState[] original = new BlockState[50 * 50 * 6];
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            int index = 0;
            for (int x = -25; x <= 24; x++) {
                for (int z = -25; z <= 24; z++) {
                    for (int y = 0; y <= 5; y++) {
                        cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                        original[index++] = level.getBlockState(cursor);
                    }
                }
            }
            BlockPos outside = center.offset(25, 1, 0);
            BlockState outsideState = level.getBlockState(outside);
            BlockState belowState = level.getBlockState(center.below());
            BlockState aboveState = level.getBlockState(center.above(6));
            try {
                level.setBlockAndUpdate(center.above(5), Blocks.DIRT.defaultBlockState());
                level.setBlockAndUpdate(outside, Blocks.GOLD_BLOCK.defaultBlockState());
                command(context, source(context).withEntity(player).withPosition(player.position()), "genshin arena");
                context.assertTrue(data.isManaged(), "Arena command enables managed mode");
                assertDisabledRules(context);
                context.assertValueEqual(player.gameMode(), GameType.ADVENTURE, "Arena changes the executing player to adventure");
                context.assertValueEqual(player.position(), new Vec3(center.getX(), center.getY() + 1, center.getZ()), "Teleport to exact arena centre");
                for (int x = -25; x <= 24; x++) {
                    for (int z = -25; z <= 24; z++) {
                        for (int y = 0; y <= 5; y++) {
                            cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                            boolean edge = x == -25 || x == 24 || z == -25 || z == 24;
                            Block expected = y == 0 ? Blocks.SMOOTH_STONE : y == 1 && edge ? Blocks.POLISHED_ANDESITE : Blocks.AIR;
                            context.assertTrue(level.getBlockState(cursor).is(expected), "Arena floor/border/air mismatch at " + cursor);
                        }
                    }
                }
                context.assertTrue(level.getBlockState(outside).is(Blocks.GOLD_BLOCK), "Arena must not write outside its horizontal bound");
                context.assertValueEqual(level.getBlockState(center.below()), belowState, "Arena must not write below its floor");
                context.assertValueEqual(level.getBlockState(center.above(6)), aboveState, "Arena must not write above its clear height");
            } finally {
                index = 0;
                for (int x = -25; x <= 24; x++) {
                    for (int z = -25; z <= 24; z++) {
                        for (int y = 0; y <= 5; y++) {
                            cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                            level.setBlock(cursor, original[index++], Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                        }
                    }
                }
                level.setBlock(outside, outsideState, Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            }
        });
        context.succeed();
    }

    private static InteractionResult place(ServerPlayer player, BlockPos floor) {
        ItemStack stack = new ItemStack(Items.STONE, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return ((BlockItem) Items.STONE).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
    }

    private static ServerPlayer player(GameTestHelper context) {
        var server = context.getLevel().getServer();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "managed-world-test");
        ServerPlayer player = new ServerPlayer(server, context.getLevel(), profile, ClientInformation.createDefault());
        // A real server game mode/packet listener, but no live socket and no player-list/world registration.
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player,
                CommonListenerCookie.createInitial(profile, false));
        player.setGameMode(GameType.SURVIVAL);
        player.snapTo(context.absoluteVec(new Vec3(0.5, 2, 0.5)));
        return player;
    }

    private static CommandSourceStack source(GameTestHelper context) {
        return context.getLevel().getServer().createCommandSourceStack().withLevel(context.getLevel())
                .withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput();
    }

    private static int command(GameTestHelper context, CommandSourceStack source, String command) {
        try {
            return context.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException exception) {
            throw new AssertionError("Command failed: " + command, exception);
        }
    }

    private static void expectDenied(GameTestHelper context, CommandSourceStack source, String command) {
        try {
            context.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException expected) {
            return;
        }
        throw new AssertionError("Unprivileged source executed " + command);
    }

    private static void assertDisabledRules(GameTestHelper context) {
        GameRules rules = context.getLevel().getGameRules();
        context.assertFalse(rules.get(GameRules.NATURAL_HEALTH_REGENERATION), "Managed regeneration rule");
        context.assertFalse(rules.get(GameRules.SPAWN_MOBS), "Managed spawning rule");
        context.assertFalse(rules.get(GameRules.MOB_GRIEFING), "Managed griefing rule");
        context.assertValueEqual(rules.get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER), 0, "Managed fire radius");
    }

    private static void withWorld(GameTestHelper context, Consumer<ManagedWorldData> test) {
        var server = context.getLevel().getServer();
        var storage = server.overworld().getDataStorage();
        ManagedWorldData originalData = ManagedWorldData.get(server);
        GameRules originalRules = server.getGameRules().copy(server.overworld().enabledFeatures());
        ManagedWorldData data = new ManagedWorldData();
        storage.set(ManagedWorldData.TYPE, data);
        try {
            test.accept(data);
        } finally {
            server.getGameRules().setAll(originalRules, server);
            storage.set(ManagedWorldData.TYPE, originalData);
        }
    }
}
