package com.redlimerl.speedrunigt.mixins.translate;

import com.redlimerl.speedrunigt.option.SpeedRunOption;
import com.redlimerl.speedrunigt.option.SpeedRunOptions;
import com.redlimerl.speedrunigt.utils.TranslateHelper;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Mixin(TranslationStorage.class)
public abstract class TranslationStorageMixin {

    @Shadow protected abstract void load(InputStream inputStream);

    @Inject(method = "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;)V", at = @At("RETURN"))
    private void onLoad(ResourceManager container, List<String> list, CallbackInfo ci) {
        for (String lang : list) {
            InputStream inputStream = TranslateHelper.setup(lang, SpeedRunOption.getOption(SpeedRunOptions.ALWAYS_ENGLISH_TRANSLATIONS));
            if (inputStream == null) continue;
            this.load(inputStream);
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Inject(method = "load(Ljava/util/List;Ljava/util/Map;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/Resource;getInputStream()Ljava/io/InputStream;"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private static void cancelExternalLoadingOfTranslations(List<Resource> resources, Map<String, String> translationMap, CallbackInfo ci, Iterator<Resource> resourceIterator, Resource resource) {
        if (SpeedRunOption.getOption(SpeedRunOptions.ALWAYS_ENGLISH_TRANSLATIONS) && resource.getId().getNamespace().equals("speedrunigt") && !resource.getId().getPath().equalsIgnoreCase("lang/en_us.json")) {
            ci.cancel();
        }
    }
}
