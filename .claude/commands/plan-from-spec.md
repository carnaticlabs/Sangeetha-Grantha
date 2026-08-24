Write an implementation plan from an accepted track Spec. Do not implement code.

Track: $ARGUMENTS

1. Find `conductor/tracks/TRACK-*-*.md` for the given id (or the in-progress track if omitted).
2. Stop unless **Spec Status is Accepted**. If Spec is missing or Draft, run `/spec-from-track` first and wait for acceptance.
3. Read the codebase (plan mode / read-only). Name files that will change, the order of work, risks, and the tests that prove it. Interrogate: what could break, which step is riskiest, which options you rejected.
4. Proof must be commands from CLAUDE.md **Verifying your work** (for example `make test`, `make test-frontend`, `verify-import`, `make agent-evals`) — not a vague “add tests.”
5. Write or replace the track **Plan** section (files, order, risks, proof). Set **Plan Status to Draft**. If later implementation diverges, update Plan in the same change.
6. Ask the user to accept the Plan. Do not edit product code until they set Plan Status to Accepted. After acceptance, implement in one pass against that Plan and check the diff against it in review.
