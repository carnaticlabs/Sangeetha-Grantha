#!/usr/bin/env python3
"""Report newer stable releases for Sangita Grantha's direct libraries.

Compares three manifests with upstream. Does not modify any file.

  * gradle/libs.versions.toml
        One probe per [versions] key. The key is a single pin shared by every
        module or plugin that references it, so the first library module (or,
        if the key is plugin-only, the first plugin) stands in for the train.
  * modules/frontend/sangita-admin-web/package.json
        dependencies and devDependencies. Resolved versions come from bun.lock.
  * tools/krithi-extract-enrich-worker/pyproject.toml
        project dependencies and the dev extra. Resolved versions come from uv.lock.

archive/ is not scanned. Transitive lockfile entries, .mise.toml, the Gradle
wrapper, and container image tags are out of scope.

Latest means the newest stable release. Pre-releases (alpha, beta, rc, eap,
milestone, snapshot) are ignored.

Usage:
    python3 tools/check-library-versions.py
    python3 tools/check-library-versions.py --include-current
    python3 tools/check-library-versions.py --json
    python3 tools/check-library-versions.py --ecosystem npm
    python3 tools/check-library-versions.py --self-test

Exit 0 when the report is complete (updates are not a failure).
Exit 1 when a manifest is missing or --self-test fails.
Exit 2 when every upstream probe fails.
"""

from __future__ import annotations

import argparse
import json
import re
import ssl
import sys
import tomllib
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "gradle" / "libs.versions.toml"
PACKAGE_JSON = ROOT / "modules" / "frontend" / "sangita-admin-web" / "package.json"
BUN_LOCK = ROOT / "modules" / "frontend" / "sangita-admin-web" / "bun.lock"
PYPROJECT = ROOT / "tools" / "krithi-extract-enrich-worker" / "pyproject.toml"
UV_LOCK = ROOT / "tools" / "krithi-extract-enrich-worker" / "uv.lock"

USER_AGENT = "sangita-grantha-library-version-audit"
# PyPI's per-project JSON includes every historical file. rapidfuzz is ~9MB.
MAX_BODY = 20_000_000
LIBRARY_REPOS = (
    "https://repo1.maven.org/maven2",
    "https://dl.google.com/dl/android/maven2",
    "https://maven.pkg.jetbrains.space/public/p/compose/dev",
)
PLUGIN_REPOS = (
    "https://plugins.gradle.org/m2",
    "https://dl.google.com/dl/android/maven2",
    "https://repo1.maven.org/maven2",
)

BUMP_ORDER = {"major": 0, "minor": 1, "patch": 2, "other": 3}

# Report order. Gradle covers the JVM catalog (Kotlin, Ktor, Android, and Java libraries).
STACKS = (
    ("gradle", "Java (including Kotlin)"),
    ("npm", "Frontend"),
    ("pypi", "Python"),
)


def stack_label(ecosystem: str) -> str:
    for key, label in STACKS:
        if key == ecosystem:
            return label
    return ecosystem


@dataclass(frozen=True)
class Item:
    ecosystem: str
    library: str
    declared: str
    resolved: str
    notes: str
    # gradle: ("maven", group, artifact) or ("plugin", plugin_id)
    probe: tuple[str, ...] = ()


@dataclass
class Row:
    ecosystem: str
    library: str
    declared: str
    resolved: str
    latest: str
    bump: str
    update_via: str
    notes: str
    status: str  # update | current | unchecked


def stable_key(version: str) -> tuple[int, ...] | None:
    """Numeric core of a stable release, or None for pre-releases and other qualifiers."""
    raw = version.strip()
    if len(raw) >= 2 and raw[0] in "vV" and raw[1].isdigit():
        raw = raw[1:]
    raw = raw.split("+", 1)[0]
    match = re.match(r"^(\d+(?:\.\d+)*)(.*)$", raw)
    if not match:
        return None
    rest = match.group(2)
    if rest != "" and not re.fullmatch(r"(?i)[.\-]?(final|release|ga)", rest):
        return None
    return tuple(int(part) for part in match.group(1).split("."))


def cmp_versions(left: tuple[int, ...], right: tuple[int, ...]) -> int:
    width = max(len(left), len(right))
    a = left + (0,) * (width - len(left))
    b = right + (0,) * (width - len(right))
    return (a > b) - (a < b)


