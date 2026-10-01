# WuLing Empire / 武灵帝国

《我的世界之武灵帝国》改编模组 · **Minecraft 1.20.1 / Forge**

## 下载

最新版本见 [**Releases**](https://github.com/caianran1/Wuling-Empire/releases/latest)，下载 jar 放进 `.minecraft/mods/` 即可。

| 产物名 | 含义 |
|---|---|
| `Wuling-Empire-v0.3.0.jar` | 正式版 |
| `Wuling-Empire-v0.3.0-test.jar` | 测试版 |

## 环境要求

| 项目 | 要求 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x |
| Java | 17 |

## 玩法概览

### 灵珠

击杀怪物掉落**灵珠**，右键搜刮尸体获得。品质决定修炼速度，修行上限人人相同。

### 开启武灵

| 途径 | 方式 |
|---|---|
| 生存 | 手持灵珠右键**村民牧师**开启，种类随机 |
| 创造 | 用**绑定器**自选种类 |

### 操作

| 按键 | 功能 |
|---|---|
| `Shift` + `M` | 凝聚武灵 |
| `Shift` + `N` | 打开升级面板 |

### 境界

木 → 石 → 黄金 → 玄铁 → 钻石 → 下界合金 → **绿宝石**。凝聚物逐级强过原版同款。

## 从源码构建

```bash
./gradlew build                     # 正式版
./gradlew build -Pmod_suffix=test   # 测试版
```

## 文档

- [`docs/武灵系统设计规格.md`](docs/武灵系统设计规格.md) —— 完整设计规格
- [`README.txt`](README.txt) —— 运行环境与支持版本

## 发布

仓库自带 `tools/publish_github.py`，走 GitHub REST API（不依赖 `git push`）：

```bash
python tools/publish_github.py status         # 查看本地与远程差异
python tools/publish_github.py sync           # 同步源码到 main
python tools/publish_github.py release 0.3.0  # 建 tag 并上传对应 jar
```

## 致谢

**本模组是《我的世界之武灵帝国》的改编作品，核心设定取自原作，谨向原作者致敬。**

**双子动漫**（ShuangZi Animation）—— 《我的世界之武灵帝国》原作者。

游戏内也能看到：Mod 列表 → **WuLing Empire / 武灵帝国** → Credits 栏。

## 许可

- [`LICENSE.txt`](LICENSE.txt) —— 本项目基于 Forge MDK 的许可声明
