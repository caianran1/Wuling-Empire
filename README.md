# WuLing Empire / 武灵帝国

《我的世界之武灵帝国》改编模组 · Minecraft 1.20.1 / Forge

## 下载

见 [**Releases**](https://github.com/caianran1/Wuling-Empire/releases/latest)，下载 jar 放进 `.minecraft/mods/` 即可。

## 环境要求

Minecraft 1.20.1 · Forge 47.x · Java 17

## 玩法概览

**灵珠** —— 击杀怪物掉落，右键搜刮尸体获得，品质决定修炼速度。

**开启武灵** —— 生存：手持灵珠右键村民牧师开启，种类随机；创造：用绑定器自选种类。

**操作** —— `Shift+M` 凝聚武灵，`Shift+N` 打开升级面板。

**境界** —— 木 → 石 → 黄金 → 玄铁 → 钻石 → 下界合金 → 绿宝石。

## 致谢

本模组是《我的世界之武灵帝国》的改编作品，核心设定取自原作，谨向原作者致敬。

原作者：**双子动漫**（ShuangZi Animation）。游戏内也可在 Mod 列表的 Credits 栏看到。

## 许可

基于 Forge MDK，见 [`LICENSE.txt`](LICENSE.txt)。

<details>
<summary>开发者</summary>

```bash
./gradlew build                     # 正式版
./gradlew build -Pmod_suffix=test   # 测试版
python tools/publish_github.py sync # 同步源码到 main
```

设计规格见 [`docs/武灵系统设计规格.md`](docs/武灵系统设计规格.md)。

</details>
