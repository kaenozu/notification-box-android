# Test matrix

| Area | Automated evidence | Manual evidence |
|---|---|---|
| Listener event order | Command queue order and continuation tests | Rapid post/remove/repost observation |
| Queue overflow | Bounded queue overflow code and reconciliation request tests | Burst notifications beyond queue capacity |
| Listener rebind | Scheduling, connection watchdog, exponential backoff, maximum-attempt, and cancellation tests | Disconnect/reconnect on multiple OEM devices |
| Permissions | Provider and ViewModel state tests | Runtime granted with app notifications disabled |
| Preferences | DataStore restore tests and IOException fail-open path | Restart, low-storage, and restored-value checks |
| Repository reads | Automatic retry state in ViewModel | Storage interruption and recovery observation |
| Retention | Repository maintenance tests and active-row prune protection | Restart with expired inactive history and high active count |
| Statistics | Reset and aggregate privacy tests | Confirm history and rules remain after reset |
| Classification | Authentication/urgent precedence, Japanese promotion terms, token-boundary tests | Representative real notification corpus review |
| Payment inbox | PayPay parser, amount normalization, transaction classification, promotion/ambiguous-notification rejection, storage-failure isolation | Real PayPay notification corpus and OEM notification delivery |
| Payment migration | Room 3-to-4 schema test and installed-APK migration validation | Upgrade from a real user database on a physical device |
| MessagingStyle | Structured latest-message test | Notifications from multiple messaging apps |
| Summary | Room aggregate DAO, repository mapping, ViewModel state, and UI state tests | Rolling 24-hour boundary on device |
| Migration | Room managed-device and installed APK overwrite gates | Multi-OEM physical migration |
| Dry-run safety | Planner, ViewModel, statistics, source guard | Confirm no execution control or OS change |
| Actions supply chain | Full-SHA workflow scanner | Review Dependabot action updates |
