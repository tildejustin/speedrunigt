package com.redlimerl.speedrunigt.mixins.screen;

import com.redlimerl.speedrunigt.timer.InGameTimerClientUtils;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow
    @Final
    protected PlayerInventory playerInventory;

    @Inject(method = "render", at = @At(value = "HEAD"))
    private void checkObtainItemCriteria(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // inputs are handled during fps limit or in swapBuffers in render,
        // but a frame retime would end once it's visibly in inventory, which is also not tick-based
        InGameTimerClientUtils.checkItemCriteria(this.playerInventory.player, true);
    }
}
