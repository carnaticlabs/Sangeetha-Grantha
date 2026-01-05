# Documentation Standards

> **Status**: Current | **Version**: 1.0 | **Last Updated**: 2025-01-27
> **Owners**: Platform Team

## General Principles

Documentation in Sangita Grantha follows a **spec-driven approach** where documentation is the source of truth for implementation. All documentation should:

- Be accurate and reflect current implementation
- Be organized in a logical, discoverable structure
- Include status, version, and last updated metadata
- Reference related documents for cross-linking
- Use clear, concise language appropriate for the audience

## File Structure Standards

### Front Matter

All documentation files should include front matter with:

- **Status**: `Current`, `Draft`, `Deprecated`, or `Archived`
- **Version**: Semantic version (e.g., `1.0`, `0.2`)
- **Last Updated**: ISO date format (YYYY-MM-DD)
- **Owners**: Team or role responsible for maintaining the document

Example:
```markdown
> **Status**: Current | **Version**: 1.0 | **Last Updated**: 2025-01-27
> **Owners**: Backend Team
```

### Directory Organization

Documentation is organized in `application_documentation/` with the following structure:

```
application_documentation/
├── 00-meta/              # Meta-documentation (standards, retention plans)
├── 01-requirements/      # PRDs, domain models, feature specs
├── 02-architecture/      # System design, tech stack, ADRs
├── 03-api/              # API contracts, integration specs
├── 04-database/         # Schema, migrations, audit logs
├── 05-frontend/         # UI specs for admin web and mobile
├── 06-backend/          # Backend-specific docs (mutation handlers, security)
├── 07-quality/          # Test plans, coverage reports
├── 08-operations/       # Runbooks, configuration guides
└── 09-ai/               # AI integration docs, knowledge base
```

## Content Standards

### Writing Style

- Use clear, professional language
- Prefer active voice
- Use consistent terminology (see glossary)
- Include code examples where helpful
- Link to related documents for context

### Code Examples

- Code examples should be executable or reflect actual implementation
- Include file paths and line numbers when referencing existing code
- Use appropriate syntax highlighting
- Keep examples focused and minimal

### Tables and Lists

- Use tables for structured data comparisons
- Use lists for sequential steps or unordered items
- Keep tables readable (avoid overly wide columns)

## Technical Documentation Standards

### API Documentation

- Document all endpoints with:
  - HTTP method and path
  - Request/response schemas
  - Authentication requirements
  - Example requests/responses
- Reference OpenAPI spec in `openapi/` directory
- Include error response formats

### Database Documentation

- Document schema changes in migration files
- Include ERDs for complex relationships
- Document constraints, indexes, and triggers
- Reference migration tool (Rust CLI, not Flyway)

### Architecture Documentation

- Include system diagrams where helpful
- Document design decisions in ADRs
- Reference tech stack versions from `gradle/libs.versions.toml`
- Document patterns and conventions

## Review Process

### Documentation Review Checklist

Before marking documentation as "Current":

- [ ] Status and version metadata present
- [ ] Content matches current implementation
- [ ] Links to related documents are valid
- [ ] Code examples are accurate
- [ ] Terminology matches glossary
- [ ] No TODO placeholders remain

### Review Responsibilities

- **Authors**: Ensure accuracy and completeness
- **Team Leads**: Review for technical correctness
- **Platform Team**: Review for standards compliance

## Maintenance Guidelines

### Regular Updates

- Update documentation when implementation changes
- Review quarterly for accuracy
- Archive obsolete documents to `archive/` directory
- Update "Last Updated" date when making changes

### Change Management

- Major changes should increment version number
- Breaking changes should update status to "Draft" until reviewed
- Deprecated features should be marked as "Deprecated"
- Use git commits to track documentation history

## Tools and Automation

### Recommended Tools

- **Markdown**: Standard format for all documentation
- **Mermaid**: For diagrams (when supported)
- **OpenAPI**: For API specifications
- **Git**: Version control and history

### Automation Goals

- Validate markdown syntax (future)
- Check for broken links (future)
- Generate API docs from OpenAPI spec (future)

## Quality Metrics

### Success Criteria

- All active features have corresponding documentation
- Documentation is discoverable and well-organized
- Code examples are tested and accurate
- Cross-references are maintained

### Monitoring

- Track documentation coverage (features vs. docs)
- Monitor broken links
- Review update frequency
- Gather feedback from team

## References

- [Sangita Grantha Documentation Index](../README.md)
- [Product Requirements Document](../01-requirements/product-requirements-document.md)
- [Tech Stack](../02-architecture/tech-stack.md)
- [Backend Architecture](../02-architecture/backend-system-design.md)
