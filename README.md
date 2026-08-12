# 通知箱

Android向けの、端末内通知履歴・分類プレビューアプリです。通知を安全に観察・分類し、自分に合う整理ルールを試す技術ベータとして開発しています。

## 現在の範囲

- 通知アクセス状態の確認と初回説明
- 通知履歴の端末内保存
- 優先 / あとで確認 / 低優先の分類
- 通知ごとの手動分類とアプリ別ルール
- ピン留め、個別削除、全消去
- 通知リスナー再接続・再同期
- 過去24時間の通知サマリー
- PayPay通知から派生情報を抽出する決済インボックス
- ライト / ダークテーマ、Android 12以降のダイナミックカラー

## 安全境界

**現行ベータはOS通知を変更しません。**

- 元通知の取消、抑制、スヌーズ、遅延、ダイジェスト配信を行いません。
- 整理プレビューは観察のみで開始します。
- 旧保存状態が `Active` でも、OS通知操作は実行しません。
- 実際のOS通知制御は現行リリース範囲外です。
- 決済インボックスは通知本文に含まれる情報だけを解析し、明細を推測しません。

## データとプライバシー

- 通知タイトル・本文は外部APIへ送信しません。
- `INTERNET` 権限、広告SDK、解析SDK、クラッシュ収集SDKを含めません。
- 分類統計は通知本文を保持せず、件数とパッケージ名だけを保存します。
- 決済イベントは通知本文を複製せず、抽出した金額・店舗・取引種別等だけを保存します。
- 通知履歴は整理ポリシーに従って端末内で保持します。
- Androidバックアップは無効です。
- 通知内容をアプリログや診断カウンターへ出力しません。
- 通知内容を表示する画面は `FLAG_SECURE` で保護します。

公開用方針: [docs/privacy-policy-ja.md](docs/privacy-policy-ja.md)

## 開発・検証

Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
.\gradlew.bat lintRelease bundleRelease assembleRelease
```

Linux / macOS:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew lintRelease bundleRelease assembleRelease
```

Room / Robolectric、migration、通知取込、分類、決済解析など、変更範囲に応じたfocused testも追加してください。

## Release

通常のPRではunsigned Release APK / AABを検証対象にします。署名付き候補は保護された `release-signing` Environment を通して生成します。

リリース時は次を別々に確認します。

1. Exact source SHA の自動ゲート
2. migration / signing / artifact metadata
3. physical-device acceptance
4. GitHub / Play側の管理設定

CIやemulatorのPASSだけで物理端末受入済みとは扱いません。

詳細: [docs/release-runbook.md](docs/release-runbook.md)

## 実機検証

ADB接続したAndroid端末で、検証対象APKのSHA-256を確認してから受入を行います。

```powershell
powershell -ExecutionPolicy Bypass -File tools/physical-device-validation.ps1 `
  -ApkPath .\app-release.apk `
  -ExpectedSha256 <APK_SHA256> `
  -Install `
  -OpenNotificationSettings
```

このrunnerは通知本文や生の通知識別子を収集しません。

## 主な資料

- [Release runbook](docs/release-runbook.md)
- [Privacy policy](docs/privacy-policy-ja.md)
- [Google Play Data safety](docs/google-play-data-safety-ja.md)
- [Google Play store listing](docs/google-play-store-listing-ja.md)

## 作業管理

READMEには変動しやすいrelease SHA、個別PR、実機結果を固定しません。最新の実装PR、current-main gate、physical-device blocker、repository ruleset は GitHub Issues / Pull Requests を正としてください。
