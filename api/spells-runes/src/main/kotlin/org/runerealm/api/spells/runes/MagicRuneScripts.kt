package org.runerealm.api.spells.runes

import kotlin.reflect.KClass
import org.runerealm.api.spells.runes.combo.ComboRuneScript
import org.runerealm.api.spells.runes.compact.CompactRuneScript
import org.runerealm.api.spells.runes.fake.FakeRuneScript
import org.runerealm.api.spells.runes.staves.StaffSubstituteScript
import org.runerealm.api.spells.runes.subs.RuneSubstituteScript
import org.runerealm.api.spells.runes.unlimited.UnlimitedRuneScript
import org.runerealm.plugin.scripts.PluginScript

/**
 * The scripts that load the rune repositories used by `MagicRuneManager`. Isolated game tests that
 * cast spells must load these alongside the scripts under test.
 */
public val MAGIC_RUNE_SCRIPTS: List<KClass<out PluginScript>> =
    listOf(
        ComboRuneScript::class,
        CompactRuneScript::class,
        FakeRuneScript::class,
        StaffSubstituteScript::class,
        RuneSubstituteScript::class,
        UnlimitedRuneScript::class,
    )
