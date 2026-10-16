# Git Commit — Type Reference

Only these types are acceptable (from project `release-it.json` / conventional changelog sections):

| Type | Section | Use when |
|---|---|---|
| `feat` | Features | Wholly new product capability from scratch (did not exist before) |
| `add` | Add | Extension to an existing feature/area; or new file, dependency, or resource that is not a full new capability |
| `fix` | Bug Fixes | Bug fix |
| `docs` | Documentation | Documentation only |
| `style` | Styles | Formatting/style with no logic change |
| `refactor` | Code Refactoring | Restructure; no feature addition, no bug fix |
| `perf` | Performance Improvements | Performance improvement |
| `test` | Tests | Adding or updating tests |
| `build` | Builds | Build system or dependency tooling changes |
| `merge` | Merge | Merge commits |
| `ci` | Continuous Integrations | CI/CD config |
| `chore` | Chores | Maintenance/misc |
| `revert` | Reverts | Revert a previous commit |
| `deprecate` | Deprecated | Mark something as deprecated |
| `security` | Security | Security fix |
| `remove` | Removals | Remove a file, dependency, or feature |
| `upgrade` | Packages Upgrades | Package/dependency version upgrade |
| `wip` | Work In Progress | Not ready for release |

## `feat` vs `add` examples

| Change | Type |
|---|---|
| First analytics dashboard (product had none) | `feat` |
| Date range filter on existing analytics | `add` |
| First auth system (sign-in/sign-up) | `feat` |
| Auth already exists; add password-reset page | `add` |
| New zod schema for existing notifications domain | `add` |
| First notifications product surface | `feat` |

## Path escaping for `git add`

Bash treats `(` `)` as metacharacters. Always escape them (and other shell specials) in suggested `git add` paths:

| Path | Escaped |
|---|---|
| `src/app/(auth)/` | `src/app/\(auth\)/` |
| `src/app/(dashboard)/settings/` | `src/app/\(dashboard\)/settings/` |
