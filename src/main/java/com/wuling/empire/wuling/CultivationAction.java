package com.wuling.empire.wuling;

/**
 * 修炼动作。
 *
 * 原文设定：小境界（前 → 中 → 后）没有渡劫，靠「使用武灵」的自然修炼进度累积，
 * 面板达到阈值即自动晋升。这里把「使用武灵」拆成五大类可监听的动作。
 */
public enum CultivationAction {

    /** 造成伤害（近战武灵） */
    ATTACK,
    /** 挖掘方块（工具类武灵） */
    MINE,
    /** 弓箭命中（远程武灵） */
    SHOOT,
    /** 承受伤害（护甲类武灵） */
    GUARD,
    /** 使用道具 / 交互（水、火、红石、书等法系武灵） */
    USE;

    public String translationKey() {
        return "action.wulingdiguo." + name().toLowerCase();
    }
}
