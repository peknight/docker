---
name: update-docker-images
description: |
  自动更新 docker-build 模块中所有 Docker 镜像的最新版本号。
  触发词："更新 Docker 镜像版本"、"update docker images"、"update docker versions"。
  流程：直接 --apply 执行 → 展示结果 → 自动 git commit。
---

# Docker 镜像版本自动更新

## 触发条件

用户说 "更新一下 Docker 镜像版本"、"update docker images"、"update docker versions" 等类似表述。

## 执行流程

### Step 1: 执行更新

```bash
python3 scripts/update-docker-images.py --apply
```

如果脚本输出中包含 GHCR 镜像查询失败（GITHUB_TOKEN 未设置），询问用户是否需要设置 `GITHUB_TOKEN` 环境变量后重新执行。

### Step 2: 展示结果

向用户展示更新结果，列出已更新/跳过/错误的镜像。

### Step 3: 自动提交变更

如果有更新的镜像，自动生成包含变更详情的 commit message 并提交：

```bash
git add docker-build/shared/src/main/scala/com/peknight/docker/build/package.scala
git commit -m "chore: bump docker-build image versions"
```

commit message body中列出每个镜像的版本变化（如 `redis: 8.6.2 → 8.6.3`）。

## 注意事项

- 脚本位于 `scripts/update-docker-images.py`，使用纯 Python 标准库，零外部依赖
- 更新范围：`docker-build/package.scala` 中所有带 `/** @versionCheck ... */` 锚点的镜像
- 排除 `apolloconfig` 和 `xuxueli` 组（版本号引用自外部模块）
- GHCR 镜像（如 openclaw）需要设置 `GITHUB_TOKEN` 环境变量
- 不要手动修改版本号，统一通过脚本执行
- 更新后验证缩进是否正确保留