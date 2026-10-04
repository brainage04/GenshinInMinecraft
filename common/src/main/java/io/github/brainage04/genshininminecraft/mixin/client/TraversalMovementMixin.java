package io.github.brainage04.genshininminecraft.mixin.client;

import io.github.brainage04.genshininminecraft.client.CombatInput;
import io.github.brainage04.genshininminecraft.client.ManagedCamera;
import io.github.brainage04.genshininminecraft.combat.TraversalGeometry;
import io.github.brainage04.genshininminecraft.rules.Traversal;
import net.minecraft.core.Direction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only the owning client moves; the server grants attachment/deployment and owns stamina/landing loss. */
@Mixin(Player.class)
public abstract class TraversalMovementMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void genshin$traversalTravel(Vec3 input, CallbackInfo callback) {
        if (!CombatInput.managed() || !((Object) this instanceof LocalPlayer player)
                || player.isPassenger() || player.getAbilities().flying || player.isInWater()) return;
        var state = CombatInput.state();
        if (!state.climbing() && !state.gliding()) return;
        Direction wall = state.climbing() ? TraversalGeometry.wall(state.wallOrdinal()) : null;
        float yaw = ManagedCamera.decoupled() ? ManagedCamera.yaw() : player.getYRot();
        Vec3 motion = TraversalGeometry.motion(state.climbing() ? Traversal.Mode.CLIMB : Traversal.Mode.GLIDE,
                wall, yaw, player.input.keyPresses, state.climbJumping(), state.climbJumpSide());
        player.setDeltaMovement(motion);
        player.resetFallDistance();
        player.move(MoverType.SELF, motion);
        callback.cancel();
    }
}