def bump_kind(old: tuple[int, ...], new: tuple[int, ...]) -> str:
    width = max(len(old), len(new), 1)
    a = old + (0,) * (width - len(old))
    b = new + (0,) * (width - len(new))
    if b == a:
        return "current"
    if b[0] != a[0]:
        return "major"
    if width > 1 and b[1] != a[1]:
        return "minor"
    return "patch"


def _caret_upper(base: tuple[int, ...]) -> tuple[int, ...]:
    # npm caret: ^1.2.3 → <2.0.0; ^0.2.3 → <0.3.0; ^0.0.3 → <0.0.4
    if base[0] > 0 or len(base) == 1:
        return (base[0] + 1,)
    if len(base) > 1 and base[1] > 0:
        return (0, base[1] + 1)
    if len(base) > 2:
        return (0, 0, base[2] + 1)
    return (0, 1)


def _tilde_upper(base: tuple[int, ...]) -> tuple[int, ...]:
    if len(base) == 1:
        return (base[0] + 1,)
    return (base[0], base[1] + 1)


def satisfies(spec: str, version: str) -> bool | None:
    """True/False if `version` satisfies an npm range or PEP 440 fragment. None if unknown."""
    if "||" in spec:
        return None
    parts = [part.strip() for part in spec.split(",") if part.strip()]
    if not parts:
        return True
    answers: list[bool] = []
    for part in parts:
        one = _satisfies_one(part, version)
        if one is None:
            return None
        answers.append(one)
    return all(answers)


def _satisfies_one(spec: str, version: str) -> bool | None:
    key = stable_key(version)
    if key is None:
        return None
    match = re.match(
        r"^(~=|>=|<=|>|<|\^|~|==|===)?\s*v?(\d+(?:\.\d+)*)([^\s]*)\s*$",
        spec.strip(),
    )
    if not match:
        return None
    op = match.group(1) or "=="
    rest = match.group(3) or ""
    if rest and not re.fullmatch(r"(?i)[.\-]?(final|release|ga)", rest):
        return None
    base = tuple(int(part) for part in match.group(2).split("."))
    if op in ("==", "==="):
        return cmp_versions(key, base) == 0
    if op == ">=":
        return cmp_versions(key, base) >= 0
    if op == ">":
        return cmp_versions(key, base) > 0
    if op == "<=":
        return cmp_versions(key, base) <= 0
    if op == "<":
        return cmp_versions(key, base) < 0
    if op == "^":
        return cmp_versions(key, base) >= 0 and cmp_versions(key, _caret_upper(base)) < 0
    if op == "~":
        return cmp_versions(key, base) >= 0 and cmp_versions(key, _tilde_upper(base)) < 0
    if op == "~=":
        if len(base) < 2:
            return None
        upper = base[:-1]
        upper = upper[:-1] + (upper[-1] + 1,)
        return cmp_versions(key, base) >= 0 and cmp_versions(key, upper) < 0
    return None


def _version_ref(table: dict) -> str | None:
    if isinstance(table.get("version.ref"), str):
        return table["version.ref"]
    version = table.get("version")
    if isinstance(version, dict) and isinstance(version.get("ref"), str):
        return version["ref"]
    return None


def _literal_version(table: dict) -> str | None:
    version = table.get("version")
    if isinstance(version, str):
        return version
    return None


def _catalog_comments(text: str) -> dict[str, str]:
    comments: dict[str, str] = {}
    in_versions = False
    pending: list[str] = []
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("[") and stripped.endswith("]"):
            in_versions = stripped == "[versions]"
            pending = []
            continue
        if not in_versions:
            continue
        if stripped.startswith("#"):
            pending.append(stripped.lstrip("#").strip())
            continue
        match = re.match(r'^([A-Za-z0-9_-]+)\s*=\s*"[^"]+"\s*(?:#\s*(.*))?$', stripped)
        if match:
            notes = list(pending)
            if match.group(2):
                notes.append(match.group(2).strip())
            if notes:
                comments[match.group(1)] = " ".join(notes)
            pending = []
            continue
        if stripped == "":
            continue
        pending = []
    return comments


def _normalize_pypi(name: str) -> str:
    return re.sub(r"[-_.]+", "-", name).lower()


