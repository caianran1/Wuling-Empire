package com.wuling.empire.network;

import com.wuling.empire.client.ClientWuLingData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * S → C：同步玩家武灵数据到客户端，供升级面板与 HUD 使用。
 */
public class WuLingSyncPacket {

    private final boolean bound;
    private final String typeKey;
    private final String sourceKey;
    private final int realmOrdinal;
    private final int stageOrdinal;
    private final double progress;
    private final double threshold;
    /** 修炼速度加成倍率（由开启武灵所用灵珠的品质决定；上限所有人一致，不再同步） */
    private final double cultivationBonus;
    /** 已缴纳的突破物资（键 → 数量），用于面板显示「已缴 N」 */
    private final Map<String, Integer> submitted;

    public WuLingSyncPacket(boolean bound, String typeKey, String sourceKey,
                            int realmOrdinal, int stageOrdinal, double progress,
                            double threshold, double cultivationBonus, Map<String, Integer> submitted) {
        this.bound = bound;
        this.typeKey = typeKey;
        this.sourceKey = sourceKey;
        this.realmOrdinal = realmOrdinal;
        this.stageOrdinal = stageOrdinal;
        this.progress = progress;
        this.threshold = threshold;
        this.cultivationBonus = cultivationBonus;
        this.submitted = submitted == null ? Map.of() : submitted;
    }

    public static void encode(WuLingSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.bound);
        buf.writeUtf(msg.typeKey == null ? "" : msg.typeKey);
        buf.writeUtf(msg.sourceKey == null ? "" : msg.sourceKey);
        buf.writeInt(msg.realmOrdinal);
        buf.writeInt(msg.stageOrdinal);
        buf.writeDouble(msg.progress);
        buf.writeDouble(msg.threshold);
        buf.writeDouble(msg.cultivationBonus);
        buf.writeVarInt(msg.submitted.size());
        for (Map.Entry<String, Integer> e : msg.submitted.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeVarInt(e.getValue());
        }
    }

    public static WuLingSyncPacket decode(FriendlyByteBuf buf) {
        boolean bound = buf.readBoolean();
        String typeKey = buf.readUtf();
        String sourceKey = buf.readUtf();
        int realmOrdinal = buf.readInt();
        int stageOrdinal = buf.readInt();
        double progress = buf.readDouble();
        double threshold = buf.readDouble();
        double cultivationBonus = buf.readDouble();
        int size = buf.readVarInt();
        Map<String, Integer> submitted = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            int amount = buf.readVarInt();
            submitted.put(key, amount);
        }
        return new WuLingSyncPacket(bound, typeKey, sourceKey, realmOrdinal,
                stageOrdinal, progress, threshold, cultivationBonus, submitted);
    }

    public static void handle(WuLingSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> ClientWuLingData.set(
                msg.bound, msg.typeKey, msg.sourceKey, msg.realmOrdinal,
                msg.stageOrdinal, msg.progress, msg.threshold, msg.cultivationBonus, msg.submitted));
        context.setPacketHandled(true);
    }
}
