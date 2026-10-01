package com.wuling.empire;

import com.wuling.empire.item.SpiritQuality;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 模组配置（COMMON）。
 *
 * 所有灵珠系统的可调数值集中在此，方便后续数值调整而无需改代码。
 */
public final class Config {

    private Config() {
    }

    /**
     * 设定层面「永不出灵珠」的生物 —— 硬编码，**不受 config 文件影响**。
     *
     * 为什么不直接靠 {@link #EXCLUDED_MOBS}：Forge 的配置文件一旦生成就固定在
     * 存档/config 目录里，改代码里的默认值对已生成的旧配置**无效**，玩家会遇到
     * 「代码里明明排除了，游戏里还掉」的怪事。这几只是设定上就不该有灵珠的，
     * 所以在这里再钉死一遍，与 EXCLUDED_MOBS 取并集。
     *   蠹虫、恼鬼 —— 原版设定点名排除；
     *   末影螨 —— 2026-09-26 用户明确「末影螨也不要」。
     */
    public static final Set<String> NEVER_DROPS = Set.of(
            "minecraft:silverfish",
            "minecraft:vex",
            "minecraft:endermite"
    );

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ===================== 灵力面板 =====================
    public static final ForgeConfigSpec.DoubleValue MAX_SPIRIT;
    public static final ForgeConfigSpec.DoubleValue REGEN_PER_MINUTE;

    /**
     * 灵力已满时，仍然吸收灵珠能换到的<b>少量修炼进度</b>（2026-10-01 用户口径
     * 「在灵力满时吸收灵珠可小幅度增加修为」）。
     *
     * <p>真正累加的进度 = 该值 × 灵珠品质的修炼倍率，所以极品珠比凡品珠划算。
     * 设为 0 即关闭（回到「灵力满时吸收只会提示灵力已满」的老行为）。
     */
    public static final ForgeConfigSpec.DoubleValue BEAD_FULL_PROGRESS;

    // ===================== 灵珠回复量：品质倍率 =====================
    // 最终回复量 = 怪物基准（item/BeadPower 的 BASE 表）× 这里的品质倍率
    public static final ForgeConfigSpec.DoubleValue RESTORE_MULT_FAN;
    public static final ForgeConfigSpec.DoubleValue RESTORE_MULT_LIANG;
    public static final ForgeConfigSpec.DoubleValue RESTORE_MULT_YOU;
    public static final ForgeConfigSpec.DoubleValue RESTORE_MULT_SHANG;
    public static final ForgeConfigSpec.DoubleValue RESTORE_MULT_JI;

    // ===================== 掉落权重 =====================
    public static final ForgeConfigSpec.DoubleValue WEIGHT_FAN;
    public static final ForgeConfigSpec.DoubleValue WEIGHT_LIANG;
    public static final ForgeConfigSpec.DoubleValue WEIGHT_YOU;
    public static final ForgeConfigSpec.DoubleValue WEIGHT_SHANG;
    public static final ForgeConfigSpec.DoubleValue WEIGHT_JI;
    public static final ForgeConfigSpec.DoubleValue STRENGTH_INFLUENCE;

    // ===================== 怪物尸体 =====================
    public static final ForgeConfigSpec.IntValue CORPSE_LIFETIME_TICKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_MOBS;
    public static final ForgeConfigSpec.BooleanValue DROP_BEADS_TO_GROUND_ON_TIMEOUT;

    // ===================== 第二部：武灵系统 =====================

    public static final ForgeConfigSpec.IntValue BINDING_CONSUMES_BEAD_COUNT;
    public static final ForgeConfigSpec.BooleanValue BINDING_REQUIRES_NPC;

    /**
     * 武灵的统一境界上限（所有玩家都一样）。
     *
     * 2026-09-27 用户修订：<b>上限不再由所交灵珠的品质决定</b>，
     * 谁来开启、拿什么品质的灵珠来开启，能修到的最高境界都相同；
     * 品质只影响<b>修炼速度</b>（见下面的 cultivationBonus*）。
     *
     * ⚠️ 键名从旧的 capFan / capLiang / capYou / capShang / capJi 换成单一的 maxRealm：
     * 老配置文件里旧键仍有值，而 Forge 不会跟随代码默认值更新，
     * 沿用旧键会出现「改了上限却没生效」的假象。
     */
    public static final ForgeConfigSpec.IntValue MAX_REALM;

