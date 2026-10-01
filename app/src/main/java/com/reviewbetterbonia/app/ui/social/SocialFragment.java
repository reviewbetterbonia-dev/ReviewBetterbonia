package com.reviewbetterbonia.app.ui.social;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import com.reviewbetterbonia.app.data.FirebaseRepository;
import com.reviewbetterbonia.app.ui.*;
import java.util.*;

public class SocialFragment extends BaseFragment {
    private LinearLayout requestBox, friendBox, allUsersContainer;
    private ScrollView allUsersScrollView;
    private EditText query;
    private final Set<String> currentFriendUids = new HashSet<>();
    private List<FirebaseRepository.UserRow> allUsersList = new ArrayList<>();
    private Map<String, List<String>> leaderTitlesMap = new HashMap<>();

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && app().user != null && !app().isOffline()
                && requestBox != null && friendBox != null) {
            loadData();
        }
    }

    @Nullable
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b) {
        Context x = requireContext();
        LinearLayout p = Ui.page(x);
        Ui.add(p, Ui.heading(x, "Social"), Ui.dp(x, 42));
        Ui.add(p, Ui.muted(x, "Discover all users, search classmates, and manage your friends.", 14), Ui.dp(x, 38));

        if (app().isOffline()) {
            LinearLayout card = Ui.card(x);
            card.addView(Ui.text(x, "Offline Mode", 18, true));
            card.addView(Ui.muted(x, "Social features, user directory, and online sync require internet and Firebase connection.", 13));
            p.addView(card);
        } else {
            ScrollView sv = new ScrollView(x);
            LinearLayout content = new LinearLayout(x);
            content.setOrientation(LinearLayout.VERTICAL);
            sv.addView(content);
            Ui.addWeight(p, sv);

            content.addView(Ui.text(x, "All Users & Search", 19, true));
            content.addView(Ui.gap(x, 6));

            LinearLayout searchRow = new LinearLayout(x);
            searchRow.setOrientation(LinearLayout.HORIZONTAL);
            query = new EditText(x);
            query.setHint("Search username or email...");
            query.setSingleLine();
            searchRow.addView(query, new LinearLayout.LayoutParams(0, Ui.dp(x, 52), 1));
            searchRow.addView(Ui.gap(x, 8), new LinearLayout.LayoutParams(Ui.dp(x, 8), -2));

            Button searchBtn = Ui.button(x, "Search", true);
            searchBtn.setOnClickListener(v -> renderAllUsers(x, query.getText().toString().trim()));
            searchRow.addView(searchBtn, new LinearLayout.LayoutParams(-2, Ui.dp(x, 52)));
            content.addView(searchRow);
            content.addView(Ui.gap(x, 12));

            allUsersContainer = Ui.card(x);
            allUsersScrollView = new ScrollView(x);
            allUsersScrollView.setVerticalScrollBarEnabled(true);
            allUsersScrollView.setOnTouchListener((v, event) -> {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    if (action == MotionEvent.ACTION_UP) {
                        v.performClick();
                    }
                }
                return false;
            });
            allUsersContainer.addView(allUsersScrollView, new LinearLayout.LayoutParams(-1, -1));
            content.addView(allUsersContainer);
            content.addView(Ui.gap(x, 16));

            content.addView(Ui.text(x, "Friend requests", 19, true));
            content.addView(Ui.gap(x, 6));
            requestBox = Ui.card(x);
            content.addView(requestBox);
            content.addView(Ui.gap(x, 16));

            content.addView(Ui.text(x, "Your friends", 19, true));
            content.addView(Ui.gap(x, 6));
            friendBox = Ui.card(x);
            content.addView(friendBox);

            loadData();
        }

        Button back = Ui.button(x, "Back", false);
        back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        Ui.add(p, back, Ui.dp(x, 52));
        return p;
    }

    private void loadData() {
        app().firebase.getLeaderTitles(titlesMap -> {
            if (!isAdded()) return;
            leaderTitlesMap = titlesMap;
            loadRequests();
            loadFriends();
            loadAllUsers();
        });
    }

    private void loadAllUsers() {
        app().firebase.loadAllUsers(app().user.uid, users -> {
            if (!isAdded()) return;
            allUsersList = users;
            renderAllUsers(requireContext(), query != null ? query.getText().toString().trim() : "");
        });
    }

    private void renderAllUsers(Context x, String term) {
        if (allUsersContainer == null || allUsersScrollView == null) return;
        allUsersScrollView.removeAllViews();

        LinearLayout usersLayout = new LinearLayout(x);
        usersLayout.setOrientation(LinearLayout.VERTICAL);

        String q = term.toLowerCase(Locale.US);

        List<FirebaseRepository.UserRow> filtered = new ArrayList<>();
        for (FirebaseRepository.UserRow u : allUsersList) {
            if (q.isEmpty() || u.username.toLowerCase(Locale.US).contains(q) || u.email.toLowerCase(Locale.US).contains(q)) {
                filtered.add(u);
            }
        }

        if (filtered.isEmpty()) {
            usersLayout.addView(Ui.muted(x, "No users found.", 13));
            allUsersScrollView.addView(usersLayout);
            allUsersContainer.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            return;
        }

        for (FirebaseRepository.UserRow u : filtered) {
            LinearLayout row = new LinearLayout(x);
            row.setOrientation(LinearLayout.VERTICAL);

            LinearLayout topRow = new LinearLayout(x);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout textCol = new LinearLayout(x);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.addView(Ui.text(x, "👤 " + u.username, 17, true));
            textCol.addView(Ui.muted(x, u.rank + " • " + u.rating + " RP", 13));
            topRow.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1));

            LinearLayout titleRow = new LinearLayout(x);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            List<String> titles = leaderTitlesMap.get(u.uid);
            if (titles != null) {
                for (String t : titles) {
                    if ("GOAT".equals(t)) {
                        titleRow.addView(createBadge(x, "GOAT", Color.parseColor("#212121"), true));
                    } else if ("Wizard".equals(t)) {
                        titleRow.addView(createBadge(x, "Wizard", Color.parseColor("#E53935"), false));
                    } else if ("Mechanic".equals(t)) {
                        titleRow.addView(createBadge(x, "Mechanic", Color.parseColor("#1E88E5"), false));
                    } else if ("Alchemist".equals(t)) {
                        titleRow.addView(createBadge(x, "Alchemist", Color.parseColor("#43A047"), false));
                    }
                }
            }
            topRow.addView(titleRow);
            row.addView(topRow);
            row.addView(Ui.gap(x, 8));

            LinearLayout btnRow = new LinearLayout(x);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);

            Button profileBtn = Ui.button(x, "Profile", false);
            GradientDrawable g = new GradientDrawable();
            g.setColor(Color.parseColor("#3F51B5"));
            g.setCornerRadius(Ui.dp(x, 20));
            profileBtn.setBackground(g);
            profileBtn.setTextColor(Color.WHITE);
            profileBtn.setOnClickListener(v -> app().navigate(ProfileFragment.forUser(u.uid, u.username, u.email, u.role, u.rating, u.rank), true));
            btnRow.addView(profileBtn, new LinearLayout.LayoutParams(0, Ui.dp(x, 40), 1));
            btnRow.addView(Ui.gap(x, 6), new LinearLayout.LayoutParams(Ui.dp(x, 6), -2));

            if (currentFriendUids.contains(u.uid)) {
                Button b = Ui.button(x, "Friends", false);
                b.setEnabled(false);
                btnRow.addView(b, new LinearLayout.LayoutParams(0, Ui.dp(x, 40), 1));
            } else {
                Button b = Ui.button(x, "Add friend", false);
                btnRow.addView(b, new LinearLayout.LayoutParams(0, Ui.dp(x, 40), 1));
                b.setOnClickListener(v -> app().firebase.sendFriendRequest(app().user.uid, u.uid, app().user.username, ok -> {
                    Toast.makeText(requireContext(), ok ? "Friend request sent." : "Could not send request.", Toast.LENGTH_SHORT).show();
                    if (ok) {
                        b.setText("Request Sent");
                        b.setEnabled(false);
                    }
                }));
            }

            row.addView(btnRow);
            usersLayout.addView(row);
            usersLayout.addView(Ui.gap(x, 10));
        }

        allUsersScrollView.addView(usersLayout);

        if (filtered.size() > 3) {
            allUsersContainer.setLayoutParams(new LinearLayout.LayoutParams(-1, Ui.dp(x, 320)));
        } else {
            allUsersContainer.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private View createBadge(Context x, String text, int bgColor, boolean glow) {
        TextView tv = new TextView(x);
        tv.setText(text);
        tv.setTextSize(10);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        int padH = Ui.dp(x, 8);
        int padV = Ui.dp(x, 3);
        tv.setPadding(padH, padV, padH, padV);

        GradientDrawable oval = new GradientDrawable();
        oval.setShape(GradientDrawable.RECTANGLE);
        oval.setCornerRadius(Ui.dp(x, 14));
        oval.setColor(bgColor);
        tv.setBackground(oval);

        if (glow) {
            tv.setShadowLayer(12f, 0f, 0f, Color.YELLOW);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMargins(0, 0, Ui.dp(x, 8), 0);
        tv.setLayoutParams(params);

        return tv;
    }

    private void loadRequests() {
        app().firebase.loadFriendRequests(app().user.uid, reqs -> {
            if (!isAdded() || requestBox == null) return;
            requestBox.removeAllViews();
            if (reqs.isEmpty()) {
                requestBox.addView(Ui.muted(requireContext(), "No pending requests.", 13));
                return;
            }
            for (FirebaseRepository.RequestRow r : reqs) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                TextView t = Ui.text(requireContext(), r.username, 16, true);
                row.addView(t, new LinearLayout.LayoutParams(0, Ui.dp(requireContext(), 52), 1));

                Button accept = Ui.button(requireContext(), "Accept", false);
                row.addView(accept);

                Button decline = Ui.button(requireContext(), "Decline", false);
                row.addView(Ui.gap(requireContext(), 6));
                row.addView(decline);

                requestBox.addView(row);

                accept.setOnClickListener(v -> {
                    accept.setEnabled(false);
                    decline.setEnabled(false);
                    app().firebase.acceptFriendRequest(app().user.uid, r.requestId, r.fromUid, ok -> {
                        if (!isAdded()) return;
                        if (ok) {
                            Toast.makeText(requireContext(), "Friend request accepted!", Toast.LENGTH_SHORT).show();
                            loadData();
                        } else {
                            accept.setEnabled(true);
                            decline.setEnabled(true);
                            Toast.makeText(requireContext(), "Could not accept request.", Toast.LENGTH_SHORT).show();
                        }
                    });
                });

                decline.setOnClickListener(v -> {
                    accept.setEnabled(false);
                    decline.setEnabled(false);
                    app().firebase.rejectFriendRequest(app().user.uid, r.requestId, ok -> {
                        if (!isAdded()) return;
                        if (ok) {
                            loadRequests();
                        } else {
                            accept.setEnabled(true);
                            decline.setEnabled(true);
                            Toast.makeText(requireContext(), "Could not decline request.", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        });
    }

    private void loadFriends() {
        app().firebase.loadFriends(app().user.uid, users -> {
            if (!isAdded() || friendBox == null) return;
            currentFriendUids.clear();
            friendBox.removeAllViews();
            if (users.isEmpty()) {
                friendBox.addView(Ui.muted(requireContext(), "No friends yet.", 13));
                return;
            }
            for (FirebaseRepository.UserRow u : users) {
                currentFriendUids.add(u.uid);

                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);

                LinearLayout textCol = new LinearLayout(requireContext());
                textCol.setOrientation(LinearLayout.VERTICAL);
                textCol.addView(Ui.text(requireContext(), "👤 " + u.username, 17, true));
                textCol.addView(Ui.muted(requireContext(), u.rank + " • " + u.rating + " RP", 13));
                row.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1));

                Button viewBtn = Ui.button(requireContext(), "Profile", false);
                GradientDrawable g = new GradientDrawable();
                g.setColor(Color.parseColor("#3F51B5"));
                g.setCornerRadius(Ui.dp(requireContext(), 20));
                viewBtn.setBackground(g);
                viewBtn.setTextColor(Color.WHITE);
                viewBtn.setOnClickListener(v -> app().navigate(ProfileFragment.forUser(u.uid, u.username, u.email, u.role, u.rating, u.rank), true));
                row.addView(viewBtn, new LinearLayout.LayoutParams(-2, Ui.dp(requireContext(), 40)));

                Button removeBtn = Ui.button(requireContext(), "Remove", false);
                GradientDrawable g2 = new GradientDrawable();
                g2.setColor(Color.parseColor("#D32F2F"));
                g2.setCornerRadius(Ui.dp(requireContext(), 20));
                removeBtn.setBackground(g2);
                removeBtn.setTextColor(Color.WHITE);
                removeBtn.setOnClickListener(v -> app().firebase.removeFriend(app().user.uid, u.uid, ok -> {
                    if (ok) {
                        loadData();
                    } else {
                        Toast.makeText(requireContext(), "Could not remove friend.", Toast.LENGTH_SHORT).show();
                    }
                }));
                row.addView(Ui.gap(requireContext(), 6));
                row.addView(removeBtn, new LinearLayout.LayoutParams(-2, Ui.dp(requireContext(), 40)));

                friendBox.addView(row);
                friendBox.addView(Ui.gap(requireContext(), 8));
            }
            renderAllUsers(requireContext(), query != null ? query.getText().toString().trim() : "");
        });
    }
}
