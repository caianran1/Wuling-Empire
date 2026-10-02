package com.wuling.empire.network;

import com.wuling.empire.capability.ModCapabilities;
import com.wuling.empire.entity.ModEntities;
import com.wuling.empire.entity.WuLingZombieEntity;
import com.wuling.empire.wuling.WuLingType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * C → S：主人按 Shift + P，让召唤出的僵尸<b>全部消散</b>。
 *
 * <p>为什么需要它：腐肉武灵一次召唤就是一整队（绿宝石档 35 只），
 * 而僵尸是永久存在的（不因距离被回收、也不会自己消失）。
 * 没有解散手段的话，玩家会攒出一大群僵尸，卡服且没法收拾。
 *
 * <p>只删「自己名下」的召唤僵尸 —— 认的是实体里记的主人 UUID，
 * 别人的队伍动不了。
 */
public class WuLingDismissPacket {

    public WuLingDismissPacket() {
    }

    public static void encode(WuLingDismissPacket msg, FriendlyByteBuf buf) {
        // 无载荷
    }

    public static WuLingDismissPacket decode(FriendlyByteBuf buf) {
        return new WuLingDismissPacket();
    }

    public static void handle(WuLingDismissPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            if (sender != null) {
                dismiss(sender);
            }
        });
        context.setPacketHandled(true);
    }

    private static void dismiss(ServerPlayer player) {
        boolean summoner = player.getCapability(ModCapabilities.WU_LING)
                .map(holder -> holder.data().type() == WuLingType.ROTTEN_FLESH)
                .orElse(false);
        if (!summoner) {
            player.sendSystemMessage(
                    Component.translatable("message.wulingdiguo.dismiss_need_rotten_flesh"));
            return;
        }

        ServerLevel level = player.serverLevel();
        List<? extends WuLingZombieEntity> zombies =
                level.getEntities(ModEntities.WU_LING_ZOMBIE.get(), zombie -> zombie.isOwner(player));
        for (WuLingZombieEntity zombie : zombies) {
            zombie.discard();
        }

        if (zombies.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.wulingdiguo.dismiss_none"));
        } else {
            player.sendSystemMessage(
                    Component.translatable("message.wulingdiguo.dismiss_ok", zombies.size()));
        }
    }
}
