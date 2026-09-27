package com.wuling.empire.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;

/**
 * 绿宝石武灵剑。
 *
 * 原文：绿宝石境界的剑是<b>单独设计</b>的一件武器，而不是「下界合金剑换个名字」，
 * 所以这里注册成一个独立物品 ——
 *
 * <ul>
 *   <li>外观：钻石剑贴图 + 绿色滤镜（刃部由青染成绿宝石绿，木柄保留）。
 *       贴图由 {@code tools/gen_emerald_gear.py} 从原版资源包生成，勿手改。</li>
 *   <li>数值：{@link ModTiers#EMERALD}，耐久 / 挖掘速度 / 伤害 / 附魔度都是<b>钻石的数倍</b>
 *       （2026-09-26 用户口径「绿宝石的所有东西都比钻石强很多倍」）。</li>
 * </ul>
 *
 * 它和别的凝聚物一样，之后还会被
 * {@link com.wuling.empire.wuling.WuLingType} 的强化逻辑再放大一次
 * （{@code manifestAttackBonus} 默认 100%，再乘绿宝石境界的 15 倍总倍数，
 * 见 {@code WuLingRealm#manifestMultiplier}，最终约 240 伤害 ——
 * 已远超一击秒杀满血末影龙的量级）。
 *
 * 斧 / 镐 / 铲 / 胸甲不需要额外的物品逻辑，直接在 {@link ModItems} 里就地注册；
 * 只有剑留着这个类，方便后续接原文第五部的「一击秒杀末影龙」。
 */
public class EmeraldSwordItem extends SwordItem {

    public EmeraldSwordItem() {
        // 3 = 剑的基础伤害档，-2.4 = 原版剑的攻击速度
        super(ModTiers.EMERALD, 3, -2.4F, new Item.Properties());
    }
}
