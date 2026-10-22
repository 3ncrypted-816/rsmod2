package org.runerealm.content.generic.npcs.combat

import org.runerealm.api.config.refs.params
import org.runerealm.api.config.refs.projanims
import org.runerealm.api.config.refs.seqs
import org.runerealm.api.config.refs.spotanims
import org.runerealm.api.npc.NpcCombatStyle
import org.runerealm.api.type.editors.npc.NpcEditor
import org.runerealm.api.type.refs.npc.NpcReferences
import org.runerealm.api.type.refs.seq.SeqReferences
import org.runerealm.api.type.refs.spot.SpotanimReferences
import org.runerealm.game.type.npc.NpcType
import org.runerealm.game.type.seq.SeqType
import org.runerealm.game.type.spot.SpotanimType

/**
 * Example npc attack styles, following each npc's OSRS Wiki infobox:
 * - [Dark wizard](https://oldschool.runescape.wiki/w/Dark_wizard): magic, attack speed 4. The level
 *   7 version casts Water Strike (max hit 2), the level 20 version Earth Strike (max hit 6).
 * - [Thrower Troll](https://oldschool.runescape.wiki/w/Thrower_Troll): ranged, attack speed 7,
 *   throws rocks.
 * - [Poison Scorpion](https://oldschool.runescape.wiki/w/Poison_Scorpion) and
 *   [Poison spider](https://oldschool.runescape.wiki/w/Poison_spider): poisonous melee npcs
 *   ("Poisonous: Yes (N)", where N is the starting poison damage).
 *
 * Every edit restates all the values it sets, since removing an edit does not repack the cache.
 */
/** The distance ranged and magic example npcs attack from. */
private const val DISTANCED_ATTACK_RANGE = 7

internal object NpcCombatEdits : NpcEditor() {
    init {
        darkWizard(
            type = combat_npcs.young_dark_wizard,
            maxHit = 2,
            launch = spotanims.waterstrike_casting,
            travel = spotanims.waterstrike_travel,
            impact = spotanims.waterstrike_impact,
        )
        darkWizard(
            type = combat_npcs.bearded_dark_wizard,
            maxHit = 6,
            launch = spotanims.earthstrike_casting,
            travel = spotanims.earthstrike_travel,
            impact = spotanims.earthstrike_impact,
        )

        for (troll in combat_npcs.thrower_trolls) {
            ranged(
                type = troll,
                attackRate = 7,
                anim = combat_seqs.troll_rock_throw,
                launch = null,
                travel = combat_spots.troll_rock_travel,
            )
        }

        poisonous(combat_npcs.poison_scorpion, damage = 3)
        poisonous(combat_npcs.poison_spider_31, damage = 3)
        poisonous(combat_npcs.poison_spider_64, damage = 6)
    }

    private fun darkWizard(
        type: NpcType,
        maxHit: Int,
        launch: SpotanimType,
        travel: SpotanimType,
        impact: SpotanimType,
    ) {
        edit(type) {
            attackRange = DISTANCED_ATTACK_RANGE
            param[params.npc_attack_style] = NpcCombatStyle.Magic.id
            param[params.npc_magic_maxhit] = maxHit
            param[params.attackrate] = 4
            param[params.attack_anim] = seqs.human_caststrike
            param[params.proj_launch] = launch
            param[params.proj_travel] = travel
            param[params.proj_impact] = impact
            param[params.proj_type] = projanims.magic_spell
        }
    }

    private fun ranged(
        type: NpcType,
        attackRate: Int,
        anim: SeqType,
        launch: SpotanimType?,
        travel: SpotanimType,
    ) {
        edit(type) {
            attackRange = DISTANCED_ATTACK_RANGE
            param[params.npc_attack_style] = NpcCombatStyle.Ranged.id
            param[params.attackrate] = attackRate
            param[params.attack_anim] = anim
            if (launch != null) {
                param[params.proj_launch] = launch
            }
            param[params.proj_travel] = travel
            param[params.proj_type] = projanims.thrown
        }
    }

    private fun poisonous(type: NpcType, damage: Int) {
        edit(type) { param[params.npc_poison_damage] = damage }
    }
}

internal object combat_npcs : NpcReferences() {
    val young_dark_wizard = find("young_dark_wizard")
    val bearded_dark_wizard = find("bearded_dark_wizard")
    val thrower_trolls =
        listOf(
            find("death_troll_thrower1"),
            find("death_troll_thrower2"),
            find("death_troll_thrower3"),
            find("death_troll_thrower4"),
            find("death_troll_thrower5"),
        )
    val poison_scorpion = find("poison_scorpion")
    val poison_spider_31 = find("dungeonspider")
    val poison_spider_64 = find("poisonspider")
}

internal object combat_seqs : SeqReferences() {
    val troll_rock_throw = find("troll_rock_throw")
}

internal object combat_spots : SpotanimReferences() {
    val troll_rock_travel = find("troll_rock_travel")
}