def gradle_items() -> list[Item]:
    text = CATALOG.read_text(encoding="utf-8")
    catalog = tomllib.loads(text)
    comments = _catalog_comments(text)
    versions: dict = catalog.get("versions", {})
    libraries: dict[str, list[str]] = {key: [] for key in versions}
    plugins: dict[str, list[str]] = {key: [] for key in versions}
    literals: list[tuple[str, str]] = []

    for alias, table in catalog.get("libraries", {}).items():
        if not isinstance(table, dict):
            continue
        module = table.get("module")
        if not isinstance(module, str) or ":" not in module:
            continue
        ref = _version_ref(table)
        if ref and ref in libraries:
            libraries[ref].append(module)
            continue
        literal = _literal_version(table)
        if literal:
            literals.append((alias, f"{module}=={literal}"))

    for alias, table in catalog.get("plugins", {}).items():
        if not isinstance(table, dict):
            continue
        plugin_id = table.get("id")
        if not isinstance(plugin_id, str):
            continue
        ref = _version_ref(table)
        if ref and ref in plugins:
            plugins[ref].append(plugin_id)

    items: list[Item] = []
    for key, declared in versions.items():
        if not isinstance(declared, str):
            continue
        modules = libraries.get(key, [])
        plugin_ids = plugins.get(key, [])
        shared = len(modules) + len(plugin_ids)
        notes: list[str] = []
        if key in comments:
            notes.append(comments[key])
        if shared > 1:
            notes.append(f"catalog key shared by {shared} entries")
        if modules:
            group, artifact = modules[0].split(":", 1)
            notes.append(f"probed {modules[0]}")
            probe: tuple[str, ...] = ("maven", group, artifact)
        elif plugin_ids:
            notes.append(f"probed plugin {plugin_ids[0]}")
            probe = ("plugin", plugin_ids[0])
        else:
            notes.append("no module or plugin references this key")
            probe = ()
        items.append(
            Item(
                ecosystem="gradle",
                library=key,
                declared=declared,
                resolved=declared,
                notes="; ".join(notes),
                probe=probe,
            )
        )
    for alias, spec in literals:
        module, _, version = spec.partition("==")
        group, artifact = module.split(":", 1)
        items.append(
            Item(
                ecosystem="gradle",
                library=alias,
                declared=version,
                resolved=version,
                notes=f"inline version; probed {module}",
                probe=("maven", group, artifact),
            )
        )
    return items


def _load_json_lenient(text: str) -> dict:
    """json.loads, after removing trailing commas. bun.lock is JSON-with-commas."""
    cleaned = re.sub(r",(\s*[}\]])", r"\1", text)
    loaded = json.loads(cleaned)
    if not isinstance(loaded, dict):
        raise ValueError("expected a JSON object")
    return loaded


def _bun_resolved(name: str, lock: dict | None) -> str:
    if not lock:
        return ""
    entry = lock.get("packages", {}).get(name)
    if not isinstance(entry, list) or not entry or not isinstance(entry[0], str):
        return ""
    return entry[0].rsplit("@", 1)[-1]


def npm_items() -> list[Item]:
    package = json.loads(PACKAGE_JSON.read_text(encoding="utf-8"))
    lock = None
    if BUN_LOCK.is_file():
        lock = _load_json_lenient(BUN_LOCK.read_text(encoding="utf-8"))
    items: list[Item] = []
    for field, label in (
        ("dependencies", "dependency"),
        ("devDependencies", "devDependency"),
        ("optionalDependencies", "optionalDependency"),
    ):
        for name, spec in package.get(field, {}).items():
            if not isinstance(spec, str):
                continue
            if spec.startswith(("workspace:", "file:", "link:", "git:", "github:")):
                continue
            items.append(
                Item(
                    ecosystem="npm",
                    library=name,
                    declared=spec,
                    resolved=_bun_resolved(name, lock),
                    notes=label,
                    probe=("npm", name),
                )
            )
    return items


def _requirement_comments(text: str) -> dict[str, str]:
    comments: dict[str, str] = {}
    pattern = re.compile(
        r"""["']([A-Za-z0-9_.-]+)(?:\[[^\]]+\])?[^"']*["'].*#\s*(.+)$"""
    )
    for line in text.splitlines():
        match = pattern.search(line)
        if match and match.group(2).strip():
            comments.setdefault(_normalize_pypi(match.group(1)), match.group(2).strip())
    return comments


def _split_requirement(requirement: str) -> tuple[str, str]:
    match = re.match(r"^([A-Za-z0-9_.-]+)(?:\[[^\]]+\])?\s*(.*)$", requirement.strip())
    if not match:
        return requirement.strip(), ""
    return match.group(1), match.group(2).strip()


def _uv_resolved(lock_text: str | None) -> dict[str, str]:
    if not lock_text:
        return {}
    lock = tomllib.loads(lock_text)
    resolved: dict[str, str] = {}
    for package in lock.get("package", []):
        source = package.get("source") or {}
        if isinstance(source, dict) and source.get("editable"):
            continue
        name = package.get("name")
        version = package.get("version")
        if isinstance(name, str) and isinstance(version, str):
            resolved[_normalize_pypi(name)] = version
    return resolved


