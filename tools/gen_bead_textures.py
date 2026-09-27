# -*- coding: utf-8 -*-
"""
《武灵帝国》灵珠图标生成器。

需求（用户口径）：
  1. 灵珠图片「一种怪物一个图片」，**不再按品质分类**
  2. 物品名是「<怪物名>灵珠」，品质退到 tooltip 副栏
  3. 图标必须是**这只怪物自己的颜色**

做法：
  - 颜色：读怪物自己的实体贴图（client-extra.jar 里的
    assets/minecraft/textures/entity/**.png），去掉最暗的 15% 像素（轮廓、
    阴影）后求平均，得到的就是它身上的主色调；核心色取贴图里最鲜艳的
    常见色（末影人 / 凋灵这类贴图本身没彩色的，用人工指定的特征色）。
    颜色**不做色相偏移**，怪物什么色就是什么色。
  - 画法：16×16 硬像素分层（5 档明暗 + 最外圈深色描边 + 左上高光），
    中心嵌一枚核心色的菱形灵核。不用连续渐变，所以放大了也不糊。
  - 选图：物品 NBT 写 `CustomModelData` = 怪物序号，基模型挂 overrides。
    没有专属图的来源（如其它模组的怪物）退回品质图。

用法：
  python tools/gen_bead_textures.py [client-extra.jar 路径]

产出：
  - src/main/resources/assets/wulingdiguo/textures/item/bead/<mob>.png  （图标）
  - src/main/resources/assets/wulingdiguo/models/item/bead/<mob>.json  （模型）
  - src/main/resources/assets/wulingdiguo/models/item/spirit_bead_*.json（基模型 + overrides）
  - src/main/java/com/wuling/empire/item/BeadModels.java               （索引表，勿手改）
  - docs/bead_icons_preview.png                                        （总览图）
  - docs/bead_preview.html                                             （带名字的验收页）
"""
import base64
import hashlib
import json
import os
import struct
import sys
import zlib

# ---------------------------------------------------------------- 基础工具

def png_read(path_or_bytes):
    """读 PNG，返回 (w, h, [(r,g,b,a), ...])。只支持 8 位非隔行。"""
    d = path_or_bytes.read() if hasattr(path_or_bytes, "read") else open(path_or_bytes, "rb").read()
    i, idat, palette, trns = 8, b"", None, None
    w = h = ct = None
    while i < len(d):
        ln = struct.unpack(">I", d[i:i + 4])[0]
        typ = d[i + 4:i + 8]
        data = d[i + 8:i + 8 + ln]
        i += 12 + ln
        if typ == b"IHDR":
            w, h, bd, ct, comp, filt, interl = struct.unpack(">IIBBBBB", data)
            assert bd == 8 and interl == 0, "只支持 8 位非隔行 PNG"
        elif typ == b"IDAT":
            idat += data
        elif typ == b"PLTE":
            palette = [tuple(data[k:k + 3]) for k in range(0, len(data), 3)]
        elif typ == b"tRNS":
            trns = list(data)
    n = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    raw = zlib.decompress(idat)
    stride = w * n
    out, prev = [], bytearray(stride)
    pos = 0
    for _ in range(h):
        f = raw[pos]
        pos += 1
        line = bytearray(raw[pos:pos + stride])
        pos += stride
        if f == 1:
            for x in range(n, stride):
                line[x] = (line[x] + line[x - n]) & 0xFF
        elif f == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 0xFF
        elif f == 3:
            for x in range(stride):
                a = line[x - n] if x >= n else 0
                line[x] = (line[x] + ((a + prev[x]) >> 1)) & 0xFF
        elif f == 4:
            for x in range(stride):
                a = line[x - n] if x >= n else 0
                b = prev[x]
                c = prev[x - n] if x >= n else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 0xFF
        prev = line
        for x in range(w):
            px = line[x * n:(x + 1) * n]
            if ct == 6:
                out.append((px[0], px[1], px[2], px[3]))
            elif ct == 2:
                out.append((px[0], px[1], px[2], 255))
            elif ct == 0:
                out.append((px[0], px[0], px[0], 255))
            elif ct == 4:
                out.append((px[0], px[0], px[0], px[1]))
            else:  # ct == 3 调色板
                idx = px[0]
                r, g, b = palette[idx]
                out.append((r, g, b, 255 if trns is None or idx >= len(trns) else trns[idx]))
    return w, h, out


def _png_blob(w, h, pixels):
    """
    打包成 PNG 字节。

    **像素是 4 元组时写 RGBA（colortype 6），只有 3 通道时写 RGB（colortype 2）。**
    灵珠必须走 RGBA —— 球体外的像素 alpha=0，写 RGB 的话 alpha 会被丢掉、
    透明区变成纯黑，进游戏就是一个黑方块背景（这个坑踩过）。
    """
    rgba = len(pixels[0]) >= 4
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w):
            p = pixels[y * w + x]
            raw += bytes(p[:4]) if rgba else bytes(p[:3])

    def chunk(typ, data):
        c = typ + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6 if rgba else 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
            + chunk(b"IEND", b""))


def png_write(path, w, h, pixels):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(_png_blob(w, h, pixels))


def png_bytes(w, h, pixels):
    """同上，但返回内存里的 PNG 字节（HTML 预览内嵌用）"""
    return _png_blob(w, h, pixels)


def rgb_to_hsv(r, g, b):
    r, g, b = r / 255, g / 255, b / 255
    mx, mn = max(r, g, b), min(r, g, b)
    d = mx - mn
    if d == 0:
        h = 0.0
    elif mx == r:
        h = (60 * ((g - b) / d)) % 360
    elif mx == g:
        h = 60 * ((b - r) / d) + 120
    else:
        h = 60 * ((r - g) / d) + 240
    return h % 360, (0 if mx == 0 else d / mx), mx


