# Known unknowns

- Current branch CI and migration conclusions are unknown until the Draft PR workflows finish.
- Physical-device behavior is not verified by repository-only changes.
- OEM-specific listener lifecycle behavior remains to be validated on Xiaomi, Pixel-family, Samsung, and another OEM.
- DataStore restoration requires a process-restart manual check in addition to compilation and unit coverage.
- MessagingStyle extraction behavior varies by source application and requires representative device checks.

Unknown items must remain explicit and must not be converted into PASS by inference.
