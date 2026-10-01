package com.wuling.empire.wuling;

import com.google.common.collect.Multimap;
import com.wuling.empire.Config;
import com.wuling.empire.WulingEmpire;
import com.wuling.empire.item.ManifestItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.Tags;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * 武灵种类。
 *
 * 稀有度权重来自原文：
 *   1/100 -> 稀有（剑、弓、水、火、书）
 *   1/10  -> 常见（斧、镐、铲、红石、盔甲）
 * 权重保留 1 : 10 的相对比例。
 *
 * 做成枚举而不是写死 switch，是为了后续补充原文里未列出的种类时，
 * 只需要在这里加一行 + 补翻译，不必改动任何核心逻辑。
 */
public enum WuLingType {

    SWORD("sword", Rarity.RARE, Items.DIAMOND_SWORD,
            EnumSet.of(CultivationAction.ATTACK), HoldRequirement.SWORD),

    AXE("axe", Rarity.COMMON, Items.DIAMOND_AXE,
            EnumSet.of(CultivationAction.ATTACK, CultivationAction.MINE), HoldRequirement.AXE),

    PICKAXE("pickaxe", Rarity.COMMON, Items.DIAMOND_PICKAXE,
            EnumSet.of(CultivationAction.MINE), HoldRequirement.PICKAXE),

    SHOVEL("shovel", Rarity.COMMON, Items.DIAMOND_SHOVEL,
            EnumSet.of(CultivationAction.MINE), HoldRequirement.SHOVEL),

    BOW("bow", Rarity.RARE, Items.BOW,
            EnumSet.of(CultivationAction.SHOOT), HoldRequirement.BOW),

    WATER("water", Rarity.RARE, Items.WATER_BUCKET,
            EnumSet.of(CultivationAction.USE, CultivationAction.GUARD), HoldRequirement.ANY),

    FIRE("fire", Rarity.RARE, Items.BLAZE_POWDER,
            EnumSet.of(CultivationAction.USE, CultivationAction.ATTACK), HoldRequirement.ANY),

    REDSTONE("redstone", Rarity.COMMON, Items.REDSTONE,
            EnumSet.of(CultivationAction.USE), HoldRequirement.ANY),

    BOOK("book", Rarity.RARE, Items.ENCHANTED_BOOK,
            EnumSet.of(CultivationAction.USE), HoldRequirement.ANY),

    ARMOR("armor", Rarity.COMMON, Items.DIAMOND_CHESTPLATE,
            EnumSet.of(CultivationAction.GUARD), HoldRequirement.ARMOR);

    /** 原文给出的稀有度，数值只保留相对意义 */
    public enum Rarity {
        /** 原文 1/100 */
        RARE(1.0D, "wuling.rarity.rare"),
        /** 原文 1/10 */
        COMMON(10.0D, "wuling.rarity.common");

        private final double weight;
        private final String labelKey;

        Rarity(double weight, String labelKey) {
            this.weight = weight;
            this.labelKey = labelKey;
        }

        public double weight() {
            return weight;
        }

        public String labelKey() {
            return labelKey;
        }
    }

    /** 该武灵认「使用」时所要求的手持条件 */
    private enum HoldRequirement {
        SWORD, AXE, PICKAXE, SHOVEL, BOW, ARMOR, ANY;

        boolean matches(ItemStack held, Set<ItemStack> armorSet) {
            switch (this) {
                case SWORD:
                    return held.is(ItemTags.SWORDS);
                case AXE:
                    return held.is(ItemTags.AXES);
                case PICKAXE:
                    return held.is(ItemTags.PICKAXES);
                case SHOVEL:
                    return held.is(ItemTags.SHOVELS);
                case BOW:
                    return held.getItem() instanceof BowItem || held.getItem() instanceof CrossbowItem;
                case ARMOR:
                    return !armorSet.isEmpty();
                case ANY:
                default:
                    return true;
            }
        }
    }

