# Review Betterbonia — Admin and Ranked Setup

## First Super Admin
1. Create your normal Review Betterbonia account in the app.
2. In Firebase Console → Firestore → `users` → your user document, set `role` to `super_admin`.
3. Set `rankedRating` to `0` and `rankedRank` to `Freshman`.
4. Log out and back in. The Super Admin Panel will appear.

Do not grant `super_admin` to other users unless you intentionally want them to control administrators.

## Admins
Super Admin → Admin Panel → User & Admin management → find a username → Grant Admin.
Admins can add/update questions. Only Super Admin can change administrator roles or permanently delete questions.

## Question adding
Admin Panel → Question Bank → Add Question. Questions are stored in Firestore under `questions`. Students download active questions and keep the bundled 261-question bank as a local fallback.

## Ranked mode
Ranked matches always use 30 questions from all available subjects. Time per question is based on rank: Freshman 30s, Sophomore 26s, Junior 22s, Senior 18s, Graduate 14s, Engineer 10s.

Current RP rules: correct +10 except Engineer +8; wrong Freshman -3, Sophomore -4, Junior -5, Senior -6, Graduate -8, Engineer -15; timeout is one extra RP loss, with Engineer -18. These are balancing values and can be changed later.

## Important security note
The current Android build calculates ranked RP on-device so the feature works without a Cloud Functions deployment. For a public high-stakes competitive system, move RP validation to a trusted server/Cloud Function before relying on the leaderboard competitively. The Firestore rules protect roles, but client-calculated rating can still be manipulated by a modified APK.

## Historical records
The intended long-term workflow is to archive graduating accounts and retain only a small historical record (display name, class/batch, subject achievement, rank/rating, best performance, date). The current UI includes the architecture/info screen; automated account archival should be added only after the exact school retention policy is decided.
