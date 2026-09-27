# 项目长期记忆：《武灵帝国》Minecraft 模组

## 定位与技术选型
小说《我的世界之武灵帝国》（双子动漫创作，风叔叔播讲）改编模组。设定按「第 N 部分」分批下发，
每轮只实现当前批次；**留意跨批次的隐含信息**（例：「灵珠种类决定修炼方向」+ 第四部物价
「极品僵尸灵珠」→ 反推出灵珠除品质外还有「来源」维度）。
- MC **1.20.1 / Forge 47.x 全版本**（编译目标锁最低版 47.0.0，见下），modid `wulingdiguo`，包 `com.wuling.empire`
- 注册表一律 `DeferredRegister`；玩家数据用 Capability + NBT 序列化
- **交付要求：真编译通过再交付**（用户明确要求）

## 构建（路径已从 Administrator 迁到 jiami）
系统只有 JDK 8，必须手动指定便携 JDK 17：
```
cd "C:/Users/jiami/Desktop/武灵帝国"
export JAVA_HOME="C:/Users/jiami/Desktop/武灵帝国/_env/jdk17/jdk-17.0.13+11"
export GRADLE_USER_HOME="C:/Users/jiami/Desktop/武灵帝国/_env/ghome"
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
"_env/ghome/wrapper/dists/gradle-8.8-bin/4u0rgm4geyrm56fyhoco0c9in/gradle-8.8/bin/gradle" build -x test --no-daemon
```
调已解压的 gradle 二进制绕过 wrapper 的 zip 锁；`JAVA_TOOL_OPTIONS` 防中文路径乱码；
增量 1~2 分钟，**一律后台跑**。services.gradle.org 不可达，wrapper 的 distributionUrl
已改到 `https://mirrors.cloud.tencent.com/gradle/`。
- ⚠️ 迁移后缓存可能变**全零字节**：症状 `Could not apply requested plugin
  [id: 'net.minecraftforge.gradle']`。`_env/ghome2` 有完好副本，覆盖即修。
  排查：看 `_env/ghome/caches/modules-2/**/*.jar` 文件头是否 `PK`。
- **核对产物**：编译用版本的 jar 在 `_env/ghome/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-<编译用的 forge 版>_mapped_official_1.20.1/`（当前 = 47.0.0）。
  reobf 后原版字段是 SRG 名（`Items.f_42420_`=WOODEN_SWORD、`f_42383_`=IRON_SWORD），
  翻名查 `.../mcp_config/1.20.1-20230612.114412/srg_to_official_1.20.1.tsrg`；
  枚举 `switch` 的 `tableswitch 1..N` 顺序 = 枚举声明顺序。`javap -c` 找常量是
  「改动真进 jar 了没」最快的手段。
- **改 lang 文件用 Python 读写**（`encoding='utf-8'`、`newline='\n'`），比 Edit 工具抗坏字节。
  体检：`open(p,'rb').read().decode('utf-8')`。

## 素材生成脚本（tools/，产出物一律自动生成、勿手改）
- 原版贴图/模型都从 `_env/ghome/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar` 读。
- `gen_bead_textures.py`：灵珠图标（一怪一图）+ `BeadModels.java` + 灵力回复量表
  （`MOB_RESTORE` 33 怪 → `item/BeadPower.java`）+ 验收页。
- `gen_emerald_gear.py`：绿宝石整套装备的贴图/模型/物品标签 + 两张验收图。
- ⚠️ 用 `%` 格式化生成 Java 源码时，文档注释里的百分号要写 `%%`。
- ⚠️ **生成 PNG 必须写 RGBA（colortype 6）**：只写 RGB 会丢 alpha、透明区变纯黑
  （游戏里＝每颗灵珠垫个黑方块）。校验 PNG 第 16~26 字节 IHDR，第 4 个值是 colortype。

## Forge 1.20.1 API 踩坑
- **没有** `IEntityWithComplexSpawn`；只有 `IEntityAdditionalSpawnData` +
  `NetworkHooks.getEntitySpawningPacket`。本项目改用 `PlayerEvent.StartTracking` + 自定义包。
