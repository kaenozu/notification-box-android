# Known unknowns

- Current branch CI and migration conclusions remain unknown until workflows finish for the final exact HEAD.
- Physical-device behavior is not verified by repository-only changes.
- OEM-specific listener lifecycle behavior remains to be validated on Xiaomi, Pixel-family, Samsung, and another OEM.
- DataStore restoration has JVM/Robolectric coverage but still requires a real process-restart check.
- MessagingStyle extraction behavior varies by source application and requires representative device checks.
- The ordered command queue uses `Channel.UNLIMITED` to avoid blocking or dropping main-thread listener callbacks; sustained extreme notification bursts can increase memory use and require device stress testing.
- Service shutdown drains commands already accepted by the queue while the process remains alive, but Android may terminate the process before asynchronous draining completes; reconnect reconciliation remains the recovery path and requires device validation.

Unknown items must remain explicit and must not be converted into PASS by inference.
