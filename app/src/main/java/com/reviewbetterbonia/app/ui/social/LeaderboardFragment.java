package com.reviewbetterbonia.app.ui.social;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.*;
import com.google.firebase.firestore.DocumentSnapshot;
import com.reviewbetterbonia.app.R;
import com.reviewbetterbonia.app.model.QuizResult;
import com.reviewbetterbonia.app.ui.*;
import java.util.*;

public class LeaderboardFragment extends BaseFragment {
    private static final String MATH = "Math";
    private static final String MACHINE_DESIGN = "Machine Design";
    private static final String POWERPLANT = "Powerplant";
    private static final String RATING = "Rating";

    private String currentFilter = RATING;
    private List<Player> cachedPlayers = new ArrayList<>();
    private LinearLayout tableContainer, leaderCard;
    private TextView leaderNameView, leaderPointsView;
    private View currentBadgeView;
    private Button btnRating, btnMath, btnMachine, btnPower;

    @Nullable
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b) {
        Context x = requireContext();
        LinearLayout p = Ui.page(x);
        Ui.add(p, Ui.heading(x, "Leaderboard"), Ui.dp(x, 42));
        Ui.add(p, Ui.muted(x, "Filter leaders by rating points or subject mastery.", 14), Ui.dp(x, 26));

        HorizontalScrollView filterScroll = new HorizontalScrollView(x);
        filterScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filterRow = new LinearLayout(x);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterScroll.addView(filterRow);

        btnRating = filterButton(x, "Rating", RATING);
        btnMath = filterButton(x, "Math", MATH);
        btnMachine = filterButton(x, "Machine Design", MACHINE_DESIGN);
        btnPower = filterButton(x, "Powerplant", POWERPLANT);

        filterRow.addView(btnRating);
        filterRow.addView(Ui.gap(x, 8));
        filterRow.addView(btnMath);
        filterRow.addView(Ui.gap(x, 8));
        filterRow.addView(btnMachine);
        filterRow.addView(Ui.gap(x, 8));
        filterRow.addView(btnPower);

        p.addView(filterScroll);
        Ui.add(p, Ui.gap(x, 12), Ui.dp(x, 12));

        LinearLayout leader = Ui.card(x);
        leaderCard = leader;
        updateLeaderBadge();
        leaderNameView = Ui.text(x, "Loading…", 23, true);
        leader.addView(leaderNameView);
        leaderPointsView = Ui.muted(x, "", 15);
        leader.addView(leaderPointsView);
        p.addView(leader);
        Ui.add(p, Ui.gap(x, 12), Ui.dp(x, 12));

        HorizontalScrollView hsv = new HorizontalScrollView(x);
        tableContainer = new LinearLayout(x);
        tableContainer.setOrientation(LinearLayout.VERTICAL);
        tableContainer.setPadding(0, 0, 0, Ui.dp(x, 4));
        hsv.addView(tableContainer);
        Ui.addWeight(p, hsv);

        Ui.add(p, Ui.button(x, "Back", false), Ui.dp(x, 52));
        ((Button) p.getChildAt(p.getChildCount() - 1)).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        updateFilterButtons();
        updateLeaderBadge();
        loadLeaderboard(x);
        return p;
    }

    private void updateLeaderBadge() {
        if (leaderCard == null) return;
        if (currentBadgeView != null) {
            leaderCard.removeView(currentBadgeView);
        }

        String title;
        int bgColor;
        boolean glow;

        if (MATH.equals(currentFilter)) {
            title = "WIZARD";
            bgColor = Color.parseColor("#E53935");
            glow = false;
        } else if (MACHINE_DESIGN.equals(currentFilter)) {
            title = "MECHANIC";
            bgColor = Color.parseColor("#1E88E5");
            glow = false;
        } else if (POWERPLANT.equals(currentFilter)) {
            title = "ALCHEMIST";
            bgColor = Color.parseColor("#43A047");
            glow = false;
        } else {
            title = "GOAT";
            bgColor = Color.parseColor("#212121");
            glow = true;
        }

        currentBadgeView = createLeaderBadge(requireContext(), title, bgColor, glow);
        leaderCard.addView(currentBadgeView, 0);
    }

    private View createLeaderBadge(Context x, String text, int bgColor, boolean glow) {
        TextView tv = new TextView(x);
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        int padH = Ui.dp(x, 14);
        int padV = Ui.dp(x, 6);
        tv.setPadding(padH, padV, padH, padV);

        GradientDrawable oval = new GradientDrawable();
        oval.setShape(GradientDrawable.RECTANGLE);
        oval.setCornerRadius(Ui.dp(x, 18));
        oval.setColor(bgColor);
        tv.setBackground(oval);

        if (glow) {
            tv.setShadowLayer(14f, 0f, 0f, Color.YELLOW);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(0, 0, 0, Ui.dp(x, 6));
        tv.setLayoutParams(lp);

        return tv;
    }

    private Button filterButton(Context x, String label, String filterKey) {
        Button b = Ui.button(x, label, filterKey.equals(currentFilter));
        b.setOnClickListener(v -> {
            currentFilter = filterKey;
            updateFilterButtons();
            updateLeaderBadge();
            renderCached();
        });
        return b;
    }

    private void updateFilterButtons() {
        styleButton(btnRating, RATING.equals(currentFilter));
        styleButton(btnMath, MATH.equals(currentFilter));
        styleButton(btnMachine, MACHINE_DESIGN.equals(currentFilter));
        styleButton(btnPower, POWERPLANT.equals(currentFilter));
    }

    private void styleButton(Button b, boolean selected) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(selected ? Ui.ACCENT : Color.WHITE);
        g.setCornerRadius(Ui.dp(requireContext(), 24));
        g.setStroke(Ui.dp(requireContext(), 1), selected ? Ui.ACCENT : Ui.BORDER);
        b.setBackground(g);
        b.setTextColor(selected ? Color.WHITE : Ui.MUTED);
    }

    private void loadLeaderboard(Context x) {
        if (app().isOffline()) {
            cachedPlayers = aggregateLocal();
            renderCached();
            return;
        }

        app().firebase.db.collection("users").get()
                .addOnSuccessListener(userSnapshot -> {
                    Map<String, Player> map = new LinkedHashMap<>();
                    for (DocumentSnapshot d : userSnapshot) {
                        String uid = d.getId();
                        String name = d.getString("username");
                        if (uid == null || uid.trim().isEmpty()) continue;
                        Player player = new Player(uid, name == null || name.trim().isEmpty() ? "Player" : name);
                        Long rating = d.getLong("rankedRating");
                        player.rating = rating == null ? 0 : rating.intValue();
                        String email = d.getString("email");
                        if (email != null) player.email = email;
                        String role = d.getString("role");
                        if (role != null) player.role = role;
                        String rank = d.getString("rankedRank");
                        if (rank != null) player.rank = rank;
                        map.put(uid, player);
                    }

                    app().firebase.db.collection("results").get()
                            .addOnSuccessListener(snapshot -> {
                                for (DocumentSnapshot d : snapshot) {
                                    String uid = d.getString("userId");
                                    String subject = displaySubject(d.getString("subject"));
                                    Long score = d.getLong("score");
                                    if (uid == null || score == null) continue;
                                    if (!isTrackedSubject(subject)) continue;

                                    Player player = map.get(uid);
                                    if (player == null) {
                                        String name = d.getString("username");
                                        player = new Player(uid, name == null || name.trim().isEmpty() ? "Player" : name);
                                        map.put(uid, player);
                                    }
                                    player.add(subject, score.intValue());
                                }
                                cachedPlayers = new ArrayList<>(map.values());
                                renderCached();
                            })
                            .addOnFailureListener(e -> {
                                leaderNameView.setText("Leaderboard unavailable");
                                leaderPointsView.setText("Unable to load quiz results.");
                            });
                })
                .addOnFailureListener(e -> {
                    leaderNameView.setText("Leaderboard unavailable");
                    leaderPointsView.setText("Unable to load player ratings.");
                });
    }

    private List<Player> aggregateLocal() {
        Map<String, Player> map = new LinkedHashMap<>();
        String uid = app().user.uid == null ? "local" : app().user.uid;
        for (QuizResult r : app().local.results()) {
            String subject = displaySubject(r.subject);
            if (!isTrackedSubject(subject)) continue;
            Player p = map.get(uid);
            if (p == null) {
                p = new Player(uid, app().user.username == null ? "Player" : app().user.username);
                map.put(uid, p);
            }
            p.add(subject, r.score);
        }
        return new ArrayList<>(map.values());
    }

    private void renderCached() {
        Context x = requireContext();
        List<Player> allPlayers = new ArrayList<>(cachedPlayers);
        List<Player> players = new ArrayList<>();

        for (Player p : allPlayers) {
            if (MATH.equals(currentFilter)) {
                if (p.math > 0) players.add(p);
            } else if (MACHINE_DESIGN.equals(currentFilter)) {
                if (p.machineDesign > 0) players.add(p);
            } else if (POWERPLANT.equals(currentFilter)) {
                if (p.powerplant > 0) players.add(p);
            } else {
                if (p.rating > 0 || p.total > 0) players.add(p);
            }
        }

        Collections.sort(players, (a, b) -> {
            int cmp = 0;
            if (MATH.equals(currentFilter)) {
                cmp = Integer.compare(b.math, a.math);
            } else if (MACHINE_DESIGN.equals(currentFilter)) {
                cmp = Integer.compare(b.machineDesign, a.machineDesign);
            } else if (POWERPLANT.equals(currentFilter)) {
                cmp = Integer.compare(b.powerplant, a.powerplant);
            } else {
                cmp = Integer.compare(b.rating, a.rating);
                if (cmp == 0) cmp = Integer.compare(b.total, a.total);
            }
            if (cmp != 0) return cmp;
            return a.name.compareToIgnoreCase(b.name);
        });

        if (players.isEmpty()) {
            leaderNameView.setText("No scores yet");
            leaderPointsView.setText("Complete a Math, Machine Design, or Powerplant quiz to appear here.");
            tableContainer.removeAllViews();
            return;
        }

        Player leader = players.get(0);
        leaderNameView.setText(leader.name);
        String leaderMetric;
        if (MATH.equals(currentFilter)) leaderMetric = leader.math + " Math points";
        else if (MACHINE_DESIGN.equals(currentFilter)) leaderMetric = leader.machineDesign + " Machine Design points";
        else if (POWERPLANT.equals(currentFilter)) leaderMetric = leader.powerplant + " Powerplant points";
        else leaderMetric = leader.rating + " rating points";

        leaderPointsView.setText(leaderMetric + "  •  Math " + leader.math + "  •  MD " + leader.machineDesign + "  •  Power " + leader.powerplant);

        tableContainer.removeAllViews();
        tableContainer.addView(header(x));
        for (int i = 0; i < players.size(); i++) {
            tableContainer.addView(row(x, players.get(i), i + 1));
        }
    }

    private LinearLayout header(Context x) {
        LinearLayout r = new LinearLayout(x);
        r.setOrientation(LinearLayout.HORIZONTAL);
        if (MATH.equals(currentFilter)) {
            addCell(r, x, "PLAYER", 240, true);
            addCell(r, x, "MATH", 120, true);
        } else if (MACHINE_DESIGN.equals(currentFilter)) {
            addCell(r, x, "PLAYER", 200, true);
            addCell(r, x, "MACHINE DESIGN", 160, true);
        } else if (POWERPLANT.equals(currentFilter)) {
            addCell(r, x, "PLAYER", 200, true);
            addCell(r, x, "POWERPLANT", 160, true);
        } else {
            addCell(r, x, "PLAYER", 240, true);
            addCell(r, x, "RATING", 120, true);
        }
        return r;
    }

    private LinearLayout row(Context x, Player p, int rank) {
        LinearLayout r = new LinearLayout(x);
        r.setOrientation(LinearLayout.HORIZONTAL);
        if (MATH.equals(currentFilter)) {
            addCell(r, x, rank + ".  " + p.name, 240, false);
            addCell(r, x, String.valueOf(p.math), 120, true);
        } else if (MACHINE_DESIGN.equals(currentFilter)) {
            addCell(r, x, rank + ".  " + p.name, 200, false);
            addCell(r, x, String.valueOf(p.machineDesign), 160, true);
        } else if (POWERPLANT.equals(currentFilter)) {
            addCell(r, x, rank + ".  " + p.name, 200, false);
            addCell(r, x, String.valueOf(p.powerplant), 160, true);
        } else {
            addCell(r, x, rank + ".  " + p.name, 240, false);
            addCell(r, x, String.valueOf(p.rating), 120, true);
        }
        r.setPadding(0, Ui.dp(x, 6), 0, Ui.dp(x, 6));

        r.setClickable(true);
        r.setFocusable(true);
        r.setOnClickListener(v -> app().navigate(ProfileFragment.forUser(p.uid, p.name, p.email, p.role, p.rating, p.rank), true));
        return r;
    }

    private void addCell(LinearLayout row, Context x, String value,
                          int widthDp, boolean bold) {
        TextView v = Ui.text(x, value, 13, bold);
        v.setMaxLines(2);
        v.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(v, new LinearLayout.LayoutParams(Ui.dp(x, widthDp), Ui.dp(x, 48)));
    }

    private String displaySubject(String subject) {
        if (subject == null) return "";
        if (subject.equalsIgnoreCase("Algebra") || subject.equalsIgnoreCase("Math")) return MATH;
        if (subject.equalsIgnoreCase("Machine Design")) return MACHINE_DESIGN;
        if (subject.equalsIgnoreCase("Powerplant") || subject.equalsIgnoreCase("Power Plant")) return POWERPLANT;
        return subject;
    }

    private boolean isTrackedSubject(String subject) {
        return MATH.equals(subject) || MACHINE_DESIGN.equals(subject) || POWERPLANT.equals(subject);
    }

    private static class Player {
        final String uid;
        final String name;
        String email = "";
        String role = "student";
        String rank = "Freshman";
        int rating;
        int total, math, machineDesign, powerplant;

        Player(String uid, String name) {
            this.uid = uid;
            this.name = name;
        }

        void add(String subject, int points) {
            total += points;
            if (MATH.equals(subject)) math += points;
            else if (MACHINE_DESIGN.equals(subject)) machineDesign += points;
            else if (POWERPLANT.equals(subject)) powerplant += points;
        }
    }
}
