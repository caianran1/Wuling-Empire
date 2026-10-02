package com.wuling.empire.entity;

import com.wuling.empire.WulingEmpire;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 实体注册
 */
@Mod.EventBusSubscriber(modid = WulingEmpire.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities {

    private ModEntities() {
    }

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, WulingEmpire.MODID);

    /**
     * 倒地尸体：怪物死亡后留在原地，右键搜刮灵珠。
     * 继承 Entity 而非 LivingEntity —— 它不需要 AI、属性、伤害系统。
     */
    public static final RegistryObject<EntityType<CorpseEntity>> SPIRIT_CORPSE =
            ENTITY_TYPES.register("spirit_corpse", () ->
                    EntityType.Builder.<CorpseEntity>of(CorpseEntity::new, MobCategory.MISC)
                            .sized(0.8F, 0.5F)
                            .clientTrackingRange(10)
                            .updateInterval(5)
                            .build("spirit_corpse"));

    /**
     * 腐肉武灵召唤出的僵尸随从。
     *
     * <p>分类刻意用 {@link MobCategory#MISC} 而不是 MONSTER：
     * 后者会挤占刷怪上限（上限 70），一次召唤几十只就会把自然刷怪掐死，
     * 玩家会以为「召唤僵尸把刷怪搞没了」。MISC 不计入刷怪上限。
     */
    public static final RegistryObject<EntityType<WuLingZombieEntity>> WU_LING_ZOMBIE =
            ENTITY_TYPES.register("wu_ling_zombie", () ->
                    EntityType.Builder.<WuLingZombieEntity>of(WuLingZombieEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build("wu_ling_zombie"));

    /** 新实体必须登记属性，否则一生成就 NPE / 崩 */
    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(WU_LING_ZOMBIE.get(), WuLingZombieEntity.createAttributes().build());
    }
}

