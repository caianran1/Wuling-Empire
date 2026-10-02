package com.wuling.empire.wuling;

import com.wuling.empire.Config;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Set;

/**
 * 腐肉武灵的专属规则（2026-10-02 用户设定）。
 *
 * <p>把「哪些怪算僵尸」「召唤几只」「能不能飞」这三条集中在这里，
 * 免得散落到事件、面板、实体里各写一份，改一处漏一处。
 */
public final class RottenFleshRule {

    private RottenFleshRule() {
    }

    /**
     * 「僵尸类全家」—— 用户口径是「只要沾僵尸就算数」。
     *
     * <p>小僵尸不是独立实体类型，是 {@link EntityType#ZOMBIE} 的幼年形态，
     * 所以丧尸宝宝天然包含在内。
     */
    private static final Set<EntityType<?>> ZOMBIE_FAMILY = Set.of(
            EntityType.ZOMBIE,
            EntityType.HUSK,
            EntityType.DROWNED,
            EntityType.ZOMBIE_VILLAGER,
            EntityType.ZOMBIFIED_PIGLIN
    );

    /** 是不是僵尸类（修炼与击杀统计都认这一个判定） */
    public static boolean isZombieFamily(Entity entity) {
        return entity != null && ZOMBIE_FAMILY.contains(entity.getType());
    }

    /**
     * 按大境界算的召唤数量：<b>每有一个大境界 5 只</b>
     * （木 5 · 石 10 · 黄金 15 · 玄铁 20 · 钻石 25 · 下界合金 30 · 绿宝石 35）。
     */
    public static int summonsFor(int realmOrdinal) {
        int perRealm = Config.ROTTEN_FLESH_ZOMBIES_PER_REALM.get();
        return Math.max(1, (Math.max(0, realmOrdinal) + 1) * perRealm);
    }

    /** 该境界召唤出的僵尸能不能飞（用户设定：钻石境界起） */
    public static boolean canFly(int realmOrdinal) {
        return realmOrdinal >= Config.ROTTEN_FLESH_FLY_REALM.get();
    }
}
