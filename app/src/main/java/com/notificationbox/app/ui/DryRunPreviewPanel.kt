package com.notificationbox.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
    var panelHeightPx by remember { mutableIntStateOf(0) }
    val panelHeight = with(LocalDensity.current) { panelHeightPx.toDp() }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = panelHeight)
        ) {
            NotificationBoxScreen(vm)
        }
        DryRunPreviewPanel(
            state = dryRunState,
            onModeSelected = vm::setOrganizationMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { panelHeightPx = it.height }
                .padding(horizontal = 16.dp, vertical = 10.dp)
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
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dry_run_preview_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(
                            R.string.dry_run_preview_active_count_short,
                            preview.activeNotificationCount
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (state.mode == OrganizationMode.DRY_RUN) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "試算の詳細を閉じる" else "試算の詳細を表示"
                        )
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = state.mode == OrganizationMode.OBSERVE_ONLY,
                    onClick = {
                        expanded = false
                        onModeSelected(OrganizationMode.OBSERVE_ONLY)
                    },
                    label = { Text(stringResource(R.string.dry_run_mode_observe)) }
                )
                FilterChip(
                    selected = state.mode == OrganizationMode.DRY_RUN,
                    onClick = { onModeSelected(OrganizationMode.DRY_RUN) },
                    label = { Text(stringResource(R.string.dry_run_mode_plan)) }
                )
            }
            if (state.mode == OrganizationMode.DRY_RUN && expanded) {
                Text(
                    text = stringResource(R.string.dry_run_planned_classification),
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InformationalChip(
                        text = stringResource(
                            R.string.dry_run_count_keep,
                            preview.countsByAction[PlannedAction.KEEP_IN_CURRENT_VIEW] ?: 0
                        )
                    )
                    InformationalChip(
                        text = stringResource(
                            R.string.dry_run_count_digest,
                            preview.countsByAction[PlannedAction.ADD_TO_DIGEST_PREVIEW] ?: 0
                        )
                    )
                    InformationalChip(
                        text = stringResource(
                            R.string.dry_run_count_low,
                            preview.countsByAction[PlannedAction.EXCLUDE_FROM_DIGEST_PREVIEW] ?: 0
                        )
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
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InformationalChip(
                        text = stringResource(
                            R.string.dry_run_source_automatic,
                            statistics.countsByDecisionSource[DecisionSource.Automatic] ?: 0
                        )
                    )
                    InformationalChip(
                        text = stringResource(
                            R.string.dry_run_source_app_rule,
                            statistics.countsByDecisionSource[DecisionSource.AppRule] ?: 0
                        )
                    )
                    InformationalChip(
                        text = stringResource(
                            R.string.dry_run_source_manual,
                            statistics.countsByDecisionSource[DecisionSource.UserOverride] ?: 0
                        )
                    )
                }
                Text(
                    text = stringResource(R.string.dry_run_stats_ephemeral),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = stringResource(R.string.dry_run_no_os_operations_short),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun InformationalChip(text: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text) }
    )
}
