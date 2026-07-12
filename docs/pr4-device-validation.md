# PR #4 実機検証手順と実施記録

対象PR: `feat/p2-notification-rules`

この文書は、CIで自動確認できるRoom移行と、物理Android端末でのみ確認できるNotificationListener・OS通知非破壊性・OEM固有挙動を分離して記録します。

## 0. 2026-07-12 実施記録

| 項目 | 結果 |
| --- | --- |
| 自動検証対象HEAD | `7d04a1708f01758c620f43760d10a6629c56b615` |
| v1基準SHA | `b32d2e4835997eada837df1ca1d3b15c760002a7` |
| Android Migration Emulator Run | `29193022007` — PASS |
| Room `MigrationTestHelper` | 1 passed / 0 failed / 0 errors / 0 skipped |
| v1 APK→v2 APK上書き移行 | API 30エミュレータでPASS |
| 物理Android端末 | BLOCKED — 実行環境へUSB端末が公開されていない |
| ADB実機接続 | BLOCKED — 実行環境にADBとUSBデバイスがない |
| Ready化可否 | 不可 |

自動上書き移行では、実際にv1 APKをインストールし、Room v1データベースを配置した後、v2 APKを`adb install -r`で上書きしました。確認結果は次のとおりです。

- v2アプリのコールド起動成功
- DB `user_version = 2`
- v1通知データ保持
- v1ピン留め状態保持
- 既存通知の`userDecision`はNULL
- `app_rules`への書き込み成功
- `classification_stats`への書き込み成功
- migration関連Fatal Exceptionなし

自動検証では合成したテストデータだけを使用しています。実ユーザーの通知タイトル・本文は外部送信していません。

エミュレータでの上書き移行成功は、物理端末・OEM固有のNotificationListener動作、通知シェード上の非破壊性、実通知を用いたlogcat漏えい確認の代替にはなりません。

## 1. 物理端末の検証情報

| 項目 | 記録 |
| --- | --- |
| 検証日 | 2026-07-12（接続試行） |
| 検証者 | GitHub連携実行環境 |
| PR HEAD | BLOCKED — 物理端末未接続 |
| v1基準SHA | `b32d2e4835997eada837df1ca1d3b15c760002a7` |
| 端末メーカー・機種 | BLOCKED — 端末なし |
| Androidバージョン | BLOCKED — 端末なし |
| セキュリティパッチ | BLOCKED — 端末なし |
| 新規インストール / 上書き | 物理端末: BLOCKED / API 30エミュレータ: PASS |

物理端末で再実施するときは、開始前に最新HEADを記録します。

```powershell
git rev-parse HEAD
```

## 2. ビルドとインストール

```powershell
.\gradlew.bat assembleDebug --no-build-cache
adb devices
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

アプリデータを維持する検証では、`adb uninstall`や「ストレージを消去」を実行しないでください。

| 確認項目 | 結果 | 備考 |
| --- | --- | --- |
| 物理端末をADBで認識する | BLOCKED | 実行環境へ物理USB端末が公開されていない |
| v2 APKを物理端末へインストールする | BLOCKED | ADB実機接続なし |

## 3. 通知アクセス

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
| 許可状態を認識する | BLOCKED | 物理端末なし |
| 解除状態を認識する | BLOCKED | 物理端末なし |
| 再許可後に取り込みを再開する | BLOCKED | 物理端末なし |

## 4. 通知取り込みと分類

テスト用アプリから識別可能な通知を複数送信します。再投稿保持の確認では、同じ送信元パッケージ・notification ID・tagの組み合わせで通知を更新してください。

確認内容:

1. 通知が履歴へ表示される。
2. 通知単位で分類を設定・解除できる。
3. 同じ通知を再投稿しても手動分類が保持される。
4. アプリ単位ルールを設定・変更・解除できる。
5. ルール設定後の新しい通知にルールが適用される。
6. 個別の手動分類がアプリルールより優先される。
7. ルールを削除すると自動分類へ戻る。
8. 強制停止・再起動後も履歴・手動分類・ルール・ピン留めが保持される。

```powershell
adb shell am force-stop com.notificationbox.app
```

期待する優先順位:

```text
通知単位の手動分類 > アプリ単位ルール > 自動分類
```

| 確認項目 | 結果 | 備考 |
| --- | --- | --- |
| 実通知を履歴へ取り込む | BLOCKED | 物理端末・実通知送信元なし |
| 手動分類を設定・解除できる | BLOCKED | 物理端末なし |
| 再投稿後も手動分類を保持する | BLOCKED | 物理端末なし |
| アプリルールを設定・解除できる | BLOCKED | 物理端末なし |
| 優先順位が正しい | BLOCKED | 物理端末なし |
| 強制停止・再起動後もデータを保持する | BLOCKED | 物理端末なし |

## 5. OS通知の非破壊性

以下の操作前後で、通知シェード上の元通知が残っていることを目視確認します。

- 通知の手動分類
- アプリルールの設定・変更・削除
- ピン留め
- 履歴の削除
- アプリの再起動

このPRでは、OS通知のcancel、snooze、抑制、遅延を行ってはいけません。

| 確認項目 | 結果 | 備考 |
| --- | --- | --- |
| 手動分類でOS通知が消えない | BLOCKED | 物理通知シェードを確認できない |
| ルール変更でOS通知が消えない | BLOCKED | 物理通知シェードを確認できない |
| 履歴削除でOS通知が消えない | BLOCKED | 物理通知シェードを確認できない |
| snooze・抑制・遅延が発生しない | BLOCKED | 物理端末での時間経過確認が必要 |

## 6. 履歴削除とルール削除の分離

1. アプリルールを1件以上登録する。
2. 通知履歴を全削除する。
3. アプリルールが残っていることを確認する。
4. アプリルールを削除する。
5. 通知履歴へ副作用がないことを確認する。

| 確認項目 | 結果 | 備考 |
| --- | --- | --- |
| 履歴全削除後もルールが残る | BLOCKED | 物理端末でのUI操作未実施 |
| ルール削除が履歴を削除しない | BLOCKED | 物理端末でのUI操作未実施 |

## 7. logcatへの通知内容漏えい確認

通知タイトルまたは本文に、他と重複しないマーカーを含めます。

```text
NBX_PRIVATE_MARKER_20260712
```

アプリプロセスのログだけを採取します。

```powershell
adb logcat -c

