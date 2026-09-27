package com.wuling.empire.item;

import com.wuling.empire.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

import java.util.Map;

/**
 * 灵珠灵力回复量表 —— <b>BASE 表由 tools/gen_bead_textures.py 生成，请勿手改</b>。
 *
 * <p>回复量分两层：
 * <ol>
 *   <li><b>怪物基准</b>（本表）= 该怪物「凡品」灵珠回复的百分比，强怪给得多；</li>
 *   <li><b>品质倍率</b>（{@link Config#restoreMultiplier}）= 凡 1.0 / 良 1.5 / 优 2.0 / 上 3.0 / 极 4.0。</li>
 * </ol>
 * 最终回复量 = {@code base(来源) × quality.multiplier()}。
 *
 * <p>两个锚点：<b>僵尸凡品 5%</b>、<b>末影龙极品 100%</b>（基准 25 × 极品倍率 4）。
 * 已退役的怪物（蠹虫 / 末影螨 / 恼鬼）不在表内。
 *
 * <p>表里没有的来源（模组怪物）按最大生命值兜底，见 {@link #fallbackBase}，
 * 所以加装其它模组也不会出现「一颗珠子回 0%」。
 *
 * <p>要调数值：改 {@code tools/gen_bead_textures.py} 的 {@code MOB_RESTORE} 后重跑脚本。
 */
public final class BeadPower {

    private BeadPower() {
    }

    /** 各怪物的基准回复量（%），即该怪物凡品灵珠的回复量 */
    public static final Map<String, Float> BASE = Map.ofEntries(
            Map.entry("minecraft:blaze", 8F),
            Map.entry("minecraft:cave_spider", 4.5F),
            Map.entry("minecraft:creeper", 6F),
            Map.entry("minecraft:drowned", 5.5F),
            Map.entry("minecraft:elder_guardian", 15F),
            Map.entry("minecraft:ender_dragon", 25F),
            Map.entry("minecraft:enderman", 9F),
            Map.entry("minecraft:evoker", 12F),
            Map.entry("minecraft:ghast", 6F),
            Map.entry("minecraft:giant", 22F),
            Map.entry("minecraft:guardian", 7F),
            Map.entry("minecraft:hoglin", 9F),
            Map.entry("minecraft:husk", 5F),
            Map.entry("minecraft:illusioner", 12F),
            Map.entry("minecraft:magma_cube", 4.5F),
            Map.entry("minecraft:phantom", 6F),
            Map.entry("minecraft:piglin", 5F),
            Map.entry("minecraft:piglin_brute", 13F),
            Map.entry("minecraft:pillager", 7F),
            Map.entry("minecraft:ravager", 17F),
            Map.entry("minecraft:shulker", 9F),
            Map.entry("minecraft:skeleton", 5F),
            Map.entry("minecraft:slime", 4F),
            Map.entry("minecraft:stray", 5.5F),
            Map.entry("minecraft:vindicator", 8F),
            Map.entry("minecraft:warden", 25F),
            Map.entry("minecraft:witch", 8F),
            Map.entry("minecraft:wither", 25F),
            Map.entry("minecraft:wither_skeleton", 8F),
            Map.entry("minecraft:zoglin", 10F),
            Map.entry("minecraft:zombie", 5F),
            Map.entry("minecraft:zombie_villager", 4.5F),
            Map.entry("minecraft:zombified_piglin", 5F)
    );

    /** 表外来源的兜底锚点：20 血 = 5% */
    private static final float HP_ANCHOR = 20.0F;
    private static final float HP_ANCHOR_VALUE = 5.0F;
    /** 每多 1 点最大生命值，基准增加 1/9（=> 200 血的末影龙正好 25） */
    private static final float HP_STEP = 1.0F / 9.0F;
    private static final float FALLBACK_MIN = 3.0F;
    private static final float FALLBACK_MAX = 25.0F;

    /** 连实体类型都取不到时（空来源 / 未注册 ID）的兜底值 */
    public static final float DEFAULT_BASE = 5.0F;

    /** 该来源的基准回复量（凡品值，%） */
    public static float base(String source) {
        if (source == null || source.isEmpty()) {
            return DEFAULT_BASE;
        }
        Float fixed = BASE.get(source);
        if (fixed != null) {
            return fixed;
        }
        float hp = maxHealth(source);
        return hp <= 0.0F ? DEFAULT_BASE : fallbackBase(hp);
    }

    /** 表外来源（模组怪物）的兜底算法：按最大生命值线性给值，钳制在 [3, 25] */
    public static float fallbackBase(float maxHealth) {
        float v = HP_ANCHOR_VALUE + (maxHealth - HP_ANCHOR) * HP_STEP;
        return Math.max(FALLBACK_MIN, Math.min(FALLBACK_MAX, v));
    }

    /** 该来源某品质灵珠的回复量（%） */
    public static float restore(String source, SpiritQuality quality) {
        return base(source) * quality.multiplier();
    }

    /** 取该来源的最大生命值；无法确定时返回 0（非生物 / 未注册 / 没有属性表） */
    @SuppressWarnings("unchecked")
    public static float maxHealth(String source) {
        ResourceLocation id = ResourceLocation.tryParse(source);
        if (id == null) {
            return 0.0F;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == null || !DefaultAttributes.hasSupplier(type)) {
            return 0.0F;
        }
        AttributeSupplier supplier =
                DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
        return (float) supplier.getValue(Attributes.MAX_HEALTH);
    }
}
