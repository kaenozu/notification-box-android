package com.notificationbox.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val pages = remember {
        listOf(
            OnboardingPage(
                icon = Icons.Filled.Notifications,
                title = "通知を、あとから落ち着いて見直す",
                description =
                    "通知箱は、届いた通知を「優先」「あとで確認」「低優先」に整理し、" +
                        "自分に合うルールを試すためのアプリです。"
            ),
            OnboardingPage(
                icon = Icons.Filled.Security,
                title = "通知データは端末内だけで処理",
                description =
                    "通知の送信元アプリ、タイトル、本文、時刻、分類結果を端末内に保存します。" +
                        "外部APIやクラウドには送信せず、Androidバックアップも無効です。"
            ),
            OnboardingPage(
                icon = Icons.Filled.CheckCircle,
                title = "通知アクセスについて",
                description =
                    "分類履歴を作るため、Androidの通知アクセスを利用して通知内容を読み取ります。" +
                        "現在のバージョンは、元のOS通知を削除・抑制・スヌーズ・遅延しません。"
            )
        )
    }
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages[pageIndex]
    val isLastPage = pageIndex == pages.lastIndex

    Scaffold(
        topBar = { TopAppBar(title = { Text("通知箱へようこそ") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                LinearProgressIndicator(
                    progress = { (pageIndex + 1).toFloat() / pages.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (isLastPage) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "明示事項",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "通知アクセスを許可すると、通知箱は他のアプリから届く通知の" +
                                    "送信元、タイトル、本文、投稿時刻をバックグラウンドで読み取り、" +
                                    "分類履歴として端末内に保存します。保存履歴はアプリ内から削除できます。"
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isLastPage) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onComplete()
                            context.startActivity(notificationListenerSettingsIntent(context))
                        }
                    ) {
                        Text("内容を理解して通知アクセスを設定")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSkip
                    ) {
                        Text("今は許可せずアプリを見る")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (pageIndex > 0) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { pageIndex -= 1 }
                            ) {
                                Text("戻る")
                            }
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { pageIndex += 1 }
                        ) {
                            Text("次へ")
                        }
                    }
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSkip
                    ) {
                        Text("説明を後で確認する")
                    }
                }
            }
        }
    }
}