def pypi_items() -> list[Item]:
    text = PYPROJECT.read_text(encoding="utf-8")
    project = tomllib.loads(text).get("project", {})
    comments = _requirement_comments(text)
    lock_text = UV_LOCK.read_text(encoding="utf-8") if UV_LOCK.is_file() else None
    resolved = _uv_resolved(lock_text)
    items: list[Item] = []

    def add(requirement: str, label: str) -> None:
        name, spec = _split_requirement(requirement)
        if not name:
            return
        key = _normalize_pypi(name)
        notes = [label]
        if key in comments:
            notes.append(comments[key])
        items.append(
            Item(
                ecosystem="pypi",
                library=name,
                declared=spec or "(unspecified)",
                resolved=resolved.get(key, ""),
                notes="; ".join(notes),
                probe=("pypi", key),
            )
        )

    for requirement in project.get("dependencies", []):
        if isinstance(requirement, str):
            add(requirement, "dependency")
    extras = project.get("optional-dependencies") or {}
    for extra, requirements in extras.items():
        for requirement in requirements:
            if isinstance(requirement, str):
                add(requirement, f"{extra} extra")
    return items


def _http_get(url: str) -> tuple[int, bytes]:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "application/json, application/xml, text/xml, */*",
        },
    )
    context = ssl.create_default_context()
    try:
        with urllib.request.urlopen(request, timeout=25, context=context) as response:
            body = response.read(MAX_BODY + 1)
            if len(body) > MAX_BODY:
                raise urllib.error.URLError(f"response exceeds {MAX_BODY} bytes")
            return response.status, body
    except urllib.error.HTTPError as exc:
        body = exc.read(200_000) if exc.fp is not None else b""
        return exc.code, body


def _http_get_retry(url: str) -> tuple[int, bytes]:
    status, body = _http_get(url)
    if status in {429, 500, 502, 503, 504}:
        status, body = _http_get(url)
    return status, body


def _best_stable(versions: list[str]) -> str | None:
    best: tuple[tuple[int, ...], str] | None = None
    for version in versions:
        key = stable_key(version)
        if key is None:
            continue
        if best is None or cmp_versions(key, best[0]) > 0:
            best = (key, version)
    return None if best is None else best[1]


def _maven_versions(body: bytes) -> list[str]:
    try:
        root = ET.fromstring(body)
    except ET.ParseError:
        return []

    def local(tag: str) -> str:
        return tag.split("}", 1)[-1]

    found: list[str] = []
    for element in root.iter():
        if local(element.tag) != "versions":
            continue
        for child in list(element):
            if local(child.tag) == "version" and child.text:
                found.append(child.text.strip())
    return found


def _latest_maven(group: str, artifact: str, repos: tuple[str, ...]) -> tuple[str | None, str]:
    path = f"{group.replace('.', '/')}/{artifact}/maven-metadata.xml"
    errors: list[str] = []
    for repo in repos:
        url = f"{repo}/{path}"
        try:
            status, body = _http_get_retry(url)
        except urllib.error.URLError as exc:
            errors.append(f"{repo} ({exc.reason})")
            continue
        if status == 200:
            latest = _best_stable(_maven_versions(body))
            if latest:
                return latest, repo
            errors.append(f"{repo} (no stable version)")
        elif status != 404:
            errors.append(f"{repo} (HTTP {status})")
    if errors:
        return None, f"{group}:{artifact}: " + "; ".join(errors)
    return None, f"{group}:{artifact}: no stable release in configured Maven repos"


def _plugin_fallbacks(plugin_id: str) -> list[tuple[str, str]]:
    if plugin_id.startswith("org.jetbrains.kotlin"):
        return [("org.jetbrains.kotlin", "kotlin-gradle-plugin")]
    if plugin_id.startswith("org.jetbrains.compose"):
        return [("org.jetbrains.compose", "compose-gradle-plugin")]
    if plugin_id.startswith("com.android."):
        return [("com.android.tools.build", "gradle")]
    if plugin_id.startswith("com.gradleup.shadow"):
        return [("com.gradleup.shadow", "shadow-gradle-plugin")]
    return []


