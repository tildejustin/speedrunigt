package com.redlimerl.speedrunigt.mixins;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.redlimerl.speedrunigt.timer.InGameTimer;
import com.redlimerl.speedrunigt.timer.InGameTimerClientUtils;
import com.redlimerl.speedrunigt.timer.InGameTimerUtils;
import com.redlimerl.speedrunigt.timer.TimerStatus;
import com.redlimerl.speedrunigt.timer.category.RunCategories;
import com.redlimerl.speedrunigt.timer.category.condition.CategoryCondition;
import com.redlimerl.speedrunigt.timer.category.condition.ObtainItemCategoryCondition;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {

    @Shadow @Final protected MinecraftClient client;

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V", at = @At("TAIL"))
    private void onMove(MovementType movementType, Vec3d vec3d, CallbackInfo ci) {
        // TODO: run inventory checks at the end of the tick, after items have been picked up. also, abstract so they can be checked every inv render as well
        InGameTimer timer = InGameTimer.getInstance();

        if (timer.getStatus() == TimerStatus.NONE || timer.getStatus() == TimerStatus.COMPLETED_LEGACY) return;

        if (timer.getStatus() == TimerStatus.IDLE && (vec3d.x != 0 || vec3d.z != 0 || this.jumping || this.isSneaking())) {
            timer.setPause(false, "moved player");
            timer.updateFirstInput();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void updateNausea(CallbackInfo ci) {
        // client inventory is updated when packets are handled, immediately before a client tick, however it should still be counted as part of that tick
        InGameTimerClientUtils.checkItemCriteria(this, false);

        InGameTimer timer = InGameTimer.getInstance();
        // Portal time update
        if (this.inNetherPortal) {
            if (timer.getLastPortalTime() == -1) {
                timer.setLastPortalTime(timer.getTicks());
            }
        }
        // if the player leaves client side, the client side timer will only get reset if the player leaves server side
        // the server side timer will only reset if the tick counter hits 0,
        // so this handles properly lag if the player leaves the portal but doesn't fully reset countdown
        else if (timer.getLastPortalTimeServer() < 0) {
            timer.setLastPortalTime(-1);
        }
    }

    @Override
    public void changeLookDirection(double cursorDeltaX, double cursorDeltaY) {
        super.changeLookDirection(cursorDeltaX, cursorDeltaY);

        if (cursorDeltaX != 0 || cursorDeltaY != 0) {
            InGameTimer timer = InGameTimer.getInstance();
            if (timer.getStatus() == TimerStatus.IDLE) {
                timer.setPause(false, "changed look direction");
            }
        }
    }
}
