package com.wuling.empire.client;

/**
 * 客户端灵力缓存。
 *
 * HUD 只读这里的值；真正的权威数据在服务端 Capability 里。
 */
public final class ClientSpiritData {

    private ClientSpiritData() {
    }

    private static float spirit = 100.0F;
    private static float max = 100.0F;

    public static void set(float spirit, float max) {
        ClientSpiritData.spirit = spirit;
        ClientSpiritData.max = max;
    }

    /** 退出世界时清空：max = 0 表示「还没有数据」，比例按 0 处理 */
    public static void reset() {
        ClientSpiritData.spirit = 0.0F;
        ClientSpiritData.max = 0.0F;
    }

    public static float getSpirit() {
        return spirit;
    }

    public static float getMax() {
        return max;
    }

    /** 0.0 ~ 1.0 */
    public static float getRatio() {
        if (max <= 0.0F) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, spirit / max));
    }
}
