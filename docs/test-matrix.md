# Test matrix

| Area | Automated evidence | Manual evidence |
|---|---|---|
| Listener event order | Command queue order and continuation tests | Rapid post/remove/repost observation |
| Permissions | Provider and ViewModel state tests | Runtime granted with app notifications disabled |
| Preferences | DataStore restore path compiled in application startup | Restart and confirm restored values |
| Retention | Repository maintenance tests | Restart with expired inactive history |
| Statistics | Reset and aggregate privacy tests | Confirm history and rules remain after reset |
| MessagingStyle | Structured latest-message test | Notifications from multiple messaging apps |
| Migration | Room managed-device and installed APK overwrite gates | Multi-OEM physical migration |
| Dry-run safety | Planner, ViewModel, statistics, source guard | Confirm no execution control or OS change |
| Actions supply chain | Full-SHA workflow scanner | Review Dependabot action updates |
