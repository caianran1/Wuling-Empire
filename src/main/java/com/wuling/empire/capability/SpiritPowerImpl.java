package com.wuling.empire.capability;

import com.wuling.empire.Config;
import net.minecraft.nbt.CompoundTag;

/**
 * 灵力数据默认实现。
 *
 * 回复规则：灵力不足时，以「每分钟 Config.regenPerMinute%」的速度恢复。
 * 1 分钟 = 1200 tick，所以每 tick 回复 regenPerMinute / 1200。
 */
public class SpiritPowerImpl implements ISpiritPower {

    private static final String KEY_SPIRIT = "spirit";

    private float spirit;
    private float carry;

    public SpiritPowerImpl() {
        this(Config.maxSpirit());
    }

    public SpiritPowerImpl(float spirit) {
        this.spirit = clamp(spirit);
    }

    private float clamp(float value) {
        return Math.max(0.0F, Math.min(Config.maxSpirit(), value));
    }

    @Override
    public float getSpirit() {
        return spirit;
    }

    @Override
    public void setSpirit(float value) {
        this.spirit = clamp(value);
    }

    @Override
    public void addSpirit(float delta) {
        this.spirit = clamp(this.spirit + delta);
    }

    @Override
    public boolean isFull() {
        return spirit >= Config.maxSpirit() - 0.001F;
    }

    @Override
    public void tick() {
        float max = Config.maxSpirit();
        if (spirit >= max) {
            spirit = max;
            return;
        }
        // 每 tick 的回复量很小（默认 1/1200 ≈ 0.00083），用累加器避免浮点精度丢失
        carry += (float) (Config.REGEN_PER_MINUTE.get() / 1200.0D);
        if (carry >= 0.005F) {
            this.spirit = clamp(this.spirit + carry);
            carry = 0.0F;
        }
    }

    @Override
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(KEY_SPIRIT, spirit);
        return tag;
    }

    @Override
    public void deserialize(CompoundTag tag) {
        if (tag.contains(KEY_SPIRIT)) {
            this.spirit = clamp(tag.getFloat(KEY_SPIRIT));
        }
    }
}
