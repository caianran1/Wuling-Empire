package com.wuling.empire.item;

import com.wuling.empire.Config;
import com.wuling.empire.WulingEmpire;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * 灵珠品质：凡品 / 良品 / 优品 / 上品 / 极品
 */
public enum SpiritQuality implements StringRepresentable {

    FAN("fan", "fan", 0xFF9E9E9E),
    LIANG("liang", "liang", 0xFF5CC95C),
    YOU("you", "you", 0xFF4A9BE8),
    SHANG("shang", "shang", 0xFFA855F7),
    JI("ji", "ji", 0xFFFFC94A);

    private final String key;
    private final String itemName;
    private final int color;

    SpiritQuality(String key, String suffix, int color) {
        this.key = key;
        this.itemName = "spirit_bead_" + suffix;
        this.color = color;
    }

    /** 内部名称，用于 NBT 与 config key */
    public String key() {
        return key;
    }

    /** 物品注册名 */
    public String itemName() {
        return itemName;
    }

    /** 物品模型/纹理路径使用 */
    public ResourceLocation itemTexture() {
        return new ResourceLocation(WulingEmpire.MODID, "item/" + itemName);
    }

    /** 显示颜色（ARGB，含 0xFF alpha） */
    public int color() {
        return color;
    }

    /**
     * 该品质的回复倍率。
     *
     * 实际回复量 = 怪物基准（{@link BeadPower#base}）× 本倍率，
     * 所以「回复多少灵力」同时取决于怪物种类与品质。
     */
    public float multiplier() {
        return Config.restoreMultiplier(this);
    }

    /** 掉落权重 */
    public double weight() {
        return Config.baseWeight(this);
    }

    /** 品质序号，凡品最小、极品最大 */
    public int tier() {
        return ordinal();
    }

    /** 翻译键 */
    public String translationKey() {
        return "quality.wulingdiguo." + key;
    }

    @Override
    public String getSerializedName() {
        return key;
    }

    /** 由内部名称反查，未知则回退到凡品 */
    public static SpiritQuality byKey(String key) {
        for (SpiritQuality q : values()) {
            if (q.key.equals(key)) {
                return q;
            }
        }
        return FAN;
    }
}
