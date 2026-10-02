package com.wuling.empire.events;

import com.wuling.empire.Config;
import com.wuling.empire.capability.ModCapabilities;
import com.wuling.empire.item.RedstoneItems;
import com.wuling.empire.network.ModMessages;
import com.wuling.empire.network.RedstoneOpenPacket;
import com.wuling.empire.wuling.RedstoneGear;
import com.wuling.empire.wuling.WuLingData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.wuling.empire.WulingEmpire;

/**
 * 红石装备的运行时逻辑（2026-10-02 用户设定）。
 *
 * <h3>耗能点</h3>
 * 用户选了「攻击命中 + 挖掘方块 + 挨打受击」三项，加上明确要求的飞行：
 *
 * <table>
 *   <tr><td>攻击命中</td><td>{@code redstone.costAttack}（默认 10）</td><td>手持红石武器命中目标</td></tr>
 *   <tr><td>挖掘方块</td><td>{@code redstone.costMine}（默认 5）</td><td>手持红石工具挖掉一个方块</td></tr>
 *   <tr><td>挨打受击</td><td>{@code redstone.costHurt}（默认 10）</td><td>穿着红石护甲被打中</td></tr>
 *   <tr><td>飞行</td><td>{@code redstone.costFly}（默认 20/秒）</td><td>穿红石靴子飞行中，每 20 tick 扣一次</td></tr>
 * </table>
 *
 * <h3>耗尽后果</h3>
 * 「加成失效 + 不能飞 + 攻击大幅下降 + 速度减缓」，用户明确<b>不要药水效果</b> ——
 * 所以惩罚走属性修饰符（见 {@link RedstoneGear#applyEmptyPenalty}），
 * 装备加成则直接撤掉（见 {@link RedstoneGear#refresh}）。
 *
 * <h3>刷新节奏</h3>
 * 每 10 tick（0.5 秒）集中做一次：重算装备属性、核对耗尽惩罚、按需同步充能给客户端。
 * 逐 tick 做不仅没必要，还会让玩家属性被反复重算。
 */
@Mod.EventBusSubscriber(modid = WulingEmpire.MODID)
public final class RedstoneEvents {

    private RedstoneEvents() {
    }

    /** 上一次推给客户端的充能值 —— 用来避免每 10 tick 都无脑发包 */
    private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();

    // ===================== 右键：打开充能菜单 =====================

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!RedstoneItems.isGear(event.getItemStack())) {
            return;
        }
        player.getCapability(ModCapabilities.WU_LING).ifPresent(holder ->
                ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                        new RedstoneOpenPacket()));
        event.setCanceled(true);
    }

    // ===================== 攻击命中 / 挨打受击 =====================

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 攻击方：手持红石武器且真的打中了才扣
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && RedstoneGear.holdingWeapon(attacker)) {
            consume(attacker, Config.REDSTONE_COST_ATTACK.get());
        }
        // 受击方：穿着红石护甲
        if (event.getEntity() instanceof ServerPlayer victim
                && RedstoneGear.wearingArmor(victim)) {
            consume(victim, Config.REDSTONE_COST_HURT.get());
        }
    }

    // ===================== 挖掘方块 =====================

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (!RedstoneGear.holdingWeapon(player)) {
            return;
        }
        consume(player, Config.REDSTONE_COST_MINE.get());
    }

    // ===================== 逐 tick：飞行 + 定时刷新 =====================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        player.getCapability(ModCapabilities.WU_LING).ifPresent(holder -> {
            WuLingData data = holder.data();
            boolean creative = player.isCreative() || player.isSpectator();
            boolean hasGear = RedstoneGear.hasAnyGear(player);
            boolean canFly = RedstoneGear.hasBoots(player) && data.redstoneCharge() > 0;

            updateFlight(player, creative, canFly);

            // 飞行耗能：每秒扣一次，只在真的处于飞行状态时扣
            if (!creative && canFly && player.getAbilities().flying
                    && player.tickCount % 20 == 0) {
                consume(player, Config.REDSTONE_COST_FLY.get());
            }

            // 每 10 tick 集中刷新一次：装备属性、耗尽惩罚、充能同步
            if (player.tickCount % 10 == 0) {
                RedstoneGear.refresh(player, data, data.redstoneCharge());
                RedstoneGear.applyEmptyPenalty(player, hasGear && data.redstoneCharge() <= 0);
                syncCharge(player, data.redstoneCharge());
            }
        });
    }

    /**
     * 飞行权限：穿红石靴子且还有充能 → 允许像创造那样飞；否则收回。
     *
     * <p>创造 / 旁观玩家本来就允许飞，不能去改他们的能力值。
     */
    private static void updateFlight(ServerPlayer player, boolean creative, boolean canFly) {
        boolean want = creative || canFly;
        Abilities abilities = player.getAbilities();
        if (abilities.mayfly == want) {
            return;
        }
        abilities.mayfly = want;
        if (!want) {
            abilities.flying = false;
        }
        player.onUpdateAbilities();
    }

    // ===================== 辅助 =====================

    /** 扣充能；为 0 或没扣动（已经空了）时不发包 */
    private static void consume(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        player.getCapability(ModCapabilities.WU_LING).ifPresent(holder -> {
            int applied = holder.data().addRedstoneCharge(-amount);
            if (applied != 0) {
                ModMessages.sendRedstoneTo(player);
                LAST_SENT.put(player.getUUID(), holder.data().redstoneCharge());
            }
        });
    }

    /** 充能值与上次推送的不同才发，省掉大量无意义的小包 */
    private static void syncCharge(ServerPlayer player, int charge) {
        Integer last = LAST_SENT.get(player.getUUID());
        if (last != null && last == charge) {
            return;
        }
        LAST_SENT.put(player.getUUID(), charge);
        ModMessages.sendRedstoneTo(player);
    }

    // ===================== 登录 / 重生 / 换维度：补一次同步 =====================

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModMessages.sendRedstoneTo(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModMessages.sendRedstoneTo(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModMessages.sendRedstoneTo(player);
        }
    }

    /** 玩家下线时清掉缓存，避免 UUID 表无限增长 */
    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player != null) {
            LAST_SENT.remove(player.getUUID());
        }
    }
}
