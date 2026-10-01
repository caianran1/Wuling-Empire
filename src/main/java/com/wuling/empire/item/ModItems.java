package com.wuling.empire.item;

import com.wuling.empire.WulingEmpire;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

/**
 * 物品注册
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

    /** 绿宝石武灵剑：绿宝石境界的剑武灵单独设计的实物（钻石剑 + 绿色滤镜） */
    public static final RegistryObject<Item> EMERALD_SWORD =
            ITEMS.register("emerald_sword", EmeraldSwordItem::new);

    // ===================== 绿宝石境界的其它凝聚物 =====================
    // 「其他全做」—— 外观一律是原版钻石同款 + 绿色滤镜（贴图由
    // tools/gen_emerald_gear.py 生成），数值一律走 ModTiers.EMERALD / ModArmorMaterials.EMERALD，
    // 都是钻石的数倍（2026-09-26 用户口径「绿宝石的所有东西都比钻石强很多倍」）。
    // 斧 / 镐 / 铲 / 胸甲没有额外逻辑，就地注册即可。

    /** 绿宝石斧 */
    public static final RegistryObject<Item> EMERALD_AXE =
            ITEMS.register("emerald_axe", () -> new AxeItem(
                    ModTiers.EMERALD, 5.0F, -3.0F, new Item.Properties()));

    /** 绿宝石镐 */
    public static final RegistryObject<Item> EMERALD_PICKAXE =
            ITEMS.register("emerald_pickaxe", () -> new PickaxeItem(
                    ModTiers.EMERALD, 1, -2.8F, new Item.Properties()));

    /** 绿宝石铲 */
    public static final RegistryObject<Item> EMERALD_SHOVEL =
            ITEMS.register("emerald_shovel", () -> new ShovelItem(
                    ModTiers.EMERALD, 1.5F, -3.0F, new Item.Properties()));

    // 绿宝石护甲：**整套四件**（2026-10-01 用户口径「护甲是全套护甲不是胸甲」）。
    // 盔甲武灵凝聚时一次给出头盔 / 胸甲 / 护腿 / 靴子，四件共用同一材质，
    // 头部与胸甲、靴子走 emerald_layer_1 贴图，护腿走 emerald_layer_2。

    /** 绿宝石头盔 */
    public static final RegistryObject<Item> EMERALD_HELMET =
            ITEMS.register("emerald_helmet", () -> new ArmorItem(
                    ModArmorMaterials.EMERALD, ArmorItem.Type.HELMET, new Item.Properties()));

    /** 绿宝石胸甲 */
    public static final RegistryObject<Item> EMERALD_CHESTPLATE =
            ITEMS.register("emerald_chestplate", () -> new ArmorItem(
                    ModArmorMaterials.EMERALD, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    /** 绿宝石护腿 */
    public static final RegistryObject<Item> EMERALD_LEGGINGS =
            ITEMS.register("emerald_leggings", () -> new ArmorItem(
                    ModArmorMaterials.EMERALD, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    /** 绿宝石靴子 */
    public static final RegistryObject<Item> EMERALD_BOOTS =
            ITEMS.register("emerald_boots", () -> new ArmorItem(
                    ModArmorMaterials.EMERALD, ArmorItem.Type.BOOTS, new Item.Properties()));

    static {
        for (SpiritQuality quality : SpiritQuality.values()) {
            BEADS.put(quality, ITEMS.register(quality.itemName(),
                    () -> new SpiritBeadItem(quality)));
        }
    }
}
