package org.runerealm.content.other.social

import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.friendchat.MessageFriendChannel
import net.rsprot.protocol.game.outgoing.friendchat.UpdateFriendChatChannelFull
import net.rsprot.protocol.game.outgoing.friendchat.UpdateFriendChatChannelFullV2
import net.rsprot.protocol.game.outgoing.friendchat.UpdateFriendChatChannelSingleUser
import org.runerealm.api.player.events.social.FriendChatJoinLeave
import org.runerealm.api.player.events.social.FriendChatMessage
import org.runerealm.api.player.output.mes
import org.runerealm.api.script.onEvent
import org.runerealm.api.script.onPlayerLogout
import org.runerealm.api.server.config.ServerConfig
import org.runerealm.game.entity.Player
import org.runerealm.game.entity.PlayerList
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * Friends chat ("Chat-channel"), following the OSRS Wiki
 * ([Chat-channel](https://oldschool.runescape.wiki/w/Chat-channel)): join a channel by its owner's
 * name, see who else is in it, and talk to everyone in it by starting a line with "/".
 *
 * Every online player owns a channel named after them that anyone may join; channel setup (names
 * and ranks) is not implemented yet.
 */
class FriendChatScript
@Inject
constructor(private val players: PlayerList, private val config: ServerConfig) : PluginScript() {
    private val channels = mutableMapOf<String, Channel>()
    private var messageCounter = 0

    override fun ScriptContext.startup() {
        onEvent<FriendChatJoinLeave> {
            val name = name
            if (name.isNullOrBlank()) player.leaveChannel() else player.joinChannel(name)
        }
        onEvent<FriendChatMessage> { player.sendChannelMessage(text) }
        onPlayerLogout { player.leaveChannel(quiet = true) }
    }

    private fun Player.joinChannel(name: String) {
        leaveChannel(quiet = true)
        mes("Attempting to join chat-channel...")
        val owner = players.firstOrNull { it.displayName.equals(name.trim(), ignoreCase = true) }
        if (owner == null) {
            mes("The channel you tried to join does not exist.")
            return
        }
        val channel =
            channels.getOrPut(owner.displayName.lowercase()) { Channel(owner.displayName) }
        channel.members += this
        client.write(
            UpdateFriendChatChannelFullV2(
                UpdateFriendChatChannelFullV2.JoinUpdate(
                    channel.owner,
                    channel.owner,
                    KICK_RANK_OWNER,
                    channel.members.map { it.entry(channel) },
                )
            )
        )
        val added =
            UpdateFriendChatChannelSingleUser.AddedFriendChatUser(
                displayName,
                config.world,
                rankIn(channel),
                worldName,
            )
        for (member in channel.members) {
            if (member !== this) {
                member.client.write(UpdateFriendChatChannelSingleUser(added))
            }
        }
        mes("Now talking in chat-channel ${channel.owner}")
        mes("To talk, start each line of chat with the / symbol.")
    }

    private fun Player.leaveChannel(quiet: Boolean = false) {
        val channel = channels.values.firstOrNull { this in it.members }
        if (channel == null) {
            if (!quiet) {
                client.write(
                    UpdateFriendChatChannelFullV2(UpdateFriendChatChannelFullV2.LeaveUpdate)
                )
            }
            return
        }
        channel.members -= this
        client.write(UpdateFriendChatChannelFullV2(UpdateFriendChatChannelFullV2.LeaveUpdate))
        val removed =
            UpdateFriendChatChannelSingleUser.RemovedFriendChatUser(displayName, config.world)
        for (member in channel.members) {
            member.client.write(UpdateFriendChatChannelSingleUser(removed))
        }
        if (channel.members.isEmpty()) {
            channels.remove(channel.owner.lowercase())
        }
        if (!quiet) {
            mes("You have left the channel.")
        }
    }

    private fun Player.sendChannelMessage(text: String) {
        val channel = channels.values.firstOrNull { this in it.members }
        if (channel == null) {
            mes("You are not currently in a chat-channel.")
            return
        }
        val message =
            MessageFriendChannel(
                displayName,
                channel.owner,
                config.world,
                ++messageCounter,
                0,
                text,
            )
        for (member in channel.members) {
            member.client.write(message)
        }
    }

    private fun Player.entry(channel: Channel): UpdateFriendChatChannelFull.FriendChatEntry =
        UpdateFriendChatChannelFull.FriendChatEntry(
            displayName,
            config.world,
            rankIn(channel),
            worldName,
        )

    private fun Player.rankIn(channel: Channel): Int =
        if (displayName.equals(channel.owner, ignoreCase = true)) RANK_OWNER else RANK_GUEST

    private val worldName: String
        get() = "World ${config.world}"

    private class Channel(val owner: String) {
        val members = mutableListOf<Player>()
    }

    private companion object {
        const val RANK_GUEST = -1
        const val RANK_OWNER = 7
        const val KICK_RANK_OWNER = 7
    }
}
