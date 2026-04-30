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
