| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Documentation checks

---

The active documentation command is:

```bash
make check-docs
```

It invokes [tools/check-doc-links.py](../../tools/check-doc-links.py). The earlier Rust CLI docs command is archived and is not part of the current workflow.

## What the gate checks

The checker scans Markdown links beginning with `./` or `../` and resolves targets against Git-tracked paths. Directory links are supported. Links inside code examples are ignored. A newly created guide must be included in the change set for tracked-target validation.

The gate deliberately does not validate external URLs, bare relative links without a dot prefix, fragment anchors, or rendered diagrams. Review those separately when editing navigation or headings. A passing file-link gate does not prove the technical accuracy of a document.

Use [writing standards](../00-meta/standards.md), [the document catalog](../00-meta/document-catalog.md), and source-linked current guides to keep the library consistent.

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
