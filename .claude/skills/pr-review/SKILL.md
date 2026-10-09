---
name: pr-review
description: Review a GitHub pull request for correctness, security, tests, and fit with its linked issue, then post the review to GitHub. Use when the user says "review PR N", "review this PR", or runs /pr-review.
argument-hint: "[pr-number] [--no-post]"
---

# PR Review

Arguments: $ARGUMENTS

## 1. Identify the PR
- If a number is given use it; otherwise use the PR for the current branch (`gh pr view --json number`), or list open PRs (`gh pr list`) and ask.
- `--no-post` means: show the review in the terminal only, do not post to GitHub.

## 2. Gather context
- `gh pr view <N> --json number,title,body,author,headRefName,baseRefName,files,additions,deletions,commits`
- `gh pr diff <N>`
- `gh pr checks <N>` (note failing/pending CI; "no checks" is fine)
- Linked issue: find `Closes #X` in the body and load it with `gh issue view X --json title,body` — its acceptance criteria are the spec.
- Read full files around changed hunks when the diff alone lacks context. If useful, check out the branch (`gh pr checkout <N>`) and run the tests — then return to the original branch afterwards.

## 3. Review checklist
Only report real, specific problems — each with file, line, why it matters, and a concrete fix. No nitpicks dressed up as blockers.

- **Correctness**: logic errors, off-by-one, null/empty handling, wrong status codes, unhandled exceptions, race conditions, transaction boundaries.
- **Spec fit**: is every acceptance criterion met? Anything missing or out of scope?
- **Security**: input validation, injection (SQL/command), authN/authZ checks, secrets in code, sensitive data in logs/responses, mass assignment.
- **API / data**: consistent request/response shapes, pagination, idempotency, migrations reversible, indexes for new queries, N+1 queries.
- **Tests**: new behaviour and error paths covered; tests assert meaningful things; no flaky timing.
- **Maintainability**: naming, duplication, dead code, follows existing project patterns.

Classify each finding as **🔴 Blocker**, **🟡 Suggestion**, or **🟢 Nit**.

## 4. Verdict
- `REQUEST_CHANGES` if any blocker, otherwise `APPROVE` (or `COMMENT` if unsure).
- Note: GitHub does not let authors approve/request changes on their own PR. If `gh pr view <N> --json author` matches `gh api user --jq .login`, post as a comment (`--comment`) and state the intended verdict in the body.

## 5. Post the review (skip if `--no-post`)
Write the review body to a temp file:

```markdown
## Review summary
<1–3 sentences: overall assessment + verdict>

### Acceptance criteria
- [x]/[ ] <criterion> — <observation>

### Findings
- 🔴 `path/to/file.py:42` — <problem>. **Fix:** <suggestion>
- 🟡 ...
- 🟢 ...

### Tests
<what was run / coverage observations>
```

Then post with one of:
```bash
gh pr review <N> --approve          --body-file <tmpfile>
gh pr review <N> --request-changes  --body-file <tmpfile>
gh pr review <N> --comment          --body-file <tmpfile>
```

For blockers on specific lines, also add inline comments via the reviews API (write JSON to a temp file):
```bash
gh api repos/{owner}/{repo}/pulls/<N>/reviews --method POST --input <review.json>
# review.json: {"event":"COMMENT","body":"Inline notes","comments":[{"path":"src/x.py","line":42,"side":"RIGHT","body":"..."}]}
```
`line` must be a line present in the diff on the RIGHT side; otherwise mention it only in the summary.

## 6. Report
Print the verdict, the findings list, and the review URL. If acting on the review is requested, use `/start-issue`-style discipline: fix, test, commit, push to the same branch.
