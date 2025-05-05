package com.redlimerl.speedrunigt.mixins.server;

import com.redlimerl.speedrunigt.timer.InGameTimer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow
    public abstract PlayerManager getPlayerManager();

    @Inject(method = "tick", at = @At("HEAD"))
    private void countType1Ticks(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (this.getPlayerManager().getPlayerList().isEmpty()) {
            InGameTimer.getInstance().tickType1();
        }
    }

    @Environment(EnvType.CLIENT)
    @Inject(method = "tick", at = @At("HEAD"))
    private void countClientServerTicks(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (!this.getPlayerManager().getPlayerList().isEmpty() && MinecraftClient.getInstance().player == null) {
            InGameTimer.getInstance().tickType3();
        }
        // maybe not precise enough, it should make sure there was a pause first so it's not just ticks from a lagging server bleeding over
        if (MinecraftClient.getInstance().currentScreen instanceof StatsScreen) {
            InGameTimer.getInstance().tickServerStats();
        }
    }
}
