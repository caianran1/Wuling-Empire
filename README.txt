
《武灵帝国》(WuLing Empire) — 运行环境与支持版本
================================================
Minecraft 1.20.1 (Forge)
Java 17

【发布产物命名】
构建出来的 jar 统一叫  Wuling-Empire-v<版本号>.jar
    例：Wuling-Empire-v0.2.27.jar
（0.2.27 起改名；更早的产物叫 wulingdiguo-0.2.x.jar，只是文件名不同，内容无差别。）
文件名由 build.gradle 的 base.archivesName='Wuling-Empire' 与 version="v"+mod_version 决定。
mod_id 仍然是 wulingdiguo，游戏 Mod 列表里显示的名字是 WuLing Empire / 武灵帝国。

支持 1.20.1 的**全部** Forge 版本：47.0.0 ~ 47.4.23（共 132 个），
mods.toml 中已声明 loaderVersion="[47,)" 与 forge versionRange="[47,)"。

【为什么能全版本支持】
gradle.properties 里的 forge_version 故意锁在 1.20.1 的**最低版 47.0.0**，
编译产物只引用 47.0.0 就已存在的 API，因此可以在更高的 47.x 上加载。
反过来说：升级 forge_version 后若引入了新版独有的 API，产物就不再向下兼容。

【想在新版 Forge 上调试】
改 gradle.properties 的 forge_version，或临时用命令行属性覆盖：
    gradle runClient -Pforge_version=47.4.23

【注意】
主类 WulingEmpire 必须保留**无参构造函数**（Forge 47.0.0~47.3.7 只认无参构造函数）。
ResourceLocation 请用 new ResourceLocation(ns, path)，不要用 fromNamespaceAndPath 等
后期才 backport 进来的静态工厂。

详细说明见 docs/武灵系统设计规格.md 第九章「Forge 版本兼容性」。


Source installation information for modders
-------------------------------------------
This code follows the Minecraft Forge installation methodology. It will apply
some small patches to the vanilla MCP source code, giving you and it access 
to some of the data and functions you need to build a successful mod.

Note also that the patches are built against "un-renamed" MCP source code (aka
SRG Names) - this means that you will not be able to read them directly against
normal code.

Setup Process:
==============================

Step 1: Open your command-line and browse to the folder where you extracted the zip file.

Step 2: You're left with a choice.
If you prefer to use Eclipse:
1. Run the following command: `./gradlew genEclipseRuns`
2. Open Eclipse, Import > Existing Gradle Project > Select Folder 
   or run `gradlew eclipse` to generate the project.

If you prefer to use IntelliJ:
1. Open IDEA, and import project.
2. Select your build.gradle file and have it import.
3. Run the following command: `./gradlew genIntellijRuns`
4. Refresh the Gradle Project in IDEA if required.

If at any point you are missing libraries in your IDE, or you've run into problems you can 
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
(this does not affect your code) and then start the process again.

Mapping Names:
=============================
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license, if you do not agree with it you can change your mapping names to other crowdsourced names in your 
build.gradle. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/MinecraftForge/MCPConfig/blob/master/Mojang.md

Additional Resources: 
=========================
Community Documentation: https://docs.minecraftforge.net/en/1.20.1/gettingstarted/
LexManos' Install Video: https://youtu.be/8VEdtQLuLO0
Forge Forums: https://forums.minecraftforge.net/
Forge Discord: https://discord.minecraftforge.net/
