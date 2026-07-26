# 通知箱

Android向けの、端末内通知履歴・分類プレビューアプリです。

現在のリリース範囲は、通知を安全に観察・分類し、自分に合う整理ルールを試す技術ベータです。PayPay通知から金額・店舗・取引種別を端末内で抽出する「決済インボックス」もベータ提供します。元のOS通知の取消、抑制、スヌーズ、遅延、ダイジェスト配信は行いません。

## 主な機能

- 初回起動時の通知アクセス明示説明
- OSから取得する通知アクセス状態
- 通知の分類ロジック
- 通知履歴の確認
- OS通知を変更しないセッション限定の整理プレビュー
- 優先 / あとで確認 / 低優先の分類フィルタ
- 通知ごとの手動分類と解除
- アプリ別ルールの作成・変更・解除
- 手動指定 > アプリ別ルール > 自動分類の判定優先順位
- 端末内だけで保持する分類補正統計と明示的リセット
- 個別ピン留め、確認付き個別削除、確認付き全消去
- `NotificationListenerService`からの通知履歴同期
- bounded単一キューによる通知投稿・削除・再接続同期の順序保持
- キュー過負荷時の専用エラー記録、通知リスナー再接続、スナップショット再同期
- 再接続タイムアウト、指数バックオフ、最大試行回数による復旧制御
- Roomによる端末内の通知履歴保存
- Room集計による過去24時間の通知サマリー
- 通知履歴、アプリ別ルール、分類統計の読取失敗時の自動再購読
- MessagingStyleを含む通知本文の安全な抽出
- 内容を取得できない通知のメタデータ限定fail-open保存
- 通知取込エラーの内容非保持カウンター
- PayPay通知からの金額・店舗・支払い／返金／チャージ／送受金の端末内解析
- 通知本文を複製せず派生情報だけを保存する決済イベントテーブル
- 今月の推定支出と決済履歴を表示する決済インボックス
- 決済履歴の確認付き全消去と、決済保存失敗の内容非保持カウンター
- ライト／ダークテーマおよびAndroid 12以降のダイナミックカラー

## データ保持とプライバシー

- 通知タイトルと本文は外部APIへ送信せず、端末内だけで処理します。
- アプリには`INTERNET`権限、広告SDK、解析SDK、クラッシュ収集SDKを含めません。
- 分類統計には通知タイトル・本文を保存せず、件数とパッケージ名だけを保持します。
- 決済イベントには通知本文を複製せず、抽出した金額、店舗、取引種別、解析器情報だけを保持します。
- 通知終了時刻から7日を超えた、非アクティブかつピン留めされていない通知履歴を整理処理で削除します。
- 通知履歴は原則500件を上限としますが、アクティブ通知とピン留め通知は上限整理から保護します。
- 決済イベントはユーザーが決済画面から削除するか、アプリをアンインストールするまで保持します。画面には直近500件を表示します。
- Androidバックアップは無効です。
- 通知タイトルや本文をアプリログや取込エラー状態へ出力しません。
- 整理プレビューの集計はセッション内だけで生成し、永続化しません。
- 通知内容を表示する画面は`FLAG_SECURE`で保護し、スクリーンショット、画面録画、タスク切替プレビューへの露出を抑止します。

公開用の方針は[`docs/privacy-policy-ja.md`](docs/privacy-policy-ja.md)を参照してください。

## ローカル検証

Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
.\gradlew.bat lintRelease bundleRelease assembleRelease
```

Linux/macOS:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew lintRelease bundleRelease assembleRelease
```

WindowsとLinuxでRoom/RobolectricテストのSQLiteバックエンドを揃えるため、Robolectric 4.16.1を使用します。

## Release署名

PRと通常の手動検証ではunsigned Release APK/AABだけを生成します。

署名付き候補は、GitHub Actionsの`Android Release Candidate`を`sign=true`で手動実行し、保護された`release-signing` Environmentの承認後にだけ生成します。署名対象は検証済みの現在の`main`完全SHAに限定され、署名工程ではリポジトリ内のGradleやスクリプトを実行しません。

`v*`タグのGitHub Releaseを公開すると、`Attach Android APK to GitHub Release`が同じタグを検証して、開発・内部確認用のdebug署名APK、unsigned Release APK、および各SHA-256チェックサムをReleaseへ添付します。実機へインストールする場合は`-debug.apk`を使用してください。debug署名APKは本番配布物ではありません。

APKとAABの署名検証が成功しなければ、署名付き成果物は作成されません。詳細は[`docs/release-runbook.md`](docs/release-runbook.md)を参照してください。

## 実機検証

1台のAndroid実機をADB接続し、Release APKのSHA-256を確認してから次を実行します。

```powershell
powershell -ExecutionPolicy Bypass -File tools/physical-device-validation.ps1 `
  -ApkPath .\app-release.apk `
  -ExpectedSha256 <APK_SHA256> `
  -Install `
  -OpenNotificationSettings
```

このスクリプトは通知本文や生の通知識別子を取得せず、端末情報と手動チェックリストだけを生成します。

## リリース文書

- [`docs/release-runbook.md`](docs/release-runbook.md)
- [`docs/privacy-policy-ja.md`](docs/privacy-policy-ja.md)
- [`docs/google-play-data-safety-ja.md`](docs/google-play-data-safety-ja.md)
- [`docs/google-play-store-listing-ja.md`](docs/google-play-store-listing-ja.md)

## 現在の安全境界

- 整理プレビューはViewModelの各セッションで必ず「観察のみ」から開始します。
- 旧バージョンの保存済み`AppMode`は互換性のため保持しますが、`Active`でもOS通知操作は実行しません。
- 決済インボックスはPayPay通知だけを対象とするベータであり、通知に含まれない明細を推測しません。
- 決済の解析・保存に失敗しても、通常の通知履歴保存を巻き戻しません。
- 外部APIやクラウド同期はありません。
- 整理プレビューにはOS通知操作APIと実行ボタンがありません。
- 実際のOS通知制御はv0.1.0の対象外です。
