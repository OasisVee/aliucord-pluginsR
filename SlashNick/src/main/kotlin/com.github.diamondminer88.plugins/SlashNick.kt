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
import com.discord.stores.StoreStream
import com.discord.utilities.permissions.PermissionUtils
import com.discord.utilities.rest.RestAPI

@Suppress("unused")
@AliucordPlugin
class SlashNick : Plugin() {
    override fun start(ctx: Context) {
        val commandName = "SNick"
        
        commands.registerCommand(
            commandName,
            "Change your nickname on this server.",
            listOf(
                Utils.createCommandOption(
                    ApplicationCommandType.STRING,
                    "nickname",
                    "New nickname"
                )
            )
        ) { ctx ->
            if (!ctx.currentChannel.isGuild())
                return@registerCommand CommandResult("You can only change nicknames in servers!", null, false)

            val newNick = ctx.getString("nickname")
            val guild = StoreStream.getGuilds().getGuild(ctx.currentChannel.guildId)
            val me = StoreStream.getUsers().me
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

            if (newNick != meMember.nick) {
                val (_, err) = RestAPI.api.updateMeGuildMember(
                    guild.id,
                    PatchGuildMemberBody(newNick ?: me.username, null, null, null, 12)
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

            val msg =
                if (newNick != null && newNick != meMember.nick)
                    "Your nickname on this server has been changed to **$newNick**."
                else "Your nickname has been reset."
            CommandResult(msg, null, false)
        }
        
        // Register the command in a case-insensitive manner
        commands.registerCommandCaseInsensitive(commandName)
    }

    override fun stop(context: Context) {
        commands.unregisterAll()
    }
}

fun CommandsAPI.registerCommandCaseInsensitive(command: String) {
    val lowerCaseCommand = command.lowercase()
    val upperCaseCommand = command.uppercase()
    val capitalizedCommand = command.capitalize()

    listOf(lowerCaseCommand, upperCaseCommand, capitalizedCommand).forEach { cmd ->
        commands.registerCommand(
            cmd,
            "Change your nickname on this server.",
            listOf(
                Utils.createCommandOption(
                    ApplicationCommandType.STRING,
                    "nickname",
                    "New nickname"
                )
            )
        ) { ctx ->
            if (!ctx.currentChannel.isGuild())
                return@registerCommand CommandResult("You can only change nicknames in servers!", null, false)

            val newNick = ctx.getString("nickname")
            val guild = StoreStream.getGuilds().getGuild(ctx.currentChannel.guildId)
            val me = StoreStream.getUsers().me
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

            if (newNick != meMember.nick) {
                val (_, err) = RestAPI.api.updateMeGuildMember(
                    guild.id,
                    PatchGuildMemberBody(newNick ?: me.username, null, null, null, 12)
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

            val msg =
                if (newNick != null && newNick != meMember.nick)
                    "Your nickname on this server has been changed to **$newNick**."
                else "Your nickname has been reset."
            CommandResult(msg, null, false)
        }
    }
}
