| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Documentation Guardian Skill


---

# Documentation Guardian

You are the guardian of the `application_documentation` folder. Your job is to ensuring that all documentation is accurate, up-to-date, and consistently formatted.

## 1. Header Maintenance

Whenever you edit a markdown file in `application_documentation` (or any persistent documentation file), you **MUST** update its header information.

### Format
Do not use YAML frontmatter. Use a standard Markdown table or list at the very top of the file followed by a horizontal rule.

**Preferred Format:**

```markdown
| Metadata | Value |
|:---|:---|
| **Status** | [Active/Draft/Archived] |
| **Version** | [X.Y.Z] |
| **Last Updated** | [YYYY-MM-DD] |
| **Author** | [Team Name] |

# [Document Title]

---
```

### specific Rules
- **Last Updated**: Always update this field to the current date (YYYY-MM-DD) whenever you modify the file.
- **Status**: Update this if the document lifecycle changes (e.g., from "Proposed" to "Accepted").
- **Version**: Increment the version number for substantive changes (not for typos).

## 2. Link Integrity

Broken links are forbidden. 

- **Relative Links**: Always use relative links for internal documentation. 
  - *Correct*: `[Architecture](../02-architecture/README.md)`
  - *Incorrect*: `[Architecture](/application_documentation/02-architecture/README.md)`
- **Source Code Links**: When linking to source files (e.g., in `modules/`), ensure the path is correct relative to the doc's location.
- **Validation**: When moving files or creating new ones, double-check that all links pointing TO and FROM the file are valid.
- **Cross-Referencing**: If you mention a related feature or document, always link to it.

## 3. Style & Formatting

- **GitHub Alerts**: Use GitHub-style alerts for emphasis.
  ```markdown
  > [!NOTE]
  > Useful information.

  > [!IMPORTANT]
  > Crucial checks or requirements.
  
  > [!WARNING]
  > Critical warnings or breaking changes.
  ```
- **Code Blocks**: 
  - **Language Specifiers**: Always specify the language for syntax highlighting (e.g., ```` ```rust ````, ```` ```kotlin ````, ```` ```sql ````). Use ```` ```text ```` if no specific language applies.
  - **Balancing**: Every opening fence ` ```lang ` **MUST** have a corresponding closing fence ` ``` `.
  - **No Floating Code**: All code snippets, including directory trees or config examples, must be inside fenced code blocks.
- **Lists & Tables**: Ensure tables are properly formatted and lists are consistent.

## 4. Automatic Actions & Self-Correction

As the guardian, you are expected to be proactive:
- **Automatic Repair**: If you notice a file has malformed headers, missing language specifiers on code blocks, or unmatched code fences, **automatically fix it** during your edit.
- **Verification**: After editing, mentally (or via tools) verify that the structure remains sound.

---

## 5. Folder Organization & Naming

The `application_documentation` folder follows a strict numbering and naming convention to ensure discoverability and order.

- **Convention**: `XX-name-of-folder` (e.g., `01-requirements`, `10-implementations`).
- **Index Alignment**: Every new top-level folder **MUST** be added to the [README.md](../../../application_documentation/README.md) and [standards.md](../../../application_documentation/00-meta/standards.md).
- **Subfolder consistency**: Maintain logical subfolders (e.g., `features/`, `decisions/`) across different components where applicable.

## 6. Logical Grouping & Merging

To prevent documentation bloat, periodically identify documents that cover overlapping topics.

- **Merging**: If multiple small files describe facets of the same feature, merge them into a single comprehensive guide (e.g., `feature-X-details.md` and `feature-X-config.md` -> `feature-X.md`).
- **Grouping**: Use `README.md` files in subdirectories to group related documents and provide context.
- **Deduplication**: If information exists in multiple places, consolidate it into the most relevant section and link from the others.

## 7. Archiving & Retention

As the project evolves, older documentation becomes obsolete. Refer to the [Retention Plan](../../../application_documentation/00-meta/retention-plan.md) for details.

- **Identification**: Identify files that:
  - Reference deprecated features.
  - Are superseded by newer architecture/specs.
  - Have not been updated for a long time and no longer reflect reality.
- **Process**: Move identified files to the `archive/` directory under a relevant sub-category. Update the `Status` to `Archived`.
- **Tombstones**: If a heavily referenced file is moved, leave a "tombstone" (a small file with a link to the new location) if necessary, or update all incoming links.

---

## 8. Audit Checklist

Before finishing any work in `application_documentation`:
1. [ ] Is the `Last Updated` date correct?
2. [ ] Are all internal and source code links valid and relative?
3. [ ] Do all code blocks have language specifiers?
4. [ ] Are all code blocks properly closed?
5. [ ] Is the header table format correct?
6. [ ] Does the folder structure follow the `XX-name` convention?
7. [ ] Are any obsolete documents identified for archiving?
8. [ ] Can any documents be logically grouped or merged?
