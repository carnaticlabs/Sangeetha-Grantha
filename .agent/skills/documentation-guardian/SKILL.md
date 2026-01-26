| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
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

## 5. Audit Checklist

Before finishing any work in `application_documentation`:
1. [ ] Is the `Last Updated` date correct?
2. [ ] Are all internal and source code links valid and relative?
3. [ ] Do all code blocks have language specifiers?
4. [ ] Are all code blocks properly closed?
5. [ ] Is the header table format correct?
