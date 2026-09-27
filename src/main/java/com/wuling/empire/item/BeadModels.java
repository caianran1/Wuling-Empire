package com.wuling.empire.item;

import java.util.List;

/**
 * 灵珠外观索引表 —— <b>由 tools/gen_bead_textures.py 生成，请勿手改</b>。
 *
 * 每只怪物一张专属灵珠图（不再按品质分图），选图靠物品 NBT 的
 * `CustomModelData`：值 = 下表序号（从 1 开始），
 * 对应 models/item/spirit_bead_*.json 里的 overrides 条目。
 * 没有专属图的来源（例如其它模组的怪物、以及已退役的怪物）不写该 NBT，退回品质图。
 *
 * 表里可能出现「空号」（如蠹虫）：那是不出灵珠后留下的占位，
 * 保留它是为了让其它怪物的序号不因退役而整体前移（否则历史灵珠会显示错图）。
 *
 * 重新生成：python tools/gen_bead_textures.py
 */
public final class BeadModels {

    private BeadModels() {
    }

    /** 有专属灵珠图的怪物来源（顺序即 CustomModelData，空号为已退役怪物） */
    public static final List<String> SOURCES = List.of(
            "minecraft:blaze",
            "minecraft:cave_spider",
            "minecraft:creeper",
            "minecraft:drowned",
            "minecraft:elder_guardian",
            "minecraft:ender_dragon",
            "minecraft:enderman",
            "minecraft:endermite",  // 已退役（不掉灵珠），仅占位
            "minecraft:evoker",
            "minecraft:ghast",
            "minecraft:giant",
            "minecraft:guardian",
            "minecraft:hoglin",
            "minecraft:husk",
            "minecraft:illusioner",
            "minecraft:magma_cube",
            "minecraft:phantom",
            "minecraft:piglin",
            "minecraft:piglin_brute",
            "minecraft:pillager",
            "minecraft:ravager",
            "minecraft:shulker",
            "minecraft:silverfish",  // 已退役（不掉灵珠），仅占位
            "minecraft:skeleton",
            "minecraft:slime",
            "minecraft:stray",
            "minecraft:vex",  // 已退役（不掉灵珠），仅占位
            "minecraft:vindicator",
            "minecraft:warden",
            "minecraft:witch",
            "minecraft:wither",
            "minecraft:wither_skeleton",
            "minecraft:zoglin",
            "minecraft:zombie",
            "minecraft:zombie_villager",
            "minecraft:zombified_piglin"
    );

    /**
     * 已退役的来源：不出灵珠，取图时直接返回 -1（退回品质图）。
     * 它们在 SOURCES 里仍占着号，只是为了让其它怪物的序号不整体前移。
     */
    public static final List<String> RETIRED = List.of(
            "minecraft:endermite",
            "minecraft:silverfish",
            "minecraft:vex"
    );

    /** 取该来源对应的 CustomModelData；没有专属图返回 -1 */
    public static int modelIndex(String source) {
        if (source == null || RETIRED.contains(source)) {
            return -1;
        }
        int i = SOURCES.indexOf(source);
        return i < 0 ? -1 : i + 1;
    }
}
