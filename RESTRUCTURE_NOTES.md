# Review Betterbonia 3.0 — restructuring notes

## What changed
- `MainActivity` is now a small navigation/state host instead of the entire app.
- Screens are separated into Fragments: auth, home, casual setup, quiz, result, ranked setup, progress, leaderboards, friends, profile, and admin.
- Quiz loading/parsing moved to `data/QuizRepository.java`.
- Firebase authentication/results/question publishing moved to `data/FirebaseRepository.java`.
- Local preferences/result history moved to `data/LocalStore.java`.
- Shared UI styling moved to `ui/Ui.java`.
- Data objects moved into `model/`.
- RecyclerView is used for progress subjects, leaderboard rows, friends search results, and the admin question list.
- Android back navigation uses the FragmentManager back stack instead of replacing the root view manually. Home is the root screen; pressing Back from Home asks before exiting.

## UI direction
The redesign uses a restrained light background, white cards, one indigo accent, rounded corners, and clearer hierarchy. It intentionally avoids excessive gradients, icons, or decorative elements.

## Casual mode
Casual setup now explicitly supports:
1. Subject selection
2. Category selection for that subject (or all categories)
3. Question count
4. Optional timed mode and seconds/question

The question pool is filtered before the quiz starts, so the requested category is actually enforced.

## Progress
Progress aggregates saved quiz attempts by subject and displays:
- overall accuracy
- total questions/correct answers
- number of attempts per subject
- subject-specific accuracy

## Firebase
The Firebase project IDs/API values from the supplied project were retained. Firebase remains optional: the app can still run in demo/offline mode, while Firebase provides account sync and online result storage.

## Important
The project does not include a machine-specific `local.properties`. Open the project in Android Studio and let Android Studio create it for the local SDK path.