# マーカーを含む通知を送信し、アプリで履歴・分類画面を操作する

$pid = (adb shell pidof com.notificationbox.app).Trim()
if (-not $pid) {
    throw "Notification Box process is not running"
}
adb logcat -d --pid=$pid > .\pr4-notification-box-logcat.txt
Select-String -Path .\pr4-notification-box-logcat.txt -Pattern "NBX_PRIVATE_MARKER_20260712"
```

期待結果: `Select-String`の一致結果が0件。

通知タイトル・本文、端末所有者情報、アカウント情報を含む生ログはPRへ添付しません。マーカー検索結果だけを記録します。

| 確認項目 | 結果 | 備考 |
| --- | --- | --- |
| アプリプロセスのlogcatにマーカーがない | BLOCKED | 実通知と物理端末プロセスが必要 |

## 8. 物理端末でのRoom v1→v2上書き移行

### 8.1 v1版を用意する

```powershell
$v1Baseline = "b32d2e4835997eada837df1ca1d3b15c760002a7"
git fetch origin
git cat-file -e "${v1Baseline}^{commit}"
git worktree add --detach ..\notification-box-v1 $v1Baseline
Push-Location ..\notification-box-v1
git rev-parse HEAD
.\gradlew.bat assembleDebug --no-build-cache
Pop-Location
```

### 8.2 v1版に実データを作る

```powershell
adb install -r ..\notification-box-v1\app\build\outputs\apk\debug\app-debug.apk
```

1. 通知アクセスを許可する。
2. 通知履歴を複数件作る。
3. 1件以上をピン留めする。
4. アプリを終了する。

### 8.3 v2版を上書きする

```powershell
.\gradlew.bat assembleDebug --no-build-cache
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

| 確認項目 | 物理端末 | API 30エミュレータ | 備考 |
| --- | --- | --- | --- |
| v2上書き後に起動できる | BLOCKED | PASS | Run `29193022007` |
| v1履歴を保持する | BLOCKED | PASS | 合成v1データを保持 |
| v1ピン留めを保持する | BLOCKED | PASS | `userPinned=1`を保持 |
| 既存`userDecision`が未設定 | BLOCKED | PASS | NULLを確認 |
| 新規ルールを作成できる | BLOCKED | PASS | `app_rules`書き込み確認 |
| 分類統計を更新できる | BLOCKED | PASS | `classification_stats`書き込み確認 |

## 9. 最終判定

以下をすべて満たした場合のみ、PRをReady for reviewへ移行できます。

- 全必須項目がPASS
- FAIL項目に未解決のものがない
- 実機名とAndroidバージョンが記録されている
- 検証HEADがPRの最新HEADと一致する
- v1基準SHAがRoom DB version 1のコミットと一致する
- 通知内容を含むログやスクリーンショットを公開していない

| 判定 | 値 |
| --- | --- |
| PASS / FAIL / BLOCKED | BLOCKED |
| 未解決事項 | 物理端末接続、NotificationListener実通知、OEM固有挙動、OS通知非破壊性、実通知logcat漏えい、物理端末v1→v2上書き |
| Ready化可否 | 不可 |

物理端末の必須項目が完了するまで、PRはDraftのまま維持し、Ready化・マージを行いません。
