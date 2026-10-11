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
