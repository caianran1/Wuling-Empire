package com.wuling.empire.item;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * 本模组自己的工具材质。
 *
 * 有两档：
 *   * 「绿宝石」—— 原文里绿宝石境界的装备是单独设计的一档实物；
 *   * 「红石」—— 充能类装备的低配底子（0.3.6），强度由动态属性修饰符托着，见 {@link #REDSTONE}。
 *
 * <b>定位：碾压钻石，不是「比下界合金略高一点」</b>（2026-09-26 用户口径
 * 「绿宝石的所有东西都比钻石强很多倍」）。所以这一档的每一项都按钻石的
 * 数倍来给，而不是在下界合金的基础上 +1：
 *
 * <pre>
 *              钻石     下界合金    绿宝石     倍数(对钻石)
 *   耐久        1561      2031      8000       5.1×
 *   挖掘速度     8.0       9.0      25.0       3.1×
 *   伤害档      +3.0      +4.0     +12.0       4.0×
 *   附魔度       10        15        40        4.0×
 * </pre>
 *
 * 注意 {@code getAttackDamageBonus()} 只是「基础值」，各物品自己的
 * 攻击力修正（{@code SwordItem} 的 3、{@code AxeItem} 的 5 …）在 {@link ModItems} 里另给，
 * 实际伤害 = 1（空手）+ 物品修正 + 这里的基础值。
 */
public final class ModTiers {

    private ModTiers() {
    }

    /** 绿宝石：耐久 / 挖掘速度 / 基础伤害 / 附魔度全面数倍于钻石 */
    public static final Tier EMERALD = new Tier() {

        @Override
        public int getUses() {
            return 8000;
        }

        @Override
        public float getSpeed() {
            return 25.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 12.0F;
        }

        @Override
        public int getLevel() {
            return 5;
        }

        @Override
        public int getEnchantmentValue() {
            return 40;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(Items.EMERALD);
        }

        /**
         * 挖掘判定的「不能正确掉落」标签。
         * 接口的默认实现会退到木制档，必须显式指到下界合金那一档，
         * 否则钻石级方块挖了不掉东西。
         */
        @Override
        public TagKey<Block> getTag() {
            return Tiers.NETHERITE.getTag();
        }
    };

    /**
     * 红石：<b>充能类装备</b>的材质档（2026-10-02）。
     *
     * <p>关键设计：这一档的数值<b>故意给得很低</b>（攻击加成 0、挖掘速度只是铁档水平），
     * 因为红石装备的实际强度不来自材质，而是来自
     * {@code wuling/RedstoneGear} 按「玩家武灵境界 + 1 档」动态挂上去的属性修饰符。
     *
     * <p>这样做的原因：<b>充能耗尽时加成要能失效</b>。材质自带的属性无法移除，
     * 修饰符可以 —— 所以基础给弱材质，强度全靠可增可减的修饰符托着。
     *
     * <p>挖掘速度是例外：原版里挖掘速度只能由 {@code Tier#getSpeed} 决定
     * （它不是属性修饰符），没法动态改，所以这里直接给铁档水平的 10 点。
     */
    public static final Tier REDSTONE = new Tier() {

        @Override
        public int getUses() {
            return 6000;
        }

        @Override
        public float getSpeed() {
            return 10.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 0.0F;
        }

        @Override
        public int getLevel() {
            return 4;
        }

        @Override
        public int getEnchantmentValue() {
            return 22;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(Items.REDSTONE);
        }

        @Override
        public TagKey<Block> getTag() {
            return Tiers.NETHERITE.getTag();
        }
    };
}
