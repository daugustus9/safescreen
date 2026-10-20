**English** · [Português](README.md)

# SafeScreen (prototype)

Open-source Android app that makes children's screen time safer and more productive. This is the MVP prototype from the project plan; feature IDs below (S1, C2…) refer to that plan.

The app's main language is Brazilian Portuguese; English is used on phones set to English.

The prototype has **no INTERNET permission**: nothing it sees can leave the phone. Parent controls sit behind a PIN on the same phone; the encrypted relay to the parent's own phone is the next step (see *Not in the prototype*).

## What's in it

| ID | Feature | Where |
|---|---|---|
| S1 | On-device risk detection (self-harm, dangerous challenges, drugs, weapons) on typed text, Portuguese and English | `core/RiskClassifier.kt`, rules in `assets/rules/risk_rules.json`, run by `service/GuardAccessibilityService.kt` |
| S5 | "I need help" button: trusted adult and helplines (CVV 188, Disque 100…), opens the dialer only | `ui/HelpScreen.kt`, `assets/content/helplines.json` |
| S7, W1 | Safety and life-advice cards, by age | `assets/content/cards.json`, `core/CardScheduler.kt` |
| — | Encouragement cards: self-view, habits, kindness, citizenship; families can add their own | `assets/content/cards.json`, `ui/ParentScreen.kt` |
| W3 | Bedtime cards inside the agreed window | `core/CardScheduler.kt` |
| P3 | Focus timer; chosen apps are sent to the home screen while it runs | `ui/ChildHomeScreen.kt`, `service/GuardAccessibilityService.kt` |
| C1 | School reminders: the evening before (from 17:00) and the morning of | `core/CardScheduler.kt` |
| C2 | Reads notifications from the school apps the family picks (Agenda Edu, Escola Paraná…) and turns them into tasks | `core/SchoolNoticeParser.kt`, `service/SchoolNotificationListener.kt` |
| T1, C3 | Weekly summary: alert categories, help presses, school, focus, screen time; per-app detail only for under-10s | `core/WeeklySummary.kt`, `ui/ParentScreen.kt` |
| T2 | Family agreement; any rule change asks parent and child to sign again | `data/PolicyStore.kt`, `ui/ParentScreen.kt` |
| T5 | Real-time alert for S1 and S5 (a local notification in the prototype, visible to the child too) | `system/AlertNotifier.kt` |
| T6 | Age tiers: under 10, 10–12, 13+ | `core/Models.kt` |
| — | "What SafeScreen sees" screen, shown before anything is switched on and reachable from the status notification | `ui/TransparencyScreen.kt` |
| — | "All messages" screen: the child and parents can read every message the app can show, family messages included | `ui/MessagesScreen.kt` |

The message engine (`CardOverlayService` + `CardPresenter`) is adapted from the subliminal-message app's overlay service, with one deliberate change: every message is visible and stays until the child reads it. Nothing is subliminal. Safety, life advice and encouragement take turns, so warnings never come three in a row.

## Privacy by construction

- Typed text is checked in memory and discarded; only the category ("SELF_HARM") is recorded. Password fields are skipped.
- The accessibility service listens to two event types only (text typed, app in front) and cannot read screen content (`canRetrieveWindowContent="false"`).
- Notifications from apps other than the chosen school apps are ignored unread.
- Events are kept 30 days on the phone (`data/EventLog.kt`); cloud backup and device transfer are disabled.
- Every card and every detection rule is a plain-text file in `app/src/main/assets`.

## Build

Requires JDK 17+ and the Android SDK (platform 36). Point `JAVA_HOME` at the JDK, then:

```bash
./gradlew testDebugUnitTest assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Unit tests cover the classifier (against the real rules file), the school-notice parser, the card scheduler, the weekly summary and the message library.

## Try it on a phone

Button names below are the English ones; on a Portuguese phone they appear in Portuguese.

1. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
2. Read the transparency screen, then turn on each access in **Set up**. On Android 13+, a sideloaded app needs *Settings › Apps › SafeScreen › ⋮ › Allow restricted settings* before accessibility and notification access can be enabled.
3. Tap **Turn on**, then **Show a card now**.
4. Risk detection: type `desafio do desmaio` or `quero morrer` in any app's search box. A help card appears and a safety alert notification is posted. The parent summary shows the category only.
5. School notices: in **Parent area › Agreement › School apps**, pick a messaging app, then send yourself a notification such as `Prova de matemática amanhã`. Or paste the text into **All tasks**.
6. Focus: pick an app under *Apps paused during focus*, start a focus session and open that app.

## Not in the prototype

- Parent pairing and the end-to-end encrypted relay (alerts and summaries on the parent's phone).
- S2 grooming detection and W7 kindness prompts (v2: need a trained on-device model).
- Google Classroom integration (P1) and the teacher view (C6–C8).
- iOS.
- A Play Store policy review: accessibility-service use must be declared and justified before publishing.

## License

- **Code:** [GNU General Public License v3.0](LICENSE). Anyone may use, study and change it; versions they distribute must stay open under the same license.
- **Message library and detection rules** (`app/src/main/assets/content/` and `app/src/main/assets/rules/`): [Creative Commons Attribution-ShareAlike 4.0](LICENSES/CC-BY-SA-4.0.txt).
- The name "SafeScreen" and its logo are not covered by these licenses.

Copyright © 2026 Denis Augusto.
