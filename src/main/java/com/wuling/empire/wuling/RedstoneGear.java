package com.wuling.empire.wuling;

import com.google.common.collect.Multimap;
import com.wuling.empire.Config;
import com.wuling.empire.item.ManifestItems;
import com.wuling.empire.item.RedstoneItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 红石装备的核心逻辑 —— 充能类装备（2026-10-02 用户设定）。
 *
 * <h3>用户口径</h3>
 * <blockquote>
 * 「用 9 个武灵红石粉可以合成红石锭，红石锭可以按照护甲合成方法合成红石战铠
 * （包括：全套护甲 + 剑斧镐铲）。每次灵力（护具或武器自带的所有武器灵力共享）耗完
 * 使用后右键打开充能菜单用灵珠充能，充能数按灵珠种类以及品质决定，
 * 穿戴红石靴子可以消耗靴子的灵力像创造那样飞行」<br>
 * 追问后补充：消耗发生在<b>攻击命中 / 挖掘方块 / 挨打受击 / 飞行</b>；
 * 耗尽的后果是「加成失效 + 不能飞 + 攻击大幅下降 + 速度减缓」，<b>不要药水效果</b>；
 * 强度按「灵师（玩家）境界 + 1 档」算，封顶绿宝石档。
 * </blockquote>
 *
 * <h3>为什么强度不写在材质上</h3>
 * 红石装备的材质档（{@link com.wuling.empire.item.ModTiers#REDSTONE} /
 * {@link com.wuling.empire.item.ModArmorMaterials#REDSTONE}）故意给得很低
 * （石 / 锁链水平），真正强度靠这里按「境界 + 1 档」动态挂上去的<b>属性修饰符</b>。
 *
 * <p>原因是「耗尽时加成要失效」：材质自带的属性（{@code Item#getDefaultAttributeModifiers}）
 * 无法移除，而 NBT 上的修饰符可以随手增删。所以基础给弱材质、强度全靠可增可减的修饰符托着，
 * 「没电了」才能真的变回废物。
 *
 * <h3>目标数值从哪来</h3>
 * 直接<b>构造那一档的武灵凝聚物</b>（{@link WuLingType#manifestPiece}）并读它的属性总和 ——
 * 这样「+1 档」拿到的就是「那个档的武灵装备」的数值，不必再抄一遍
 * {@code 原版同款 × 境界倍数} 的算法，也不会与凝聚物产生偏差。
 * 档位只有 7 档 × 8 件，结果全部缓存。
 */
public final class RedstoneGear {

    private RedstoneGear() {
    }

    /**
     * 物品 NBT：这一件已经按哪一档加成过。
     *
     * <p>{@code -1} = 当前没有任何加成（充能耗尽或尚未应用），
     * {@code 0..6} = 已按该大境界档位加成。刷新时拿它和目标档位比，一致就不动 NBT，
     * 避免每 tick 重写属性导致玩家属性被反复重算。
     */
    public static final String TAG_TIER = "RedstoneTier";

    /** 我们自己挂的修饰符名字，用来在重写前把旧的清掉 */
    private static final String MODIFIER_NAME = "WuLing redstone";

    private static UUID uuid(String name) {
        return UUID.nameUUIDFromBytes(("wulingdiguo:redstone:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private static final UUID[] UUID_ARMOR = {
            uuid("armor_0"), uuid("armor_1"), uuid("armor_2"), uuid("armor_3")};
    private static final UUID[] UUID_TOUGHNESS = {
            uuid("toughness_0"), uuid("toughness_1"), uuid("toughness_2"), uuid("toughness_3")};
    private static final UUID UUID_WEAPON_MAINHAND = uuid("weapon_main");
    private static final UUID UUID_WEAPON_OFFHAND = uuid("weapon_off");

    /** 充能耗尽时挂在玩家身上的两条惩罚（临时修饰符，不会写进玩家 NBT） */
    private static final UUID UUID_EMPTY_ATTACK = uuid("empty_attack");
    private static final UUID UUID_EMPTY_SPEED = uuid("empty_speed");

    /** 目标数值缓存：档位 × 件数 只有几十种组合，算一次就够 */
    private static final Map<String, Double> ATTACK_CACHE = new HashMap<>();
    private static final Map<String, double[]> ARMOR_CACHE = new HashMap<>();

    // ===================== 档位 =====================

    /**
     * 目标档位 = 玩家当前大境界 + 1，<b>封顶绿宝石档</b>（2026-10-02 用户口径）。
     *
     * <p>所以木境界的红石装备 = 石档强度，钻石境界 = 下界合金档强度，
     * 绿宝石境界维持绿宝石档（不再往上涨）。未开启武灵时按木境界算（→ 石档）。
     */
    public static WuLingRealm targetRealm(WuLingData data) {
        int ordinal = Math.min(data.realmOrdinal() + 1, WuLingRealm.EMERALD.ordinal());
        return WuLingRealm.byOrdinal(Math.max(0, ordinal));
    }

    // ===================== 目标数值 =====================

    /** 该档某类武器的「最终攻击力」（玩家基础 1 + 物品修正 + 该档 Tier 加成，再乘境界倍数） */
    public static double weaponAttack(WuLingRealm realm, WuLingType type) {
        String key = realm.name() + ":" + type.name();
        Double cached = ATTACK_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        ItemStack reference = type.manifestPiece(realm.ordinal(), 0, -1);
        double value = sumAttribute(reference, Attributes.ATTACK_DAMAGE);
        ATTACK_CACHE.put(key, value);
        return value;
    }

    /** 该档某部位护甲的 {@code [护甲值, 盔甲韧性]} */
    public static double[] armorValues(WuLingRealm realm, ArmorItem.Type type) {
        String key = realm.name() + ":" + type.name();
        double[] cached = ARMOR_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        ItemStack reference = WuLingType.ARMOR.manifestPiece(realm.ordinal(), 0,
                ManifestItems.armorIndex(type));
        double[] values = {
                sumAttribute(reference, Attributes.ARMOR),
                sumAttribute(reference, Attributes.ARMOR_TOUGHNESS)};
        ARMOR_CACHE.put(key, values);
        return values;
    }

    // ===================== 刷新装备属性 =====================

    /**
     * 按「玩家境界 +1 档」与充能状态，刷新玩家身上所有红石装备的属性。
     *
     * <p>幂等：档位与上次一致时整段跳过，所以每 tick 调也只会写 NBT 一次（状态真的变了那次）。
     *
     * @param charge 当前充能；{@code <= 0} 表示耗尽，只保留材质自身的低配底子
     */
    public static void refresh(Player player, WuLingData data, int charge) {
        WuLingRealm target = targetRealm(data);
        boolean powered = charge > 0;

        applyTo(player.getMainHandItem(), target, powered);
        applyTo(player.getOffhandItem(), target, powered);
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            applyTo(player.getItemBySlot(slot), target, powered);
        }
    }

    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private static void applyTo(ItemStack stack, WuLingRealm target, boolean powered) {
        if (!RedstoneItems.isGear(stack)) {
            return;
        }
        int want = powered ? target.ordinal() : -1;
        if (appliedTier(stack) == want) {
            return;
        }
        EquipmentSlot slot = LivingEntity.getEquipmentSlotForItem(stack);
        stripOurModifiers(stack);

        if (want >= 0) {
            WuLingType weapon = weaponTypeOf(stack);
            if (weapon != null) {
                double base = baseAttribute(stack, Attributes.ATTACK_DAMAGE, slot);
                addModifier(stack, Attributes.ATTACK_DAMAGE, slot, uuidForWeapon(slot),
                        weaponAttack(target, weapon) - base);
            } else if (stack.getItem() instanceof ArmorItem armor) {
                int index = ManifestItems.armorIndex(armor.getType());
                double[] values = armorValues(target, armor.getType());
                addModifier(stack, Attributes.ARMOR, slot, UUID_ARMOR[index],
                        values[0] - baseAttribute(stack, Attributes.ARMOR, slot));
                addModifier(stack, Attributes.ARMOR_TOUGHNESS, slot, UUID_TOUGHNESS[index],
                        values[1] - baseAttribute(stack, Attributes.ARMOR_TOUGHNESS, slot));
            }
        }
        stack.getOrCreateTag().putInt(TAG_TIER, want);
    }

    /** 这一件已经按哪一档加成过；没记录过返回 {@link Integer#MIN_VALUE} 表示「必须重算一次」 */
    public static int appliedTier(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_TIER, Tag.TAG_INT)) {
            return Integer.MIN_VALUE;
        }
        return tag.getInt(TAG_TIER);
    }

    /** 是不是红石武器（剑 / 斧 / 镐 / 铲） */
    private static WuLingType weaponTypeOf(ItemStack stack) {
        Item item = stack.getItem();
        if (item == RedstoneItems.REDSTONE_SWORD.get()) {
            return WuLingType.SWORD;
        }
        if (item == RedstoneItems.REDSTONE_AXE.get()) {
            return WuLingType.AXE;
        }
        if (item == RedstoneItems.REDSTONE_PICKAXE.get()) {
            return WuLingType.PICKAXE;
        }
        if (item == RedstoneItems.REDSTONE_SHOVEL.get()) {
            return WuLingType.SHOVEL;
        }
        return null;
    }

    /** 红石武器是「武器」还是「工具」：斧 / 镐 / 铲也能挖方块 */
    public static boolean isWeapon(ItemStack stack) {
        return RedstoneItems.isGear(stack) && weaponTypeOf(stack) != null;
    }

    private static UUID uuidForWeapon(EquipmentSlot slot) {
        return slot == EquipmentSlot.OFFHAND ? UUID_WEAPON_OFFHAND : UUID_WEAPON_MAINHAND;
    }

    // ===================== 属性读写工具 =====================

    /** 物品类型自带的某项属性总和（**不含** NBT 上我们挂的修饰符） */
    private static double baseAttribute(ItemStack stack, Attribute attribute, EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> defaults =
                stack.getItem().getDefaultAttributeModifiers(slot);
        double sum = 0.0D;
        for (AttributeModifier modifier : defaults.get(attribute)) {
            sum += modifier.getAmount();
        }
        return sum;
    }

    /** 物品当前的某项属性总和（**含** NBT 修饰符） */
    private static double sumAttribute(ItemStack stack, Attribute attribute) {
        Multimap<Attribute, AttributeModifier> modifiers =
                stack.getAttributeModifiers(LivingEntity.getEquipmentSlotForItem(stack));
        double sum = 0.0D;
        for (AttributeModifier modifier : modifiers.get(attribute)) {
            sum += modifier.getAmount();
        }
        return sum;
    }

    private static void addModifier(ItemStack stack, Attribute attribute, EquipmentSlot slot,
                                    UUID uuid, double amount) {
        if (Math.abs(amount) < 0.001D) {
            return;
        }
        stack.addAttributeModifier(attribute,
                new AttributeModifier(uuid, MODIFIER_NAME, amount,
                        AttributeModifier.Operation.ADDITION),
                slot);
    }

    /**
     * 把上一次挂上去的修饰符清掉。
     *
     * <p>原版 {@code ItemStack#addAttributeModifier} 只会<b>追加</b>，不做去重，
     * 不先清就会每刷新一次多一条，NBT 无限膨胀。
     * 按修饰符名字识别，不动玩家自己用命令加的那些。
     */
    private static void stripOurModifiers(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("AttributeModifiers", Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList("AttributeModifiers", Tag.TAG_COMPOUND);
        for (int i = list.size() - 1; i >= 0; i--) {
            CompoundTag entry = list.getCompound(i);
            if (MODIFIER_NAME.equals(entry.getString("Name"))) {
                list.remove(i);
            }
        }
    }

    // ===================== 充能耗尽：玩家惩罚 =====================

    /**
     * 充能耗尽时给玩家挂上「攻击大幅下降 + 移速减缓」。
     *
     * <p>用户明确要求<b>不要药水效果</b>，所以走属性修饰符（临时修饰符，不写进玩家 NBT）。
     *
     * @param on true = 挂上惩罚；false = 移除（充上能或脱下红石装备时）
     */
    public static void applyEmptyPenalty(Player player, boolean on) {
        double attackPenalty = Config.REDSTONE_EMPTY_ATTACK_PENALTY.get();
        double speedPenalty = Config.REDSTONE_EMPTY_SPEED_PENALTY.get();

        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(UUID_EMPTY_ATTACK);
            if (on && attackPenalty > 0.0D) {
                attack.addTransientModifier(new AttributeModifier(UUID_EMPTY_ATTACK,
                        MODIFIER_NAME + " empty attack", -attackPenalty,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(UUID_EMPTY_SPEED);
            if (on && speedPenalty > 0.0D) {
                speed.addTransientModifier(new AttributeModifier(UUID_EMPTY_SPEED,
                        MODIFIER_NAME + " empty speed", -speedPenalty,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
    }

    // ===================== 查询 =====================

    /** 玩家身上有没有红石装备（主手 / 副手 / 四件护甲任一） */
    public static boolean hasAnyGear(Player player) {
        if (RedstoneItems.isGear(player.getMainHandItem())
                || RedstoneItems.isGear(player.getOffhandItem())) {
            return true;
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (RedstoneItems.isGear(player.getItemBySlot(slot))) {
                return true;
            }
        }
        return false;
    }

    /** 玩家是否穿着红石靴子（飞行条件） */
    public static boolean hasBoots(Player player) {
        return RedstoneItems.isBoots(player.getItemBySlot(EquipmentSlot.FEET));
    }

    /** 手里是否拿着红石武器 / 工具（攻击、挖掘扣能的判据） */
    public static boolean holdingWeapon(Player player) {
        return isWeapon(player.getMainHandItem());
    }

    /** 身上是否穿着红石护甲（受击扣能的判据） */
    public static boolean wearingArmor(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (RedstoneItems.isArmor(player.getItemBySlot(slot))) {
                return true;
            }
        }
        return false;
    }
}
