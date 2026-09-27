package com.wuling.empire.client;

import com.wuling.empire.WulingEmpire;
import com.wuling.empire.entity.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端 MOD 总线事件：渲染器注册
 */
@Mod.EventBusSubscriber(modid = WulingEmpire.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModBusEvents {

    private ClientModBusEvents() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SPIRIT_CORPSE.get(), CorpseRenderer::new);
    }
}
