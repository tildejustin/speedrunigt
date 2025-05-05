package com.redlimerl.speedrunigt.mixins;

import com.redlimerl.speedrunigt.timer.InGameTimer;
import com.redlimerl.speedrunigt.timer.category.RunCategories;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {
    @Shadow
    private UUID owner;

    @Shadow
    public abstract ItemStack getStack();

    private static int lastTicked = -1;

    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "onPlayerCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ItemEntity;getStack()Lnet/minecraft/item/ItemStack;"))
    private void countSignificantPreClientTicks(PlayerEntity player, CallbackInfo ci) {
        // does not account for if the item can actually fit in the player's inventory but that is very hard to check w/o modifying state
        if (MinecraftClient.getInstance().player == null && (this.owner == null || this.owner.equals(player.getUuid()))) {
            if (InGameTimer.getInstance().getCategory() == RunCategories.OBTAIN_DIAMOND && getStack().getItem() != Items.DIAMOND) return;

            int ticks = this.getServer().getTicks();
            if (lastTicked != ticks) {
                InGameTimer.getInstance().tickAdded();
                lastTicked = ticks;
            }
            InGameTimer.getInstance().setPause(false, "significant pre-client tick");
        }
    }
}
