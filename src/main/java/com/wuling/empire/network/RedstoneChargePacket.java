package com.wuling.empire.network;

import com.wuling.empire.wuling.RedstoneCharging;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：用一颗灵珠给红石装备充能。
 *
 * <p>界面里列的是「来源 + 品质」这种一组珠子（比如「僵尸灵珠 · 凡品 × 12」），
 * 所以这里只传这两个标识，具体消耗背包里哪一颗、加多少充能，
 * 全部由服务端 {@link RedstoneCharging} 决定 —— 客户端传的是意图，不是结果。
 */
public class RedstoneChargePacket {

    private final String source;
    private final String qualityKey;

    public RedstoneChargePacket(String source, String qualityKey) {
        this.source = source == null ? "" : source;
        this.qualityKey = qualityKey == null ? "" : qualityKey;
    }

    public static void encode(RedstoneChargePacket msg, FriendlyByteBuf buffer) {
        buffer.writeUtf(msg.source);
        buffer.writeUtf(msg.qualityKey);
    }

    public static RedstoneChargePacket decode(FriendlyByteBuf buffer) {
        return new RedstoneChargePacket(buffer.readUtf(), buffer.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer sender = ctx.getSender();
        ctx.enqueueWork(() -> {
            if (sender != null) {
                RedstoneCharging.chargeFromBead(sender, this.source, this.qualityKey);
            }
        });
        ctx.setPacketHandled(true);
    }
}
