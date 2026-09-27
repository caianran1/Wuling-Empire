package com.wuling.empire.wuling;

/**
 * 小境界：前期 / 中期 / 后期
 */
public enum WuLingStage {

    EARLY("early"),
    MID("mid"),
    LATE("late");

    private final String key;

    WuLingStage(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public String translationKey() {
        return "wuling.stage." + key;
    }

    public static WuLingStage byOrdinal(int ordinal) {
        WuLingStage[] values = values();
        if (ordinal < 0) {
            return values[0];
        }
        if (ordinal >= values.length) {
            return values[values.length - 1];
        }
        return values[ordinal];
    }
}
