package com.notificationbox.app.domain

import com.notificationbox.app.model.NotificationDecision

data class NotificationSample(
    val packageName: String,
    val title: String?,
    val text: String?
)

class NotificationClassifier {
    fun classify(sample: NotificationSample): Pair<NotificationDecision, String> {
        val content = listOfNotNull(sample.title, sample.text).joinToString(" ").lowercase()

        if (content.contains("otp") || content.contains("認証") || content.contains("確認コード") || content.contains("2fa")) {
            return NotificationDecision.KeepNow to "認証コードやセキュリティ通知を即時通過"
        }
        if (content.contains("urgent") || content.contains("緊急") || content.contains("至急") || content.contains("障害")) {
            return NotificationDecision.KeepNow to "緊急性の高い文言を検出"
        }
        if (content.contains("sale") || content.contains("クーポン") || content.contains("広告") || content.contains("キャンペーン")) {
            return NotificationDecision.Ignore to "販促・告知系の可能性が高い"
        }
        if (sample.packageName.contains("messenger", true) || sample.packageName.contains("mail", true)) {
            return NotificationDecision.HoldForDigest to "会話・連絡系は後で要確認に回す"
        }
        return NotificationDecision.HoldForDigest to "既定ルールでダイジェスト対象"
    }
}
