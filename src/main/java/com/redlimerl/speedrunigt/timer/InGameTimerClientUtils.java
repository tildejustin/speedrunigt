package com.redlimerl.speedrunigt.timer;

import com.google.common.collect.Lists;
import com.redlimerl.speedrunigt.gui.screen.FailedCategoryInitScreen;
import com.redlimerl.speedrunigt.mixins.access.MinecraftClientAccessorForAttack;
import com.redlimerl.speedrunigt.mixins.access.WorldRendererAccessor;
import com.redlimerl.speedrunigt.timer.category.InvalidCategoryException;
import com.redlimerl.speedrunigt.timer.category.RunCategories;
import com.redlimerl.speedrunigt.timer.category.condition.CategoryCondition;
import com.redlimerl.speedrunigt.timer.category.condition.ObtainItemCategoryCondition;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.stat.Stats;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class InGameTimerClientUtils {

    public static boolean canUnpauseTimer(boolean checkRender) {
        MinecraftClient client = MinecraftClient.getInstance();
        InGameTimer timer = InGameTimer.getInstance();

        if (timer.getStatus() != TimerStatus.IDLE) return false;

        if (!client.isPaused() && client.worldRenderer != null && client.isWindowFocused() && client.mouse.isCursorLocked()) {
            if (checkRender) {
                WorldRendererAccessor worldRenderer = (WorldRendererAccessor) client.worldRenderer;
                int chunks = worldRenderer.invokeCompletedChunkCount();
                int entities = worldRenderer.getRegularEntityCount() - (client.options.perspective > 0 ? 0 : 1);

                return chunks + entities > 0;
            }
            return true;
        }
        return false;
    }

    public static float getGeneratedChunkRatio() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            int chunks = client.options.viewDistance * 2 + 1;
            return (float) client.world.getChunkManager().getLoadedChunkCount() / (chunks*chunks);
        }
        return 0;
    }

    public static boolean isHardcoreWorld() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && client.player.world.getLevelProperties().isHardcore();
    }

    public static Long getPlayerTime() {
        Integer ticks = getPlayerTicks();
        return ticks != null ? ticks * 50L : null;
    }

    public static Integer getPlayerTicks() {
        MinecraftServer server = getClientServer();
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (server != null && player != null) {
            ServerStatHandler statHandler = server.getPlayerManager().createStatHandler(player);
            return statHandler == null ? null : statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_ONE_MINUTE));
        }
        return null;
    }

    public static @Nullable FailedCategoryInitScreen FAILED_CATEGORY_INIT_SCREEN = null;
    static void setCategoryWarningScreen(@Nullable String conditionFileName, InvalidCategoryException exception) {
        if (MinecraftClient.getInstance().currentScreen == null)
            FAILED_CATEGORY_INIT_SCREEN = new FailedCategoryInitScreen(conditionFileName, exception);
        else MinecraftClient.getInstance().openScreen(new FailedCategoryInitScreen(conditionFileName, exception));
    }

    static MinecraftServer getClientServer() {
        return MinecraftClient.getInstance().getServer();
    }

    public static boolean isFocusedClick() {
        return MinecraftClient.getInstance().player != null && !MinecraftClient.getInstance().player.isUsingItem()
                && ((MinecraftClientAccessorForAttack) MinecraftClient.getInstance()).getAttackCoolDown() <= 0;
    }

    public static void checkItemCriteria(PlayerEntity player, boolean rebase) {
        InGameTimer timer = InGameTimer.getInstance();

        List<ItemStack> playerItemList = Lists.newArrayList();
        playerItemList.addAll(player.inventory.armor);
        playerItemList.addAll(player.inventory.offHand);
        playerItemList.addAll(player.inventory.main);

        // Custom Json category
        if (timer.getCategory().getConditionJson() != null) {
            for (CategoryCondition.Conditions conditions : timer.getCustomCondition().map(CategoryCondition::getConditions).orElse(Lists.newArrayList())) {
                int strict = 0;
                for (CategoryCondition.Condition<?> condition : conditions.getConditions()) {
                    if (condition instanceof ObtainItemCategoryCondition) {
                        ObtainItemCategoryCondition obtainItemCondition = (ObtainItemCategoryCondition) condition;
                        boolean canComplete = obtainItemCondition.checkConditionComplete(playerItemList);
                        if (obtainItemCondition.isStrictMode()) {
                            if (!canComplete) strict++;
                        } else {
                            timer.updateCondition(obtainItemCondition, playerItemList);
                        }
                    }
                }

                if (strict == 0) {
                    for (CategoryCondition.Condition<?> condition : conditions.getConditions()) {
                        if (condition instanceof ObtainItemCategoryCondition) {
                            ObtainItemCategoryCondition obtainItemCondition = (ObtainItemCategoryCondition) condition;
                            timer.updateCondition(obtainItemCondition, playerItemList);
                        }
                    }
                }

            }
            timer.checkConditions(rebase);
        }

        //HIGH%
        if (timer.getCategory() == RunCategories.HIGH && player.getY() >= 420) {
            InGameTimer.complete();
            return;
        }

        //Full Inventory
        if (timer.getCategory() == RunCategories.FULL_INV) {
            if (player.inventory.main.stream().filter(itemStack -> itemStack != null && itemStack != ItemStack.EMPTY && itemStack.getItem() != Items.AIR).map(ItemStack::getItem).distinct().toArray().length == 36)
                InGameTimer.complete(rebase);
            return;
        }

        for (ItemStack itemStack : playerItemList) {
            int shells = 0;

            if (itemStack == null) continue;

            // Timelines
            if (itemStack.getItem() == Items.TRIDENT) {
                timer.tryInsertNewTimeline("got_trident");
            }
            if (itemStack.getItem() == Items.NAUTILUS_SHELL) {
                shells += itemStack.getCount();
            }
            if (itemStack.getItem() instanceof BlockItem && ((BlockItem) itemStack.getItem()).getBlock() instanceof ShulkerBoxBlock) {
                shells += InGameTimerUtils.getItemCountFromShulkerBox(itemStack, Items.NAUTILUS_SHELL);
            }
            if (shells > timer.getMoreData(1541)) {
                int i = 1;
                while (shells >= timer.getMoreData(1541) + i) {
                    timer.tryInsertNewTimeline("got_shell_" + (timer.getMoreData(1541) + i++));
                }
                timer.updateMoreData(1541, shells);
            }



            //Stack of Lime Wool
            if (timer.getCategory() == RunCategories.STACK_OF_LIME_WOOL) {
                if (itemStack.getItem() == Items.LIME_WOOL && itemStack.getCount() == 64) InGameTimer.complete(rebase);
            }
        }

        List<Item> items = Stream.concat(player.inventory.main.stream(), player.inventory.offHand.stream()).map(ItemStack::getItem).collect(Collectors.toList());
        List<Item> armors = player.inventory.armor.stream().map(ItemStack::getItem).collect(Collectors.toList());

        if (timer.getCategory() == RunCategories.OBTAIN_OBSIDIAN && items.contains(Items.OBSIDIAN)) {
            InGameTimer.complete(rebase);
        }

        //All Workstations
        if (timer.getCategory() == RunCategories.ALL_WORKSTATIONS) {
            if (items.contains(Items.BLAST_FURNACE) &&
                    items.contains(Items.SMOKER) &&
                    items.contains(Items.CARTOGRAPHY_TABLE) &&
                    items.contains(Items.BREWING_STAND) &&
                    items.contains(Items.COMPOSTER) &&
                    items.contains(Items.BARREL) &&
                    items.contains(Items.FLETCHING_TABLE) &&
                    items.contains(Items.CAULDRON) &&
                    items.contains(Items.LECTERN) &&
                    items.contains(Items.STONECUTTER) &&
                    items.contains(Items.LOOM) &&
                    items.contains(Items.SMITHING_TABLE) &&
                    items.contains(Items.GRINDSTONE)) {
                InGameTimer.complete(rebase);
            }
        }

        //All Swords
        if (timer.getCategory() == RunCategories.ALL_SWORDS) {
            if (items.contains(Items.STONE_SWORD) &&
                    items.contains(Items.DIAMOND_SWORD) &&
                    items.contains(Items.GOLDEN_SWORD) &&
                    items.contains(Items.IRON_SWORD) &&
                    items.contains(Items.NETHERITE_SWORD) &&
                    items.contains(Items.WOODEN_SWORD)) {
                InGameTimer.complete(rebase);
            }
        }

        //All Minerals
        if (timer.getCategory() == RunCategories.ALL_MINERALS) {
            if (items.contains(Items.COAL) &&
                    items.contains(Items.IRON_INGOT) &&
                    items.contains(Items.GOLD_INGOT) &&
                    items.contains(Items.DIAMOND) &&
                    items.contains(Items.REDSTONE) &&
                    items.contains(Items.LAPIS_LAZULI) &&
                    items.contains(Items.EMERALD) &&
                    items.contains(Items.QUARTZ) &&
                    items.contains(Items.NETHERITE_INGOT)) {
                InGameTimer.complete(rebase);
            }
        }

        //Iron Armors & lvl 15
        if (timer.getCategory() == RunCategories.FULL_IA_15_LVL) {
            if (armors.contains(Items.IRON_HELMET) &&
                    armors.contains(Items.IRON_CHESTPLATE) &&
                    armors.contains(Items.IRON_BOOTS) &&
                    armors.contains(Items.IRON_LEGGINGS) && player.experienceLevel >= 15) {
                InGameTimer.complete(rebase);
            }
        }

        if (timer.getCategory() == RunCategories.OBTAIN_DIAMOND && items.contains(Items.DIAMOND)) InGameTimer.complete(rebase);
        if (timer.getCategory() == RunCategories.OBTAIN_EMERALD && items.contains(Items.EMERALD)) InGameTimer.complete(rebase);
        if (timer.getCategory() == RunCategories.OBTAIN_CAKE && items.contains(Items.CAKE)) InGameTimer.complete(rebase);
        if (timer.getCategory() == RunCategories.OBTAIN_GOLDEN_APPLE && (items.contains(Items.GOLDEN_APPLE) || items.contains(Items.ENCHANTED_GOLDEN_APPLE)))
            InGameTimer.complete(rebase);
        if (timer.getCategory() == RunCategories.OBTAIN_NETHERITE && items.contains(Items.NETHERITE_INGOT)) InGameTimer.complete(rebase);


        if (timer.getCategory() == RunCategories.ALL_LOGS &&
                (items.contains(Items.OAK_LOG) || items.contains(Items.STRIPPED_OAK_LOG) || items.contains(Items.OAK_WOOD) || items.contains(Items.STRIPPED_OAK_WOOD)) &&
                (items.contains(Items.SPRUCE_LOG) || items.contains(Items.STRIPPED_SPRUCE_LOG) || items.contains(Items.SPRUCE_WOOD) || items.contains(Items.STRIPPED_SPRUCE_WOOD)) &&
                (items.contains(Items.BIRCH_LOG) || items.contains(Items.STRIPPED_BIRCH_LOG) || items.contains(Items.BIRCH_WOOD) || items.contains(Items.STRIPPED_BIRCH_WOOD)) &&
                (items.contains(Items.JUNGLE_LOG) || items.contains(Items.STRIPPED_JUNGLE_LOG) || items.contains(Items.JUNGLE_WOOD) || items.contains(Items.STRIPPED_JUNGLE_WOOD)) &&
                (items.contains(Items.ACACIA_LOG) || items.contains(Items.STRIPPED_ACACIA_LOG) || items.contains(Items.ACACIA_WOOD) || items.contains(Items.STRIPPED_ACACIA_WOOD)) &&
                (items.contains(Items.DARK_OAK_LOG) || items.contains(Items.STRIPPED_DARK_OAK_LOG) || items.contains(Items.DARK_OAK_WOOD) || items.contains(Items.STRIPPED_DARK_OAK_WOOD)) &&
                (items.contains(Items.CRIMSON_STEM) || items.contains(Items.STRIPPED_CRIMSON_STEM) || items.contains(Items.CRIMSON_HYPHAE) || items.contains(Items.STRIPPED_CRIMSON_HYPHAE)) &&
                (items.contains(Items.WARPED_STEM) || items.contains(Items.STRIPPED_WARPED_STEM) || items.contains(Items.WARPED_HYPHAE) || items.contains(Items.STRIPPED_WARPED_HYPHAE))) {
            InGameTimer.complete(rebase);
        }

        if (timer.getCategory() == RunCategories.ALL_WOOL && items.contains(Items.WHITE_WOOL) &&
                items.contains(Items.ORANGE_WOOL) && items.contains(Items.MAGENTA_WOOL) && items.contains(Items.LIGHT_BLUE_WOOL) &&
                items.contains(Items.YELLOW_WOOL) && items.contains(Items.LIME_WOOL) && items.contains(Items.PINK_WOOL) &&
                items.contains(Items.GRAY_WOOL) && items.contains(Items.LIGHT_GRAY_WOOL) && items.contains(Items.CYAN_WOOL) &&
                items.contains(Items.PURPLE_WOOL) && items.contains(Items.BLUE_WOOL) && items.contains(Items.BROWN_WOOL) &&
                items.contains(Items.GREEN_WOOL) && items.contains(Items.RED_WOOL) && items.contains(Items.BLACK_WOOL)) {
            InGameTimer.complete(rebase);
        }
    }
}
