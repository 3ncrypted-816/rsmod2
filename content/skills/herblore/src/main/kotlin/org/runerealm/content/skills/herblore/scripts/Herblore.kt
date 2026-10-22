package org.runerealm.content.skills.herblore.scripts

import jakarta.inject.Inject
import org.runerealm.api.config.refs.stats
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.stat.herbloreLvl
import org.runerealm.api.random.GameRandom
import org.runerealm.api.script.onOpHeld1
import org.runerealm.api.script.onOpHeldU
import org.runerealm.api.script.onPlayerQueueWithArgs
import org.runerealm.api.stats.xpmod.XpModifiers
import org.runerealm.content.skills.herblore.configs.HerbRecipe
import org.runerealm.content.skills.herblore.configs.HerbloreRecipes
import org.runerealm.content.skills.herblore.configs.herblore_queues
import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.obj.ObjTypeList
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * Herblore, per the OSRS Wiki ([Herblore](https://oldschool.runescape.wiki/w/Herblore)):
 * - "Clean" on grimy herbs.
 * - Herbs into vials of water (and coconut milk / vials of blood) make unfinished potions.
 * - Secondaries into unfinished potions make 3-dose potions; dose-based upgrades (stamina, extended
 *   antifire, anti-venom, divine potions) keep the dose count.
 * - Herbs with 15 swamp tar (and a pestle and mortar) make herb tar.
 * - A pestle and mortar crushes secondaries, repeating over every carried item.
 * - Potions of the same kind can be combined by using one on another.
 */
class Herblore
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val xpMods: XpModifiers,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (herb in HerbloreRecipes.herbs) {
            onOpHeld1(herb.grimy) { clean(it.slot, herb) }
        }
        for (combo in HerbloreRecipes.combos) {
            onOpHeldU(combo.first, combo.second) { start(combo.recipes) }
        }
        registerDoseCombining()
        onPlayerQueueWithArgs<HerbTask>(herblore_queues.herblore) { make(it.args) }
    }

    private fun ScriptContext.registerDoseCombining() {
        for (series in HerbloreRecipes.potionSeries) {
            val doses = series.doses
            for (i in doses.indices) {
                for (j in i until doses.size) {
                    onOpHeldU(doses[i], doses[j]) {
                        PotionDoses.combine(this, it.firstSlot, it.secondSlot, series)
                    }
                }
            }
        }
    }

    private fun ProtectedAccess.clean(slot: Int, herb: HerbloreRecipes.Herb) {
        if (player.herbloreLvl < herb.level) {
            mes("You need level ${herb.level} Herblore to clean the ${name(herb.grimy)}.")
            return
        }
        if (!invReplaceSlot(inv, slot, 1, herb.clean).success) {
            return
        }
        statAdvance(stats.herblore, herb.xp * xpMods.get(player, stats.herblore))
        spam("You clean the dirt from the ${name(herb.clean)}.")
    }

    private suspend fun ProtectedAccess.start(options: List<HerbRecipe>) {
        val actions = options.map { actionsAvailable(it) }
        val single = options.size == 1
        val auto = single && options.single().auto
        val skipInterface = auto || (single && actions.single() <= 1)
        val (recipe, quantity) =
            if (skipInterface) {
                options.single() to if (auto) Int.MAX_VALUE else 1
            } else {
                val selection =
                    skillMulti(
                        title = MAKE_TITLE,
                        choices = options.map { it.product },
                        maxQuantity = actions.max(),
                    )
                val selected = options.first { it.product.id == selection.obj.id }
                val quantity =
                    if (selection.quantity >= SKILL_MULTI_CAP) Int.MAX_VALUE else selection.quantity
                selected to quantity
            }

        if (!canMake(recipe)) {
            return
        }
        val task = HerbTask(recipe, remaining = quantity)
        if (skipInterface) {
            make(task)
        } else {
            weakQueue(herblore_queues.herblore, FIRST_ACTION_DELAY, task)
        }
    }

    private fun ProtectedAccess.actionsAvailable(recipe: HerbRecipe): Int =
        recipe.inputs.minOf { invTotal(inv, it.obj) / it.count }

    private fun ProtectedAccess.canMake(recipe: HerbRecipe): Boolean {
        if (player.herbloreLvl < recipe.level) {
            val name = name(recipe.product)
            mes("You need a Herblore level of ${recipe.level} to make ${article(name)}$name.")
            return false
        }
        val missingTool = recipe.tools.firstOrNull { it !in inv }
        if (missingTool != null) {
            val name = name(missingTool)
            mes("You need ${article(name)}$name to do that.")
            return false
        }
        val missing = recipe.inputs.firstOrNull { invTotal(inv, it.obj) < it.count }
        if (missing != null) {
            if (missing.count > 1) {
                mes("You need ${missing.count} ${name(missing.obj)} to do that.")
            }
            return false
        }
        return true
    }

    private fun ProtectedAccess.make(task: HerbTask) {
        val recipe = task.recipe
        if (!canMake(recipe)) {
            return
        }
        for (input in recipe.inputs) {
            invDel(inv, input.obj, count = input.count)
        }
        val count =
            if (recipe.productCountMax > recipe.productCount) {
                random.of(recipe.productCount, recipe.productCountMax)
            } else {
                recipe.productCount
            }
        invAdd(inv, recipe.product, count = count)
        recipe.byproduct?.let { invAdd(inv, it, count = recipe.byproductCount) }
        recipe.anim?.let(::anim)
        if (recipe.xp > 0) {
            statAdvance(stats.herblore, recipe.xp * xpMods.get(player, stats.herblore))
        }
        recipe.message?.let {
            val text =
                it.replace("{input}", name(recipe.inputs.first().obj))
                    .replace("{product}", name(recipe.product))
            spam(text)
        }

        val next = task.copy(remaining = task.remaining - 1)
        if (next.remaining <= 0 || actionsAvailable(recipe) <= 0) {
            return
        }
        weakQueue(herblore_queues.herblore, recipe.ticks + 1, next)
    }

    private fun name(obj: ObjType): String = objTypes[obj].name.lowercase()

    private fun article(name: String): String =
        if (name.endsWith("s")) "" else if (name.first() in "aeiou") "an " else "a "

    data class HerbTask(val recipe: HerbRecipe, val remaining: Int)

    private companion object {
        const val MAKE_TITLE = "How many would you like to make?"
        const val SKILL_MULTI_CAP = 28

        /**
         * Weak queues are decremented on the cycle they are queued: the first action is next cycle.
         */
        const val FIRST_ACTION_DELAY = 2
    }
}