- 创造物品栏事件 = `BuildCreativeModeTabContentsEvent`，判 tab 用 `getTabKey()`。
- `FloatArgumentType` 在 **Brigadier**（`com.mojang.brigadier.arguments.*`）。
- `LivingEntityRenderer` 朝向取自 `yBodyRot/yHeadRot`，不是 `render()` 的 yaw 参数。
- `CommandSourceStack#getPlayerOrException()` 只能在 Brigadier lambda 内用，抽成普通方法编译不过。
- **事件处理器不要重复注册**：`@Mod.EventBusSubscriber` 与 `modBus.addListener` 同时用会跑两遍
  （曾因此崩：`newSimpleChannel` 抛 Channel already registered）。
- `Entity#defineSynchedData()` 虽是 abstract，但基类构造已注册基础字段，直接继承 Entity 的实体留空安全。
- 1.20.1 的 `CustomModelData` 是**物品 NBT 键**（不是组件），模型写
  `{"predicate":{"custom_model_data":N}}`；`ItemProperties` 在 `net.minecraft.client.renderer.item`。
- 附魔字段名：`BLOCK_EFFICIENCY` / `BLOCK_FORTUNE` / `ALL_DAMAGE_PROTECTION` / `MOB_LOOTING` /
  `SWEEPING_EDGE` / `POWER_ARROWS` / `INFINITY_ARROWS`。
- ⚠️ **`en_us.json` 曾有 4 处「→」被写成 `e2 86 3f`**（2026-09-27 已修）：症状是 Edit 报
  「String to replace not found」、Python 按 utf-8 读直接抛 `UnicodeDecodeError`。

## 用户已确认的口径（交互 / 命名）
- **「凝聚」= 在世界上生成实体实物**（不是开界面、不是只写数据）：实物由 **武灵种类 + 当前境界** 决定。
- **Shift+M = 凝聚武灵**（`ClientTickHandler` 只发包）；**Shift+N = 打开升级面板**（0.2.20，
  纯客户端界面，数据由 `WuLingSyncPacket` 推）；两者都**不需要绑定器**。
  1.20.1 的 KeyMapping 不支持修饰键 → 键映射注册 M/N，Shift 在 tick 里单独判。
- **绑定器只是「选种类」的入口**（且仅限创造模式取得）：Shift+右键（创造绑定器则右键）
  打开 `WuLingChooseScreen` 十选一，种类自选、非随机；原文 1/100、1/10 只作稀有度标签。
- 自然修炼 = **使用绑定的武灵做对应动作**（伤害/挖掘/射箭/承伤/使用道具）。
- 灵珠物品名 = **`<怪物名>灵珠`**，品质不进名字、放 tooltip 副栏；图标 **一怪一图、不按品质分**。
- 灵力回复量 = **怪物基准 × 品质倍率**；基准在 `BeadPower.BASE`（4~25），
  倍率 `Config.restoreMultiplierFan..Ji`（1/1.5/2/3/4，须与生成器 `QUALITY_MULT` 一致）；
  表外模组怪按最大生命值兜底。**旧键 `restoreFan..restoreJi` 已废弃**（故意改名）。
- **突破物资可分批提交**：物资分「背包」与「缴纳池」，面板「提交物资」分批缴入、
  「突破」时自动补齐并消耗；缴纳池按目标大境界记账，目标变化或解绑时**原物退还**。

## 灵珠（第一部分）
掉落 → 倒地尸体实体 → 右键搜刮入包 → Shift+右键吸收。尸体 = 还原原怪 NBT + 复用原版渲染器 + 放倒。
- **退役怪物：不出灵珠、不出图标**（蠹虫 / 末影螨 / 恼鬼）→ 现 33 只。
  两处同步：`Config.NEVER_DROPS` 与生成器 `RETIRED`。
  **⚠️ 退役只删图、不删号**：`CustomModelData` 写在 NBT 里，序号前移会让已刷出的灵珠串图，
  所以 `BeadModels.SOURCES` 留空号占位（蠹虫 23、末影螨 8、恼鬼 27）。