    /** 灵珠品质 -> 修炼速度加成倍率（乘在每次修炼动作的进度上） */
    public static final ForgeConfigSpec.DoubleValue CULTIVATION_BONUS_FAN;
    public static final ForgeConfigSpec.DoubleValue CULTIVATION_BONUS_LIANG;
    public static final ForgeConfigSpec.DoubleValue CULTIVATION_BONUS_YOU;
    public static final ForgeConfigSpec.DoubleValue CULTIVATION_BONUS_SHANG;
    public static final ForgeConfigSpec.DoubleValue CULTIVATION_BONUS_JI;

    /** 小境界晋升所需进度曲线 */
    public static final ForgeConfigSpec.DoubleValue THRESHOLD_BASE;
    public static final ForgeConfigSpec.DoubleValue THRESHOLD_GROWTH;

    /** 各类修炼动作的进度产出 */
    public static final ForgeConfigSpec.DoubleValue PROGRESS_ATTACK;
    public static final ForgeConfigSpec.DoubleValue PROGRESS_MINE;
    public static final ForgeConfigSpec.DoubleValue PROGRESS_SHOOT;
    public static final ForgeConfigSpec.DoubleValue PROGRESS_GUARD;
    public static final ForgeConfigSpec.DoubleValue PROGRESS_USE;

    /**
     * 突破是否消耗（清空）灵力。
     * 2026-09-26 起默认 false —— 灵力改由「凝聚武灵」消耗。
     * ⚠️ 键名从旧的 breakthroughClearsSpirit 改成 breakthroughConsumesSpirit：
     * 老配置文件里旧键是 true，Forge 不会跟随代码默认值更新，沿用旧键会导致「改了默认值却没生效」。
     */
    public static final ForgeConfigSpec.BooleanValue BREAKTHROUGH_CONSUMES_SPIRIT;

    /** 凝聚一次实体武灵消耗的灵力（百分制） */
    public static final ForgeConfigSpec.DoubleValue CONDENSE_SPIRIT_COST;

    /** 凝聚出的武灵比原版同款多出的攻击力（按原版数值的百分比） */
    public static final ForgeConfigSpec.DoubleValue MANIFEST_ATTACK_BONUS;

    /** 凝聚出的武灵比原版同款多出的护甲值（按原版数值的百分比） */
    public static final ForgeConfigSpec.DoubleValue MANIFEST_ARMOR_BONUS;

    /**
     * 「每种怪物灵珠各 N 个」类突破条件里额外排除的怪物 ID。
     * 这些怪物照样掉灵珠，只是不参与这几档突破的收集要求。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BREAKTHROUGH_EXCLUDED_MOBS;

    /** 6 段大境界突破的物资需求 */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REQ_WOOD_TO_STONE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REQ_STONE_TO_GOLD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REQ_GOLD_TO_METEOR;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REQ_METEOR_TO_DIAMOND;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REQ_DIAMOND_TO_NETHERITE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REQ_NETHERITE_TO_EMERALD;

    // ===================== 怪物等级（武灵属性，2026-09-27） =====================
    /** 怪物带武灵等级的概率 */
    public static final ForgeConfigSpec.DoubleValue MONSTER_TIER_CHANCE;
    /** 等级越高的稀有个衰减比（几何衰减的底数） */
    public static final ForgeConfigSpec.DoubleValue MONSTER_TIER_RARITY;
    /** 每高一个大境界，最大生命值的追加比例 */
    public static final ForgeConfigSpec.DoubleValue MONSTER_TIER_HEALTH_BONUS;
    /** 不参与怪物等级系统的生物 ID */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MONSTER_TIER_EXCLUDED_MOBS;

