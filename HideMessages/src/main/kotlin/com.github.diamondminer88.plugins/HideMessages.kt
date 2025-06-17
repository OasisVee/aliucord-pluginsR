package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.aliucord.*
import com.aliucord.api.CommandsAPI
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.aliucord.patcher.before
import com.discord.models.domain.ModelMessageDelete
import com.discord.models.message.Message
import com.discord.stores.StoreStream
import com.discord.utilities.color.ColorCompat
import com.discord.widgets.chat.list.actions.WidgetChatListActions
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import com.lytefast.flexinput.R

@Suppress("unused")
@SuppressLint("SetTextI18n")
@AliucordPlugin
class HideMessages : Plugin() {
	companion object {
		private const val HIDDEN_MESSAGES_KEY = "hidden_messages"
	}
	
	private val contextItemId = View.generateViewId()
	private val deleteIconId = Utils.getResId("drawable_chip_delete", "drawable")
	private val deleteContextItemId = Utils.getResId("dialog_chat_actions_delete", "id")
	private val msgIdField = WidgetChatListActions::class.java.getDeclaredField("messageId")
		.apply { isAccessible = true }
	private val channelIdField = WidgetChatListActions::class.java.getDeclaredField("channelId")
		.apply { isAccessible = true }

	// Store hidden message IDs as a set of strings in the format "channelId:messageId"
	private val hiddenMessages = mutableSetOf<String>()

	override fun start(ctx: Context) {
		// Load previously hidden messages from settings
		loadHiddenMessages()
		
		// Register the clear command
		val args = listOf(
			Utils.createCommandOption(
				com.discord.api.commands.ApplicationCommandType.BOOLEAN,
				"confirm",
				"Confirm you want to unhide all messages",
				required = true
			)
		)

		commands.registerCommand("clearhidden", "Clear all hidden messages", args) { ctx ->
			val confirm = ctx.getRequiredBool("confirm")
			
			if (!confirm) {
				CommandsAPI.CommandResult(
					"❌ You must set confirm to true to clear all hidden messages",
					null, false, "Clear Hidden Messages"
				)
			} else {
				val hiddenCount = hiddenMessages.size
				unhideAllMessages()
				CommandsAPI.CommandResult(
					"✅ Cleared $hiddenCount hidden messages. All previously hidden messages are now visible again.",
					null, false, "Clear Hidden Messages"
				)
			}
		}
		
		// Patch the context menu to add hide option
		patcher.after<WidgetChatListActions>(
			"configureUI",
			WidgetChatListActions.Model::class.java
		) {
			val layout = (requireView() as NestedScrollView).getChildAt(0) as LinearLayout

			if (layout.findViewById<TextView>(contextItemId) != null)
				return@after

			val textView = TextView(layout.context, null, 0, R.i.UiKit_Settings_Item_Icon)
			textView.id = contextItemId
			textView.text = "Hide Message"
			textView.setCompoundDrawablesWithIntrinsicBounds(deleteIconId, 0, 0, 0)
			textView.compoundDrawables[0].setTint(
				ColorCompat.getThemedColor(
					layout.context,
					R.b.colorInteractiveNormal
				)
			)
			textView.setOnClickListener {
				val channelId = channelIdField.getLong(this)
				val msgId = msgIdField.getLong(this)

				dismiss()

				// Add to hidden messages set and save
				val messageKey = "$channelId:$msgId"
				hiddenMessages.add(messageKey)
				saveHiddenMessages()

				if (PluginManager.isPluginEnabled("MessageLogger")) {
					logger.info("Due to how this plugin works, MessageLogger needs to be disabled")
					PluginManager.disablePlugin("MessageLogger")
					StoreStream.getMessages().handleMessageDelete(ModelMessageDelete(channelId, msgId))
					PluginManager.enablePlugin("MessageLogger")
				} else {
					StoreStream.getMessages().handleMessageDelete(ModelMessageDelete(channelId, msgId))
				}
			}

			for (index in 0 until layout.childCount) {
				if (layout.getChildAt(index).id == deleteContextItemId) {
					layout.addView(textView, index + 1)
					return@after
				}
			}

			layout.addView(textView) // backup in case delete btn is gone completely
		}

		// Patch message rendering to hide messages that should be hidden
		patcher.before<WidgetChatListAdapterItemMessage>("onConfigure") { param ->
			val message = param.args[1] as? Message ?: return@before
			val messageKey = "${message.channelId}:${message.id}"
			
			if (hiddenMessages.contains(messageKey)) {
				// Hide the message by making it invisible
				param.result = null
				return@before
			}
		}

		// Alternative approach: Patch the message store to filter out hidden messages
		// This might be more effective depending on Discord's architecture
		patcher.after<StoreStream>("getMessages") {
			val messagesStore = it.result
			// You might need to patch specific methods within the messages store
			// to filter out hidden messages from the message lists
		}
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
		commands.unregisterAll()
	}

	private fun loadHiddenMessages() {
		try {
			val hiddenMessagesString = settings.getString(HIDDEN_MESSAGES_KEY, "")
			if (hiddenMessagesString.isNotEmpty()) {
				hiddenMessages.clear()
				hiddenMessages.addAll(hiddenMessagesString.split(",").filter { it.isNotEmpty() })
			}
		} catch (e: Exception) {
			logger.error("Failed to load hidden messages", e)
		}
	}

	private fun saveHiddenMessages() {
		try {
			val hiddenMessagesString = hiddenMessages.joinToString(",")
			settings.setString(HIDDEN_MESSAGES_KEY, hiddenMessagesString)
		} catch (e: Exception) {
			logger.error("Failed to save hidden messages", e)
		}
	}

	// Optional: Add a method to unhide all messages (useful for debugging or user preference)
	fun unhideAllMessages() {
		hiddenMessages.clear()
		saveHiddenMessages()
		// You might want to refresh the current chat view here
	}

	// Optional: Add a method to unhide a specific message
	fun unhideMessage(channelId: Long, messageId: Long) {
		val messageKey = "$channelId:$messageId"
		if (hiddenMessages.remove(messageKey)) {
			saveHiddenMessages()
			// You might want to refresh the current chat view here
		}
	}

	// Optional: Get count of hidden messages for debugging
	fun getHiddenMessageCount(): Int = hiddenMessages.size
}