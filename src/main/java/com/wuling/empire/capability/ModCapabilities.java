package com.wuling.empire.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

/**
 * Capability 注册中心
 */
public final class ModCapabilities {

    private ModCapabilities() {
    }

    // ⚠️ 用经典构造函数 new ResourceLocation(ns, path)，不要用 ResourceLocation.fromNamespaceAndPath：
    // 后者是 Forge 在 1.20.1 后期（47.3.x 起）才 backport 进 MC 的，
    // 在 47.0.0 上编译不过、运行会 NoSuchMethodError。构造函数两版都在。
    public static final ResourceLocation SPIRIT_POWER_ID =
            new ResourceLocation("wulingdiguo", "spirit_power");

    public static final ResourceLocation WU_LING_ID =
            new ResourceLocation("wulingdiguo", "wu_ling");

    public static Capability<ISpiritPower> SPIRIT_POWER;
    public static Capability<IWuLing> WU_LING;

    /** 初始化（在主类构造期间调用） */
    public static void init() {
        SPIRIT_POWER = CapabilityManager.get(new CapabilityToken<>() {
        });
        WU_LING = CapabilityManager.get(new CapabilityToken<>() {
        });
    }

    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ISpiritPower.class);
        event.register(IWuLing.class);
    }
}
