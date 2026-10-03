# Fidelity ledger

Track behaviour that is approximated or deliberately changed for Minecraft, as required by [the brief](../docs/GENSHIN_MINECRAFT_BRIEF.md). Status is one of `specified`, `implemented`, `verified`, or `adapted`; do not mark a design implemented or verified without the corresponding code or evidence. Mechanics reference is Genshin 7.1 unless a dated decision re-pins it.

| behaviour | status (`specified` / `implemented` / `verified` / `adapted`) | reason |
| --- | --- | --- |
| Genshin's 60 fps combat frame timing → Minecraft's 20 TPS execution, using a deterministic ordered rules timeline with sub-tick timestamps rather than rounding every event to a tick. | specified | Minecraft ticks every 50 ms, while Genshin reference frames are about 16.67 ms. Preserve source event times and ordering in the rules layer; server/world integration still runs on Minecraft ticks. Implementation and residual timing/visual differences remain to be measured and recorded, not assumed equivalent. |
| World preservation and vanilla interference in the generated development arena. | adapted | Saved-world managed mode disables hunger/starvation/hunger healing, natural regeneration/spawning, fire spread and mob griefing; survival/adventure breaking, placement and farmland trampling are refused, with creative bypass for owner editing. Weather/day-night and vanilla entity damage are unchanged. This is a Minecraft map-preservation adaptation, not a sourced Genshin combat/traversal implementation; see the 2026-10-04 managed-mode decision. |
