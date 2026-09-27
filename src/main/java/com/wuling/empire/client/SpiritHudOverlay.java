package com.wuling.empire.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wuling.empire.WulingEmpire;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 灵力 HUD：在饥饿/生命值上方显示一条灵力条。
 */
@Mod.EventBusSubscriber(modid = WulingEmpire.MODID, value = Dist.CLIENT)
public final class SpiritHudOverlay {

    private SpiritHudOverlay() {
    }

    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 6;
    private static final int BACKGROUND = 0xFF000000;
    private static final int FRAME = 0xFF3B3B3B;
    private static final int FILL_LOW = 0xFFFF4A4A;
    private static final int FILL_MID = 0xFFFFC94A;
    private static final int FILL_HIGH = 0xFF4FD3FF;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.options.hideGui) {
            return;
        }
        if (mc.player == null || mc.getCameraEntity() == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float ratio = ClientSpiritData.getRatio();
        float spirit = ClientSpiritData.getSpirit();
        float max = ClientSpiritData.getMax();

        int x = screenWidth / 2 - BAR_WIDTH / 2;
        // 放在饥饿值与经验条上方
        int y = screenHeight - 52;

        // 背景与边框
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, FRAME);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BACKGROUND);

        // 灵力填充
        int fillColor = ratio < 0.25F ? FILL_LOW : (ratio < 0.6F ? FILL_MID : FILL_HIGH);
        int filled = Math.round(BAR_WIDTH * ratio);
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + BAR_HEIGHT, fillColor);
        }

        // 数值文本，居中在灵力条上方
        Component label = Component.translatable("hud.wulingdiguo.spirit",
                String.format("%.1f", spirit), String.format("%.0f", max));
        int textWidth = font.width(label);
        graphics.drawString(font, label,
                screenWidth / 2 - textWidth / 2, y - 10, 0xFFFFFFFF, true);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