    private final String key;
    private final Rarity rarity;
    private final net.minecraft.world.item.Item icon;
    private final Set<CultivationAction> actions;
    private final HoldRequirement holdRequirement;

    /**
     * 武灵强化用的属性修饰符 UUID，固定值（由 {@code UUID.nameUUIDFromBytes("wulingdiguo:manifest/…")} 生成后写死）。
     *
     * 必须是稳定 UUID：原版换装备时会用 UUID 先把旧的摘掉再挂新的
     * （{@code AttributeInstance#removeModifier(UUID)}），UUID 每次都变就会叠成一堆，
     * 而且两边都非 0，否则 {@code ItemStack#getAttributeModifiers} 会直接丢掉这条。
     *
     * <b>每个装备槽独立一个 UUID</b>：不然同时穿武灵头盔 + 武灵胸甲时，
     * 两件共用同一个 UUID，属性实例只会保留先挂上的那一条。
     */
    private static final UUID UUID_ATTACK_MAINHAND =
            UUID.fromString("f21c8857-2445-3f3b-84eb-1abcad0323f8");
    private static final UUID UUID_ATTACK_OFFHAND =
            UUID.fromString("9b90fce9-dd19-36ab-b21c-2613a0e06b5e");
    /** 下标 = EquipmentSlot#getIndex()，防具槽 0 脚 / 1 腿 / 2 胸 / 3 头 */
    private static final UUID[] UUID_ARMOR = {
            UUID.fromString("8a77a060-3782-3b56-8f92-c59947088d72"),
            UUID.fromString("4b1c865d-e125-359b-994d-66232adf3490"),
            UUID.fromString("dc2989ed-01fa-3ccf-b75e-a169dd68a349"),
            UUID.fromString("daec2c2e-642e-3e08-a7f8-14b620c7e211"),
    };
    private static final UUID[] UUID_ARMOR_TOUGHNESS = {
            UUID.fromString("d3d0167f-21ba-3328-8777-2417bc07f52f"),
            UUID.fromString("89b05740-7df6-3a98-9864-7461a03e1ead"),
            UUID.fromString("00207a29-abd8-3e44-a37c-b3e44011086e"),
            UUID.fromString("89b73273-a998-3b88-8680-bbb67d61aecd"),
    };

    WuLingType(String key, Rarity rarity, net.minecraft.world.item.Item icon,
               Set<CultivationAction> actions, HoldRequirement holdRequirement) {
        this.key = key;
        this.rarity = rarity;
        this.icon = icon;
        this.actions = actions;
        this.holdRequirement = holdRequirement;
    }

    public String key() {
        return key;
    }

    public double weight() {
        return rarity.weight();
    }

    public Rarity rarity() {
        return rarity;
    }

    public ItemStack iconStack() {
        return new ItemStack(icon);
    }

    public Set<CultivationAction> actions() {
        return actions;
    }

    public String translationKey() {
        return "wuling.type." + key;
    }

    public ResourceLocation id() {
        return new ResourceLocation(WulingEmpire.MODID, key);
    }

    /** 当前条件是否满足「正在使用这个武灵」 */
    public boolean countsAs(ItemStack held, Inventory inventory) {
        if (holdRequirement == HoldRequirement.ARMOR) {
            return inventory != null && inventory.getArmor(0) != null
                    && !inventory.getArmor(0).isEmpty();
        }
        return holdRequirement.matches(held, java.util.Collections.emptySet());
    }

    /** 判断是否属于防具类携带要求 */
    public boolean isArmorBound() {
        return holdRequirement == HoldRequirement.ARMOR;
    }

