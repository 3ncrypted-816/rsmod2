package org.runerealm.api.slayer

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.runerealm.api.config.refs.params
import org.runerealm.api.config.refs.stats
import org.runerealm.api.slayer.configs.LevelStat
import org.runerealm.api.slayer.configs.SlayerDbColumns
import org.runerealm.api.slayer.configs.SlayerDbTables
import org.runerealm.game.dbtable.DbTableResolver
import org.runerealm.game.type.npc.UnpackedNpcType

/** A Slayer task category, from the cache `slayer_task` table. */
public data class SlayerTask(
    val id: Int,
    val rowId: Int,
    val name: String,
    val displayName: String,
    val minCombatLevel: Int,
    val statRequirements: List<LevelStat>,
) {
    /** The Slayer level needed to be assigned (and to damage) this task's monsters. */
    public val slayerLevel: Int
        get() = statRequirements.firstOrNull { it.stat == stats.slayer.id }?.level ?: 1
}

/** A Konar task location, from the cache `slayer_area` table. */
public data class SlayerArea(val id: Int, val text: String, val name: String)

/** A Slayer reward unlock, from the cache `slayer_unlock` table. */
public data class SlayerUnlock(val rowId: Int, val bit: Int, val cost: Int, val name: String)

/** One entry of a master's task list, from the cache `slayer_master_task` table. */
public data class SlayerAssignment(
    val masterId: Int,
    val task: SlayerTask,
    val weight: Int,
    val minAmount: Int,
    val maxAmount: Int,
    val areas: List<SlayerArea>,
    val unlock: SlayerUnlock?,
)

/**
 * Slayer task data read from the cache db tables (`slayer_task`, `slayer_master_task`,
 * `slayer_area` and `slayer_unlock`), which hold the same task lists, weights and amounts as the
 * [Slayer task](https://oldschool.runescape.wiki/w/Slayer_task) wiki tables.
 */
@Singleton
public class SlayerTasks @Inject constructor(private val dbTables: DbTableResolver) {
    private val data by lazy { load() }

    public fun task(id: Int): SlayerTask? = data.tasks[id]

    public fun area(id: Int): SlayerArea? = data.areas[id]

    public fun unlock(bit: Int): SlayerUnlock? = data.unlocks.values.firstOrNull { it.bit == bit }

    public fun assignments(masterId: Int): List<SlayerAssignment> =
        data.assignments[masterId] ?: emptyList()

    /** The task category of [type], or `null` if it is not a Slayer monster. */
    public fun category(type: UnpackedNpcType): SlayerTask? {
        val id = type.slayerCategory ?: return null
        return task(id)
    }

    /**
     * The Slayer level needed to damage [type]: the npc's `slayer_levelrequire` param when set,
     * the [SlayerLevelOverrides] entry for its name, or its task category's Slayer requirement.
     */
    public fun levelRequirement(type: UnpackedNpcType): Int {
        val param = type.paramOrNull(params.slayer_levelrequire) ?: 0
        if (param > 0) {
            return param
        }
        val override = SlayerLevelOverrides[type.name.lowercase()]
        if (override != null) {
            return override
        }
        return category(type)?.slayerLevel ?: 1
    }

    private fun load(): Data {
        val tasks = mutableMapOf<Int, SlayerTask>()
        val tasksByRow = mutableMapOf<Int, SlayerTask>()
        for (row in dbTables[SlayerDbTables.slayer_task]) {
            val id = row.getOrNull(SlayerDbColumns.task_id) ?: continue
            if (id < 0) {
                // Boss tasks share id `-1` and are picked through `slayer_target_bossid`.
                continue
            }
            val task =
                SlayerTask(
                    id = id,
                    rowId = row.type.id,
                    name = row.getOrNull(SlayerDbColumns.task_name_lowercase) ?: "monsters",
                    displayName = row.getOrNull(SlayerDbColumns.task_name_uppercase) ?: "Monsters",
                    minCombatLevel = row.getOrNull(SlayerDbColumns.task_min_comlevel) ?: 0,
                    statRequirements = row.getOrNull(SlayerDbColumns.task_stat_reqs) ?: emptyList(),
                )
            tasks[id] = task
            tasksByRow[row.type.id] = task
        }

        val areas = mutableMapOf<Int, SlayerArea>()
        val areasByRow = mutableMapOf<Int, SlayerArea>()
        for (row in dbTables[SlayerDbTables.slayer_area]) {
            val id = row.getOrNull(SlayerDbColumns.area_id) ?: continue
            val text = row.getOrNull(SlayerDbColumns.area_text) ?: ""
            val name = row.getOrNull(SlayerDbColumns.area_name) ?: text
            val area = SlayerArea(id, text, name)
            areas[id] = area
            areasByRow[row.type.id] = area
        }

        val unlocks = mutableMapOf<Int, SlayerUnlock>()
        for (row in dbTables[SlayerDbTables.slayer_unlock]) {
            val bit = row.getOrNull(SlayerDbColumns.unlock_bit) ?: continue
            val cost = row.getOrNull(SlayerDbColumns.unlock_cost) ?: 0
            val name = row.getOrNull(SlayerDbColumns.unlock_name) ?: ""
            unlocks[row.type.id] = SlayerUnlock(row.type.id, bit, cost, name)
        }

        val assignments = mutableMapOf<Int, MutableList<SlayerAssignment>>()
        for (row in dbTables[SlayerDbTables.slayer_master_task]) {
            val master = row.getOrNull(SlayerDbColumns.master_id) ?: continue
            val taskRow = row.getOrNull(SlayerDbColumns.master_task) ?: continue
            val task = tasksByRow[taskRow.id] ?: continue
            val assignment =
                SlayerAssignment(
                    masterId = master,
                    task = task,
                    weight = row.getOrNull(SlayerDbColumns.master_weight) ?: 0,
                    minAmount = row.getOrNull(SlayerDbColumns.master_min_amount) ?: 1,
                    maxAmount = row.getOrNull(SlayerDbColumns.master_max_amount) ?: 1,
                    areas =
                        row.getOrNull(SlayerDbColumns.master_areas)?.mapNotNull {
                            areasByRow[it.id]
                        } ?: emptyList(),
                    unlock = row.getOrNull(SlayerDbColumns.master_task_unlock)?.let { unlocks[it.id] },
                )
            assignments.getOrPut(master) { mutableListOf() } += assignment
        }
        return Data(tasks, areas, unlocks, assignments)
    }

    private data class Data(
        val tasks: Map<Int, SlayerTask>,
        val areas: Map<Int, SlayerArea>,
        val unlocks: Map<Int, SlayerUnlock>,
        val assignments: Map<Int, List<SlayerAssignment>>,
    )
}

/**
 * Monsters whose Slayer requirement differs from their task category's, keyed by lowercase npc
 * name ([Slayer#Slayer monsters](https://oldschool.runescape.wiki/w/Slayer)).
 */
private val SlayerLevelOverrides: Map<String, Int> =
    mapOf(
        "sulphur lizard" to 44,
        "spitting wyvern" to 66,
        "taloned wyvern" to 66,
        "long-tailed wyvern" to 66,
        "ancient wyvern" to 66,
    )
