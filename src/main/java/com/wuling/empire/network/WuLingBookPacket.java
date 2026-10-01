package com.wuling.empire.network;

import com.wuling.empire.wuling.WuLingBinding;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C → S：凝聚「自选附魔书」。
 *
 * <p>只有书武灵走这条路：在附魔书凝聚界面里挑好附魔与等级后点「凝聚」，
 * 把选择发给服务端。服务端才是权威 —— 是否已开启武灵、是不是书武灵、
 * 灵力够不够、等级是否超过附魔上限，全部在那边再判一次。
 */
public class WuLingBookPacket {

    private final String enchantId;
    private final int level;

    public WuLingBookPacket(String enchantId, int level) {
        this.enchantId = enchantId == null ? "" : enchantId;
        this.level = level;
    }

    public static void encode(WuLingBookPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.enchantId);
        buf.writeVarInt(msg.level);
    }

    public static WuLingBookPacket decode(FriendlyByteBuf buf) {
        return new WuLingBookPacket(buf.readUtf(), buf.readVarInt());
    }

    public static void handle(WuLingBookPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            if (sender != null) {
                WuLingBinding.condenseBook(sender, msg.enchantId, msg.level);
            }
        });
        context.setPacketHandled(true);
    }
}
