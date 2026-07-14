package com.notificationbox.app.model

enum class IngestionErrorCode {
    ACTIVE_SNAPSHOT_FAILED,
    RECORD_MAPPING_FAILED,
    REPOSITORY_OPERATION_FAILED,
    COMMAND_QUEUE_CLOSED
}

data class NotificationIngestionHealth(
    val processedCommands: Long = 0,
    val failedCommands: Long = 0,
    val lastError: IngestionErrorCode? = null
)
