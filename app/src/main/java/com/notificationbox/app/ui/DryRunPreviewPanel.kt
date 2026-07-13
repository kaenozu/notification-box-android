package com.notificationbox.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notificationbox.app.R
import com.notificationbox.app.domain.dryrun.OrganizationMode
import com.notificationbox.app.domain.dryrun.PlannedAction
import com.notificationbox.app.domain.dryrun.toStatistics
import com.notificationbox.app.model.DecisionSource

@Composable
fun Phase1NotificationBoxScreen(vm: NotificationBoxViewModel) {
    val dryRunState by vm.dryRunState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        NotificationBoxScreen(vm)
        DryRunPreviewPanel(
            state = dryRunState,
            onModeSelected = vm::setOrganizationMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DryRunPreviewPanel(
    state: NotificationBoxViewModel.DryRunState,
    onModeSelected: (OrganizationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val preview = state.preview
    val statistics = remember(preview) { preview.toStatistics() }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.dry_run_preview_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(
                    R.string.dry_run_preview_active_count,
                    preview.activeNotificationCount
                )
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.mode == OrganizationMode.OBSERVE_ONLY,
                    onClick = { onModeSelected(OrganizationMode.OBSERVE_ONLY) },
                    label = { Text(stringResource(R.string.dry_run_mode_observe)) }
                )
                FilterChip(
                    selected = state.mode == OrganizationMode.DRY_RUN,
                    onClick = { onModeSelected(OrganizationMode.DRY_RUN) },
                    label = { Text(stringResource(R.string.dry_run_mode_plan)) }
                )
            }
            if (state.mode == OrganizationMode.DRY_RUN) {
                Text(
                    text = stringResource(R.string.dry_run_planned_classification),
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                stringResource(
                                    R.string.dry_run_count_keep,
                                    preview.countsByAction[PlannedAction.KEEP_IN_CURRENT_VIEW] ?: 0
                                )
                            )
                        }
                    )
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                stringResource(
                                    R.string.dry_run_count_digest,
                                    preview.countsByAction[PlannedAction.ADD_TO_DIGEST_PREVIEW] ?: 0
                                )
                            )
                        }
                    )
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                stringResource(
                                    R.string.dry_run_count_low,
                                    preview.countsByAction[PlannedAction.EXCLUDE_FROM_DIGEST_PREVIEW] ?: 0
                                )
                            )
                        }
                    )
                }
                Text(
                    text = stringResource(
                        R.string.dry_run_session_app_count,
                        statistics.uniqueApplicationCount
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                stringResource(
                                    R.string.dry_run_source_automatic,
                                    statistics.countsByDecisionSource[DecisionSource.Automatic] ?: 0
                                )
                            )
                        }
                    )
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                stringResource(
                                    R.string.dry_run_source_app_rule,
                                    statistics.countsByDecisionSource[DecisionSource.AppRule] ?: 0
                                )
                            )
                        }
                    )
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                stringResource(
                                    R.string.dry_run_source_manual,
                                    statistics.countsByDecisionSource[DecisionSource.UserOverride] ?: 0
                                )
                            )
                        }
                    )
                }
                Text(
                    text = stringResource(R.string.dry_run_stats_ephemeral),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = stringResource(R.string.dry_run_no_os_operations),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
