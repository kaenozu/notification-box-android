# 通知箱

Android向けの、端末内通知履歴・分類プレビューアプリです。

現在のリリース範囲は、通知を安全に観察・分類し、自分に合う整理ルールを試す技術ベータです。元のOS通知の取消、抑制、スヌーズ、遅延、ダイジェスト配信は行いません。

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
- 単一キューによる通知投稿・削除・再接続同期の順序保持
- Roomによる端末内の通知履歴保存
- MessagingStyleを含む通知本文の安全な抽出
- 通知取込エラーの内容非保持カウンター
- ライト／ダークテーマおよびAndroid 12以降のダイナミックカラー

## データ保持とプライバシー

- 通知タイトルと本文は外部APIへ送信せず、端末内だけで処理します。
- アプリには`INTERNET`権限、広告SDK、解析SDK、クラッシュ収集SDKを含めません。
- 分類統計には通知タイトル・本文を保存せず、件数とパッケージ名だけを保持します。
- 非アクティブかつピン留めされていない7日超の履歴は、アプリ起動・通知投稿・削除・再同期・ピン解除時の整理処理で削除します。
- 履歴は原則500件を上限とし、ピン留め通知は自動削除から保護します。
- Androidバックアップは無効です。
- 通知タイトルや本文をアプリログや取込エラー状態へ出力しません。
- 整理プレビューの集計はセッション内だけで生成し、永続化しません。

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

Release署名は秘密情報をソースへ書かず、次の環境変数がすべてある場合だけ有効になります。

```text
ANDROID_KEYSTORE_PATH
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

GitHub Actionsでは`ANDROID_KEYSTORE_BASE64`を加えた4つのActions Secretsから一時キーストアを生成します。詳細は[`docs/release-runbook.md`](docs/release-runbook.md)を参照してください。

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
- 外部APIやクラウド同期はありません。
- 整理プレビューにはOS通知操作APIと実行ボタンがありません。
- 実際のOS通知制御はv0.1.0の対象外です。
