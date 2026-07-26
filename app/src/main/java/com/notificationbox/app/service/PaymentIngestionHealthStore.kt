package com.notificationbox.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PaymentIngestionHealth(
    val parsedEvents: Long = 0,
    val failedEvents: Long = 0
)

interface PaymentIngestionHealthReporter {
    val health: StateFlow<PaymentIngestionHealth>

    fun recordSuccess()

    fun recordFailure()
}

object PaymentIngestionHealthStore : PaymentIngestionHealthReporter {
    private val mutableHealth = MutableStateFlow(PaymentIngestionHealth())

    override val health: StateFlow<PaymentIngestionHealth> = mutableHealth.asStateFlow()

    override fun recordSuccess() {
        mutableHealth.update { current ->
            current.copy(parsedEvents = current.parsedEvents + 1)
        }
    }

    override fun recordFailure() {
        mutableHealth.update { current ->
            current.copy(failedEvents = current.failedEvents + 1)
        }
    }

    internal fun resetForTests() {
        mutableHealth.value = PaymentIngestionHealth()
    }
}
