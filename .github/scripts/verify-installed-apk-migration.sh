#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <v1-apk> <current-apk>" >&2
  exit 2
fi

V1_APK="$1"
CURRENT_APK="$2"
APP_ID="com.notificationbox.app"
ACTIVITY="$APP_ID/.MainActivity"
DB_NAME="notification-box.db"
REPORT_DIR="installed-apk-migration-report"
SEED_DB="$REPORT_DIR/v1-seeded.db"
MIGRATED_DB="$REPORT_DIR/current-migrated.db"
LOGCAT_FILE="$REPORT_DIR/logcat.txt"
RESULT_FILE="$REPORT_DIR/result.txt"

mkdir -p "$REPORT_DIR"
: > "$RESULT_FILE"

record() {
  printf '%s\n' "$*" | tee -a "$RESULT_FILE"
}

record "Installed APK migration validation"
record "v1 APK: $V1_APK"
record "current APK: $CURRENT_APK"
record "device: $(adb shell getprop ro.product.manufacturer | tr -d '\r') $(adb shell getprop ro.product.model | tr -d '\r')"
record "android: $(adb shell getprop ro.build.version.release | tr -d '\r') (API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))"

adb uninstall "$APP_ID" >/dev/null 2>&1 || true
adb install -r "$V1_APK" | tee "$REPORT_DIR/install-v1.txt"

python3 - "$SEED_DB" <<'PY'
import sqlite3
import sys

path = sys.argv[1]
connection = sqlite3.connect(path)
try:
    connection.executescript(
        """
        PRAGMA user_version = 1;
        CREATE TABLE IF NOT EXISTS notifications (
            `key` TEXT NOT NULL,
            packageName TEXT NOT NULL,
            appLabel TEXT NOT NULL,
            title TEXT,
            text TEXT,
            postTimeMillis INTEGER NOT NULL,
            notificationId INTEGER NOT NULL,
            tag TEXT,
            channelId TEXT,
            category TEXT NOT NULL,
            reason TEXT NOT NULL,
            userPinned INTEGER NOT NULL,
            isActive INTEGER NOT NULL,
            removedAtMillis INTEGER,
            PRIMARY KEY(`key`)
        );
        CREATE INDEX IF NOT EXISTS index_notifications_postTimeMillis
            ON notifications(postTimeMillis);
        CREATE INDEX IF NOT EXISTS index_notifications_userPinned
            ON notifications(userPinned);
        CREATE INDEX IF NOT EXISTS index_notifications_isActive
            ON notifications(isActive);
        CREATE TABLE IF NOT EXISTS room_master_table (
            id INTEGER PRIMARY KEY,
            identity_hash TEXT
        );
        INSERT OR REPLACE INTO room_master_table(id, identity_hash)
            VALUES(42, '01d573e57a76f503e44f6c782467c726');
        INSERT INTO notifications(
            `key`, packageName, appLabel, title, text, postTimeMillis,
            notificationId, tag, channelId, category, reason,
            userPinned, isActive, removedAtMillis
        ) VALUES(
            'installed-upgrade-existing',
            'com.example.synthetic',
            'Synthetic Test App',
            'Synthetic migration title',
            'Synthetic migration body',
            1000,
            42,
            'migration-tag',
            'migration-channel',
            'KeepNow',
            'synthetic automatic reason',
            1,
            1,
            NULL
        );
        """
    )
    connection.commit()
finally:
    connection.close()
PY

adb shell run-as "$APP_ID" mkdir -p databases
adb shell run-as "$APP_ID" rm -f \
  "databases/$DB_NAME" \
  "databases/$DB_NAME-wal" \
  "databases/$DB_NAME-shm"
adb push "$SEED_DB" "/data/local/tmp/$DB_NAME" | tee "$REPORT_DIR/push-v1-db.txt"
adb shell chmod 0644 "/data/local/tmp/$DB_NAME"
adb shell run-as "$APP_ID" cp "/data/local/tmp/$DB_NAME" "databases/$DB_NAME"
adb shell rm -f "/data/local/tmp/$DB_NAME"

