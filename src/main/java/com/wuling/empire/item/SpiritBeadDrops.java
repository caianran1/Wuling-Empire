package com.wuling.empire.item;

import com.wuling.empire.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 灵珠掉落。
 *
 * <p><b>2026-10-02 用户口径：「怪物掉落灵珠改为直接掉落而不是尸体」。</b>
 * 以前是「怪物死亡 → 原地留一具妖兽尸体 → 玩家右键搜刮」，
 * 现在普通敌对生物死亡时就地掉一颗灵珠（立刻就捡得起来），不再留尸体。
 *
 * <p><b>唯一的例外是末影龙</b>：它仍走专属尸体那条路（大命中箱 + 紫色光柱地标，
 * 取珠后尸体不消失）—— 用户 2026-10-02 明确选择保留。
 *
 * <p>掉落规则不变：一只怪只掉 <b>1 颗</b>，怪物强度只影响<b>品质</b>、不影响数量
 * （{@link Config#STRENGTH_INFLUENCE}）。
 */
public final class SpiritBeadDrops {

    private SpiritBeadDrops() {
    }

    /** 末影龙灵珠的来源 ID（与 {@code CorpseEntity} 里的写法保持一致） */
    public static final String ENDER_DRAGON_SOURCE = "minecraft:ender_dragon";

    /**
     * 按怪物强度掷一颗灵珠，并记好来源 —— 这仍是第二部「灵珠种类决定修炼方向」
     * 与第四部「极品 XX 灵珠」分物种定价的依据。
     */
    public static ItemStack roll(LivingEntity dead) {
        ItemStack bead = new ItemStack(ModItems.BEADS.get(randomQuality(dead)).get());
        ResourceLocation sourceId = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType());
        SpiritBeadItem.setSource(bead, sourceId == null ? "" : sourceId.toString());
        return bead;
    }

    /** 末影龙的专属灵珠：固定极品 */
    public static ItemStack dragonBead() {
        ItemStack bead = new ItemStack(ModItems.BEADS.get(SpiritQuality.JI).get());
        SpiritBeadItem.setSource(bead, ENDER_DRAGON_SOURCE);
        return bead;
    }

    /**
     * 把灵珠掉在怪物死亡处。
     *
     * <p>用户 2026-10-02 选择「立刻可捡」：掉落瞬间就能捡起，
     * 手感接近原来的右键取珠，不会被 0.5 秒拾取延迟卡一下。
     */
    public static void dropAt(Level level, LivingEntity dead, ItemStack bead) {
        if (bead.isEmpty()) {
            return;
        }
        ItemEntity item = new ItemEntity(level, dead.getX(),
                dead.getY() + 0.3D, dead.getZ(), bead);
        item.setNoPickUpDelay();
        RandomSource random = level.getRandom();
        item.setDeltaMovement(random.nextGaussian() * 0.04D, 0.12D + random.nextDouble() * 0.06D,
                random.nextGaussian() * 0.04D);
        level.addFreshEntity(item);
    }

    /**
     * 品质随机：权重 = 基础权重 × 强度系数^品质序号。
     * 强度系数由怪物最大生命值推定，越硬的怪越容易出高品质灵珠。
     * 这是「一个怪的实力差体现在品质而非数量」的落地。
     */
    private static SpiritQuality randomQuality(LivingEntity dead) {
        double influence = Config.STRENGTH_INFLUENCE.get();
        double hp = dead.getMaxHealth();
        double base = Math.max(0.05D, 1.0D + ((hp - 20.0D) / 40.0D) * influence);

        SpiritQuality[] values = SpiritQuality.values();
        double[] weights = new double[values.length];
        double total = 0.0D;
        for (int i = 0; i < values.length; i++) {
            double w = Math.max(0.0D, values[i].weight() * Math.pow(base, values[i].tier()));
            weights[i] = w;
            total += w;
        }
        if (total <= 0.0D) {
            return SpiritQuality.FAN;
        }

        double roll = dead.getRandom().nextDouble() * total;
        double acc = 0.0D;
        for (int i = 0; i < values.length; i++) {
            acc += weights[i];
            if (roll < acc) {
                return values[i];
            }
        }
        return SpiritQuality.FAN;
    }
}
