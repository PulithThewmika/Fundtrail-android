# Contributing

## Branches

- Work branches off `Dev`, never off `main`.
- Name branches `feature/<issue-no>-short-name`, e.g. `feature/32-repo-conventions`.
- Use `fix/<issue-no>-short-name` for bug fixes.
- One branch per issue. Open the PR against `Dev`.

## Commits

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary>
```

Common types: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `build`, `ci`.
Scope is optional and usually the area touched (e.g. `auth`, `readme`, `ci`).

Examples:
- `docs(readme): add overview and firebase setup placeholders`
- `feat(auth): add email/password sign-in`
- `fix(goal): correct required-saving recalculation`

Keep the summary under ~72 characters, imperative mood, no trailing period. Reference the issue in the PR, not in every commit.

## Definition of Done

A task is done when, before requesting review:

- [ ] All acceptance criteria on the issue are checked off
- [ ] The branch is up to date with `Dev` and builds/passes tests locally
- [ ] No TODOs left unmarked — use `TODO(M<n>)` for anything deferred to a later milestone
- [ ] The PR links the issue(s) it closes (`Closes #<issue-no>`)
- [ ] CodeRabbit review comments are addressed or replied to
- [ ] At least one approval before merging to `Dev`

This checklist is also what the [PR template](.github/pull_request_template.md) asks reviewers to confirm.

## Pull requests

- Keep PRs scoped to one issue (or its sub-issues) where possible.
- Fill in the PR template — summary, what's included, acceptance criteria, related issues.
- Squash or keep history as the reviewer prefers; either way, commit messages should still follow the convention above.

## Reporting and fixing bugs

When a test fails, report it — don't fix it silently. The full workflow lives on
[EPIC-019](https://github.com/PulithThewmika/Fundtrail-android/issues/262); this is the short version.

**Reporting**

1. Search open issues labelled `bug` first. If it already exists, add your device and steps as a
   comment instead of opening a duplicate.
2. Open a new issue with the **Bug** template. Title: `[BUG] <screen>: <what goes wrong>`.
3. Label it `bug` + `sub-task` + one `area:*` + a priority:
   - `P0` — crash, data loss, wrong money figure, security issue, or it blocks the demo/APK
   - `P1` — feature broken but a workaround exists, or a wrong non-money UI state
   - `P2` — cosmetic or minor (spacing, copy, alignment)
4. Set milestone `M4 – Harden & Ship`, set the project **Epic** field to `EPIC-019`, and link it as
   a sub-issue of [T4: Bug bash + fix window](https://github.com/PulithThewmika/Fundtrail-android/issues/272).
5. No steps or no evidence means the bug gets `needs-spec` and comes back to you.
6. Cross-reference: comment `Found #<bug>` on the test task, and put the bug number in the result
   row in [`Docs/testing/regression.md`](Docs/testing/regression.md).

**Fixing**

- Branch `fix/<issue-no>-short-name` off `Dev`.
- PR title starts with `fix:`; the body says `Closes #<bug>`.
- Add a unit or UI test that reproduces the bug wherever that is feasible.

**Closing**

- The **reporter** closes the bug, not the fixer. Re-test on the same device and API level, then
  close with a comment: `Verified on <device>, API <n>, build <commit>`. If it still fails, reopen.

**After code freeze**, only `P0` bugs are fixed. Everything else is labelled `known-issue` and
listed in the design PDF.