- **排除名单写两层**：`Config.NEVER_DROPS`（代码硬名单，设定性排除）+ `drop.excludedMobs`
  （玩家配置项）。原因：Forge 配置**生成后不跟随代码默认值更新**。

## 境界上限与修炼速度（0.2.22 口径）
- **上限人人相同**：`Config.maxRealm`（默认 6 = 绿宝石），与灵珠品质无关。
  ~~旧的 `capFan..capJi`~~ 已删；键改名是故意的，老配置里的旧键不再被读。
- **灵珠品质只决定修炼速度**：`Config.cultivationBonusFan..Ji` = ×1 / 1.25 / 1.5 / 2 / 3，
  在 `WuLingData#addProgress` 里 `progress += amount * cultivationBonus` 一处收口；
  `WuLingData.bind(type, source, cultivationBonus)`（旧参数是 int cap）。
  创造绑定器不消耗灵珠 → ×1。
- 面板显示「修炼速度 ×N（上限人人相同）」；灵珠 tooltip 多一行「修炼速度 ×N」。
- 旧存档没有 `CultivationBonus` 字段 → 按 ×1.0 读入（想拿加成需重开武灵）。

## 绿宝石境界突破条件（0.2.13 口径 / 0.2.23 键名）
- 条件 = **`ALL_BEADS;10` + `minecraft:emerald_block;10`**（绿宝石块保留，用户明确）。
  **配置键 = `emerald_beads_any_quality`**（0.2.23 从 `netherite_to_emerald` 改名）。
- `ALL_BEADS` = **不限品质**；`ALL_JI_BEADS` = 旧口径只收极品（保留兼容）。
- ⚠️ **同一类坑第二次出现**：0.2.13 改了默认值却没改键名，老存档配置里照旧是
  「只收极品」，游戏里表现为「改了没生效」。改语义 → 必须换键名。
  `Config#logEffectiveRequirements()` 会在配置加载/重载时把六段突破物资的生效值打进日志，
  排查这类问题先看日志，别翻 toml。
- 覆盖来源 = 注册表 MONSTER 分类 **∪** `BeadModels.SOURCES` 去 `RETIRED`
  —— 必须用并集：疣猪兽是敌对但分类 CREATURE。再减 `NEVER_DROPS` ∪ `EXCLUDED_MOBS` ∪
  `BREAKTHROUGH_EXCLUDED_MOBS`（巨人/疣猪兽/幻术师/远古守卫者/流浪者/监守者）→ **27 种 × 10**。
- ⚠️ **缴纳池灵珠键带品质段** `bead|<来源>|<品质>`：共用一个键会「缴凡品退极品」＝白嫖。

## 灵力去向（0.2.15）
- **突破完全与灵力无关**（`breakthroughRequiresFullSpirit` 键已整个删除）。
- **灵力只在「凝聚武灵」时消耗**：`condenseSpiritCost` 默认 25；凝聚不再需要绑定器。
- 键名改名史：`breakthroughClearsSpirit` → `breakthroughConsumesSpirit`（默认 false）。
  ⚠️ 凡是「语义反转」的配置一律**换新键名**，否则老配置的旧值继续生效。

## 武灵凝聚物的强化（`WuLingType#empower`）
**0.2.26 起语义 = 总倍数**：`最终数值 = 该物品原版同款的数值 × 境界倍数`
（修饰符 = 原版值 × (倍数-1) × 配置百分比；`manifestAttackBonus`/`manifestArmorBonus` 默认 100）。
- **境界倍数**（`WuLingRealm#manifestMultiplier`）= 木 **3** / 石 **4** / 黄金 **8** / 玄铁 **9**
  / 钻石 **11** / 下界合金 **14** / 绿宝石 **15**。**每档碾压上一档 ≥1.43 倍**。
  剑最终：4→**12** →20 →32 →54 →77 →112 →**240**（+锋利 V ≈ 243）。
  倍数不等比是**故意的**：原版底子不等差（金剑 4 < 石剑 5），黄金档必须给到 8 才不倒挂。
