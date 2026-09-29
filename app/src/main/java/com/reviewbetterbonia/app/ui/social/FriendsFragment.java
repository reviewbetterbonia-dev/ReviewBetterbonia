package com.reviewbetterbonia.app.ui.social;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.*;
import com.reviewbetterbonia.app.data.FirebaseRepository;
import com.reviewbetterbonia.app.ui.*;
import java.util.*;

public class FriendsFragment extends BaseFragment {
    private LinearLayout requestBox, friendBox;
    private RecyclerView results;
    private EditText query;
    private Set<String> currentFriendUids = new HashSet<>();

    @Override
    public void onResume() {
        super.onResume();
        // Refresh reciprocal friendships when returning to this screen. This also
        // updates a sender's already-open Friends screen after the recipient accepts.
        if (isAdded() && app().user != null && !app().isOffline()
                && requestBox != null && friendBox != null) {
            loadRequests();
            loadFriends();
        }
    }

    @Nullable
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b) {
        Context x = requireContext();
        LinearLayout p = Ui.page(x);
        Ui.add(p, Ui.heading(x, "Friends"), Ui.dp(x, 42));
        Ui.add(p, Ui.muted(x, "Find classmates, send requests, and manage your accepted friends.", 14), Ui.dp(x, 38));

        if (app().isOffline()) {
            LinearLayout card = Ui.card(x);
            card.addView(Ui.text(x, "Offline Mode", 18, true));
            card.addView(Ui.muted(x, "Friends and online sync require internet and Firebase connection. Local review and quizzes are fully available offline.", 13));
            p.addView(card);
        } else {
            Ui.add(p, Ui.text(x, "Find a classmate", 19, true), Ui.dp(x, 35));
            query = new EditText(x);
            query.setHint("Exact username");
            query.setSingleLine();
            Ui.add(p, query, Ui.dp(x, 54));

            Button search = Ui.button(x, "Search", true);
            Ui.add(p, search, Ui.dp(x, 52));

            results = new RecyclerView(x);
            results.setLayoutManager(new LinearLayoutManager(x));
            results.setNestedScrollingEnabled(false);
            p.addView(results, new LinearLayout.LayoutParams(-1, Ui.dp(x, 180)));
            search.setOnClickListener(v -> search());

            Ui.add(p, Ui.text(x, "Friend requests", 19, true), Ui.dp(x, 35));
            requestBox = Ui.card(x);
            p.addView(requestBox);

            Ui.add(p, Ui.text(x, "Your friends", 19, true), Ui.dp(x, 35));
            friendBox = Ui.card(x);
            p.addView(friendBox);

            loadRequests();
            loadFriends();
        }

        Ui.addWeight(p, new Space(x));
        Button back = Ui.button(x, "Back", false);
        back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        Ui.add(p, back, Ui.dp(x, 52));
        return p;
    }

    private void search() {
        String term = query.getText().toString().trim();
        if (term.isEmpty()) return;
        app().firebase.searchUsers(term, app().user.uid, users -> {
            List<FirebaseRepository.UserRow> data = users;
            results.setAdapter(new RecyclerView.Adapter<VH>() {
                public VH onCreateViewHolder(ViewGroup p, int t) {
                    return new VH(Ui.card(p.getContext()));
                }

                public void onBindViewHolder(VH h, int pos) {
                    FirebaseRepository.UserRow u = data.get(pos);
                    h.box.removeAllViews();
                    h.box.addView(Ui.text(h.box.getContext(), u.username, 17, true));
                    h.box.addView(Ui.muted(h.box.getContext(), u.rank + " • " + u.rating + " RP", 13));
                    h.box.addView(Ui.gap(h.box.getContext(), 6));

                    LinearLayout btnRow = new LinearLayout(h.box.getContext());
                    btnRow.setOrientation(LinearLayout.HORIZONTAL);

                    Button profileBtn = Ui.button(h.box.getContext(), "Profile", false);
                    profileBtn.setOnClickListener(v -> app().navigate(ProfileFragment.forUser(u.uid, u.username, u.email, u.role, u.rating, u.rank), true));
                    btnRow.addView(profileBtn);
                    btnRow.addView(Ui.gap(h.box.getContext(), 6));

                    if (currentFriendUids.contains(u.uid)) {
                        Button b = Ui.button(h.box.getContext(), "Friends", false);
                        b.setEnabled(false);
                        btnRow.addView(b);
                    } else {
                        Button b = Ui.button(h.box.getContext(), "Add friend", false);
                        btnRow.addView(b);
                        b.setOnClickListener(v -> app().firebase.sendFriendRequest(app().user.uid, u.uid, app().user.username, ok -> {
                            Toast.makeText(requireContext(), ok ? "Friend request sent." : "Could not send request.", Toast.LENGTH_SHORT).show();
                            if (ok) {
                                b.setText("Request Sent");
                                b.setEnabled(false);
                            }
                        }));
                    }
                    h.box.addView(btnRow);
                }

                public int getItemCount() {
                    return data.size();
                }
            });
        });
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
                            loadRequests();
                            loadFriends();
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
                g.setCornerRadius(Ui.dp(requireContext(), 24));
                viewBtn.setBackground(g);
                viewBtn.setTextColor(Color.WHITE);
                viewBtn.setOnClickListener(v -> app().navigate(ProfileFragment.forUser(u.uid, u.username, u.email, u.role, u.rating, u.rank), true));
                row.addView(viewBtn, new LinearLayout.LayoutParams(-2, Ui.dp(requireContext(), 42)));

                Button removeBtn = Ui.button(requireContext(), "Remove", false);
                GradientDrawable g2 = new GradientDrawable();
                g2.setColor(Color.parseColor("#D32F2F"));
                g2.setCornerRadius(Ui.dp(requireContext(), 24));
                removeBtn.setBackground(g2);
                removeBtn.setTextColor(Color.WHITE);
                removeBtn.setOnClickListener(v -> app().firebase.removeFriend(app().user.uid, u.uid, ok -> {
                    if (ok) {
                        loadFriends();
                    } else {
                        Toast.makeText(requireContext(), "Could not remove friend.", Toast.LENGTH_SHORT).show();
                    }
                }));
                row.addView(Ui.gap(requireContext(), 6));
                row.addView(removeBtn, new LinearLayout.LayoutParams(-2, Ui.dp(requireContext(), 42)));

                friendBox.addView(row);
                friendBox.addView(Ui.gap(requireContext(), 8));
            }
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout box;

        VH(LinearLayout v) {
            super(v);
            box = v;
        }
    }
}
