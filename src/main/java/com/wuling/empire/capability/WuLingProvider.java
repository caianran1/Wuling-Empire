package com.wuling.empire.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 武灵 Capability 提供者，附着到玩家实体。
 */
public class WuLingProvider implements ICapabilitySerializable<CompoundTag> {

    private final WuLingImpl impl = new WuLingImpl();
    private final LazyOptional<IWuLing> holder = LazyOptional.of(() -> impl);

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        return capability == ModCapabilities.WU_LING ? holder.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return impl.serialize();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        impl.deserialize(tag);
    }
}
