package com.wuling.empire.capability;

/**
 * 玩家灵力数据接口（百分制）。
 */
public interface ISpiritPower {

    /** 当前灵力值，范围 [0, max] */
    float getSpirit();

    /** 直接设置灵力值（自动夹紧） */
    void setSpirit(float value);

    /** 增量修改灵力（可为负，自动夹紧） */
    void addSpirit(float delta);

    /** 是否处于满灵力状态 */
    boolean isFull();

    /** 每 tick 的自然回复 */
    void tick();

    /** 序列化，用于跨维度 / 死亡继承 */
    net.minecraft.nbt.CompoundTag serialize();

    void deserialize(net.minecraft.nbt.CompoundTag tag);
}
