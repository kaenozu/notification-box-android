#!/usr/bin/env bash
set -euo pipefail

manifest="$(
  find app/build/intermediates -type f -name AndroidManifest.xml -print \
    | grep -E '/(merged_manifests|merged_manifest|packaged_manifests)/release/' \
    | head -n 1 \
    || true
)"

if [[ -z "$manifest" || ! -f "$manifest" ]]; then
  echo "Release merged manifest was not found." >&2
  find app/build/intermediates -type f -name AndroidManifest.xml -print >&2 || true
  exit 1
fi

echo "Verifying merged release manifest: $manifest"

require_pattern() {
  local pattern="$1"
  local description="$2"
  if ! grep -Eq "$pattern" "$manifest"; then
    echo "Missing required release manifest property: $description" >&2
    exit 1
  fi
}

forbid_pattern() {
  local pattern="$1"
  local description="$2"
  if grep -Eq "$pattern" "$manifest"; then
    echo "Forbidden release manifest property found: $description" >&2
    exit 1
  fi
}

require_pattern 'package="com\.notificationbox\.app"' 'application package'
require_pattern 'android:versionCode="1"' 'versionCode 1'
require_pattern 'android:versionName="0\.1\.0"' 'versionName 0.1.0'
require_pattern 'android:allowBackup="false"' 'Android backup disabled'
require_pattern 'android:usesCleartextTraffic="false"' 'cleartext traffic disabled'
require_pattern 'android:dataExtractionRules="@xml/data_extraction_rules"' 'Android 12+ backup rules'
require_pattern 'android:fullBackupContent="@xml/backup_rules"' 'legacy backup rules'
require_pattern 'android:name="com\.notificationbox\.app\.service\.NotificationRelayService"' 'notification listener service'
require_pattern 'android:permission="android\.permission\.BIND_NOTIFICATION_LISTENER_SERVICE"' 'listener bind permission'
require_pattern 'android:exported="false"' 'listener service not exported'

# v0.1.0 performs no networking and sends no app-generated notifications.
forbid_pattern '<uses-permission' 'any requested runtime or normal permission'
forbid_pattern 'android\.permission\.INTERNET' 'network permission'
forbid_pattern 'android\.permission\.POST_NOTIFICATIONS' 'notification posting permission'

printf '%s\n' \
  'Release manifest boundary: PASS' \
  'requested_permissions=0' \
  'backup=disabled' \
  'cleartext_traffic=disabled' \
  'notification_listener_service=present_not_exported'
