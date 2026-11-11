package org.runerealm.api.slayer.configs

import org.runerealm.api.type.builders.varp.VarpBuilder
import org.runerealm.api.type.refs.dbcol.DbColumnReferences
import org.runerealm.api.type.refs.dbtable.DbTableReferences
import org.runerealm.api.type.refs.param.ParamReferences
import org.runerealm.api.type.refs.varbit.VarBitReferences
import org.runerealm.api.type.refs.varp.VarpReferences
import org.runerealm.game.dbtable.DbColumnCodec
import org.runerealm.game.dbtable.DbListColumn
import org.runerealm.game.dbtable.DbValueColumn
import org.runerealm.game.type.TypeListMap
import org.runerealm.game.type.dbrow.DbRowType
import org.runerealm.game.type.dbtable.DbTableType
import org.runerealm.game.type.literal.CacheVarLiteral
import org.runerealm.game.type.param.ParamType
import org.runerealm.game.type.varbit.VarBitType
import org.runerealm.game.type.varp.VarpType

public typealias slayer_varps = SlayerVarps

public typealias slayer_varbits = SlayerVarBits

/** Cache (client-known) Slayer varps, so the client's Slayer helper shows the current task. */
public object SlayerVarps : VarpReferences() {
    /** The amount of monsters left to kill on the current task. */
    public val count: VarpType = find("slayer_count")

    /** The current task id (`slayer_task:id`), kept after completion as the previous task. */
    public val target: VarpType = find("slayer_target")

    /** The Konar task location (`slayer_area:area_id`), or `0` for no location restriction. */
    public val area: VarpType = find("slayer_area")

    /** The amount the current task started with. */
    public val count_original: VarpType = find("slayer_count_original")

    /** Bitmask of bought Slayer unlocks `0..31` (`slayer_unlock:bit`). */
    public val unlocks: VarpType = find("slayer_rewards_unlocks")

    /** Bitmask of bought Slayer unlocks `32..63` (`slayer_unlock:bit - 32`). */
    public val unlocks1: VarpType = find("slayer_rewards_unlocks1")

    /** Server-side: total Slayer tasks completed (never reset). */
    public val tasks_total: VarpType = find("slayer_tasks_total")
}

internal object SlayerVarpBuilder : VarpBuilder() {
    init {
        build("slayer_tasks_total")
    }
}

public object SlayerVarBits : VarBitReferences() {
    /** The master that assigned the current task (`slayer_master_task:master_id`). */
    public val master: VarBitType = find("slayer_master")

    public val points: VarBitType = find("slayer_points")

    /** The current task streak ("tasks completed in a row"). */
    public val streak: VarBitType = find("slayer_tasks_completed")
}

public object SlayerParams : ParamReferences() {
    /** The npc's Slayer task category, which matches `slayer_task:id` and the `slayer_target`. */
    public val slayer_category: ParamType<Int> = find("slayer_category")
}

public object SlayerDbTables : DbTableReferences() {
    public val slayer_task: DbTableType = find("slayer_task")
    public val slayer_master_task: DbTableType = find("slayer_master_task")
    public val slayer_area: DbTableType = find("slayer_area")
    public val slayer_unlock: DbTableType = find("slayer_unlock")
}

public object SlayerDbColumns : DbColumnReferences() {
    public val task_id: DbValueColumn<Int, Int> = int("slayer_task:id")
    public val task_min_comlevel: DbValueColumn<Int, Int> = int("slayer_task:min_comlevel")
    public val task_stat_reqs: DbListColumn<Int, LevelStat> =
        list("slayer_task:min_stat_requirement_all", LevelStatCodec)
    public val task_name_lowercase: DbValueColumn<String, String> =
        string("slayer_task:name_lowercase")
    public val task_name_uppercase: DbValueColumn<String, String> =
        string("slayer_task:name_uppercase")

    public val master_id: DbValueColumn<Int, Int> = int("slayer_master_task:master_id")
    public val master_task: DbValueColumn<Int, DbRowType> = dbRow("slayer_master_task:task")
    public val master_weight: DbValueColumn<Int, Int> = int("slayer_master_task:weight")
    public val master_min_amount: DbValueColumn<Int, Int> = int("slayer_master_task:min_amount")
    public val master_max_amount: DbValueColumn<Int, Int> = int("slayer_master_task:max_amount")
    public val master_areas: DbListColumn<Int, DbRowType> =
        list("slayer_master_task:areas", DbColumnCodec.DbRowTypeCodec)
    public val master_task_unlock: DbValueColumn<Int, DbRowType> =
        dbRow("slayer_master_task:task_unlock")

    public val area_id: DbValueColumn<Int, Int> = int("slayer_area:area_id")
    public val area_text: DbValueColumn<String, String> = string("slayer_area:area_text")
    public val area_name: DbValueColumn<String, String> = string("slayer_area:area_name_in_helper")

    public val unlock_bit: DbValueColumn<Int, Int> = int("slayer_unlock:bit")
    public val unlock_cost: DbValueColumn<Int, Int> = int("slayer_unlock:cost")
    public val unlock_name: DbValueColumn<String, String> = string("slayer_unlock:name")
}

/** A `(level, stat id)` pair as stored by `slayer_task:min_stat_requirement_all`. */
public data class LevelStat(val level: Int, val stat: Int)

private object LevelStatCodec : DbColumnCodec<Int, LevelStat> {
    override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.INT, CacheVarLiteral.STAT)

    override fun decode(
        iterator: DbColumnCodec.Iterator<Int, LevelStat>,
        types: TypeListMap,
    ): LevelStat {
        val level = iterator.next()
        val stat = iterator.next()
        return LevelStat(level, stat)
    }

    override fun encode(value: LevelStat): List<Int> = listOf(value.level, value.stat)
}
