# NeoForge 客户端 Mod 自动添加多人游戏服务器 — 项目总结

> 本文档总结了从需求分析到自动构建的完整方案，适用于需要在 Minecraft Java 版（NeoForge）客户端中自动注入/管理多人游戏服务器列表的场景。

---

## 一、核心需求

1. **功能需求**：NeoForge 客户端 Mod 能够在游戏内自动将指定服务器地址添加到“多人游戏”服务器列表中。
2. **开发需求**：不想在本地配置 Gradle/Java 环境，希望直接通过 GitHub Actions 自动构建产出 `.jar` 文件。
3. **生态现状**：希望寻找现成的 Mod 或模板项目，避免从零搭建。

---

## 二、关键结论

### 2.1 权限可行性：完全可以

Minecraft Java 版（Forge/NeoForge/Fabric）的 Mod 与客户端运行在同一 JVM 中，**系统权限等同于游戏进程本身**。因此：

- 可以直接调用 Minecraft 原生的 `ServerList` 类读写 `servers.dat`；
- 可以读写文件系统、发起网络请求、执行系统命令；
- 可以自动在玩家启动游戏时或打开多人游戏界面时，静默添加服务器条目。

### 2.2 现成 Mod 现状：NeoForge 生态中几乎没有

| 项目 | 平台 | 功能 | 是否满足需求 |
|------|------|------|-------------|
| **Server Fetcher** | Fabric 1.21.1 | 启动时从 HTTP 接口拉取 IP 并自动写入 `servers.dat` | ❌ 不支持 NeoForge |
| **Server Browser** | Fabric/Forge/NeoForge | 在多人游戏界面内置服务器浏览器/发现页 | ❌ 仅浏览，不自动添加 |
| **BisectHosting Server Integration Menu** | NeoForge | 主机商集成菜单 | ❌ 功能未知，大概率仅推广特定服务商 |

**结论**：NeoForge 下没有现成的通用"自动添加服务器"Mod，需要自行开发或基于模板改造。

---

## 三、推荐的开箱即用模板项目

以下项目均内置 GitHub Actions 工作流，Clone 后修改 `modid` 和服务器地址即可使用。

| 模板项目 | 特点 | 适用场景 |
|---------|------|---------|
| **CleanroomModTemplate** | 最完整的 CI/CD 模板，自带 `build.yml` + `release.yml` + `release-to-cf-mr.yml`，支持自动发布到 CurseForge 和 Modrinth | 需要完整发布流程的首选 |
| **Multi-ModLoaderTemplate** | Fabric + NeoForge 双平台，共用一套源码，带基础 CI | 需要跨平台兼容 |
| **stonecutter-mod-template** | 支持 Fabric/NeoForge/Forge，多版本管理，带自动构建和发布 | 需要维护多个 MC 版本 |
| **Straywave/ModTemplate** | 极简模板，Push 即自动构建，产物保留 90 天 | 快速验证、最小化上手 |

---

## 四、最小实现方案（NeoForge 1.21.1）

### 4.1 核心代码逻辑

```java
package com.example.autoserver;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;

@Mod(value = "autoserver", dist = Dist.CLIENT)
public class AutoServerMod {
    private static boolean added = false;

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (!added && Minecraft.getInstance().level == null) {
            addServer("我的服务器", "your.server.ip:25565");
            added = true;
        }
    }

    private static void addServer(String name, String ip) {
        ServerList list = new ServerList(Minecraft.getInstance());
        list.load();
        // 去重检查
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).ip.equals(ip)) return;
        }
        ServerData data = new ServerData(name, ip, ServerData.Type.SERVER);
        list.add(data, false);
        list.save();
    }
}
```

### 4.2 进阶：从远程 URL 拉取动态 IP

```java
// 在 addServer 前加入 HTTP 请求逻辑
String ip = fetchIpFromUrl("https://your-api.com/ip.txt");
addServer("动态服务器", ip);
```

---

## 五、GitHub Actions 自动构建配置

### 5.1 最小构建工作流（`.github/workflows/build.yml`）

```yaml
name: Build NeoForge Mod

on:
  push:
    branches: [main, master]
  pull_request:
    branches: [main, master]

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          cache-read-only: ${{ github.ref != 'refs/heads/main' }}

      - name: Grant execute permission
        run: chmod +x gradlew

      - name: Build with Gradle
        run: ./gradlew build --no-daemon

      - name: Upload Artifact
        uses: actions/upload-artifact@v4
        with:
          name: neoforge-mod
          path: neoforge/build/libs/*.jar
          if-no-files-found: error
```

**关键点**：
- NeoForge 1.21.x 要求 **Java 21**，不可低于此版本；
- `setup-gradle` 自带缓存，重复构建会大幅加速；
- 构建产物默认在 `neoforge/build/libs/` 下，Actions 会自动上传为可下载的 Artifact。

### 5.2 自动发布 GitHub Release（可选）

```yaml
name: Release
on:
  push:
    tags:
      - "v*"
permissions:
  contents: write
jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"
      - uses: gradle/actions/setup-gradle@v3
      - run: chmod +x gradlew
      - name: Build
        run: ./gradlew build --no-daemon
      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          files: neoforge/build/libs/*.jar
          generate_release_notes: true
```

**用法**：本地执行 `git tag v1.0.0 && git push origin v1.0.0`，GitHub 自动构建并发布 Release。

---

## 六、快速开始步骤

1. **Fork/Clone** 推荐的模板项目（如 CleanroomModTemplate）；
2. **修改** `modid`、服务器名称和 IP 地址；
3. **创建** `.github/workflows/build.yml`（模板通常已自带，按需调整）；
4. **Push** 到 GitHub，进入 Actions 标签页查看构建进度；
5. **下载** 构建完成的 Artifact（或自动发布的 Release 附件）。

---

## 七、安全提示

Java 版 Mod 运行在客户端 JVM 中，拥有与游戏进程同等的系统权限。请确保：
- 只从可信来源下载或使用 Mod；
- 如果 Mod 包含从远程 URL 拉取服务器地址的功能，确保 URL 可控且使用 HTTPS；
- 避免在 Mod 中硬编码敏感信息（如 API 密钥、私有服务器凭证）。
