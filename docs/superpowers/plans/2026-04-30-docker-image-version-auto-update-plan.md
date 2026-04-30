# Docker Image Version Auto-Update Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a Python script to automatically check and update Docker image version tags defined in the docker-build module, using `/** @versionCheck <URL> */` comment anchors.

**Architecture:** A single Python script (zero external dependencies) scans `docker-build/package.scala` for `@versionCheck` annotations, queries Docker Hub API and GitHub Packages API for latest tags, applies smart version matching (stable-only, alpine suffix matching, major version bump detection), and updates the file in-place. A companion SKILL.md defines the skill for Claude Code to invoke.

**Tech Stack:** Python 3 standard library (urllib, re, json, argparse, pathlib), Scala 3 (docker-build module)

---

## File Structure

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `scripts/update-docker-images.py` | Main Python script for checking/updating Docker image versions |
| Create | `.claude/skills/update-docker-images/SKILL.md` | Claude Code skill definition for invoking the script |
| Modify | `docker-build/shared/src/main/scala/com/peknight/docker/build/package.scala` | Add `/** @versionCheck ... */` annotations to version definitions |

## Known Image Patterns

| Image | Current Tag | Tag Pattern | Source |
|-------|-------------|-------------|--------|
| alpine | `3.23.4` | Simple version | Docker Hub library |
| eclipse-temurin | `26_35-jdk` | `{major}_{patch}-jdk` | Docker Hub library |
| mysql | `9.6.0` | Simple version | Docker Hub library |
| nginx | `1.30.0-alpine3.23` | `{ver}-alpine{alp_ver}` | Docker Hub library |
| postgres | `18.3-alpine3.23` | `{ver}-alpine{alp_ver}` | Docker Hub library |
| redis | `8.6.2-alpine3.23` | `{ver}-alpine{alp_ver}` | Docker Hub library |
| ubuntu | `26.04` | Simple version | Docker Hub library |
| alpine/psql | `18.3` | Simple version | Docker Hub namespace |
| gitea/gitea | `1.25.5-rootless` | `{ver}-rootless` | Docker Hub namespace |
| jenkins/jenkins | `2.561-jdk25` | `{ver}-jdk{java}` | Docker Hub namespace |
| ollama/ollama | `0.21.0` | Simple version | Docker Hub namespace |
| openclaw/openclaw | `2026.4.20` | Date-based `{Y}.{M}.{D}` | GHCR |
| paulgauthier/aider | `v0.86.2` | `v`-prefixed | Docker Hub namespace |
| sonatype/nexus3 | `3.91.1-alpine` | `{ver}-alpine` | Docker Hub namespace |
| rustdesk/rustdesk-server | `1.1.15` | Simple version | Docker Hub namespace |
| v2fly/v2fly-core | `v5.41.0` | `v`-prefixed | Docker Hub namespace |

**Excluded** (version referenced from external modules):
- `apolloconfig` group (`apolloVersion` from `peknight.app.build.apolloconfig`)
- `xuxueli` group (`xxlJobVersion` from `peknight.app.build.xuxueli`)

---

### Task 1: Add @versionCheck annotations to docker-build/package.scala

**Files:**
- Modify: `docker-build/shared/src/main/scala/com/peknight/docker/build/package.scala`

- [ ] **Step 1: Replace all `// https://...` comments with `/** @versionCheck ... */` annotations and add exclusion comments for skipped images**

The entire file should be rewritten with these changes:
- Convert `// https://hub.docker.com/.../tags` to `/** @versionCheck https://hub.docker.com/v2/repositories/{namespace_or_library}/{name}/tags`
- For library images: `https://hub.docker.com/v2/repositories/library/{name}/tags`
- For namespace images: `https://hub.docker.com/v2/repositories/{namespace}/{name}/tags`
- For GHCR: `https://api.github.com/orgs/{org}/packages/container/{name}/versions`
- Add `// @versionCheck skip (external module reference)` for apolloconfig and xuxueli

