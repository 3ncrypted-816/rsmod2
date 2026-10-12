package org.runerealm.content.other.social

import net.rsprot.protocol.game.outgoing.friendchat.MessageFriendChannel
import net.rsprot.protocol.game.outgoing.friendchat.UpdateFriendChatChannelFullV2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.runerealm.api.player.events.social.FriendChatJoinLeave
import org.runerealm.api.player.events.social.FriendChatMessage
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.capture.CaptureClient

class FriendChatScriptTest {
    @Test
    fun GameTestState.`join a channel and talk in it`() =
        runGameTest(FriendChatScript::class) {
            player.displayName = "Alice"
            val bob = registerPlayer()
            bob.displayName = "Bob"
            val aliceClient = player.client as CaptureClient
            val bobClient = bob.client as CaptureClient

            eventBus.publish(FriendChatJoinLeave(player, "alice"))
            eventBus.publish(FriendChatJoinLeave(bob, "Alice"))
            assertTrue(bobClient.filterIsInstance<UpdateFriendChatChannelFullV2>().isNotEmpty())

            aliceClient.clearOutgoing()
            bobClient.clearOutgoing()
            eventBus.publish(FriendChatMessage(bob, "hello"))
            assertEquals(
                "hello",
                aliceClient.filterIsInstance<MessageFriendChannel>().single().message,
            )
            assertEquals("Bob", bobClient.filterIsInstance<MessageFriendChannel>().single().sender)

            aliceClient.clearOutgoing()
            eventBus.publish(FriendChatJoinLeave(bob, null))
            eventBus.publish(FriendChatMessage(player, "anyone?"))
            assertTrue(
                bobClient.filterIsInstance<MessageFriendChannel>().none { it.message == "anyone?" }
            )
        }

    @Test
    fun GameTestState.`joining an offline player's channel fails`() =
        runGameTest(FriendChatScript::class) {
            player.displayName = "Alice"
            eventBus.publish(FriendChatJoinLeave(player, "Nobody"))
            val client = player.client as CaptureClient
            assertTrue(client.filterIsInstance<UpdateFriendChatChannelFullV2>().isEmpty())
        }
}
