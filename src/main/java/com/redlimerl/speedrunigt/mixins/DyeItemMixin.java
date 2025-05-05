package com.redlimerl.speedrunigt.mixins;

import com.redlimerl.speedrunigt.timer.InGameTimer;
import com.redlimerl.speedrunigt.timer.category.RunCategories;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.HashSet;

@Mixin(DyeItem.class)
public abstract class DyeItemMixin {
    @Shadow
    @Final
    private DyeColor color;

    @Inject(method = "useOnEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ActionResult;success(Z)Lnet/minecraft/util/ActionResult;"))
    private void stopTimerDyeOnClient(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!user.world.isClient) return;
        InGameTimer timer = InGameTimer.getInstance();
        if (timer.getCategory() == RunCategories.SONIC_TAILS_KNUCKLES && entity instanceof SheepEntity) {
            int key;
            switch (this.color) {
                case BLUE: key = 338; break;
                case ORANGE: key = 339; break;
                case RED: key = 340; break;
                default: return;
            }
            Thread.dumpStack();
            timer.updateMoreData(key, entity.getEntityId());
            HashSet<Integer> sheep = new HashSet<>(Arrays.asList(timer.getMoreData(338), timer.getMoreData(339), timer.getMoreData(340)));
            sheep.remove(0);
            if (sheep.size() == 3) {
                InGameTimer.complete();
            }
        }
    }
}
