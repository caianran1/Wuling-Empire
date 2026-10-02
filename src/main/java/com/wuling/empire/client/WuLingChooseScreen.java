package com.wuling.empire.client;

import com.wuling.empire.network.ModMessages;
import com.wuling.empire.network.WuLingCondensePacket;
import com.wuling.empire.wuling.WuLingType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 武灵选择界面。
 *
 * 原文设定：武灵绑定器由玩家自己选择修炼方向（种类），
 * 灵珠品质只决定修炼速度、不决定种类，也不决定境界上限。
 */
public class WuLingChooseScreen extends Screen {

    private static final int PANEL_WIDTH = 260;
    // 11 种武灵 × 2 列 = 6 行，比 10 种时多一行 —— 面板跟着加高，否则会压到底部提示
    private static final int PANEL_HEIGHT = 192;
    private static final int COLS = 2;

    /** 是否以「创造模式」打开：选中后强制覆盖当前武灵（无视已拥有） */
    private final boolean creative;

    public WuLingChooseScreen() {
        this(false);
    }

    public WuLingChooseScreen(boolean creative) {
        super(Component.translatable("wuling.choose.title"));
        this.creative = creative;
    }

    @Override
    protected void init() {
        super.init();

        WuLingType[] types = WuLingType.values();
        int rows = (types.length + COLS - 1) / COLS;

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        int cellW = (PANEL_WIDTH - 24) / COLS;
        int cellH = 20;
        int startY = top + 24;

        for (int i = 0; i < types.length; i++) {
            WuLingType type = types[i];
            int col = i % COLS;
            int row = i / COLS;
            int x = left + 12 + col * cellW;
            int y = startY + row * (cellH + 2);

            Component label = Component.translatable(type.translationKey())
                    .append(" ")
                    .append(Component.translatable(type.rarity().labelKey()));

            this.addRenderableWidget(Button.builder(label, button -> choose(type))
                    .bounds(x, y, cellW - 2, cellH)
                    .build());
        }
        this.rows = rows;
    }

    private int rows = 0;

    private void choose(WuLingType type) {
        ModMessages.INSTANCE.sendToServer(new WuLingCondensePacket(type.key(), creative));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        Font font = this.font;
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int cx = this.width / 2;

        graphics.drawCenteredString(font, this.title, cx, top + 8, 0xFFAA66FF);
        graphics.drawCenteredString(font,
                Component.translatable("wuling.choose.hint"),
                cx, top + PANEL_HEIGHT - 12, 0xFF888888);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
