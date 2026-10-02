package com.wuling.empire.client;

/**
 * 客户端红石充能缓存（HUD 与充能界面读这里）。
 *
 * <p>{@link #getMax()} 为 0 表示「还没有收到服务端数据」，此时 HUD 不显示充能条 ——
 * 与 {@link ClientSpiritData} 同一套约定。
 */
public final class ClientRedstoneData {

    private ClientRedstoneData() {
    }

    private static int charge = 0;
    private static int max = 0;

    public static void set(int value, int cap) {
        ClientRedstoneData.charge = Math.max(0, value);
        ClientRedstoneData.max = Math.max(0, cap);
    }

    /** 退出世界时清空 */
    public static void reset() {
        charge = 0;
        max = 0;
    }

    public static int getCharge() {
        return charge;
    }

    public static int getMax() {
        return max;
    }

    public static boolean hasData() {
        return max > 0;
    }

    public static boolean isEmpty() {
        return charge <= 0;
    }

    /** 0.0 ~ 1.0 */
    public static float getRatio() {
        if (max <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (float) charge / (float) max));
    }
}
