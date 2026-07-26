package com.notificationbox.app.domain.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PayPayNotificationParserTest {
    private val parser = PayPayNotificationParser()

    @Test
    fun `parses purchase amount and labeled merchant`() {
        val result = parser.parse(
            input(
                title = "支払い完了",
                text = "利用先：セブン-イレブン\n1,280円のお支払いが完了しました"
            )
        )

        requireNotNull(result)
        assertEquals(1_280L, result.amountYen)
        assertEquals("セブン-イレブン", result.merchantName)
        assertEquals(PaymentTransactionType.PURCHASE, result.transactionType)
        assertTrue(result.confidencePercent >= 90)
    }

    @Test
    fun `normalizes full width charge amount`() {
        val result = parser.parse(
            input(
                title = "チャージ完了",
                text = "PayPay残高に５，０００円をチャージしました"
            )
        )

        requireNotNull(result)
        assertEquals(5_000L, result.amountYen)
        assertEquals(PaymentTransactionType.CHARGE, result.transactionType)
    }

    @Test
    fun `classifies refund before purchase wording`() {
        val result = parser.parse(
            input(
                title = "返金完了",
                text = "1,280円のお支払いを返金しました"
            )
        )

        requireNotNull(result)
        assertEquals(PaymentTransactionType.REFUND, result.transactionType)
    }

    @Test
    fun `ignores notification without yen amount`() {
        val result = parser.parse(
            input(
                title = "PayPayからのお知らせ",
                text = "新しいお知らせを確認できます"
            )
        )

        assertNull(result)
    }

    @Test
    fun `ignores promotion even when it contains yen amount`() {
        val result = parser.parse(
            input(
                title = "キャンペーンのお知らせ",
                text = "抽選で10,000円相当のポイントが当たる"
            )
        )

        assertNull(result)
    }

    @Test
    fun `ignores ambiguous amount without transaction wording`() {
        val result = parser.parse(
            input(
                title = "残高のお知らせ",
                text = "現在の残高は3,000円です"
            )
        )

        assertNull(result)
    }

    @Test
    fun `registry ignores unsupported package`() {
        val result = PaymentParserRegistry().parse(
            PaymentNotificationInput(
                packageName = "com.example.unrelated",
                appLabel = "Unrelated",
                title = "支払い完了",
                text = "1,280円",
                postTimeMillis = 1L
            )
        )

        assertNull(result)
    }

    private fun input(
        title: String?,
        text: String?
    ) = PaymentNotificationInput(
        packageName = PayPayNotificationParser.PAYPAY_PACKAGE_NAME,
        appLabel = "PayPay",
        title = title,
        text = text,
        postTimeMillis = 1L
    )
}