def _latest_plugin(plugin_id: str) -> tuple[str | None, str]:
    """Plugin Portal marker first, then the JAR that actually carries the version."""
    latest, detail = _latest_maven(plugin_id, f"{plugin_id}.gradle.plugin", PLUGIN_REPOS)
    if latest:
        return latest, detail
    errors = [detail]
    for group, artifact in _plugin_fallbacks(plugin_id):
        latest, detail = _latest_maven(group, artifact, LIBRARY_REPOS)
        if latest:
            return latest, detail
        errors.append(detail)
    return None, errors[-1]


def _latest_npm(name: str) -> tuple[str | None, str]:
    encoded = name.replace("/", "%2f")
    url = f"https://registry.npmjs.org/{encoded}/latest"
    try:
        status, body = _http_get_retry(url)
    except urllib.error.URLError as exc:
        return None, f"{url} ({exc.reason})"
    if status != 200:
        return None, f"{url} (HTTP {status})"
    try:
        document = json.loads(body)
    except json.JSONDecodeError:
        return None, f"{url} (not JSON)"
    version = document.get("version")
    if not isinstance(version, str):
        return None, f"{url} (no version)"
    if stable_key(version) is None:
        return None, f"{url} (latest tag {version} is not a stable release)"
    note = "npm latest"
    if isinstance(document.get("deprecated"), str) and document["deprecated"].strip():
        note = "npm latest; deprecated: " + document["deprecated"].strip()
    return version, note


def _latest_pypi(name: str) -> tuple[str | None, str]:
    url = f"https://pypi.org/pypi/{name}/json"
    try:
        status, body = _http_get_retry(url)
    except urllib.error.URLError as exc:
        return None, f"{url} ({exc.reason})"
    if status != 200:
        return None, f"{url} (HTTP {status})"
    try:
        document = json.loads(body)
    except json.JSONDecodeError:
        return None, f"{url} (not JSON)"
    info = document.get("info") or {}
    version = info.get("version") if isinstance(info, dict) else None
    if isinstance(version, str) and stable_key(version) is not None:
        return version, "PyPI"
    releases = document.get("releases")
    if isinstance(releases, dict):
        latest = _best_stable(list(releases))
        if latest:
            return latest, "PyPI"
    shown = version if isinstance(version, str) else "none"
    return None, f"{url} (no stable release; info.version={shown})"


def _baseline(declared: str, resolved: str) -> str | None:
    if resolved and stable_key(resolved) is not None:
        return resolved
    match = re.search(r"\d+(?:\.\d+)+", declared)
    if match and stable_key(match.group(0)) is not None:
        return match.group(0)
    return None


def _update_via(item: Item, in_range: bool | None) -> str:
    if item.ecosystem == "gradle":
        return "gradle/libs.versions.toml"
    if item.ecosystem == "npm":
        if in_range is True:
            return "bun update (in range)"
        if in_range is False:
            return "package.json (out of range)"
        return "package.json"
    if in_range is False:
        return "pyproject.toml (above the floor)"
    return "uv lock (floor still allows it)"


def _join_notes(*parts: str) -> str:
    return "; ".join(part.strip() for part in parts if part and part.strip())


def check_item(item: Item) -> Row:
    if not item.probe:
        return Row(
            item.ecosystem,
            item.library,
            item.declared,
            item.resolved or "—",
            "—",
            "—",
            "—",
            _join_notes(item.notes, "nothing to probe"),
            "unchecked",
        )
    kind = item.probe[0]
    try:
        if kind == "maven":
            latest, detail = _latest_maven(item.probe[1], item.probe[2], LIBRARY_REPOS)
        elif kind == "plugin":
            latest, detail = _latest_plugin(item.probe[1])
        elif kind == "npm":
            latest, detail = _latest_npm(item.probe[1])
        elif kind == "pypi":
            latest, detail = _latest_pypi(item.probe[1])
        else:
            latest, detail = None, f"unknown probe {kind}"
    except urllib.error.URLError as exc:
        latest, detail = None, str(exc.reason)

    if not latest:
        return Row(
            item.ecosystem,
            item.library,
            item.declared,
            item.resolved or "—",
            "—",
            "—",
            "—",
            _join_notes(item.notes, detail),
            "unchecked",
        )

    base = _baseline(item.declared, item.resolved)
    base_key = stable_key(base) if base else None
    latest_key = stable_key(latest)
    if base_key is None or latest_key is None:
        return Row(
            item.ecosystem,
            item.library,
            item.declared,
            item.resolved or "—",
            latest,
            "—",
            "—",
            _join_notes(item.notes, "could not compare versions", detail),
            "unchecked",
        )
    in_range = satisfies(item.declared, latest) if item.ecosystem != "gradle" else False
    order = cmp_versions(latest_key, base_key)
    if order <= 0:
        return Row(
            item.ecosystem,
            item.library,
            item.declared,
            item.resolved or base or "—",
            latest,
            "current",
            "—",
            _join_notes(item.notes, detail if item.ecosystem == "npm" and "deprecated" in detail else ""),
            "current",
        )
    return Row(
        item.ecosystem,
        item.library,
        item.declared,
        item.resolved or base or "—",
        latest,
        bump_kind(base_key, latest_key),
        _update_via(item, in_range),
        _join_notes(item.notes, detail if item.ecosystem == "npm" and "deprecated" in detail else ""),
        "update",
    )


