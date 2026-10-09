---
name: create-issue
description: Create a well-structured GitHub issue (feature, bug, or chore) in this repo from a short description. Use when the user says "create an issue", "file a ticket", or runs /create-issue.
argument-hint: "<short description of the feature/bug/task>"
allowed-tools: Bash(gh issue:*), Bash(gh label:*), Bash(gh repo view:*), Bash(git log:*), Bash(git ls-files:*), Read, Grep, Glob
---

# Create Issue

Turn the user's request into a clear, implementable GitHub issue.

Request: $ARGUMENTS

## Steps

1. **Understand the request.** If `$ARGUMENTS` is empty, ask the user what the issue is about. If it is ambiguous in a way that changes scope (e.g. which entity, which endpoint), ask ONE concise clarifying question; otherwise proceed with sensible assumptions and list them in the issue.

2. **Ground it in the codebase.** Quickly scan the repo (`git ls-files`, Grep/Glob) to find the modules, models, routes or files the issue will touch. Reference real paths in the issue. If the repo is empty, say so in "Technical notes".

3. **Classify** as one of:
   - `enhancement` — new feature / endpoint / behaviour
   - `bug` — something broken (include repro steps, expected vs actual)
   - `documentation` / `chore` — docs, refactor, tooling
   Ensure the label exists: `gh label list`; create missing ones with `gh label create <name> --color <hex> --description "<desc>"`.

4. **Write the issue** with a title in imperative mood, ≤ 70 chars (e.g. `Add POST /orders endpoint with validation`). Body template:

   ```markdown
   ## Summary
   <1–3 sentences: what and why>

   ## Context
   <background, affected files/modules, assumptions made>

   ## Acceptance criteria
   - [ ] <observable, testable criterion>
   - [ ] <include error cases / validation / status codes for APIs>
   - [ ] Tests cover the new/changed behaviour

   ## Technical notes
   <suggested approach, data model changes, edge cases, out of scope>
   ```
   For bugs, add `## Steps to reproduce`, `## Expected`, `## Actual` sections.

5. **Create it** — write the body to a temp file to avoid shell-quoting problems, then:
   ```bash
   gh issue create --title "<title>" --body-file <tmpfile> --label "<label>" --assignee @me
   ```

6. **Report** the issue number and URL, and suggest the next step: `/start-issue <number>`.
