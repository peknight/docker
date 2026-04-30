# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## 项目概述

Docker 相关的通用逻辑功能库。所有 Scala 项目中与 Docker 相关的通用功能都定义在此项目中，作为 `peknight` 单体仓库层级 6（系统与服务）的一部分。

## 模块设计原则

项目分为两类模块，职责边界明确：

### 通用模块（保持通用性，不掺杂个人偏好）

- **docker-core** — 核心领域模型和命令定义
  - `Identifier` — Docker 资源标识符（Image、Container、Volume、Network 的 Name/Id）
  - `Repository`、`Tag`、`Namespace` — 镜像仓库坐标
  - `command/` — Docker CLI 命令的选项模型（run/build/pull/tag/stop/rm/inspect/network/volume 等）
  - `network/`、`path/` — 网络配置和路径定义
  - 跨平台模块（JVM + JS）

- **docker-client** — Docker CLI 进程调用封装
  - 依赖 `docker-core`
  - 通过 `fs2.io.process.ProcessBuilder` 调用本地 `docker` 命令，将 `docker-core` 中的命令模型和选项转换为实际进程
  - 仅 JVM（需要本地进程支持）

- **docker-service** — 高层服务抽象
  - 依赖 `docker-client`
  - 封装 `buildIfNotExists`、`pullIfNotExists`、`createNetworkIfNotExists`、`run` 等幂等操作
  - `dockerInDockerVolume` — DinD 卷挂载配置
  - 仅 JVM（需要本地进程支持）

### 个人偏好模块（封装个人习惯和约定）

- **docker-build** — 镜像及版本定义
  - 依赖 `docker-core`
  - 定义常用的基础镜像（alpine、eclipse-temurin、mysql、nginx、postgres、redis 等）和第三方服务镜像（apollo、gitea、jenkins、ollama 等）
  - 版本号统一引用 `peknight.build.gav` 中定义的版本字面量
  - 跨平台模块（JVM + JS）

- **docker-custom** — 个人习惯编码
  - 依赖 `docker-service`
  - 个人 Docker 命名规范：`pek-*` 前缀的容器名、`pek-network` 网络、`docker.peknight.com` 私有仓库
  - IP 管理：`172.18.0.0/16` 网段，gateway `172.18.0.1`
  - `backupTag` / `backupImage` — 备份镜像命名约定
  - `timezoneVolumeMount` / `localtimeVolumeMount` — 时区挂载
  - `custom.service` — 个人应用运行服务（`runScalaApp`、`runNetworkApp`、`runHostApp`、`buildImageIfNotExists`）
  - 跨平台模块（JVM + JS），部分服务逻辑仅 JVM

## 模块依赖关系

```
docker-build ──┐
               ├── docker-core
docker-custom ──→ docker-service ──→ docker-client ──┘
```

- 通用模块（core/client/service）保持纯净，不引用 `docker-build` 和 `docker-custom`
- `docker-custom` 是个人偏好的最终汇聚层，可以引用所有模块

## 构建

```bash
# 编译所有模块
sbt compile

# 运行测试
sbt test

# 运行单个模块测试
sbt dockerCore/test
sbt "dockerCoreJVM/test"

# 快速编译（仅 JVM）
sbt "dockerCoreJVM/compile; dockerClientJVM/compile; dockerServiceJVM/compile; dockerCustomJVM/compile; dockerBuildJVM/compile"
```

## 编码规范

- 遵循全局 `~/Projects/peknight/claude/CLAUDE.md` 中的规范
- **通用模块中禁止出现个人偏好**：如特定命名前缀、私有仓库地址、个人网络配置等
- **个人偏好必须收敛到 `docker-custom` 和 `docker-build` 中**