package com.wuling.empire.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 武灵绑定器。
 *
 * 原文：开启武灵需要「武灵绑定器」（仅限创造模式），消耗灵珠，
 *       灵珠种类决定修炼方向，品质决定境界上限。
 * <b>2026-09-27 用户修订：品质不再决定境界上限，只决定修炼速度；
 * 上限所有人一致。</b>
 *
 * 交互分工：
 *   右键             → 打开武灵升级 / 修炼面板
 *   Shift + 右键     → 选择武灵种类并绑定（即「选武灵」）
 *   凝聚（Shift + M） → 按已绑定武灵的种类 + 当前境界，在面前生成对应实物（如钻石剑）
 *
 * 注意：凝聚<b>不需要携带绑定器</b>（0.2.14 起），改为消耗灵力；
 * 绑定器只负责「开武灵 / 选种类 / 开面板」这三件事。
 */
public class WuLingBinderItem extends Item {

    public WuLingBinderItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // 打开选择界面，由玩家自己挑修炼方向（与 Shift + M 等价）
            if (level.isClientSide()) {
                DistExecutor.runWhenOn(Dist.CLIENT,
                        () -> com.wuling.empire.client.WuLingClientBridge::openChoose);
            }
            return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
        }

        if (level.isClientSide()) {
            DistExecutor.runWhenOn(Dist.CLIENT,
                    () -> com.wuling.empire.client.WuLingClientBridge::openPanel);
            return InteractionResultHolder.sidedSuccess(held, true);
        }
        return InteractionResultHolder.sidedSuccess(held, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.wulingdiguo.binder_1"));
        tooltip.add(Component.translatable("tooltip.wulingdiguo.binder_2"));
        tooltip.add(Component.translatable("tooltip.wulingdiguo.binder_3"));
    }
}