    static {
        BUILDER.push("spirit");

        MAX_SPIRIT = BUILDER.comment("灵力上限（百分制）")
                .defineInRange("maxSpirit", 100.0D, 1.0D, 1000.0D);

        REGEN_PER_MINUTE = BUILDER.comment("灵力自然回复速度：每分钟回复百分比")
                .defineInRange("regenPerMinute", 1.0D, 0.0D, 100.0D);

        BEAD_FULL_PROGRESS = BUILDER.comment("灵力已满时吸收灵珠得到的少量修炼进度；",
                        "实际累加 = 该值 × 灵珠品质的修炼倍率。0 = 关闭。")
                .defineInRange("beadFullProgress", 3.0D, 0.0D, 10000.0D);

        BUILDER.pop();

        BUILDER.push("bead");

        RESTORE_MULT_FAN = BUILDER.comment("凡品灵珠的回复倍率（与怪物基准相乘）")
                .defineInRange("restoreMultiplierFan", 1.0D, 0.0D, 100.0D);
        RESTORE_MULT_LIANG = BUILDER.comment("良品灵珠的回复倍率")
                .defineInRange("restoreMultiplierLiang", 1.5D, 0.0D, 100.0D);
        RESTORE_MULT_YOU = BUILDER.comment("优品灵珠的回复倍率")
                .defineInRange("restoreMultiplierYou", 2.0D, 0.0D, 100.0D);
        RESTORE_MULT_SHANG = BUILDER.comment("上品灵珠的回复倍率")
                .defineInRange("restoreMultiplierShang", 3.0D, 0.0D, 100.0D);
        RESTORE_MULT_JI = BUILDER.comment("极品灵珠的回复倍率。" +
                        "回复量 = 怪物基准（item/BeadPower 的 BASE 表，凡品值）× 本倍率；" +
                        "例：僵尸基准 5 → 极品 20%，末影龙基准 25 → 极品 100%。")
                .defineInRange("restoreMultiplierJi", 4.0D, 0.0D, 100.0D);

        BUILDER.pop();

        BUILDER.push("drop");

        WEIGHT_FAN = BUILDER.comment("凡品灵珠基础权重")
                .defineInRange("weightFan", 50.0D, 0.0D, 100000.0D);
        WEIGHT_LIANG = BUILDER.comment("良品灵珠基础权重")
                .defineInRange("weightLiang", 30.0D, 0.0D, 100000.0D);
        WEIGHT_YOU = BUILDER.comment("优品灵珠基础权重")
                .defineInRange("weightYou", 14.0D, 0.0D, 100000.0D);
        WEIGHT_SHANG = BUILDER.comment("上品灵珠基础权重")
                .defineInRange("weightShang", 5.0D, 0.0D, 100000.0D);
        WEIGHT_JI = BUILDER.comment("极品灵珠基础权重")
                .defineInRange("weightJi", 1.0D, 0.0D, 100000.0D);

        STRENGTH_INFLUENCE = BUILDER.comment("怪物强度对高品质灵珠权重的影响系数。" +
                        "实际权重 = 基础权重 * (1 + (怪物最大生命值 - 20) / 40 * 该系数) ^ 品质序号，" +
                        "序号：凡品0 / 良品1 / 优品2 / 上品3 / 极品4。设为 0 则完全随机，与怪物强度无关。")
                .defineInRange("strengthInfluence", 1.0D, 0.0D, 5.0D);

        EXCLUDED_MOBS = BUILDER.comment("不掉落灵珠的生物 ID 列表（原版设定：蠹虫、恼鬼除外；末影螨另加）")
                .defineList("excludedMobs", Arrays.asList(
                        "minecraft:silverfish",
                        "minecraft:vex",
                        "minecraft:endermite"
                ), o -> o instanceof String);

        BUILDER.pop();

        BUILDER.push("corpse");

        CORPSE_LIFETIME_TICKS = BUILDER.comment("尸体存在时长（tick），1200 tick = 1 分钟")
                .defineInRange("lifetimeTicks", 12000, 20, Integer.MAX_VALUE);

        DROP_BEADS_TO_GROUND_ON_TIMEOUT = BUILDER.comment("尸体超时消失时是否把未取走的灵珠掉落到地面")
                .define("dropBeadsOnTimeout", true);

        BUILDER.pop();

        // ===================== 第二部：武灵系统 =====================

        BUILDER.push("wuling");

        BINDING_CONSUMES_BEAD_COUNT = BUILDER.comment("开启 / 绑定一次武灵需要消耗的灵珠数量")
                .defineInRange("bindingBeadCount", 1, 1, 64);

        BINDING_REQUIRES_NPC = BUILDER.comment("是否必须靠近「拥有水武灵的 NPC」才能开启武灵。" +
                        "NPC 实体属于后续阶段，因此当前默认 false——false 时拿着绑定器就能绑。")
                .define("bindingRequiresNpc", false);

        // 统一的境界上限 + 品质 -> 修炼速度。境界序号：木0 石1 黄金2 玄铁3 钻石4 下界合金5 绿宝石6
        MAX_REALM = BUILDER.comment(
                        "所有武灵共用的最高境界序号（0木 1石 2黄金 3玄铁 4钻石 5下界合金 6绿宝石）。" +
                        "2026-09-27 起上限与灵珠品质无关：不论谁来开启、交什么品质的灵珠，" +
                        "能修到的最高境界都一样；品质只影响修炼速度（见下面的 cultivationBonus*）。" +
                        "默认 6 —— 人人都能修到绿宝石。")
                .defineInRange("maxRealm", 6, 0, 6);

        CULTIVATION_BONUS_FAN = BUILDER.comment(
                        "凡品灵珠开启的武灵，修炼速度加成倍率（乘在每次修炼动作累积的进度上）。" +
                        "1.0 = 基准速度不变。")
                .defineInRange("cultivationBonusFan", 1.0D, 0.0D, 100.0D);
        CULTIVATION_BONUS_LIANG = BUILDER.comment("良品灵珠开启的武灵，修炼速度加成倍率")
                .defineInRange("cultivationBonusLiang", 1.25D, 0.0D, 100.0D);
        CULTIVATION_BONUS_YOU = BUILDER.comment("优品灵珠开启的武灵，修炼速度加成倍率")
                .defineInRange("cultivationBonusYou", 1.5D, 0.0D, 100.0D);
        CULTIVATION_BONUS_SHANG = BUILDER.comment("上品灵珠开启的武灵，修炼速度加成倍率")
                .defineInRange("cultivationBonusShang", 2.0D, 0.0D, 100.0D);
        CULTIVATION_BONUS_JI = BUILDER.comment(
                        "极品灵珠开启的武灵，修炼速度加成倍率。" +
                        "默认 3.0 —— 同一套动作，极品灵珠攒进度是凡品的 3 倍快，" +
                        "但不能突破上限（上限由 maxRealm 统一决定）。")
                .defineInRange("cultivationBonusJi", 3.0D, 0.0D, 100.0D);

        THRESHOLD_BASE = BUILDER.comment("小境界晋升基准进度；第 n 个境界所需进度 = 该值 * (n+1)")
                .defineInRange("thresholdBase", 120.0D, 1.0D, 1000000.0D);

        THRESHOLD_GROWTH = BUILDER.comment("每提升一个大境界，所需进度的增长倍率")
                .defineInRange("thresholdGrowth", 2.2D, 1.0D, 20.0D);

        PROGRESS_ATTACK = BUILDER.comment("修炼动作 - 造成伤害：每点伤害累积的进度")
                .defineInRange("progressAttack", 1.0D, 0.0D, 1000.0D);
        PROGRESS_MINE = BUILDER.comment("修炼动作 - 挖掘方块：每次累积的进度")
                .defineInRange("progressMine", 1.0D, 0.0D, 1000.0D);
        PROGRESS_SHOOT = BUILDER.comment("修炼动作 - 弓箭命中：每次累积的进度")
                .defineInRange("progressShoot", 6.0D, 0.0D, 1000.0D);
        PROGRESS_GUARD = BUILDER.comment("修炼动作 - 承受伤害：每次累积的进度")
                .defineInRange("progressGuard", 2.0D, 0.0D, 1000.0D);
        PROGRESS_USE = BUILDER.comment("修炼动作 - 使用道具 / 交互：每次累积的进度")
                .defineInRange("progressUse", 4.0D, 0.0D, 1000.0D);

        BREAKTHROUGH_CONSUMES_SPIRIT = BUILDER.comment("大境界突破完成后是否清空全部灵力。" +
                        "0.2.14 起默认 false：突破不消耗灵力，灵力改由「凝聚武灵」消耗。")
                .define("breakthroughConsumesSpirit", false);

        CONDENSE_SPIRIT_COST = BUILDER.comment("凝聚一次实体武灵消耗的灵力（百分制，100 = 灵力上限）。" +
                        "灵力不足则无法凝聚；设为 0 表示免费。")
                .defineInRange("condenseSpiritCost", 25.0D, 0.0D, 1000.0D);

        MANIFEST_ATTACK_BONUS = BUILDER.comment("凝聚出的武灵比原版同款多出的攻击力，" +
                        "按原版该物品的攻击力百分比计算（100 = 追加 100%，0 = 与原版一致）。" +
                        "只对剑 / 斧 / 镐 / 铲这类有攻击力的近战装备生效。" +
                        "实际追加量还会再乘一个「境界倍数-1」（木3 石4 黄金8 玄铁9 " +
                        "钻石11 下界合金14 绿宝石15，见 WuLingRealm#manifestMultiplier），" +
                        "所以配置 100 时凝聚物的最终数值 = 原版同款 × 境界倍数：" +
                        "每一档都比上一档强 1.43 倍以上，绿宝石剑约 240。")
                .defineInRange("manifestAttackBonus", 100.0D, 0.0D, 1000.0D);

        MANIFEST_ARMOR_BONUS = BUILDER.comment("凝聚出的武灵比原版同款多出的护甲值，" +
                        "按原版该物品的护甲值百分比计算（100 = 护甲翻倍）。只对盔甲武灵生效。" +
                        "同样会再乘 (境界倍数-1)（见 manifestAttackBonus 的说明）。")
                .defineInRange("manifestArmorBonus", 100.0D, 0.0D, 1000.0D);

        // 大境界突破所需物资。格式： "物品注册名;数量"，
        // 特殊项 "ALL_BEADS;数量"   = 除排除名单外，每种怪物（不限品质）的灵珠各若干个；
        // 特殊项 "ALL_JI_BEADS;数量" = 同上但只收极品灵珠（旧口径，保留兼容）。
        BREAKTHROUGH_EXCLUDED_MOBS = BUILDER.comment(
                        "「每种怪物灵珠各 N 个」类突破条件里额外排除的怪物 ID。" +
                        "这些怪物照样掉灵珠，只是不参与这几档突破的收集要求。" +
                        "2026-09-26 用户点名：巨人、疣猪兽、幻术师、远古守卫者、流浪者、监守者。")
                .defineList("breakthroughExcludedMobs", Arrays.asList(
                        "minecraft:giant",
                        "minecraft:hoglin",
                        "minecraft:illusioner",
                        "minecraft:elder_guardian",
                        "minecraft:stray",
                        "minecraft:warden"
                ), o -> o instanceof String);

        REQ_WOOD_TO_STONE = requirement("wood_to_stone",
                "木 → 石", Arrays.asList("minecraft:oak_log;64"));
        REQ_STONE_TO_GOLD = requirement("stone_to_gold",
                "石 → 黄金", Arrays.asList("minecraft:cobblestone;512", "minecraft:coal;64"));
        REQ_GOLD_TO_METEOR = requirement("gold_to_meteor",
                "黄金 → 玄铁", Arrays.asList("minecraft:gold_ingot;128", "minecraft:iron_ingot;256"));
        REQ_METEOR_TO_DIAMOND = requirement("meteor_to_diamond",
                "玄铁 → 钻石", Arrays.asList("minecraft:iron_block;128", "minecraft:obsidian;256"));
        REQ_DIAMOND_TO_NETHERITE = requirement("diamond_to_netherite",
                "钻石 → 下界合金（原文：约 18 组金锭 + 约 18 组远古残骸）",
                Arrays.asList("minecraft:gold_ingot;1152", "minecraft:ancient_debris;1152"));
        // ⚠️ 键名从旧的 netherite_to_emerald 改成 emerald_beads_any_quality（2026-09-27）：
        // 0.2.13 就把默认值从 ALL_JI_BEADS 改成了 ALL_BEADS，但**键名没换**，
        // 于是老存档里那份配置里仍然是「只收极品」，游戏里照旧只认极品 ——
        // Forge 不会把已生成的配置跟随代码默认值更新。换键名才能让新默认值真正生效。
        REQ_NETHERITE_TO_EMERALD = requirement("emerald_beads_any_quality",
                "下界合金 → 绿宝石（2026-09-27 修订：除巨人/疣猪兽/幻术师/远古守卫者/流浪者/监守者外，"
                        + "每种怪物灵珠不限品质各 10 个 + 10 个绿宝石块）",
                Arrays.asList("ALL_BEADS;10", "minecraft:emerald_block;10"));

        BUILDER.pop();

        // ===================== 怪物等级（武灵属性） =====================

        BUILDER.push("monsterTier");

        MONSTER_TIER_CHANCE = BUILDER.comment(
                        "怪物带「武灵等级（属性）」的概率。0 = 关闭整个系统，1 = 每只怪都有等级。" +
                        "带等级的怪物会手持该等级的剑（木剑 → 石剑 → 金剑 → 铁剑 → 钻石剑 → " +
                        "下界合金剑 → 绿宝石剑），近战伤害随之提高，最大生命值也会按下面的配置放大。")
                .defineInRange("tierChance", 0.10D, 0.0D, 1.0D);

        MONSTER_TIER_RARITY = BUILDER.comment(
                        "「等级越高越非常稀有」的衰减比：第 n 阶的权重 = 1 / 本值的 n 次方。" +
                        "默认 2.5 时，在「有等级的怪」里木阶约 60%、石阶约 24%、黄金约 9.6%、" +
                        "玄铁约 3.8%、钻石约 1.5%、下界合金约 0.6%、绿宝石约 0.25%；" +
                        "再乘 tierChance（默认 0.1），绿宝石阶约每 4000 只怪出一只。" +
                        "调大 = 高阶更罕见，调成 1 = 七个等级等概率。")
                .defineInRange("tierRarityRatio", 2.5D, 1.0D, 20.0D);

        MONSTER_TIER_HEALTH_BONUS = BUILDER.comment(
                        "每高一个大境界，怪物最大生命值的追加比例：倍率 = 1 + 大境界序号 × 本值。" +
                        "默认 0.5 → 木阶 1.0 倍（不变）、石阶 1.5 倍、黄金 2 倍、玄铁 2.5 倍、" +
                        "钻石 3 倍、下界合金 3.5 倍、绿宝石 4 倍。设为 0 = 只换武器不加血。" +
                        "（灵珠品质按来源怪最大生命值推算，所以高阶怪的灵珠天然更好。）")
                .defineInRange("tierHealthBonusPerRealm", 0.5D, 0.0D, 100.0D);

        MONSTER_TIER_EXCLUDED_MOBS = BUILDER.comment(
                        "不参与怪物等级系统的生物 ID。默认排除两类：" +
                        "① 不近战的（苦力怕、史莱姆、岩浆怪、恶魂、烈焰人、女巫、潜影贝）——" +
                        "给它们剑也不会用；" +
                        "② 不渲染手持物或另有攻击方式的（幻翼、守卫者、远古守卫者、蠹虫、末影螨、" +
                        "凋灵、末影龙）。把它们删掉就会一起参与。")
                .defineList("tierExcludedMobs", Arrays.asList(
                        "minecraft:creeper",
                        "minecraft:slime",
                        "minecraft:magma_cube",
                        "minecraft:ghast",
                        "minecraft:blaze",
                        "minecraft:witch",
                        "minecraft:shulker",
                        "minecraft:phantom",
                        "minecraft:guardian",
                        "minecraft:elder_guardian",
                        "minecraft:silverfish",
                        "minecraft:endermite",
                        "minecraft:wither",
                        "minecraft:ender_dragon"
                ), o -> o instanceof String);

        BUILDER.pop();
    }

