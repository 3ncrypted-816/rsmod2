package org.runerealm.api.player.events.social

import org.runerealm.events.UnboundEvent
import org.runerealm.game.entity.Player

/** Friend and ignore list requests sent by the client. */
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
