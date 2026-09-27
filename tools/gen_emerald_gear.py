# -*- coding: utf-8 -*-
"""《武灵帝国》绿宝石装备贴图生成器。

需求（用户口径 2026-09-26）：
    「绿宝石境界的剑是单独设计的，钻石剑上有绿色滤镜，形成绿宝石剑。」
    「其他全做」—— 斧 / 镐 / 铲 / 盔甲一并做绿宝石版。

做法：拿对应的原版**钻石**装备贴图当底子，把「宝石那一族」的青色像素
（色相 H≈167~178 的青绿）整体搬到 **绿宝石绿**（H≈143），
亮度 V 与描边 / 高光的层次原封不动 —— 所以形状、明暗、轮廓和原版完全一致，
只是一眼就是绿宝石色。木质 / 皮质的褐色部件（H≈38）与纯白高光（S=0）
**不进滤镜，保持原样**，维持原版物品的观感。

贴图来源是原版资源包（client-extra.jar，里面就是 assets/minecraft/**），
和灵珠生成器 `gen_bead_textures.py` 取同一个 jar。

产出：
  textures/item/emerald_sword.png          剑
  textures/item/emerald_axe.png            斧
  textures/item/emerald_pickaxe.png        镐
  textures/item/emerald_shovel.png         铲
  textures/item/emerald_chestplate.png     胸甲（物品图标）
  textures/models/armor/emerald_layer_1.png  胸甲（穿在身上时的盔甲层）
  models/item/emerald_*.json               5 份物品模型
  data/minecraft/tags/items/*.json         把装备挂进原版标签（swords/axes/…）
  docs/emerald_gear_preview.png            物品图标 8 倍放大验收图（原版 | 绿宝石）
  docs/emerald_armor_layer_preview.png     盔甲层贴图 4 倍放大验收图（原版 | 绿宝石）
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

# 要生成的物品图标：(原版物品贴图名, 产出名)
ITEMS = [
    ("diamond_sword", "emerald_sword"),
    ("diamond_axe", "emerald_axe"),
    ("diamond_pickaxe", "emerald_pickaxe"),
    ("diamond_shovel", "emerald_shovel"),
    ("diamond_chestplate", "emerald_chestplate"),
]

# 穿在身上的盔甲层（胸甲用 layer_1；只有护腿才用 layer_2）
ARMOR_LAYER_SRC = "diamond_layer_1"
ARMOR_LAYER_OUT = "emerald_layer_1"

# 原版物品标签**不会自动收模组物品**，必须显式把自己加进去。
# 不加的后果：绿宝石工具不算「剑 / 斧 / 镐 / 铲」——
# 武灵修炼的持械判定（WuLingType.HoldRequirement 读的就是那几个 ItemTags）
# 会认不出来，玩家拿着绿宝石镐挖矿不给修炼进度；盔甲纹饰也上不去。
TAGS = {
    "swords": ["emerald_sword"],
    "axes": ["emerald_axe"],
    "pickaxes": ["emerald_pickaxe"],
    "shovels": ["emerald_shovel"],
    "trimmable_armor": ["emerald_chestplate"],
}

TAG_DIR = os.path.join(ROOT, "src/main/resources/data/minecraft/tags/items")

# 绿宝石的绿：取自原版绿宝石物品贴图里最常见的那一档（#17DD62, H=142.7）
EMERALD_HUE = 142.7
# 只有这个色相区间的像素会被「染绿」，其它（褐色、纯白）原样保留
GEM_HUE_MIN = 120.0
GEM_HUE_MAX = 210.0
# 宝石通常比钻石再艳一点
SAT_BOOST = 1.15


def tint(src_pixels):
    """把青色的钻石部件染成绿宝石绿，其余像素不动。返回 (新像素, 染色数, 不透明数)。"""
    out = []
    changed = 0
    opaque = 0
    for r, g, b, a in src_pixels:
        if a == 0:
            out.append((r, g, b, a))
            continue
        opaque += 1
        h, s, v = G.rgb_to_hsv(r, g, b)
        # s 极小的纯白 / 灰高光不进滤镜，免得把高光染脏
        if GEM_HUE_MIN <= h <= GEM_HUE_MAX and s > 0.05:
            h = EMERALD_HUE
            s = min(1.0, s * SAT_BOOST)
            changed += 1
        rr, gg, bb = G.hsv_to_rgb(h, s, v)
        out.append((rr, gg, bb, a))
    return out, changed, opaque


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
    """把若干「行」的贴图拼成一张验收图，透明处铺棋盘格。

    rows: [(scale, [(w, h, pixels), ...]), ...]
    """
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
    """给 5 件物品各写一份模型 JSON（工具类手持、盔甲类平铺）"""
    for src, out in ITEMS:
        parent = "item/generated" if "chestplate" in src or "helmet" in src \
            or "leggings" in src or "boots" in src else "item/handheld"
        path = os.path.join(ROOT, ASSETS, "models/item/%s.json" % out)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write('{\n  "parent": "%s",\n  "textures": {\n    "layer0": "wulingdiguo:item/%s"\n  }\n}\n'
                    % (parent, out))
        print("  %-20s -> models/item/%s.json  (parent=%s)" % (out, out, parent))


def write_tags():
    """把绿宝石装备挂进原版物品标签（不然它们不算剑/斧/镐/铲）"""
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

    write_models()
    write_tags()

    # 盔甲层（穿在身上用），尺寸不是 16×16，单独一行、倍率调小
    w, h, layer_src = G.png_read(io.BytesIO(z.read(VN_ARMOR % ARMOR_LAYER_SRC)))
    layer_out, changed, opaque = tint(layer_src)
    G.png_write(os.path.join(ARMOR_DIR, ARMOR_LAYER_OUT + ".png"), w, h, layer_out)
    print("  %-20s %2d×%-2d  染色 %3d/%-3d -> textures/models/armor/%s.png"
          % (ARMOR_LAYER_SRC, w, h, changed, opaque, ARMOR_LAYER_OUT))

    # 两张验收图分开出：物品图标（8×）与盔甲层（4×）
    W, H, canvas = compose_rows(item_rows)
    G.png_write(os.path.join(DOCS, "emerald_gear_preview.png"), W, H, canvas)
    print("验收图（每行：左=原版钻石 右=绿宝石）-> docs/emerald_gear_preview.png  %d×%d" % (W, H))

    W, H, canvas = compose_rows([(4, [(w, h, layer_src), (w, h, layer_out)])])
    G.png_write(os.path.join(DOCS, "emerald_armor_layer_preview.png"), W, H, canvas)
    print("验收图（盔甲层：左=原版钻石 右=绿宝石）-> docs/emerald_armor_layer_preview.png  %d×%d"
          % (W, H))


if __name__ == "__main__":
    main()
