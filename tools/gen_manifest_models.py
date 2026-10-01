# -*- coding: utf-8 -*-
"""《武灵帝国》武灵凝聚物 —— 低境界物品模型生成器。

2026-10-01 用户口径「武灵物品重新注册为新物品」：每个大境界的每种武灵
都是一件独立物品（见 src/.../item/ManifestItems.java，共 7 × 13 = 91 件）。

其中 **绿宝石档 8 件**是单独设计的一档实物（钻石贴图染色），模型由
`gen_emerald_gear.py` 生成；剩下 **83 件**直接用原版同名物品的模型当 parent
（贴图仍是原版的），本脚本负责把它们写出来。

产出：assets/wulingdiguo/models/item/<境界>_<种类>.json
      内容形如 {"parent": "minecraft:item/wooden_sword"}

⚠️ 这里只生成模型（贴图引用），**不生成贴图** —— 低境界的外观就是原版外观。
"""

import io
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL_DIR = os.path.join(ROOT, "src/main/resources/assets/wulingdiguo/models/item")

# 大境界 key（与 WuLingRealm#key 一致）
REALMS = ["wood", "stone", "gold", "meteor_iron", "diamond", "netherite", "emerald"]

# 工具 / 护甲：绿宝石档是单独设计的一档实物（贴图染色），模型由
# gen_emerald_gear.py 生成，所以这里只做前 6 个境界。
TOOL_REALMS = ["wood", "stone", "gold", "meteor_iron", "diamond", "netherite"]

# 工具材质 → 原版物品模型前缀
#   木 wooden / 石 stone / 黄金 golden / 玄铁 iron / 钻石 diamond / 下界合金 netherite
TOOL_PREFIX = {
    "wood": "wooden",
    "stone": "stone",
    "gold": "golden",
    "meteor_iron": "iron",
    "diamond": "diamond",
    "netherite": "netherite",
}

# 护甲材质 → 原版物品模型前缀
#   木(皮革) leather / 石(锁链) chainmail / 黄金 golden / 玄铁 iron / 钻石 diamond / 下界合金 netherite
ARMOR_PREFIX = {
    "wood": "leather",
    "stone": "chainmail",
    "gold": "golden",
    "meteor_iron": "iron",
    "diamond": "diamond",
    "netherite": "netherite",
}

# 工具 / 武器类：<前缀>_<部位>
TOOL_TYPES = ["sword", "axe", "pickaxe", "shovel"]
# 护甲：<前缀>_<部位>
ARMOR_TYPES = ["helmet", "chestplate", "leggings", "boots"]
# 抽象类：所有境界共用同一个原版模型
ABSTRACT_TYPES = {
    "bow": "bow",
    "water": "water_bucket",
    "fire": "blaze_rod",
    "redstone": "redstone",
    "book": "enchanted_book",
}

GREEN = "\033[32m"
GREY = "\033[90m"
RESET = "\033[0m"


def write_model(item_id, vanilla_parent):
    """写一份以原版模型为 parent 的模型 json；已存在（绿宝石档）则跳过"""
    path = os.path.join(MODEL_DIR, item_id + ".json")
    if os.path.exists(path):
        return False
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write('{\n  "parent": "minecraft:item/%s"\n}\n' % vanilla_parent)
    return True


def main():
    os.makedirs(MODEL_DIR, exist_ok=True)
    made = 0
    for realm in TOOL_REALMS:
        tp = TOOL_PREFIX[realm]
        ap = ARMOR_PREFIX[realm]
        pairs = []
        for t in TOOL_TYPES:
            pairs.append(("%s_%s" % (realm, t), "%s_%s" % (tp, t)))
        for t in ARMOR_TYPES:
            pairs.append(("%s_%s" % (realm, t), "%s_%s" % (ap, t)))
        for item_id, vanilla in pairs:
            if write_model(item_id, vanilla):
                made += 1
                print("  %s%-22s%s <- minecraft:item/%s" % (GREEN, item_id, RESET, vanilla))

    # 弓 / 水 / 火 / 红石 / 书：所有境界都是同一个原版外观，绿宝石档也不例外
    # （绿宝石档只有工具与护甲是染色实物）
    for realm in REALMS:
        for t, vanilla in ABSTRACT_TYPES.items():
            item_id = "%s_%s" % (realm, t)
            if write_model(item_id, vanilla):
                made += 1
                print("  %s%-22s%s <- minecraft:item/%s" % (GREEN, item_id, RESET, vanilla))

    print()
    print("新增 %d 份模型 -> %s" % (made, os.path.relpath(MODEL_DIR, ROOT)))
    print("%s（绿宝石档的 8 份是单独设计的贴图模型，由 gen_emerald_gear.py 生成，本次跳过）%s"
          % (GREY, RESET))


if __name__ == "__main__":
    main()
