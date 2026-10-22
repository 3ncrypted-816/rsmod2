package org.runerealm.content.skills.herblore.configs

import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.seq.SeqType

/** An obj (and amount) consumed by a single [HerbRecipe] action. */
data class Ingredient(val obj: ObjType, val count: Int = 1)

/**
 * A Herblore product.
 *
 * Each action consumes every [inputs] entry once, produces [productCount] of [product] (or a random
 * amount in [productCount]..[productCountMax] when that is set) and grants [xp] experience.
 *
 * @param tools objs that must be carried but are not consumed (e.g., a pestle and mortar).
 * @param byproduct an obj returned for each action (e.g., the empty vial of a potion upgrade).
 * @param auto `true` when the action repeats on every carried input without a make-X prompt (e.g.,
 *   crushing with a pestle and mortar).
 * @param ticks the delay between consecutive actions.
 */
data class HerbRecipe(
    val product: ObjType,
    val inputs: List<Ingredient>,
    val level: Int,
    val xp: Double,
    val productCount: Int = 1,
    val productCountMax: Int = productCount,
    val tools: List<ObjType> = emptyList(),
    val byproduct: ObjType? = null,
    val byproductCount: Int = 1,
    val anim: SeqType? = null,
    val auto: Boolean = false,
    val ticks: Int = 2,
    val message: String? = null,
)

/** A pair of objs that, used on one another, offers [recipes]. */
data class HerbCombo(val first: ObjType, val second: ObjType, val recipes: List<HerbRecipe>)

/** A potion series, highest dose first. */
data class PotionSeries(val name: String, val doses: List<ObjType>) {
    val maxDoses: Int
        get() = doses.size

    /** Returns the obj holding [count] doses (1-based). */
    fun dose(count: Int): ObjType = doses[doses.size - count]

    /** Returns the number of doses held by [obj], or `0` if it is not part of this series. */
    fun dosesOf(obj: ObjType): Int = dosesOf(obj.id)

    /** Returns the number of doses held by the obj [id], or `0` if it is not in this series. */
    fun dosesOf(id: Int): Int {
        val index = doses.indexOfFirst { it.id == id }
        return if (index == -1) 0 else doses.size - index
    }
}
