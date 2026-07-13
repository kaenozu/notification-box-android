# PR #4 実機検証手順と実施記録

対象PR: `feat/p2-notification-rules`

この文書は、CIで自動確認できるRoom移行と、物理Android端末でのみ確認できるNotificationListener・OS通知非破壊性・OEM固有挙動を分離して記録します。

## 記録方針

このtracked文書へ検証後の最新PR HEADを書き込むと、その記録コミット自身によってHEADが変わります。そのため、次の方式で記録します。

- この文書には、再実施可能な手順と過去に完了した自動検証の対象コードコミットを記録する。
- 物理端末検証を開始する直前に実装を固定し、`git rev-parse HEAD`で検証対象SHAを取得する。
- 物理端末での実施結果、端末情報、検証対象SHAは、trackedファイルを変更しないPRコメントへ記録する。
- 物理端末検証後にtrackedファイルへ変更が入った場合は、変更内容にかかわらず検証対象SHAと最新PR HEADが不一致になるため、Ready化前に再検証する。
- PR本文のCI Runと検証記録は履歴として保持し、現在のPR HEADを表す値と混同しない。

## 0. 2026-07-12 自動検証実施記録

| 項目 | 結果 |
| --- | --- |
| 自動検証対象コードコミット | `72f3057e541332484334bfd389df832973afbc84` |
| v1基準SHA | `b32d2e4835997eada837df1ca1d3b15c760002a7` |
| Android CI Run | `29193476128` — PASS |
| Android Migration Emulator Run | `29193476097` — PASS |
| Room `MigrationTestHelper` | 1 passed / 0 failed / 0 errors / 0 skipped |
| v1 APK→v2 APK上書き移行 | API 30エミュレータでPASS |
| 物理Android端末 | BLOCKED — 実行環境へUSB端末が公開されていない |
| ADB実機接続 | BLOCKED — 実行環境にADBとUSBデバイスがない |
| Ready化可否 | 不可 |

`72f3057e541332484334bfd389df832973afbc84`は、上記自動検証で確認したアプリ・CI実装のコード状態です。後続の記録専用変更を含む現在のPR HEADを表す値ではありません。アプリ、ビルド設定、Room schema、migration、テストまたはworkflowへ変更が入った場合は、新しいHEADで自動検証を再実行して記録を更新します。

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

物理端末検証の結果はPRコメントへ記録します。次の表をコメントへコピーし、空欄を埋めてください。

| 項目 | 記録 |
| --- | --- |
| 検証日 |  |
| 検証者 |  |
| 検証対象PR HEAD |  |
| v1基準SHA | `b32d2e4835997eada837df1ca1d3b15c760002a7` |
| 端末メーカー・機種 |  |
| Androidバージョン |  |
| セキュリティパッチ |  |
| 接続方法 | USB / Wireless debugging |
| 新規インストール / 上書き |  |

開始前に、PRの最新状態を取得し、HEADを記録します。

```powershell
git fetch origin
git switch feat/p2-notification-rules
git pull --ff-only
git rev-parse HEAD
```

このSHAを物理端末検証中に変更しないでください。別コミットがpushされた場合は、最新HEADを取得して最初から再実施します。

## 2. ビルドとインストール

```powershell
.\gradlew.bat assembleDebug --no-build-cache
adb devices -l
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

`adb devices -l`で対象端末の状態が`device`であり、検証対象の物理端末だけが接続されていることを確認します。`emulator-`で始まるserialは物理端末検証には使用しません。

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
| ピン留めでOS通知が消えない | BLOCKED | 物理通知シェードを確認できない |
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

通知タイトルまたは本文に、他と重複しないマーカーを含めます。検証日ごとに新しい値へ変更してください。

```text
NBX_PRIVATE_MARKER_YYYYMMDD_RANDOM
```

アプリプロセスのログだけを採取します。

```powershell
$marker = "NBX_PRIVATE_MARKER_YYYYMMDD_RANDOM"
adb logcat -c

