package org.runerealm.api.slayer

import org.runerealm.api.player.vars.intVarBit
import org.runerealm.api.player.vars.intVarp
import org.runerealm.api.slayer.configs.SlayerParams
import org.runerealm.api.slayer.configs.slayer_varbits
import org.runerealm.api.slayer.configs.slayer_varps
import org.runerealm.game.entity.Player
import org.runerealm.game.type.npc.UnpackedNpcType

/** Monsters left on the current task; `0` when the player has no task. */
public var Player.slayerCount: Int by intVarp(slayer_varps.count)

/** The current (or, once [slayerCount] is `0`, the previous) task id. */
public var Player.slayerTarget: Int by intVarp(slayer_varps.target)

/** The Konar task location id, or `0` when the task has no location. */
public var Player.slayerArea: Int by intVarp(slayer_varps.area)

public var Player.slayerCountOriginal: Int by intVarp(slayer_varps.count_original)

public var Player.slayerMaster: Int by intVarBit(slayer_varbits.master)

public var Player.slayerPoints: Int by intVarBit(slayer_varbits.points)

public var Player.slayerStreak: Int by intVarBit(slayer_varbits.streak)

public var Player.slayerTasksTotal: Int by intVarp(slayer_varps.tasks_total)

private var Player.unlockBits0: Int by intVarp(slayer_varps.unlocks)
private var Player.unlockBits1: Int by intVarp(slayer_varps.unlocks1)

public val Player.hasSlayerTask: Boolean
    get() = slayerCount > 0

/** The npc's Slayer task category (cache param `slayer_category`), or `null` if it has none. */
public val UnpackedNpcType.slayerCategory: Int?
    get() = paramOrNull(SlayerParams.slayer_category)?.takeIf { it > 0 }

/**
 * Whether [type] belongs to the player's current Slayer task. This does not check Konar's task
 * location; kill credit checks it separately.
 */
public fun Player.isOnSlayerTask(type: UnpackedNpcType): Boolean {
    if (!hasSlayerTask) {
        return false
    }
    val category = type.slayerCategory ?: return false
    return category == slayerTarget
}

/** Whether the Slayer unlock with `slayer_unlock:bit` [bit] has been bought. */
public fun Player.hasSlayerUnlock(bit: Int): Boolean {
    val bits = if (bit < 32) unlockBits0 else unlockBits1
    return (bits shr (bit % 32)) and 1 == 1
}

public fun Player.setSlayerUnlock(bit: Int, unlocked: Boolean) {
    val mask = 1 shl (bit % 32)
    if (bit < 32) {
        unlockBits0 = if (unlocked) unlockBits0 or mask else unlockBits0 and mask.inv()
    } else {
        unlockBits1 = if (unlocked) unlockBits1 or mask else unlockBits1 and mask.inv()
    }
}
