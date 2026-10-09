---
name: raise-pr
description: Verify, push the current feature branch and open a GitHub pull request linked to its issue. Use when the user says "raise a PR", "open a pull request", or runs /raise-pr.
argument-hint: "[optional extra notes for the PR description]"
---

# Raise PR

Extra notes from user: $ARGUMENTS

## 1. Pre-flight checks (stop and report if any fail)
- Current branch: `git branch --show-current`. If it is `main`, stop — PRs must come from a feature branch (suggest `/start-issue`).
- Uncommitted changes (`git status --porcelain`): show them and ask whether to commit them (Conventional Commit message) before continuing.
- Commits ahead of main: `git fetch origin main && git log --oneline origin/main..HEAD`. If none, stop — nothing to raise.
- If a PR already exists for this branch (`gh pr view --json url,state`), just push the new commits and report the existing PR URL.

## 2. Verify
- Run the project's test suite and linter (detect from `package.json`, `pyproject.toml`, `Makefile`, etc.).
- If anything fails, stop and fix it (or report it) — do not open a PR with failing tests.
- If main has moved on, rebase or merge `origin/main` and re-run tests; resolve conflicts carefully.

## 3. Build the PR
- Issue number: parse from the branch name (`feat/12-...` → 12). Load it: `gh issue view <N> --json title,body`.
- Review the full change: `git diff origin/main...HEAD --stat` and `git diff origin/main...HEAD`.
- Title: Conventional Commit style, ≤ 70 chars, e.g. `feat(orders): add POST /orders endpoint`.
- Body (write to a temp file):

  ```markdown
  ## Summary
  <what changed and why, 2–4 bullets>

  Closes #<N>

  ## Changes
  - <file/module>: <change>

  ## Acceptance criteria
  - [x] <criterion from issue> — <how it is met>

  ## Testing
  - <commands run and results, e.g. `pytest` → 24 passed>
  - <manual checks, sample request/response if API>

  ## Notes for reviewers
  <trade-offs, follow-ups, anything out of scope>
  ```
  Include the user's extra notes if provided.

## 4. Push and open
```bash
git push -u origin HEAD
gh pr create --base main --title "<title>" --body-file <tmpfile> --assignee @me
```
Add the issue's label(s) with `--label` if they exist.

## 5. Report
Give the PR URL and number, and suggest `/pr-review <number>` as the next step.
