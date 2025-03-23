package com.github.diamondminer88.plugins

import android.content.Context
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI.CommandResult
import com.aliucord.entities.Plugin
import com.aliucord.utils.DimenUtils
import com.aliucord.utils.RxUtils.await
import com.discord.api.commands.ApplicationCommandType
import com.discord.api.guildmember.PatchGuildMemberBody
import com.discord.api.permission.Permission
import com.discord.stores.StoreStream
import com.discord.utilities.permissions.PermissionUtils
import com.discord.utilities.rest.RestAPI
import com.discord.utilities.analytics.AnalyticSuperProperties
import com.discord.stores.StoreExperiments
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import rx.Observable

@Suppress("unused")
@AliucordPlugin
class SlashNick : Plugin() {
    override fun start(ctx: Context) {
        // Keep the original /nick command for server nicknames
        commands.registerCommand(
            "nick",
            "Change your nickname on this server.",
            listOf(
                Utils.createCommandOption(
                    ApplicationCommandType.STRING,
                    "nickname",
                    "New nickname"
                )
            )
        ) {
            if (!it.currentChannel.isGuild())
                return@registerCommand CommandResult("You can only change nicknames in servers!", null, false)

            val newNick = it.getString("nickname")
            val guild = StoreStream.getGuilds().getGuild(it.currentChannel.guildId)
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
        
        // Add a new /displayname command for global display names
        commands.registerCommand(
            "displayname",
            "Change your global display name across all servers.",
            listOf(
                Utils.createCommandOption(
                    ApplicationCommandType.STRING,
                    "name",
                    "New display name (or empty to reset)"
                )
            )
        ) {
            val newName = it.getString("name")
            
            try {
                // Create a simple JSON request body
                val body = JsonObject()
                body.addProperty("global_name", newName)
                
                // Retrieve the OAuth2 token with identify scope
                val token = StoreStream.getAuthentication().getSession().accessToken
                
                // Make a direct PATCH request to the users/@me endpoint with the OAuth2 token
                val client = OkHttpClient()
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = body.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("https://discord.com/api/v9/users/@me")
                    .patch(requestBody)
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@registerCommand CommandResult(
                        "Failed to change display name. Check log for more details.",
                        null,
                        false
                    )
                }
                
                val msg = if (newName != null && newName.isNotEmpty()) 
                    "Your global display name has been changed to **$newName**." 
                else 
                    "Your global display name has been reset."
                    
                CommandResult(msg, null, false)
            } catch (e: Exception) {
                e.printStackTrace()
                CommandResult("Failed to change display name: ${e.message}", null, false)
            }
        }
    }

    override fun stop(context: Context) {
        commands.unregisterAll()
    }
}
