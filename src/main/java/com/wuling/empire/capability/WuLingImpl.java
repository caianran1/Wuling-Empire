package com.wuling.empire.capability;

import com.wuling.empire.wuling.CultivationAction;
import com.wuling.empire.wuling.WuLingData;
import net.minecraft.nbt.CompoundTag;

/**
 * 武灵 Capability 实现。
 *
 * 只有匹配当前武灵种类的动作才会累积进度 —— 对应原文
 * 「依靠使用武灵的自然修炼进度」：你得真的在用你的武灵。
 */
public class WuLingImpl implements IWuLing {

    private final WuLingData data = new WuLingData();

    @Override
    public WuLingData data() {
        return data;
    }

    @Override
    public boolean addProgress(double amount, CultivationAction action) {
        if (!data.isBound()) {
            return false;
        }
        if (!data.type().actions().contains(action)) {
            return false;
        }
        return data.addProgress(amount);
    }

    @Override
    public CompoundTag serialize() {
        return data.serialize();
    }

    @Override
    public void deserialize(CompoundTag tag) {
        data.deserialize(tag);
    }
}