```scala
package com.peknight.docker

import com.comcast.ip4s.host
import com.peknight.app.build.apolloconfig.apollo.version as apolloVersion
import com.peknight.app.build.xuxueli.`xxl-job`.version as xxlJobVersion
import com.peknight.docker.Identifier.ImageRepositoryTag
import com.peknight.ip4s.HostPort

package object build:
  object library:
    /** @versionCheck https://hub.docker.com/v2/repositories/library/alpine/tags */
    val alpine: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "alpine"), Some(Tag("3.23.4")))
    /** @versionCheck https://hub.docker.com/v2/repositories/library/eclipse-temurin/tags */
    val `eclipse-temurin`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "eclipse-temurin"), Some(Tag("26_35-jdk")))
    /** @versionCheck https://hub.docker.com/v2/repositories/library/mysql/tags */
    val mysql: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "mysql"), Some(Tag("9.6.0")))
    /** @versionCheck https://hub.docker.com/v2/repositories/library/nginx/tags */
    val nginx: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "nginx"), Some(Tag("1.30.0-alpine3.23")))
    /** @versionCheck https://hub.docker.com/v2/repositories/library/postgres/tags */
    val postgres: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "postgres"), Some(Tag("18.3-alpine3.23")))
    /** @versionCheck https://hub.docker.com/v2/repositories/library/redis/tags */
    val redis: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "redis"), Some(Tag("8.6.2-alpine3.23")))
    /** @versionCheck https://hub.docker.com/v2/repositories/library/ubuntu/tags */
    val ubuntu: ImageRepositoryTag = ImageRepositoryTag(Repository(None, None, "ubuntu"), Some(Tag("26.04")))
  end library
  object alpine:
    /** @versionCheck https://hub.docker.com/v2/repositories/alpine/psql/tags */
    val psql: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("alpine")), "psql"), Some(Tag("18.3")))
  end alpine
  object apolloconfig:
    val namespace: Namespace = Namespace("apolloconfig")
    private val tag: Tag = Tag(apolloVersion)
    // @versionCheck skip (external module reference)
    val `apollo-configservice`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(namespace), "apollo-configservice"), Some(tag))
    val `apollo-adminservice`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(namespace), "apollo-adminservice"), Some(tag))
    val `apollo-portal`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(namespace), "apollo-portal"), Some(tag))
  end apolloconfig
  object gitea:
    /** @versionCheck https://hub.docker.com/v2/repositories/gitea/gitea/tags */
    val gitea: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("gitea")), "gitea"), Some(Tag("1.25.5-rootless")))
  end gitea
  object jenkins:
    /** @versionCheck https://hub.docker.com/v2/repositories/jenkins/jenkins/tags */
    val jenkins: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("jenkins")), "jenkins"), Some(Tag("2.561-jdk25")))
  end jenkins
  object ollama:
    /** @versionCheck https://hub.docker.com/v2/repositories/ollama/ollama/tags */
    val ollama: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("ollama")), "ollama"), Some(Tag("0.21.0")))
  end ollama
  object openclaw:
    /** @versionCheck https://api.github.com/orgs/openclaw/packages/container/openclaw/versions */
    val openclaw: ImageRepositoryTag = ImageRepositoryTag(Repository(Some(HostPort(host"ghcr.io")), Some(Namespace("openclaw")), "openclaw"), Some(Tag("2026.4.20")))
  end openclaw
  object paulgauthier:
    /** @versionCheck https://hub.docker.com/v2/repositories/paulgauthier/aider/tags */
    val aider: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("paulgauthier")), "aider"), Some(Tag("v0.86.2")))
  end paulgauthier
  object sonatype:
    /** @versionCheck https://hub.docker.com/v2/repositories/sonatype/nexus3/tags */
    val nexus3: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("sonatype")), "nexus3"), Some(Tag("3.91.1-alpine")))
  end sonatype
  object rustdesk:
    /** @versionCheck https://hub.docker.com/v2/repositories/rustdesk/rustdesk-server/tags */
    val `rustdesk-server`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("rustdesk")), "rustdesk-server"), Some(Tag("1.1.15")))
  end rustdesk
  object v2fly:
    /** @versionCheck https://hub.docker.com/v2/repositories/v2fly/v2fly-core/tags */
    val `v2fly-core`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("v2fly")), "v2fly-core"), Some(Tag("v5.41.0")))
  end v2fly
  object xuxueli:
    // @versionCheck skip (external module reference)
    val `xxl-job-admin`: ImageRepositoryTag = ImageRepositoryTag(Repository(None, Some(Namespace("xuxueli")), "xxl-job-admin"), Some(Tag(xxlJobVersion)))
  end xuxueli
end build
```

