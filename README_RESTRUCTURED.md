# Review Betterbonia — Restructured Build

Open the **Review Betterbonia** folder in Android Studio and let Gradle sync. Build with **Build > Make Project** or run the `app` configuration.

The original question-bank CSV files under `app/src/main/assets/questions/` are preserved and are loaded recursively at runtime.

Leaderboard update (2026-09-27): the Leaderboard screen now shows one overall ranking sorted by total points, with separate Math, Machine Design, and Powerplant point columns. Algebra quiz results are displayed under the Math column. Ranked/mixed results are not double-counted into subject totals because they cannot be attributed to a single subject.
