package com.notificationbox.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaymentDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NotificationDatabase::class.java
    )

    @Test
    fun migrate3To4CreatesWritablePaymentEvents() {
        helper.createDatabase(TEST_DATABASE, 3).close()

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            4,
            true,
            NotificationDatabase.MIGRATION_3_4
        ).use { database ->
            database.execSQL(
                """
                INSERT INTO payment_events(
                    sourceNotificationKey,
                    packageName,
                    appLabel,
                    amountYen,
                    merchantName,
                    transactionType,
                    occurredAtMillis,
                    parserId,
                    parserVersion,
                    confidencePercent,
                    status
                ) VALUES(
                    'migration-payment',
                    'jp.ne.paypay.android.app',
                    'PayPay',
                    1280,
                    'Synthetic Store',
                    'PURCHASE',
                    1000,
                    'paypay',
                    1,
                    95,
                    'UNREVIEWED'
                )
                """.trimIndent()
            )

            database.query(
                "SELECT amountYen, transactionType FROM payment_events " +
                    "WHERE sourceNotificationKey = 'migration-payment'"
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(1_280L, cursor.getLong(0))
                assertEquals("PURCHASE", cursor.getString(1))
            }
        }
    }

    companion object {
        private const val TEST_DATABASE = "payment-migration-test"
    }
}
