package com.wuling.empire.item;

import com.wuling.empire.WulingEmpire;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * 红石装备（充能类）的物品注册 —— 2026-10-02 用户口径：
 *
 * <blockquote>
 * 「用 9 个武灵红石粉可以合成红石锭，红石锭可以按照护甲合成方法合成红石战铠
 * （包括：全套护甲 + 剑斧镐铲）。每次灵力耗完使用后右键打开充能菜单用灵珠充能……
 * 穿戴红石靴子可以消耗靴子的灵力像创造那样飞行」
 * </blockquote>
 *
 * <p>共 9 件：红石锭 + 护甲四件 + 工具四件。
 *
 * <p><b>数值不写死在物品上</b>：这里用的 {@link ModTiers#REDSTONE} /
 * {@link ModArmorMaterials#REDSTONE} 都是「低配底子」（石/锁链水平），
 * 真正强度由 {@code wuling/RedstoneGear} 按「玩家武灵境界 + 1 档」动态挂属性修饰符 ——
 * 这样充能耗尽时才能把加成整体撤掉（材质自带的属性是撤不掉的）。
 *
 * <p>工具参数照抄原版同形的低档物品，保证「基础值 = 原版同款」这个口径成立，
 * 差额一律由修饰符补。
 */
public final class RedstoneItems {

    private RedstoneItems() {
    }

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WulingEmpire.MODID);

    /** 红石锭：9 个武灵红石粉合成 */
    public static final RegistryObject<Item> REDSTONE_INGOT =
            ITEMS.register("redstone_ingot", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> REDSTONE_HELMET =
            ITEMS.register("redstone_helmet",
                    () -> new ArmorItem(ModArmorMaterials.REDSTONE, ArmorItem.Type.HELMET,
                            new Item.Properties()));

    public static final RegistryObject<Item> REDSTONE_CHESTPLATE =
            ITEMS.register("redstone_chestplate",
                    () -> new ArmorItem(ModArmorMaterials.REDSTONE, ArmorItem.Type.CHESTPLATE,
                            new Item.Properties()));

    public static final RegistryObject<Item> REDSTONE_LEGGINGS =
            ITEMS.register("redstone_leggings",
                    () -> new ArmorItem(ModArmorMaterials.REDSTONE, ArmorItem.Type.LEGGINGS,
                            new Item.Properties()));

    public static final RegistryObject<Item> REDSTONE_BOOTS =
            ITEMS.register("redstone_boots",
                    () -> new ArmorItem(ModArmorMaterials.REDSTONE, ArmorItem.Type.BOOTS,
                            new Item.Properties()));

    /** 剑：原版剑的修正值 3 / 速度 -2.4 */
    public static final RegistryObject<Item> REDSTONE_SWORD =
            ITEMS.register("redstone_sword",
                    () -> new SwordItem(ModTiers.REDSTONE, 3, -2.4F, new Item.Properties()));

    /** 斧：修正值 6（对齐木 / 黄金档的原版斧），速度 -3.0 */
    public static final RegistryObject<Item> REDSTONE_AXE =
            ITEMS.register("redstone_axe",
                    () -> new AxeItem(ModTiers.REDSTONE, 6.0F, -3.0F, new Item.Properties()));

    public static final RegistryObject<Item> REDSTONE_PICKAXE =
            ITEMS.register("redstone_pickaxe",
                    () -> new PickaxeItem(ModTiers.REDSTONE, 1, -2.8F, new Item.Properties()));

    public static final RegistryObject<Item> REDSTONE_SHOVEL =
            ITEMS.register("redstone_shovel",
                    () -> new ShovelItem(ModTiers.REDSTONE, 1.5F, -3.0F, new Item.Properties()));

    /** 护甲四件，顺序与 {@link ManifestItems#armorIndex} 一致：头 / 胸 / 腿 / 靴 */
    public static final List<RegistryObject<Item>> ARMOR = List.of(
            REDSTONE_HELMET, REDSTONE_CHESTPLATE, REDSTONE_LEGGINGS, REDSTONE_BOOTS);

    /** 四件武器 / 工具 */
    public static final List<RegistryObject<Item>> TOOLS = List.of(
            REDSTONE_SWORD, REDSTONE_AXE, REDSTONE_PICKAXE, REDSTONE_SHOVEL);

    /** 八件红石战铠（不含红石锭） */
    public static final List<RegistryObject<Item>> GEAR = List.of(
            REDSTONE_HELMET, REDSTONE_CHESTPLATE, REDSTONE_LEGGINGS, REDSTONE_BOOTS,
            REDSTONE_SWORD, REDSTONE_AXE, REDSTONE_PICKAXE, REDSTONE_SHOVEL);

    // ===================== 判定 =====================

    /** 是不是红石装备（护甲或工具 / 武器，不含红石锭） */
    public static boolean isGear(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        for (RegistryObject<Item> gear : GEAR) {
            if (item == gear.get()) {
                return true;
            }
        }
        return false;
    }

    /** 是不是红石护甲 */
    public static boolean isArmor(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        for (RegistryObject<Item> piece : ARMOR) {
            if (item == piece.get()) {
                return true;
            }
        }
        return false;
    }

    /** 是不是红石靴子（飞行用） */
    public static boolean isBoots(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == REDSTONE_BOOTS.get();
    }
}
