---
name: start-issue
description: Start work on a GitHub issue - fetch it, create a feature branch from up-to-date main, plan, implement with tests, and commit. Use when the user says "start issue N", "work on #N", or runs /start-issue.
argument-hint: "<issue-number>"
---

# Start Issue

Issue: #$ARGUMENTS

## 1. Load the issue
- If no issue number was given, run `gh issue list --assignee @me --state open` (fall back to `gh issue list --state open`) and ask which one.
- `gh issue view <N> --json number,title,body,labels,state,comments`
- If the issue is closed, stop and tell the user.
- Extract the **acceptance criteria** — they are the definition of done.

## 2. Prepare the branch
- `git status --porcelain` must be empty. If not, stop and ask the user (do not stash or discard their work).
- `git checkout main && git pull --ff-only origin main`
- Branch name: `<type>/<N>-<short-kebab-slug>` where type is `feat` (enhancement), `fix` (bug), `docs`, or `chore`. Example: `feat/12-add-orders-endpoint`.
- If the branch already exists, check it out and continue instead of recreating it.
- `git checkout -b <branch>`
- Mark it in progress: `gh issue edit <N> --add-assignee @me` and post a short comment: `gh issue comment <N> --body "Started work on branch \`<branch>\`."`

## 3. Plan
- Explore the relevant code (read existing patterns: project layout, routing, models, test style).
- Write a short plan to the user: files to add/change, approach, tests to write, and how each acceptance criterion will be met. Keep it to a few bullets; proceed unless something is genuinely ambiguous.

## 4. Implement (test-first)
- Detect the stack and its commands (e.g. `package.json` scripts, `pyproject.toml`/`pytest`, `pom.xml`/`build.gradle`, `go.mod`, `Makefile`). Prefer what the project already uses.
- For each criterion: write a failing test → implement the minimal code → make it pass.
- Follow existing conventions (naming, error handling, validation, response shapes). Cover error paths (validation errors, not found, auth) for API work.
- Do not touch unrelated code.

## 5. Verify
- Run the full test suite, plus linter/formatter/type-checker if the project has them. Fix failures before continuing. Report the actual output — never claim tests pass without running them.

## 6. Commit
- Use Conventional Commits, referencing the issue:
  `feat(orders): add POST /orders endpoint (#<N>)`
- Small logical commits are fine; never commit secrets, `.env`, or build artifacts.

## 7. Hand off
Summarise what changed, how each acceptance criterion is satisfied, and test results. Suggest `/raise-pr` as the next step.
