| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-07-18 |
| **Author** | Sangeetha Grantha Team |

# Workflow

---

Development and documentation workflows are defined in:
[Documentation Standards](../application_documentation/00-meta/standards.md)

---

## Registry & Track File Sync Rules

**One source of truth:** The `tracks.md` registry status is authoritative. The individual track file status must match. Both must update in the same commit.

### Rules

1. When changing a track's status, update **both** `tracks.md` (registry row) and the track file header in a single commit.
2. Never leave a track file with a stale status — if in doubt, run `python3 conductor/check-registry-sync.py` to detect drift.
3. New tracks require both a file under `conductor/tracks/` and a row in `tracks.md` before the commit lands.
4. Deprecated tracks move to the "Deprecated Tracks" section but retain their file.

### Verification

```bash
python3 conductor/check-registry-sync.py
```

Returns exit 0 if all registry rows match their file statuses and no orphans exist.
