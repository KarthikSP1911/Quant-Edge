# QuantEdge

AI-powered stock research and simulated trading platform.

See [CLAUDE.md](./CLAUDE.md) for the full project spec, tech stack, and workflow rules.

## Setup

After cloning, run these once from the repo root:

```bash
npm install          # installs lefthook, commitlint, prettier at root
                      # and auto-runs `lefthook install` via postinstall
cd frontend && npm install
cd ../backend && ./mvnw -q -DskipTests install
```

This wires up the git hooks (pre-commit formatting, commit-msg linting,
pre-push checks) documented in [CLAUDE.md § Tooling](./CLAUDE.md#tooling).

If `npm install` reports pending install scripts under `npm warn allow-scripts`,
approve lefthook's postinstall so hooks actually get installed:

```bash
npm approve-scripts lefthook
```
