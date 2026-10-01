package com.wuling.empire.wuling;

import com.wuling.empire.Config;
import com.wuling.empire.item.ManifestItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 怪物等级（武灵属性）系统 —— 2026-09-27 用户设定。
 *
 * <p>原文口径：<b>怪物也有武灵属性，等级用同一套阶梯「木 → 石 → 黄金 → 玄铁 → 钻石 →
 * 下界合金 → 绿宝石」；带属性的怪物一律用「本属性的剑」攻击；每种怪物都有小概率带属性，
 * 而且等级越高的越非常稀有。</b>
 *
 * <p>落地方式刻意做得「看得见」：
 * <ul>
 *   <li>等级写进怪物主手 —— 木阶拿木剑、钻石阶拿钻石剑、绿宝石阶直接拿
 *       {@link ManifestItems 武灵剑}（绿宝石阶那把一眼就是绿的）。玩家一眼就能看出对面是什么阶。</li>
 *   <li>近战伤害随之提高：装备的 {@code ATTACK_DAMAGE} 修饰符由原版
 *       {@code LivingEntity#handleEquipmentChanges} 自动挂上，不必自己写伤害逻辑。</li>
 *   <li>剑的掉落率钉成 0：绿宝石阶极其稀有（默认约万分之一），若还能掉出绿宝石剑
 *       就等于绕开整条武灵晋升线，所以只作为「战利品税」不给装备。</li>
 * </ul>
 *
 * <p>抽取概率：先按 {@code monsterTier.tierChance} 决定「这只有没有等级」，
 * 再按 {@code tierRarityRatio} 的几何衰减在 7 个境界里抽。
 * 默认（10% / 2.5）的实际分布见 {@code docs/武灵系统设计规格.md}。
 *
 * <p>数据落在怪物的 Forge 持久化数据（{@code Entity#getPersistentData}）里，
 * 只抽一次：区块反复加载不会重掷等级。血量加成走固定 UUID 的属性修饰符，
 * 每次进世界先摘后挂，永远不会叠加。
 */
public final class MonsterTier {

    private MonsterTier() {
    }

    /** 大境界序号 + 1（0 / 缺省 = 普通怪，没有等级） */
    private static final String KEY_TIER = "wulingdiguo:MonsterTier";
    /** 是否已经掷过骰子（保证一只怪一辈子只有一个等级） */
    private static final String KEY_ROLLED = "wulingdiguo:MonsterTierRolled";
    /** 是否已经补过血（避免区块重载时白送一次满血） */
    private static final String KEY_HEALTH = "wulingdiguo:MonsterTierHealth";

    /** 稳定的修饰符 UUID：先摘后挂，重复进世界也不会叠血 */
    private static final UUID UUID_HEALTH =
            UUID.fromString("6f3a5e2c-9f41-3c62-93d5-6f1b6f5b0a31");

    // ===================== 入口 =====================

    /**
     * 敌对生物进入世界时调用（自然刷怪 / 刷怪笼 / 刷怪蛋 / 区块加载都会走到）。
     * 幂等：掷骰子只做一次，装备与血量每次进来都重新校正一遍。
     */
    public static void onJoin(Mob mob) {
        if (mob.level().isClientSide()) {
            return;
        }
        // 只对「怪物」生效，村民 / 动物这类和平生物不参与
        if (!(mob instanceof Enemy)) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (id == null || Config.MONSTER_TIER_EXCLUDED_MOBS.get().contains(id.toString())) {
            return;
        }

        CompoundTag data = mob.getPersistentData();
        if (!data.getBoolean(KEY_ROLLED)) {
            data.putBoolean(KEY_ROLLED, true);
            WuLingRealm realm = roll(mob.getRandom());
            if (realm != null) {
                data.putInt(KEY_TIER, realm.ordinal() + 1);
            }
        }

        WuLingRealm realm = tierOf(mob);
        if (realm == null) {
            return;
        }
        equip(mob, realm, data);
    }

    /** 该生物带的武灵等级；普通怪返回 null */
    @Nullable
    public static WuLingRealm tierOf(Entity entity) {
        int stored = entity.getPersistentData().getInt(KEY_TIER);
        return stored <= 0 ? null : WuLingRealm.byOrdinal(stored - 1);
    }

    // ===================== 抽取 =====================

    /**
     * 掷一次等级。
     *
     * @return 抽到的境界；没抽中（绝大多数怪）返回 null
     */
    @Nullable
    public static WuLingRealm roll(RandomSource random) {
        double chance = Config.MONSTER_TIER_CHANCE.get();
        if (chance <= 0.0D || random.nextDouble() >= chance) {
            return null;
        }

        WuLingRealm[] realms = WuLingRealm.values();
        double ratio = Math.max(1.0D, Config.MONSTER_TIER_RARITY.get());
        double[] weights = new double[realms.length];
        double total = 0.0D;
        // 「等级越高越非常稀有」：权重按 ratio 的阶数几何衰减
        for (int i = 0; i < realms.length; i++) {
            weights[i] = 1.0D / Math.pow(ratio, i);
            total += weights[i];
        }

        double pick = random.nextDouble() * total;
        double acc = 0.0D;
        for (int i = 0; i < realms.length; i++) {
            acc += weights[i];
            if (pick < acc) {
                return realms[i];
            }
        }
        return realms[0];
    }

    /**
     * 「本属性的剑」：境界 → 对应那件武灵剑。
     *
     * 2026-10-01 起所有境界的武灵实物都是 {@link ManifestItems} 里独立注册的物品
     * （{@code wulingdiguo:wood_sword} … {@code emerald_sword}），所以这里直接查表，
     * 不再区分「原版剑 / 本模组剑」。
     */
    public static Item swordOf(WuLingRealm realm) {
        return ManifestItems.single(realm, WuLingType.SWORD);
    }

    // ===================== 应用 =====================

    private static void equip(Mob mob, WuLingRealm realm, CompoundTag data) {
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(swordOf(realm)));
        // 不掉落：木剑随手捡无所谓，绿宝石剑掉出来会直接绕开武灵晋升线
        // （严格说 Looting III 仍有约 3% 的兜底判定，数量级可忽略）
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        applyHealth(mob, realm, data);
    }

    /**
     * 等级越高血越厚：木阶不变，每高一阶 +{@code monsterTier.tierHealthBonusPerRealm} 倍。
     * 顺带一个隐性联动 —— 灵珠品质是按「来源怪最大生命值」推的（{@code CorpseEntity#randomQuality}），
     * 所以高阶怪掉的灵珠品质天然更好。
     */
    private static void applyHealth(Mob mob, WuLingRealm realm, CompoundTag data) {
        double per = Config.MONSTER_TIER_HEALTH_BONUS.get();
        double mult = 1.0D + per * realm.ordinal();
        if (mult <= 1.0D) {
            return;
        }
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        double base = health.getBaseValue();
        if (base <= 0.0D) {
            return;
        }

        health.removeModifier(UUID_HEALTH);
        health.addPermanentModifier(new AttributeModifier(UUID_HEALTH, "WuLing monster tier",
                base * (mult - 1.0D), AttributeModifier.Operation.ADDITION));

        // 只在第一次补齐血量：区块重载时不能白送一次满血
        if (!data.getBoolean(KEY_HEALTH)) {
            data.putBoolean(KEY_HEALTH, true);
            mob.setHealth(mob.getMaxHealth());
        }
    }
}
