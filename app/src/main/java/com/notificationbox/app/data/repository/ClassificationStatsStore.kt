package com.notificationbox.app.data.repository

import com.notificationbox.app.data.db.ClassificationStatEntity
import com.notificationbox.app.data.db.ClassificationStatsDao
import com.notificationbox.app.model.ClassificationStats
import com.notificationbox.app.model.NotificationDecision
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Owns classification-statistic keys, mutations, and aggregate mapping. */
internal class ClassificationStatsStore(
    private val dao: ClassificationStatsDao
) {
    fun observe(): Flow<ClassificationStats> = dao.observeAll().map(::toModel)

    suspend fun recordAutomatic(decision: NotificationDecision) {
        dao.increment(AUTOMATIC_TOTAL)
        dao.increment(automaticDecisionKey(decision))
    }

    suspend fun recordUserOverride(
        packageName: String,
        decision: NotificationDecision?
    ) {
        dao.increment(USER_OVERRIDE_CHANGES)
        dao.increment(appChangeKey(packageName))
        decision?.let { dao.increment(selectedDecisionKey(it)) }
    }

    suspend fun recordAppRule(
        packageName: String,
        decision: NotificationDecision?
    ) {
        dao.increment(APP_RULE_CHANGES)
        dao.increment(appChangeKey(packageName))
        decision?.let { dao.increment(selectedDecisionKey(it)) }
    }

    suspend fun reset() {
        dao.clearAll()
    }

    private fun toModel(entities: List<ClassificationStatEntity>): ClassificationStats {
        val counts = entities.associate { it.key to it.count }
        return ClassificationStats(
            automaticallyClassified = counts[AUTOMATIC_TOTAL] ?: 0,
            userOverrideChanges = counts[USER_OVERRIDE_CHANGES] ?: 0,
            appRuleChanges = counts[APP_RULE_CHANGES] ?: 0,
            automaticByDecision = NotificationDecision.entries.associateWith { decision ->
                counts[automaticDecisionKey(decision)] ?: 0
            },
            selectedByDecision = NotificationDecision.entries.associateWith { decision ->
                counts[selectedDecisionKey(decision)] ?: 0
            },
            appChangeCounts = counts
                .filterKeys { it.startsWith(APP_PREFIX) }
                .mapKeys { (key, _) -> key.removePrefix(APP_PREFIX) }
        )
    }

    private companion object {
        const val AUTOMATIC_TOTAL = "automatic.total"
        const val USER_OVERRIDE_CHANGES = "override.total"
        const val APP_RULE_CHANGES = "rule.total"
        const val AUTOMATIC_PREFIX = "automatic.decision."
        const val SELECTED_PREFIX = "selected.decision."
        const val APP_PREFIX = "app."

        fun automaticDecisionKey(decision: NotificationDecision): String =
            AUTOMATIC_PREFIX + decision.name

        fun selectedDecisionKey(decision: NotificationDecision): String =
            SELECTED_PREFIX + decision.name

        fun appChangeKey(packageName: String): String = APP_PREFIX + packageName
    }
}