def collect(ecosystem: str | None) -> list[Row]:
    missing = [path for path in (CATALOG, PACKAGE_JSON, PYPROJECT) if not path.is_file()]
    if missing:
        joined = ", ".join(str(path.relative_to(ROOT)) for path in missing)
        raise SystemExit(f"missing manifest: {joined}")
    items = gradle_items() + npm_items() + pypi_items()
    if ecosystem:
        items = [item for item in items if item.ecosystem == ecosystem]
    if not items:
        return []
    rows: list[Row] = []
    with ThreadPoolExecutor(max_workers=8) as pool:
        futures = [pool.submit(check_item, item) for item in items]
        for future in as_completed(futures):
            rows.append(future.result())
    return rows


def _cell(value: str) -> str:
    return value.replace("|", "/").replace("\n", " ").strip() or "—"


def _selected_stacks(ecosystem: str | None) -> tuple[tuple[str, str], ...]:
    if ecosystem is None:
        return STACKS
    return tuple(pair for pair in STACKS if pair[0] == ecosystem)


def _stack_source(ecosystem: str) -> str:
    if ecosystem == "gradle":
        return f"`{CATALOG.relative_to(ROOT)}`"
    if ecosystem == "npm":
        return f"`{PACKAGE_JSON.relative_to(ROOT)}` (resolved from bun.lock)"
    if ecosystem == "pypi":
        return f"`{PYPROJECT.relative_to(ROOT)}` (resolved from uv.lock)"
    return ecosystem


def _by_severity(rows: list[Row]) -> list[Row]:
    return sorted(rows, key=lambda row: (BUMP_ORDER.get(row.bump, 9), row.library.lower()))


