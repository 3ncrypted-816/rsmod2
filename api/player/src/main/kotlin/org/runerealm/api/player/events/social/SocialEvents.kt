package org.runerealm.api.player.events.social

import org.runerealm.events.UnboundEvent
import org.runerealm.game.entity.Player

/** Friend and ignore list requests sent by the client. */
/** Joining (with a channel owner's name) or leaving (null name) a friends chat channel. */
public data class FriendChatJoinLeave(val player: Player, val name: String?) : UnboundEvent

/** A friends chat message: public chat typed with a leading "/". */
public data class FriendChatMessage(val player: Player, val text: String) : UnboundEvent

public sealed class SocialEvents : UnboundEvent {
    public abstract val player: Player
    public abstract val name: String

    public data class FriendAdd(override val player: Player, override val name: String) :
        SocialEvents()

    public data class FriendDel(override val player: Player, override val name: String) :
        SocialEvents()

    public data class IgnoreAdd(override val player: Player, override val name: String) :
        SocialEvents()

    public data class IgnoreDel(override val player: Player, override val name: String) :
        SocialEvents()
}
