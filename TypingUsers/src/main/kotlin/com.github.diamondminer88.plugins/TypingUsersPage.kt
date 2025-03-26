package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.view.View
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.fragments.SettingsPage
import com.aliucord.wrappers.ChannelWrapper.Companion.name
import com.discord.stores.StoreStream
import com.lytefast.flexinput.R

@SuppressLint("SetTextI18n")
class TypingUsersPage : SettingsPage() {
	override fun onViewBound(view: View?) {
		super.onViewBound(view)

		setActionBarTitle('#' + StoreStream.getChannelsSelected().selectedChannel.name)
		setActionBarSubtitle(null)

		val model = ViewModelProvider(this).get(TypingUsersPageModel::class.java)
		val currentlyTypingAdapter = TypingUsersAdapter(model.currentlyTyping, this.parentFragmentManager)
		val previouslyTypingAdapter = TypingUsersAdapter(model.previouslyTyping, this.parentFragmentManager)

		val ctx = this.requireContext()
		val container = this.linearLayout.apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = FrameLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
		}

		TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply {
			text = "Currently typing"
			isAllCaps = true
			container.addView(this)
		}

		RecyclerView(ctx).apply {
			adapter = currentlyTypingAdapter
			layoutManager = LinearLayoutManager(ctx)
			container.addView(this)
		}

		TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply {
			text = "Previously typing"
			isAllCaps = true
			container.addView(this)
		}

		RecyclerView(ctx).apply {
			adapter = previouslyTypingAdapter
			layoutManager = LinearLayoutManager(ctx)
			container.addView(this)
		}
	}
}
