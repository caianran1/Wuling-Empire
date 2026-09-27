package com.wuling.empire.network;

import com.wuling.empire.entity.CorpseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：同步尸体的外观数据（死亡怪物的 NBT + 朝向）。
 *
 * 只在玩家开始追踪该尸体时发送一次，客户端据此还原出用于渲染的实体。
 */
public class CorpseDataPacket {

    private final int entityId;
    private final CompoundTag mobData;
    private final float yaw;

    public CorpseDataPacket(int entityId, CompoundTag mobData, float yaw) {
        this.entityId = entityId;
        this.mobData = mobData;
        this.yaw = yaw;
    }

    public static void encode(CorpseDataPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.entityId);
        buffer.writeNbt(msg.mobData);
        buffer.writeFloat(msg.yaw);
    }

    public static CorpseDataPacket decode(FriendlyByteBuf buffer) {
        return new CorpseDataPacket(buffer.readInt(), buffer.readNbt(), buffer.readFloat());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) {
                return;
            }
            Entity target = Minecraft.getInstance().level.getEntity(this.entityId);
            if (target instanceof CorpseEntity corpse) {
                corpse.setMobData(this.mobData, this.yaw);
            }
        });
        ctx.setPacketHandled(true);
    }
}
