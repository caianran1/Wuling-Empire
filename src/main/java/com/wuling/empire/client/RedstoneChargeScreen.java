package com.wuling.empire.client;

import com.wuling.empire.item.SpiritBeadItem;
import com.wuling.empire.item.SpiritQuality;
import com.wuling.empire.network.ModMessages;
import com.wuling.empire.network.RedstoneChargePacket;
import com.wuling.empire.wuling.RedstoneCharging;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 红石充能界面 —— 手持任意红石装备右键打开（2026-10-02 用户口径）。
 *
 * <p>列出<b>背包里现有的灵珠</b>（按「来源 + 品质」聚合成一行，显示总数），
 * 每行末尾一个「充能」按钮，点一次消耗一颗、按
 * {@link RedstoneCharging#chargeValue} 折算充能点数加进装备池。
 *
 * <p>界面只负责挑珠子、发包；扣哪一颗、加多少、上限是多少，全部由服务端算 ——
 * 所以这里<b>不缓存</b>背包快照，每帧直接读背包重建列表，
 * 服务端扣完珠子后界面会立刻跟着变（不用手动刷新）。
 *
 * <p>纯手写渲染 + 自算命中区，风格与 {@code WuLingBookScreen} 保持一致。
 */
public class RedstoneChargeScreen extends Screen {

    private static final int PANEL_W = 320;
    private static final int PANEL_H = 220;
    private static final int ROW_H = 18;
    private static final int LIST_PAD = 8;
    private static final int HEADER_H = 46;
    private static final int FOOTER_H = 16;
    private static final int BTN_W = 40;
    private static final int GAP = 6;

    private static final int COL_TEXT = 0xFFE6E6E6;
    private static final int COL_DIM = 0xFF9A9A9A;
    private static final int COL_TITLE = 0xFFFF5555;
    private static final int COL_BTN = 0xFF6A2A2A;
    private static final int COL_BTN_HOVER = 0xFF9A3A3A;
    private static final int COL_BTN_BORDER = 0xFFC08080;
    private static final int COL_ROW_HOVER = 0x30FFFFFF;
    private static final int COL_BAR_BG = 0x99000000;
    private static final int COL_BAR_FILL = 0xCCFF3B30;
    /** 充能标尺同样用「红涨绿跌」以外的中性配色：这里只是能量条，用红石红 */
    private static final int COL_GAIN = 0xFFFF9090;

    /** 一行 = 一种「来源 + 品质」的灵珠 */
    private static final class Row {
        final String source;
        final SpiritQuality quality;
        final Component beadName;
        int count;

        Row(String source, SpiritQuality quality, Component beadName, int count) {
            this.source = source;
            this.quality = quality;
            this.beadName = beadName;
            this.count = count;
        }

        int gain() {
            return RedstoneCharging.chargeValue(source, quality);
        }

        String label() {
            return beadName.getString();
        }
    }

    private int scroll = 0;

    public RedstoneChargeScreen() {
        super(Component.translatable("wuling.redstone.title"));
    }

    // ===================== 数据 =====================

    /**
     * 从背包现算一份列表（按「来源 + 品质」聚合）。
     *
     * <p>每帧重建：服务端扣完灵珠后物品栏会同步下来，列表跟着变，
     * 不用维护「界面上显示的库存」与真实背包两份状态。
     */
    private List<Row> collect() {
        Map<String, Row> map = new LinkedHashMap<>();
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return new ArrayList<>();
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof SpiritBeadItem)) {
                continue;
            }
            String source = SpiritBeadItem.getSource(stack);
            SpiritQuality quality = SpiritBeadItem.getQuality(stack);
            String key = source + "|" + quality.key();
            Row row = map.get(key);
            if (row == null) {
                row = new Row(source, quality, stack.getHoverName(), 0);
                map.put(key, row);
            }
            row.count += stack.getCount();
        }
        List<Row> rows = new ArrayList<>(map.values());
        // 品质高的排前面，同品质按名字
        rows.sort(Comparator.comparingInt((Row r) -> -r.quality.ordinal())
                .thenComparing(Row::label));
        return rows;
    }

    // ===================== 布局 =====================

    private int left() {
        return (this.width - PANEL_W) / 2;
    }

    private int top() {
        return (this.height - PANEL_H) / 2;
    }

    private int listTop() {
        return top() + HEADER_H;
    }

    private int listBottom() {
        return top() + PANEL_H - FOOTER_H;
    }

    private int listHeight() {
        return listBottom() - listTop();
    }

    private int listLeft() {
        return left() + LIST_PAD;
    }

    private int listRight() {
        return left() + PANEL_W - LIST_PAD;
    }

    private int buttonX() {
        return listRight() - BTN_W;
    }

    private int maxScroll(int rowCount) {
        return Math.max(0, rowCount * ROW_H - listHeight());
    }

    // ===================== 渲染 =====================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int l = left();
        int t = top();
        graphics.fill(l - 1, t - 1, l + PANEL_W + 1, t + PANEL_H + 1, 0xFF000000);
        graphics.fill(l, t, l + PANEL_W, t + PANEL_H, 0xC8180808);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, t + 6, COL_TITLE);

        drawChargeBar(graphics, l, t);

        List<Row> rows = collect();
        scroll = Math.max(0, Math.min(scroll, maxScroll(rows.size())));

        int lt = listTop();
        int lb = listBottom();
        if (rows.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("wuling.redstone.no_beads"),
                    this.width / 2, lt + listHeight() / 2, COL_DIM);
        } else {
            graphics.enableScissor(listLeft(), lt, listRight(), lb);
            int first = Math.max(0, scroll / ROW_H - 1);
            int last = Math.min(rows.size(), (scroll + listHeight()) / ROW_H + 2);
            for (int i = first; i < last; i++) {
                drawRow(graphics, rows.get(i), lt + i * ROW_H - scroll, mouseX, mouseY, lt, lb);
            }
            graphics.disableScissor();
        }

        drawScrollbar(graphics, lt, lb, rows.size());

        graphics.drawCenteredString(this.font,
                Component.translatable("wuling.redstone.hint"),
                this.width / 2, t + PANEL_H - 11, COL_DIM);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawChargeBar(GuiGraphics graphics, int l, int t) {
        int x = l + LIST_PAD;
        int y = t + 20;
        int w = PANEL_W - LIST_PAD * 2;
        int h = 10;

        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        graphics.fill(x, y, x + w, y + h, COL_BAR_BG);
        int filled = Math.round(w * ClientRedstoneData.getRatio());
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + h, COL_BAR_FILL);
        }

        Component text = Component.translatable("wuling.redstone.charge_value",
                String.valueOf(ClientRedstoneData.getCharge()),
                String.valueOf(ClientRedstoneData.getMax()));
        int tx = x + w / 2 - this.font.width(text) / 2;
        graphics.drawString(this.font, text, tx, y + 1, 0xFFFFFFFF, true);
    }

    private void drawRow(GuiGraphics graphics, Row row, int y,
                         int mouseX, int mouseY, int listTop, int listBottom) {
        int x0 = listLeft();
        int x1 = listRight();

        boolean hovered = mouseX >= x0 && mouseX < x1
                && mouseY >= Math.max(y, listTop) && mouseY < Math.min(y + ROW_H, listBottom);
        if (hovered) {
            graphics.fill(x0, y, x1, y + ROW_H, COL_ROW_HOVER);
        }

        // 品质颜色的小方块 + 「名字 × N」
        graphics.fill(x0 + 3, y + 4, x0 + 9, y + ROW_H - 4, row.quality.color());
        String label = row.label() + " × " + row.count;
        graphics.drawString(this.font, label, x0 + 14, y + 5, COL_TEXT, false);

        // 「+50」
        String gain = "+" + row.gain();
        int gainW = this.font.width(gain);
        int gainX = buttonX() - GAP - gainW;
        graphics.drawString(this.font, gain, gainX, y + 5, COL_GAIN, false);

        int btnY = y + 2;
        int btnH = ROW_H - 4;
        drawButton(graphics, buttonX(), btnY, BTN_W, btnH,
                Component.translatable("wuling.redstone.charge").getString(),
                hit(mouseX, mouseY, buttonX(), btnY, BTN_W, btnH));
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int w, int h,
                            String label, boolean hovered) {
        graphics.fill(x, y, x + w, y + h, hovered ? COL_BTN_HOVER : COL_BTN);
        graphics.fill(x, y, x + w, y + 1, COL_BTN_BORDER);
        graphics.fill(x, y + h - 1, x + w, y + h, COL_BTN_BORDER);
        graphics.fill(x, y, x + 1, y + h, COL_BTN_BORDER);
        graphics.fill(x + w - 1, y, x + w, y + h, COL_BTN_BORDER);
        graphics.drawCenteredString(this.font, label, x + w / 2, y + (h - 8) / 2 + 1, COL_TEXT);
    }

    private void drawScrollbar(GuiGraphics graphics, int lt, int lb, int rowCount) {
        int max = maxScroll(rowCount);
        if (max <= 0) {
            return;
        }
        int trackH = lb - lt;
        int barH = Math.max(12, trackH * trackH / (rowCount * ROW_H));
        int barY = lt + (trackH - barH) * scroll / max;
        int x = left() + PANEL_W - 5;
        graphics.fill(x, lt, x + 2, lb, 0x40FFFFFF);
        graphics.fill(x, barY, x + 2, barY + barH, 0xFFAAAAAA);
    }

    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ===================== 交互 =====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        List<Row> rows = collect();
        int lt = listTop();
        int lb = listBottom();
        if (mouseX < listLeft() || mouseX >= listRight() || mouseY < lt || mouseY >= lb) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int index = (int) ((mouseY - lt + scroll) / ROW_H);
        if (index < 0 || index >= rows.size()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int y = lt + index * ROW_H - scroll;
        if (y < lt || y + ROW_H > lb) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int btnY = y + 2;
        int btnH = ROW_H - 4;
        if (hit((int) mouseX, (int) mouseY, buttonX(), btnY, BTN_W, btnH)) {
            Row row = rows.get(index);
            ModMessages.INSTANCE.sendToServer(
                    new RedstoneChargePacket(row.source, row.quality.key()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, Math.min(maxScroll(collect().size()),
                scroll - (int) (delta * ROW_H * 2)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
