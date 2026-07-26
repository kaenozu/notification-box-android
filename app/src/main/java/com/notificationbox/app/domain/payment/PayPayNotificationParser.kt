package com.notificationbox.app.domain.payment

import java.text.Normalizer

class PayPayNotificationParser : PaymentNotificationParser {
    override fun supports(packageName: String): Boolean = packageName == PAYPAY_PACKAGE_NAME

    override fun parse(input: PaymentNotificationInput): PaymentParseResult? {
        val normalized = Normalizer.normalize(
            listOfNotNull(input.title, input.text).joinToString("\n"),
            Normalizer.Form.NFKC
        ).trim()
        if (normalized.isBlank()) return null
        if (PROMOTION_KEYWORDS.any(normalized::contains)) return null

        val amount = extractAmount(normalized) ?: return null
        val transactionType = detectTransactionType(normalized)
        if (
            transactionType == PaymentTransactionType.UNKNOWN &&
            GENERIC_TRANSACTION_KEYWORDS.none(normalized::contains)
        ) {
            return null
        }

        val merchant = extractMerchant(normalized)
        val confidence = when {
            transactionType == PaymentTransactionType.UNKNOWN -> 60
            merchant != null -> 95
            else -> 85
        }

        return PaymentParseResult(
            amountYen = amount,
            merchantName = merchant,
            transactionType = transactionType,
            confidencePercent = confidence,
            parserId = PARSER_ID,
            parserVersion = PARSER_VERSION
        )
    }

    private fun extractAmount(content: String): Long? {
        val raw = YEN_SUFFIX_AMOUNT.find(content)?.groupValues?.get(1)
            ?: YEN_PREFIX_AMOUNT.find(content)?.groupValues?.get(1)
            ?: return null
        return raw.replace(",", "").toLongOrNull()
    }

    private fun detectTransactionType(content: String): PaymentTransactionType = when {
        REFUND_KEYWORDS.any(content::contains) -> PaymentTransactionType.REFUND
        CHARGE_KEYWORDS.any(content::contains) -> PaymentTransactionType.CHARGE
        TRANSFER_IN_KEYWORDS.any(content::contains) -> PaymentTransactionType.TRANSFER_IN
        TRANSFER_OUT_KEYWORDS.any(content::contains) -> PaymentTransactionType.TRANSFER_OUT
        PURCHASE_KEYWORDS.any(content::contains) -> PaymentTransactionType.PURCHASE
        else -> PaymentTransactionType.UNKNOWN
    }

    private fun extractMerchant(content: String): String? {
        LABELED_MERCHANT.find(content)?.groupValues?.get(1)?.cleanMerchant()?.let { return it }
        PAYMENT_AT_MERCHANT.find(content)?.groupValues?.get(1)?.cleanMerchant()?.let { return it }
        return null
    }

    private fun String.cleanMerchant(): String? =
        lineSequence()
            .firstOrNull()
            ?.trim()
            ?.trim('「', '」', '『', '』', ' ', ':', '：')
            ?.takeIf { value -> value.length in 1..80 }

    companion object {
        const val PAYPAY_PACKAGE_NAME = "jp.ne.paypay.android.app"
        const val PARSER_ID = "paypay"
        const val PARSER_VERSION = 2

        private val YEN_SUFFIX_AMOUNT = Regex("([0-9][0-9,]*)\\s*円")
        private val YEN_PREFIX_AMOUNT = Regex("[¥￥]\\s*([0-9][0-9,]*)")
        private val LABELED_MERCHANT = Regex(
            "(?:利用先|支払先|店舗|お店)\\s*[:：]\\s*([^\\n]+)"
        )
        private val PAYMENT_AT_MERCHANT = Regex(
            "(?:^|\\n)([^\\n]{1,80}?)で(?:のお支払い|の支払い|決済|\\s*[¥￥]?[0-9])"
        )

        private val PROMOTION_KEYWORDS = listOf(
            "キャンペーン",
            "クーポン",
            "ポイント還元",
            "抽選",
            "当たる"
        )
        private val REFUND_KEYWORDS = listOf("返金", "払い戻し", "取消", "キャンセル")
        private val CHARGE_KEYWORDS = listOf("チャージ", "残高に入金")
        private val TRANSFER_IN_KEYWORDS = listOf("受け取り", "受取", "受領")
        private val TRANSFER_OUT_KEYWORDS = listOf("送金", "送付")
        private val PURCHASE_KEYWORDS = listOf("支払い", "お支払い", "決済")
        private val GENERIC_TRANSACTION_KEYWORDS = listOf("取引完了", "取引を受け付け")
    }
}
