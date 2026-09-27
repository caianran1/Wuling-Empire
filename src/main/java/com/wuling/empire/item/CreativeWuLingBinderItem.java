package com.wuling.empire.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 创造武灵绑定器。
 *
 * 用途：创造模式下直接切换武灵，无视「已经拥有武灵」的限制。
 * 右键打开选择界面，选中的武灵会直接覆盖当前武灵——
 * 不消耗灵珠、不需要普通绑定器；因为没有灵珠，所以没有品质带来的修炼速度加成（×1），
 * 境界上限则与所有人一致（{@link com.wuling.empire.Config#maxRealm()}）。
 */
public class CreativeWuLingBinderItem extends Item {

    public CreativeWuLingBinderItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide()) {
            DistExecutor.runWhenOn(Dist.CLIENT,
                    () -> com.wuling.empire.client.WuLingClientBridge::openChooseCreative);
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.wulingdiguo.binder_creative_1"));
        tooltip.add(Component.translatable("tooltip.wulingdiguo.binder_creative_2"));
    }
}