adb install -r "$CURRENT_APK" | tee "$REPORT_DIR/install-current.txt"
adb logcat -c
adb shell am start -W -n "$ACTIVITY" | tee "$REPORT_DIR/launch-current.txt"
sleep 3

PID="$(adb shell pidof "$APP_ID" | tr -d '\r')"
if [ -z "$PID" ]; then
  adb logcat -d > "$LOGCAT_FILE"
  record "FAIL: current process is not running after launch"
  exit 1
fi
record "current process: $PID"

adb shell am force-stop "$APP_ID"
adb logcat -d > "$LOGCAT_FILE"
adb exec-out run-as "$APP_ID" cat "databases/$DB_NAME" > "$MIGRATED_DB"

python3 - "$MIGRATED_DB" "$RESULT_FILE" <<'PY'
import sqlite3
import sys

path, result_path = sys.argv[1:]
connection = sqlite3.connect(path)
connection.row_factory = sqlite3.Row
try:
    user_version = connection.execute("PRAGMA user_version").fetchone()[0]
    if user_version != 3:
        raise AssertionError(f"expected user_version 3, got {user_version}")

    row = connection.execute(
        """
        SELECT `key`, packageName, title, text, userPinned, userDecision,
               contentAvailability
        FROM notifications
        WHERE `key` = 'installed-upgrade-existing'
        """
    ).fetchone()
    if row is None:
        raise AssertionError("seed notification was not preserved")
    if row["packageName"] != "com.example.synthetic":
        raise AssertionError("package name changed during migration")
    if row["title"] != "Synthetic migration title":
        raise AssertionError("title changed during migration")
    if row["text"] != "Synthetic migration body":
        raise AssertionError("body changed during migration")
    if row["userPinned"] != 1:
        raise AssertionError("pin state was not preserved")
    if row["userDecision"] is not None:
        raise AssertionError("existing notification userDecision is not NULL")
    if row["contentAvailability"] != "AVAILABLE":
        raise AssertionError("existing content availability did not default to AVAILABLE")

    tables = {
        item[0]
        for item in connection.execute(
            "SELECT name FROM sqlite_master WHERE type = 'table'"
        )
    }
    required_tables = {"notifications", "app_rules", "classification_stats"}
    missing = required_tables - tables
    if missing:
        raise AssertionError(f"missing tables: {sorted(missing)}")

    connection.execute(
        """
        INSERT INTO app_rules(packageName, appLabel, decision, updatedAtMillis)
        VALUES('com.example.synthetic', 'Synthetic Test App', 'Ignore', 2000)
        """
    )
    connection.execute(
        "INSERT INTO classification_stats(`key`, count) VALUES('synthetic.total', 1)"
    )
    connection.commit()

    rule = connection.execute(
        "SELECT decision FROM app_rules WHERE packageName = 'com.example.synthetic'"
    ).fetchone()
    stat = connection.execute(
        "SELECT count FROM classification_stats WHERE `key` = 'synthetic.total'"
    ).fetchone()
    if rule is None or rule[0] != "Ignore":
        raise AssertionError("app_rules write validation failed")
    if stat is None or stat[0] != 1:
        raise AssertionError("classification_stats write validation failed")

    with open(result_path, "a", encoding="utf-8") as output:
        output.write("PASS: database user_version is 3\n")
        output.write("PASS: installed v1 notification data preserved\n")
        output.write("PASS: installed v1 pin state preserved\n")
        output.write("PASS: existing userDecision is NULL\n")
        output.write("PASS: existing contentAvailability is AVAILABLE\n")
        output.write("PASS: app_rules table is writable\n")
        output.write("PASS: classification_stats table is writable\n")
finally:
    connection.close()
PY

if grep -E "FATAL EXCEPTION|Room cannot verify|IllegalStateException:.*migration" "$LOGCAT_FILE"; then
  record "FAIL: migration-related fatal error found in logcat"
  exit 1
fi

record "PASS: v1 APK to current APK overwrite migration completed"