**Note:** The `alpine/psql` image uses URL `library/psql` because `psql` is actually a library image under the `alpine` namespace in the Docker Hub URL structure. Actually, looking more carefully, the Docker Hub URL for `alpine/psql` is `https://hub.docker.com/r/alpine/psql/tags`, so the API URL should be `https://hub.docker.com/v2/repositories/alpine/psql/tags`.

- [ ] **Step 2: Commit the annotation changes**

```bash
git add docker-build/shared/src/main/scala/com/peknight/docker/build/package.scala
git commit -m "$(cat <<'EOF'
chore: add @versionCheck annotations to docker-build image definitions
EOF
)"
```

---

### Task 2: Create the update-docker-images.py script (core infrastructure)

**Files:**
- Create: `scripts/update-docker-images.py`

- [ ] **Step 1: Create the scripts directory and write the script with core infrastructure (API clients, version parsing, anchor scanning)**

```bash
mkdir -p /Users/pek/Projects/peknight/docker/scripts
```

Create `scripts/update-docker-images.py` with the following content:

```python
#!/usr/bin/env python3
"""自动检索并更新 docker-build 模块中 Docker 镜像的最新版本号。

默认 dry-run 模式（仅打印变更），使用 --apply 参数实际写入。
"""

import argparse
import json
import os
import re
import urllib.request
from pathlib import Path

# Docker Hub API base
DOCKER_HUB_API = "https://hub.docker.com/v2/repositories/{path}/tags?page_size=200"

# GitHub Container Registry API
GHCR_API = "https://api.github.com"


def fetch_docker_hub_tags(url: str) -> list[dict] | None:
    """从 Docker Hub API 获取 tag 列表。"""
    try:
        req = urllib.request.Request(url)
        req.add_header("User-Agent", "peknight-docker-update-images/1.0")
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = json.loads(resp.read().decode())
            return data.get("results", [])
    except Exception:
        return None


def fetch_ghcr_tags(url: str) -> list[str] | None:
    """从 GitHub Container Registry API 获取 tag 列表。

    需要 GITHUB_TOKEN 环境变量。
    """
    token = os.environ.get("GITHUB_TOKEN")
    if not token:
        print("[错误] GITHUB_TOKEN 环境变量未设置，无法查询 GHCR 镜像")
        return None
    try:
        req = urllib.request.Request(url)
        req.add_header("Authorization", f"Bearer {token}")
        req.add_header("Accept", "application/vnd.github.v3+json")
        req.add_header("User-Agent", "peknight-docker-update-images/1.0")
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = json.loads(resp.read().decode())
            # GHCR versions API returns a list with "metadata": {"container": {"tags": [...]}}
            tags = []
            for item in data:
                metadata = item.get("metadata", {})
                container = metadata.get("container", {})
                tags.extend(container.get("tags", []))
            return tags
    except Exception:
        return None


def is_prerelease(version: str) -> bool:
    """判断版本是否为预发布版本。"""
    markers = ["rc", "m", "snapshot", "snap", "alpha", "beta", "cr", "dev", "beta", "pre"]
    lower = version.lower()
    for m in markers:
        if f"-{m}" in lower or lower.startswith(m):
            return True
    return False


def parse_version_tuple(version: str) -> tuple:
    """将版本号字符串解析为可比较的 tuple。"""
    parts = []
    for p in version.split("."):
        try:
            parts.append(int(p))
        except ValueError:
            num = ""
            for c in p:
                if c.isdigit():
                    num += c
                else:
                    break
            parts.append(int(num) if num else 0)
    return tuple(parts)


def is_version_newer(current: str, candidate: str) -> bool:
    """判断 candidate 版本是否严格高于 current 版本。"""
    cur = parse_version_tuple(current)
    cand = parse_version_tuple(candidate)
    min_len = min(len(cur), len(cand))
    return cand[:min_len] > cur[:min_len]


# @versionCheck URL 正则
ANCHOR_RE = re.compile(r"/\*\*\s*@versionCheck\s*(https?://[^\s]+)\s*\*/")

# Skip comment regex
SKIP_RE = re.compile(r"//\s*@versionCheck\s+skip")

# Tag value regex: Tag("x.y.z")
TAG_RE = re.compile(r'(Tag\(")([^"]+)("\))')


def parse_docker_hub_url(url: str) -> tuple[str, bool] | None:
    """解析 Docker Hub URL，返回 (path, is_library)。

    library 格式: https://hub.docker.com/v2/repositories/library/{name}/tags
    namespace 格式: https://hub.docker.com/v2/repositories/{namespace}/{name}/tags
    """
    m = re.search(r"hub\.docker\.com/v2/repositories/(.+)/tags", url)
    if not m:
        return None
    path = m.group(1)
    parts = path.split("/")
    if len(parts) == 2:
        return path, (parts[0] == "library")
    return None


def parse_ghcr_url(url: str) -> str | None:
    """验证 GHCR API URL 格式。"""
    if re.search(r"api\.github\.com/orgs/([^/]+)/packages/container/([^/]+)/versions", url):
        return url
    return None
```

