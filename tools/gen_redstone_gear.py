# -*- coding: utf-8 -*-
"""《武灵帝国》红石装备贴图生成器（2026-10-02）。

需求（用户口径）：
    「用 9 个武灵红石粉可以合成红石锭，红石锭可以按照护甲合成方法合成红石战铠
      （包括：全套护甲 + 剑斧镐铲）」

做法与 `gen_emerald_gear.py` 一致：拿对应的原版**钻石**装备贴图当底子，
把「宝石那一族」的青绿像素（H≈120~210）整体搬到 **红石红**（H=0），
亮度 V 与描边 / 高光的层次原封不动 —— 形状、明暗、轮廓与钻石装备完全一致，
只是一眼就是红石色。木质 / 皮质的褐色部件（H≈38）与纯白高光（S=0）不进滤镜。

红石锭没有现成的「锭」形素材可用（武灵红石粉 = 红石粉的贴图），
所以拿原版**铁锭**贴图整体染红：铁锭是低饱和的灰白，色相判断对它无效，
只能整片重上色 —— 保留明暗层次，色相统一到红石红、饱和度拉满。

产出：
  textures/item/redstone_ingot.png          红石锭
  textures/item/redstone_sword.png          剑
  textures/item/redstone_axe.png            斧
  textures/item/redstone_pickaxe.png        镐
  textures/item/redstone_shovel.png         铲
  textures/item/redstone_helmet.png         头盔（物品图标）
  textures/item/redstone_chestplate.png     胸甲（物品图标）
  textures/item/redstone_leggings.png       护腿（物品图标）
  textures/item/redstone_boots.png          靴子（物品图标）
  textures/models/armor/redstone_layer_1.png  头 / 胸 / 靴（穿在身上时的盔甲层）
  textures/models/armor/redstone_layer_2.png  护腿（穿在身上时的盔甲层）
  models/item/redstone_*.json               9 份物品模型
  data/minecraft/tags/items/*.json          把红石装备挂进原版标签（与绿宝石装备并列，
                                            注意脚本重写这些文件，必须把绿宝石的一起写上）
  docs/redstone_gear_preview.png            验收图（原版 | 红石）
"""

import io
import json
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_bead_textures as G  # noqa: E402  复用它的 png_read / png_write / hsv 工具

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CLIENT_JAR = os.path.join(
    ROOT, "_env/ghome/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar")

ASSETS = "src/main/resources/assets/wulingdiguo"
ITEM_DIR = os.path.join(ROOT, ASSETS, "textures/item")
ARMOR_DIR = os.path.join(ROOT, ASSETS, "textures/models/armor")
DOCS = os.path.join(ROOT, "docs")

VN_ITEM = "assets/minecraft/textures/item/%s.png"
VN_ARMOR = "assets/minecraft/textures/models/armor/%s.png"

# 「宝石一族」= 要染成红色的部件（钻石的青绿）
ITEMS = [
    ("diamond_sword", "redstone_sword"),
    ("diamond_axe", "redstone_axe"),
    ("diamond_pickaxe", "redstone_pickaxe"),
    ("diamond_shovel", "redstone_shovel"),
    ("diamond_helmet", "redstone_helmet"),
    ("diamond_chestplate", "redstone_chestplate"),
    ("diamond_leggings", "redstone_leggings"),
    ("diamond_boots", "redstone_boots"),
]

# 红石锭：整体染色的那种（源贴图, 产出名）
INGOTS = [
    ("iron_ingot", "redstone_ingot"),
]

# 穿在身上的盔甲层：头 / 胸 / 靴走 layer_1，护腿走 layer_2
ARMOR_LAYERS = [
    ("diamond_layer_1", "redstone_layer_1"),
    ("diamond_layer_2", "redstone_layer_2"),
]

# ⚠️ 这三个文件会被本脚本整体重写，所以**必须把绿宝石装备一起列上**，
# 否则跑一次就把绿宝石从标签里挤掉了（工具就不再算剑 / 斧 / 镐 / 铲）。
TAGS = {
    "swords": ["emerald_sword", "redstone_sword"],
    "axes": ["emerald_axe", "redstone_axe"],
    "pickaxes": ["emerald_pickaxe", "redstone_pickaxe"],
    "shovels": ["emerald_shovel", "redstone_shovel"],
    "trimmable_armor": ["emerald_helmet", "emerald_chestplate",
                        "emerald_leggings", "emerald_boots",
                        "redstone_helmet", "redstone_chestplate",
                        "redstone_leggings", "redstone_boots"],
}

TAG_DIR = os.path.join(ROOT, "src/main/resources/data/minecraft/tags/items")

# 红石的色相：正红（H=0）
REDSTONE_HUE = 0.0
# 只有这个色相区间的像素会被「染红」（钻石的青绿宝石）
GEM_HUE_MIN = 120.0
GEM_HUE_MAX = 210.0
# 红石比钻石艳
SAT_BOOST = 1.25
# 红石锭（铁锭整体上色）的最低饱和度
INGOT_MIN_SAT = 0.70


def tint(src_pixels):
    """把青色的钻石部件染成红石红，其余像素不动。返回 (新像素, 染色数, 不透明数)。"""
    out = []
    changed = 0
    opaque = 0
    for r, g, b, a in src_pixels:
        if a == 0:
            out.append((r, g, b, a))
            continue
        opaque += 1
        h, s, v = G.rgb_to_hsv(r, g, b)
        if GEM_HUE_MIN <= h <= GEM_HUE_MAX and s > 0.05:
            h = REDSTONE_HUE
            s = min(1.0, s * SAT_BOOST)
            changed += 1
        rr, gg, bb = G.hsv_to_rgb(h, s, v)
        out.append((rr, gg, bb, a))
    return out, changed, opaque


