package org.runerealm.content.areas.wilderness

import org.runerealm.map.CoordGrid

/** Wilderness boundaries and levels, ported from the client's `wilderness_level` proc. */
object WildernessArea {
    /** Returns the Wilderness level shown by the client at [coord], or `0` outside it. */
    fun level(coord: CoordGrid): Int {
        val x = coord.x
        val z = coord.z
        val mx = x shr 6
        val mz = z shr 6
        return when {
            mx in 52..54 &&
                mz in 62..64 &&
                !(mx == 53 && mz == 63 && (x and 63) in 21..42 && (z and 63) in 21..42) -> 5
            mx in 46..52 && mz in 55..67 -> (z - 55 * 64) / 8 + 1
            mx == 47 && mz == 158 -> (z - 155 * 64) / 8 - 1
            mx == 51 && mz == 159 -> 35
            mx == 53 && mz == 159 -> 35
            mx == 52 && mz == 161 -> 40
            mx == 27 && mz == 180 -> 21
            mx == 29 && mz == 180 -> 21
            mx == 25 && mz == 180 -> 29
            mx == 52 && mz == 160 -> 33 + ((z and 63) - 6) * 7 / 50
            mx in 46..53 && mz in 155..169 -> (z - 155 * 64) / 8 + 1
            else -> 0
        }
    }

    /**
     * Whether [coord] is inside the Wilderness. Ferox Enclave lies within the Wilderness level
     * boundaries but is a safe zone, so it does not count.
     */
    fun contains(coord: CoordGrid): Boolean = level(coord) > 0 && !inFeroxEnclave(coord)

    private fun inFeroxEnclave(coord: CoordGrid): Boolean =
        coord.level == 0 && coord.x in FEROX_X && coord.z in FEROX_Z

    // Ferox Enclave's walls, approximately (the wiki map centres it on 3137, 3623).
    private val FEROX_X = 3123..3155
    private val FEROX_Z = 3617..3646
}
