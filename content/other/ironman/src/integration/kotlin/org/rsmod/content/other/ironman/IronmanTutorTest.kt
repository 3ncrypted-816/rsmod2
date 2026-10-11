package org.rsmod.content.other.ironman

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.ironman.IronmanMode
import org.rsmod.api.player.ironman.ironmanMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.other.ironman.configs.ironman_components
import org.rsmod.content.other.ironman.configs.ironman_interfaces
import org.rsmod.game.entity.player.SessionStateEvent

class IronmanTutorTest {
    @Test
    fun GameTestState.`fresh account can switch freely between modes`() =
        runGameTest(IronmanTutor::class) {
            player.ifOpenMain(ironman_interfaces.ironman_setup)

            player.ifButton(ironman_components.uim_button)
            advance(ticks = 1)
            assertEquals(IronmanMode.Ultimate, player.ironmanMode)

            player.ifButton(ironman_components.hcim_button)
            advance(ticks = 1)
            assertEquals(IronmanMode.Hardcore, player.ironmanMode)

            player.ifButton(ironman_components.none_button)
            advance(ticks = 1)
            assertEquals(IronmanMode.None, player.ironmanMode)
        }

    @Test
    fun GameTestState.`trained account can only schedule a downgrade`() =
        runGameTest(IronmanTutor::class) {
            val downgrades = IronmanDowngrades(cacheTypes.stats)
            downgrades.setMode(player, IronmanMode.Hardcore)
            player.statMap.setXP(stats.attack, 100)
            assertFalse(downgrades.isFreshAccount(player))
            player.ifOpenMain(ironman_interfaces.ironman_setup)

            // Upgrading is not allowed once the account has left its "Tutorial Island" stage.
            player.ifButton(ironman_components.uim_button)
            advance(ticks = 1)
            assertEquals(IronmanMode.Hardcore, player.ironmanMode)
            assertFalse(downgrades.hasPendingDowngrade(player))

            player.ifButton(ironman_components.im_button)
            advance(ticks = 1)
            assertEquals(IronmanMode.Hardcore, player.ironmanMode)
            assertTrue(downgrades.hasPendingDowngrade(player))
            assertEquals(IronmanDowngrades.SHORT_DAYS, downgrades.daysRemaining(player))

            player.ifButton(ironman_components.downgrade_cancel)
            advance(ticks = 1)
            assertFalse(downgrades.hasPendingDowngrade(player))
        }

    @Test
    fun GameTestState.`due downgrade applies on login`() =
        runGameTest(IronmanTutor::class) {
            val downgrades = IronmanDowngrades(cacheTypes.stats)
            downgrades.setMode(player, IronmanMode.Ultimate)
            val today = downgrades.today()
            downgrades.scheduleDowngrade(player, IronmanMode.Standard, today - 3)
            assertTrue(downgrades.hasPendingDowngrade(player))

            eventBus.publish(SessionStateEvent.Login(player))
            assertEquals(IronmanMode.Ultimate, player.ironmanMode) // Warning period not over yet.

            downgrades.scheduleDowngrade(player, IronmanMode.Standard, today - 7)
            eventBus.publish(SessionStateEvent.Login(player))
            assertEquals(IronmanMode.Standard, player.ironmanMode)
            assertFalse(downgrades.hasPendingDowngrade(player))
        }

    @Test
    fun GameTestState.`long downgrade protection requires 1000 total level`() =
        runGameTest(IronmanTutor::class) {
            val downgrades = IronmanDowngrades(cacheTypes.stats)
            downgrades.setMode(player, IronmanMode.Standard)
            player.ifOpenMain(ironman_interfaces.ironman_setup)
            player.ifButton(ironman_components.downgrade_long)
            advance(ticks = 1)
            assertMessageSent("You need a total level of at least 1,000 to select this option.")
        }
}
