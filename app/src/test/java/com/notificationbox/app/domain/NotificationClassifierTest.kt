package com.notificationbox.app.domain

import com.notificationbox.app.model.NotificationDecision
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationClassifierTest {
    private val classifier = NotificationClassifier()

    @Test
    fun `Japanese sale wording is classified as low priority`() {
        val result = classifier.classify(
            NotificationSample(
                packageName = "com.example.shop",
                title = "週末セール",
                text = "対象商品が20%割引です"
            )
        )

        assertEquals(NotificationDecision.Ignore, result.first)
    }

    @Test
    fun `point promotion is classified as low priority`() {
        val result = classifier.classify(
            NotificationSample(
                packageName = "com.example.shop",
                title = "ポイント還元",
                text = "本日限定キャンペーン"
            )
        )

        assertEquals(NotificationDecision.Ignore, result.first)
    }

    @Test
    fun `authentication takes precedence over promotion wording`() {
        val result = classifier.classify(
            NotificationSample(
                packageName = "com.example.shop",
                title = "キャンペーンへのログインコード",
                text = "確認コード 123456"
            )
        )

        assertEquals(NotificationDecision.KeepNow, result.first)
    }

    @Test
    fun `urgent wording takes precedence over promotion wording`() {
        val result = classifier.classify(
            NotificationSample(
                packageName = "com.example.service",
                title = "緊急: サービス障害",
                text = "復旧キャンペーンではありません"
            )
        )

        assertEquals(NotificationDecision.KeepNow, result.first)
    }

    @Test
    fun `sale substring inside an English word is not treated as promotion`() {
        val result = classifier.classify(
            NotificationSample(
                packageName = "com.example.inventory",
                title = "Wholesale inventory report",
                text = "Daily stock update"
            )
        )

        assertEquals(NotificationDecision.HoldForDigest, result.first)
    }

    @Test
    fun `empty content remains fail open`() {
        val result = classifier.classify(
            NotificationSample(
                packageName = "com.example.app",
                title = null,
                text = null
            )
        )

        assertEquals(NotificationDecision.HoldForDigest, result.first)
    }
}
