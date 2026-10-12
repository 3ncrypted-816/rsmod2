package org.runerealm.content.other.social

import net.rsprot.protocol.game.outgoing.social.UpdateFriendList
import net.rsprot.protocol.game.outgoing.social.UpdateIgnoreList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.runerealm.api.config.refs.components
import org.runerealm.api.config.refs.interfaces
import org.runerealm.api.player.events.social.SocialEvents
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.capture.CaptureClient
import org.runerealm.game.entity.player.SessionStateEvent

class SocialScriptTest {
    @Test
    fun GameTestState.`adding an online friend shows them online`() =
        runGameTest(SocialScript::class) {
            player.displayName = "Alice"
            val bob = registerPlayer()
            bob.displayName = "Bob"
            val client = player.client as CaptureClient
            client.clearOutgoing()

            eventBus.publish(SocialEvents.FriendAdd(player, "bob"))
            assertEquals(listOf("Bob"), player.friendList)
            val update = client.filterIsInstance<UpdateFriendList>().single()
            assertTrue(update.friends.single() is UpdateFriendList.OnlineFriend)

            eventBus.publish(SocialEvents.IgnoreAdd(player, "Bob"))
            assertTrue(player.ignoreList.isEmpty(), "friends cannot also be ignored")

            eventBus.publish(SocialEvents.FriendDel(player, "Bob"))
            assertTrue(player.friendList.isEmpty())

            client.clearOutgoing()
            eventBus.publish(SocialEvents.IgnoreAdd(player, "Bob"))
            assertEquals(listOf("Bob"), player.ignoreList)
            assertTrue(client.filterIsInstance<UpdateIgnoreList>().isNotEmpty())
        }

    @Test
    fun GameTestState.`friends are told when you log in and out`() =
        runGameTest(SocialScript::class) {
            player.displayName = "Alice"
            player.friendList += "Bob"
            val bob = registerPlayer()
            bob.displayName = "Bob"
            val client = player.client as CaptureClient

            client.clearOutgoing()
            eventBus.publish(SessionStateEvent.Logout(bob))
            val offline = client.filterIsInstance<UpdateFriendList>().single().friends.single()
            assertTrue(offline is UpdateFriendList.OfflineFriend)

            client.clearOutgoing()
            eventBus.publish(SessionStateEvent.Login(bob))
            val online = client.filterIsInstance<UpdateFriendList>().single().friends.single()
            assertTrue(online is UpdateFriendList.OnlineFriend)
        }

    @Test
    fun GameTestState.`view ignore list switches the tab and back`() =
        runGameTest(SocialScript::class) {
            player.ifOpenOverlay(interfaces.friends, components.toplevel_target_side9)
            player.ifButton(social_components.view_ignore)
            advance()
            assertTrue(player.ui.containsOverlay(social_interfaces.ignore))

            player.ifButton(social_components.view_friends)
            advance()
            assertTrue(player.ui.containsOverlay(interfaces.friends))
        }
}