    private static ForgeConfigSpec.ConfigValue<List<? extends String>> requirement(
            String key, String displayName, List<String> defaults) {
        return BUILDER.comment("大境界突破物资 · " + displayName + "。每项格式：\"物品注册名;数量\"")
                .defineList(key, defaults, o -> o instanceof String);
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // ===================== 便捷读取 =====================

    /** 灵力上限 */
    public static float maxSpirit() {
        return (float) (double) MAX_SPIRIT.get();
    }

    /**
     * 某个品质灵珠的回复倍率。
     *
     * 实际回复量还要乘上怪物基准，见 {@code item/BeadPower#restore}。
     */
    public static float restoreMultiplier(SpiritQuality quality) {
        switch (quality) {
            case LIANG:
                return (float) (double) RESTORE_MULT_LIANG.get();
            case YOU:
                return (float) (double) RESTORE_MULT_YOU.get();
            case SHANG:
                return (float) (double) RESTORE_MULT_SHANG.get();
            case JI:
                return (float) (double) RESTORE_MULT_JI.get();
            case FAN:
            default:
                return (float) (double) RESTORE_MULT_FAN.get();
        }
    }

    /** 某个品质灵珠的基础权重 */
    public static double baseWeight(SpiritQuality quality) {
        switch (quality) {
            case LIANG:
                return WEIGHT_LIANG.get();
            case YOU:
                return WEIGHT_YOU.get();
            case SHANG:
                return WEIGHT_SHANG.get();
            case JI:
                return WEIGHT_JI.get();
            case FAN:
            default:
                return WEIGHT_FAN.get();
        }
    }

    /** 所有武灵共用的最高境界序号（与灵珠品质无关） */
    public static int maxRealm() {
        return MAX_REALM.get();
    }

    /**
     * 某品质灵珠开启的武灵的<b>修炼速度加成</b>。
     *
     * 2026-09-27 起品质不再决定境界上限，只决定「攒进度快多少倍」：
     * 最终进度 = 动作基准（progressAttack / progressMine / …）× 本倍率。
     */
    public static double cultivationBonus(SpiritQuality quality) {
        if (quality == null) {
            return 1.0D;
        }
        switch (quality) {
            case LIANG: return CULTIVATION_BONUS_LIANG.get();
            case YOU: return CULTIVATION_BONUS_YOU.get();
            case SHANG: return CULTIVATION_BONUS_SHANG.get();
            case JI: return CULTIVATION_BONUS_JI.get();
            case FAN:
            default: return CULTIVATION_BONUS_FAN.get();
        }
    }

    /** 第 realmOrdinal 个大境界内，晋升一个小境界所需的累积进度 */
    public static double stageThreshold(int realmOrdinal) {
        return THRESHOLD_BASE.get() * Math.pow(THRESHOLD_GROWTH.get(), Math.max(0, realmOrdinal));
    }

    /** 六段突破的中文名，只用于启动日志 */
    private static final String[] REALM_STEP_NAMES = {
            "?", "木→石", "石→黄金", "黄金→玄铁", "玄铁→钻石", "钻石→下界合金", "下界合金→绿宝石"
    };

    /**
     * 把「当前真正生效的」突破物资打进日志。
     *
     * <p>存在的意义：Forge 的配置文件一旦生成就固定在那里，<b>代码改默认值对它无效</b>，
     * 这个坑已经踩过好几次（灵力门槛、capFan…、绿宝石只要极品）。
     * 启动时打一行日志，下次就能一眼看出「配置里的生效值」与「代码里写的」是否一致，
     * 不必进游戏一间间试。
     */
    public static void logEffectiveRequirements() {
        for (int realm = 1; realm <= 6; realm++) {
            WulingEmpire.LOGGER.info("[武灵帝国] 突破物资生效值 · {} = {}（ALL_BEADS = 不限品质，"
                    + "ALL_JI_BEADS = 只要极品）", REALM_STEP_NAMES[realm], breakthroughRequirement(realm));
        }
    }

    /** 突破到第 realmIndex 个大境界所需的物资清单原始字符串 */
    public static List<? extends String> breakthroughRequirement(int realmOrdinal) {
        switch (realmOrdinal) {
            case 1: return REQ_WOOD_TO_STONE.get();
            case 2: return REQ_STONE_TO_GOLD.get();
            case 3: return REQ_GOLD_TO_METEOR.get();
            case 4: return REQ_METEOR_TO_DIAMOND.get();
            case 5: return REQ_DIAMOND_TO_NETHERITE.get();
            case 6: return REQ_NETHERITE_TO_EMERALD.get();
            default: return java.util.Collections.emptyList();
        }
    }
}
