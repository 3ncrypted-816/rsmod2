package org.runerealm.content.other.social

import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.social.FriendListLoaded
import net.rsprot.protocol.game.outgoing.social.UpdateFriendList
import net.rsprot.protocol.game.outgoing.social.UpdateIgnoreList
import org.runerealm.api.config.refs.components
import org.runerealm.api.config.refs.interfaces
import org.runerealm.api.player.events.social.SocialEvents
import org.runerealm.api.player.output.mes
import org.runerealm.api.player.ui.ifOpenOverlay
import org.runerealm.api.script.onEvent
import org.runerealm.api.script.onIfOverlayButton
import org.runerealm.api.script.onPlayerLogin
import org.runerealm.api.script.onPlayerLogout
import org.runerealm.api.server.config.ServerConfig
import org.runerealm.api.type.refs.comp.ComponentReferences
import org.runerealm.api.type.refs.interf.InterfaceReferences
import org.runerealm.events.EventBus
import org.runerealm.game.entity.Player
import org.runerealm.game.entity.PlayerList
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * Friend and ignore lists, following the OSRS Wiki ([Friends
 * List](https://oldschool.runescape.wiki/w/Friends_List),
 * [Ignore List](https://oldschool.runescape.wiki/w/Ignore_List)):
 * - Free players may have 200 friends and members 400; the ignore list holds 400 names.
 * - Friends show as online, with their world, while they are logged in.
 */
class SocialScript
@Inject
constructor(
    private val players: PlayerList,
    private val config: ServerConfig,
    private val eventBus: EventBus,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin {
            player.sendLists()
            player.notifyFriends(online = true)
        }
        onPlayerLogout { player.notifyFriends(online = false) }
        onEvent<SocialEvents.FriendAdd> { player.addFriend(name) }
        onEvent<SocialEvents.FriendDel> { player.removeFriend(name) }
        onEvent<SocialEvents.IgnoreAdd> { player.addIgnore(name) }
        onEvent<SocialEvents.IgnoreDel> { player.removeIgnore(name) }
        onIfOverlayButton(social_components.view_ignore) {
            player.ifOpenOverlay(
                social_interfaces.ignore,
                components.toplevel_target_side9,
                eventBus,
            )
        }
        onIfOverlayButton(social_components.view_friends) {
            player.ifOpenOverlay(interfaces.friends, components.toplevel_target_side9, eventBus)
        }
    }

    private fun Player.sendLists() {
        if (ignoreList.isNotEmpty()) {
            client.write(UpdateIgnoreList(ignoreList.map { ignoreEntry(it, added = false) }))
        }
        if (friendList.isEmpty()) {
            client.write(FriendListLoaded)
        } else {
            client.write(UpdateFriendList(friendList.map { friendEntry(it, added = false) }))
        }
    }

    private fun Player.addFriend(input: String) {
        val name = input.trim()
        if (!isValidName(name)) {
            return
        }
        if (name.equals(displayName, ignoreCase = true)) {
            mes("You can't add yourself to your own friend list.")
            return
        }
        val limit = if (members) MEMBERS_FRIEND_LIMIT else FREE_FRIEND_LIMIT
        if (friendList.size >= limit) {
            mes("Your friend list is full. Max of 200 for free users, and 400 for members.")
            return
        }
        if (friendList.containsName(name)) {
            mes("$name is already on your friend list.")
            return
        }
        if (ignoreList.containsName(name)) {
            mes("Please remove $name from your ignore list first.")
            return
        }
        val stored = onlinePlayer(name)?.displayName ?: name
        friendList += stored
        client.write(UpdateFriendList(listOf(friendEntry(stored, added = true))))
    }

    private fun Player.removeFriend(name: String) {
        friendList.removeAll { it.equals(name.trim(), ignoreCase = true) }
    }

    private fun Player.addIgnore(input: String) {
        val name = input.trim()
        if (!isValidName(name)) {
            return
        }
        if (name.equals(displayName, ignoreCase = true)) {
            mes("You can't add yourself to your own ignore list.")
            return
        }
        if (ignoreList.size >= IGNORE_LIMIT) {
            mes("Your ignore list is full. Max of 400 users.")
            return
        }
        if (ignoreList.containsName(name)) {
            mes("$name is already on your ignore list.")
            return
        }
        if (friendList.containsName(name)) {
            mes("Please remove $name from your friend list first.")
            return
        }
        val stored = onlinePlayer(name)?.displayName ?: name
        ignoreList += stored
        client.write(UpdateIgnoreList(listOf(ignoreEntry(stored, added = true))))
    }

    private fun Player.removeIgnore(name: String) {
        val removed = ignoreList.firstOrNull { it.equals(name.trim(), ignoreCase = true) }
        if (removed != null) {
            ignoreList.remove(removed)
            client.write(UpdateIgnoreList(listOf(UpdateIgnoreList.RemovedIgnoredEntry(removed))))
        }
    }

    /** Tells every online player with this player on their friend list that it logged in/out. */
    private fun Player.notifyFriends(online: Boolean) {
        val name = displayName
        if (name.isBlank()) {
            return
        }
        for (other in players) {
            if (other === this || !other.friendList.containsName(name)) {
                continue
            }
            if (online && ignoreList.containsName(other.displayName)) {
                continue
            }
            val entry =
                if (online) {
                    onlineEntry(name, added = false)
                } else {
                    UpdateFriendList.OfflineFriend(false, name, null, 0, 0, "")
                }
            other.client.write(UpdateFriendList(listOf(entry)))
        }
    }

    private fun Player.friendEntry(name: String, added: Boolean): UpdateFriendList.Friend {
        val target = onlinePlayer(name)
        // A friend who ignores this player appears offline to them.
        if (target != null && !target.ignoreList.containsName(displayName)) {
            return onlineEntry(target.displayName, added)
        }
        return UpdateFriendList.OfflineFriend(added, name, null, 0, 0, "")
    }

    private fun onlineEntry(name: String, added: Boolean): UpdateFriendList.OnlineFriend =
        UpdateFriendList.OnlineFriend(
            added,
            name,
            null,
            config.world,
            0,
            0,
            "",
            "World ${config.world}",
            0,
            0,
        )

    private fun ignoreEntry(name: String, added: Boolean): UpdateIgnoreList.IgnoredPlayer =
        UpdateIgnoreList.AddedIgnoredEntry(name, null, "", added)

    private fun onlinePlayer(name: String): Player? =
        players.firstOrNull { it.displayName.equals(name, ignoreCase = true) }

    private fun List<String>.containsName(name: String): Boolean = any {
        it.equals(name, ignoreCase = true)
    }

    private fun isValidName(name: String): Boolean = name.length in 1..MAX_NAME_LENGTH

    private companion object {
        const val FREE_FRIEND_LIMIT = 200
        const val MEMBERS_FRIEND_LIMIT = 400
        const val IGNORE_LIMIT = 400
        const val MAX_NAME_LENGTH = 12
    }
}

typealias social_interfaces = SocialInterfaces

typealias social_components = SocialComponents

object SocialInterfaces : InterfaceReferences() {
    val ignore = find("ignore")
}

object SocialComponents : ComponentReferences() {
    val view_ignore = find("friends:ignore")
    val view_friends = find("ignore:friends")
}
