package io.github.brainage04.genshininminecraft;

import io.github.brainage04.genshininminecraft.gametest.GenshinInMinecraftGameTests;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/// Each function has a matching `data/genshininminecraft/test_instance/<name>.json`.
@EventBusSubscriber(modid = GenshinInMinecraft.MOD_ID)
public final class GenshinInMinecraftNeoForgeGameTest {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
            "genshin_command_is_registered", GenshinInMinecraftGameTests::genshinCommandIsRegistered
    );

    private GenshinInMinecraftNeoForgeGameTest() {
    }

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        TESTS.forEach((name, test) -> event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(GenshinInMinecraft.MOD_ID, name),
                () -> test
        ));
    }
}
