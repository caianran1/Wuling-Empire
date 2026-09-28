package com.wuling.empire.client;

import com.wuling.empire.wuling.WuLingRealm;
import com.wuling.empire.wuling.WuLingStage;
import com.wuling.empire.wuling.WuLingType;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * 客户端武灵数据缓存。真正的权威数据在服务端。
 */
public final class ClientWuLingData {

    private ClientWuLingData() {
    }

    private static boolean bound = false;
    private static String typeKey = WuLingType.SWORD.key();
    private static String sourceKey = "";
    private static int realmOrdinal = 0;
    private static int stageOrdinal = 0;
    private static double progress = 0.0D;
    private static double threshold = 1.0D;
    /** 修炼速度加成倍率（品质决定，上限所有人一致） */
    private static double cultivationBonus = 1.0D;
    /** 已缴纳的突破物资（键 → 数量），由服务端同步 */
    private static Map<String, Integer> submitted = Map.of();

    public static void set(boolean bound, String typeKey, String sourceKey,
                           int realmOrdinal, int stageOrdinal, double progress,
                           double threshold, double cultivationBonus, Map<String, Integer> submitted) {
        ClientWuLingData.bound = bound;
        ClientWuLingData.typeKey = typeKey;
        ClientWuLingData.sourceKey = sourceKey;
        ClientWuLingData.realmOrdinal = realmOrdinal;
        ClientWuLingData.stageOrdinal = stageOrdinal;
        ClientWuLingData.progress = progress;
        ClientWuLingData.threshold = threshold;
        ClientWuLingData.cultivationBonus = cultivationBonus;
        ClientWuLingData.submitted = submitted == null ? Map.of() : submitted;
    }

    /** 已缴纳物资快照，供突破面板渲染 */
    public static Map<String, Integer> submitted() {
        return submitted;
    }

    /**
     * 退出世界时清空缓存（bound 回到 false → 灵力 HUD 直接隐藏）。
     * 不清的话，「开过武灵的存档 → 没开武灵的存档」会有一小段残留显示。
     */
    public static void reset() {
        set(false, WuLingType.SWORD.key(), "", 0, 0, 0.0D, 1.0D, 1.0D, Map.of());
    }

    public static boolean isBound() {
        return bound;
    }

    public static WuLingType type() {
        return WuLingType.byKey(typeKey);
    }

    public static String sourceKey() {
        return sourceKey;
    }

    public static WuLingRealm realm() {
        return WuLingRealm.byOrdinal(realmOrdinal);
    }

    public static WuLingStage stage() {
        return WuLingStage.byOrdinal(stageOrdinal);
    }

    public static int realmOrdinal() {
        return realmOrdinal;
    }

    public static int stageOrdinal() {
        return stageOrdinal;
    }

    /** 修炼速度加成倍率（由开启武灵所用灵珠的品质决定） */
    public static double cultivationBonus() {
        return cultivationBonus;
    }

    /** 3.0 → "3"，1.25 → "1.25"：给「修炼速度 ×N」用 */
    public static String cultivationLabel() {
        return Math.abs(cultivationBonus - Math.round(cultivationBonus)) < 0.005D
                ? String.valueOf(Math.round(cultivationBonus))
                : String.valueOf(Math.round(cultivationBonus * 100.0D) / 100.0D);
    }

    public static double progress() {
        return progress;
    }

    public static double threshold() {
        return threshold;
    }

    public static double ratio() {
        if (threshold <= 0.0D) {
            return 1.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, progress / threshold));
    }

    /** 武灵名只有种类本身，例如「剑」，不带括号 */
    public static Component displayName() {
        return Component.translatable(type().translationKey());
    }

    /** 「钻石 · 中期」这样的完整境界名 */
    public static Component realmLabel() {
        return Component.translatable(realm().translationKey())
                .append(" · ")
                .append(Component.translatable(stage().translationKey()));
    }
}
