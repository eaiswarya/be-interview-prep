# be-mock-prep

Backend mock-interview project. Repo: `eaiswarya/be-mock-prep` (GitHub, via `gh` CLI).

## Development workflow

Every change goes through the issue → branch → PR → review loop, driven by project skills:

| Step | Command | What it does |
|------|---------|--------------|
| 1 | `/create-issue <description>` | Creates a structured GitHub issue with acceptance criteria |
| 2 | `/start-issue <N>` | Branches from fresh `main`, plans, implements test-first, commits |
| 3 | `/raise-pr` | Runs tests, pushes, opens a PR with `Closes #N` |
| 4 | `/pr-review [N]` | Reviews the PR against the issue and posts the review on GitHub |

## Conventions

- Never commit directly to `main`; all work on feature branches.
- Branch names: `<feat|fix|docs|chore>/<issue-number>-<kebab-slug>` (e.g. `feat/3-add-user-endpoint`).
- Commits: Conventional Commits with issue ref, e.g. `feat(users): add GET /users/{id} (#3)`.
- PR bodies must include `Closes #<N>` and a Testing section with real command output.
- Tests must pass before raising a PR; don't claim tests pass without running them.
- Never commit secrets or `.env` files.
- Write multi-line issue/PR/review bodies to a temp file and pass `--body-file` (avoids shell quoting issues on Windows).
