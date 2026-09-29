# Review Betterbonia Algebra 2.0

This version turns the original single quiz into a player-based algebra game.

## Included
- Email/password account creation and login
- Forgot-password flow when Firebase is configured
- Persistent player profile
- 30-question mode
- 50-question mode
- Timed mode: 30 questions / 10 minutes
- Shuffled questions and answer choices
- Per-attempt results and local progress history
- Overall accuracy and personal bests
- Global online leaderboards per mode
- Friends-only leaderboards per mode
- Username search
- Friend requests and acceptance
- Online score storage in Firestore
- Offline/demo mode before Firebase setup
- Modern card-based orange/cream interface
- Existing 261 questions migrated into the editable CSV question bank under `app/src/main/assets/questions/`

## Firebase
See `FIREBASE_SETUP.md`. The source intentionally contains placeholders instead of real Firebase credentials.

After adding your Firebase API key, App ID and Project ID to `strings.xml`, enable Email/Password Authentication and publish `firebase.rules`.
