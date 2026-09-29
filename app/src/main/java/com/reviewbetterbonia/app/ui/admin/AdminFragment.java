package com.reviewbetterbonia.app.ui.admin;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.*;

import com.reviewbetterbonia.app.data.FirebaseRepository;
import com.reviewbetterbonia.app.model.Question;
import com.reviewbetterbonia.app.model.QuizResult;
import com.reviewbetterbonia.app.ui.*;

import java.util.*;

public class AdminFragment extends BaseFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context x = requireContext();
        LinearLayout p = Ui.page(x);
        Ui.add(p, Ui.heading(x, "Admin tools"), Ui.dp(x, 42));
        Ui.add(p, Ui.muted(x, "Manage question bank organized by subject and category.", 14), Ui.dp(x, 35));

        if (app().isAdmin()) {
            Button add = Ui.button(x, "＋ Add question", true);
            add.setOnClickListener(v -> app().navigate(new AddQuestionFragment(), true));
            Ui.add(p, add, Ui.dp(x, 56));

            Ui.add(p, Ui.gap(x, 8), Ui.dp(x, 8));
            Button review = Ui.button(x, "Review pending questions", false);
            review.setOnClickListener(v -> showPendingSubmissions(x));
            Ui.add(p, review, Ui.dp(x, 56));

            Ui.add(p, Ui.muted(x, "New questions stay pending until an admin approves them. Approved questions become available through Firebase immediately.", 12), Ui.dp(x, 45));

            LinearLayout userCard = Ui.card(x);
            userCard.addView(Ui.text(x, "User Profiles & Management", 18, true));
            userCard.addView(Ui.muted(x, "View user profiles, search accounts, and manage user progress.", 13));

            Button manageUsers = Ui.button(x, "View & Manage Users", false);
            manageUsers.setOnClickListener(v -> app().navigate(new ManageUsersFragment(), true));
            userCard.addView(manageUsers, new LinearLayout.LayoutParams(-1, Ui.dp(x, 50)));
            p.addView(userCard);
        }

        if (app().isSuperAdmin()) {
            Ui.add(p, Ui.gap(x, 8), Ui.dp(x, 8));
            LinearLayout superCard = Ui.card(x);
            superCard.addView(Ui.text(x, "Super Admin Controls", 18, true));
            superCard.addView(Ui.muted(x, "Manage global user scores and clean up accounts.", 13));

            Button resetScores = Ui.button(x, "Reset All User Scores", false);
            resetScores.setOnClickListener(v -> {
                new AlertDialog.Builder(x)
                        .setTitle("Reset All Scores?")
                        .setMessage("This will reset the rating points and rank of every user to 0 (Freshman) and clear ranked data. Continue?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Reset", (d, w) -> {
                            long now = System.currentTimeMillis();
                            app().local.setRankedClearedTime(now);
                            List<QuizResult> kept = new ArrayList<>();
                            for (QuizResult r : app().local.results()) {
                                boolean isRanked = r.mode != null && r.mode.toLowerCase(Locale.US).contains("ranked");
                                if (!isRanked) kept.add(r);
                            }
                            app().local.replaceResults(kept);
                            app().rating = 0;
                            app().rank = "Freshman";
                            app().local.prefs().edit().putInt("ranked_rating", 0).apply();

                            if (app().isOffline()) {
                                Toast.makeText(x, "Local scores and ranked data reset.", Toast.LENGTH_SHORT).show();
                            } else {
                                app().firebase.resetAllScores(ok -> Toast.makeText(x, ok ? "All user scores and ranked data reset successfully." : "Could not reset scores.", Toast.LENGTH_SHORT).show());
                            }
                        })
                        .show();
            });
            superCard.addView(resetScores, new LinearLayout.LayoutParams(-1, Ui.dp(x, 50)));

            superCard.addView(Ui.gap(x, 8));
            Button clearData = Ui.button(x, "Clear All Users Data", false);
            clearData.setBackgroundColor(Color.parseColor("#E53935"));
            clearData.setTextColor(Color.WHITE);
            clearData.setOnClickListener(v -> {
                new AlertDialog.Builder(x)
                        .setTitle("Clear All Users Data?")
                        .setMessage("WARNING: This will permanently delete ALL user accounts and quiz results from the system (including your own account). Continue?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Clear Everything", (d, w) -> {
                            if (app().isOffline()) {
                                app().local.prefs().edit().clear().apply();
                                app().signOut();
                                Toast.makeText(x, "Local data cleared.", Toast.LENGTH_SHORT).show();
                            } else {
                                app().firebase.clearAllUsersData(ok -> {
                                    if (ok) {
                                        app().local.prefs().edit().clear().apply();
                                        Toast.makeText(x, "All users data cleared.", Toast.LENGTH_LONG).show();
                                        app().signOut();
                                    } else {
                                        Toast.makeText(x, "Could not clear data.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        })
                        .show();
            });
            superCard.addView(clearData, new LinearLayout.LayoutParams(-1, Ui.dp(x, 50)));

            p.addView(superCard);
            Ui.add(p, Ui.gap(x, 10), Ui.dp(x, 10));
        }

        RecyclerView list = new RecyclerView(x);
        list.setLayoutManager(new LinearLayoutManager(x));
        list.setAdapter(new GroupedQuestionAdapter(app().quiz.questions));
        Ui.addWeight(p, list);

        Button back = Ui.button(x, "Back", false);
        back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        Ui.add(p, back, Ui.dp(x, 52));
        return p;
    }

    private void showPendingSubmissions(Context x) {
        if (!app().firebase.online) {
            Toast.makeText(x, "Firebase is not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        app().firebase.loadPendingQuestionSubmissions(submissions -> {
            if (submissions.isEmpty()) {
                Toast.makeText(x, "There are no pending question submissions.", Toast.LENGTH_SHORT).show();
                return;
            }

            ScrollView scroll = new ScrollView(x);
            LinearLayout body = new LinearLayout(x);
            body.setOrientation(LinearLayout.VERTICAL);
            body.setPadding(Ui.dp(x, 4), Ui.dp(x, 4), Ui.dp(x, 4), Ui.dp(x, 4));
            scroll.addView(body);

            AlertDialog dialog = new AlertDialog.Builder(x)
                    .setTitle("Pending questions (" + submissions.size() + ")")
                    .setView(scroll)
                    .setNegativeButton("Close", null)
                    .create();

            for (FirebaseRepository.QuestionSubmissionRow row : submissions) {
                LinearLayout card = Ui.card(x);
                TextView meta = Ui.muted(x,
                        row.subject + " • " + row.category + "\n" +
                                "CSV: " + row.targetFile + "\n" +
                                "Submitted by: " + (row.submittedByName.isEmpty() ? row.submittedBy : row.submittedByName),
                        12);
                card.addView(meta);
                card.addView(Ui.gap(x, 4));
                card.addView(Ui.text(x, row.question, 15, true));
                card.addView(Ui.text(x, "A. " + row.choiceA + "\nB. " + row.choiceB + "\nC. " + row.choiceC + "\nD. " + row.choiceD, 13, false));
                card.addView(Ui.muted(x, "Correct answer: " + row.correct, 12));

                LinearLayout actions = new LinearLayout(x);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                actions.setPadding(0, Ui.dp(x, 8), 0, 0);

                Button reject = Ui.button(x, "Reject", false);
                Button approve = Ui.button(x, "Approve", true);
                actions.addView(reject, new LinearLayout.LayoutParams(0, Ui.dp(x, 50), 1));
                actions.addView(Ui.gap(x, 8), new LinearLayout.LayoutParams(Ui.dp(x, 8), 1));
                actions.addView(approve, new LinearLayout.LayoutParams(0, Ui.dp(x, 50), 1));
                card.addView(actions);

                reject.setOnClickListener(v -> {
                    final EditText reason = new EditText(x);
                    reason.setHint("Optional rejection reason");
                    new AlertDialog.Builder(x)
                            .setTitle("Reject question?")
                            .setView(reason)
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Reject", (d, w) -> app().firebase.rejectQuestionSubmission(
                                    app().user.uid,
                                    row.submissionId,
                                    reason.getText().toString(),
                                    ok -> {
                                        Toast.makeText(x, ok ? "Question rejected." : "Could not reject question.", Toast.LENGTH_SHORT).show();
                                        if (ok) dialog.dismiss();
                                    }
                            ))
                            .show();
                });

                approve.setOnClickListener(v -> new AlertDialog.Builder(x)
                        .setTitle("Approve question?")
                        .setMessage("This will publish the question to Firebase immediately and make it available to app users. It will also be included in the next GitHub CSV synchronization.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Approve", (d, w) -> app().firebase.approveQuestionSubmission(
                                app().user.uid,
                                row,
                                ok -> {
                                    Toast.makeText(x, ok ? "Question approved and published." : "Could not approve question.", Toast.LENGTH_SHORT).show();
                                    if (ok) {
                                        app().firebase.loadActiveQuestions(qs -> app().quiz.mergeRemoteQuestions(qs));
                                        dialog.dismiss();
                                    }
                                }
                        ))
                        .show());

                body.addView(card, new LinearLayout.LayoutParams(-1, -2));
                Ui.add(body, Ui.gap(x, 10), Ui.dp(x, 10));
            }

            dialog.show();
        });
    }

    static class GroupedQuestionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_SUBJECT = 0;
        private static final int TYPE_CATEGORY = 1;
        private static final int TYPE_QUESTION = 2;

        private static class Item {
            int type;
            String title;
            Question question;
            Item(int t, String s) { type = t; title = s; }
            Item(int t, Question q) { type = t; question = q; }
        }

        private final List<Item> items = new ArrayList<>();

        GroupedQuestionAdapter(List<Question> rawQuestions) {
            Map<String, Map<String, List<Question>>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (Question q : rawQuestions) {
                String sub = q.subject == null || q.subject.trim().isEmpty() ? "General" : q.subject.trim();
                String cat = q.category == null || q.category.trim().isEmpty() ? "General" : q.category.trim();

                Map<String, List<Question>> catMap = map.get(sub);
                if (catMap == null) {
                    catMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    map.put(sub, catMap);
                }
                List<Question> qList = catMap.get(cat);
                if (qList == null) {
                    qList = new ArrayList<>();
                    catMap.put(cat, qList);
                }
                qList.add(q);
            }

            for (Map.Entry<String, Map<String, List<Question>>> subEntry : map.entrySet()) {
                items.add(new Item(TYPE_SUBJECT, subEntry.getKey()));
                for (Map.Entry<String, List<Question>> catEntry : subEntry.getValue().entrySet()) {
                    items.add(new Item(TYPE_CATEGORY, catEntry.getKey()));
                    for (Question q : catEntry.getValue()) items.add(new Item(TYPE_QUESTION, q));
                }
            }
        }

        @Override public int getItemViewType(int position) { return items.get(position).type; }

        @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context x = parent.getContext();
            if (viewType == TYPE_SUBJECT) {
                TextView tv = new TextView(x);
                tv.setTextSize(18);
                tv.setTypeface(null, Typeface.BOLD);
                tv.setTextColor(Ui.ACCENT);
                tv.setPadding(Ui.dp(x, 12), Ui.dp(x, 16), Ui.dp(x, 12), Ui.dp(x, 4));
                return new SubjectVH(tv);
            } else if (viewType == TYPE_CATEGORY) {
                TextView tv = new TextView(x);
                tv.setTextSize(15);
                tv.setTypeface(null, Typeface.BOLD);
                tv.setPadding(Ui.dp(x, 24), Ui.dp(x, 10), Ui.dp(x, 12), Ui.dp(x, 2));
                return new CategoryVH(tv);
            } else {
                LinearLayout card = Ui.card(x);
                return new QuestionVH(card);
            }
        }

        @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Item item = items.get(position);
            if (holder instanceof SubjectVH) {
                ((SubjectVH) holder).tv.setText("📚 " + item.title);
            } else if (holder instanceof CategoryVH) {
                ((CategoryVH) holder).tv.setText("📁 " + item.title);
            } else if (holder instanceof QuestionVH) {
                QuestionVH qvh = (QuestionVH) holder;
                qvh.box.removeAllViews();
                qvh.box.addView(Ui.text(qvh.box.getContext(), item.question.html, 14, true));
            }
        }

        @Override public int getItemCount() { return items.size(); }

        static class SubjectVH extends RecyclerView.ViewHolder { TextView tv; SubjectVH(TextView v) { super(v); tv = v; } }
        static class CategoryVH extends RecyclerView.ViewHolder { TextView tv; CategoryVH(TextView v) { super(v); tv = v; } }
        static class QuestionVH extends RecyclerView.ViewHolder { LinearLayout box; QuestionVH(LinearLayout v) { super(v); box = v; } }
    }
}
