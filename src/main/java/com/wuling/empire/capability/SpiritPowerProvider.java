package com.wuling.empire.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 灵力 Capability 提供者，附着到玩家实体上。
 */
public class SpiritPowerProvider implements ICapabilitySerializable<CompoundTag> {

    private final SpiritPowerImpl impl = new SpiritPowerImpl();
    private final LazyOptional<ISpiritPower> holder = LazyOptional.of(() -> impl);

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        return capability == ModCapabilities.SPIRIT_POWER ? holder.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return impl.serialize();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        impl.deserialize(tag);
    }

    /** 服务端：强制直接取得实现，跳过 LazyOptional */
    public SpiritPowerImpl impl() {
        return impl;
    }
}