def hsv_to_rgb(h, s, v):
    h = h % 360
    c = v * s
    x = c * (1 - abs((h / 60) % 2 - 1))
    m = v - c
    seg = int(h // 60)
    r, g, b = [(c, x, 0), (x, c, 0), (0, c, x), (0, x, c), (x, 0, c), (c, 0, x)][seg]
    return tuple(max(0, min(255, int(round((v2 + m) * 255)))) for v2 in (r, g, b))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def dist(a, b):
    return sum((x - y) ** 2 for x, y in zip(a[:3], b[:3])) ** 0.5


def mix(a, b, t):
    """颜色线性插值，t=0 取 a，t=1 取 b"""
    return tuple(max(0, min(255, int(round(x + (y - x) * t)))) for x, y in zip(a[:3], b[:3]))


def hx(c):
    return "#%02X%02X%02X" % tuple(c[:3])


# ------------------------------------------------------- 怪物 → 实体贴图

# 掉落灵珠的怪物（1.20.1 中实现 Enemy 的敌对生物 + 末影龙单独处理）
# 不在这个表里的怪物不掉灵珠，也不生成图标
MOB_TEXTURES = {
    "blaze": "blaze.png",
    "cave_spider": "spider/cave_spider.png",
    "creeper": "creeper/creeper.png",
    "drowned": "zombie/drowned.png",
    "elder_guardian": "guardian_elder.png",
    "ender_dragon": "enderdragon/dragon.png",
    "enderman": "enderman/enderman.png",
    "evoker": "illager/evoker.png",
    "ghast": "ghast/ghast.png",
    "giant": "zombie/zombie.png",          # 巨人复用僵尸贴图
    "guardian": "guardian.png",
    "hoglin": "hoglin/hoglin.png",
    "husk": "zombie/husk.png",
    "illusioner": "illager/illusioner.png",
    "magma_cube": "slime/magmacube.png",
    "phantom": "phantom.png",
    "piglin": "piglin/piglin.png",
    "piglin_brute": "piglin/piglin_brute.png",
    "pillager": "illager/pillager.png",
    "ravager": "illager/ravager.png",
    "shulker": "shulker/shulker.png",
    "skeleton": "skeleton/skeleton.png",
    "slime": "slime/slime.png",
    "stray": "skeleton/stray.png",
    "vindicator": "illager/vindicator.png",
    "warden": "warden/warden.png",
    "witch": "witch.png",
    "wither": "wither/wither.png",
    "wither_skeleton": "skeleton/wither_skeleton.png",
    "zoglin": "hoglin/zoglin.png",
    "zombie": "zombie/zombie.png",
    "zombie_villager": "zombie_villager/zombie_villager.png",
    "zombified_piglin": "piglin/zombified_piglin.png",
}

# 已停用的怪物：**不生成图标和模型，但占用序号槽位**。
# 序号是写进灵珠 NBT 的 CustomModelData，一旦整体前移，已刷出的灵珠就会全部串图，
# 所以退役的怪物只在 BeadModels.SOURCES 里留一个占位空号。
#   endermite（末影螨）：2026-09-26 用户明确「末影螨也不要」
#   silverfish（蠹虫）：2026-09-26 用户明确「蠹虫不要灵珠」，同时也本就在
#     Config.EXCLUDED_MOBS / NEVER_DROPS 里不掉珠，故从灵珠体系整体移除。
#   vex（恼鬼）：2026-09-26 用户明确「恼鬼也不要」，原版设定也点名排除过。
RETIRED = ("endermite", "silverfish", "vex")

# 中文名，只用于验收页展示
MOB_CN = {
    "blaze": "烈焰人", "cave_spider": "洞穴蜘蛛", "creeper": "苦力怕",
    "drowned": "溺尸", "elder_guardian": "远古守卫者", "ender_dragon": "末影龙",
    "enderman": "末影人", "endermite": "末影螨", "evoker": "唤魔者",
    "ghast": "恶魂", "giant": "巨人", "guardian": "守卫者",
    "hoglin": "疣猪兽", "husk": "尸壳", "illusioner": "幻术师",
    "magma_cube": "岩浆怪", "phantom": "幻翼", "piglin": "猪灵",
    "piglin_brute": "猪灵蛮兵", "pillager": "掠夺者", "ravager": "劫掠兽",
    "shulker": "潜影贝", "silverfish": "蠹虫", "skeleton": "骷髅",
    "slime": "史莱姆", "stray": "流浪者", "vex": "恼鬼",
    "vindicator": "卫道士", "warden": "监守者", "witch": "女巫",
    "wither": "凋灵", "wither_skeleton": "凋灵骷髅", "zoglin": "僵尸疣猪兽",
    "zombie": "僵尸", "zombie_villager": "僵尸村民", "zombified_piglin": "僵尸猪灵",
}

# ------------------------------------------------------------ 灵力回复量

# 每一档品质的回复倍率，**必须与 Config 里 restoreMultiplierFan..Ji 的默认值一致**。
# 最终回复量 = 怪物基准（下表）× 品质倍率。
QUALITY_KEYS = ("fan", "liang", "you", "shang", "ji")
QUALITY_CN = ("凡品", "良品", "优品", "上品", "极品")
QUALITY_MULT = (1.0, 1.5, 2.0, 3.0, 4.0)

# 怪物基准回复量（= 该怪物「凡品」灵珠的回复百分比）。
# 两个锚点：僵尸凡品 5%、末影龙极品 100%（即基准 25 × 极品倍率 4）。
# 表里没有的来源（模组怪物）在 Java 侧按最大生命值兜底，见 BeadPower.fallbackBase()。
MOB_RESTORE = {
    # —— 弱小：4 ~ 6
    "slime": 4.0,
    "magma_cube": 4.5,
    "zombie_villager": 4.5,
    "cave_spider": 4.5,
    "zombie": 5.0,             # 锚点：凡品 5%
    "husk": 5.0,
    "skeleton": 5.0,
    "piglin": 5.0,
    "zombified_piglin": 5.0,
    "stray": 5.5,
    "drowned": 5.5,
    "creeper": 6.0,
    "ghast": 6.0,
    "phantom": 6.0,
    # —— 普通：7 ~ 10
    "pillager": 7.0,
    "guardian": 7.0,
    "witch": 8.0,
    "blaze": 8.0,
    "wither_skeleton": 8.0,
    "vindicator": 8.0,
    "enderman": 9.0,
    "hoglin": 9.0,
    "shulker": 9.0,
    "zoglin": 10.0,
    # —— 精英：12 ~ 17
    "evoker": 12.0,
    "illusioner": 12.0,
    "piglin_brute": 13.0,
    "elder_guardian": 15.0,
    "ravager": 17.0,
    # —— 首领：22 ~ 25
    "giant": 22.0,
    "ender_dragon": 25.0,      # 锚点：极品 100%
    "wither": 25.0,
    "warden": 25.0,
}

# ------------------------------------------------------------ 取色

# 贴图平均色被杂色带偏的怪物，直接指定身上最显眼部位的颜色
PRIMARY_OVERRIDE = {
    "zombie": (88, 150, 60),          # 绿皮肤（平均值被青色衬衫拉跑）
    "giant": (74, 128, 52),           # 同上，比僵尸略深
    "witch": (122, 66, 176),          # 紫袍（平均值被黑袍 + 肤色拉成褐紫）
    "pillager": (110, 104, 100),      # 灰褐斗篷（原本太暗）
    "vindicator": (108, 110, 116),    # 灰蓝衣（原本太暗）
    "evoker": (128, 124, 108),        # 灰金袍（原本太暗）
    "cave_spider": (46, 88, 74),      # 洞穴蜘蛛是暗青绿，但要看得见
    "magma_cube": (176, 62, 18),      # 岩浆怪是发光的橙红
}

# 贴图本身没有彩色的怪物（末影人 / 凋灵…），灵核用特征色标出来
ACCENT_OVERRIDE = {
    "enderman": (154, 74, 224),       # 末影紫
    "ender_dragon": (150, 70, 210),   # 末影紫
    "wither": (108, 132, 186),        # 灵魂蓝
    "wither_skeleton": (176, 84, 76), # 暗红（凋灵骷髅的剑与眼）
    "skeleton": (128, 116, 96),       # 骨黄
    "giant": (60, 104, 44),
}


def dominant_colors(pixels, top=6):
    """统计不透明像素的颜色频次（量化到 4 值一档，避免抗锯齿碎色）"""
    freq = {}
    for r, g, b, a in pixels:
        if a < 200:
            continue
        key = (r >> 2 << 2, g >> 2 << 2, b >> 2 << 2)
        freq[key] = freq.get(key, 0) + 1
    return sorted(freq.items(), key=lambda kv: -kv[1])[:top]


def body_color(pixels, mob_id):
    """
    怪物主色 = 贴图里去掉最暗 15% 像素后的平均色。

    去掉最暗那部分是为了不被黑色描边/阴影拉灰；剩下的平均值基本就是这只怪
    在游戏里给人的整体印象色（骷髅灰白、苦力怕亮绿、岩浆怪橙红）。
    只做「亮到看得见」的兜底，不碰色相。
    """
    if mob_id in PRIMARY_OVERRIDE:
        base = PRIMARY_OVERRIDE[mob_id]
        h, s, v = rgb_to_hsv(*base)
        return hsv_to_rgb(h, s, max(v, 0.42))

    op = [(r, g, b) for r, g, b, a in pixels if a >= 200]
    if not op:
        return (150, 150, 150)
    op.sort(key=lum)
    core = op[int(len(op) * 0.15):] or op
    avg = tuple(int(sum(c[i] for c in core) / len(core)) for i in range(3))

    h, s, v = rgb_to_hsv(*avg)
    if s < 0.12:
        # 中性色怪物：骷髅 / 蠹虫 / 劫掠兽是「浅灰」，末影人 / 凋灵是「黑」。
        # 暗的那批只把明度抬到能看清，并保留原本的相对深浅（龙 > 凋灵 > 末影人）
        v = max(v, 0.55) if v >= 0.30 else min(0.55, 0.30 + 0.4 * v)
    else:
        v = max(v, 0.42)
        s = max(s, 0.32)
    return hsv_to_rgb(h, min(1.0, s), min(v, 0.92))


def contrast_core(primary, accent):
    """
    灵核和球体主色分不开时才动它（16×16 上太接近就糊成一团）。
    分得开就**原样保留怪物自己的颜色**，不做美化 —— 用户要的是「按怪物的颜色」。
    """
    if dist(primary, accent) >= 80:
        return accent
    ph, ps, pv = rgb_to_hsv(*primary)
    ah, as_, av = rgb_to_hsv(*accent)
    if pv >= 0.55:                       # 浅色球 → 核往暗里推
        av = min(av, pv - 0.28)
    else:                                # 深色球 → 核往亮里推
        av = max(av, min(0.92, pv + 0.34))
    if as_ > 0.15:                       # 原本是彩色的才提饱和，中性色保持中性
        as_ = max(as_, 0.45)
    return hsv_to_rgb(ah, min(1.0, as_), max(0.16, min(0.95, av)))


def core_color(pixels, mob_id, primary):
    """灵核色 = 贴图里最鲜艳的常见色；没有彩色就用人工指定的特征色"""
    if mob_id in ACCENT_OVERRIDE:
        return contrast_core(primary, ACCENT_OVERRIDE[mob_id])
    best, best_score = None, 0.0
    for c, n in dominant_colors(pixels, 12):
        h, s, v = rgb_to_hsv(*c)
        if s < 0.25 or v < 0.20:
            continue
        score = s * v * (1 + 0.15 * min(n, 200) / 200.0)
        if score > best_score:
            best, best_score = c, score
    if best is None or dist(best, primary) < 55:
        # 贴图里没有能和主色分开的彩色 → 用主色的深/亮版本当核
        h, s, v = rgb_to_hsv(*primary)
        if v > 0.55:
            best = hsv_to_rgb(h, min(1.0, s * 1.15), v * 0.52)
        else:
            best = hsv_to_rgb(h, max(0.15, s * 0.6), min(0.95, v + 0.38))
    return contrast_core(primary, best)


# ------------------------------------------------------------ 画图

# 4 档明暗：+ 向白，- 向黑。硬档位（不插值），所以放大了依然是清晰的像素块
TIERS = (0.38, 0.16, -0.06, -0.26)
LAM_TIERS = (0.82, 0.60, 0.36)


def _lambert(dx, dy, R):
    nx, ny = dx / R, dy / R
    nz = max(0.0, 1.0 - nx * nx - ny * ny) ** 0.5
    # 光照方向：左上偏前
    return max(0.0, nx * -0.55 + ny * -0.62 + nz * 0.56)


def _tier(lam):
    for i, th in enumerate(LAM_TIERS):
        if lam > th:
            return TIERS[i]
    return TIERS[-1]


def render_bead(primary, accent):
    """
    方案 A：16×16 球体。
    左上亮面 → 中面 → 右下暗面（4 档硬边）+ 最外圈深色描边 + 左上一两点高光，
    中央嵌一枚菱形灵核（亮心 + 本色 + 一圈暗槽衬底），形体最完整。
    """
    w = h = 16
    cx = cy = 7.5
    R = 7.3
    out = []
    for y in range(h):
        for x in range(w):
            dx, dy = x - cx, y - cy
            d2 = dx * dx + dy * dy
            if d2 > R * R:
                out.append((0, 0, 0, 0))
                continue
            d = d2 ** 0.5
            lam = _lambert(dx, dy, R)
            rho = abs(dx) + abs(dy)               # 到中心的菱形距离

            if d > R - 0.95:
                col = mix(primary, (16, 16, 26), 0.62)         # 描边
            elif rho <= 1.2:
                col = mix(accent, (255, 255, 255), 0.42)       # 灵核亮心
            elif rho <= 2.3:
                col = accent                                   # 灵核本体
            elif rho <= 3.2:
                t = _tier(lam) - 0.16                          # 灵核外的暗槽，把核衬出来
                col = mix(primary, (255, 255, 255), t) if t > 0 else mix(primary, (0, 0, 0), -t)
            else:
                t = _tier(lam)
                col = mix(primary, (255, 255, 255), t) if t > 0 else mix(primary, (0, 0, 0), -t)
                if lam > 0.97:
                    col = mix(col, (255, 255, 255), 0.55)      # 左上一两点高光
            out.append(col + (255,))
    return w, h, out


def crop_opaque(pixels, w, h):
    """怪物贴图的不透明包围盒（皮肤贴图四周有大片空白）"""
    xs, ys = [], []
    for i, (r, g, b, a) in enumerate(pixels):
        if a >= 200:
            xs.append(i % w)
            ys.append(i // w)
    if not xs:
        return 0, 0, w, h
    return min(xs), min(ys), max(xs) + 1, max(ys) + 1


def sample_grid(pixels, w, h, box, n):
    """把包围盒里的贴图缩成 n×n，每格取该格出现最多的不透明色（最近邻风格，保住像素感）"""
    bx0, by0, bx1, by1 = box
    bw, bh = max(1, bx1 - bx0), max(1, by1 - by0)
    grid = []
    for gy in range(n):
        row = []
        for gx in range(n):
            x0, x1 = bx0 + bw * gx // n, max(bx0 + bw * gx // n + 1, bx0 + bw * (gx + 1) // n)
            y0, y1 = by0 + bh * gy // n, max(by0 + bh * gy // n + 1, by0 + bh * (gy + 1) // n)
            cnt = {}
            for yy in range(y0, min(y1, by1)):
                for xx in range(x0, min(x1, bx1)):
                    r, g, b, a = pixels[yy * w + xx]
                    if a >= 200:
                        cnt[(r, g, b)] = cnt.get((r, g, b), 0) + 1
            row.append(max(cnt, key=cnt.get) if cnt else None)
        grid.append(row)
    return grid


def render_bead_texture(primary, pixels, w, h, n=10):
    """
    方案 B：珠心直接嵌**这只怪物自己的贴图**（缩成 n×n 后圆形裁切），
    外圈用主色包边。颜色 100% 是怪物本来的像素，辨识度最高。
    """
    grid = sample_grid(pixels, w, h, crop_opaque(pixels, w, h), n)
    half = n / 2.0
    size = 16
    cx = cy = 7.5
    R = 7.3
    inner_r = 5.6                                     # 贴图区半径
    out = []
    for y in range(size):
        for x in range(size):
            dx, dy = x - cx, y - cy
            d2 = dx * dx + dy * dy
            if d2 > R * R:
                out.append((0, 0, 0, 0))
                continue
            d = d2 ** 0.5
            lam = _lambert(dx, dy, R)
            if d > R - 0.95:
                col = mix(primary, (16, 16, 26), 0.60)
            elif d <= inner_r:
                gx = int((dx + half) / n * n)
                gy = int((dy + half) / n * n)
                gx = max(0, min(n - 1, gx))
                gy = max(0, min(n - 1, gy))
                base = grid[gy][gx] or primary
                t = _tier(lam) * 0.40                 # 只做轻微明暗，别糊掉像素
                col = mix(base, (255, 255, 255), t) if t > 0 else mix(base, (0, 0, 0), -t)
            else:
                t = _tier(lam)
                col = mix(primary, (255, 255, 255), t) if t > 0 else mix(primary, (0, 0, 0), -t)
                col = mix(col, (255, 255, 255), 0.62) if lam > 0.955 else col
            out.append(col + (255,))
    return size, size, out


# ------------------------------------------------------------ 验收页

DIGITS = {
    "0": ("111", "101", "101", "101", "111"),
    "1": ("010", "110", "010", "010", "111"),
    "2": ("111", "001", "111", "100", "111"),
    "3": ("111", "001", "111", "001", "111"),
    "4": ("101", "101", "111", "001", "001"),
    "5": ("111", "100", "111", "001", "111"),
    "6": ("111", "100", "111", "101", "111"),
    "7": ("111", "001", "001", "010", "010"),
    "8": ("111", "101", "111", "101", "111"),
    "9": ("111", "101", "111", "001", "111"),
}


def write_preview(path, items, cols=6, scale=5):
    """把所有灵珠图标拼成一张总览图（图标下方是编号）"""
    cw = 16 * scale
    cell_h = cw + 12
    rows = (len(items) + cols - 1) // cols
    W, H = cols * cw, rows * cell_h
    canvas = [(52, 52, 58)] * (W * H)

    def put(x, y, c):
        if 0 <= x < W and 0 <= y < H:
            canvas[y * W + x] = c

    for i, (idx, mob, px) in enumerate(items):
        gx, gy = (i % cols) * cw, (i // cols) * cell_h
        for y in range(16):
            for x in range(16):
                r, g, b, a = px[y * 16 + x]
                if a == 0:
                    continue
                for dy in range(scale):
                    for dx in range(scale):
                        put(gx + x * scale + dx, gy + y * scale + dy, (r, g, b))
        label = str(idx)
        lw = len(label) * 4 - 1
        lx = gx + (cw - lw) // 2
        ly = gy + cw + 3
        for ch in label:
            for ry, rowbits in enumerate(DIGITS[ch]):
                for rx, bit in enumerate(rowbits):
                    if bit == "1":
                        put(lx + rx, ly + ry, (230, 230, 235))
            lx += 4
    png_write(path, W, H, canvas)


def write_html(path, records):
    """
    验收页：把正式采用的图标（方案 A）放大到 76px + 中英文名 + 主色/灵核色号，
    方便逐只核对颜色。B 方案（珠心嵌贴图）作为备选留在 render_bead_texture 里，
    想换风格时改 main() 里那一行即可。
    """
    cells = []
    for rec in records:
        a = base64.b64encode(rec["png"]).decode("ascii")
        cells.append(
            '<div class="cell">'
            '<div class="head"><span class="idx">%d</span>'
            '<span class="name">%s</span><span class="en">%s</span></div>'
            '<img src="data:image/png;base64,%s" alt="%s">'
            '<div class="sw"><i style="background:%s"></i>%s'
            '<i style="background:%s"></i>%s</div>'
            '</div>' % (rec["idx"], MOB_CN.get(rec["mob"], rec["mob"]), rec["mob"],
                        a, rec["mob"], hx(rec["primary"]), hx(rec["primary"]).lower(),
                        hx(rec["accent"]), hx(rec["accent"]).lower()))
    html = """<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="utf-8">
<title>灵珠图标验收页</title>
<style>
  body{margin:0;padding:28px 32px 48px;background:#f5f6f8;color:#1f2329;
       font:14px/1.5 -apple-system,"Segoe UI","Microsoft YaHei",sans-serif}
  h1{margin:0 0 6px;font-size:20px;font-weight:600}
  p.sub{margin:0 0 8px;color:#6b7280;font-size:13px}
  .legend{background:#fff;border:1px solid #e5e7eb;border-radius:10px;padding:12px 16px;
          margin:0 0 22px;font-size:13px;color:#374151;line-height:1.7}
  .legend b{color:#111827}
  .grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(132px,1fr));gap:12px}
  .cell{background:#fff;border:1px solid #e5e7eb;border-radius:10px;padding:12px 8px 10px;
        text-align:center;box-shadow:0 1px 2px rgba(16,24,40,.04)}
  .head{margin-bottom:8px}
  .idx{color:#9ca3af;font-size:11px;margin-right:5px}
  .name{font-weight:600;font-size:13px}
  .en{color:#9ca3af;font-size:10px;margin-left:5px}
  /* 棋盘格垫底：灵珠背景是透明的，白底页面上浅色灵珠（恶魂、骷髅）会看不见 */
  img{width:76px;height:76px;image-rendering:pixelated;display:block;margin:0 auto;border-radius:6px;
      background-image:linear-gradient(45deg,#e6e9ef 25%%,transparent 25%%,transparent 75%%,#e6e9ef 75%%),
                       linear-gradient(45deg,#e6e9ef 25%%,transparent 25%%,transparent 75%%,#e6e9ef 75%%);
      background-size:12px 12px;background-position:0 0,6px 6px}
  .sw{font-size:10px;color:#6b7280;display:flex;align-items:center;justify-content:center;
      gap:4px;flex-wrap:wrap;margin-top:6px}
  .sw i{width:9px;height:9px;border-radius:2px;border:1px solid rgba(0,0,0,.14);display:inline-block}
</style></head><body>
<h1>灵珠图标验收页</h1>
<p class="sub">%d 只怪物各一张专属图，颜色取自它自己的实体贴图。</p>
<div class="legend">
  球体主色 = 该怪物贴图去掉轮廓暗部后的平均色；<b>灵核</b> = 它贴图上最鲜艳的特征色
  （僵尸的青、末影人的紫、岩浆怪的暗红）。每格下面两个色块就是这两个色号。
  要调哪只，报编号或名字即可。
</div>
<div class="grid">
%s
</div></body></html>
""" % (len(records), "\n".join(cells))
    with open(path, "w", encoding="utf-8") as f:
        f.write(html)


# ------------------------------------------------------------ 主流程

def fmt(v):
    """4.0 -> '4'，7.5 -> '7.5'，13.75 -> '13.8'"""
    s = ("%.2f" % v).rstrip("0").rstrip(".")
    return s if s else "0"


def write_power_java(java_dir):
    """生成 BeadPower.java：怪物基准回复量表 + 表外来源的兜底算法"""
    mobs = [m for m in sorted(MOB_RESTORE) if m not in RETIRED]
    lines = []
    for mob in mobs:
        lines.append('            Map.entry("minecraft:%s", %sF)%s'
                     % (mob, fmt(MOB_RESTORE[mob]), "," if mob != mobs[-1] else ""))
    body = "\n".join(lines)
    src = '''package com.wuling.empire.item;

import com.wuling.empire.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

import java.util.Map;

/**
 * 灵珠灵力回复量表 —— <b>BASE 表由 tools/gen_bead_textures.py 生成，请勿手改</b>。
 *
 * <p>回复量分两层：
 * <ol>
 *   <li><b>怪物基准</b>（本表）= 该怪物「凡品」灵珠回复的百分比，强怪给得多；</li>
 *   <li><b>品质倍率</b>（{@link Config#restoreMultiplier}）= 凡 1.0 / 良 1.5 / 优 2.0 / 上 3.0 / 极 4.0。</li>
 * </ol>
 * 最终回复量 = {@code base(来源) × quality.multiplier()}。
 *
 * <p>两个锚点：<b>僵尸凡品 5%%</b>、<b>末影龙极品 100%%</b>（基准 25 × 极品倍率 4）。
 * 已退役的怪物（蠹虫 / 末影螨 / 恼鬼）不在表内。
 *
 * <p>表里没有的来源（模组怪物）按最大生命值兜底，见 {@link #fallbackBase}，
 * 所以加装其它模组也不会出现「一颗珠子回 0%%」。
 *
 * <p>要调数值：改 {@code tools/gen_bead_textures.py} 的 {@code MOB_RESTORE} 后重跑脚本。
 */
public final class BeadPower {

    private BeadPower() {
    }

    /** 各怪物的基准回复量（%%），即该怪物凡品灵珠的回复量 */
    public static final Map<String, Float> BASE = Map.ofEntries(
%s
    );

    /** 表外来源的兜底锚点：20 血 = 5%% */
    private static final float HP_ANCHOR = 20.0F;
    private static final float HP_ANCHOR_VALUE = 5.0F;
    /** 每多 1 点最大生命值，基准增加 1/9（=> 200 血的末影龙正好 25） */
    private static final float HP_STEP = 1.0F / 9.0F;
    private static final float FALLBACK_MIN = 3.0F;
    private static final float FALLBACK_MAX = 25.0F;

    /** 连实体类型都取不到时（空来源 / 未注册 ID）的兜底值 */
    public static final float DEFAULT_BASE = 5.0F;

    /** 该来源的基准回复量（凡品值，%%） */
    public static float base(String source) {
        if (source == null || source.isEmpty()) {
            return DEFAULT_BASE;
        }
        Float fixed = BASE.get(source);
        if (fixed != null) {
            return fixed;
        }
        float hp = maxHealth(source);
        return hp <= 0.0F ? DEFAULT_BASE : fallbackBase(hp);
    }

    /** 表外来源（模组怪物）的兜底算法：按最大生命值线性给值，钳制在 [3, 25] */
    public static float fallbackBase(float maxHealth) {
        float v = HP_ANCHOR_VALUE + (maxHealth - HP_ANCHOR) * HP_STEP;
        return Math.max(FALLBACK_MIN, Math.min(FALLBACK_MAX, v));
    }

    /** 该来源某品质灵珠的回复量（%%） */
    public static float restore(String source, SpiritQuality quality) {
        return base(source) * quality.multiplier();
    }

    /** 取该来源的最大生命值；无法确定时返回 0（非生物 / 未注册 / 没有属性表） */
    @SuppressWarnings("unchecked")
    public static float maxHealth(String source) {
        ResourceLocation id = ResourceLocation.tryParse(source);
        if (id == null) {
            return 0.0F;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == null || !DefaultAttributes.hasSupplier(type)) {
            return 0.0F;
        }
        AttributeSupplier supplier =
                DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
        return (float) supplier.getValue(Attributes.MAX_HEALTH);
    }
}
''' % body
    path = os.path.join(java_dir, "BeadPower.java")
    with open(path, "w", encoding="utf-8") as f:
        f.write(src)


def write_power_html(path):
    """验收页：33 只怪 x 5 品质的回复量对照表"""
    mobs = [m for m in sorted(MOB_RESTORE) if m not in RETIRED]
    rows = []
    for mob in mobs:
        base = MOB_RESTORE[mob]
        cells = "".join(
            '<td class="num%d">%s</td>' % (i, fmt(base * QUALITY_MULT[i]))
            for i in range(len(QUALITY_KEYS)))
        rows.append(
            '<tr><td class="name">%s</td><td class="en">%s</td><td class="base">%s</td>%s</tr>'
            % (MOB_CN.get(mob, mob), mob, fmt(base), cells))
    head = "".join('<th>%s<div class="mult">x%s</div></th>' % (QUALITY_CN[i], fmt(QUALITY_MULT[i]))
                   for i in range(len(QUALITY_KEYS)))
    html = """<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="utf-8">
<title>灵珠灵力回复量验收表</title>
<style>
  body{margin:0;padding:28px 32px 48px;background:#f5f6f8;color:#1f2329;
       font:14px/1.5 -apple-system,"Segoe UI","Microsoft YaHei",sans-serif}
  h1{margin:0 0 6px;font-size:20px;font-weight:600}
  p.sub{margin:0 0 14px;color:#6b7280;font-size:13px}
  .legend{background:#fff;border:1px solid #e5e7eb;border-radius:10px;padding:12px 16px;
          margin:0 0 20px;font-size:13px;color:#374151;line-height:1.8;max-width:900px}
  .legend b{color:#111827}
  code{background:#f3f4f6;border-radius:4px;padding:1px 5px;font-size:12px}
  table{border-collapse:separate;border-spacing:0;background:#fff;font-size:13px;
        border:1px solid #e5e7eb;border-radius:10px;overflow:hidden;
        box-shadow:0 1px 2px rgba(16,24,40,.04)}
  th,td{padding:7px 12px;text-align:center;border-bottom:1px solid #f1f3f5}
  thead th{background:#fafbfc;font-weight:600;color:#374151;white-space:nowrap}
  thead th .mult{font-weight:400;color:#9ca3af;font-size:11px}
  tbody tr:last-child td{border-bottom:none}
  tbody tr:hover td{background:#fcfdfe}
  td.name{font-weight:600;text-align:left;white-space:nowrap}
  td.en{color:#9ca3af;font-size:11px;text-align:left}
  td.base{background:#fafbfc;color:#6b7280;font-weight:600}
  td.num0{color:#6b7280}
  td.num1{color:#3f9142}
  td.num2{color:#2b7bc4}
  td.num3{color:#8b46d6}
  td.num4{color:#c98a00;font-weight:600}
  td .pc{color:#c3c8d0;font-size:10px}
</style></head><body>
<h1>灵珠灵力回复量验收表</h1>
<p class="sub">共 %d 只怪物。数值 = 吸收一颗该怪物该品质灵珠可回复的灵力百分比（灵力上限 100）。</p>
<div class="legend">
  <b>算法</b>：回复量 = <b>怪物基准</b> × <b>品质倍率</b>。表中「基准」列就是该怪物<u>凡品</u>的回复量。<br>
  <b>锚点</b>：僵尸凡品 <code>5%%</code>、末影龙极品 <code>100%%</code>（基准 25 × 倍率 4）。<br>
  <b>表外来源</b>（其它模组的怪）：按最大生命值兜底 —— 20 血 = 5%%，每多 1 点血 +1/9，钳制在 3 ~ 25。<br>
  <b>改数值</b>：改 <code>tools/gen_bead_textures.py</code> 的 <code>MOB_RESTORE</code> 重跑脚本，本页与 Java 会一起更新。
</div>
<table>
  <thead><tr><th>怪物</th><th>ID</th><th>基准</th>%s</tr></thead>
  <tbody>
%s
  </tbody>
</table>
</body></html>
""" % (len(mobs), head, "\n".join(rows))
    with open(path, "w", encoding="utf-8") as f:
        f.write(html)


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    jar = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        root, "_env/ghome/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar")
    if not os.path.isfile(jar):
        sys.exit("找不到 client-extra.jar：%s" % jar)

    import zipfile
    z = zipfile.ZipFile(jar)

    tex_dir = os.path.join(root, "src/main/resources/assets/wulingdiguo/textures/item/bead")
    mod_dir = os.path.join(root, "src/main/resources/assets/wulingdiguo/models/item/bead")
    for d in (tex_dir, mod_dir):
        if os.path.isdir(d):
            for f in os.listdir(d):
                os.remove(os.path.join(d, f))
        os.makedirs(d, exist_ok=True)

    mobs = sorted(MOB_TEXTURES)
    # 序号槽位含已退役怪物：蠹虫留下了空号，所以后面怪物的序号不会前移
    slots = sorted(set(MOB_TEXTURES) | set(RETIRED))
    overrides, report, records, icons = [], [], [], []
    for mob in mobs:
        idx = slots.index(mob) + 1
        rel = "assets/minecraft/textures/entity/" + MOB_TEXTURES[mob]
        w, h, pixels = png_read(z.open(rel))
        primary = body_color(pixels, mob)
        accent = core_color(pixels, mob, primary)
        # 正式采用方案 A（球体 + 菱形灵核）。想换成方案 B（珠心嵌怪物贴图）就改成：
        #   bw, bh, bpx = render_bead_texture(primary, pixels, w, h)
        bw, bh, bpx = render_bead(primary, accent)
        png_write(os.path.join(tex_dir, mob + ".png"), bw, bh, bpx)
        with open(os.path.join(mod_dir, mob + ".json"), "w", encoding="utf-8") as f:
            json.dump({
                "parent": "minecraft:item/generated",
                "textures": {"layer0": "wulingdiguo:item/bead/%s" % mob},
            }, f, indent=2)
            f.write("\n")
        overrides.append({
            "predicate": {"custom_model_data": idx},
            "model": "wulingdiguo:item/bead/%s" % mob,
        })
        report.append((idx, mob, primary, accent))
        records.append({"idx": idx, "mob": mob, "primary": primary, "accent": accent,
                        "png": png_bytes(bw, bh, bpx)})
        icons.append((idx, mob, bpx))

    # 5 个品质基模型挂上同一份 overrides（没有来源的灵珠就用品质色兜底）
    for q in ("fan", "liang", "you", "shang", "ji"):
        p = os.path.join(root, "src/main/resources/assets/wulingdiguo/models/item/spirit_bead_%s.json" % q)
        with open(p, "w", encoding="utf-8") as f:
            json.dump({
                "parent": "minecraft:item/generated",
                "textures": {"layer0": "wulingdiguo:item/spirit_bead_%s" % q},
                "overrides": overrides,
            }, f, indent=2)
            f.write("\n")

    # Java 索引表（服务端写 CustomModelData 用，必须与上面的序号一致）
    java_dir = os.path.join(root, "src/main/java/com/wuling/empire/item")
    os.makedirs(java_dir, exist_ok=True)
    with open(os.path.join(java_dir, "BeadModels.java"), "w", encoding="utf-8") as f:
        f.write('''package com.wuling.empire.item;

import java.util.List;

/**
 * 灵珠外观索引表 —— <b>由 tools/gen_bead_textures.py 生成，请勿手改</b>。
 *
 * 每只怪物一张专属灵珠图（不再按品质分图），选图靠物品 NBT 的
 * `CustomModelData`：值 = 下表序号（从 1 开始），
 * 对应 models/item/spirit_bead_*.json 里的 overrides 条目。
 * 没有专属图的来源（例如其它模组的怪物、以及已退役的怪物）不写该 NBT，退回品质图。
 *
 * 表里可能出现「空号」（如蠹虫）：那是不出灵珠后留下的占位，
 * 保留它是为了让其它怪物的序号不因退役而整体前移（否则历史灵珠会显示错图）。
 *
 * 重新生成：python tools/gen_bead_textures.py
 */
public final class BeadModels {

    private BeadModels() {
    }

    /** 有专属灵珠图的怪物来源（顺序即 CustomModelData，空号为已退役怪物） */
    public static final List<String> SOURCES = List.of(
''')
        for i, mob in enumerate(slots):
            comma = "," if i < len(slots) - 1 else ""
            tail = "  // 已退役（不掉灵珠），仅占位" if mob in RETIRED else ""
            f.write('            "minecraft:%s"%s%s\n' % (mob, comma, tail))
        f.write('    );\n\n')

        f.write('    /**\n'
                '     * 已退役的来源：不出灵珠，取图时直接返回 -1（退回品质图）。\n'
                '     * 它们在 SOURCES 里仍占着号，只是为了让其它怪物的序号不整体前移。\n'
                '     */\n'
                '    public static final List<String> RETIRED = List.of(\n')
        for i, mob in enumerate(RETIRED):
            comma = "," if i < len(RETIRED) - 1 else ""
            f.write('            "minecraft:%s"%s\n' % (mob, comma))
        f.write('    );\n\n')

        f.write('''    /** 取该来源对应的 CustomModelData；没有专属图返回 -1 */
    public static int modelIndex(String source) {
        if (source == null || RETIRED.contains(source)) {
            return -1;
        }
        int i = SOURCES.indexOf(source);
        return i < 0 ? -1 : i + 1;
    }
}
''')

    # 灵力回复量表（与 Config 里的品质倍率配套）
    write_power_java(java_dir)
    write_power_html(os.path.join(root, "docs/bead_power.html"))

    write_preview(os.path.join(root, "docs/bead_icons_preview.png"), icons)
    write_html(os.path.join(root, "docs/bead_preview.html"), records)

    print("生成 %d 张灵珠图 -> %s" % (len(mobs), tex_dir))
    print("总览图 -> docs/bead_icons_preview.png   验收页 -> docs/bead_preview.html")
    for idx, mob, primary, accent in report:
        print("  %2d  %-16s %-8s 主色 %s   灵核 %s"
              % (idx, mob, MOB_CN.get(mob, ""), hx(primary), hx(accent)))

    print()
    print("灵力回复量（基准 = 凡品值，后面是 良/优/上/极）：")
    print("      %-16s %6s  %6s %6s %6s %6s %6s"
          % ("怪物", "基准", "凡品", "良品", "优品", "上品", "极品"))
    for mob in sorted(MOB_RESTORE):
        if mob in RETIRED:
            continue
        b = MOB_RESTORE[mob]
        vals = " ".join("%6s" % fmt(b * m) for m in QUALITY_MULT)
        print("      %-16s %6s  %s" % (MOB_CN.get(mob, mob), fmt(b), vals))
    print("验收表 -> docs/bead_power.html   Java -> item/BeadPower.java")


if __name__ == "__main__":
    main()