- ⚠️ **攻击力要算偏移 1**：原版 tooltip「木剑 4」= 修正值 3 + 玩家基础攻击力 1，
  所以 `amplify(..., referenceOffset)` 对 ATTACK_DAMAGE 传 **1.0**、护甲/韧性传 **0.0**，
  「显示值 × 倍数」才严格成立。（旧字段名 `manifestScale` 已废弃）
- 绿宝石境界（倍数 15）：剑 243(含附魔) · 斧 **270** · 镐 210 · 铲 217.5 ·
  胸甲 护甲 **240** / 韧性 **180**。护甲值远超原版 clamp 20，仍只是「看得到、实际封顶」。
- **工具挖掘速度**（0.2.25 起）：原版没有「挖掘速度」属性/修饰符（只有 `Tier#getSpeed`），
  所以斧/镐/铲在非绿宝石分支附赠 `BLOCK_EFFICIENCY = min(境界序号+1, 5)`。
- ⚠️ **弓在钻石档以上会「追平」**（`POWER_ARROWS = min(序号+1,5)` 撞原版上限 5），
  水/火/红石/书没有战斗属性 → 这四类目前**没有境界阶梯**，待用户发话。
- **给物品加属性（已验证，别再想别的招）**：`ItemStack#addAttributeModifier(...)` +
  原版 `LivingEntity#handleEquipmentChanges` 自动应用 → **不用写自定义 Item 类**；
  原版数值从 `Item#getDefaultAttributeModifiers(slot)` 读。
  **UUID 每装备槽一个**且 MSB/LSB 非 0（否则 `ItemStack#getAttributeModifiers` 直接丢弃）。
  tooltip 附加行写 `display.Lore`。

## 绿宝石境界整套装备（0.2.15~0.2.17）
`emerald_sword/_axe/_pickaxe/_shovel/_chestplate`，都不是下界合金换皮。
- 外观 = 原版**钻石**贴图 + 绿色滤镜：青色 H≈167~178 → **H≈143**，饱和度 ×1.15、亮度不动；
  褐色木柄与纯白高光不进滤镜。
- `ModTiers.EMERALD`：耐久 **8000** / 速度 25 / 伤害档 +12 / 附魔度 40；
  **必须覆写 `getTag()` 指到 `Tiers.NETHERITE.getTag()`**，否则退到木制档、钻石级方块挖了不掉东西。
- `ModArmorMaterials.EMERALD`：耐久倍率 120 / 护甲 6·16·12·6 / 韧性 12 / 击退 0.6。
  护甲值只给 2 倍：原版把有效护甲 clamp 到 20，价值在韧性而非堆护甲。
- 凝聚附魔：剑 锋利 V·火焰 II·抢夺 III·横扫 III·耐久 III；斧/镐/铲 效率 V(+时运 III)；
  胸甲 保护 IV·荆棘 III·耐久 III。
- 盔甲层：`ArmorMaterial#getName()` 必须写 `wulingdiguo:emerald`，贴图
  `textures/models/armor/emerald_layer_1.png`（护腿才 layer_2）。
- ⚠️ **原版物品标签不自动收模组物品**：生成器同时写
  `data/minecraft/tags/items/{swords,axes,pickaxes,shovels,trimmable_armor}.json`，
  否则 `ItemTags.PICKAXES` 判定失败、挖矿不给修炼进度。
- 盔甲武灵目前**只凝聚胸甲一件**，四件套待用户发话。

## 开启武灵的入口
- **生存唯一途径：手持灵珠右键村民牧师**（`VillagerProfession.CLERIC`）→
  `ModEvents#onClericOpenWuLing` → `WuLingBinding#openByNpc`。种类由牧师**抽取**（`WuLingType.roll`）。
  **接管策略**：手里拿着灵珠才取消原版交互（不弹交易界面），空手完全不干涉。
  已拥有武灵则拒绝（摇头 + 冒火粒子）。成功 = 附魔台音效 + 村民「嗯哼」+ 符文/绿色粒子。