    public static WuLingType byKey(String key) {
        for (WuLingType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        return SWORD;
    }

    /** 按稀有度权重随机抽取一种武灵 */
    public static WuLingType roll(net.minecraft.util.RandomSource random) {
        double total = 0.0D;
        for (WuLingType type : values()) {
            total += type.weight();
        }
        double roll = random.nextDouble() * total;
        double acc = 0.0D;
        for (WuLingType type : values()) {
            acc += type.weight();
            if (roll < acc) {
                return type;
            }
        }
        return SWORD;
    }

    /**
     * 凝聚出实体武灵：按当前境界返回对应的实物（**可能不止一件**）。
     *
     * <p>实物取自 {@link ManifestItems} —— <b>每个大境界 × 每种武灵一件独立物品</b>
     * （{@code wulingdiguo:wood_sword} … {@code wulingdiguo:emerald_boots}），
     * 所以境界一升，拿到的是<b>另一件物品</b>，而不是同一件改名字。
     * 绿宝石档外观是单独设计的一档（钻石同款 + 绿色滤镜，数值数倍于钻石），
     * 低境界则与原版同材质同参数，靠 {@link #empower} 的属性追加拉出境界差距。
     *
     * <b>护甲武灵一次给整套四件</b>（头盔 / 胸甲 / 护腿 / 靴子）——
     * 2026-10-01 用户口径「护甲是全套护甲不是胸甲」；其余种类仍是一件。
     *
     * @return 至少一件；调用方应逐件生成实体
     */
    public java.util.List<ItemStack> manifest(int realmOrdinal, int stageOrdinal) {
        if (this == ARMOR) {
            java.util.List<ItemStack> out = new java.util.ArrayList<>(4);
            for (int i = 0; i < ManifestItems.ARMOR_PART_KEYS.length; i++) {
                out.add(manifestPiece(realmOrdinal, stageOrdinal, i));
            }
            return out;
        }
        return java.util.List.of(manifestPiece(realmOrdinal, stageOrdinal, -1));
    }

    /**
     * 凝聚出<b>单件</b>实物 —— 境界升级时用它替换背包里的旧武灵物品，
     * 也是 {@link #manifest} 的基础。
     *
     * <p>物品来自 {@link ManifestItems}（每个境界 × 每种武灵一件独立物品），
     * 因此不同境界得到的是<b>不同的物品</b>，而不是同一件改了名字。
     *
     * @param armorPart 护甲部位 0 头 / 1 胸 / 2 腿 / 3 靴；非护甲传 -1
     */
    public ItemStack manifestPiece(int realmOrdinal, int stageOrdinal, int armorPart) {
        WuLingRealm realm = WuLingRealm.byOrdinal(realmOrdinal);
        ItemStack stack;
        String partKey = null;
        if (this == ARMOR) {
            int index = Math.max(0, Math.min(ManifestItems.ARMOR_PART_KEYS.length - 1, armorPart));
            stack = new ItemStack(ManifestItems.armor(realm, ManifestItems.armorTypeOf(index)));
            partKey = ManifestItems.ARMOR_PART_KEYS[index];
        } else {
            stack = new ItemStack(ManifestItems.single(realm, this));
        }
        applyManifestEnchants(stack, realm, realmOrdinal);
        // 打标记：升级时靠它认出「哪些是凝聚出的武灵实物」
        ManifestItems.markManifest(stack, this);
        finish(stack, realm, partKey);
        return stack;
    }

    /**
     * 凝聚物自带的附魔。
     *
     * <ul>
     *   <li><b>绿宝石境界</b>：每一件都是「满配」（剑锋利 V / 火 II / 抢夺 III / 横扫 III，
     *       镐时运 III，护甲保护 IV + 荆棘 III，弓无限 I）。</li>
     *   <li><b>低境界工具</b>：按境界递增给效率附魔 —— 挖掘速度在原版不是属性修饰符
     *       （只有 {@code Tier#getSpeed}），想让低境界工具也强于原版同款只能靠附魔。</li>
     *   <li>低境界的剑 / 护甲不给附魔，强度全靠 {@link #empower} 的属性追加。</li>
     * </ul>
     */
    private void applyManifestEnchants(ItemStack stack, WuLingRealm realm, int realmOrdinal) {
        boolean emerald = realm == WuLingRealm.EMERALD;
        switch (this) {
            case SWORD -> {
                if (emerald) {
                    stack.enchant(Enchantments.SHARPNESS, 5);
                    stack.enchant(Enchantments.FIRE_ASPECT, 2);
                    stack.enchant(Enchantments.MOB_LOOTING, 3);
                    stack.enchant(Enchantments.SWEEPING_EDGE, 3);
                    stack.enchant(Enchantments.UNBREAKING, 3);
                }
            }
            case AXE -> {
                if (emerald) {
                    stack.enchant(Enchantments.SHARPNESS, 5);
                    stack.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
                    stack.enchant(Enchantments.UNBREAKING, 3);
                } else {
                    stack.enchant(Enchantments.BLOCK_EFFICIENCY, Math.min(realmOrdinal + 1, 5));
                }
            }
            case PICKAXE -> {
                if (emerald) {
                    stack.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
                    stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
                    stack.enchant(Enchantments.UNBREAKING, 3);
                } else {
                    stack.enchant(Enchantments.BLOCK_EFFICIENCY, Math.min(realmOrdinal + 1, 5));
                }
            }
            case SHOVEL -> {
                if (emerald) {
                    stack.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
                    stack.enchant(Enchantments.UNBREAKING, 3);
                } else {
                    stack.enchant(Enchantments.BLOCK_EFFICIENCY, Math.min(realmOrdinal + 1, 5));
                }
            }
            case ARMOR -> {
                if (emerald) {
                    stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
                    stack.enchant(Enchantments.THORNS, 3);
                    stack.enchant(Enchantments.UNBREAKING, 3);
                }
            }
            case BOW -> {
                stack.enchant(Enchantments.POWER_ARROWS, Math.min(realmOrdinal + 1, 5));
                if (emerald) {
                    stack.enchant(Enchantments.INFINITY_ARROWS, 1);
                }
            }
            case BOOK -> stack.enchant(Enchantments.UNBREAKING, Math.min(realmOrdinal + 1, 3));
            default -> {
            }
        }
    }

    /**
     * 统一收尾：按境界强化。
     *
     * <p>不写 {@code setHoverName} —— 显示名由物品本身的 {@code getName} 给出
     * （见 {@link ManifestItems}），这样名字不会带原版「自定义名」的斜体样式。
     *
     * @param partKey 护甲部位的翻译键，保留给将来需要单独处理时用
     */
    private ItemStack finish(ItemStack stack, WuLingRealm realm, String partKey) {
        empower(stack, realm, realm.manifestMultiplier());
        return stack;
    }

    // ===================== 武灵凝聚物的强化 =====================

    /**
     * 「武灵的伤害或防御力比原版加强了很多」—— 2026-09-26 设定。
     *
     * 做法：不改物品本身，而是在物品 NBT 上<b>再加一条属性修饰符</b>
     * （{@code ItemStack#addAttributeModifier}，1.20.1 原版装备变更时会自动生效，
     * 见 {@code LivingEntity#handleEquipmentChanges}）。
     *
     * 追加量 = 该物品原版该属性的数值 × 配置百分比 × (境界倍数 - 1)
     * （{@code Config.MANIFEST_ATTACK_BONUS} / {@code MANIFEST_ARMOR_BONUS}，默认 100）。
     *
     * <p>境界倍数自 2026-09-27 起改为「**最终数值 = 原版同款数值 × 倍数**」的口径，
     * 且按 {@code 木3 石4 黄金8 玄铁9 钻石11 下界合金14 绿宝石15} 递增 ——
     * 用户要求「每个级别都碾压上一个级别」。实测每一档都是上一档的 1.43 倍以上，
     * 绿宝石档（剑 240）是下界合金档（112）的 2.14 倍。
     * 倍数不是等比的，是因为原版各材质底子不等差（金剑 4 &lt; 石剑 5），
     * 详见 {@link WuLingRealm#manifestMultiplier()}。
     * 每个境界都是**整套统一**：剑 / 斧 / 镐 / 铲 / 胸甲一视同仁。
     *
     * 这样写的好处：木剑 / 下界合金剑、皮革甲 / 下界合金甲都自动按各自底子放大，
     * 不必给每个材质档次单独填表。没有对应属性的（弓、水/火/红石/书）自然跳过。
     */
    private static void empower(ItemStack stack, WuLingRealm realm, double multiplier) {

        // 原版数值 + 追加量 = 原版数值 × multiplier，所以追加百分比 = (multiplier - 1) × 配置。
        // 攻击力要额外算上玩家自身的基础攻击力 1（游戏里显示的「木剑 4」= 修正值 3 + 基础 1），
        // 否则「显示值 × 倍数」这个口径会差 1 点。护甲值没有这层基础，偏移为 0。
        double addPct = multiplier - 1.0D;

        double attack = amplify(stack, Attributes.ATTACK_DAMAGE, EquipmentSlot.MAINHAND,
                (double) Config.MANIFEST_ATTACK_BONUS.get() * addPct, UUID_ATTACK_MAINHAND, 1.0D);
        if (attack > 0.0D) {
            addLore(stack, Component.translatable("tooltip.wulingdiguo.manifest_attack", trim(attack)));
        }

        EquipmentSlot armorSlot = LivingEntity.getEquipmentSlotForItem(stack);
        if (armorSlot.getType() == EquipmentSlot.Type.ARMOR) {
            int index = Math.max(0, Math.min(UUID_ARMOR.length - 1, armorSlot.getIndex()));
            double armor = amplify(stack, Attributes.ARMOR, armorSlot,
                    (double) Config.MANIFEST_ARMOR_BONUS.get() * addPct, UUID_ARMOR[index], 0.0D);
            double toughness = amplify(stack, Attributes.ARMOR_TOUGHNESS, armorSlot,
                    (double) Config.MANIFEST_ARMOR_BONUS.get() * addPct, UUID_ARMOR_TOUGHNESS[index], 0.0D);
            if (armor > 0.0D || toughness > 0.0D) {
                addLore(stack, Component.translatable("tooltip.wulingdiguo.manifest_armor", trim(armor)));
            }
        }
    }

    /**
     * 把该物品「原版自带的」某项属性放大 pct%，作为一条额外修饰符挂到 NBT 上。
     *
     * @param referenceOffset 计算基数时补上的偏移量：攻击力 +1（玩家自身的基础攻击力，
     *                        原版 tooltip 上的「木剑 4」= 修正值 3 + 这 1 点），
     *                        护甲值 / 韧性 +0。补上之后，「最终显示值 = 原版显示值 × 倍数」
     *                        才严格成立。
     * @return 实际追加的数值；该物品原本没有这项属性时返回 0
     */
    private static double amplify(ItemStack stack, Attribute attribute, EquipmentSlot slot,
                                  double pct, UUID uuid, double referenceOffset) {
        if (pct <= 0.0D) {
            return 0.0D;
        }
        Multimap<Attribute, AttributeModifier> defaults =
                stack.getItem().getDefaultAttributeModifiers(slot);
        if (!defaults.containsKey(attribute)) {
            return 0.0D;
        }
        double base = 0.0D;
        for (AttributeModifier modifier : defaults.get(attribute)) {
            base += modifier.getAmount();
        }
        if (base <= 0.0D) {
            return 0.0D;
        }
        double extra = (base + referenceOffset) * pct / 100.0D;
        stack.addAttributeModifier(attribute,
                new AttributeModifier(uuid, "WuLing manifest bonus", extra,
                        AttributeModifier.Operation.ADDITION),
                slot);
        return extra;
    }

    /** 往 tooltip 里塞一行说明（走原版 display.Lore 标签），让加成看得见 */
    private static void addLore(ItemStack stack, Component line) {
        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag lore = display.getList("Lore", Tag.TAG_STRING);
        lore.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        display.put("Lore", lore);
    }

    /** 5.0 → "5"，7.5 → "7.5" */
    private static String trim(double value) {
        return Math.abs(value - Math.round(value)) < 0.05D
                ? String.valueOf(Math.round(value))
                : String.format("%.1f", value);
    }

}
