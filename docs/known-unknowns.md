# Known unknowns

- Physical-device behavior is not verified by repository-only changes.
- OEM-specific listener lifecycle behavior remains to be validated on Xiaomi, Pixel-family, Samsung, and another OEM.
- DataStore restoration has JVM/Robolectric coverage but still requires a real process-restart check.
- MessagingStyle extraction behavior varies by source application and requires representative device checks.
- The ordered command queue is bounded to 256 accepted commands. Overflow is recorded separately, triggers listener rebind, and relies on a fresh active-notification snapshot to reconcile any dropped callback.
- Listener rebind now uses a connection watchdog, exponential backoff, and a maximum of three attempts. OEM-specific behavior when Android accepts `requestRebind()` but does not deliver `onListenerConnected()` still requires device validation.
- Service shutdown drains commands already accepted by the queue while the process remains alive, but Android may terminate the process before asynchronous draining completes; reconnect reconciliation remains the recovery path and requires device validation.
- Notification-history, app-rule, and classification-stat observation retry automatically after non-cancellation read failures. Real storage corruption and low-disk behavior still require device validation.
- Active notifications are protected from the 500-row history cap. The database may temporarily exceed 500 rows when active or pinned notifications alone exceed the cap; this is intentional and requires stress validation.

Unknown items must remain explicit and must not be converted into PASS by inference.
