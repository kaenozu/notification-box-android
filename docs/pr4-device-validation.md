# PR #4 実機検証手順

対象PR: `feat/p2-notification-rules`

この手順は、CIやエミュレータでは確認できないNotificationListener、OS通知の非破壊性、実インストール状態でのRoom v1→v2移行を確認するためのものです。

## 検証情報

| 項目 | 記録 |
| --- | --- |
| 検証日 |  |
| 検証者 |  |
| PR HEAD |  |
| 端末メーカー・機種 |  |
| Androidバージョン |  |
| セキュリティパッチ |  |
| 新規インストール / 上書き |  |

検証開始前にHEADを記録します。

```powershell
git rev-parse HEAD
```

## 1. ビルドとインストール

```powershell
.\gradlew.bat assembleDebug --no-build-cache
adb devices
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

アプリデータを維持する検証では、`adb uninstall`や「ストレージを消去」を実行しないでください。

## 2. 通知アクセス

1. アプリを起動する。
2. 通知アクセス設定を開く。
3. Notification Boxを許可する。
4. アプリへ戻り、アクセス許可済みとして表示されることを確認する。
5. 設定から許可を解除し、未許可状態へ戻ることを確認する。
6. 再度許可する。

確認用コマンド:

```powershell
adb shell settings get secure enabled_notification_listeners
```

期待結果: `com.notificationbox.app`のNotificationListenerコンポーネントが含まれる。

| 確認項目 | 結果 | 備考 |
| --- | --- | --- |
| 許可状態を認識する | NOT RUN |  |
| 解除状態を認識する | NOT RUN |  |
| 再許可後に取り込みを再開する | NOT RUN |  |

## 3. 通知取り込みと分類

テスト用アプリから、識別しやすい通知を複数送信します。

1. 通知が履歴へ表示される。
2. 通知単位で分類を変更できる。
3. 同じ通知を再投稿しても手動分類が保持される。
4. アプリ単位ルールを設定できる。
5. ルール設定後の新しい通知にルールが適用される。
6. 個別の手動分類がアプリルールより優先される。
7. ルールを削除すると自動分類へ戻る。
8. アプリ再起動後も履歴・手動分類・ルール・ピン留めが保持される。

期待する優先順位:

```text
通知単位の手動分類 > アプリ単位ルール > 自動分類
```

| 確認項目 | 結果 | 備考 |
| --- | --- | --- |
| 実通知を履歴へ取り込む | NOT RUN |  |
| 手動分類を設定・解除できる | NOT RUN |  |
| 再投稿後も手動分類を保持する | NOT RUN |  |
| アプリルールを設定・解除できる | NOT RUN |  |
| 優先順位が正しい | NOT RUN |  |
| 再起動後もデータを保持する | NOT RUN |  |

## 4. OS通知の非破壊性

以下の操作前後で、通知シェード上の元通知が残っていることを目視確認します。

- 通知の手動分類
- アプリルールの設定・変更・削除
- ピン留め
- 履歴の削除
- アプリの再起動

このPRでは、OS通知のcancel、snooze、抑制、遅延を行ってはいけません。

| 確認項目 | 結果 | 備考 |
| --- | --- | --- |
| 手動分類でOS通知が消えない | NOT RUN |  |
| ルール変更でOS通知が消えない | NOT RUN |  |
| 履歴削除でOS通知が消えない | NOT RUN |  |
| snoozeや遅延が発生しない | NOT RUN |  |

## 5. 履歴削除とルール削除の分離

1. アプリルールを1件以上登録する。
2. 通知履歴を全削除する。
3. アプリルールが残っていることを確認する。
4. アプリルールを削除する。
5. 通知履歴へ副作用がないことを確認する。

| 確認項目 | 結果 | 備考 |
| --- | --- | --- |
| 履歴全削除後もルールが残る | NOT RUN |  |
| ルール削除が履歴を削除しない | NOT RUN |  |

## 6. logcatへの通知内容漏えい確認

通知タイトルまたは本文に、他と重複しないマーカーを含めます。

```text
NBX_PRIVATE_MARKER_20260712
```

アプリプロセスのログだけを採取します。

```powershell
adb logcat -c

# マーカーを含む通知を送信し、アプリで履歴・分類画面を操作する

$pid = (adb shell pidof com.notificationbox.app).Trim()
adb logcat -d --pid=$pid > .\pr4-notification-box-logcat.txt
Select-String -Path .\pr4-notification-box-logcat.txt -Pattern "NBX_PRIVATE_MARKER_20260712"
```

期待結果: `Select-String`の一致結果が0件。

ログをPRへ添付する場合は、通知タイトル・本文、端末所有者情報、アカウント情報が含まれていないことを確認してください。

| 確認項目 | 結果 | 備考 |
| --- | --- | --- |
| アプリプロセスのlogcatにマーカーがない | NOT RUN |  |

## 7. 実インストール状態でのRoom v1→v2移行

### 7.1 v1版を用意する

`main`のDB v1版を別ディレクトリでビルドします。

```powershell
git fetch origin
git worktree add ..\notification-box-v1 origin/main
Push-Location ..\notification-box-v1
.\gradlew.bat assembleDebug --no-build-cache
Pop-Location
```

### 7.2 v1版にデータを作る

```powershell
adb install -r ..\notification-box-v1\app\build\outputs\apk\debug\app-debug.apk
```

1. 通知アクセスを許可する。
2. 通知履歴を複数件作る。
3. 1件以上をピン留めする。
4. アプリを終了する。

### 7.3 v2版を上書きする

PRブランチでAPKをビルドし、アンインストールせず上書きします。

```powershell
.\gradlew.bat assembleDebug --no-build-cache
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

確認事項:

1. 起動時にクラッシュしない。
2. v1で作成した履歴が残る。
3. ピン留めが残る。
4. 既存通知の`userDecision`は未設定として扱われる。
5. アプリルールを新規作成できる。
6. 分類統計を更新する操作でクラッシュしない。

| 確認項目 | 結果 | 備考 |
| --- | --- | --- |
| v2上書き後に起動できる | NOT RUN |  |
| v1履歴を保持する | NOT RUN |  |
| v1ピン留めを保持する | NOT RUN |  |
| 新規ルールを作成できる | NOT RUN |  |
| 分類統計を更新できる | NOT RUN |  |

## 8. 最終判定

以下をすべて満たした場合のみ、PRをReady for reviewへ移行できます。

- 全必須項目がPASS
- FAIL項目に未解決のものがない
- 実機名とAndroidバージョンが記録されている
- 検証HEADがPRの最新HEADと一致する
- 通知内容を含むログやスクリーンショットを公開していない

| 判定 | 値 |
| --- | --- |
| PASS / FAIL / BLOCKED |  |
| 未解決事項 |  |
| Ready化可否 |  |
