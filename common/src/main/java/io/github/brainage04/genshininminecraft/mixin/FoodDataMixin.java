package io.github.brainage04.genshininminecraft.mixin;

import io.github.brainage04.genshininminecraft.world.ManagedWorld;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;
    @Shadow private float exhaustionLevel;
    @Shadow private int tickTimer;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void genshin$freezeHunger(ServerPlayer player, CallbackInfo callback) {
        if (ManagedWorld.isManaged(player.level())) {
            foodLevel = 20;
            saturationLevel = 20.0F;
            exhaustionLevel = 0.0F;
            tickTimer = 0;
            callback.cancel(); // No hunger-driven healing or starvation, even if another mod changes the rule.
        }
    }
}
