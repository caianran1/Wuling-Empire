package com.wuling.empire.entity;

import com.wuling.empire.WulingEmpire;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 实体注册
 */
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
}
