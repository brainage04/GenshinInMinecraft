Read `docs/GENSHIN_MINECRAFT_BRIEF.md` (the project spec) and `todo.md`, then start milestone M0.

First steps:
1. Write `AGENTS.md` (verified build/run/test commands, invariants from the brief) and `docs/decisions.md` (seed it with the brief's fixed decisions).
2. Spike the map port: inspect `reference/map/Blocky Teyvat 5.1.0-001.zip` without modifying it, confirm the world's DataVersion and whether any non-vanilla blocks exist, then script the extract-and-upgrade to 26.2 into a git-ignored working directory.
3. Build the dev-only control bridge (screenshot + input + server state check) and prove it on both loaders.
4. Dispatch the `gpt-researcher` agent in parallel on the M1 starter party (Traveler (Anemo), Amber, Kaeya, Lisa), the Mondstadt starter enemies, and the core elemental/damage rules for Genshin 7.1.

Keep `todo.md` current with the next runnable task.
