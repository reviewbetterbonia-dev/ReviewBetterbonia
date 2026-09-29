REVIEW BETTERBONIA - ANDROID QUIZ PROJECT

This version uses an organized CSV question bank instead of the old quiz.xml file.

QUESTION BANK:
- app/src/main/assets/questions/
- 261 migrated questions are in questions/algebra/general.csv
- Add new subjects/categories by creating new CSV files inside questions/<subject>/
- See QUESTION_BANK_GUIDE.md for the exact spreadsheet format.

QUIZ FEATURES:
- Subject and category selection
- 20 / 30 / 50 / 100 questions
- Untimed mode
- Timed mode: 10 or 30 seconds per question
- Personal progress and history
- Firebase accounts, online results, friends, and leaderboards when Firebase is configured

BUILD:
1. Open this folder in Android Studio.
2. Let Android Studio sync Gradle and download dependencies if prompted.
3. Choose Build > Make Project.
4. Choose Build > Build Bundle(s) / APK(s) > Build APK(s).
5. The debug APK will be under app/build/outputs/apk/debug/.

FIREBASE:
See FIREBASE_SETUP.md. The Firebase project is intentionally not bundled with credentials.

LEGACY SOURCE:
The original XML question source is preserved under legacy_question_source/ for reference only.
