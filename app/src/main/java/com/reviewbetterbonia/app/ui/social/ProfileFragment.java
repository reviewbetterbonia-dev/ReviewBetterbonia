package com.reviewbetterbonia.app.ui.social;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.*;
import com.reviewbetterbonia.app.model.*;
import com.reviewbetterbonia.app.ui.*;
import java.util.*;

public class ProfileFragment extends BaseFragment {
    private String currentTimeFilter = "All Time";
    private String currentModeFilter = "Overall";
    private List<QuizResult> cachedResults = new ArrayList<>();
    private LinearLayout overallCard, timeRow, modeRow;
    private RecyclerView subjectList;
    private Button btnAllTime, btnMonth, btnWeek;
    private Button btnOverall, btnRanked, btnCasual;

    private String targetUid, targetUsername, targetEmail, targetRole, targetRank;
    private int targetRating = 0;
    private TextView nameHeaderView, emailHeaderView, rankHeaderView, statsHeaderView, subjectSectionHeader;
    private LinearLayout titleBadgeContainer;
    private boolean canViewDetails = true;

    public static ProfileFragment forUser(String uid, String username, String email, String role, int rating, String rank) {
        ProfileFragment f = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("uid", uid);
        args.putString("username", username);
        args.putString("email", email);
        args.putString("role", role);
        args.putInt("rating", rating);
        args.putString("rank", rank);
        f.setArguments(args);
        return f;
    }

    private boolean isOther() {
        return targetUid != null && app().user != null && !targetUid.equals(app().user.uid);
    }

