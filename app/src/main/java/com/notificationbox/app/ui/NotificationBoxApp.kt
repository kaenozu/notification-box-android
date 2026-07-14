package com.notificationbox.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notificationbox.app.ui.theme.NotificationBoxTheme

@Composable
fun NotificationBoxApp(
    factory: NotificationBoxViewModelFactory,
    vm: NotificationBoxViewModel = viewModel(factory = factory)
) {
    val state by vm.state.collectAsStateWithLifecycle()

    NotificationBoxTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when {
                !state.preferencesLoaded -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                !state.onboardingCompleted -> {
                    OnboardingScreen(
                        onComplete = vm::completeOnboarding,
                        onSkip = vm::completeOnboarding
                    )
                }
                else -> Phase1NotificationBoxScreen(vm)
            }
        }
    }
}
