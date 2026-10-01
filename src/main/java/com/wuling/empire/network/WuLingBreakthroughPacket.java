package com.wuling.empire.network;

import com.wuling.empire.Config;
import com.wuling.empire.capability.ModCapabilities;
import com.wuling.empire.wuling.BreakthroughRequirement;
import com.wuling.empire.wuling.WuLingBinding;
import com.wuling.empire.wuling.WuLingRealm;
import com.wuling.empire.wuling.WuLingStage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C → S：玩家在武灵升级面板里点下「突破」。
 *
 * 大境界突破流程：
 *   在大境界的「后期」提交指定物资即可突破。
 *   突破<b>完全与灵力无关</b>（0.2.15 起）：既不消耗灵力，也不要求灵力圆满。
 *   灵力改由「凝聚武灵」消耗，见 {@code Config#CONDENSE_SPIRIT_COST}。
 */
public class WuLingBreakthroughPacket {

    public WuLingBreakthroughPacket() {
    }

    public static void encode(WuLingBreakthroughPacket msg, FriendlyByteBuf buf) {
        // 无载荷
    }

    public static WuLingBreakthroughPacket decode(FriendlyByteBuf buf) {
        return new WuLingBreakthroughPacket();
    }

    public static void handle(WuLingBreakthroughPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            if (sender == null) {
                return;
            }
            perform(sender);
        });
        context.setPacketHandled(true);
    }

    private static void perform(ServerPlayer player) {
        player.getCapability(ModCapabilities.WU_LING).ifPresent(wuLing -> {
            var data = wuLing.data();

            if (!data.isBound()) {
                player.sendSystemMessage(Component.translatable("message.wulingdiguo.not_bound"));
                return;
            }
            if (data.isMaxed()) {
                player.sendSystemMessage(Component.translatable("message.wulingdiguo.already_max"));
                return;
            }
            // 大境界突破只能在大境界的「后期」发起
            if (data.stage() != WuLingStage.LATE) {
                player.sendSystemMessage(Component.translatable("message.wulingdiguo.not_late_stage"));
                return;
            }
            // 统一上限（与灵珠品质无关）
            if (data.realmOrdinal() >= data.capRealm()) {
                player.sendSystemMessage(Component.translatable("message.wulingdiguo.hit_cap",
                        Component.translatable(WuLingRealm.byOrdinal(data.capRealm()).translationKey())));
                return;
            }

            int target = data.realmOrdinal() + 1;

            // 突破不校验灵力：灵力只与「凝聚武灵」挂钩（0.2.15 起门槛也一并去掉）

            // 物资校验 + 扣除（会先把背包里剩余的自动缴进缴纳池，再判定池子是否齐全）
            BreakthroughRequirement.Result result =
                    BreakthroughRequirement.tryConsumeAndAdvance(player, target, data);
            if (!result.success()) {
                player.sendSystemMessage(result.message());
                // 物资不够，但缴纳池可能已经入账，同步给客户端刷新面板
                ModMessages.sendWuLingTo(player);
                return;
            }

            data.advanceRealm();

            // 背包里的武灵实物跟着升一档（2026-10-01 用户口径：
            // 「每一级升级时手上的武灵物品也会一同升级」）
            int upgraded = WuLingBinding.refreshManifestItems(player);
            if (upgraded > 0) {
                player.sendSystemMessage(Component.translatable(
                        "message.wulingdiguo.manifest_upgraded", upgraded));
            }

            // 突破不消耗灵力；只有明确开启时才清空
            if (Config.BREAKTHROUGH_CONSUMES_SPIRIT.get()) {
                player.getCapability(ModCapabilities.SPIRIT_POWER).ifPresent(spirit -> {
                    spirit.setSpirit(0.0F);
                    ModMessages.sendSpiritTo(player);
                });
            }

            player.sendSystemMessage(Component.translatable("message.wulingdiguo.breakthrough_ok",
                    Component.translatable(WuLingRealm.byOrdinal(target).translationKey())));
            ModMessages.sendWuLingTo(player);
        });
    }
}