    @Nullable
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b) {
        Bundle args = getArguments();
        if (args != null && args.containsKey("uid") && app().user != null && !app().user.uid.equals(args.getString("uid"))) {
            targetUid = args.getString("uid");
            targetUsername = args.getString("username", "Player");
            targetEmail = args.getString("email", "");
            targetRole = args.getString("role", "student");
            targetRank = args.getString("rank", "Freshman");
            targetRating = args.getInt("rating", 0);
        } else {
            targetUid = app().user.uid;
            targetUsername = app().user.username;
            targetEmail = app().user.email;
            targetRole = app().user.role;
            targetRank = app().rank;
            targetRating = app().rating;
        }

        Context x = requireContext();
        LinearLayout p = Ui.page(x);
        Ui.add(p, Ui.heading(x, isOther() ? targetUsername + "'s Profile" : "Profile & Stats"), Ui.dp(x, 42));

        LinearLayout card = Ui.card(x);
        LinearLayout nameRow = new LinearLayout(x);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textCol = new LinearLayout(x);
        textCol.setOrientation(LinearLayout.VERTICAL);
        nameHeaderView = Ui.text(x, "👤 " + targetUsername, 23, true);
        textCol.addView(nameHeaderView);
        emailHeaderView = Ui.muted(x, targetEmail != null ? targetEmail : "", 14);
        if (targetEmail != null && !targetEmail.isEmpty()) {
            textCol.addView(emailHeaderView);
        }
        nameRow.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1));

        titleBadgeContainer = new LinearLayout(x);
        titleBadgeContainer.setOrientation(LinearLayout.HORIZONTAL);
        titleBadgeContainer.setGravity(Gravity.CENTER_VERTICAL);
        nameRow.addView(titleBadgeContainer);

        card.addView(nameRow);
        card.addView(Ui.gap(x, 4));
        rankHeaderView = Ui.text(x, "Rank: " + targetRank, 18, true);
        card.addView(rankHeaderView);
        statsHeaderView = Ui.muted(x, "Rating: " + targetRating + " RP  •  Role: " + targetRole, 13);
        card.addView(statsHeaderView);
        p.addView(card);
        Ui.add(p, Ui.gap(x, 12), Ui.dp(x, 12));

        timeRow = new LinearLayout(x);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        btnAllTime = filterButton(x, "All Time", "All Time", true);
        btnMonth = filterButton(x, "This Month", "This Month", true);
        btnWeek = filterButton(x, "This Week", "This Week", true);
        timeRow.addView(btnAllTime);
        timeRow.addView(Ui.gap(x, 8));
        timeRow.addView(btnMonth);
        timeRow.addView(Ui.gap(x, 8));
        timeRow.addView(btnWeek);
        p.addView(timeRow);
        Ui.add(p, Ui.gap(x, 8), Ui.dp(x, 8));

        modeRow = new LinearLayout(x);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        btnOverall = filterButton(x, "Overall", "Overall", false);
        btnRanked = filterButton(x, "Ranked", "Ranked", false);
        btnCasual = filterButton(x, "Casual", "Casual", false);
        modeRow.addView(btnOverall);
        modeRow.addView(Ui.gap(x, 8));
        modeRow.addView(btnRanked);
        modeRow.addView(Ui.gap(x, 8));
        modeRow.addView(btnCasual);
        p.addView(modeRow);
        Ui.add(p, Ui.gap(x, 8), Ui.dp(x, 8));

        if (!isOther()) {
            Button clearCasualBtn = Ui.button(x, "Clear Casual Progress", false);
            GradientDrawable g = new GradientDrawable();
            g.setColor(Color.parseColor("#FB8C00"));
            g.setCornerRadius(Ui.dp(x, 24));
            g.setStroke(Ui.dp(x, 1), Color.parseColor("#FB8C00"));
            clearCasualBtn.setBackground(g);
            clearCasualBtn.setTextColor(Color.WHITE);
            clearCasualBtn.setOnClickListener(v -> {
                new AlertDialog.Builder(x)
                    .setTitle("Clear Casual Progress?")
                    .setMessage("This will remove all your casual and practice quiz results, keeping your ranked record intact. Continue?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Clear", (dialog, which) -> {
                        long now = System.currentTimeMillis();
                        app().local.setCasualClearedTime(now);
                        List<QuizResult> kept = filterClearedResults(cachedResults);
                        cachedResults = kept;
                        app().local.replaceResults(kept);
                        Toast.makeText(x, "Casual progress cleared.", Toast.LENGTH_SHORT).show();
                        renderFiltered();

                        if (!app().isOffline()) {
                            app().firebase.clearCasualResults(app().user.uid, ok -> {});
                        }
                    })
                    .show();
            });
            p.addView(clearCasualBtn);
            Ui.add(p, Ui.gap(x, 8), Ui.dp(x, 8));
        }

        overallCard = Ui.card(x);
        p.addView(overallCard);
        Ui.add(p, Ui.gap(x, 12), Ui.dp(x, 12));

        subjectSectionHeader = Ui.text(x, "Performance by subject", 20, true);
        Ui.add(p, subjectSectionHeader, Ui.dp(x, 35));

        subjectList = new RecyclerView(x);
        subjectList.setLayoutManager(new LinearLayoutManager(x));
        Ui.addWeight(p, subjectList);

        Button back = Ui.button(x, "Back", false);
        back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        Ui.add(p, back, Ui.dp(x, 52));

        updateButtons();
        load();
        return p;
    }

    private Button filterButton(Context x, String label, String filterKey, boolean isTime) {
        boolean selected = isTime ? filterKey.equals(currentTimeFilter) : filterKey.equals(currentModeFilter);
        Button b = Ui.button(x, label, selected);
        b.setOnClickListener(v -> {
            if (isTime) {
                currentTimeFilter = filterKey;
            } else {
                currentModeFilter = filterKey;
            }
            updateButtons();
            renderFiltered();
        });
        return b;
    }

    private void updateButtons() {
        styleButton(btnAllTime, "All Time".equals(currentTimeFilter));
        styleButton(btnMonth, "This Month".equals(currentTimeFilter));
        styleButton(btnWeek, "This Week".equals(currentTimeFilter));

        styleButton(btnOverall, "Overall".equals(currentModeFilter));
        styleButton(btnRanked, "Ranked".equals(currentModeFilter));
        styleButton(btnCasual, "Casual".equals(currentModeFilter));
    }

    private void styleButton(Button b, boolean selected) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(selected ? Ui.ACCENT : Color.WHITE);
        g.setCornerRadius(Ui.dp(requireContext(), 24));
        g.setStroke(Ui.dp(requireContext(), 1), selected ? Ui.ACCENT : Ui.BORDER);
        b.setBackground(g);
        b.setTextColor(selected ? Color.WHITE : Ui.MUTED);
    }

    private void updateHeaderViews() {
        if (nameHeaderView != null) nameHeaderView.setText("👤 " + targetUsername);
        if (emailHeaderView != null) {
            emailHeaderView.setText(targetEmail != null ? targetEmail : "");
            emailHeaderView.setVisibility(targetEmail != null && !targetEmail.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (rankHeaderView != null) rankHeaderView.setText("Rank: " + targetRank);
        if (statsHeaderView != null) statsHeaderView.setText("Rating: " + targetRating + " RP  •  Role: " + targetRole);
    }

    private void load() {
        checkTitles(titleBadgeContainer);

        if (app().isOffline()) {
            canViewDetails = !isOther() || app().isAdmin();
            fetchProfileData();
            return;
        }

        if (!isOther() || app().isAdmin()) {
            canViewDetails = true;
            fetchProfileData();
        } else {
            app().firebase.db.collection("users").document(app().user.uid)
                    .collection("friends").document(targetUid).get()
                    .addOnCompleteListener(t -> {
                        boolean isFriend = t.isSuccessful() && t.getResult() != null && t.getResult().exists();
                        canViewDetails = isFriend;
                        fetchProfileData();
                    });
        }
    }

    private void fetchProfileData() {
        if (!isAdded()) return;

        if (isOther()) {
            app().firebase.db.collection("users").document(targetUid).get().addOnSuccessListener(doc -> {
                if (doc != null && doc.exists()) {
                    String name = doc.getString("username");
                    if (name != null && !name.trim().isEmpty()) targetUsername = name;
                    String email = doc.getString("email");
                    if (email != null) targetEmail = email;
                    String role = doc.getString("role");
                    if (role != null && !role.trim().isEmpty()) targetRole = role;
                    Long rLong = doc.getLong("rankedRating");
                    if (rLong != null) targetRating = rLong.intValue();
                    String rRank = doc.getString("rankedRank");
                    if (rRank != null && !rRank.trim().isEmpty()) targetRank = rRank;
                    updateHeaderViews();
                }
            });
        }

        if (canViewDetails) {
            app().firebase.syncResults(targetUid, rs -> {
                if (!isAdded()) return;
                List<QuizResult> clean = isOther() ? rs : filterClearedResults(rs);
                if (!isOther()) app().local.replaceResults(clean);
                cachedResults = clean;
                renderFiltered();
            });
        } else {
            renderPrivateProfile();
        }
    }

    private void renderPrivateProfile() {
        Context x = requireContext();
        if (timeRow != null) timeRow.setVisibility(View.GONE);
        if (modeRow != null) modeRow.setVisibility(View.GONE);
        if (subjectSectionHeader != null) subjectSectionHeader.setVisibility(View.GONE);
        if (subjectList != null) subjectList.setVisibility(View.GONE);

        overallCard.removeAllViews();
        overallCard.addView(Ui.text(x, "🔒 Private Profile", 20, true));
        overallCard.addView(Ui.gap(x, 6));
        overallCard.addView(Ui.muted(x, "You must be friends with " + targetUsername + " to view their detailed performance and quiz history.", 14));
        overallCard.addView(Ui.gap(x, 12));

        Button addFriendBtn = Ui.button(x, "Send Friend Request", true);
        addFriendBtn.setOnClickListener(v -> {
            addFriendBtn.setEnabled(false);
            app().firebase.sendFriendRequest(app().user.uid, targetUid, app().user.username, ok -> {
                Toast.makeText(x, ok ? "Friend request sent." : "Could not send request.", Toast.LENGTH_SHORT).show();
                if (ok) addFriendBtn.setText("Request Sent");
                else addFriendBtn.setEnabled(true);
            });
        });
        overallCard.addView(addFriendBtn, new LinearLayout.LayoutParams(-1, Ui.dp(x, 48)));
    }

    private void checkTitles(LinearLayout container) {
        if (container == null || app().isOffline()) return;
        container.removeAllViews();
        app().firebase.checkUserTitles(targetUid, (isGoat, isWizard, isMechanic, isAlchemist) -> {
            if (!isAdded() || container == null) return;
            container.removeAllViews();
            Context x = requireContext();

            if (isGoat) {
                container.addView(createBadge(x, "GOAT", Color.parseColor("#212121"), true));
            }
            if (isWizard) {
                container.addView(createBadge(x, "Wizard", Color.parseColor("#E53935"), false));
            }
            if (isMechanic) {
                container.addView(createBadge(x, "Mechanic", Color.parseColor("#1E88E5"), false));
            }
            if (isAlchemist) {
                container.addView(createBadge(x, "Alchemist", Color.parseColor("#43A047"), false));
            }
        });
    }

    private View createBadge(Context x, String text, int bgColor, boolean glow) {
        TextView tv = new TextView(x);
        tv.setText(text);
        tv.setTextSize(11);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        int padH = Ui.dp(x, 10);
        int padV = Ui.dp(x, 4);
        tv.setPadding(padH, padV, padH, padV);

        GradientDrawable oval = new GradientDrawable();
        oval.setShape(GradientDrawable.RECTANGLE);
        oval.setCornerRadius(Ui.dp(x, 16));
        oval.setColor(bgColor);
        tv.setBackground(oval);

        if (glow) {
            tv.setShadowLayer(14f, 0f, 0f, Color.YELLOW);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMargins(0, 0, Ui.dp(x, 8), 0);
        tv.setLayoutParams(params);

        return tv;
    }

    private List<QuizResult> filterClearedResults(List<QuizResult> list) {
        long casualClearedAt = app().local.casualClearedTime();
        long rankedClearedAt = app().local.rankedClearedTime();
        if (casualClearedAt <= 0 && rankedClearedAt <= 0) return list;
        List<QuizResult> out = new ArrayList<>();
        for (QuizResult r : list) {
            boolean isRanked = r.mode != null && r.mode.toLowerCase(Locale.US).contains("ranked");
            if (isRanked) {
                if (rankedClearedAt <= 0 || r.time > rankedClearedAt) {
                    out.add(r);
                }
            } else {
                if (casualClearedAt <= 0 || r.time > casualClearedAt) {
                    out.add(r);
                }
            }
        }
        return out;
    }

    private void renderFiltered() {
        List<QuizResult> filtered = filterResults(cachedResults, currentTimeFilter, currentModeFilter);
        render(requireContext(), overallCard, subjectList, filtered);
    }

    private List<QuizResult> filterResults(List<QuizResult> all, String timeFilter, String modeFilter) {
        List<QuizResult> out = new ArrayList<>();
        Calendar now = Calendar.getInstance();
        int nowYear = now.get(Calendar.YEAR);
        int nowMonth = now.get(Calendar.MONTH);
        int nowWeek = now.get(Calendar.WEEK_OF_YEAR);

        for (QuizResult r : all) {
            if (!"All Time".equals(timeFilter)) {
                Calendar resCal = Calendar.getInstance();
                resCal.setTimeInMillis(r.time);
                int resYear = resCal.get(Calendar.YEAR);
                int resMonth = resCal.get(Calendar.MONTH);
                int resWeek = resCal.get(Calendar.WEEK_OF_YEAR);

                if ("This Month".equals(timeFilter)) {
                    if (resYear != nowYear || resMonth != nowMonth) continue;
                } else if ("This Week".equals(timeFilter)) {
                    if (resYear != nowYear || resWeek != nowWeek) continue;
                }
            }

            boolean isRanked = r.mode != null && r.mode.toLowerCase(Locale.US).contains("ranked");

            if ("Ranked".equals(modeFilter)) {
                if (!isRanked) continue;
            } else if ("Casual".equals(modeFilter)) {
                if (isRanked) continue;
            }

            out.add(r);
        }
        return out;
    }

    private Stat getStat(Map<String, Stat> map, String key) {
        Stat s = map.get(key);
        if (s == null) {
            s = new Stat();
            map.put(key, s);
        }
        return s;
    }

    private void render(Context x, LinearLayout overall, RecyclerView list, List<QuizResult> rs) {
        overall.removeAllViews();
        int total = 0, correct = 0;
        LinkedHashMap<String, Stat> stats = new LinkedHashMap<>();

        for (QuizResult r : rs) {
            total += r.total;
            correct += r.score;
            String sub = normalize(r.subject);
            if ("All Subjects".equalsIgnoreCase(sub) || sub.contains("Ranked") || sub.contains("All Subjects")) {
                int thirdTotal = r.total / 3;
                int thirdScore = r.score / 3;
                int remTotal = r.total % 3;
                int remScore = r.score % 3;
                if (thirdTotal > 0 || r.total > 0) {
                    getStat(stats, "Math").add(thirdScore + (remScore > 0 ? 1 : 0), thirdTotal + (remTotal > 0 ? 1 : 0));
                    getStat(stats, "Machine Design").add(thirdScore, thirdTotal);
                    getStat(stats, "Powerplant").add(thirdScore, thirdTotal);
                }
            } else {
                getStat(stats, sub).add(r.score, r.total);
            }
        }

        int pct = total == 0 ? 0 : (int) Math.round(correct * 100.0 / total);
        overall.addView(Ui.text(x, pct + "% overall accuracy", 25, true));
        overall.addView(Ui.muted(x, correct + " correct out of " + total + " questions  •  " + rs.size() + " attempts", 13));
        list.setAdapter(new SubjectAdapter(stats));
    }

    private String normalize(String s) {
        if (s == null || s.trim().isEmpty()) return "Uncategorized";
        if (s.equalsIgnoreCase("Algebra") || s.equalsIgnoreCase("Math")) return "Math";
        if (s.equalsIgnoreCase("Machine Design")) return "Machine Design";
        if (s.equalsIgnoreCase("Powerplant") || s.equalsIgnoreCase("Power Plant")) return "Powerplant";
        return s;
    }

    class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.H> {
        List<Map.Entry<String, Stat>> data;
        SubjectAdapter(Map<String, Stat> m) { data = new ArrayList<>(m.entrySet()); }
        class H extends RecyclerView.ViewHolder {
            LinearLayout box;
            H(LinearLayout v) { super(v); box = v; }
        }
        public H onCreateViewHolder(ViewGroup p, int t) { return new H(Ui.card(p.getContext())); }
        public void onBindViewHolder(H h, int pos) {
            Map.Entry<String, Stat> e = data.get(pos);
            h.box.removeAllViews();
            h.box.addView(Ui.text(h.box.getContext(), e.getKey(), 18, true));
            h.box.addView(Ui.text(h.box.getContext(), e.getValue().pct() + "%", 28, true));
            h.box.addView(Ui.muted(h.box.getContext(), e.getValue().correct + " / " + e.getValue().total + " correct  •  " + e.getValue().attempts + " attempts", 13));
        }
        public int getItemCount() { return data.size(); }
    }
}
