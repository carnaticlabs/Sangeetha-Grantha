#!/usr/bin/env python3

"""
Utility script for TRACK-002 (Documentation Header Standardization).

Features:
- Scans `application_documentation/` and `.agent/skills/` for `.md` files.
- Detects files missing the standard Metadata table header.
- --write mode: Replaces/Inserts standard header.
- Preserves existing YAML frontmatter fields or determines defaults.
"""

import argparse
import datetime
import pathlib
import re
from typing import List, Optional, Tuple, Dict

REPO_ROOT = pathlib.Path(__file__).resolve().parents[1]
SEARCH_PATHS = [
    REPO_ROOT / "application_documentation",
    REPO_ROOT / ".agent/skills",
]

HEADER_FIRST_LINE = "| Metadata | Value |"
HEADER_TEMPLATE = """| Metadata | Value |
|:---|:---|
| **Status** | {status} |
| **Version** | {version} |
| **Last Updated** | {date} |
| **Author** | {author} |

"""

# Regex helpers
YAML_FRONTMATTER_REGEX = re.compile(r"^---\s*\n(.*?)\n---\s*\n", re.DOTALL | re.MULTILINE)


def find_markdown_files() -> List[pathlib.Path]:
    files = []
    for root in SEARCH_PATHS:
        if root.exists():
            files.extend([p for p in root.rglob("*.md") if p.is_file()])
    return sorted(files)


def has_standard_header(content: str) -> bool:
    lines = content.lstrip().splitlines()
    if not lines:
        return False
    return lines[0].strip() == HEADER_FIRST_LINE


def parse_metadata(content: str) -> Tuple[Dict[str, str], str]:
    """
    Extracts metadata from various formats (YAML, existing Table) and returns
    (metadata_dict, cleaned_content).
    """
    metadata = {
        "status": "Active",
        "version": "1.0.0",
        "date": datetime.date.today().isoformat(),
        "author": "Sangeetha Grantha Team",
    }
    cleaned_content = content

    # 1. Try YAML Frontmatter (at start of file)
    yaml_match = YAML_FRONTMATTER_REGEX.match(content)
    if yaml_match:
        yaml_content = yaml_match.group(1)
        cleaned_content = content[yaml_match.end() :].lstrip()
        
        for line in yaml_content.splitlines():
            if ":" in line:
                key, val = line.split(":", 1)
                process_metadata_kv(metadata, key, val)
        return metadata, cleaned_content

    # 2. Search for existing Markdown Table (anywhere in first 50 lines)
    # We look for the specific header line
    lines = content.splitlines()
    table_start_idx = -1
    
    for i, line in enumerate(lines[:50]):
        if "| Metadata | Value |" in line:
            table_start_idx = i
            break
    
    if table_start_idx != -1:
        # Found a table. Let's try to extract from it and remove it.
        # We assume the table ends when we hit a non-pipe line or empty line
        table_end_idx = table_start_idx + 1
        while table_end_idx < len(lines):
            line = lines[table_end_idx].strip()
            if not line or not line.startswith("|"):
                break
            
            # Extract data
            if "|" in line and "---" not in line:
                parts = [p.strip() for p in line.split("|") if p.strip()]
                if len(parts) >= 2:
                    key = parts[0].replace("**", "")
                    val = parts[1]
                    process_metadata_kv(metadata, key, val)
            
            table_end_idx += 1
            
        # Remove the table lines
        # Also remove potential blank lines around it to clean up
        before = lines[:table_start_idx]
        after = lines[table_end_idx:]
        cleaned_content = "\n".join(before + after).strip() + "\n"

    return metadata, cleaned_content


def process_metadata_kv(metadata: Dict[str, str], key: str, val: str):
    key = key.strip().lower()
    val = val.strip().strip('"\'')
    if key == "status":
        metadata["status"] = val
    elif key == "version":
        metadata["version"] = val
    elif key == "date" or key == "last updated":
        pass # We always update date to today
    elif key == "author":
        metadata["author"] = val


def process_file(path: pathlib.Path, write: bool) -> bool:
    """Returns True if file was (or would be) modified."""
    try:
        content = path.read_text(encoding="utf-8", errors="ignore")
    except Exception as e:
        print(f"Skipping {path}: {e}")
        return False

    # Check if we ALREADY have a correct header at line 0
    # If so, we might still want to update the date, but let's be careful not to loop.
    # For now, if line 0 is header, we assume it's good, UNLESS we see a second header later.
    # But our parse_metadata logic will strip the "second" header if found.
    
    metadata, new_body = parse_metadata(content)
    
    # If the file started with the header, parse_metadata (logic 2) would have stripped it.
    # So 'new_body' is now header-less.
    # We reconstruct.
    
    metadata["date"] = datetime.date.today().isoformat()
    new_header = HEADER_TEMPLATE.format(**metadata)
    new_full_content = new_header + new_body.lstrip()

    if new_full_content != content:
        if write:
            path.write_text(new_full_content, encoding="utf-8")
            print(f"FIXED: {path.relative_to(REPO_ROOT)}")
        else:
            print(f"NEEDS FIX: {path.relative_to(REPO_ROOT)}")
        return True
    
    return False


def main() -> None:
    parser = argparse.ArgumentParser(description="Standardize documentation headers.")
    parser.add_argument("--write", action="store_true", help="Apply changes in-place.")
    args = parser.parse_args()

    candidates = find_markdown_files()
    modified_count = 0

    print(f"Scanning {len(candidates)} files...")
    
    for path in candidates:
        if process_file(path, args.write):
            modified_count += 1

    print()
    if args.write:
        print(f"Updated {modified_count} files.")
    else:
        print(f"Found {modified_count} files needing updates. Run with --write to fix.")


if __name__ == "__main__":
    main()
