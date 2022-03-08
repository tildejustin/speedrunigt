package com.redlimerl.speedrunigt.mixins.retime;

import com.redlimerl.speedrunigt.timer.InGameTimerUtils;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionButtonWidget;
import net.minecraft.client.gui.widget.OptionSliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ButtonWidget.class)
public class OptionButtonWidgetMixin {

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "mouseClicked", at = @At("RETURN"))
    public void onClickOption(double d, double e, int i, CallbackInfoReturnable<Boolean> cir) {
        if (((Object) this) instanceof OptionButtonWidget || ((Object) this) instanceof OptionSliderWidget) {
            InGameTimerUtils.RETIME_IS_CHANGED_OPTION = true;
        }
    }
}
