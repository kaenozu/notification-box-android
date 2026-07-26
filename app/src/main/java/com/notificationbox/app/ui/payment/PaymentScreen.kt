package com.notificationbox.app.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notificationbox.app.R
import com.notificationbox.app.data.repository.PaymentEvent
import com.notificationbox.app.data.repository.PaymentSummary
import com.notificationbox.app.domain.payment.PaymentTransactionType
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PaymentRoute(
    viewModel: PaymentViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PaymentScreen(uiState = uiState, modifier = modifier)
}

@Composable
fun PaymentScreen(
    uiState: PaymentUiState,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        PaymentUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        PaymentUiState.Error -> PaymentMessageCard(
            title = stringResource(R.string.payment_error_title),
            message = stringResource(R.string.payment_error_body),
            modifier = modifier.padding(16.dp)
        )

        is PaymentUiState.Content -> PaymentContent(
            events = uiState.events,
            summary = uiState.summary,
            modifier = modifier
        )
    }
}

@Composable
private fun PaymentContent(
    events: List<PaymentEvent>,
    summary: PaymentSummary,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.payment_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.payment_beta_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.payment_monthly_net),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = summary.netSpendYen.asYen(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            R.string.payment_summary_detail,
                            summary.eventCount,
                            summary.needsReviewCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (events.isEmpty()) {
            item {
                PaymentMessageCard(
                    title = stringResource(R.string.payment_empty_title),
                    message = stringResource(R.string.payment_empty_body)
                )
            }
        } else {
            items(
                items = events,
                key = PaymentEvent::sourceNotificationKey
            ) { event ->
                PaymentEventCard(event)
            }
        }
        item {
            Text(
                text = stringResource(R.string.payment_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaymentEventCard(event: PaymentEvent) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.merchantName
                        ?: stringResource(R.string.payment_merchant_unknown),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = event.amountYen.asYen(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = stringResource(
                    R.string.payment_event_detail,
                    event.appLabel,
                    event.transactionType.label(),
                    event.occurredAt.displayTime()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (event.transactionType == PaymentTransactionType.UNKNOWN) {
                Text(
                    text = stringResource(R.string.payment_needs_review),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PaymentTransactionType.label(): String = when (this) {
    PaymentTransactionType.PURCHASE -> stringResource(R.string.payment_type_purchase)
    PaymentTransactionType.REFUND -> stringResource(R.string.payment_type_refund)
    PaymentTransactionType.CHARGE -> stringResource(R.string.payment_type_charge)
    PaymentTransactionType.TRANSFER_OUT -> stringResource(R.string.payment_type_transfer_out)
    PaymentTransactionType.TRANSFER_IN -> stringResource(R.string.payment_type_transfer_in)
    PaymentTransactionType.UNKNOWN -> stringResource(R.string.payment_type_unknown)
}

@Composable
private fun PaymentMessageCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message)
        }
    }
}

private fun Long.asYen(): String =
    NumberFormat.getCurrencyInstance(Locale.JAPAN).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }.format(this)

private fun java.time.Instant.displayTime(): String =
    EVENT_TIME_FORMATTER.withZone(ZoneId.systemDefault()).format(this)

private val EVENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("M/d HH:mm", Locale.getDefault())
