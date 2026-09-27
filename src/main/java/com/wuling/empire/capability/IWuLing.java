package com.wuling.empire.capability;

import com.wuling.empire.wuling.CultivationAction;
import com.wuling.empire.wuling.WuLingData;
import net.minecraft.nbt.CompoundTag;

/**
 * 玩家武灵数据 Capability。
 */
public interface IWuLing {

    WuLingData data();

    /**
     * 累积修炼进度。
     *
     * @return 本次是否发生了小境界晋升
     */
    boolean addProgress(double amount, CultivationAction action);

    CompoundTag serialize();

    void deserialize(CompoundTag tag);
}