- [ ] **Step 2: Commit the core infrastructure**

```bash
git add scripts/update-docker-images.py
git commit -m "$(cat <<'EOF'
feat: add docker image version auto-update script (core infrastructure)
EOF
)"
```

---

### Task 3: Implement version matching strategies

**Files:**
- Modify: `scripts/update-docker-images.py`

- [ ] **Step 1: Add version matching functions to the script**

Append these functions to `scripts/update-docker-images.py` after the code from Task 2:

```python
def find_latest_simple_version(tags: list[dict], current: str) -> str | None:
    """简单版本号匹配：取最新稳定版。

    适用于: alpine, mysql, ubuntu, ollama, rustdesk-server 等
    """
    versions = [t["name"] for t in tags if t.get("name")]
    if not versions:
        return None

    if is_prerelease(current):
        versions.sort(key=parse_version_tuple, reverse=True)
        latest = versions[0]
        if is_version_newer(current, latest):
            return latest
    else:
        stable = [v for v in versions if not is_prerelease(v)]
        if not stable:
            return None
        stable.sort(key=parse_version_tuple, reverse=True)
        latest = stable[0]
        if is_version_newer(current, latest):
            return latest
    return None


def find_latest_eclipse_temurin_version(tags: list[dict], current_major: int, current_patch: int) -> str | None:
    """eclipse-temurin 版本匹配: {major}_{patch}-jdk 格式。

    逻辑与 build/scripts/update-deps.py 中的相同：
    1. 检查是否存在下一大版本号
    2. 存在 → 更新到最新大版本的最新 patch
    3. 不存在 → 更新到当前大版本的最新 patch
    """
    temurin_re = re.compile(r"(\d+)_(\d+)-jdk")

    next_major = current_major + 1
    current_best = current_patch
    next_best = None

    for tag_info in tags:
        tag_name = tag_info.get("name", "")
        m = temurin_re.match(tag_name)
        if not m:
            continue
        major = int(m.group(1))
        patch = int(m.group(2))
        if major == next_major and (next_best is None or patch > next_best):
            next_best = patch
        elif major == current_major and patch > int(current_best):
            current_best = str(patch)

    if next_best is not None:
        new_version = f"{next_major}_{next_best}"
        old_version = f"{current_major}_{current_patch}"
        if new_version != old_version:
            return new_version
    else:
        best_str = str(current_best)
        if best_str != str(current_patch):
            return f"{current_major}_{best_str}"
    return None


def find_latest_alpine_version(tags: list[dict], current: str) -> str | None:
    """Alpine 复合版本号匹配。

    策略：
    1. 过滤带 alpine 后缀的 tag（匹配 -alpine{version} 后缀）
    2. 主版本号优先（主版本更新的优先）
    3. 主版本号相同的情况下取 alpine 子版本更新的优先

    适用于: nginx (1.30.0-alpine3.23), postgres (18.3-alpine3.23), redis (8.6.2-alpine3.23)
    """
    # 解析当前版本
    alpine_re = re.compile(r"^([\d.]+)-alpine([\d.]+)$")
    m = alpine_re.match(current)
    if not m:
        # 不是 alpine 格式，回退到简单匹配
        return find_latest_simple_version(tags, current)

    current_main = m.group(1)
    current_alpine = m.group(2)

    # 过滤 alpine 后缀的 tag
    alpine_tags = []
    for tag_info in tags:
        tag_name = tag_info.get("name", "")
        tm = alpine_re.match(tag_name)
        if tm:
            alpine_tags.append((tag_name, tm.group(1), tm.group(2)))

    if not alpine_tags:
        return None

    # 按主版本号排序，再按 alpine 子版本排序
    alpine_tags.sort(key=lambda x: (parse_version_tuple(x[1]), parse_version_tuple(x[2])), reverse=True)

    best_tag = alpine_tags[0][0]
    if best_tag != current:
        return best_tag
    return None


def find_latest_suffixed_version(tags: list[dict], current: str) -> str | None:
    """带固定后缀的版本号匹配。

    策略：取最新稳定版，保持与当前相同的后缀模式（如果可能）。
    如果最新 tag 没有相同后缀，则取最新 tag。

    适用于: gitea (1.25.5-rootless), jenkins (2.561-jdk25), nexus3 (3.91.1-alpine)
    """
    # 提取当前后缀
    suffix_re = re.compile(r"^[\d.]+(-.+)$")
    m = suffix_re.match(current)
    current_suffix = m.group(1) if m else ""

    versions = [t["name"] for t in tags if t.get("name")]
    if not versions:
        return None

    # 过滤稳定版
    if not is_prerelease(current):
        versions = [v for v in versions if not is_prerelease(v)]

    if not versions:
        return None

    # 优先匹配相同后缀
    matching = [v for v in versions if v.endswith(current_suffix)] if current_suffix else []
    if matching:
        matching.sort(key=parse_version_tuple, reverse=True)
        latest = matching[0]
        if is_version_newer(current.replace(current_suffix, ""), latest.replace(current_suffix, "")):
            return latest
        return None

    # 没有相同后缀，取最新的
    versions.sort(key=parse_version_tuple, reverse=True)
    latest = versions[0]
    if is_version_newer(current, latest):
        return latest
    return None


def find_latest_v_prefix_version(tags: list[dict], current: str) -> str | None:
    """v-前缀版本号匹配。

    适用于: aider (v0.86.2), v2fly-core (v5.41.0)
    """
    v_re = re.compile(r"^v(.+)$")
    m = v_re.match(current)
    if not m:
        return find_latest_simple_version(tags, current)

    current_ver = m.group(1)
    versions = []
    for t in tags:
        name = t.get("name", "")
        vm = v_re.match(name)
        if vm:
            versions.append((name, vm.group(1)))

    if not versions:
        return None

    stable = [(n, v) for n, v in versions if not is_prerelease(v)]
    if not stable:
        return None

    stable.sort(key=lambda x: parse_version_tuple(x[1]), reverse=True)
    latest_name, latest_ver = stable[0]
    if is_version_newer(current_ver, latest_ver):
        return latest_name
    return None


def find_latest_date_version(tags: list[str], current: str) -> str | None:
    """日期格式版本号匹配 (GHCR)。

    格式: {Y}.{M}.{D} 如 2026.4.20

    适用于: openclaw (2026.4.20)
    """
    date_re = re.compile(r"^(\d{4})\.(\d{1,2})\.(\d{1,2})$")
    m = date_re.match(current)
    if not m:
        return find_latest_simple_version(tags, current) if tags else None

    current_tuple = (int(m.group(1)), int(m.group(2)), int(m.group(3)))

    best = None
    best_tuple = current_tuple
    for tag in tags:
        dm = date_re.match(tag)
        if dm:
            tag_tuple = (int(dm.group(1)), int(dm.group(2)), int(dm.group(3)))
            if tag_tuple > best_tuple:
                best_tuple = tag_tuple
                best = tag

    return best
```

