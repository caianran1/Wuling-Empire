package com.wuling.empire.wuling;

import com.wuling.empire.Config;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 大境界：木 → 石 → 黄金 → 玄铁 → 钻石 → 下界合金 → 绿宝石
 *
 * 每个大境界内部再分前 / 中 / 后三个小境界，
 * 因此总共有 7 × 3 = 21 个台阶。
 */
public enum WuLingRealm {

    WOOD("wood", Items.OAK_LOG, 3.0D),
    STONE("stone", Items.COBBLESTONE, 4.0D),
    GOLD("gold", Items.GOLD_INGOT, 8.0D),
    METEOR_IRON("meteor_iron", Items.IRON_INGOT, 9.0D),
    DIAMOND("diamond", Items.DIAMOND, 11.0D),
    NETHERITE("netherite", Items.NETHERITE_INGOT, 14.0D),
    EMERALD("emerald", Items.EMERALD, 15.0D);

    private final String key;
    private final Item icon;
    private final double manifestMultiplier;

    WuLingRealm(String key, Item icon, double manifestMultiplier) {
        this.key = key;
        this.icon = icon;
        this.manifestMultiplier = manifestMultiplier;
    }

    /**
     * 「武灵之力」在本境界的<b>总倍数</b>。
     *
     * <p><b>最终数值 = 该物品原版同款的数值 × 本倍数</b>（配置默认 100% 时）。
     * 实现见 {@code WuLingType#empower}：挂上去的修饰符 = 原版数值 ×
     * (本倍数 - 1) × 配置百分比，于是原版数值 + 追加量 = 原版数值 × 本倍数。
     *
     * <p>2026-09-27 用户要求「每个级别都碾压上一个级别」。注意原版各材质的
     * 底子<b>并不是等差的</b>（剑的伤害：木 4、石 5、<b>金 4（比石还低）</b>、
     * 玄铁 6、钻石 7、下界合金 8，绿宝石是单独设计的物品 16），所以要让
     * <b>实际数值</b>形成明显的阶梯，倍数本身必须按底子补偿。最终结果：
     *
     * <pre>
     *   境界      总倍数   剑(原版→凝聚)   相邻倍率
     *   木          3       4  →  12          —
     *   石          4       5  →  20         1.67×
     *   黄金        8       4  →  32         1.60×
     *   玄铁        9       6  →  54         1.69×
     *   钻石       11       7  →  77         1.43×
     *   下界合金   14       8  → 112         1.45×
     *   绿宝石     15      16  → 240         2.14×
     * </pre>
     *
     * <p>每一档都比上一档强 1.43 倍以上，没有任何一档在原地踏步，
     * 也不存在低境界倒挂（黄金档倍数给到 8，是因为原版金剑底子只有 4、
     * 比石剑的 5 还低，沿用等比会掉到石境界之下）。绿宝石档 240 已经
     * 远超一击秒杀满血末影龙（200 血）所需的量级，为原文第五部留了位置。
     *
     * <p>倍数由「整个境界」共用：剑 / 斧 / 镐 / 铲 / 胸甲一视同仁。
     * 若将来只想给某一件单独调，在 {@code WuLingType#manifest} 里加分支即可。
     */
    public double manifestMultiplier() {
        return manifestMultiplier;
    }

    /**
     * 算上小境界之后的倍率 —— <b>2026-10-02 用户口径「每升一等级基础属性都会提升」</b>。
     *
     * <p>以前只有大境界会改数值，小境界（前 / 中 / 后）纯属进度条上的刻度，
     * 升了看不出差别。现在改成：
     *
     * <pre>
     *   最终倍数 = 大境界倍数 × (1 + manifestStageStep × 小境界序号)
     *             小境界序号：前期 0 / 中期 1 / 后期 2
     * </pre>
     *
     * <p>默认 {@code manifestStageStep = 0.1}，于是木档剑 4 → 12 / 13.2 / 14.4，
     * 绿宝石档 → 240 / 264 / 288。大境界之间的断层（1.43 倍以上）不受影响，
     * 因为每一档的小境界都是从「前期」重新起步的。
     */
    public double manifestMultiplier(int stageOrdinal) {
        return manifestMultiplier * stageFactor(stageOrdinal);
    }

    /** 小境界系数，见 {@link #manifestMultiplier(int)} */
    public static double stageFactor(int stageOrdinal) {
        int step = Math.max(0, Math.min(WuLingStage.LATE.ordinal(), stageOrdinal));
        return 1.0D + Config.MANIFEST_STAGE_STEP.get() * step;
    }

    public String key() {
        return key;
    }

    public ItemStack iconStack() {
        return new ItemStack(icon);
    }

    public String translationKey() {
        return "wuling.realm." + key;
    }

    public static WuLingRealm byOrdinal(int ordinal) {
        WuLingRealm[] values = values();
        if (ordinal < 0) {
            return values[0];
        }
        if (ordinal >= values.length) {
            return values[values.length - 1];
        }
        return values[ordinal];
    }
}
