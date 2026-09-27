package com.wuling.empire.network;

import com.wuling.empire.Config;
import com.wuling.empire.WulingEmpire;
import com.wuling.empire.capability.ISpiritPower;
import com.wuling.empire.capability.ModCapabilities;
import com.wuling.empire.entity.CorpseEntity;
import com.wuling.empire.wuling.WuLingData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络通道
 */
@Mod.EventBusSubscriber(modid = WulingEmpire.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModMessages {

    private ModMessages() {
    }

    private static final String PROTOCOL_VERSION = "1";

    public static SimpleChannel INSTANCE;

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        INSTANCE = NetworkRegistry.newSimpleChannel(
                WulingEmpire.id("main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals);

        INSTANCE.registerMessage(0, SpiritPowerSyncPacket.class,
                SpiritPowerSyncPacket::encode, SpiritPowerSyncPacket::decode, SpiritPowerSyncPacket::handle);
        INSTANCE.registerMessage(1, CorpseDataPacket.class,
                CorpseDataPacket::encode, CorpseDataPacket::decode, CorpseDataPacket::handle);
        INSTANCE.registerMessage(2, WuLingSyncPacket.class,
                WuLingSyncPacket::encode, WuLingSyncPacket::decode, WuLingSyncPacket::handle);
        INSTANCE.registerMessage(3, WuLingBreakthroughPacket.class,
                WuLingBreakthroughPacket::encode, WuLingBreakthroughPacket::decode,
                WuLingBreakthroughPacket::handle);
        INSTANCE.registerMessage(4, WuLingCondensePacket.class,
                WuLingCondensePacket::encode, WuLingCondensePacket::decode,
                WuLingCondensePacket::handle);
        INSTANCE.registerMessage(5, WuLingSubmitPacket.class,
                WuLingSubmitPacket::encode, WuLingSubmitPacket::decode,
                WuLingSubmitPacket::handle);
    }

    /** 把玩家的灵力值推送给该玩家的客户端 */
    public static void sendSpiritTo(ServerPlayer player) {
        ISpiritPower power = player.getCapability(ModCapabilities.SPIRIT_POWER).orElse(null);
        if (power == null) {
            return;
        }
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new SpiritPowerSyncPacket(power.getSpirit(), Config.maxSpirit()));
    }

    /** 玩家开始追踪某个尸体时，把它的外观数据推送过去 */
    public static void sendCorpseDataTo(ServerPlayer player, CorpseEntity corpse) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new CorpseDataPacket(corpse.getId(), corpse.getMobData(), corpse.getVisualYaw()));
    }

    /** 把玩家的武灵数据推送给该玩家的客户端 */
    public static void sendWuLingTo(ServerPlayer player) {
        player.getCapability(ModCapabilities.WU_LING).ifPresent(wuLing -> {
            WuLingData data = wuLing.data();
            INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                    new WuLingSyncPacket(
                            data.isBound(),
                            data.typeKey(),
                            data.sourceKey(),
                            data.realmOrdinal(),
                            data.stageOrdinal(),
                            data.progress(),
                            Config.stageThreshold(data.realmOrdinal()),
                            data.cultivationBonus(),
                            data.submittedSnapshot()));
        });
    }
}
