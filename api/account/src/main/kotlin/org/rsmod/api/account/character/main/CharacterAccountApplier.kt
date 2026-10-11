package org.rsmod.api.account.character.main

import jakarta.inject.Inject
import java.time.LocalDateTime
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.game.entity.Player
import org.rsmod.game.type.mod.ModLevelTypeList
import org.rsmod.map.CoordGrid

public class CharacterAccountApplier
@Inject
constructor(private val modLevelTypes: ModLevelTypeList) :
    CharacterDataStage.Applier<CharacterAccountData> {
    override fun apply(player: Player, data: CharacterAccountData) {
        player.accountId = data.accountId
        player.characterId = data.characterId

        val accountHash = (data.accountId.toLong() shl 32) or data.realm.toLong()
        val userHash = data.loginName.hashCode().toLong()
        player.userId = data.characterId.toLong()
        player.accountHash = accountHash
        player.userHash = userHash

        val uuid = data.characterId.toLong()
        player.uuid = uuid
        player.observerUUID = uuid

        val device = data.knownDevice
        player.lastKnownDevice = device
        player.members = data.members
        player.username = data.loginName
        player.displayName = data.displayName ?: ""
        player.coords = CoordGrid(data.coordX, data.coordZ, data.coordLevel)
        player.runEnergy = data.runEnergy
        player.xpRate = data.xpRate
        player.lastLogin = LocalDateTime.now()
        player.vars.backing.putAll(data.varps)
        player.assignModLevel(data.modLevel)
        data.appearance?.let { player.assignAppearance(it) }
    }

    private fun Player.assignAppearance(data: CharacterAppearanceData) {
        appearance.bodyType = data.bodyType
        appearance.pronoun = data.pronoun
        for ((index, kit) in data.identKit.withIndex()) {
            appearance.setIdentKit(index, kit)
        }
        for ((index, colour) in data.colours.withIndex()) {
            appearance.setColour(index, colour)
        }
    }

    private fun Player.assignModLevel(modLevelName: String?) {
        val match =
            if (modLevelName != null) {
                modLevelTypes.values.firstOrNull { it.internalName == modLevelName }
            } else {
                null
            }
        modLevel = match ?: modLevelTypes.default()
    }
}
