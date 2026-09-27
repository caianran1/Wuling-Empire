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
 * 目前只有一档「绿宝石」—— 原文里绿宝石境界的装备是单独设计的一档实物。
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
}