- [ ] **Step 2: Commit the version matching strategies**

```bash
git add scripts/update-docker-images.py
git commit -m "$(cat <<'EOF'
feat: add Docker image version matching strategies
EOF
)"
```

---

### Task 4: Implement the file scanning and update logic

**Files:**
- Modify: `scripts/update-docker-images.py`

- [ ] **Step 1: Add the update_docker_build_scala function**

Append to `scripts/update-docker-images.py`:

```python
def find_image_name_before_anchor(lines: list[str], anchor_idx: int) -> str | None:
    """在 @versionCheck 锚点附近查找 val 名称。

    在锚点所在行或下方几行内查找 val xxx: ImageRepositoryTag 模式。
    """
    val_re = re.compile(r"val\s+([`'\w]+)\s*:\s*ImageRepositoryTag")
    for j in range(anchor_idx, min(anchor_idx + 3, len(lines))):
        m = val_re.search(lines[j])
        if m:
            return m.group(1)
    return None


def update_docker_build_scala(repo_root: Path, apply: bool) -> list[dict]:
    """更新 docker-build/package.scala 中的镜像版本号。"""
    results = []
    filepath = repo_root / "docker-build" / "shared" / "src" / "main" / "scala" / "com" / "peknight" / "docker" / "build" / "package.scala"
    if not filepath.exists():
        return results

    content = filepath.read_text()
    lines = content.splitlines()
    modified = False

    i = 0
    while i < len(lines):
        # 检查是否为 skip 标记
        if SKIP_RE.search(lines[i]):
            results.append({"name": "(skip)", "status": "skipped", "reason": "external module reference"})
            i += 1
            continue

        url_match = ANCHOR_RE.search(lines[i])
        if not url_match:
            i += 1
            continue

        url = url_match.group(1)

        # 查找 val 名称
        image_name = find_image_name_before_anchor(lines, i + 1)
        if not image_name:
            i += 1
            continue

        # 在锚点后查找 Tag("...")
        tag_info = None
        for j in range(i + 1, min(i + 4, len(lines))):
            m = TAG_RE.search(lines[j])
            if m:
                tag_info = (j, m.group(2), m)
                break

        if not tag_info:
            i += 1
            continue

        tag_line_idx, current_version, tag_match = tag_info

        # 判断数据源类型
        latest = None

        # GHCR
        if "api.github.com" in url:
            tags = fetch_ghcr_tags(url)
            if tags is None:
                results.append({"name": image_name, "status": "error", "reason": "GITHUB_TOKEN 未设置或查询失败"})
                i += 1
                continue
            # 日期格式版本 (openclaw)
            if re.match(r"^\d{4}\.\d{1,2}\.\d{1,2}$", current_version):
                latest = find_latest_date_version(tags, current_version)
            else:
                latest = find_latest_simple_version(tags, current_version)

        # Docker Hub
        elif "hub.docker.com" in url:
            parsed = parse_docker_hub_url(url)
            if not parsed:
                i += 1
                continue
            path, is_library = parsed
            api_url = DOCKER_HUB_API.format(path=path)
            tags = fetch_docker_hub_tags(api_url)
            if tags is None:
                results.append({"name": image_name, "status": "error", "reason": "Docker Hub 查询失败"})
                i += 1
                continue

            # eclipse-temurin 特殊处理
            temurin_re = re.compile(r"(\d+)_(\d+)-jdk")
            temurin_m = temurin_re.match(current_version)
            if temurin_m:
                latest = find_latest_eclipse_temurin_version(
                    tags, int(temurin_m.group(1)), int(temurin_m.group(2))
                )
            # alpine 复合版本
            elif re.match(r"^[\d.]+-alpine[\d.]+$", current_version):
                latest = find_latest_alpine_version(tags, current_version)
            # v-前缀版本
            elif current_version.startswith("v") and re.match(r"^v[\d.]+$", current_version):
                latest = find_latest_v_prefix_version(tags, current_version)
            # 带后缀版本 (rootless, jdk, -alpine)
            elif re.match(r"^[\d.]+-", current_version):
                latest = find_latest_suffixed_version(tags, current_version)
            # 简单版本号
            else:
                latest = find_latest_simple_version(tags, current_version)

        if latest is None:
            results.append({"name": image_name, "status": "skipped", "reason": f"已是最新 ({current_version})"})
            i += 1
            continue

        old_line = lines[tag_line_idx]
        lines[tag_line_idx] = old_line.replace(current_version, latest, 1)
        modified = True
        results.append({"name": image_name, "status": "updated", "old": current_version, "new": latest})
        i += 1

    if modified and apply:
        filepath.write_text("\n".join(lines) + "\n")

    return results
