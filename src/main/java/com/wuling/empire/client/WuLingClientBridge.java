package com.wuling.empire.client;

import net.minecraft.client.Minecraft;

/**
 * 服务端 ↔ 客户端的桥接点。
 *
 * 普通 java 代码通过 DistExecutor.runWhenOn 调到这里，
 * 这样客户端专属类不会在专用服务端上被加载。
 */
public final class WuLingClientBridge {

    private WuLingClientBridge() {
    }

    /** DistExecutor 的目标方法，用于在客户端打开武灵升级面板 */
    public static void openPanel() {
        Minecraft.getInstance().setScreen(new WuLingUpgradeScreen());
    }

    /** DistExecutor 的目标方法，用于在客户端打开武灵种类选择界面 */
    public static void openChoose() {
        Minecraft.getInstance().setScreen(new WuLingChooseScreen());
    }

    /** 创造模式版本：选中后强制覆盖当前武灵（无视已拥有） */
    public static void openChooseCreative() {
        Minecraft.getInstance().setScreen(new WuLingChooseScreen(true));
    }
}