def render_markdown(rows: list[Row], include_current: bool, ecosystem: str | None) -> str:
    updates = [row for row in rows if row.status == "update"]
    current = [row for row in rows if row.status == "current"]
    unchecked = [row for row in rows if row.status == "unchecked"]
    stacks = _selected_stacks(ecosystem)

    lines = [
        "# Library version audit",
        "",
        f"Checked {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC')}. "
        "Direct dependencies only, grouped by tech stack. "
        "Latest is the newest stable release; pre-releases are ignored. "
        "Manifests were not modified.",
        "",
        (
            f"Updates: {len(updates)} "
            f"({sum(row.bump == 'major' for row in updates)} major, "
            f"{sum(row.bump == 'minor' for row in updates)} minor, "
            f"{sum(row.bump == 'patch' for row in updates)} patch). "
            f"Up to date: {len(current)}. Unchecked: {len(unchecked)}."
        ),
        "",
        "| Stack | Checked | Updates | Major | Minor | Patch | Up to date | Unchecked |",
        "|---|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for key, label in stacks:
        stack_rows = [row for row in rows if row.ecosystem == key]
        stack_updates = [row for row in stack_rows if row.status == "update"]
        lines.append(
            "| "
            + " | ".join(
                (
                    label,
                    str(len(stack_rows)),
                    str(len(stack_updates)),
                    str(sum(row.bump == "major" for row in stack_updates)),
                    str(sum(row.bump == "minor" for row in stack_updates)),
                    str(sum(row.bump == "patch" for row in stack_updates)),
                    str(sum(row.status == "current" for row in stack_rows)),
                    str(sum(row.status == "unchecked" for row in stack_rows)),
                )
            )
            + " |"
        )
    lines.append("")

    for key, label in stacks:
        stack_rows = [row for row in rows if row.ecosystem == key]
        stack_updates = _by_severity(row for row in stack_rows if row.status == "update")
        stack_unchecked = sorted(
            (row for row in stack_rows if row.status == "unchecked"),
            key=lambda row: row.library.lower(),
        )
        stack_current = sorted(
            (row for row in stack_rows if row.status == "current"),
            key=lambda row: row.library.lower(),
        )
        lines.append(f"## {label}")
        lines.append("")
        lines.append(f"{_stack_source(key)} — {len(stack_rows)} checked.")
        lines.append("")
        if stack_updates:
            lines.extend(_table(stack_updates))
        else:
            lines.append("No newer stable releases.")
            lines.append("")
        if stack_unchecked:
            lines.append("### Unchecked")
            lines.append("")
            lines.extend(_table(stack_unchecked))
        if include_current and stack_current:
            lines.append("### Up to date")
            lines.append("")
            lines.extend(_table(stack_current))

    if current and not include_current:
        lines.append("Up-to-date rows are hidden. Pass `--include-current` to list them.")
        lines.append("")
    lines.append(
        "This command does not change files. A Java row is one Gradle catalog pin, "
        "including Kotlin. "
        "A Frontend in-range row can be refreshed with `bun update` in the admin web project; "
        "an out-of-range row needs `package.json` edited. "
        "A Python row whose floor still allows the latest release is a lockfile refresh (`uv lock`)."
    )
    lines.append("")
    return "\n".join(lines)


def _table(rows: list[Row]) -> list[str]:
    header = (
        "| Library | Declared | Resolved | Latest | Bump | Update via | Notes |",
        "|---|---|---|---|---|---|---|",
    )
    body = [
        "| "
        + " | ".join(
            _cell(value)
            for value in (
                row.library,
                row.declared,
                row.resolved,
                row.latest,
                row.bump,
                row.update_via,
                row.notes,
            )
        )
        + " |"
        for row in rows
    ]
    return [*header, *body, ""]


def render_json(rows: list[Row]) -> str:
    order = {key: index for index, (key, _label) in enumerate(STACKS)}
    payload = [
        {
            "stack": stack_label(row.ecosystem),
            "ecosystem": row.ecosystem,
            "library": row.library,
            "declared": row.declared,
            "resolved": row.resolved,
            "latest": row.latest,
            "bump": row.bump,
            "update_via": row.update_via,
            "notes": row.notes,
            "status": row.status,
        }
        for row in sorted(rows, key=lambda row: (order.get(row.ecosystem, 99), row.library.lower()))
    ]
    return json.dumps(payload, indent=2) + "\n"


def self_test() -> int:
    failures: list[str] = []

    def check(condition: bool, message: str) -> None:
        if not condition:
            failures.append(message)

    check(stable_key("1.2.3") == (1, 2, 3), "stable 1.2.3")
    check(stable_key("v2.0.0") == (2, 0, 0), "v prefix")
    check(stable_key("1.2.3-RC1") is None, "rc")
    check(stable_key("2.4.0-Beta1") is None, "beta")
    check(stable_key("1.0.0-alpha.2") is None, "alpha")
    check(stable_key("9.0.0-SNAPSHOT") is None, "snapshot")
    check(stable_key("1.2.3-M1") is None, "milestone")
    check(cmp_versions((1, 7, 3), (1, 7, 10)) < 0, "numeric minor ordering")
    check(cmp_versions((2, 0), (2, 0, 0)) == 0, "trailing zeros")
    check(bump_kind((1, 2, 3), (2, 0, 0)) == "major", "major bump")
    check(bump_kind((1, 2, 3), (1, 3, 0)) == "minor", "minor bump")
    check(bump_kind((1, 2, 3), (1, 2, 4)) == "patch", "patch bump")
    check(bump_kind((2, 0), (2, 0, 1)) == "patch", "patch on short version")
    check(satisfies("^19.3.1", "19.4.0") is True, "caret includes minor")
    check(satisfies("^19.3.1", "20.0.0") is False, "caret excludes next major")
    check(satisfies("^0.5.7", "0.5.9") is True, "caret 0.x includes patch")
    check(satisfies("^0.5.7", "0.6.0") is False, "caret 0.x excludes next minor")
    check(satisfies("^0.0.3", "0.0.3") is True, "caret 0.0 includes itself")
    check(satisfies("^0.0.3", "0.0.4") is False, "caret 0.0 excludes next patch")
    check(satisfies("~6.0.3", "6.0.9") is True, "tilde includes patch")
    check(satisfies("~6.0.3", "6.1.0") is False, "tilde excludes minor")
    check(satisfies("7.0.1", "7.0.1") is True, "exact pin matches")
    check(satisfies("7.0.1", "7.0.2") is False, "exact pin rejects newer")
    check(satisfies(">=1.24.0", "2.0.0") is True, "floor allows major")
    check(satisfies(">=1.24.0", "1.23.0") is False, "floor rejects older")

    for path in (CATALOG, PACKAGE_JSON, PYPROJECT):
        check(path.is_file(), f"missing {path.relative_to(ROOT)}")
    if CATALOG.is_file():
        gradle = gradle_items()
        by_name = {item.library: item for item in gradle}
        check("kotlin" in by_name, "catalog key kotlin")
        check("ktor" in by_name, "catalog key ktor")
        check(by_name.get("kotlin") and by_name["kotlin"].probe[0] == "plugin", "kotlin probes a plugin")
        check(by_name.get("ktor") and by_name["ktor"].probe[0] == "maven", "ktor probes a module")
        check(by_name.get("ktor") and by_name["ktor"].probe[1] == "io.ktor", "ktor group")
        icons = by_name.get("composeMaterialIcons")
        check(icons is not None and "frozen" in icons.notes, "icons comment is kept")
    if PACKAGE_JSON.is_file():
        npm = {item.library: item for item in npm_items()}
        check("react" in npm and npm["react"].declared.startswith("^"), "react range")
        check("eslint-plugin-react-hooks" in npm, "exact frontend pin present")
        if BUN_LOCK.is_file():
            check(bool(npm["react"].resolved), "react resolved from bun.lock")
    if PYPROJECT.is_file():
        pypi = {_normalize_pypi(item.library): item for item in pypi_items()}
        check("pydantic" in pypi, "pydantic dependency")
        check("pymupdf" in pypi, "pymupdf dependency")
        check(pypi["pymupdf"].declared.startswith(">="), "pymupdf floor")
        if UV_LOCK.is_file():
            check(bool(pypi["pydantic"].resolved), "pydantic resolved from uv.lock")

    check(stack_label("gradle") == "Java (including Kotlin)", "java stack label")
    check(stack_label("npm") == "Frontend", "frontend stack label")
    check(stack_label("pypi") == "Python", "python stack label")
    sample = [
        Row("pypi", "ruff", ">=0.4.0", "0.17.2", "0.17.3", "patch", "uv lock", "", "update"),
        Row("gradle", "kotlin", "2.4.21", "2.4.21", "2.4.22", "patch", "gradle/libs.versions.toml", "", "update"),
        Row("npm", "react", "^19.3.1", "19.3.1", "19.4.0", "minor", "bun update (in range)", "", "update"),
        Row("npm", "typescript", "~6.0.3", "6.0.3", "7.0.2", "major", "package.json (out of range)", "", "update"),
    ]
    rendered = render_markdown(sample, include_current=False, ecosystem=None)
    java_at = rendered.find("## Java (including Kotlin)")
    frontend_at = rendered.find("## Frontend")
    python_at = rendered.find("## Python")
    check(0 <= java_at < frontend_at < python_at, "stack section order")
    frontend_body = rendered[frontend_at:python_at]
    check("kotlin" in rendered[java_at:frontend_at], "kotlin is in the Java section")
    check("kotlin" not in frontend_body, "kotlin is not in Frontend")
    check(
        "typescript" in frontend_body
        and "react" in frontend_body
        and frontend_body.find("typescript") < frontend_body.find("react"),
        "frontend majors precede minors",
    )
    check("ruff" in rendered[python_at:], "python package stays in Python")
    java_only = render_markdown(sample, include_current=False, ecosystem="gradle")
    check("## Frontend" not in java_only and "## Python" not in java_only, "ecosystem filter keeps one stack")

    if failures:
        print("self-test failed:", file=sys.stderr)
        for message in failures:
            print(f"  {message}", file=sys.stderr)
        return 1
    print(f"self-test ok ({len(gradle_items())} gradle, {len(npm_items())} npm, {len(pypi_items())} pypi)")
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--include-current", action="store_true", help="list libraries that are already current")
    parser.add_argument("--json", action="store_true", help="print JSON instead of Markdown")
    parser.add_argument("--ecosystem", choices=("gradle", "npm", "pypi"), help="check one ecosystem")
    parser.add_argument("--self-test", action="store_true", help="check parsers and version rules, no network")
    args = parser.parse_args(argv)
    if args.self_test:
        return self_test()

    print("Checking upstream releases…", file=sys.stderr)
    rows = collect(args.ecosystem)
    if args.json:
        sys.stdout.write(render_json(rows))
    else:
        sys.stdout.write(render_markdown(rows, args.include_current, args.ecosystem))
    if rows and all(row.status == "unchecked" for row in rows):
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
