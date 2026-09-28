package com.wuling.empire.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.wuling.empire.WulingEmpire;
import com.wuling.empire.network.ModMessages;
import com.wuling.empire.network.WuLingCondensePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端每 tick 监听武灵的两个快捷键（都要按住 Shift）：
 *
 * <ul>
 *   <li><b>Shift + M</b> → 发包给服务端「凝聚」：服务端按已绑定的武灵种类 + 当前境界，
 *       在玩家面前生成对应实物（如钻石境界的剑武灵 → 钻石剑）。不开任何界面。</li>
 *   <li><b>Shift + N</b> → 直接打开武灵升级面板（纯客户端界面，数据由服务端同步过来）。</li>
 * </ul>
 *
 * 两者都<b>不要求携带武灵绑定器</b>（0.2.20 起）—— 绑定器只负责「选种类」。
 * 只按 M / N 不给 Shift 会给出提示，避免误触。
 */
@Mod.EventBusSubscriber(modid = WulingEmpire.MODID, value = Dist.CLIENT)
public final class ClientTickHandler {

    private ClientTickHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            // 没在世界里（主菜单 / 退档途中）：清掉客户端缓存，
            // 免得下次进入「没开武灵」的存档时灵力 HUD 先闪一下
            ClientWuLingData.reset();
            ClientSpiritData.reset();
            return;
        }

        // 注意：consumeClick 必须每个 tick 都调用，否则按键事件会积压，
        // 所以先取「是否点了」再判断 screen，而不是反过来。
        boolean condense = ModKeyMappings.CONDENSE.consumeClick();
        boolean panel = ModKeyMappings.PANEL.consumeClick();
        if (!condense && !panel) {
            return;
        }
        if (mc.screen != null) {
            return;
        }

        boolean shift = isShiftDown(mc);

        if (condense) {
            if (!shift) {
                mc.player.sendSystemMessage(
                        Component.translatable("message.wulingdiguo.condense_need_shift"));
            } else {
                ModMessages.INSTANCE.sendToServer(new WuLingCondensePacket());
            }
        }

        if (panel) {
            if (!shift) {
                mc.player.sendSystemMessage(
                        Component.translatable("message.wulingdiguo.panel_need_shift"));
            } else {
                mc.setScreen(new WuLingUpgradeScreen());
            }
        }
    }

    private static boolean isShiftDown(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
