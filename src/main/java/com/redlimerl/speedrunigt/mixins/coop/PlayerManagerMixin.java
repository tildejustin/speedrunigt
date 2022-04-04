package com.redlimerl.speedrunigt.mixins.coop;

import com.redlimerl.speedrunigt.timer.InGameTimer;
import com.redlimerl.speedrunigt.timer.TimerPacketHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Shadow public abstract List<ServerPlayerEntity> getPlayerList();

    @Shadow public abstract int getCurrentPlayerCount();

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    public void onPlayerConnectInject(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        if (InGameTimer.getInstance().isStarted() && !InGameTimer.getInstance().isCompleted() && this.getCurrentPlayerCount() > 1) {
            TimerPacketHandler.serverSend(getPlayerList(), InGameTimer.getInstance(), InGameTimer.getCompletedInstance());
        }
    }
}
