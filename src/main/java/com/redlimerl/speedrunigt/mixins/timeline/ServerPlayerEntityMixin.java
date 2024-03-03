package com.redlimerl.speedrunigt.mixins.timeline;

import com.redlimerl.speedrunigt.SpeedRunIGT;
import com.redlimerl.speedrunigt.instance.GameInstance;
import com.redlimerl.speedrunigt.timer.InGameTimer;
import com.redlimerl.speedrunigt.timer.InGameTimerUtils;
import com.redlimerl.speedrunigt.timer.TimerStatus;
import com.redlimerl.speedrunigt.timer.category.RunCategories;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.OverworldDimension;
import net.minecraft.world.dimension.TheNetherDimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(PlayerManager.class)
public abstract class ServerPlayerEntityMixin {

    private ServerWorld beforeWorld = null;
    private Vec3d lastPortalPos = null;

    @Inject(method = "teleportToDimension", at = @At("HEAD"))
    public void onChangeDimension(ServerPlayerEntity player, int dimension, CallbackInfo ci) {
        InGameTimer timer = InGameTimer.getInstance();

        beforeWorld = player.getServerWorld();
        lastPortalPos = player.getPos();
        InGameTimerUtils.IS_CAN_WAIT_WORLD_LOAD = !InGameTimer.getInstance().isCoop() && InGameTimer.getInstance().getCategory() == RunCategories.ANY;

        //All Portals
        SpeedRunIGT.debug("Current portals : " + timer.getEndPortalPosList().size());
        if (InGameTimerUtils.IS_KILLED_ENDER_DRAGON && timer.getCategory() == RunCategories.ALL_PORTALS && timer.getEndPortalPosList().size() == 3) {
            InGameTimer.complete();
        }
    }

    @Inject(method = "teleportToDimension", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;method_1986(Lnet/minecraft/entity/player/ServerPlayerEntity;Lnet/minecraft/server/world/ServerWorld;)V", shift = At.Shift.AFTER))
    public void onChangedDimension(ServerPlayerEntity player, int dimension, CallbackInfo ci) {
        Dimension oldDimension = beforeWorld.dimension;
        Dimension newDimension = player.world.dimension;

        InGameTimer timer = InGameTimer.getInstance();
        if (timer.getStatus() != TimerStatus.NONE) {
            if (oldDimension instanceof OverworldDimension && newDimension instanceof TheNetherDimension) {
                if (!timer.isCoop() && InGameTimer.getInstance().getCategory() == RunCategories.ANY) InGameTimerUtils.IS_CAN_WAIT_WORLD_LOAD = InGameTimerUtils.isLoadableBlind(newDimension, player.getPos().add(0, 0, 0), lastPortalPos.add(0, 0, 0));
            }

            if (oldDimension instanceof TheNetherDimension && newDimension instanceof OverworldDimension) {
                if (this.isEnoughTravel(player)) {
                    int portalIndex = InGameTimerUtils.isBlindTraveled(lastPortalPos);
                    GameInstance.getInstance().callEvents("nether_travel", factory -> factory.getDataValue("portal").equals(String.valueOf(portalIndex)));
                    InGameTimer.getInstance().tryInsertNewTimeline("nether_travel");
                    if (portalIndex == 0) {
                        InGameTimer.getInstance().tryInsertNewTimeline("nether_travel_home");
                    } else {
                        timer.tryInsertNewTimeline("nether_travel_blind");
                    }
                }
                if (!timer.isCoop() && InGameTimer.getInstance().getCategory() == RunCategories.ANY) InGameTimerUtils.IS_CAN_WAIT_WORLD_LOAD = InGameTimerUtils.isLoadableBlind(newDimension, lastPortalPos.add(0, 0, 0), player.getPos().add(0, 0, 0));
            }
        }
    }

    @Unique
    private boolean isEnoughTravel(ServerPlayerEntity serverPlayerEntity) {
        Set<Item> currentItemTypes = Arrays.stream(serverPlayerEntity.inventory.main) // Go over both main inventory item list
                .filter(Objects::nonNull) // Remove nulls
                .map(ItemStack::getItem) // Turn each item stack into its item
                .collect(Collectors.toSet()); // Collect to a set of items that the player has
        return currentItemTypes.contains(Items.EYE_OF_ENDER) || (currentItemTypes.contains(Items.ENDER_PEARL) && (currentItemTypes.contains(Items.BLAZE_ROD) || currentItemTypes.contains(Items.BLAZE_POWDER)));
    }
}
