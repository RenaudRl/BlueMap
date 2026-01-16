# BlueMap

![Java Version](https://img.shields.io/badge/Java-21-orange)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Target](https://img.shields.io/badge/Target-Folia%20/%20Paper%20/%20BTC--CORE-blue)

**BlueMap** is a high-performance, strictly optimized fork of **BlueMap**, engineered specifically for the **BTC Studio** infrastructure. This fork drops support for legacy platforms (Spigot, Bukkit, older NMS, Fabric, Forge) to provide native, blazingly fast integration with **Paper** and **Folia**.

> [!WARNING]
> **PLATFORM COMPATIBILITY NOTICE**
> This fork is **STRICTLY** for Paper 1.21.11+ and Folia 1.21.11+. Legacy compatibility layers have been removed to maximize performance. If you are not running modern Paper/Folia, this plugin **will not function**.

---

## 🚀 Key Features in Detail

### ⚡ Concurrency & Threading (Folia Native)
- **Native Folia Support**: Deeply integrated with Folia's `RegionScheduler` and `GlobalRegionScheduler`. No main-thread blocking operations.
- **Async Rendering**: Map updates happen asynchronously, respecting regionized threading rules to prevent server lag.
- **Zero-Overhead Logic**: Removed unnecessary abstraction layers for non-compatible platforms.

### 🎨 CraftEngine Integration
- **Custom Block Support**: Natively renders `CraftEngine` furniture and custom blocks.
- **Auto-Discovery**: Automatically parses CraftEngine configuration packs to resolve custom models.
- **Zero-Config**: Works out of the box by scanning `plugins/CraftEngine`.

### 🛠️ Core Optimisations & Debloating
- **Java 21 Native**: Leveraging the latest JVM optimizations for maximum throughput and memory efficiency.
- **Legacy Cleanup**: Removed support for Forge, Fabric, Sponge, and CLI to focus purely on the server plugin implementing.
- **BTC Core Integration**: Native detection of BTC Core platform to enable specialized optimizations.

### 🌍 Deployment & Startup
- **Steamlined Loading**: Faster startup times through reduced library dependencies.
- **Plug & Play**: Automatic threading context detection for both Paper and Folia environments.

---

## ⚙️ Configuration

BlueMap is optimized out-of-the-box, but stays configurable via `core.conf` and `webapp.conf` in `plugins/BlueMap`.

### Key Settings
| File | Description |
|-----|-------------|
| `core.conf` | Core rendering settings, threading, data storage. |
| `webapp.conf` | Webserver settings, port, external webroot. |

### CraftEngine Support
No manual configuration needed. The plugin automatically detects configurations in `plugins/CraftEngine/packs` or `plugins/CraftEngine`.

---

## 🛠 Building & Deployment

Requires **Java 21**.

```bash
# Clean and compile the project
./gradlew clean build
```

The resulting artifact will be in `implementations/paper/build/libs/`.

---

## 🤝 Credits & Inspiration
This project is built upon the innovation of the broader Minecraft development community:
- **[BlueMap](https://github.com/BlueMap-Minecraft/BlueMap)** - The original project.

---

## 📜 License
- **Custom BTC-CORE Patches**: Proprietary to **BTC Studio**.
- **Upstream Source**: Original licenses apply to their respective components from BlueMap (MIT).

---
**Fork maintained by BTCSTUDIO**
