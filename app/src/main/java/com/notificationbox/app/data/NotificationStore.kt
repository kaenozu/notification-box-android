package com.notificationbox.app.data

import com.notificationbox.app.domain.NotificationClassifier
import com.notificationbox.app.domain.NotificationSample
import com.notificationbox.app.model.AppMode
import com.notificationbox.app.model.AppState
import com.notificationbox.app.model.DigestSchedule
import com.notificationbox.app.model.NotificationDecision
import com.notificationbox.app.model.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

object NotificationStore {
    private val classifier = NotificationClassifier()

    private val _state = MutableStateFlow(
        AppState(
            items = demoItems()
        )
    )

    val state: StateFlow<AppState> = _state.asStateFlow()

    fun grantNotificationAccess(granted: Boolean = true) {
        _state.update { it.copy(notificationAccessGranted = granted) }
        NotificationPreferences.savePermissions(granted, _state.value.postNotificationsGranted)
    }

    fun grantPostNotifications(granted: Boolean = true) {
        _state.update { it.copy(postNotificationsGranted = granted) }
        NotificationPreferences.savePermissions(_state.value.notificationAccessGranted, granted)
    }

    fun setMode(mode: AppMode) {
        _state.update { it.copy(mode = mode) }
        NotificationPreferences.saveMode(mode.name)
    }

    fun setFilter(filter: NotificationDecision?) {
        _state.update { it.copy(selectedFilter = filter) }
    }

    fun pauseSummary(label: String) {
        _state.update { it.copy(pausedUntilText = label) }
        NotificationPreferences.savePausedText(label)
    }

    fun setDigestHours(hours: List<Int>) {
        _state.update { it.copy(digestSchedule = DigestSchedule(hours)) }
        NotificationPreferences.saveDigestHours(hours)
    }

    fun clearAll() {
        _state.update { it.copy(items = emptyList()) }
    }

    fun addNotification(sample: NotificationSample) {
        val (decision, reason) = classifier.classify(sample)
        val item = NotificationItem(
            id = System.currentTimeMillis(),
            packageName = sample.packageName,
            appLabel = sample.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() },
            title = sample.title,
            text = sample.text,
            postTime = Instant.now(),
            category = decision,
            reason = reason
        )
        _state.update { current ->
            current.copy(items = listOf(item) + current.items)
        }
    }

    fun markPinned(id: Long, pinned: Boolean) {
        _state.update { current ->
            current.copy(items = current.items.map { if (it.id == id) it.copy(userPinned = pinned) else it })
        }
    }

    fun deleteItem(id: Long) {
        _state.update { current -> current.copy(items = current.items.filterNot { it.id == id }) }
    }

    fun upsertFromListener(packageName: String, title: String?, text: String?) {
        addNotification(NotificationSample(packageName, title, text))
    }

    fun seedDemoTraffic() {
        listOf(
            NotificationSample("com.google.android.gm", "会議の返信", "今日の15時で大丈夫ですか"),
            NotificationSample("com.bank.app", "認証コード", "確認コード 482913"),
            NotificationSample("com.shop.app", "今だけ50%OFF", "キャンペーン終了間近"),
        ).forEach(::addNotification)
    }

    private fun demoItems(): List<NotificationItem> = listOf(
        NotificationItem(1, "com.bank.app", "Bank", "認証コード", "確認コード 482913", Instant.now(), NotificationDecision.KeepNow, "認証コードやセキュリティ通知を即時通過"),
        NotificationItem(2, "com.shop.app", "Shop", "セールのお知らせ", "今だけ50%OFF", Instant.now(), NotificationDecision.Ignore, "販促・告知系の可能性が高い"),
        NotificationItem(3, "com.chat.app", "Chat", "会議の返信", "今日の15時で大丈夫ですか", Instant.now(), NotificationDecision.HoldForDigest, "会話・連絡系は後で要確認に回す")
    )
}
