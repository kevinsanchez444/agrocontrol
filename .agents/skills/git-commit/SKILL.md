---
name: git-commit
description: Suggests conventional commit messages using project types from release-it.json. Distinguishes feat (wholly new capability) from add (extension to existing). Outputs separate copyable git add, git commit, and message blocks with shell-escaped paths. AI suggests only — never commits or pushes. Use when an implementation is complete, when asked for a commit message, or when deciding how to stage and group changes.
---

# Git Commit

**AI role: suggest only. Never run `git commit`, `git push`, or any git write command.**

When an `IMPLEMENTATION_PLAN_*` **commit checkpoint** exists for the finished phase: use it as **scope/boundary + subject hint** only. Re-read the working tree and emit exact `git add` / message here — live tree wins if paths or wording drifted. Do not mix later-phase files called out in the checkpoint boundary.

## Commit Format

```
<type>[optional scope]: <description>
```

- No body/footer — subject only; aim **≤75** chars, **hard cap 100**
- Imperative present: `add` not `added`; focus on major change
- Breaking: `!` after type/before scope (`feat(api)!: rename checkout payload`)
- Ban: first/second person, meta phrases (`this commit`, `as requested`, `now`), AI attribution, emoji (unless project requires)

## Types

Only types from project `release-it.json` — see [REFERENCE.md](REFERENCE.md).

### `feat` vs `add` (critical)

| | `feat` | `add` |
|---|---|---|
| When | Product capability **did not exist** — shipped from scratch | Extend **existing** feature/area, or new file/dep/resource that is not a full new capability |
| Example | No analytics → ship analytics | Analytics exists → date filter / extra chart |
| Default | — | Prefer `add` when unsure |

Other common: `fix`, `refactor`, `docs`, `style`, `perf`, `test`, `build`, `ci`, `chore`, `remove`, `upgrade`, `security`, `deprecate`, `revert`, `merge`, `wip`.

## Output format (every suggestion)

**Three separate fenced blocks** — never one combined block:

1. `git add …`
2. `git commit -m "…"`
3. Message alone (same subject)

**Shell-escape** `(` `)` and other metacharacters in paths: `src/app/(auth)/` → `src/app/\(auth\)/`.

### Example (single)

```
git add src/schemas/notification.ts
```

```
git commit -m "add(schemas): add notification zod schema"
```

```
add(schemas): add notification zod schema
```

### Multi-feature split

Split by domain; foundational first. Repeat three-block pattern per group.

```
git add src/schemas/auth.ts src/actions/auth/
```

```
git commit -m "feat(auth): add sign-in, sign-up, and password reset"
```

```
feat(auth): add sign-in, sign-up, and password reset
```

```
git add src/app/\(auth\)/
```

```
git commit -m "add(routing): add auth route group with login and signup"
```

```
add(routing): add auth route group with login and signup
```

## What NOT to Suggest

- Never `.env`, credentials, or secrets — full policy: [secrets-hygiene](../secrets-hygiene/SKILL.md)
- Before suggesting `git add`, scan candidate paths for `.env*` (except `.env.example`) and secret-shaped files; omit them and warn
- Never implementation-detail descriptions
- Never minor-change focus when a major change exists in the same group