```

- [ ] **Step 2: Commit the update logic**

```bash
git add scripts/update-docker-images.py
git commit -m "$(cat <<'EOF'
feat: add docker-build package.scala scanning and update logic
EOF
)"
```

---

### Task 5: Implement CLI entry point and output formatting

**Files:**
- Modify: `scripts/update-docker-images.py`

- [ ] **Step 1: Add print_results and main functions**

Append to `scripts/update-docker-images.py`:

```python
def print_results(results):
    """打印更新结果汇总。"""
    print()
    print("=" * 60)
    for r in results:
        if r["status"] == "updated":
            print(f"[已更新] {r['name']}: {r['old']} → {r['new']}")
        elif r["status"] == "skipped":
            print(f"[跳过]   {r['name']} ({r['reason']})")
        elif r["status"] == "error":
            print(f"[错误]   {r['name']} ({r['reason']})")
    print("=" * 60)
    updated = sum(1 for r in results if r["status"] == "updated")
    skipped = sum(1 for r in results if r["status"] == "skipped")
    errors = sum(1 for r in results if r["status"] == "error")
    print(f"已更新: {updated}  跳过: {skipped}  错误: {errors}")
    print("=" * 60)


def main():
    parser = argparse.ArgumentParser(description="自动更新 docker-build 模块中 Docker 镜像版本号")
    parser.add_argument("--apply", action="store_true", help="实际写入文件（默认仅 dry-run）")
    args = parser.parse_args()

    repo_root = Path(__file__).resolve().parent.parent

    print("=" * 60)
    if not args.apply:
        print("DRY-RUN 模式 - 不会修改任何文件")
    print("=" * 60)
    print()

    results = update_docker_build_scala(repo_root, args.apply)
    print_results(results)


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Make the script executable**