def tint_ingot(src_pixels):
    """整体上色：所有不透明像素统一到红石红，保留明暗。返回 (新像素, 不透明数)。"""
    out = []
    opaque = 0
    for r, g, b, a in src_pixels:
        if a == 0:
            out.append((r, g, b, a))
            continue
        opaque += 1
        h, s, v = G.rgb_to_hsv(r, g, b)
        s = max(s, INGOT_MIN_SAT) * SAT_BOOST
        v = min(1.0, v * 0.92)
        rr, gg, bb = G.hsv_to_rgb(REDSTONE_HUE, min(1.0, s), v)
        out.append((rr, gg, bb, a))
    return out, opaque


def scale(pixels, w, h, k):
    """最近邻放大 k 倍（硬像素，不糊）"""
    out = []
    for y in range(h):
        row = pixels[y * w:(y + 1) * w]
        for _ in range(k):
            for p in row:
                out.extend([p] * k)
    return out


def compose_rows(rows, gap=6, cell=8):
    """把若干「行」的贴图拼成一张验收图，透明处铺棋盘格。"""
    width = max(sum(w * k for w, _, _ in panels) + gap * (len(panels) + 1)
                for k, panels in rows)
    heights = [max(h for _, h, _ in panels) * k + gap for k, panels in rows]
    W = width
    H = sum(heights) + gap
    canvas = [(0, 0, 0, 0)] * (W * H)
    for y in range(H):
        for x in range(W):
            tile = ((x - gap) // cell + (y - gap) // cell) & 1
            canvas[y * W + x] = (205, 205, 205, 255) if tile else (232, 232, 232, 255)

    y0 = gap
    for i, (k, panels) in enumerate(rows):
        x0 = gap
        for w, h, px in panels:
            big = scale(px, w, h, k)
            for y in range(h * k):
                for x in range(w * k):
                    p = big[y * w * k + x]
                    if p[3] == 0:
                        continue
                    canvas[(y0 + y) * W + x0 + x] = p
            x0 += w * k + gap
        y0 += heights[i]
    return W, H, canvas


def write_models():
    """9 件物品各写一份模型 JSON（工具 / 武器手持、盔甲与锭平铺）"""
    for src, out in ITEMS + INGOTS:
        parent = "item/handheld" if ("sword" in out or "axe" in out
                                     or "pickaxe" in out or "shovel" in out) else "item/generated"
        path = os.path.join(ROOT, ASSETS, "models/item/%s.json" % out)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write('{\n  "parent": "%s",\n  "textures": {\n    "layer0": "wulingdiguo:item/%s"\n  }\n}\n'
                    % (parent, out))
        print("  %-20s -> models/item/%s.json  (parent=%s)" % (out, out, parent))


def write_tags():
    """把红石装备（连同绿宝石装备）挂进原版物品标签"""
    os.makedirs(TAG_DIR, exist_ok=True)
    for tag, items in TAGS.items():
        path = os.path.join(TAG_DIR, "%s.json" % tag)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump({"replace": False,
                       "values": ["wulingdiguo:%s" % i for i in items]},
                      f, indent=2, ensure_ascii=False)
            f.write("\n")
        print("  minecraft:%-18s <- %s" % (tag, ", ".join(items)))


def main():
    z = zipfile.ZipFile(CLIENT_JAR)
    item_rows = []

    for src, out in ITEMS:
        w, h, src_px = G.png_read(io.BytesIO(z.read(VN_ITEM % src)))
        tinted, changed, opaque = tint(src_px)
        G.png_write(os.path.join(ITEM_DIR, out + ".png"), w, h, tinted)
        print("  %-20s %2d×%-2d  染色 %3d/%-3d -> textures/item/%s.png"
              % (src, w, h, changed, opaque, out))
        item_rows.append((8, [(w, h, src_px), (w, h, tinted)]))

    for src, out in INGOTS:
        w, h, src_px = G.png_read(io.BytesIO(z.read(VN_ITEM % src)))
        tinted, opaque = tint_ingot(src_px)
        G.png_write(os.path.join(ITEM_DIR, out + ".png"), w, h, tinted)
        print("  %-20s %2d×%-2d  整体上色 %3d -> textures/item/%s.png"
              % (src, w, h, opaque, out))
        item_rows.append((8, [(w, h, src_px), (w, h, tinted)]))

    write_models()
    write_tags()

    armor_rows = []
    for layer_src_name, layer_out_name in ARMOR_LAYERS:
        w, h, layer_src = G.png_read(io.BytesIO(z.read(VN_ARMOR % layer_src_name)))
        layer_out, changed, opaque = tint(layer_src)
        G.png_write(os.path.join(ARMOR_DIR, layer_out_name + ".png"), w, h, layer_out)
        print("  %-20s %2d×%-2d  染色 %3d/%-3d -> textures/models/armor/%s.png"
              % (layer_src_name, w, h, changed, opaque, layer_out_name))
        armor_rows.append((4, [(w, h, layer_src), (w, h, layer_out)]))

    W, H, canvas = compose_rows(item_rows)
    G.png_write(os.path.join(DOCS, "redstone_gear_preview.png"), W, H, canvas)
    print("验收图（每行：左=原版钻石/铁 右=红石）-> docs/redstone_gear_preview.png  %d×%d" % (W, H))


if __name__ == "__main__":
    main()
