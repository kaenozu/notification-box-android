package com.notificationbox.app.domain

import com.notificationbox.app.model.NotificationDecision
import java.util.Locale

data class NotificationSample(
    val packageName: String,
    val title: String?,
    val text: String?
)

class NotificationClassifier {
    fun classify(sample: NotificationSample): Pair<NotificationDecision, String> {
        val content = listOfNotNull(sample.title, sample.text)
            .joinToString(" ")
            .lowercase(Locale.ROOT)

        if (
            content.containsAsciiToken("otp") ||
            content.containsAsciiToken("2fa") ||
            AUTHENTICATION_KEYWORDS.any(content::contains)
        ) {
            return NotificationDecision.KeepNow to "認証コードやセキュリティ通知を即時通過"
        }
        if (
            content.containsAsciiToken("urgent") ||
            URGENT_KEYWORDS.any(content::contains)
        ) {
            return NotificationDecision.KeepNow to "緊急性の高い文言を検出"
        }
        if (
            content.containsAsciiToken("sale") ||
            PROMOTION_KEYWORDS.any(content::contains)
        ) {
            return NotificationDecision.Ignore to "販促・告知系の可能性が高い"
        }
        if (MESSAGE_PACKAGE_HINTS.any { hint -> sample.packageName.contains(hint, true) }) {
            return NotificationDecision.HoldForDigest to "会話・連絡系は後で要確認に回す"
        }
        return NotificationDecision.HoldForDigest to "既定ルールでダイジェスト対象"
    }

    private fun String.containsAsciiToken(token: String): Boolean =
        Regex("(?<![a-z0-9])${Regex.escape(token)}(?![a-z0-9])")
            .containsMatchIn(this)

    private companion object {
        val AUTHENTICATION_KEYWORDS = listOf(
            "認証",
            "確認コード",
            "ワンタイムパスワード",
            "ログインコード",
            "セキュリティコード"
        )
        val URGENT_KEYWORDS = listOf(
            "緊急",
            "至急",
            "障害発生",
            "サービス障害",
            "システム障害"
        )
        val PROMOTION_KEYWORDS = listOf(
            "クーポン",
            "広告",
            "キャンペーン",
            "セール",
            "特売",
            "割引",
            "ポイント還元",
            "タイムセール",
            "送料無料"
        )
        val MESSAGE_PACKAGE_HINTS = listOf(
            "messenger",
            "message",
            "mail",
            "chat"
        )
    }
}