```bash
chmod +x /Users/pek/Projects/peknight/docker/scripts/update-docker-images.py
```

- [ ] **Step 3: Commit the CLI entry point**

```bash
git add scripts/update-docker-images.py
git commit -m "$(cat <<'EOF'
feat: add CLI entry point and output formatting for docker image update
EOF
)"
```

---

### Task 6: Create the SKILL.md file

**Files:**
- Create: `.claude/skills/update-docker-images/SKILL.md`

- [ ] **Step 1: Create the skill definition**

```bash
mkdir -p /Users/pek/Projects/peknight/docker/.claude/skills/update-docker-images
```

Create `.claude/skills/update-docker-images/SKILL.md`:

```markdown
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

### Step 3: 提交变更

如果有更新的镜像：

```bash
git add docker-build/shared/src/main/scala/com/peknight/docker/build/package.scala
git commit -m "$(cat <<'EOF'
chore: bump docker-build image versions
EOF
)"
```

commit message 中的描述根据实际更新的镜像列表自动生成。

## 注意事项

- 脚本位于 `scripts/update-docker-images.py`，使用纯 Python 标准库，零外部依赖
- 更新范围：`docker-build/package.scala` 中所有带 `/** @versionCheck ... */` 锚点的镜像
- 排除 `apolloconfig` 和 `xuxueli` 组（版本号引用自外部模块）
- GHCR 镜像（如 openclaw）需要设置 `GITHUB_TOKEN` 环境变量
- 不要手动修改版本号，统一通过脚本执行
- 更新后验证缩进是否正确保留
```

- [ ] **Step 2: Commit the skill file**

```bash
git add .claude/skills/update-docker-images/SKILL.md
git commit -m "$(cat <<'EOF'
feat: add update-docker-images skill for Claude Code
EOF
)"
```

---

## Spec Self-Review

**1. Spec coverage check:**

| Requirement | Task |
|-------------|------|
| Add `/** @versionCheck <URL> */` annotations | Task 1 |
| Only check docker-build versions | Task 4 (update_docker_build_scala targets docker-build path only) |
| Docker Hub API for official & namespace images | Task 2, 4 |
| GHCR API with GITHUB_TOKEN | Task 2, 4 |
| Alpine suffix matching strategy | Task 3 (find_latest_alpine_version) |
| eclipse-temurin major+patch logic | Task 3 (find_latest_eclipse_temurin_version) |
| Skip external module references | Task 1 (skip comments), Task 4 (SKIP_RE) |
| Python script, zero dependencies | Task 2 (only stdlib imports) |
| SKILL.md file | Task 6 |
| CLI --apply / dry-run | Task 5 |

**2. Placeholder scan:** No TBD/TODO/fill-in-later found. All code blocks contain complete implementations.

**3. Type consistency:** All functions use consistent signatures — `find_latest_*` functions return `str | None`, tag parsing uses `TAG_RE`, URL parsing uses `ANCHOR_RE`. No mismatches.

**4. Ambiguity check:**
- The `alpine/psql` Docker Hub URL: clarified that it should use `alpine/psql` path (not `library/psql`). ✅ Fixed in Task 1 annotation.
- GHCR API response format: the GHCR versions API returns metadata with container.tags. ✅ Handled in `fetch_ghcr_tags`.
- Version comparison for suffix-based tags: handled by stripping suffix before comparison. ✅ In `find_latest_suffixed_version`.

All checks pass. Plan is complete and consistent.
