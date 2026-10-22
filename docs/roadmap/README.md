# RuneRealm — OSRS Remake Master Roadmap

> Goal: move `rsmod2` toward broad, verifiable Old School RuneScape parity. Implement in dependency order; do not mark content complete merely because code exists.
>
> Canonical reference: [Old School RuneScape Wiki](https://oldschool.runescape.wiki/). Recheck live pages and linked subpages before verification because OSRS changes over time.

## Repository context
- Repository: https://github.com/3ncrypted-816/rsmod2
- Stack observed: Kotlin / Gradle; default branch `main`.
- GitHub Issues are enabled. Initial backlog created in issues #1–#27. A GitHub Project still needs to be created and the issues added to it; the available connector does not expose Project creation actions.
- This is a roadmap and coverage taxonomy, not a claim that every OSRS entity has already been individually enumerated or implemented.

## Completion tracking
Use one issue per bounded feature or content slice, and split individually testable entities (quest, boss, item interaction, NPC dialogue set) into separate issues when Issues are enabled.

Suggested statuses: `Not Started`, `Ready`, `In Progress`, `Blocked`, `Implemented`, `Testing`, `Verified`, `Deferred`.

Suggested priorities:
- **P0 — Foundation:** blocks multiple systems or all meaningful gameplay.
- **P1 — Core parity:** essential gameplay loop and common content.
- **P2 — Broad content:** substantial world and secondary systems.
- **P3 — Long-tail/polish:** niche, seasonal, optional or refinement work.

## Dependency-first implementation order

### Phase 0 — Audit and baseline (P0)
- [ ] Inventory repository modules, existing systems, data sources and test coverage before duplicating work.
- [ ] Establish a parity ledger: Wiki URL, entity ID/name, review date, implementation state, tests and known deviations.
- [ ] Define the authoritative data pipeline for cache-derived definitions and manually authored scripts.
- [ ] Establish reproducible builds, test conventions, deterministic fixtures and content verification workflow.
- [ ] Record intentional RuneRealm-specific differences separately from OSRS parity gaps.

**Exit:** each known system is marked existing, partial, missing or intentionally out of scope with evidence.

**Baseline status (2026-10-13):** the first audit is recorded in [CONTENT_COVERAGE.md](CONTENT_COVERAGE.md) and [content-inventory.csv](content-inventory.csv), including RuneRealm-specific differences, known risks/unverified values and a prioritized next-work list.

### Phase 1 — Engine and protocol foundations (P0)
- [ ] Protocol, packet decoding/encoding, session lifecycle, login/logout and reconnect.
- [ ] Tick scheduler, world update cycle, entity lifecycle and deterministic event ordering.
- [ ] Player/NPC/object/ground-item entity model and world registration.
- [ ] Coordinates, regions, collision maps, pathfinding, line of sight and reachability.
- [ ] Movement, run/walk, forced movement, teleportation and region/instance transitions.
- [ ] Inventory, equipment, bank, container synchronization and slot/stack rules.
- [ ] Player state, appearance, animation, graphics, overheads, chat and interfaces.
- [ ] Persistence for account data, skills, inventory, equipment, quests, settings and unlocks.
- [ ] Permissions, admin/debug tools, diagnostics, logging and crash recovery.

### Phase 2 — Shared interaction and content runtime (P0)
- [ ] Interaction dispatch: player, NPC, object, ground item, item-on-item/NPC/object/player and spell-on-target.
- [ ] Dialogue state machine, options, chat heads, conditions, rewards and interruption rules.
- [ ] Object/item/NPC actions, examine text, respawns and region-scoped scripts.
- [ ] Timers, queues, delayed events, random rolls, weighted tables, cooldowns and cancellation.
- [ ] Definition loader, validation, duplicate-ID checks and missing-reference reports.
- [ ] Shared requirements/rewards: skills, quests, items, flags, unlocks, XP, currency and teleports.
- [ ] World events, persistent/per-player variables, quest vars and temporary state.

### Phase 3 — Core combat and progression (P0/P1)
- [ ] Combat target selection, timers, range, line of sight, aggression, retaliation, death and respawn.
- [ ] Melee, ranged, magic, special attacks, attack styles, accuracy, defence, damage and prayer bonuses.
- [ ] Hit splats, poison, venom, disease, freezes, binds, stuns, recoil, lifesteal and protection rules.
- [ ] Player/NPC stats, boosts/drains, prayer points, hitpoints and death/reclaim rules.
- [ ] XP curves, level-up, combat level, requirements and unlock hooks.
- [ ] NPC combat definitions, animations, graphics, attack styles, immunities, drops and safespots.
- [ ] Deterministic combat test harness with tick-by-tick assertions and edge-case coverage.

### Phase 4 — Core world, items and economy (P1)
- [ ] Item definitions: IDs, names, examine, stackability, tradeability, weight, value, noted/placeholder forms and equipment stats.
- [ ] Ground items, ownership/visibility timers, pickup/despawn and item-on-ground actions.
- [ ] Shops, stock/restock, buy/sell pricing, shop interfaces, currencies and specialty shops.
- [ ] Banks, deposits/withdrawals, search, placeholders, notes and supported presets.
- [ ] Trading, confirmations, wealth/quantity checks, cancellation and anti-duplication invariants.
- [ ] Equipment slots, requirements, bonuses, set effects, two-handed rules, ammo/runes and charged/degradable gear.
- [ ] Grand Exchange if in target scope: offers, matching, tax, collection, limits and persistence.
- [ ] Regions, collision/object/NPC spawns, doors, ladders, stairs, gates, bridges and shortcuts.
- [ ] Transport: ships, charters, fairy rings, spirit trees, gliders, minecarts and teleport networks.
- [ ] Music regions and environmental interactions.

### Phase 5 — All skills and shared skill infrastructure (P1)
Build common XP/level/requirement/tool/animation/resource/product/boost primitives first, then track each skill independently:
- [ ] Attack
- [ ] Strength
- [ ] Defence
- [ ] Ranged
- [ ] Prayer
- [ ] Magic
- [ ] Runecraft
- [ ] Hitpoints
- [ ] Crafting
- [ ] Mining
- [ ] Smithing
- [ ] Fishing
- [ ] Cooking
- [ ] Firemaking
- [ ] Woodcutting
- [ ] Agility
- [ ] Herblore
- [ ] Thieving
- [ ] Fletching
- [ ] Slayer
- [ ] Farming
- [ ] Construction
- [ ] Hunter
- [ ] Sailing — verify live Wiki and exact target game revision.

For each skill, audit actions, requirements, tools, boosts, XP awards, resource depletion, products, failure states, animations/graphics, unlocks, quest/diary/minigame integrations and edge cases.

### Phase 6 — Quests, diaries and unlocks (P1/P2)
- [ ] Quest framework: start conditions, prerequisites, stages, variables, dialogues, interactions, combat encounters, rewards and quest points.
- [ ] Enumerate every current Wiki-listed quest, including miniquests and applicable variants.
- [ ] Implement prerequisite chains and shared systems before dependent quests; group by series/region where useful.
- [ ] Quest journals, objectives, completion state, rewards, unlocks and post-quest dialogue.
- [ ] Achievement diaries by region/tier: tasks, rewards, lamps, teleports and passive effects.
- [ ] Combat Achievements: task definitions, completion conditions, tiers, rewards, kill counters and variants.
- [ ] Collection log and unlock tracking for bosses, clues, minigames, pets and uniques.
- [ ] Account flags, music tracks, emotes, areas, spells, prayers and transport unlocks.

### Phase 7 — Bosses, raids, Slayer and endgame PvE (P1/P2)
- [ ] Slayer assignment, task progress, streaks, points, unlocks, extensions, blocks and superior monsters.
- [ ] Boss framework: phases, mechanics, minions, projectiles, specials, arena boundaries, resets and loot.
- [ ] Enumerate every current OSRS boss and variant, including wilderness and group encounters.
- [ ] Raids: Chambers of Xeric, Theatre of Blood and Tombs of Amascut; rooms, puzzles, scaling, roles, rewards and replay state.
- [ ] God Wars Dungeon, wilderness bosses, demi-bosses, skilling bosses and quest bosses.
- [ ] Drop tables, unique rates, tertiary drops, collection-log entries, kill counts, pet rolls and team rules.
- [ ] Instancing, group state, encounter ownership, loot ownership, disconnect/rejoin and exploit cases.

### Phase 8 — Minigames, activities, PvP and social systems (P2)
- [ ] Enumerate every current minigame/activity; entry, rounds, scoring, rewards, failure and exit behavior.
- [ ] PvP combat rules, wilderness levels, skulls, safe/unsafe zones, target restrictions, loot keys and death drops.
- [ ] Clan/group features and party coordination where in scope.
- [ ] Clue scrolls: all tiers, steps, coordinates, emote clues, puzzle boxes, ciphers, hot/cold, rewards and unique rolls.
- [ ] Treasure Trails rewards, collection log and clue-specific requirements.
- [ ] Random events, eligibility, rewards and supported mechanics.
- [ ] Achievement systems, music, emotes, pets, POH, farming patches and recurring activities.

### Phase 9 — Full content parity sweep (P2/P3)
- [ ] Complete item catalog, including variants, charges, degradation, transforms, noted forms and placeholders.
- [ ] Complete NPC catalog: dialogue, shops, combat stats, drops, transforms, respawns and spawn locations.
- [ ] Complete object catalog: actions, state changes, collision, animation, replacements and coordinates.
- [ ] Complete ground item, spot animation, projectile, graphic and animation definitions needed by server behavior.
- [ ] Complete regions, instances, dungeon layouts, object/NPC spawns, multi-combat and wilderness flags.
- [ ] Complete spells, spellbooks, rune costs, requirements, autocast, target rules and effects.
- [ ] Complete prayers, drain rates, conflicts, overheads, protection mechanics and unlock conditions.
- [ ] Complete equipment, sets, jewellery, charges, degradables, specials, ammunition and set bonuses.
- [ ] Complete drop tables, shops, crafting recipes, item sinks, rewards, spawn/death/respawn definitions.
- [ ] Decide scope for seasonal, holiday, Leagues, Deadman, beta and temporary content; record explicit exclusions.
- [ ] Split remaining gaps from the live Wiki audit into issue-sized tasks.

### Phase 10 — Verification and release gates (P0 across all phases)
- [ ] Unit tests for shared rules and data validation.
- [ ] Integration tests for player journeys, quest chains, combat encounters, trading and persistence.
- [ ] Golden parity fixtures for definitions, rewards, XP, drops, dialogue branches and tick timing.
- [ ] Regression checklist after engine/protocol changes.
- [ ] Load/performance tests, persistence recovery and economy/duplication checks.
- [ ] Content coverage report: audited/implemented/tested/verified/excluded by domain.
- [ ] Release-candidate pass and documented known differences.

## Test bots (planned, not built yet)

Automated in-game bots that exercise content the way players do, so parity gaps show up as structured reports instead of being found by hand. **Status:** planned. Add it once the Phase 0 audit and the core systems (Phases 1-3) are in place, and build **Layer 1 first**. Tracked under #11 (verification), with findings filed against the owning issues.

**Bot players**
- Real, visible in-game players (they appear to other players and in the client), not headless test fixtures.
- At most **300** bots spawned at once.
- Each bot is spawned from a profile with configurable skill levels, weapon, worn gear and inventory, so the same task can be tried at different levels and loadouts.
- Bots attempt a task, record the result, then **move on** to the next task. A failure never stops the run.

**Structured failure reports**
Each failed attempt records at least: bot id and profile, task id, target (npc/loc/obj type and internal name, coordinates), op or action attempted, expected result, observed result, failure kind (no handler, exception, wrong message, wrong item/XP/state, timeout/stuck), server exception or stack trace if any, tick number, and the wiki URL the expectation came from. Reports are machine-readable so they can be grouped by module and turned into issues, and they feed the coverage counts in [CONTENT_COVERAGE.md](CONTENT_COVERAGE.md).

**Layer 1: cache-driven right-click sweep (build first)**
- Walk every npc, loc and obj type in the cache (and their spawns), resolve multi-types with the bot's vars, and try every right-click op the client would show.
- Report any op with no handler (the generic "Nothing interesting happens." fallback), any server error, and any op that leaves the bot stuck or delayed.
- Needs no hand-written expectations, so it covers the whole world at once. `LumbridgeNpcOpsTest` (`content/areas/city/lumbridge`) is a Lumbridge-only prototype of this check.

**Layer 2: wiki-scripted task bots**
- Scripted tasks taken from wiki pages (e.g. "chop an oak at level 15 with a steel axe", "cook a raw trout on a range", "pickpocket a man"), each with expected results: items gained/lost, XP, messages, animations, level requirements and failure paths.
- Run each task across bot profiles (below/at/above the level requirement, with and without the tool) and diff the observed result against the expectation.
- Tasks cite the wiki page and review date, so a passing task is evidence toward `Verified`.

## Definition of Verified
A task is `Verified` only after the current Wiki page and relevant subpages are reviewed and dated; required data/behaviors and edge cases are enumerated; implementation paths/commits are linked; automated tests or documented manual verification pass; differences are fixed or explicitly excluded; and the inventory is updated.

## Recommended GitHub Project
Project name: **RuneRealm — OSRS Parity Roadmap**.
Suggested fields: Status, Priority, Phase, Domain, Dependencies, Wiki reviewed date, Coverage state. Suggested views: Roadmap (group by Phase), Execution board (group by Status), P0 blockers, Content audit (group by Domain), Ready next.

## Primary Wiki entry points
- [OSRS Wiki](https://oldschool.runescape.wiki/)
- [Skills](https://oldschool.runescape.wiki/w/Skills)
- [Quests](https://oldschool.runescape.wiki/w/Quests)
- [Items](https://oldschool.runescape.wiki/w/Items)
- [NPCs](https://oldschool.runescape.wiki/w/NPC)
- [Monsters](https://oldschool.runescape.wiki/w/Monsters)
- [Bosses](https://oldschool.runescape.wiki/w/Boss)
- [Locations](https://oldschool.runescape.wiki/w/Locations)
- [Minigames](https://oldschool.runescape.wiki/w/Minigames)
- [Activities](https://oldschool.runescape.wiki/w/Activities)
- [Spells](https://oldschool.runescape.wiki/w/Spells)
- [Prayer](https://oldschool.runescape.wiki/w/Prayer)
- [Achievement Diaries](https://oldschool.runescape.wiki/w/Achievement_Diary)
- [Combat Achievements](https://oldschool.runescape.wiki/w/Combat_Achievements)
- [Clue scrolls](https://oldschool.runescape.wiki/w/Clue_scroll)
- [Treasure Trails](https://oldschool.runescape.wiki/w/Treasure_Trails)
- [Random events](https://oldschool.runescape.wiki/w/Random_events)
- [Game updates](https://oldschool.runescape.wiki/w/Updates)

These are discovery entry points. Follow their current category/subpage links to maintain an exhaustive inventory for the selected game revision.