# $markerを含む通知を送信し、アプリで履歴・分類画面を操作する

$pid = (adb shell pidof com.notificationbox.app).Trim()
if (-not $pid) {
    throw "Notification Box process is not running"
}
adb logcat -d --pid=$pid > .\pr4-notification-box-logcat.txt
$matches = @(Select-String -Path .\pr4-notification-box-logcat.txt -SimpleMatch $marker)
"markerMatches=$($matches.Count)"
```

期待結果: `markerMatches=0`。

通知タイトル・本文、端末所有者情報、アカウント情報を含む生ログはPRへ添付しません。PRへ記録するのはマーカー文字列を伏せた一致件数だけです。確認後、生ログを安全に削除します。

```powershell
Remove-Item .\pr4-notification-box-logcat.txt -Force
```

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

この工程では`adb uninstall`、アプリデータ削除、端末設定からのストレージ消去を行いません。

| 確認項目 | 物理端末 | API 30エミュレータ | 備考 |
| --- | --- | --- | --- |
| v2上書き後に起動できる | BLOCKED | PASS | Run `29193476097` |
| v1履歴を保持する | BLOCKED | PASS | 合成v1データを保持 |
| v1ピン留めを保持する | BLOCKED | PASS | `userPinned=1`を保持 |
| 既存`userDecision`が未設定 | BLOCKED | PASS | NULLを確認 |
| 新規ルールを作成できる | BLOCKED | PASS | `app_rules`書き込み確認 |
| 分類統計を更新できる | BLOCKED | PASS | `classification_stats`書き込み確認 |

## 9. PRコメント用の実施結果テンプレート

物理端末検証完了後は、次のテンプレートをPRコメントへ貼り付けます。通知タイトル・本文、生logcat、個人情報を含むスクリーンショットは添付しません。

```markdown
## PR #4 物理端末検証結果

- 検証日:
- 検証者:
- 検証対象HEAD:
- v1基準SHA: `b32d2e4835997eada837df1ca1d3b15c760002a7`
- メーカー・機種:
- Androidバージョン:
- セキュリティパッチ:
- 接続方法:

| 確認項目 | 結果 | 備考 |
| --- | --- | --- |
| 通知アクセス許可・解除・再許可 |  |  |
| 実通知の履歴取り込み |  |  |
| 手動分類の設定・解除 |  |  |
| アプリルールの設定・解除 |  |  |
| 手動分類 > アプリルール > 自動分類 |  |  |
| 同一通知再投稿後の分類保持 |  |  |
| 強制停止・再起動後の保持 |  |  |
| OS通知の非破壊性 |  |  |
| snooze・抑制・遅延なし |  |  |
| 履歴削除とルール削除の分離 |  |  |
| logcat marker matches |  | 一致件数のみ記録 |
| 物理端末v1→v2上書き移行 |  |  |

- 総合判定: PASS / FAIL / BLOCKED
- 未解決事項:
- Ready化可否: 可 / 不可
- 未解決review thread数:
```

## 10. 最終判定

以下をすべて満たした場合のみ、PRをReady for reviewへ移行できます。

- 全必須項目がPASS
- FAIL項目に未解決のものがない
- 実機名とAndroidバージョンが記録されている
- 物理端末の検証対象HEADがPRの最新HEADと一致する
- v1基準SHAがRoom DB version 1のコミットと一致する
- OS通知の非破壊性を目視確認している
- logcatのマーカー一致件数が0件
- 通知内容を含むログやスクリーンショットを公開していない
- 物理端末検証結果をPRコメントへ記録している
- 未解決review threadが0件

| 判定 | 値 |
| --- | --- |
| PASS / FAIL / BLOCKED | BLOCKED |
| 未解決事項 | 物理端末接続、NotificationListener実通知、OEM固有挙動、OS通知非破壊性、実通知logcat漏えい、物理端末v1→v2上書き |
| Ready化可否 | 不可 |

物理端末の必須項目が完了するまで、PRはDraftのまま維持し、Ready化・マージを行いません。
