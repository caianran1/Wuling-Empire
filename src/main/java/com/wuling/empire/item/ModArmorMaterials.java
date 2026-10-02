package com.wuling.empire.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 本模组自己的盔甲材质。
 *
 * 有两档：
 *   * 「绿宝石」—— 各项都数倍于钻石（见下）；
 *   * 「红石」—— 充能类护甲的低配底子（0.3.6），护甲值 / 韧性故意给得很低，
 *     强度由 {@code wuling/RedstoneGear} 按「玩家境界 + 1 档」动态挂的属性修饰符托着，
 *     这样充能耗尽时加成才能被整体撤掉。
 *
 * 以下是绿宝石档的说明 —— 各项都数倍于钻石
 * （2026-09-26 用户口径「绿宝石的所有东西都比钻石强很多倍」）。
 *
 * <pre>
 *                   钻石     下界合金    绿宝石     倍数(对钻石)
 *   护甲值(头胸腿靴) 3/8/6/3  3/8/6/3   6/16/12/6    2.0×
 *   盔甲韧性          2.0       3.0       12.0        6.0×
 *   击退抗性          0.0       0.1        0.6        —
 *   耐久倍率          33        37        120        3.6×
 *   附魔度            10        15         40        4.0×
 * </pre>
 *
 * 为什么护甲值只给 2 倍而不是更高：原版减伤公式里有效护甲会被 clamp 到 20 点
 * （{@code CombatRules#getDamageAfterAbsorb}），所以「护甲值堆到 40」的意义
 * 不在面板数字，而在<b>抵抗大伤害时的削减</b> —— 高护甲 + 高韧性可以让
 * 一次 40 点重击的减伤从钻石套的 16% 提升到 80%，这才是真正的「强很多倍」。
 *
 * <b>贴图路径由 {@link #getName()} 决定</b>：原版渲染时会按
 * {@code <namespace>:textures/models/armor/<name>_layer_1.png} 去取
 * （见 {@code HumanoidArmorLayer#getArmorResource}，它会先按 {@code ':'} 切命名空间），
 * 所以名字必须写成 {@code "wulingdiguo:emerald"}，
 * 贴图放在 {@code assets/wulingdiguo/textures/models/armor/emerald_layer_1.png}。
 * 只有护腿用 {@code _layer_2}。
 *
 * 贴图由 {@code tools/gen_emerald_gear.py} 从原版钻石盔甲层染色生成，勿手改。
 */
public final class ModArmorMaterials {

    private ModArmorMaterials() {
    }

    /** 耐久倍率：胸甲 = 16 × 倍率。钻石是 33，绿宝石给 120（胸甲 1920 点） */
    private static final int DURABILITY_MULTIPLIER = 120;

    public static final ArmorMaterial EMERALD = new ArmorMaterial() {

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return switch (type) {
                case HELMET -> 11 * DURABILITY_MULTIPLIER;
                case CHESTPLATE -> 16 * DURABILITY_MULTIPLIER;
                case LEGGINGS -> 15 * DURABILITY_MULTIPLIER;
                case BOOTS -> 13 * DURABILITY_MULTIPLIER;
            };
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            // 钻石是 3 / 6 / 8 / 3 —— 这里整体翻倍
            return switch (type) {
                case HELMET -> 6;
                case CHESTPLATE -> 16;
                case LEGGINGS -> 12;
                case BOOTS -> 6;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 40;
        }

        @Override
        public SoundEvent getEquipSound() {
            // 原版没有「绿宝石」装备音，2026-10-01 用户口径「绿宝石护甲不该是钻石的声音」，
            // 改用原版里最厚重的金属装备声（下界合金），刚好也对得上绿宝石是最强境界。
            return SoundEvents.ARMOR_EQUIP_NETHERITE;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(Items.EMERALD);
        }

        @Override
        public String getName() {
            return "wulingdiguo:emerald";
        }

        @Override
        public float getToughness() {
            return 12.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.6F;
        }
    };

    /**
     * 红石：<b>充能类护甲</b>的材质档（2026-10-02）。
     *
     * <p>与 {@link ModTiers#REDSTONE} 同一个思路：材质数值故意给得很低
     * （锁链级的 2/5/4/1、韧性 0），实际强度由 {@code wuling/RedstoneGear}
     * 按「玩家武灵境界 + 1 档」动态挂属性修饰符托上去。
     *
     * <p>材质的护甲值 / 韧性无法被移除，所以只留一点底子 —— 充能耗尽时
     * 加成修饰符被撤掉，这套甲就退回「比锁链甲强一点」的水平，
     * 配合玩家身上的攻击 / 移速惩罚，体感就是「没电了」。
     *
     * <p><b>贴图</b>：{@code textures/models/armor/redstone_layer_1.png}（头/胸/靴）
     * 与 {@code _layer_2.png}（护腿），由 {@code tools/gen_redstone_gear.py} 生成。
     */
    public static final ArmorMaterial REDSTONE = new ArmorMaterial() {

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            int multiplier = 40;
            return switch (type) {
                case HELMET -> 11 * multiplier;
                case CHESTPLATE -> 16 * multiplier;
                case LEGGINGS -> 15 * multiplier;
                case BOOTS -> 13 * multiplier;
            };
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            // 锁链档：2 / 5 / 4 / 1 —— 只作底子
            return switch (type) {
                case HELMET -> 2;
                case CHESTPLATE -> 5;
                case LEGGINGS -> 4;
                case BOOTS -> 1;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 22;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_IRON;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(Items.REDSTONE);
        }

        @Override
        public String getName() {
            return "wulingdiguo:redstone";
        }

        @Override
        public float getToughness() {
            return 0.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };
}
