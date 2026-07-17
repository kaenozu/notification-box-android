package com.notificationbox.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notificationbox.app.ui.summary.NotificationSummaryViewModel
import com.notificationbox.app.ui.summary.NotificationSummaryViewModelFactory
import com.notificationbox.app.ui.theme.NotificationBoxTheme

@Composable
fun NotificationBoxApp(
    notificationFactory: NotificationBoxViewModelFactory,
    summaryFactory: NotificationSummaryViewModelFactory,
    vm: NotificationBoxViewModel = viewModel(factory = notificationFactory),
    summaryViewModel: NotificationSummaryViewModel = viewModel(factory = summaryFactory)
) {
    val settingsRules by vm.settingsRulesState.collectAsStateWithLifecycle()
    val operationMessage by vm.operationMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(operationMessage) {
        operationMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            vm.consumeOperationMessage()
        }
    }

    NotificationBoxTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                val settings = settingsRules.settings
                when {
                    !settings.preferencesLoaded -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    !settings.onboardingCompleted -> {
                        OnboardingScreen(
                            onComplete = vm::completeOnboarding,
                            onContinueWithoutPermission = vm::completeOnboarding
                        )
                    }

                    else -> NotificationHomeScreen(vm, summaryViewModel)
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}
