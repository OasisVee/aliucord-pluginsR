package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI.CommandResult
import com.aliucord.entities.Plugin
import com.aliucord.utils.RxUtils.await
import com.discord.api.commands.ApplicationCommandType
import com.discord.api.guildmember.PatchGuildMemberBody
import com.discord.api.permission.Permission
import com.discord.api.user.PatchUserBody
import com.discord.stores.StoreStream
import com.discord.utilities.permissions.PermissionUtils
import com.discord.utilities.rest.RestAPI
import com.discord.api.message.NullSerializable

@Suppress("unused")
@AliucordPlugin
class SlashNick : Plugin() {
    override fun start(ctx: Context) {
        commands.registerCommand(
            "changename",
            "Change your display name or server nickname.",
            listOf(
                Utils.createCommandOption(
                    ApplicationCommandType.STRING,
                    "type",
                    "Type of name to change (displayname/nickname)",
                    required = true
                ),
                Utils.createCommandOption(
                    ApplicationCommandType.STRING,
                    "name",
                    "New name",
                    required = true
                )
            )
        ) {
            val type = it.getString("type")
            val newName = it.getString("name")
            val me = StoreStream.getUsers().me

            when (type) {
                "displayname" -> {
                    // Create a PatchUserBody with the global_name parameter
                    // Wrap the new name in NullSerializable to satisfy type requirements
                    val (_, err) = RestAPI.api.patchUser(
                        PatchUserBody(
                            NullSerializable.ofNullable(null),
                            NullSerializable.ofNullable(null),
                            NullSerializable.ofNullable(newName),
                            NullSerializable.ofNullable(null)
                        )
                    ).await()

                    if (err != null) {
                        err.printStackTrace()
                        return@registerCommand CommandResult(
                            "Failed to change display name. Check log for more details.",
                            null,
                            false
                        )
                    }
                    
                    CommandResult("Your display name has been changed to **$newName**.", null, false)
                }
                "nickname" -> {
                    if (!it.currentChannel.isGuild())
                        return@registerCommand CommandResult("You can only change nicknames in servers!", null, false)

                    val guild = StoreStream.getGuilds().getGuild(it.currentChannel.guildId)
                    val meMember = StoreStream.getGuilds().getMember(guild.id, me.id)
                    val roles = StoreStream.getGuilds().roles[guild.id]
                    val permissions = PermissionUtils.computeNonThreadPermissions(
                        meMember.userId,
                        guild.id,
                        guild.ownerId,
                        meMember,
                        roles,
                        null
                    )
                    if (!PermissionUtils.can(Permission.CHANGE_NICKNAME, permissions))
                        return@registerCommand CommandResult(
                            "You do not have sufficient permissions to change your nickname.",
                            null,
                            false
                        )

                    if (newName != meMember.nick) {
                        val (_, err) = RestAPI.api.updateMeGuildMember(
                            guild.id,
                            PatchGuildMemberBody(newName ?: me.username, null, null, null, 12)
                        ).await()

                        if (err != null) {
                            err.printStackTrace()
                            return@registerCommand CommandResult(
                                "Failed to change nickname. Check log for more details.",
                                null,
                                false
                            )
                        }
                    }
                    CommandResult("Your nickname on this server has been changed to **$newName**.", null, false)
                }
                else -> CommandResult("Invalid type. Please specify 'displayname' or 'nickname'.", null, false)
            }
        }
    }

    override fun stop(context: Context) {
        commands.unregisterAll()
    }
}
