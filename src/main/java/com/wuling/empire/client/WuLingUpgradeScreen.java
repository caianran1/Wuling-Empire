package com.wuling.empire.client;

import com.wuling.empire.network.ModMessages;
import com.wuling.empire.wuling.BreakthroughRequirement;
import com.wuling.empire.wuling.WuLingStage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 专属武灵升级面板。
 *
 * 小境界靠累积自动晋升，这里只展示进度；
 * 大境界（后期 → 下一境界）在这里提交物资并突破。
 */
public class WuLingUpgradeScreen extends Screen {

    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 200;

    private static final int BAR_WIDTH = 180;
    private static final int BAR_HEIGHT = 6;

    private List<BreakthroughRequirement.Row> rows = List.of();
    private int refreshCounter = 0;

    public WuLingUpgradeScreen() {
        super(Component.translatable("wuling.panel.title"));
    }

    @Override
    protected void init() {
        super.init();
        refreshRows();

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int buttonY = top + PANEL_HEIGHT - 28;
        int gap = 6;
        int halfWidth = (PANEL_WIDTH - 40 - gap) / 2;

        // 提交物资：背包放不下时，先把能缴的缴进缴纳池
        this.addRenderableWidget(Button.builder(
                        Component.translatable("wuling.panel.submit"),
                        button -> {
                            ModMessages.INSTANCE.sendToServer(
                                    new com.wuling.empire.network.WuLingSubmitPacket());
                            refreshRows();
                        })
                .bounds(left + 20, buttonY, halfWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("wuling.panel.breakthrough"),
                        button -> {
                            ModMessages.INSTANCE.sendToServer(
                                    new com.wuling.empire.network.WuLingBreakthroughPacket());
                            Minecraft.getInstance().setScreen(null);
                        })
                .bounds(left + 20 + halfWidth + gap, buttonY, halfWidth, 20)
                .build());
    }

    private void refreshRows() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            rows = List.of();
            return;
        }
        rows = BreakthroughRequirement.evaluate(mc.player, ClientWuLingData.realmOrdinal() + 1,
                ClientWuLingData.submitted());
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

        if (!ClientWuLingData.isBound()) {
            graphics.drawCenteredString(font,
                    Component.translatable("wuling.panel.unbound"),
                    cx, top + 40, 0xFFAAAAAA);
            graphics.drawCenteredString(font,
                    Component.translatable("wuling.panel.unbound_hint"),
                    cx, top + 56, 0xFF777777);
            return;
        }

        // 武灵图标 + 种类
        ItemStack icon = ClientWuLingData.type().iconStack();
        graphics.renderItem(icon, left + 16, top + 26);
        graphics.drawString(font, ClientWuLingData.displayName(),
                left + 36, top + 30, 0xFFFFFFFF);

        // 境界
        graphics.drawString(font, ClientWuLingData.realmLabel(), left + 16, top + 48, 0xFFDDDDDD);

        // 修炼速度加成（品质只影响快慢，不影响上限）
        graphics.drawString(font,
                Component.translatable("wuling.panel.cultivation",
                        ClientWuLingData.cultivationLabel()),
                left + 16, top + 60, 0xFF8FBF8F);

        // 进度条
        int barX = cx - BAR_WIDTH / 2;
        int barY = top + 76;
        graphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, 0xFF3B3B3B);
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF000000);
        int filled = (int) Math.round(BAR_WIDTH * ClientWuLingData.ratio());
        if (filled > 0) {
            graphics.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, 0xFFAA66FF);
        }
        graphics.drawCenteredString(font,
                Component.translatable("wuling.panel.progress",
                        String.format("%.0f", ClientWuLingData.progress()),
                        String.format("%.0f", ClientWuLingData.threshold())),
                cx, barY + 10, 0xFFCCCCCC);

        // 每 10 帧刷新一次背包统计，避免每帧全盘扫描
        if (++refreshCounter % 10 == 0) {
            refreshRows();
        }

        boolean atLate = ClientWuLingData.stage() == WuLingStage.LATE;
        int y = barY + 24;

        if (!atLate) {
            graphics.drawCenteredString(font,
                    Component.translatable("wuling.panel.cultivating"),
                    cx, y, 0xFF888888);
            return;
        }

        graphics.drawCenteredString(font,
                BreakthroughRequirement.titleFor(ClientWuLingData.realmOrdinal() + 1),
                cx, y, 0xFFFFCC55);
        y += 12;

        for (BreakthroughRequirement.Row row : rows) {
            int color = row.ok() ? 0xFF66FF66 : 0xFFFF6666;
            graphics.drawString(font, row.name(), left + 16, y, 0xFFDDDDDD);
            if (row.deposited() > 0) {
                graphics.drawString(font,
                        Component.translatable("wuling.panel.deposited", row.deposited()),
                        left + 16 + font.width(row.name()) + 4, y, 0xFF7F93B5);
            }
            graphics.drawString(font,
                    Component.translatable("wuling.panel.count", row.have(), row.need()),
                    left + PANEL_WIDTH - 76, y, color);
            y += 10;
            if (y > top + PANEL_HEIGHT - 44) {
                graphics.drawString(font, Component.literal("…"), left + 16, y, 0xFF777777);
                break;
            }
        }

        // 背包放不下时可以先缴一部分
        graphics.drawString(font,
                Component.translatable("wuling.panel.submit_hint"),
                left + 16, top + PANEL_HEIGHT - 40, 0xFF888888);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 供按钮判断是否可用（当前不做动态启用/禁用，仅保留接口） */
    protected boolean canBreakThrough() {
        // 突破不看灵力：灵力只与凝聚武灵有关；上限所有人一致（Config#maxRealm）
        return ClientWuLingData.isBound()
                && ClientWuLingData.stage() == WuLingStage.LATE
                && ClientWuLingData.realmOrdinal() < com.wuling.empire.Config.maxRealm();
    }
}
