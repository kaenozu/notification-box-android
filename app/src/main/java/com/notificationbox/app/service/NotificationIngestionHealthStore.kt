package com.notificationbox.app.service

import com.notificationbox.app.model.IngestionErrorCode
import com.notificationbox.app.model.NotificationIngestionHealth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface NotificationIngestionHealthReporter {
    val health: StateFlow<NotificationIngestionHealth>
    fun recordSuccess()
    fun recordFailure(code: IngestionErrorCode)
}

object NotificationIngestionHealthStore : NotificationIngestionHealthReporter {
    private val mutableHealth = MutableStateFlow(NotificationIngestionHealth())

    override val health: StateFlow<NotificationIngestionHealth> = mutableHealth.asStateFlow()

    override fun recordSuccess() {
        mutableHealth.update { current ->
            current.copy(processedCommands = current.processedCommands + 1)
        }
    }

    override fun recordFailure(code: IngestionErrorCode) {
        mutableHealth.update { current ->
            current.copy(
                failedCommands = current.failedCommands + 1,
                lastError = code
            )
        }
    }

    internal fun resetForTests() {
        mutableHealth.value = NotificationIngestionHealth()
    }
}