- **创造：绑定器自选种类**（绑定器仅限创造取得）。

## 怪物等级 / 武灵属性（`wuling/MonsterTier.java`）
- **口径：等级 = 属性**（「木属性」＝木阶，用该阶的剑打）。若要拆成
  「等级 + 剑/斧/水/火种类」两维度，加一层即可（文档开放项 6）。
- 只对 `Enemy` 且不在 `tierExcludedMobs`（默认 14 项：不近战的 + 不渲染手持物的）生效。
- 概率 = `tierChance`(0.10) × 几何衰减 `tierRarityRatio`(2.5) → 绿宝石阶 ≈ 1/4000；
  血量倍率 = 1 + 阶数 × `tierHealthBonusPerRealm`(0.5)。
- **三个必须记住的点**：①等级只掷一次 → 存 `Entity#getPersistentData()`（区块重载不重掷）；
  ②血量走**固定 UUID** 修饰符（先 remove 再 addPermanent）→ 永不叠血，补满血只在第一次；
  ③`Mob#setDropChance(MAINHAND, 0.0F)` **不掉剑**，否则刷出绿宝石剑＝绕开晋升线。
- 伤害不用自己写：`setItemSlot(MAINHAND, 剑)` 后由原版 `handleEquipmentChanges` 自动挂属性。
- 灵珠品质按来源怪最大生命值推算 → 高阶怪掉的珠子天然更好（隐性联动）。

## Forge 版本兼容性（0.2.24，重要惯例）
- **产物要支持 1.20.1 全部 132 个 Forge 版本（47.0.0~47.4.23）**。
  `gradle.properties` 的 `forge_version` **故意锁最低版 47.0.0**：用最低版编译，
  字节码只引用 47.0.0 就有的 API，天然向上兼容。想在最新版调试用
  `gradle runClient -Pforge_version=47.4.23`（命令行属性优先，不必改文件）。
- ⚠️ **主类构造函数必须无参**：47.0.0~47.3.7 的 `FMLModContainer#constructMod()`
  只有 `getDeclaredConstructor().newInstance()`；`47.3.10` 起才支持注入
  `FMLJavaModLoadingContext`（新版实现是「先试带参、NoSuchMethod 再退回无参」）。
  无参写法新老通吃 → 主类留 `public WulingEmpire()` + `FMLJavaModLoadingContext.get()`。
- ⚠️ **配置注册用 `ModLoadingContext.get().registerConfig(...)`**：
  47.0.0 的 `FMLJavaModLoadingContext` 是独立类（不继承 `ModLoadingContext`），
  没有 `registerConfig` 方法；继承关系是后期才加的。
- ⚠️ **`ResourceLocation` 只用两参构造函数**：`fromNamespaceAndPath` / `parse` /
  `withDefaultNamespace` / `bySeparator` / `tryBySeparator` 都是 Forge 后期
  backport 进 1.20.1 的（47.0.0 没有）；`tryParse` 两版都有，可用。
- 验证手段：把 `forge_version` 降到最低版编译（**编译器就是 API 清单**）+
  两端各起一次 `gradle runServer` 看是否 `Done`。

## 待办
- **第 4 部分（经济 NPC）、第 5 部分（终极突破）原文未下发**，需用户提供。
  第五部已知「一击 200+ 秒杀满血末影龙」，位置预留在 `EmeraldSwordItem`
  （0.2.26 现 **243** 伤害，已达成且有余量）。
- 待用户发话：牧师是否收绿宝石 / 大师级门槛；高阶怪要不要额外掉落；
  怪物「属性」是否与「等级」拆成两个维度；盔甲武灵四件套；
  **弓（钻石档以上追平）/ 水·火·红石·书（无战斗属性）要不要补境界阶梯**。
