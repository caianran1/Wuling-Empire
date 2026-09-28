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
 * 灵力 HUD：屏幕<b>右上角</b>的一条<b>半透明</b>灵力条。
 *
 * <p><b>0.2.28 起：武灵未开启时整条不显示</b>，判据 = {@link ClientWuLingData#isBound()}。
 * 灵力只在「凝聚武灵」时才有消耗（突破与灵力无关），没开武灵时这条没有意义，
 * 摆在屏幕上纯属干扰。</p>
 *
 * <p>位置策略：右/上各留 {@link #MARGIN} 像素；原版的药水效果图标也占着右上角那一列，
 * 所以玩家身上有药水效果时整条再左移 {@link #EFFECT_COLUMN} 像素避让，避免叠在一起。</p>
 */
@Mod.EventBusSubscriber(modid = WulingEmpire.MODID, value = Dist.CLIENT)
public final class SpiritHudOverlay {

    private SpiritHudOverlay() {
    }

    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 6;
    /** 距屏幕右边缘 / 上边缘的间距 */
    private static final int MARGIN = 8;
    /** 原版药水效果图标那一列的宽度（含边距），有药水时用来避让 */
    private static final int EFFECT_COLUMN = 34;

    // 半透明配色：前两位是 alpha，0x66≈40%、0x99≈60%、0xCC≈80%
    private static final int FRAME = 0x66FFFFFF;
    private static final int BACKGROUND = 0x99000000;
    private static final int FILL_LOW = 0xCCFF4A4A;
    private static final int FILL_MID = 0xCCFFC94A;
    private static final int FILL_HIGH = 0xCC4FD3FF;
    private static final int TEXT = 0xCCFFFFFF;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.options.hideGui) {
            return;
        }
        if (mc.player == null || mc.getCameraEntity() == null) {
            return;
        }
        // 武灵未开启 → 不显示灵力条
        if (!ClientWuLingData.isBound()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        float ratio = ClientSpiritData.getRatio();
        float spirit = ClientSpiritData.getSpirit();
        float max = ClientSpiritData.getMax();

        // 右上角；有药水效果时往左让出效果图标那一列
        int right = screenWidth - MARGIN;
        if (!mc.player.getActiveEffects().isEmpty()) {
            right -= EFFECT_COLUMN;
        }
        int x = right - BAR_WIDTH;
        int y = MARGIN;

        // 显式开混合，保证 fill 的 alpha 真的被应用
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 边框与背景
        graphics.fill(x - 1, y - 1, right + 1, y + BAR_HEIGHT + 1, FRAME);
        graphics.fill(x, y, right, y + BAR_HEIGHT, BACKGROUND);

        // 灵力填充
        int fillColor = ratio < 0.25F ? FILL_LOW : (ratio < 0.6F ? FILL_MID : FILL_HIGH);
        int filled = Math.round(BAR_WIDTH * ratio);
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + BAR_HEIGHT, fillColor);
        }

        // 数值文本：右对齐，贴在灵力条下方（条已经在屏幕顶边了，放上面会出界）
        Component label = Component.translatable("hud.wulingdiguo.spirit",
                String.format("%.1f", spirit), String.format("%.0f", max));
        graphics.drawString(font, label,
                right - font.width(label), y + BAR_HEIGHT + 2, TEXT, true);

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
