package com.notificationbox.app.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.notificationbox.app.ui.theme.NotificationBoxTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun disclosureMustBeViewedBeforeOnboardingCanFinish() {
        var completed = false

        composeRule.setContent {
            NotificationBoxTheme(dynamicColor = false) {
                OnboardingScreen(
                    onComplete = { completed = true },
                    onContinueWithoutPermission = { completed = true }
                )
            }
        }

        composeRule.onNodeWithText("通知データは端末内だけで処理")
            .assertDoesNotExist()
        composeRule.onNodeWithText("内容を理解して通知アクセスを設定")
            .assertDoesNotExist()
        composeRule.onNodeWithText("内容を理解して、今は許可せず使う")
            .assertDoesNotExist()

        composeRule.onNodeWithText("次へ").performClick()
        composeRule.onNodeWithText("通知データは端末内だけで処理")
            .assertIsDisplayed()
        composeRule.onNodeWithText("内容を理解して通知アクセスを設定")
            .assertDoesNotExist()

        composeRule.onNodeWithText("次へ").performClick()
        val onboardingList = composeRule.onNodeWithTag("onboarding_list")

        onboardingList.performScrollToNode(hasText("通知アクセスについて"))
        composeRule.onNodeWithText("通知アクセスについて")
            .assertIsDisplayed()

        onboardingList.performScrollToNode(hasText("明示事項"))
        composeRule.onNodeWithText("明示事項")
            .assertIsDisplayed()

        onboardingList.performScrollToNode(hasText("内容を理解して通知アクセスを設定"))
        composeRule.onNodeWithText("内容を理解して通知アクセスを設定")
            .assertIsDisplayed()

        onboardingList.performScrollToNode(hasText("内容を理解して、今は許可せず使う"))
        composeRule.onNodeWithText("内容を理解して、今は許可せず使う")
            .performClick()

        composeRule.runOnIdle {
            assertTrue("Onboarding completion callback was not invoked", completed)
        }
    }
}
