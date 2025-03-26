package com.github.diamondminer88.plugins

import androidx.lifecycle.ViewModel
import com.aliucord.Utils
import com.aliucord.utils.RxUtils.subscribe
import com.discord.stores.StoreStream
import rx.Subscription
import rx.subjects.BehaviorSubject

class TypingUsersPageModel : ViewModel() {
	private var currentChannelSubscription: Subscription? = null
	private var typingSubscription: Subscription? = null

	val currentlyTyping: BehaviorSubject<Set<Long>> = BehaviorSubject.l0(emptySet())
	val previouslyTyping: BehaviorSubject<Set<Long>> = BehaviorSubject.l0(emptySet())

	init {
		currentChannelSubscription = StoreStream.getChannelsSelected().observeId().subscribe {
			typingSubscription?.unsubscribe()
			typingSubscription = StoreStream.getUsersTyping().observeTypingUsers(this).subscribe {
				Utils.mainThread.post {
					previouslyTyping.onNext((previouslyTyping.n0() + currentlyTyping.n0()) - this)
					currentlyTyping.onNext(this)
				}
			}
		}
	}

	override fun onCleared() {
		currentChannelSubscription?.unsubscribe()
		typingSubscription?.unsubscribe()
	}
}
