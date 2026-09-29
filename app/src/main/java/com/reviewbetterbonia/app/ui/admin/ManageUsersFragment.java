package com.reviewbetterbonia.app.ui.admin;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import com.google.firebase.firestore.DocumentSnapshot;
import com.reviewbetterbonia.app.ui.*;
import com.reviewbetterbonia.app.ui.social.ProfileFragment;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageUsersFragment extends BaseFragment {
    private LinearLayout container;
    private EditText searchInput;
    private final List<UserData> cachedUsers = new ArrayList<>();

    private static class UserData {
        String uid, userName, email, role, rank;
        int rating;
        DocumentSnapshot doc;
        UserData(String uid, String userName, String email, String role, String rank, int rating, DocumentSnapshot doc) {
            this.uid = uid; this.userName = userName; this.email = email; this.role = role; this.rank = rank; this.rating = rating; this.doc = doc;
        }
    }

    @Nullable
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b) {
        Context x = requireContext();
        LinearLayout p = Ui.page(x);
        Ui.add(p, Ui.heading(x, "Manage Users"), Ui.dp(x, 42));
        Ui.add(p, Ui.muted(x, "View user profiles, search accounts, or manage user data.", 14), Ui.dp(x, 35));

        LinearLayout searchRow = new LinearLayout(x);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);

        searchInput = new EditText(x);
        searchInput.setHint("Search username or email...");
        searchInput.setSingleLine();
        searchRow.addView(searchInput, new LinearLayout.LayoutParams(0, Ui.dp(x, 52), 1));
        searchRow.addView(Ui.gap(x, 8), new LinearLayout.LayoutParams(Ui.dp(x, 8), -2));

        Button searchBtn = Ui.button(x, "Search", true);
        searchBtn.setOnClickListener(v -> renderUsers(x, cachedUsers, searchInput.getText().toString().trim()));
        searchRow.addView(searchBtn, new LinearLayout.LayoutParams(-2, Ui.dp(x, 52)));
        p.addView(searchRow);
        Ui.add(p, Ui.gap(x, 10), Ui.dp(x, 10));

        ScrollView sv = new ScrollView(x);
        container = new LinearLayout(x);
        container.setOrientation(LinearLayout.VERTICAL);
        sv.addView(container);
        Ui.addWeight(p, sv);

        Button back = Ui.button(x, "Back", false);
        back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        Ui.add(p, back, Ui.dp(x, 52));

        loadUsers(x);
        return p;
    }

    private void loadUsers(Context x) {
        if (app().isOffline()) {
            container.removeAllViews();
            container.addView(Ui.muted(x, "User management requires online Firebase mode.", 15));
            return;
        }

        container.removeAllViews();
        container.addView(Ui.muted(x, "Loading users…", 15));

        app().firebase.db.collection("users").get().addOnSuccessListener(snapshot -> {
            cachedUsers.clear();
            if (snapshot.isEmpty()) {
                container.removeAllViews();
                container.addView(Ui.muted(x, "No users found.", 15));
                return;
            }

            for (DocumentSnapshot d : snapshot) {
                String uid = d.getId();
                String rawName = d.getString("username");
                final String userName = (rawName == null || rawName.isEmpty()) ? "Player" : rawName;
                String email = d.getString("email");
                String role = d.getString("role");
                if (role == null) role = "student";

                Long ratingLong = d.getLong("rankedRating");
                final int rating = ratingLong == null ? 0 : ratingLong.intValue();
                String rawRank = d.getString("rankedRank");
                final String rank = (rawRank == null || rawRank.isEmpty()) ? "Freshman" : rawRank;
                final String finalEmail = email == null ? "" : email;
                final String finalRole = role;

                cachedUsers.add(new UserData(uid, userName, finalEmail, finalRole, rank, rating, d));
            }

            String currentQuery = searchInput != null ? searchInput.getText().toString().trim() : "";
            renderUsers(x, cachedUsers, currentQuery);
        }).addOnFailureListener(e -> {
            container.removeAllViews();
            container.addView(Ui.muted(x, "Failed to load users: " + e.getMessage(), 15));
        });
    }

    private void renderUsers(Context x, List<UserData> users, String query) {
        container.removeAllViews();
        String q = query.toLowerCase(Locale.US);

        List<UserData> filtered = new ArrayList<>();
        for (UserData u : users) {
            if (q.isEmpty() || u.userName.toLowerCase(Locale.US).contains(q) || u.email.toLowerCase(Locale.US).contains(q)) {
                filtered.add(u);
            }
        }

        if (filtered.isEmpty()) {
            container.addView(Ui.muted(x, "No matching users found.", 15));
            return;
        }

        for (UserData u : filtered) {
            boolean isMe = u.uid.equals(app().user.uid);

            LinearLayout card = Ui.card(x);
            card.setOrientation(LinearLayout.VERTICAL);

            TextView nameView = Ui.text(x, u.userName + (isMe ? " (You)" : ""), 17, true);
            TextView infoView = Ui.muted(x, "Email: " + (!u.email.isEmpty() ? u.email : "N/A") + " • Role: " + u.role + "\nRank: " + u.rank + " (" + u.rating + " RP)", 13);

            LinearLayout textCol = new LinearLayout(x);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.addView(nameView);
            textCol.addView(infoView);
            card.addView(textCol);
            card.addView(Ui.gap(x, 8));

            LinearLayout btnRow = new LinearLayout(x);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);

            Button viewProfileBtn = Ui.button(x, "Profile", true);
            viewProfileBtn.setOnClickListener(v -> {
                app().navigate(ProfileFragment.forUser(u.uid, u.userName, u.email, u.role, u.rating, u.rank), true);
            });
            btnRow.addView(viewProfileBtn, new LinearLayout.LayoutParams(0, Ui.dp(x, 42), 1));
            btnRow.addView(Ui.gap(x, 6), new LinearLayout.LayoutParams(Ui.dp(x, 6), -2));

            Button clearBtn = Ui.button(x, "Clear Data", false);
            clearBtn.setBackgroundColor(Color.parseColor("#FB8C00"));
            clearBtn.setTextColor(Color.WHITE);
            clearBtn.setOnClickListener(v -> {
                new AlertDialog.Builder(x)
                    .setTitle("Clear Progress Data?")
                    .setMessage("This will reset rating points to 0 and clear all quiz history for '" + u.userName + "', keeping their account active.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Clear", (dialog, which) -> {
                        app().firebase.clearUserData(u.uid, ok -> {
                            Toast.makeText(x, ok ? "User progress data cleared." : "Could not clear data.", Toast.LENGTH_SHORT).show();
                            if(isMe){
                                app().rating = 0;
                                app().rank = "Freshman";
                                app().local.replaceResults(new ArrayList<>());
                                app().local.prefs().edit().putInt("ranked_rating", 0).apply();
                            }
                            loadUsers(x);
                        });
                    })
                    .show();
            });
            btnRow.addView(clearBtn, new LinearLayout.LayoutParams(0, Ui.dp(x, 42), 1));
            btnRow.addView(Ui.gap(x, 6), new LinearLayout.LayoutParams(Ui.dp(x, 6), -2));

            Button del = Ui.button(x, "Delete", false);
            del.setBackgroundColor(Color.parseColor("#E53935"));
            del.setTextColor(Color.WHITE);
            del.setOnClickListener(v -> {
                new AlertDialog.Builder(x)
                    .setTitle("Delete User Account?")
                    .setMessage("Are you sure you want to completely delete user account '" + u.userName + "'?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, which) -> {
                        app().firebase.db.collection("users").document(u.uid).delete().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(x, "User account deleted.", Toast.LENGTH_SHORT).show();
                                if(isMe){
                                    app().signOut();
                                }else{
                                    loadUsers(x);
                                }
                            } else {
                                Toast.makeText(x, "Failed to delete user account.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .show();
            });
            btnRow.addView(del, new LinearLayout.LayoutParams(0, Ui.dp(x, 42), 1));

            card.addView(btnRow);
            container.addView(card);
            container.addView(Ui.gap(x, 8));
        }
    }
}
