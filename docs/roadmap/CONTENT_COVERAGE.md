# OSRS Content Coverage Inventory

Use this as the top-level completeness audit. Split each row into smaller issues when an individual entity or behavior can be implemented and tested independently. This is a taxonomy checklist, not a pre-enumerated list of every individual OSRS entity.

The machine-readable version of this audit is [`content-inventory.csv`](content-inventory.csv) (one row per slice, with the same status, evidence and deviation columns).

## Phase 0 baseline (issue #1)

- **Audited:** 2026-10-13, against branch `claude/magical-brahmagupta-7cdkeo` at `d0dd913` (revision 233 cache, packages `org.runerealm.*`).
- **Method:** module-by-module code read, `TODO` and comment grep, and `@Test` method counts per `src/integration` source set (398 integration tests in 39 modules; there are no `@ParameterizedTest`/`@TestFactory` tests in those source sets). Unit tests (`src/test`) are listed where they matter.
- **Wiki review:** the OSRS Wiki host could not be resolved from the audit environment, so the "what's missing" lists below come from the wiki's skill and system pages as known at audit time plus the repo's own wiki citations. Each slice must re-check the live wiki page before it is marked `Verified` (see the README's Definition of Verified).
- **Client verification:** nothing below has been verified in the real (RSProx RuneLite) client unless the row says so. Every row is **not client-verified** by default.

### Status legend

| Mark | Status | Meaning |
|---|---|---|
| `[x]` | **Existing** | Implemented for the full OSRS scope of the slice and covered by tests. Still needs wiki and client verification before `Verified`. |
| `[~]` | **Partial** | Some of the slice works and is tested; gaps are listed. |
| `[ ]` | **Missing** | No implementation (cache refs or interface scaffolding alone do not count). |
| `[-]` | **Out of scope** | Intentionally excluded or deferred; the reason is given. |

"Tests" means integration test classes (`src/integration`) and their `@Test` count, written `ClassName (n)`. All test paths are under the module named in the same row.

## Shared engine and runtime

| Status | Slice | Modules | Integration tests | Known deviations / gaps |
|---|---|---|---|---|
| `[~]` | Protocol, login/session, packets, client sync | `api/net` (RSProt), `api/account`, `api/registry`, `engine/game`, `server/app` | `api/game-process`: `PlayerMapUpdateTest (1)`, `PlayerZoneUpdateProcessorTest (2)`; `content/other/login`: `LoginVarsTest (1)`. Unit: `engine/game` 128, `server/app` 4 | **No reconnection** (`ConnectionHandler.kt` "TODO: Reconnection"), no token auth, email-login usernames undecided. 2FA/TOTP exists. Login/logout edge cases are not tested. |
| `[~]` | Tick scheduler, queues, timers, event ordering | `api/game-process`, `engine/coroutine`, `engine/events` | `api/game-process`: `PlayerQueueProcessorTest (2)`, `NpcQueueProcessorTest (2)`, `NpcTimerProcessorTest (1)`, `ControllerTimerProcessorTest (1)`; `api/player`: `PlayerDelayTest (3)`. Unit: `engine/coroutine` 9, `engine/events` 12 | `GameCycle.kt` post-tick ordering is "an educated guess". `delay(n)` deliberately differs from the official `p_delay` convention (`docs/quirks.md`). Keyed events allow one handler per key (startup crash on duplicates). |
| `[~]` | Entity lifecycle: player, NPC, loc, ground obj, projectile, spotanim | `engine/game`, `api/repo`, `api/npc`, `api/obj-plugin` | `api/game-process`: `NpcWanderProcessorTest (3)`, `NpcPatrolModeProcessorTest (5)`, `NpcFaceSquareProcessorTest (3)`, `PlayerFaceSquareProcessorTest (4)`; `api/repo`: `RegionRepositoryTest (1)` | NPC modes `OpPlayer6-8`/`ApPlayer6-8` are `TODO()` (throw). Ground-obj despawn duration unverified (`ObjRepository.kt` TODO). |
| `[~]` | Pathfinding, collision, line of sight, reachability, movement | `engine/routefinder`, `engine/map`, `api/route`, `api/game-process` | `api/game-process`: `PlayerMovementProcessorTest (5)`, `PlayerRouteRequestTest (3)`, `PlayerMovementMapFlagTest (3)`, `NpcMovementProcessorTest (1)`, `PlayerInteractionProcessorTest (7)`, `NpcInteractionProcessor*Test (3)`; `api/player`: `ProtectedAccessPlayerMove*Test (9)`. Unit: `engine/routefinder` 39, `engine/map` 54 | Run energy drain/restore and weight exist (`PlayerRunUpdateProcessor`) but are untested. Forced movement only via agility shortcuts. |
| `[~]` | Teleportation, regions/instances, map flags | `engine/map`, `api/game-process`, `api/area-checker`, `api/cache-enricher` | `api/game-process`: `PlayerTeleportTest (4)` | Instancing exists at engine level (regions) but no content uses it. Multiway areas come from cache enrichment; "singles-plus" is not supported in combat (`PvNCombatScript`/`PvPCombatScript` TODOs). |
| `[~]` | Persistence: account, skills, invs, settings, unlocks | `api/account`, `api/db`, `api/db-gateway` (SQLite + Flyway migrations V1-V12) | `content/other/login`: `LoginVarsTest (1)`; others indirectly | Only `persistent` varps load from saves (7959b36). No emergency backup when the save service fails (`AccountRegistry.kt` TODO). No quest/diary/collection-log state because those systems do not exist. |
| `[~]` | Containers: inventory, equipment, bank, trade, shop | `engine/objtx`, `api/invtx`, `api/inv-plugin`, `api/inv-weight` | `api/invtx`: `InvTransactionsTest (9)`; `api/inv-plugin`: `InvMapInitTest (2)`; `api/player`: `HeldEquipOpTest (11)`, `WornUnequipOpTest (5)`. Unit: `engine/objtx` 117 | Equipment tab inv types flagged incorrect (`EquipmentTabInvs.kt` TODO). |
| `[~]` | Interfaces, dialogue, chat, anims, graphics, sounds, music | `api/player` (dialogue, ui), `api/player-output`, `api/music`, `api/music-plugin`, `content/interfaces/*` | `api/player`: `IfCloseTest (2)`; settings `SettingsCloseTest (2)`, `EscClosesInterfaceTest (1)` | Music region unlocks exist but first-play timing differs (`MusicTimerScript` TODOs). Dialogue mesanims partly wrong (`BarfyBill.kt` TODO). |
| `[~]` | Definitions / data loading, validation, ID cross-reference | `api/cache`, `api/cache-enricher`, `api/type/*` (type builders, editors, references, verifier), `.data/symbols` | `api/cache`: `TypeIdentityHashCollisionTest (16)`, `VarpBitCollisionTest (1)`, `VarnBitCollisionTest (1)`. Unit: `api/cache` 24, `api/registry` 21 | Server-only ids are hand-numbered in `.data/symbols/*.sym` and clash on merges. Removing a type edit does not repack an existing cache. No unresolved-reference report beyond startup verification. |
| `[~]` | Admin/debug tooling, logging, security | `content/other/commands`, `api/cheat`, `api/pw-hash` (Argon2), `api/totp` | none | Commands: `anim invadd invclear locadd locdel master mypos npcadd realm reboot reset slowreboot spot tele telezone varbit varp`. No metrics, no anti-duplication audit, no structured crash recovery. |

## Interactions, dialogue and timers (Phase 2 runtime)

| Status | Slice | Modules | Integration tests | Known deviations / gaps |
|---|---|---|---|---|
| `[~]` | Op dispatch: npc/loc/obj/player/held/worn ops, item-on-X, spell-on-X, IF_SUBOP | `api/player/interact`, `api/npc/interact`, `api/net` handlers, `api/script` | `api/player`: `HeldEquipOpTest (11)`, `WornUnequipOpTest (5)`, `PlayerProtectedAccessTest (2)`; Lumbridge `LumbridgeNpcOpsTest (1)` audits every Lumbridge npc op | No generic "Nothing interesting happens." audit outside Lumbridge. Ops are only handled where a content script registers them. |
| `[~]` | Dialogue state machine, options, chatheads | `api/player/dialogue`, `content/areas/city/lumbridge` | `LumbridgeNpcTalkTest (1)`, `LumbridgeNpcRightClickTest (6)` | Text width for mod icons hard-coded (`TextAlignment.kt` TODO). |
| `[~]` | Timers, queues, delays, randoms, weighted tables | `api/game-process`, `api/random`, `api/drop-table` | see engine rows; unit `api/random` 16, `api/drop-table` 5 | All randoms must go through `GameRandom` (konsist test enforces). |
| `[~]` | Generic locs: doors, gates, ladders, stairs, bookcases, signposts, pickables, search | `content/generic/generic-locs` | `SpiralStaircaseScriptTest (4)` | Handlers are keyed by content group, so only locs assigned in `DoorConfigs` (21 locs), `GateConfigs` (12), `LaddersConfig` (6), `StaircaseConfig` (11) work. Pickables: cabbage, onion, potato, wheat only (no flax). Doors/gates/ladders have no tests. |
| `[ ]` | Shared requirements/rewards framework (quest, diary, unlock checks) | none | none | Each skill checks its own requirements; there is no shared quest/unlock requirement type. |

## Combat and progression

| Status | Slice | Modules | Integration tests | Known deviations / gaps |
|---|---|---|---|---|
| `[~]` | Melee (PvN, NvP, PvP) | `api/combat/*`, `api/weapons`, `api/combat-accuracy`, `api/combat-maxhit` | `api/combat/combat-formulas`: `PvNMeleeAccuracyTest (1)`, `NvPMeleeAccuracyTest (1)`, `PvPFormulaTest (1)` (table-driven); unit tests in `combat-formulas`/`combat-maxhit` | Vampyre modifiers, slayer-task modifier and "in Wilderness" npc attribute are TODO. **NPCs can only attack with melee** (`NvPCombat.attack` takes `CombatAttack.NpcMelee` only). No recoil/degradation in hit processors. |
| `[~]` | Ranged | `api/combat`, `api/weapons`, `content/other/special-weapons` | `PvNRangedAccuracyTest (1)`, `NvPRangedAccuracyTest (1)` | Bows, crossbows, thrown and ammo are handled. Enchanted bolt effects are missing. Blowpipe dart bonus and Dizana's quiver bonus are TODO (`WornBonuses.kt`). Chinchompa AoE not audited. |
| `[~]` | Magic combat | `api/spells`, `api/spells-runes`, `api/spells-autocast`, `content/skills/magic/spell-attacks`, `content/other/special-weapons` (Tumeken's shadow) | `PvNMagicSpellAccuracyTest (1)`, `NvPMagicAccuracyTest (1)`, `PvNMultiStyleAccuracyMagicalMeleeTest (1)`; unit `api/spells-runes` 3 | Only the 20 standard elemental spells (Strike/Bolt/Blast/Wave/Surge) are castable, plus autocast and Tumeken's shadow. No bind/snare/entangle, Ancient Magicks, god spells, Iban Blast, Magic Dart, Crumble Undead, Lunar or Arceuus combat spells. No combat spell-specific tests. |
| `[~]` | Special attacks | `api/specials`, `content/other/special-attacks` | `CombatSpecialAttacksTest (12)` | Weapons registered: abyssal whip (+frozen/volcanic), dragon dagger (+p/p+/p++), dragon longsword, dragon mace, dragon scimitar (+or), dragon warhammer (+cr), dragon claws (+cr), Armadyl/Bandos/Saradomin/Zamorak godswords (+or), dark bow (all colours, BH), magic shortbow (+i); stat-boost specials for dragon/infernal/3rd age/crystal axes, harpoons and pickaxes. **Tests only cover** dragon dagger, DWH, BGS, dragon claws, ZGS, dragon scimitar and energy costs; whip, DLS, mace, AGS, SGS, dark bow, MSB and stat-boost specs are untested. Missing: granite maul, dragon halberd, SGS-style others, Voidwaker, blowpipe, ACB, ZCB, ballistae, etc. Multipliers are from code comments, not re-checked against the live wiki (see Known risks). |
| `[~]` | Prayers: activation, drain, effects | `content/interfaces/prayer-tab`, `api/combat` | `ProtectionPrayerLockTest (2)` (prayer-tab); `CombatPrayerTest (6)`, `RedemptionTest (2)` (game-process); `PrayerTrainingTest (4)` (skills/prayer) | Drain, quick prayers, Preserve, Rapid Heal/Restore, Smite, Redemption, Retribution, protection (40% in PvP), Sever/prayer-disable lock exist. **Prayer unlocks are ungated** (Piety/Chivalry/Rigour/Augury/Preserve need no quest or scroll). Deadeye/Mystic Vigour presence noted but effects not audited. |
| `[~]` | Freeze / bind | `api/freeze` | `FreezeTest (6)` (api/freeze); ZGS tests in specials | Mechanic is complete (immunity, 10-tile PvP break, npc resistance) but **only the Zamorak godsword uses it**: no bind/snare/entangle/ice spells exist yet. |
| `[ ]` | Poison, venom, disease, stuns, recoil | hitmark refs only (`BaseHitmarks`) | none | No status-effect engine. Pickpocket stun is local to Thieving. Ring of recoil missing (`StandardPlayerHitProcessor` TODO). |
| `[~]` | Player death, gravestones, Death's Office, items kept | `api/death`, `api/death-plugin`, `content/interfaces/equipment` (Items Kept on Death) | `DeathItemsTest (8)`, `WildernessDeathTest (2)`, `HardcoreIronmanDeathTest (2)` (game-process) | 3 items kept (+1 Protect Item, 0 skulled), 15-minute grave paused while logged out, wiki fee tiers, Death's Office (120 stacks, 5% fee, ironmen half). No Death's coffer (fees from bank/inventory). Office entrances/coordinates not client-verified. |
| `[~]` | NPC death and drops | `api/death` (`NpcDeath`), `api/drop-table`, `content/other/npc-drops` | `NpcDeathDropsTest (2)` (game-process), `LumbridgeNpcDropsTest (5)` | Drop tables exist **only for Lumbridge npcs** plus shared tables; other npcs use a fallback (`NpcDeath.kt` TODO). No rare drop table audit, tertiaries, pets, or clue drops. |
| `[~]` | PvP / Wilderness | `content/areas/wilderness`, `api/area-checker`, `api/combat` (PvP), `api/death` | `WildernessDitchTest (3)`, `WildernessOverlayTest (3)`; `WildernessDeathTest (2)`; `CombatPrayerTest (6)` | Attack option in Wilderness, level range, 20-minute skull, ditch warning, overlay, safe first two tiles, PvP death drops. Missing: loot keys, PvP worlds, multi-combat PvP rules ("updated multiway logic" TODO), target-death flag, Ferox Enclave bounds are approximate, Wilderness agility/resource area content, Revenants, bounty hunter. |
| `[~]` | Combat tab, styles, autocast UI | `content/interfaces/combat-tab`, `api/spells-autocast` | `CombatTabConfigTest (1)` | Auto-retaliate and styles present; little test coverage. |
| `[~]` | XP, levels, combat level, boosts/drains, stat regen | `api/player/stat`, `api/stats`, `api/stats-plugin`, `api/stats/xpmod`, `api/stats/levelmod` | `StatOperationsTest (12)`, `PlayerSkillXPTest (2)` (api/player); `WornXpModifiersTest (1)` (xpmod) | Stat regen exists (`StatRegenScript`) but is untested. |
| `[ ]` | Slayer | none | none | No masters, tasks, points, unlocks or superiors (only slayer params referenced by formulas). |
| `[-]` | Bosses and raids | none | none | Not started; deferred to Phase 7 (#8, #19, #20). |

## Skills (each skill audited individually)

Every skill below is **not client-verified**. "Missing" lists are the main training methods from the wiki's skill and training pages that have no implementation.

| Status | Skill | Modules | Integration tests | What exists | What's missing |
|---|---|---|---|---|---|
| `[~]` | Attack | `api/combat`, `content/interfaces/combat-tab` | combat formula tests (see Combat) | XP from melee combat with Accurate/Controlled styles. | Training spots outside Lumbridge (most npcs have no drops/aggression tuning), Attack-specific unlocks (Warriors' Guild entry requirements, cyclops, defenders). |
| `[~]` | Strength | `api/combat` | as above | XP from melee combat. | Warriors' Guild, strength-only unlocks, potions are drinkable but not makeable (no Herblore). |
| `[~]` | Defence | `api/combat` | as above | XP from all combat styles' defensive/longrange modes; Defence skillcape from Harlan (melee tutor). | No Defence-specific content beyond combat. |
| `[~]` | Hitpoints | `api/combat`, `api/stats-plugin` | `StatOperationsTest (12)` | XP from combat damage, regen, food (`content/other/consumables`, `ConsumablesScriptTest (7)`). | Hitpoints regen boosts (Regen bracelet, Rapid Heal audit), most foods (only pies, pizzas, cakes, karambwan, cooked fish/meat). |
| `[~]` | Ranged | `api/combat`, `content/other/special-weapons` | `PvNRangedAccuracyTest`, `NvPRangedAccuracyTest` | Bows, crossbows, thrown weapons, dark bow / MSB specs. | Enchanted bolt effects, blowpipe, chinchompa audit, Ranging Guild, ammo recovery audit (Ava's devices). |
| `[~]` | Prayer | `content/skills/prayer` | `PrayerTrainingTest (4)` | Bury bones, scatter demonic ashes (wiki XP), "Pray-at" altars, Wilderness Chaos Temple altar (350%, 50% save). | Gilded altar (needs Construction), Ectofuntus, ensouled heads (needs Arceuus spells), bonecrusher/ash sanctifier, Libation bowl / Shrine of Ralos, prayer unlock gating. |
| `[~]` | Magic | `content/skills/magic/spell-attacks`, `content/skills/magic/teleports`, `api/spells*` | `HomeTeleportTest (7)`, `ItemTeleportsTest (7)`; `SpellbookFilterTest (1)` | 20 elemental combat spells, autocast, Lumbridge Home Teleport (standard book only), teleport jewellery, teleport tablets, spellbook filters. | **Every non-combat standard spell** (Varrock/Lumbridge/Falador/Camelot/Ardougne/Watchtower/Trollheim/Ape Atoll/Kourend teleports, alchemy, Superheat, enchant jewellery/bolts, bones to bananas/peaches, Telekinetic Grab, charge orbs, Teleother), bind/snare/entangle, Crumble Undead, god spells, Iban Blast, Magic Dart, Charge; all of Ancient, Lunar and Arceuus spellbooks (including their home teleports); spellbook switching. |
| `[ ]` | Runecraft | none (essence mining exists in Mining) | none | Rune and pure essence can be mined. | Everything: altars and ruins, talismans/tiaras, crafting runes and multiples, pouches, combination runes, Abyss, ZMI, Guardians of the Rift, blood/soul/Daeyalt essence. |
| `[~]` | Crafting | `content/skills/crafting` | `CraftingScriptTest (14)` | Spinning (wool, flax, magic roots, sinew), pottery wheel and oven, soft clay, leather/hard leather/snakeskin/d'hide armour, studded armour, gem cutting (semi-precious crush), amethyst, gold and silver jewellery via the furnace interfaces, amulet stringing, molten glass and glassblowing, battlestaves. | **Tanning** (Ellis / tanners: no handler anywhere), loom weaving (sacks, baskets, cloth), birdhouses, splitbark, Xerician fabric, drift nets, tiaras/unholy symbols audit, lantern assembly, seaweed/giant seaweed glass, Crafting Guild requirements. |
| `[~]` | Mining | `content/skills/mining` | `MiningScriptTest (8)` | Every standard rock by cache category (clay to runite, blurite, lovakite, soft clay, gem rocks, amethyst, granite, sandstone, rune/pure essence), pickaxe tiers, respawns, random gem table (glory boost). | Mining gloves, Varrock armour, mining cape and celestial ring effects; clue geodes, Rock golem pet; infernal pickaxe smelting, crystal pickaxe charges; Motherlode Mine, shooting stars, Volcanic Mine, Zalcano, Blast Mine, Daeyalt essence, Mining Guild boosts. |
| `[~]` | Smithing | `content/skills/smithing` | `SmithingScriptTest (8)` | Smelting all bars (iron 50% / ring of forging 140 charges, coal ratios, goldsmith gauntlets), anvil smithing bronze to rune via cache enums, Lumbridge rusted anvil bronze-only. | Cannonballs (ammo mould), Blast Furnace, Giants' Foundry, Varrock armour double bars, smithing cape, lovakite bars / Shayzien armour, dragon/crystal forging, Smithing-specific quest items. |
| `[~]` | Fishing | `content/skills/fishing` | `FishingScriptTest (10)` | **Only the Lumbridge spots**: small net (shrimps, anchovies), sea bait (sardine, herring), lure (trout, salmon), river bait (pike). | Fishing spot movement (250-530 ticks), cage/harpoon spots (lobster, tuna, swordfish, shark), big net, monkfish, karambwan, barbarian fishing, anglerfish, dark crab, infernal eel, minnows, aerial fishing, Tempoross, angler outfit, clue bottles, Heron pet. |
| `[~]` | Cooking | `content/skills/cooking` | `CookingScriptTest (11)` | Cooking on fires and ranges (the Lumbridge Castle range is blocked because Cook's Assistant cannot be completed) for shrimps, anchovies, sardine, herring, trout, pike, salmon, beef/rat/bear meat and chicken, with wiki burn charts. | Every other fish (lobster up to anglerfish, karambwan), bread, pies, pizzas, stews, cakes, wine, brewing, gnome cooking, sinew, cooking gauntlets/cape, Hosidius range bonus, combination food. |
| `[~]` | Firemaking | `content/skills/firemaking` | `FiremakingTest (2)` | Tinderbox on logs (all 12 log types incl. blisterwood, redwood), light from ground, step-off direction, ashes. | Firelighters/coloured logs, adding logs to existing fires (bonfires), lanterns, pyre logs/barbarian firemaking, Wintertodt, Forestry campfires, fire duration verification. |
| `[~]` | Woodcutting | `content/skills/woodcutting` | `WoodcuttingScriptTest (5)`, `WoodcuttingConfigTest (2)` | All standard trees by cache data (regular to redwood, hollow), axe success rates, lumberjack outfit, canoe station shaping (`content/travel/canoe`). | Bird nests, axe effects/charges, Woodcutting Guild +7 invisible boost, group chopping boost (+1 per player, cap +10), Forestry events, Woodcutting cape, Beaver pet. |
| `[~]` | Agility | `content/skills/agility` | `AgilityShortcutsTest (5)`, `ShortcutDataTest (3)` | 65 shortcuts (Misthalin/Asgarnia and many others) with wiki levels/XP and forced movement; graceful run restore. | **Every Agility course** (Gnome Stronghold, Draynor and all rooftops, Barbarian, Wilderness, Ape Atoll, Werewolf, Priff), marks of grace, Agility Pyramid, Brimhaven Arena, Hallowed Sepulchre, Penguin course, most remaining shortcuts. |
| `[ ]` | Herblore | none (potions are drinkable in `content/other/consumables`) | none | Drinking attack/strength/defence/combat/ranging/magic/prayer/restore/super/energy potions and Saradomin brews. | Everything to train it: cleaning herbs, unfinished potions, secondaries, potion making, decanting (Bob Barter), swamp tar, barbarian mixes, Mixology, herb sack, antipoison/antifire/stamina/divine potions as consumables. |
| `[~]` | Thieving | `content/skills/thieving` | `PickpocketingTest (3)` | Pickpocketing man, woman, farmer and Sigmund (wiki chances, damage, 8-tick stun). | Coin pouches (OSRS gives pouches since 2019; here coins go straight to the inventory, verify), every other pickpocket NPC, stalls, chests, doors, blackjacking, Rogues' Den, Pyramid Plunder, Thieving cape, rogue outfit, dodgy necklace. |
| `[~]` | Fletching | `content/skills/fletching` | `FletchingScriptTest (9)` | Knife on logs (shafts, javelin shafts, unstrung bows, crossbow stocks), stringing bows/crossbows, crossbow assembly, headless and tiered arrows, bolts and gem bolt tips (incl. dragon), darts, javelins. | Broad arrows/bolts (Slayer unlock), ogre and brutal arrows, amethyst ammo audit, Fletching cape. Mostly complete; needs an item-by-item wiki check. |
| `[ ]` | Slayer | none | none | Combat formula hooks only. | Masters, assignments, task tracking, points, rewards/unlocks, superiors, Slayer helm, slayer-only monsters and requirements. |
| `[ ]` | Farming | none (`content/areas/city/lumbridge`: Fayeth, Vasquen, Tool Leprechaun dialogue only) | none | Farmer dialogue; Tool Leprechaun talk. | Patches and growth ticks, seeds, compost, protection payments, tool storage, Tithe Farm, Farming Guild, herb/tree/fruit/hops/allotment/special patches. |
| `[ ]` | Construction | none | none | — | POH, building, furniture, servants, portals, altars (gilded), Mahogany Homes. |
| `[ ]` | Hunter | none (`api/hunt` is NPC aggression "hunt", not the skill) | none | — | Birds, butterflies (net), implings, box traps/chinchompas, salamanders, kebbits/falconry, pitfall traps, Herbiboar, Hunter Guild/rumours, birdhouses. |
| `[-]` | Sailing | none | none | — | Out of scope for the current rev-233 baseline; recheck when the target revision is decided (#26). |

## Items, equipment and economy

| Status | Slice | Modules | Integration tests | Known deviations / gaps |
|---|---|---|---|---|
| `[~]` | Item definitions, examine, weight, value | cache + `api/cache-enricher` (objs.toml examines), `api/inv-weight` | none directly | Market price = store value (`DefaultMarketPrices` returns `cost`), so death ranking and guide prices are not GE prices. |
| `[~]` | Equipment: slots, requirements, bonuses, two-handed, ammo | `content/interfaces/equipment`, `api/player/bonus` (`WornBonuses`), `api/weapons` | `HeldEquipOpTest (11)`, `WornUnequipOpTest (5)` | Equipment stats, Items Kept on Death and Guide Prices interfaces exist (untested). Blowpipe/quiver/ToA bonus TODOs. Followers (pets) not available. |
| `[~]` | Charged/degradable gear | `api/obj-charges` | none | Charges framework exists (ring of forging, Tumeken's shadow, jewellery). No degradation (Barrows, crystal) and no recoil. |
| `[~]` | Consumables: food, potions | `content/other/consumables` | `ConsumablesScriptTest (7)` | See Hitpoints/Herblore rows. No antipoison/antifire/stamina/divine/combat-boost variety. |
| `[~]` | Ground items, ownership, despawn | `api/obj-plugin`, `api/repo`, `engine/game/obj` | none dedicated | Ironman ownership checks exist; despawn timer unverified (`ObjRepository.kt` TODO). |
| `[~]` | Shops | `api/shops`, `content/areas/city/lumbridge` (general store, Bob's axes, gardener, Perdu) | `StandardGpShopOperationsBuyTest (7)` (api/shops); unit `api/shops` 2 | Buy/sell/restock framework exists. Only Lumbridge shops are scripted; selling is not integration-tested. No specialty currencies (tokkul, marks, points). |
| `[x]` | Bank (booths, chests, deposit box, tabs, placeholders, settings, tutorial) | `content/interfaces/bank`, `api/invtx` | `BankInvScriptTest (18)`, `BankInvScriptQuirksTest (17)`, `BankLocsScriptTest (3)`, `BankTutorialScriptTest (2)`; unit 4 | Bank PIN missing; one message flagged possibly wrong (`BankInvScript.kt:279`). Bank title is the RuneRealm branding. |
| `[~]` | Trading | `content/interfaces/trade` | `TradeScriptTest (12)` | Ironman block present. No anti-dup/stress test, no trade-limit / wealth-warning audit. |
| `[ ]` | Grand Exchange | none (`api/market` only exposes store value) | none | No offers, matching, tax, limits, clerks or collection box. |
| `[~]` | Drop tables | `api/drop-table`, `content/other/npc-drops` | `LumbridgeNpcDropsTest (5)`, `NpcDeathDropsTest (2)`; unit 5 | Lumbridge only (see Combat). |

## World and geography

| Status | Slice | Modules | Integration tests | Known deviations / gaps |
|---|---|---|---|---|
| `[~]` | Lumbridge NPC coverage | `content/areas/city/lumbridge` (47 npc scripts, spawns in `npcs.toml`), `content/generic/generic-npcs` (banker, cows, ducks, sheep, generic people), `content/other/ironman` (Adam) | `LumbridgeNpcOpsTest (1)` (every visible op of every spawned npc has a handler, Attack excluded), `LumbridgeNpcRightClickTest (6)`, `LumbridgeNpcTalkTest (1)` | Dialogue for every Lumbridge NPC including tutors, Duke, Cook, Hans, Doomsayer, Veos, Perdu, Lost City adventurers and farmers. Quest-dependent branches are mostly stubbed (Cook's Assistant, Sheep Shearer, X Marks the Spot, Cold War, Lost Tribe). TODOs: world map panning from tutors, achievement diary overlay (Hatius), Perdu lost-item tracking, tool leprechaun storage, farmer payments, Hans account age. |
| `[~]` | Stairs, ladders, doors, gates | `content/generic/generic-locs`, `content/other/windmill` | `SpiralStaircaseScriptTest (4)` | Lumbridge Castle stack verified by tests; other staircases/ladders/doors only where their loc is in a content group (see Interactions). No world-wide door/stair audit. |
| `[~]` | Agility shortcuts | `content/skills/agility` | `AgilityShortcutsTest (5)`, `ShortcutDataTest (3)` | 65 shortcuts; endpoints derived from map data, not client-verified. |
| `[~]` | Teleports | `content/skills/magic/teleports`, `api/death-plugin` (Death's Office) | `HomeTeleportTest (7)`, `ItemTeleportsTest (7)` | Home Teleport (no cooldown, see differences), glory/eternal glory, ring of dueling, games necklace, combat bracelet, skills necklace, ring of wealth, standard/ancient/NMZ tablets. Missing: spellbook teleports, fairy rings, spirit trees, gliders, charter ships, boats, minecarts, canoes are present (`content/travel/canoe`, `CanoeConfigTest (3)`). |
| `[~]` | Wilderness boundaries / multi-combat | `content/areas/wilderness`, `content/areas/misc/multiways`, `api/area-checker` | Wilderness tests (6) | Ferox Enclave bounds approximate. Multiway areas from the cache. |
| `[ ]` | Other cities, dungeons, regions | none | none | No scripted NPCs outside Lumbridge (except the Clan Hub clerk and Death). Cache spawns load world-wide, but ops are unhandled. |
| `[~]` | Music regions | `api/music`, `api/music-plugin` | none | Unlock-on-entry exists; emulation timing TODOs. |

## UI, social and supporting systems

| Status | Slice | Modules | Integration tests | Known deviations / gaps |
|---|---|---|---|---|
| `[~]` | Settings (all settings, audio, display, controls, run mode, Esc closes) | `content/interfaces/settings` | `EscClosesInterfaceTest (1)`, `QuestListSettingsTest (1)`, `SpellbookFilterTest (1)`, `XpDropsTest (1)`, `SettingsCloseTest (2)` | `settings_tracking` varp spam-synced (TODO). Most individual toggles are not tested. |
| `[~]` | World map | `content/interfaces/worldmap` | `WorldMapScriptTest (3)` | Floater, fullscreen, fixed mode, resize. Not client-verified. |
| `[~]` | XP drops | `content/interfaces/settings` (`XpDropsScript`) | `XpDropsTest (1)` | Orb toggle and setup interface. |
| `[~]` | Gameframe, logout tab, journal tab (summary), skill guides, emotes, character creator, fade overlay, map clock | `content/interfaces/*`, `content/other/mapclock` | `EmotesConfigTest (1)`, `EmotesScriptTest (1)`, `CharacterCreatorScriptTest (5)` | Emote unlocks (quest/diary emotes) ungated or missing; skill guides untested. |
| `[~]` | Friends / ignore lists | `content/other/social` | `SocialScriptTest (8)` | 400 friends for everyone. |
| `[~]` | Friends chat (chat-channel) | `content/other/social` | `FriendChatScriptTest (4)`, `ChannelsTabScriptTest (1)` | Join, messages, setup, ranks, kicks. |
| `[~]` | Clans | `content/other/social/clan` | `ClanScriptTest (5)` | Formation via Clan Hub clerk, clan chat, persistence (migration V12), vexillum recruit. Missing: guest clan channels, clan settings/ranks UI beyond basics, clan events, GIM. |
| `[~]` | Ironman | `content/other/ironman`, `api/player/ironman`, checks in trade/obj-take/bank/death | `IronmanTutorTest (6)`, `IronmanStartScriptTest (2)`; `HardcoreIronmanDeathTest (2)` | Mode selection after character creation, downgrades, Adam's armour, trade/loot/UIM-bank restrictions, HCIM death. Missing: Group Ironman, ironman shop-stock rules audit, PvP ironman interactions (`Obj.kt` TODO), ironman-blocked hitmark. |
| `[ ]` | Bank PIN, authenticator UI | none in content | none | Account 2FA exists at login (`api/totp`) but no in-game setup. |

## Quests, tasks and unlocks

| Status | Slice | Evidence | Notes |
|---|---|---|---|
| `[ ]` | Quest framework, quest list, quest points, journal | No quest state/vars or QP anywhere; journal tab shows the summary only | Quest-gated content is currently ungated (see RuneRealm differences). The `cookquest` varp is read (Lumbridge range, Cooking tutor, Donie/Gee) but nothing can advance it. |
| `[ ]` | All quests and miniquests | none | Enumerate in #13. |
| `[ ]` | Achievement Diaries | Hatius Cosaintus dialogue only (TODO: diary overlay) | #24. |
| `[ ]` | Combat Achievements | none | #24. |
| `[ ]` | Collection Log | none | #24. |
| `[ ]` | Music/emote/area/teleport/spell/prayer unlock tracking | music unlocks only | Prayer and emote unlocks are ungated. |

## Minigames, activities and modes

| Status | Slice | Evidence | Notes |
|---|---|---|---|
| `[ ]` | Minigames and activities | none | #21. |
| `[ ]` | Clue scrolls / Treasure Trails | Arthur the Clue Hunter dialogue only | #22. |
| `[ ]` | Random events | none | #22. |
| `[ ]` | Pets / followers | none (`EquipmentTabScript` TODO) | #27. |
| `[-]` | Leagues, Deadman, seasonal and holiday content | Deadman npcs are spawned in Lumbridge from the cache but have no behaviour | Out of scope until #26 decides. |

## Verification

| Status | Slice | Evidence |
|---|---|---|
| `[~]` | Unit tests for shared mechanics | `engine/*` (game 128, objtx 117, map 54, routefinder 39, utils 48, events 12, interact 9, coroutine 9), `api/*` (cache 24, registry 21, random 16, drop-table 5, player 5, spells-runes 3, shops 2, combat-maxhit 2), `server/app` 4; konsist architecture tests in `api/meta`. |
| `[~]` | Integration tests for gameplay | 398 `@Test` methods in 39 modules (counts above). Run with `./gradlew <module>:integration --max-workers=1`. |
| `[ ]` | Golden parity fixtures (XP, drops, dialogue, tick timing) | none |
| `[~]` | Unresolved-reference checks | startup type verifier and identity-hash collision tests only |
| `[ ]` | Client verification | none recorded |
| `[ ]` | Automated in-game test bots | planned, see README "Test bots" |

## RuneRealm-specific intentional differences

These are deliberate and are **not** parity gaps. Do not "fix" them.

| Difference | Where | Notes |
|---|---|---|
| No members/F2P distinction: every player is a member | `Player.members` always true (989f3a9); login tells the client it is on a members world | Members-only checks are removed and must never be added. Friend list is 400 for everyone. |
| No Home Teleport cooldown | `content/skills/magic/teleports/HomeTeleport.kt` ("intentionally has no 30-minute cooldown") | The 10-seconds-out-of-combat and level 20 Wilderness rules still apply. |
| Branding: RuneRealm instead of RuneScape / RS Mod | client cache text rename (a11e6ad, a3e29d2), login screen (0debf22), "The Bank of RuneRealm" bank title (4a4885a), login message migration V9 | Player-facing text must never say RuneScape or RS Mod. Wiki URLs are fine. |
| Quest-gated content is currently ungated | e.g. prayers (Piety, Chivalry, Rigour, Augury, Preserve), shortcuts and areas that need quests | Temporary: there is no quest system. The reverse also happens: the Lumbridge Castle range checks the `cookquest` varp, which nothing can complete, so that range is **unusable** (`Cooking.kt:100`). Gating returns per area once #7 delivers quest state. Track each ungated unlock in its quest issue. |
| Ironman modes chosen in Lumbridge, not on Tutorial Island | `content/other/ironman` | There is no Tutorial Island; free mode switching applies while the account is "fresh". |
| Jewellery/tablet Wilderness limits | `content/skills/magic/teleports` | Follow the wiki (jewellery level 30, tablets level 20); listed here only because they were reviewed together. |

## Known risks / unverified

Values below were taken from memory, estimated, approximated, or explicitly flagged in code as unverified. None of them has been checked in the real client. Each must be re-checked against the live wiki (and the client where behaviour is visible) before its slice is marked `Verified`. The wiki was unreachable from the audit environment, so no item here has been re-confirmed during this audit.

| Area | Value / behaviour | Location | Why it is a risk |
|---|---|---|---|
| Special attacks | Accuracy/max-hit multipliers: whip 1.25/1.0; DLS 1.25/1.25; dragon mace 1.25/1.5; dragon scimitar 1.25/1.0 (+8-tick prayer disable); dragon dagger 1.15/1.15 x2; DWH 1.0/1.5 (30% Defence drain); godswords 2.0 accuracy, 1.1 max hit, AGS x1.25, BGS x1.1; ZGS freeze 32 ticks; dark bow 1.3 (min 5) / 1.5 (min 8, max 48); MSB accuracy 10/7 | `content/other/special-attacks/src/main/kotlin/.../{melee,ranged}/*.kt` | Written from memory without a wiki citation per value. **DLS accuracy 1.25 is suspect**: the wiki describes Cleave as a damage boost only. Whip, DLS, mace, AGS, SGS, dark bow and MSB have no tests. |
| Special attack energy | Costs come from cache params via `SpecialAttackWeapons` | `api/specials` | Only checked by one registration test. |
| Death's Office | Arrival `CoordGrid(0,49,89,38,30)` = (3174, 5726), exit = Lumbridge (3221, 3218); entrance locs near respawn points | `api/death-plugin/.../DeathOfficeScript.kt` | Coordinates from the wiki map marker, not the client; entrances not client-checked. |
| Gravestone | 15 minutes = 1500 ticks at 100 ticks/minute, 1-minute warning | `api/death/.../Gravestones.kt` | Real minutes are 100 ticks; warning timing not verified. |
| Ferox Enclave bounds | x 3123-3155, z 3617-3646 "approximately" | `api/area-checker/.../WildernessArea.kt` | Affects Wilderness level 0 inside Ferox; PvP and teleport rules depend on it. |
| Game cycle order | post-tick ordering "an educated guess" | `api/game-process/.../GameCycle.kt` | Off-by-one timing for `loc_del` and similar. |
| NPC mode | `npc_setmode` behaviour "will need to verify" | `api/game-process/.../NpcModeProcessor.kt` | NPC AI edge cases. |
| Magic accuracy | Assumed intentional quirk in calculator | `api/combat/combat-formulas/.../MagicAccuracyOperations.kt:54` | Magic hit chance may differ. |
| Firemaking | Fire lasts 100-199 ticks; ground logs 200 ticks; ashes 200 ticks; 4-tick light attempts | `content/skills/firemaking/Firemaking.kt` | Durations not cited; wiki only says "unpredictable". |
| Fishing messages | Cast, missing-tool and missing-bait messages; pike catch message | `content/skills/fishing/configs/FishingSpots.kt` (TODOs) | The wiki does not document them. |
| Cooking messages | Success/burn messages for all foods except shrimp; two messages in `Cooking.kt` | `content/skills/cooking` (TODOs) | Not documented on the wiki. |
| Teleport tablets | Lumbridge, Falador, Ardougne, NMZ and Ancient tablets land on "the standard landing tiles", not wiki markers | `content/skills/magic/teleports/TeleportTablets.kt` | Landing tiles may be off by a few tiles. |
| Agility shortcuts | Walkable endpoints derived from map data | `content/skills/agility/Shortcuts.kt` | Not client-verified; animations/timing per shortcut type are generic. |
| Thieving | Coins go straight to the inventory (no coin pouches) | `content/skills/thieving/Pickpocketing.kt` | Likely deviation from current OSRS. |
| Bank | Message "may be incorrect" | `content/interfaces/bank/.../BankInvScript.kt:279` | |
| Dialogue | Barfy Bill mesanims wrong; Hans age uses tracked playtime, not account creation date | `content/areas/city/lumbridge/npcs/{BarfyBill,Hans}.kt` | |
| Tumeken's shadow | "Already fully charged" message | `content/other/special-weapons/.../TumekensShadowCharging.kt` | |
| Market prices | GE price = store value | `api/market/DefaultMarketPrices.kt` | Death keep order, guide prices and Death's fees use the wrong values. |
| Ground items | Despawn duration calculation | `api/repo/.../ObjRepository.kt` | |
| Patrol mode | Multi-level waypoints "an educated guess" (commit history) | `api/game-process` NPC patrol | |

## Next work (prioritized)

Ordered by what unblocks the most parity work. Issue numbers refer to the roadmap backlog.

1. **#1 (this audit) → #11:** keep this ledger current; add the Layer 1 right-click sweep bot (README "Test bots"), generalising `LumbridgeNpcOpsTest` to every npc/loc/obj op in the cache, as the first automated coverage report.
2. **#2 Engine:** reconnection, NPC `OpPlayer6-8`/`ApPlayer6-8` modes, emergency save backup, verify game-cycle ordering.
3. **#3 Runtime:** shared requirement/unlock types and a quest-state primitive (needed before any gating); world-wide door/gate/ladder/stair coverage by loc category instead of hand-listed configs.
4. **#4 Combat:** NPC ranged/magic attacks and per-npc combat definitions; status effects (poison, venom, stun, recoil); bind/snare/entangle and Ancient Magicks (re-use `api/freeze`); verify and test every special attack multiplier; enchanted bolts.
5. **#6 / #12 Skills, highest value first:** Herblore and Runecraft (missing entirely, many dependents), Fishing/Cooking beyond Lumbridge fish, standard spellbook utility and city teleports (Magic), Agility courses, Thieving stalls/NPCs, Crafting tanning; then Slayer (#8), Farming, Hunter, Construction.
6. **#5 / #23 Economy:** real market prices, shops outside Lumbridge, Grand Exchange decision (in scope or excluded), trade anti-dup tests, bank PIN.
7. **#15 / #17 / #10 World:** drop tables and NPC scripts beyond Lumbridge, region by region (Draynor, Varrock, Al Kharid, Falador next); transport networks (#17).
8. **#7 / #13 Quests:** quick fix first: ungate the Lumbridge Castle range (or implement Cook's Assistant) so it is usable. Then the quest framework, then F2P-era quests that gate current content (Cook's Assistant, Sheep Shearer, X Marks the Spot, Restless Ghost, prayer unlock quests).
9. **#18 Spells and prayers:** full spellbook enumeration; prayer unlock gating once quests exist.
10. **#25 PvP/social:** loot keys, multi-combat PvP rules, Ferox bounds; clan feature gaps.
11. **#24, #21, #22, #27:** diaries, Combat Achievements, collection log, minigames, clues, random events, pets, animation/music audit, after the systems above exist.
12. **#8, #19, #20:** bosses, Slayer and raids, after combat definitions and instancing content support.
13. **#26:** decide Sailing and seasonal/temporary mode scope for the target revision.
