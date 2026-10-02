package com.wuling.empire.events;

import com.wuling.empire.Config;
import com.wuling.empire.capability.ModCapabilities;
import com.wuling.empire.capability.SpiritPowerProvider;
import com.wuling.empire.capability.WuLingProvider;
import com.wuling.empire.entity.CorpseEntity;
import com.wuling.empire.item.SpiritBeadDrops;
import com.wuling.empire.wuling.BreakthroughRequirement;
import com.wuling.empire.wuling.CultivationAction;
import com.wuling.empire.wuling.MonsterTier;
import com.wuling.empire.wuling.RottenFleshRule;
import com.wuling.empire.wuling.WuLingBinding;
import com.wuling.empire.wuling.WuLingRealm;
import com.wuling.empire.wuling.WuLingType;
import com.wuling.empire.network.ModMessages;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * 游戏内事件处理：灵力计算、灵珠掉落、数据同步、调试指令。
 */
public class ModEvents {

    // ===================== 灵力 Capability 挂载 =====================

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ModCapabilities.SPIRIT_POWER_ID, new SpiritPowerProvider());
            event.addCapability(ModCapabilities.WU_LING_ID, new WuLingProvider());
        }
    }

    // ===================== 第二部：武灵修炼进度 =====================

    /** 造成伤害（近战武灵：手中的武器要与武灵种类对上） */
    @SubscribeEvent
    public void onCultivateAttack(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        // 把目标一起带进去 —— 腐肉武灵只认僵尸类目标（见 WuLingType#acceptsTarget）
        cultivate(player, Config.PROGRESS_ATTACK.get() * event.getAmount(),
                CultivationAction.ATTACK, target);
    }

    /** 弓箭命中（远程武灵） */
    @SubscribeEvent
    public void onCultivateShoot(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!(arrow.getOwner() instanceof Player player)) {
            return;
        }
        cultivate(player, Config.PROGRESS_SHOOT.get(), CultivationAction.SHOOT);
    }

    /** 承受伤害（护甲类武灵） */
    @SubscribeEvent
    public void onCultivateGuard(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        cultivate(player, Config.PROGRESS_GUARD.get(), CultivationAction.GUARD);
    }

    /** 挖掘方块（工具类武灵） */
    @SubscribeEvent
    public void onCultivateMine(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) {
            return;
        }
        cultivate(player, Config.PROGRESS_MINE.get(), CultivationAction.MINE);
    }

    /** 使用道具（水 / 火 / 红石 / 书 等法系武灵） */
    @SubscribeEvent
    public void onCultivateUse(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        cultivate(player, Config.PROGRESS_USE.get(), CultivationAction.USE);
    }

    private void cultivate(Player player, double amount, CultivationAction action) {
        cultivate(player, amount, action, null);
    }

    /**
     * @param target 本次动作作用的目标；没有具体目标（挖掘 / 承伤 / 用道具）时传 null。
     *               腐肉武灵靠它把「打僵尸才算修炼」这条规则落到实处
     */
    private void cultivate(Player player, double amount, CultivationAction action,
                           LivingEntity target) {
        player.getCapability(ModCapabilities.WU_LING).ifPresent(wuLing -> {
            // 只有真的「在使用自己的武灵」才计入修炼进度
            if (!wuLing.data().type().countsAs(player.getMainHandItem(), player.getInventory())) {
                return;
            }
            // 腐肉武灵：打别的怪不给进度，必须打僵尸
            if (target != null && !wuLing.data().type().acceptsTarget(target)) {
                return;
            }
            boolean promoted = wuLing.addProgress(amount, action);
            if (promoted && player instanceof ServerPlayer serverPlayer) {
                player.sendSystemMessage(Component.translatable("message.wulingdiguo.stage_up",
                        ClientSafe.realmLabel(wuLing.data())));
                // 小境界不改实物档次，但每次升级都顺手同步一遍 ——
                // 若手上已是当前境界那件就什么都不做（见 WuLingBinding#refreshManifestItems）
                WuLingBinding.refreshManifestItems(player);
                ModMessages.sendWuLingTo(serverPlayer);
            }
        });
    }

    // ===================== 腐肉武灵：击杀僵尸计数 =====================

    /**
     * 击杀僵尸类 → 累计击杀数 +1。
     *
     * <p>这是腐肉武灵<b>唯一的突破货币</b>（用户设定「升级不消耗材料，
     * 只消耗击杀僵尸的量」），所以只认「玩家亲手打死」的僵尸 ——
     * 别的生物互殴致死、或者僵尸自己烧死的都不算。
     */
    @SubscribeEvent
    public void onZombieSlain(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide()) {
            return;
        }
        if (!RottenFleshRule.isZombieFamily(dead)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        player.getCapability(ModCapabilities.WU_LING).ifPresent(wuLing -> {
            if (wuLing.data().type() != WuLingType.ROTTEN_FLESH) {
                return;
            }
            wuLing.data().addZombieKills(1);
            // 击杀本身也算一次修炼（走完整流程，会乘灵珠品质的修炼速度加成）
            boolean promoted = wuLing.addProgress(
                    Config.ROTTEN_FLESH_KILL_PROGRESS.get(), CultivationAction.ATTACK);
            if (player instanceof ServerPlayer serverPlayer) {
                if (promoted) {
                    player.sendSystemMessage(Component.translatable("message.wulingdiguo.stage_up",
                            ClientSafe.realmLabel(wuLing.data())));
                }
                ModMessages.sendWuLingTo(serverPlayer);
            }
        });
    }

    /** 玩家维度的提示文案构造（避免在服务端引用客户端类） */
    private static final class ClientSafe {
        static Component realmLabel(com.wuling.empire.wuling.WuLingData data) {
            return Component.translatable(data.realm().translationKey())
                    .append(" · ")
                    .append(Component.translatable(data.stage().translationKey()));
        }
    }

    // ===================== 怪物等级（武灵属性，2026-09-27） =====================

    /**
     * 敌对生物进入世界时掷一次「武灵等级」。
     * 自然刷怪 / 刷怪笼 / 刷怪蛋 / 区块加载都会走到这里，
     * 判定与幂等处理都在 {@link MonsterTier#onJoin} 里。
     */
    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Mob mob) {
            MonsterTier.onJoin(mob);
        }
    }

    // ===================== 村民牧师：抽取开启武灵 =====================

    /**
     * 手持灵珠右键<b>村民牧师</b> → 请牧师开启武灵，修炼方向<b>抽取</b>而非自选。
     *
     * <p>只在这种「递珠子」的交互上接管（取消原版交互，避免交易界面盖上来）；
     * 空手或没拿灵珠时完全不干涉，牧师交易照常。
     */
    @SubscribeEvent
    public void onClericOpenWuLing(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getTarget() instanceof Villager villager)) {
            return;
        }
        if (villager.getVillagerData().getProfession() != VillagerProfession.CLERIC) {
            return;
        }
        if (WuLingBinding.openByNpc(event.getEntity(), villager)) {
            event.setCanceled(true);
        }
    }

    // ===================== 怪物死亡 -> 掉落灵珠 =====================

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Level level = dead.level();
        if (level.isClientSide()) {
            return;
        }

        // 带武灵等级的怪物：等级越高越稀有，打死谁就报给谁一声
        WuLingRealm tier = MonsterTier.tierOf(dead);
        if (tier != null && event.getSource().getEntity() instanceof Player killer) {
            killer.sendSystemMessage(Component.translatable("message.wulingdiguo.tier_slain",
                    Component.translatable(tier.translationKey())));
        }

        // Part 3：末影龙死亡生成专属尸体（末影龙并非 Enemy，需单独处理，且先于下方 Enemy 判断以免漏掉）
        // 2026-10-02：末影龙是唯一还留尸体的怪（大命中箱 + 紫色光柱地标），
        // 用户明确要求保留；普通怪改为直接掉灵珠，见下。
        if (dead instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            CorpseEntity corpse = CorpseEntity.create(level, dead);
            level.addFreshEntity(corpse);
            return;
        }
        // 只对敌对生物生效
        if (!(dead instanceof Enemy)) {
            return;
        }
        // 排除名单：设定硬名单（NEVER_DROPS，不受 config 影响）+ 可配置的 EXCLUDED_MOBS
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType());
        if (key != null && (Config.NEVER_DROPS.contains(key.toString())
                || Config.EXCLUDED_MOBS.get().contains(key.toString()))) {
            return;
        }

        // 2026-10-02 用户口径「怪物掉落灵珠改为直接掉落而不是尸体」：
        // 不再留尸体，就地掉一颗灵珠，走过去立刻就能捡。
        SpiritBeadDrops.dropAt(level, dead, SpiritBeadDrops.roll(dead));
    }

    // ===================== 灵力自然回复 + 定期同步 =====================

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        serverPlayer.getCapability(ModCapabilities.SPIRIT_POWER).ifPresent(power -> {
            power.tick();
            if (serverPlayer.tickCount % 5 == 0) {
                ModMessages.sendSpiritTo(serverPlayer);
            }
            if (serverPlayer.tickCount % 20 == 0) {
                ModMessages.sendWuLingTo(serverPlayer);
            }
        });
    }

    // ===================== 数据同步时机 =====================

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ModMessages.sendSpiritTo(serverPlayer);
            ModMessages.sendWuLingTo(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ModMessages.sendSpiritTo(serverPlayer);
            ModMessages.sendWuLingTo(serverPlayer);
        }
    }

    /** 死亡重生 / 跨维度：继承灵力与武灵数据 */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(ModCapabilities.SPIRIT_POWER).ifPresent(old -> {
            event.getEntity().getCapability(ModCapabilities.SPIRIT_POWER).ifPresent(fresh -> {
                fresh.deserialize(old.serialize());
            });
        });
        event.getOriginal().getCapability(ModCapabilities.WU_LING).ifPresent(old -> {
            event.getEntity().getCapability(ModCapabilities.WU_LING).ifPresent(fresh -> {
                fresh.deserialize(old.serialize());
            });
        });
    }

    // ===================== 尸体外观同步（只剩末影龙尸体） =====================

    /** 玩家进入尸体追踪范围时，补发外观数据（NBT 太大，不适合走 EntityData） */
    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof CorpseEntity corpse)) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ModMessages.sendCorpseDataTo(serverPlayer, corpse);
        }
    }

    // ===================== 右键搜刮尸体（只剩末影龙尸体） =====================

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof CorpseEntity corpse)) {
            return;
        }
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        if (!corpse.hasBeads()) {
            return;
        }

        Player player = event.getEntity();
        List<ItemStack> beads = corpse.takeBeads();

        int total = 0;
        for (ItemStack stack : beads) {
            ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
            total += stack.getCount();
        }

        player.sendSystemMessage(Component.translatable("message.wulingdiguo.corpse_looted", total));
        level.playSound(null, corpse.getX(), corpse.getY(), corpse.getZ(),
                SoundEvents.ITEM_PICKUP, player.getSoundSource(), 0.6F, 1.0F);

        // 末影龙尸体作为光柱地标保留（取完珠尸体不消失）
        if (!corpse.isDragon()) {
            corpse.discard();
        }
        event.setCanceled(true);
    }

    // ===================== 调试指令 =====================

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wuling")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("spirit")
                                .then(Commands.literal("get").executes(ctx -> {
                                    ServerPlayer target = ctx.getSource().getPlayerOrException();
                                    target.getCapability(ModCapabilities.SPIRIT_POWER).ifPresent(power ->
                                            feedback(ctx.getSource(), Component.translatable(
                                                    "message.wulingdiguo.spirit_query",
                                                    String.format("%.1f", power.getSpirit()),
                                                    String.format("%.1f", Config.maxSpirit()))));
                                    return 1;
                                }))
                                .then(Commands.literal("set").then(Commands.argument("value",
                                        FloatArgumentType.floatArg(0.0F, Config.maxSpirit())).executes(ctx -> {
                                    ServerPlayer target = ctx.getSource().getPlayerOrException();
                                    float value = FloatArgumentType.getFloat(ctx, "value");
                                    target.getCapability(ModCapabilities.SPIRIT_POWER).ifPresent(power -> {
                                        power.setSpirit(value);
                                        feedback(ctx.getSource(), Component.translatable(
                                                "message.wulingdiguo.spirit_set", String.format("%.1f", value)));
                                        ModMessages.sendSpiritTo(target);
                                    });
                                    return 1;
                                })))
                                .then(Commands.literal("unbind").executes(ctx -> {
                                    ServerPlayer target = ctx.getSource().getPlayerOrException();
                                    target.getCapability(ModCapabilities.WU_LING).ifPresent(holder -> {
                                        // 已缴纳的突破物资先退还，避免凭空消失
                                        BreakthroughRequirement.refundAll(target, holder.data());
                                        holder.data().unbind();
                                        ModMessages.sendWuLingTo(target);
                                        feedback(ctx.getSource(), Component.translatable(
                                                "message.wulingdiguo.unbind_ok"));
                                    });
                                    return 1;
                                }))
                                .then(Commands.literal("add").then(Commands.argument("value",
                                        FloatArgumentType.floatArg(-Config.maxSpirit(), Config.maxSpirit())).executes(ctx -> {
                                    ServerPlayer target = ctx.getSource().getPlayerOrException();
                                    float value = FloatArgumentType.getFloat(ctx, "value");
                                    target.getCapability(ModCapabilities.SPIRIT_POWER).ifPresent(power -> {
                                        power.addSpirit(value);
                                        feedback(ctx.getSource(), Component.translatable(
                                                "message.wulingdiguo.spirit_set",
                                                String.format("%.1f", power.getSpirit())));
                                        ModMessages.sendSpiritTo(target);
                                    });
                                    return 1;
                                })))
                        )
        );
    }

    private static void feedback(CommandSourceStack source, Component message) {
        source.sendSystemMessage(message);
    }
}
