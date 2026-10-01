package com.wuling.empire.item;

import com.wuling.empire.WulingEmpire;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

/**
 * 物品注册（灵珠 / 绑定器）。
 *
 * <p>武灵凝聚物（剑 / 斧 / 镐 / 铲 / 弓 / 水 / 火 / 红石 / 书 / 护甲四件套）
 * 不在这里 —— 它们是「每个大境界一件独立物品」，共 91 件，
 * 统一注册在 {@link ManifestItems}。绿宝石档的 id（{@code emerald_sword} 等）
 * 与 0.2.x 保持一致，只是搬了家。
 */
public final class ModItems {

    private ModItems() {
    }

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WulingEmpire.MODID);

    /** 品质 -> 对应灵珠物品 */
    public static final Map<SpiritQuality, RegistryObject<Item>> BEADS =
            new EnumMap<>(SpiritQuality.class);

    /** 武灵绑定器：仅限创造模式取得 */
    public static final RegistryObject<Item> WU_LING_BINDER =
            ITEMS.register("wu_ling_binder", WuLingBinderItem::new);

    /** 创造武灵绑定器：创造模式直接切换武灵，无视「已拥有」限制 */
    public static final RegistryObject<Item> CREATIVE_WU_LING_BINDER =
            ITEMS.register("creative_wu_ling_binder", CreativeWuLingBinderItem::new);

    static {
        for (SpiritQuality quality : SpiritQuality.values()) {
            BEADS.put(quality, ITEMS.register(quality.itemName(),
                    () -> new SpiritBeadItem(quality)));
        }
    }
}
