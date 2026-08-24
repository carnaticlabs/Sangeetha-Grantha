Create a commit following this project's conventions (`.agents/skills/commit-policy/SKILL.md`, `.cursor/rules/git-conventions.mdc`).

Requirements:
1. Confirm the current branch matches the work (`track-<nnn>-<kebab-slug>` or `<type>/<kebab-slug>`). Do not use a `cursor/` prefix unless the user asked. Do not commit on `main` unless they asked.
2. Review all staged and unstaged changes
3. Draft a commit message that:
   - Uses `TRACK-ID: <short summary>` when a conductor track exists
   - Includes exactly one `Ref: application_documentation/...` line pointing to an existing spec
4. Stage relevant files (never stage .env, credentials, or secrets)
5. Create the commit

If unsure which documentation reference to use, check `application_documentation/` for the most relevant spec file.
