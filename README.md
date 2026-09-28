# WuLing Empire / 武灵帝国

《我的世界之武灵帝国》官方改编模组 · **Minecraft 1.20.1 / Forge**

## 下载

最新版本见 [**Releases**](https://github.com/caianran1/Wuling-Empire/releases/latest) —— 直接下载 `Wuling-Empire-vX.Y.Z.jar` 放进 `.minecraft/mods/` 即可。

> `build/` 目录不在仓库里（构建中间产物，每次编译重新生成）。可下载的模组文件一律放在 **Releases** 附件中。

## 环境要求

| 项目 | 要求 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | **47.0.0 ~ 47.4.23**（覆盖 1.20.1 全部 132 个版本） |
| Java | 17 |

编译目标锁定 Forge 最低版 `47.0.0`，字节码只引用该版本即存在的 API，因此天然向上兼容。

## 玩法概览

### 灵珠

击杀怪物掉落**灵珠**，右键搜刮倒地尸体后获得。灵珠品质决定**修炼速度**（×1 / ×1.25 / ×1.5 / ×2 / ×3），修行上限人人相同。

### 开启武灵

| 途径 | 方式 |
|---|---|
| 生存 | 手持灵珠右键**村民牧师**，由其抽取并开启武灵（种类随机） |
| 创造 | 用**绑定器**自选武灵种类 |

### 操作

| 按键 | 功能 |
|---|---|
| `Shift` + `M` | 凝聚武灵（在当前境界生成实体武器/护甲） |
| `Shift` + `N` | 打开升级面板 |

### 境界

木 → 石 → 黄金 → 玄铁 → 钻石 → 下界合金 → **绿宝石**。每一境界的凝聚物都比原版同款更强，且逐级碾压上一档。

## 从源码构建

```bash
./gradlew build          # 产物：build/libs/Wuling-Empire-v<版本>.jar
./gradlew build -x test  # 跳过测试
```

## 文档

- [`docs/武灵系统设计规格.md`](docs/武灵系统设计规格.md) —— 完整设计规格与历代口径变更
- [`README.txt`](README.txt) —— 运行环境与支持版本
- [`changelog.txt`](changelog.txt) —— 版本变更记录

## 发布流程

仓库自带发布脚本（走 GitHub REST API，不依赖 `git push`）：

```bash
python tools/publish_github.py status          # 查看本地与远程差异
python tools/publish_github.py sync            # 同步源码到 main
python tools/publish_github.py release 0.2.29  # 建 tag + Release + 上传 jar
```

## 许可与致谢

见 [`LICENSE.txt`](LICENSE.txt) 与 [`CREDITS.txt`](CREDITS.txt)。
