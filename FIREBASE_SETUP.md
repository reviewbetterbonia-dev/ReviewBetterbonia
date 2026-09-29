# Review Betterbonia Firebase setup

1. Create a Firebase project.
2. Add an Android app using package name `com.reviewbetterbonia.app`.
3. In Authentication > Sign-in method, enable Email/Password.
4. Create a Cloud Firestore database.
5. Replace the three placeholder strings in `app/src/main/res/values/strings.xml` with the Firebase Web API key, App ID, and Project ID used by this app.
6. Publish `firebase.rules` to **Firestore Database > Rules**.
7. Create the first admin user normally, then change that user's Firestore document at `users/{UID}` so `role` is `admin` (or `super_admin`). Do not allow public registration to create admin roles.
8. Run the app.

## Firestore collections used by the app

- `users/{uid}` — profile, role, ranked rating
- `users/{uid}/results/{resultId}` — that user's progress history
- `users/{uid}/friends/{friendUid}` — accepted friendships
- `users/{uid}/friendRequests/{requestId}` — pending requests
- `results/{resultId}` — global result records used by the leaderboard
- `questions/{questionId}` — admin-published quiz questions

The app downloads active Firebase questions in addition to the CSV question bank packaged in `assets/questions/`. The Friends screen uses real Firestore friend requests, and Progress syncs the signed-in user's online results before rendering.

If Firebase is not configured, the app falls back to local demo mode. Demo-mode data is stored only on the device.
