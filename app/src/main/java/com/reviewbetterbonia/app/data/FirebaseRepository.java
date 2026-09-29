package com.reviewbetterbonia.app.data;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.reviewbetterbonia.app.R;
import com.reviewbetterbonia.app.model.Answer;
import com.reviewbetterbonia.app.model.Question;
import com.reviewbetterbonia.app.model.QuizResult;
import java.util.*;

/** All Firebase access lives here so UI fragments do not need to know Firestore details. */
public class FirebaseRepository {
    public final FirebaseAuth auth;
    public final FirebaseFirestore db;
    public final boolean online;

    public FirebaseRepository(Context c) {
        FirebaseAuth a = null; FirebaseFirestore f = null; boolean ok = false;
        try {
            String key = c.getString(R.string.firebase_api_key);
            String app = c.getString(R.string.firebase_app_id);
            String project = c.getString(R.string.firebase_project_id);
            if (!key.startsWith("PASTE_") && !app.startsWith("PASTE_") && !project.startsWith("PASTE_")) {
                if (FirebaseApp.getApps(c).isEmpty()) {
                    FirebaseApp.initializeApp(c, new FirebaseOptions.Builder()
                            .setApiKey(key).setApplicationId(app).setProjectId(project).build());
                }
                a = FirebaseAuth.getInstance();
                f = FirebaseFirestore.getInstance();
                ok = true;
            }
        } catch (Exception ignored) { }
        auth = a; db = f; online = ok;
    }

    public interface AuthCallback { void done(boolean ok, String message, String uid, String name, String role, int rating); }
    public interface ResultCallback { void done(List<QuizResult> results); }
    public interface BoolCallback { void done(boolean ok); }
    public interface QuestionCallback { void done(List<Question> questions); }
    public interface SubmissionCallback { void done(List<QuestionSubmissionRow> submissions); }
    public interface UserCallback { void done(boolean ok, String message, String uid, String username); }
    public interface UsersCallback { void done(List<UserRow> users); }
    public interface RequestCallback { void done(List<RequestRow> requests); }

    public static class UserRow {
        public String uid, username, email, role, rank;
        public int rating;
        public UserRow(String uid, String username) {
            this(uid, username, "", "student", 0, "Freshman");
        }
        public UserRow(String uid, String username, String email, String role, int rating, String rank) {
            this.uid = uid; this.username = username; this.email = email; this.role = role; this.rating = rating; this.rank = rank;
        }
    }
    public static class RequestRow {
        public String requestId, fromUid, username;
        public RequestRow(String requestId,String fromUid,String username){this.requestId=requestId;this.fromUid=fromUid;this.username=username;}
    }

    public static class QuestionSubmissionRow {
        public String submissionId, submittedBy, submittedByName, subject, category, question, correct, difficulty, targetFile,
                choiceA, choiceB, choiceC, choiceD, correctFeedback, incorrectFeedback, status;
        public Date submittedAt, reviewedAt;

        public QuestionSubmissionRow(String submissionId) {
            this.submissionId = submissionId;
            this.status = "pending";
        }
    }

    public void login(String email, String password, AuthCallback cb) {
        if (!online) { cb.done(false, "Firebase is not configured.", "", "", "student", 0); return; }
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(t -> {
            if (!t.isSuccessful()) { cb.done(false, message(t.getException(), "Login failed."), "", "", "student", 0); return; }
            loadUser(t.getResult().getUser(), cb);
        });
    }

    public void register(String username, String email, String password, AuthCallback cb) {
        if (!online) {
            cb.done(false, "Firebase is not configured.", "", "", "student", 0);
            return;
        }

        String cleanUsername = username.trim();
        String cleanEmail = email.trim();
        String usernameLower = cleanUsername.toLowerCase(Locale.US);

        if (cleanUsername.isEmpty() || cleanEmail.isEmpty() || password.isEmpty()) {
            cb.done(false, "Please fill in all fields.", "", "", "student", 0);
            return;
        }

        auth.createUserWithEmailAndPassword(cleanEmail, password)
                .addOnCompleteListener(t -> {
                    if (!t.isSuccessful()) {
                        cb.done(false, message(t.getException(), "Registration failed."), "", "", "student", 0);
                        return;
                    }

                    FirebaseUser u = t.getResult().getUser();
                    if (u == null) {
                        cb.done(false, "Registration failed: Firebase user was not created.", "", "", "student", 0);
                        return;
                    }

                    db.collection("users")
                            .whereEqualTo("usernameLower", usernameLower)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                boolean taken = false;
                                for (DocumentSnapshot d : snapshot) {
                                    if (!d.getId().equals(u.getUid())) {
                                        taken = true;
                                        break;
                                    }
                                }

                                if (taken) {
                                    u.delete();
                                    cb.done(false, "Username is already taken. Please choose another one.", "", "", "student", 0);
                                    return;
                                }

                                Map<String, Object> m = new HashMap<>();
                                m.put("username", cleanUsername);
                                m.put("usernameLower", usernameLower);
                                m.put("email", cleanEmail);
                                m.put("createdAt", FieldValue.serverTimestamp());
                                m.put("role", "student");
                                m.put("rankedRating", 0);
                                m.put("rankedRank", "Freshman");

                                db.collection("users")
                                        .document(u.getUid())
                                        .set(m)
                                        .addOnCompleteListener(x -> {
                                            if (!x.isSuccessful()) {
                                                cb.done(
                                                        false,
                                                        "Account created, but profile setup failed: "
                                                                + message(x.getException(), "Firestore permission denied."),
                                                        "",
                                                        "",
                                                        "student",
                                                        0
                                                );
                                                return;
                                            }

                                            cb.done(true, "", u.getUid(), cleanUsername, "student", 0);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Map<String, Object> m = new HashMap<>();
                                m.put("username", cleanUsername);
                                m.put("usernameLower", usernameLower);
                                m.put("email", cleanEmail);
                                m.put("createdAt", FieldValue.serverTimestamp());
                                m.put("role", "student");
                                m.put("rankedRating", 0);
                                m.put("rankedRank", "Freshman");

                                db.collection("users")
                                        .document(u.getUid())
                                        .set(m)
                                        .addOnCompleteListener(x -> {
                                            if (!x.isSuccessful()) {
                                                cb.done(false, "Account created, but profile setup failed: " + message(x.getException(), "Firestore permission denied."), "", "", "student", 0);
                                                return;
                                            }
                                            cb.done(true, "", u.getUid(), cleanUsername, "student", 0);
                                        });
                            });
                });
    }

    private void loadUser(FirebaseUser u, AuthCallback cb) {
        db.collection("users").document(u.getUid()).get().addOnCompleteListener(t -> {
            if (!t.isSuccessful()) { cb.done(false, message(t.getException(), "Could not load your profile."), "", "", "student", 0); return; }
            DocumentSnapshot d = t.getResult();
            if (!d.exists()) { cb.done(false, "Your Firebase account has no user profile.", "", "", "student", 0); return; }
            String role = d.getString("role"); if (role == null) role = "student";
            String name = d.getString("username"); if (name == null || name.trim().isEmpty()) name = "Player";
            Long x = d.getLong("rankedRating"); int rating = x == null ? 0 : x.intValue();
            cb.done(true, "", u.getUid(), name, role, rating);
        });
    }

    public void saveResult(String uid, String username, QuizResult r, boolean ranked, int newRating, String newRank) {
        if (!online || uid == null || uid.isEmpty()) return;
        Map<String,Object> m = new HashMap<>();
        m.put("userId", uid); m.put("username", username); m.put("mode", r.mode);
        m.put("score", r.score); m.put("total", r.total); m.put("accuracy", r.accuracy());
        m.put("timeTaken", r.seconds); m.put("subject", r.subject); m.put("category", r.category);
        m.put("ranked", ranked); m.put("completedAt", r.time > 0 ? new Date(r.time) : FieldValue.serverTimestamp());
        // One global result document is enough for leaderboard + history.  Do not create a second copy.
        db.collection("results").add(m).addOnSuccessListener(ref -> {
            db.collection("users").document(uid).collection("results").document(ref.getId()).set(m);
        });
        if (ranked) updateRating(uid, newRating, newRank);
    }

    /** Backward-compatible overload for callers that do not update ranked rating. */
    public void saveResult(String uid, String username, QuizResult r) {
        saveResult(uid, username, r, false, 0, "Freshman");
    }

    private void updateRating(String uid, int rating, String rank) {
        Map<String,Object> m = new HashMap<>();
        m.put("rankedRating", rating); m.put("rankedRank", rank);
        db.collection("users").document(uid).update(m);
    }

    public void syncResults(String uid, ResultCallback done) {
        if (!online || uid == null || uid.isEmpty()) { done.done(new ArrayList<>()); return; }
        db.collection("users").document(uid).collection("results")
                .limit(200).get()
                .addOnSuccessListener(s -> {
                    List<QuizResult> list = toResults(s);
                    if (!list.isEmpty()) {
                        done.done(list);
                    } else {
                        db.collection("results").whereEqualTo("userId", uid).limit(200).get()
                                .addOnSuccessListener(s2 -> done.done(toResults(s2)))
                                .addOnFailureListener(e2 -> done.done(new ArrayList<>()));
                    }
                })
                .addOnFailureListener(e -> {
                    db.collection("results").whereEqualTo("userId", uid).limit(200).get()
                            .addOnSuccessListener(s2 -> done.done(toResults(s2)))
                            .addOnFailureListener(e2 -> done.done(new ArrayList<>()));
                });
    }

    private List<QuizResult> toResults(QuerySnapshot s) {
        List<QuizResult> out = new ArrayList<>();
        for (DocumentSnapshot d : s) {
            Long sc=d.getLong("score"), tot=d.getLong("total"), sec=d.getLong("timeTaken");
            if (sc != null && tot != null) {
                long time = System.currentTimeMillis();
                Object cat = d.get("completedAt");
                if (cat instanceof Timestamp) {
                    time = ((Timestamp) cat).toDate().getTime();
                } else if (cat instanceof Date) {
                    time = ((Date) cat).getTime();
                } else if (cat instanceof Long) {
                    time = (Long) cat;
                }
                out.add(new QuizResult(d.getString("mode"), sc.intValue(), tot.intValue(), sec==null?0:sec.intValue(),
                        time, d.getString("subject"), d.getString("category")));
            }
        }
        return out;
    }

    /** Loads questions published by admins and makes them available to the local quiz repository. */
    public void loadActiveQuestions(QuestionCallback done) {
        if (!online) { done.done(new ArrayList<>()); return; }
        db.collection("questions").whereEqualTo("active", true).get()
                .addOnSuccessListener(s -> {
                    List<Question> out = new ArrayList<>();
                    for (DocumentSnapshot d : s) {
                        Question q = new Question(); q.id = d.getId();
                        q.subject = safe(d.getString("subject")); q.category = safe(d.getString("category"));
                        q.html = safe(d.getString("question")); q.correctFeedback = safe(d.getString("correctFeedback"));
                        q.incorrectFeedback = safe(d.getString("incorrectFeedback"));
                        String[] choices = {safe(d.getString("choiceA")),safe(d.getString("choiceB")),safe(d.getString("choiceC")),safe(d.getString("choiceD"))};
                        String correct = safe(d.getString("correct"));
                        for (int i=0;i<4;i++) { Answer a=new Answer(); a.html=choices[i]; a.correct=correct.equalsIgnoreCase(String.valueOf((char)('A'+i))) || correct.equals(String.valueOf(i)); q.answers.add(a); }
                        out.add(q);
                    }
                    done.done(out);
                }).addOnFailureListener(e -> done.done(new ArrayList<>()));
    }

    /** Submits a question for admin review. It is not visible to quiz users until approved. */
    public void submitQuestion(String uid, String username, String subject, String category, String question,
                               String[] choices, String correct, String difficulty, String targetFile, BoolCallback done) {
        if (!online || uid == null || uid.trim().isEmpty()) { done.done(false); return; }
        if (choices == null || choices.length < 4 || targetFile == null || targetFile.trim().isEmpty()) { done.done(false); return; }

        String id = db.collection("questionSubmissions").document().getId();
        Map<String, Object> m = new HashMap<>();
        m.put("subject", subject);
        m.put("category", category);
        m.put("question", question);
        m.put("choiceA", choices[0]);
        m.put("choiceB", choices[1]);
        m.put("choiceC", choices[2]);
        m.put("choiceD", choices[3]);
        m.put("correct", correct);
        m.put("difficulty", difficulty);
        m.put("targetFile", targetFile);
        m.put("submittedBy", uid);
        m.put("submittedByName", username == null ? "" : username);
        m.put("status", "pending");
        m.put("createdAt", FieldValue.serverTimestamp());
        db.collection("questionSubmissions").document(id).set(m)
                .addOnCompleteListener(t -> done.done(t.isSuccessful()));
    }

    /** Loads questions waiting for admin approval. */
    public void loadPendingQuestionSubmissions(SubmissionCallback done) {
        if (!online) { done.done(new ArrayList<>()); return; }
        db.collection("questionSubmissions").whereEqualTo("status", "pending").get()
                .addOnSuccessListener(snapshot -> {
                    List<QuestionSubmissionRow> out = new ArrayList<>();
                    for (DocumentSnapshot d : snapshot) {
                        QuestionSubmissionRow row = new QuestionSubmissionRow(d.getId());
                        row.submittedBy = safe(d.getString("submittedBy"));
                        row.submittedByName = safe(d.getString("submittedByName"));
                        row.subject = safe(d.getString("subject"));
                        row.category = safe(d.getString("category"));
                        row.question = safe(d.getString("question"));
                        row.correct = safe(d.getString("correct"));
                        row.difficulty = safe(d.getString("difficulty"));
                        row.targetFile = safe(d.getString("targetFile"));
                        row.choiceA = safe(d.getString("choiceA"));
                        row.choiceB = safe(d.getString("choiceB"));
                        row.choiceC = safe(d.getString("choiceC"));
                        row.choiceD = safe(d.getString("choiceD"));
                        row.correctFeedback = safe(d.getString("correctFeedback"));
                        row.incorrectFeedback = safe(d.getString("incorrectFeedback"));
                        row.status = safe(d.getString("status"));
                        Timestamp created = d.getTimestamp("createdAt");
                        if (created != null) row.submittedAt = created.toDate();
                        out.add(row);
                    }
                    Collections.sort(out, (a, b) -> {
                        long ta = a.submittedAt == null ? 0L : a.submittedAt.getTime();
                        long tb = b.submittedAt == null ? 0L : b.submittedAt.getTime();
                        return Long.compare(tb, ta);
                    });
                    done.done(out);
                })
                .addOnFailureListener(e -> done.done(new ArrayList<>()));
    }

    /** Approves a submission and atomically creates the published question. */
    public void approveQuestionSubmission(String adminUid, QuestionSubmissionRow row, BoolCallback done) {
        if (!online || row == null || row.submissionId == null || row.submissionId.trim().isEmpty()) { done.done(false); return; }

        String questionId = "fb_" + db.collection("questions").document().getId();
        Map<String, Object> question = new HashMap<>();
        question.put("id", questionId);
        question.put("subject", row.subject);
        question.put("category", row.category);
        question.put("question", row.question);
        question.put("choiceA", row.choiceA);
        question.put("choiceB", row.choiceB);
        question.put("choiceC", row.choiceC);
        question.put("choiceD", row.choiceD);
        question.put("correct", row.correct);
        question.put("difficulty", row.difficulty == null || row.difficulty.trim().isEmpty() ? "Normal" : row.difficulty);
        question.put("targetFile", row.targetFile);
        question.put("active", true);
        question.put("createdBy", row.submittedBy);
        question.put("createdAt", FieldValue.serverTimestamp());
        question.put("approvedBy", adminUid);
        question.put("approvedAt", FieldValue.serverTimestamp());
        question.put("submissionId", row.submissionId);
        if (row.correctFeedback != null && !row.correctFeedback.isEmpty()) question.put("correctFeedback", row.correctFeedback);
        if (row.incorrectFeedback != null && !row.incorrectFeedback.isEmpty()) question.put("incorrectFeedback", row.incorrectFeedback);

        Map<String, Object> submissionUpdate = new HashMap<>();
        submissionUpdate.put("status", "approved");
        submissionUpdate.put("reviewedBy", adminUid);
        submissionUpdate.put("reviewedAt", FieldValue.serverTimestamp());
        submissionUpdate.put("questionId", questionId);

        WriteBatch batch = db.batch();
        DocumentReference submissionRef = db.collection("questionSubmissions").document(row.submissionId);
        DocumentReference questionRef = db.collection("questions").document(questionId);
        batch.update(submissionRef, submissionUpdate);
        batch.set(questionRef, question);
        batch.commit().addOnCompleteListener(t -> done.done(t.isSuccessful()));
    }

    /** Rejects a pending submission without publishing it. */
    public void rejectQuestionSubmission(String adminUid, String submissionId, String reason, BoolCallback done) {
        if (!online || submissionId == null || submissionId.trim().isEmpty()) { done.done(false); return; }
        Map<String, Object> m = new HashMap<>();
        m.put("status", "rejected");
        m.put("reviewedBy", adminUid);
        m.put("reviewedAt", FieldValue.serverTimestamp());
        if (reason != null && !reason.trim().isEmpty()) m.put("rejectionReason", reason.trim());
        db.collection("questionSubmissions").document(submissionId).update(m)
                .addOnCompleteListener(t -> done.done(t.isSuccessful()));
    }

    public void searchUsers(String term, String currentUid, UsersCallback cb) {
        if (!online || term == null || term.trim().isEmpty()) { cb.done(new ArrayList<>()); return; }
        db.collection("users").whereEqualTo("usernameLower", term.trim().toLowerCase(Locale.US)).limit(10).get()
                .addOnSuccessListener(s -> {
                    List<UserRow> out=new ArrayList<>();
                    for(DocumentSnapshot d:s) {
                        if(!d.getId().equals(currentUid)) {
                            Long rLong = d.getLong("rankedRating");
                            int rInt = rLong == null ? 0 : rLong.intValue();
                            String rRank = d.getString("rankedRank");
                            if (rRank == null) rRank = "Freshman";
                            out.add(new UserRow(d.getId(), safe(d.getString("username")), safe(d.getString("email")), safe(d.getString("role")), rInt, rRank));
                        }
                    }
                    cb.done(out);
                })
                .addOnFailureListener(e -> cb.done(new ArrayList<>()));
    }

    public void sendFriendRequest(String fromUid, String toUid, String fromUsername, BoolCallback cb) {
        if (!online || fromUid == null || toUid == null || fromUid.equals(toUid)) { cb.done(false); return; }
        String id = fromUid + "_" + toUid;
        Map<String,Object> m=new HashMap<>();m.put("fromUid",fromUid);m.put("toUid",toUid);m.put("fromUsername",fromUsername);m.put("status","pending");m.put("createdAt",FieldValue.serverTimestamp());
        db.collection("users").document(toUid).collection("friendRequests").document(id).set(m).addOnCompleteListener(t -> cb.done(t.isSuccessful()));
    }

    public void acceptFriendRequest(String myUid, String requestId, String friendUid, BoolCallback cb) {
        if (!online || myUid == null || myUid.trim().isEmpty()) { cb.done(false); return; }

        String targetUid = friendUid;
        if ((targetUid == null || targetUid.trim().isEmpty()) && requestId != null && requestId.contains("_")) {
            String[] parts = requestId.split("_");
            if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                targetUid = parts[0].trim();
            }
        }

        if (targetUid == null || targetUid.trim().isEmpty() || targetUid.equals(myUid)) {
            cb.done(false);
            return;
        }

        final String resolvedFriendUid = targetUid;
        String canonicalPair = myUid.compareTo(resolvedFriendUid) < 0 ? myUid + "_" + resolvedFriendUid : resolvedFriendUid + "_" + myUid;

        Map<String,Object> friendship = new HashMap<>();
        friendship.put("users", Arrays.asList(myUid, resolvedFriendUid));
        friendship.put("user1", myUid);
        friendship.put("user2", resolvedFriendUid);
        friendship.put("createdAt", FieldValue.serverTimestamp());

        Map<String,Object> friendForMe = new HashMap<>();
        friendForMe.put("uid", resolvedFriendUid);
        friendForMe.put("addedAt", FieldValue.serverTimestamp());

        Map<String,Object> friendForSender = new HashMap<>();
        friendForSender.put("uid", myUid);
        friendForSender.put("addedAt", FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();
        batch.set(db.collection("friendships").document(canonicalPair), friendship);
        batch.set(db.collection("users").document(myUid).collection("friends").document(resolvedFriendUid), friendForMe);
        batch.set(db.collection("users").document(resolvedFriendUid).collection("friends").document(myUid), friendForSender);

        Set<String> deletedReqIds = new LinkedHashSet<>();
        if (requestId != null && !requestId.trim().isEmpty()) {
            deletedReqIds.add(requestId.trim());
        }
        deletedReqIds.add(resolvedFriendUid + "_" + myUid);

        for (String reqIdToDelete : deletedReqIds) {
            batch.delete(db.collection("users").document(myUid).collection("friendRequests").document(reqIdToDelete));
        }

        batch.commit().addOnCompleteListener(t -> {
            if (!t.isSuccessful() && t.getException() != null) {
                Log.w("FirebaseRepository", "Error accepting friend request", t.getException());
            }
            cb.done(t.isSuccessful());
        });
    }

    public void rejectFriendRequest(String myUid, String requestId, BoolCallback cb) {
        if (!online || myUid == null || requestId == null) { cb.done(false); return; }
        db.collection("users").document(myUid).collection("friendRequests").document(requestId).delete()
                .addOnCompleteListener(t -> cb.done(t.isSuccessful()));
    }

    public void removeFriend(String myUid, String friendUid, BoolCallback cb) {
        if (!online || myUid == null || friendUid == null) { cb.done(false); return; }
        String canonicalPair = myUid.compareTo(friendUid) < 0 ? myUid + "_" + friendUid : friendUid + "_" + myUid;
        
        // 1. Delete from my friends subcollection
        db.collection("users").document(myUid).collection("friends").document(friendUid).delete().addOnCompleteListener(t1 -> {
            // 2. Delete from friend's friends subcollection
            db.collection("users").document(friendUid).collection("friends").document(myUid).delete().addOnCompleteListener(t2 -> {
                // 3. Delete canonical friendship document
                db.collection("friendships").document(canonicalPair).delete().addOnCompleteListener(t3 -> {
                    // 4. Clean up any remaining top-level friendship docs containing both users
                    db.collection("friendships").whereArrayContains("users", myUid).get().addOnCompleteListener(t4 -> {
                        if (t4.isSuccessful() && t4.getResult() != null) {
                            WriteBatch batch = db.batch();
                            int count = 0;
                            for (DocumentSnapshot d : t4.getResult()) {
                                List<?> list = (List<?>) d.get("users");
                                if (list != null && list.contains(friendUid)) {
                                    batch.delete(d.getReference());
                                    count++;
                                }
                            }
                            if (count > 0) {
                                batch.commit().addOnCompleteListener(t5 -> cb.done(true));
                                return;
                            }
                        }
                        cb.done(true);
                    });
                });
            });
        });
    }

    public void loadFriends(String uid, UsersCallback cb) {
        if (!online || uid == null || uid.isEmpty()) { cb.done(new ArrayList<>()); return; }
        
        Set<String> friendUids = Collections.synchronizedSet(new LinkedHashSet<>());

        // 1. Check my own friends subcollection
        db.collection("users").document(uid).collection("friends").get().addOnCompleteListener(t1 -> {
            if (t1.isSuccessful() && t1.getResult() != null) {
                for (DocumentSnapshot d : t1.getResult()) {
                    friendUids.add(d.getId());
                    String fid = d.getString("uid");
                    if (fid != null && !fid.isEmpty()) friendUids.add(fid);
                }
            }

            // 2. Check top-level friendships where users array contains my uid
            db.collection("friendships").whereArrayContains("users", uid).get().addOnCompleteListener(t2 -> {
                if (t2.isSuccessful() && t2.getResult() != null) {
                    for (DocumentSnapshot d : t2.getResult()) {
                        List<?> list = (List<?>) d.get("users");
                        if (list != null) {
                            for (Object o : list) {
                                String uStr = String.valueOf(o);
                                if (!uStr.equals(uid) && !uStr.isEmpty()) {
                                    boolean isNew = friendUids.add(uStr);
                                    if (isNew) {
                                        // Self-healing: create missing friend document in my friends subcollection
                                        Map<String, Object> mine = new HashMap<>();
                                        mine.put("uid", uStr);
                                        mine.put("addedAt", FieldValue.serverTimestamp());
                                        db.collection("users").document(uid).collection("friends").document(uStr).set(mine);
                                    }
                                }
                            }
                        }
                    }
                }

                fetchUserRows(friendUids, cb);
            });
        }).addOnFailureListener(e -> cb.done(new ArrayList<>()));
    }

    private void fetchUserRows(Set<String> uids, UsersCallback cb) {
        if (uids.isEmpty()) {
            cb.done(new ArrayList<>());
            return;
        }

        List<UserRow> out = Collections.synchronizedList(new ArrayList<>());
        int[] left = {uids.size()};
        for (String fid : uids) {
            db.collection("users").document(fid).get().addOnCompleteListener(t -> {
                if (t.isSuccessful() && t.getResult() != null && t.getResult().exists()) {
                    DocumentSnapshot doc = t.getResult();
                    Long rLong = doc.getLong("rankedRating");
                    int rInt = rLong == null ? 0 : rLong.intValue();
                    String rRank = doc.getString("rankedRank");
                    if (rRank == null) rRank = "Freshman";
                    String role = safe(doc.getString("role"));
                    if (role.isEmpty()) role = "student";
                    out.add(new UserRow(fid, safe(doc.getString("username")), safe(doc.getString("email")), role, rInt, rRank));
                }
                if (--left[0] == 0) {
                    cb.done(out);
                }
            });
        }
    }

    public void loadFriendRequests(String uid, RequestCallback cb) {
        if (!online || uid == null || uid.trim().isEmpty()) { cb.done(new ArrayList<>()); return; }
        db.collection("users").document(uid).collection("friendRequests").get().addOnSuccessListener(s -> {
            List<RequestRow> out = Collections.synchronizedList(new ArrayList<>());
            if (s == null || s.isEmpty()) {
                cb.done(out);
                return;
            }
            List<DocumentSnapshot> docs = s.getDocuments();
            int[] remaining = {docs.size()};

            for (DocumentSnapshot d : docs) {
                String reqId = d.getId();
                String fromUid = safe(d.getString("fromUid"));
                if (fromUid.isEmpty() && reqId.contains("_")) {
                    fromUid = reqId.split("_")[0];
                }
                String fromUsername = safe(d.getString("fromUsername"));
                if (fromUsername.isEmpty()) {
                    fromUsername = safe(d.getString("username"));
                }

                final String finalFromUid = fromUid;
                final String finalFromUsername = fromUsername;

                if (finalFromUsername.isEmpty() && !finalFromUid.isEmpty()) {
                    db.collection("users").document(finalFromUid).get().addOnCompleteListener(userTask -> {
                        String name = finalFromUid;
                        if (userTask.isSuccessful() && userTask.getResult() != null && userTask.getResult().exists()) {
                            String uName = safe(userTask.getResult().getString("username"));
                            if (!uName.isEmpty()) name = uName;
                        }
                        out.add(new RequestRow(reqId, finalFromUid, name));
                        if (--remaining[0] == 0) {
                            cb.done(out);
                        }
                    });
                } else {
                    String displayName = finalFromUsername.isEmpty() ? "Unknown User" : finalFromUsername;
                    out.add(new RequestRow(reqId, finalFromUid, displayName));
                    if (--remaining[0] == 0) {
                        cb.done(out);
                    }
                }
            }
        }).addOnFailureListener(e -> cb.done(new ArrayList<>()));
    }

    public void resetAllScores(BoolCallback cb) {
        if (!online) { cb.done(false); return; }
        db.collection("users").get()
            .addOnSuccessListener(snapshot -> {
                for (DocumentSnapshot d : snapshot) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("rankedRating", 0);
                    m.put("rankedRank", "Freshman");
                    d.getReference().update(m);
                }
                db.collection("results").get().addOnSuccessListener(resultsSnap -> {
                    for (DocumentSnapshot resDoc : resultsSnap) {
                        String mode = resDoc.getString("mode");
                        Boolean ranked = resDoc.getBoolean("ranked");
                        if ((ranked != null && ranked) || (mode != null && mode.toLowerCase(Locale.US).contains("ranked"))) {
                            resDoc.getReference().delete();
                        }
                    }
                });
                db.collection("users").get().addOnSuccessListener(usersSnap -> {
                    for (DocumentSnapshot uDoc : usersSnap) {
                        uDoc.getReference().collection("results").get().addOnSuccessListener(subResSnap -> {
                            for (DocumentSnapshot subDoc : subResSnap) {
                                String mode = subDoc.getString("mode");
                                Boolean ranked = subDoc.getBoolean("ranked");
                                if ((ranked != null && ranked) || (mode != null && mode.toLowerCase(Locale.US).contains("ranked"))) {
                                    subDoc.getReference().delete();
                                }
                            }
                        });
                    }
                });
                cb.done(true);
            })
            .addOnFailureListener(e -> cb.done(false));
    }

    public void deleteOldUsers(long cutoffTimestampMillis, BoolCallback cb) {
        if (!online) { cb.done(false); return; }
        Date cutoff = new Date(cutoffTimestampMillis);
        db.collection("users").whereLessThan("createdAt", cutoff).get().addOnSuccessListener(snapshot -> {
            WriteBatch batch = db.batch();
            int count = 0;
            for (DocumentSnapshot d : snapshot) {
                String role = d.getString("role");
                if ("super_admin".equals(role) || "admin".equals(role)) continue;
                batch.delete(d.getReference());
                count++;
            }
            if (count == 0) { cb.done(true); return; }
            batch.commit().addOnCompleteListener(t -> cb.done(t.isSuccessful()));
        }).addOnFailureListener(e -> cb.done(false));
    }

    public void clearAllUsersData(BoolCallback cb) {
        if (!online) { cb.done(false); return; }
        db.collection("users").get().addOnSuccessListener(userSnap -> {
            WriteBatch batch = db.batch();
            for (DocumentSnapshot d : userSnap) {
                batch.delete(d.getReference());
            }
            db.collection("results").get().addOnSuccessListener(resultSnap -> {
                for (DocumentSnapshot d : resultSnap) {
                    batch.delete(d.getReference());
                }
                batch.commit().addOnCompleteListener(t -> cb.done(t.isSuccessful()));
            }).addOnFailureListener(e -> cb.done(false));
        }).addOnFailureListener(e -> cb.done(false));
    }

    public void clearUserData(String uid, BoolCallback cb) {
        if (!online || uid == null || uid.isEmpty()) { cb.done(false); return; }
        Map<String, Object> m = new HashMap<>();
        m.put("rankedRating", 0);
        m.put("rankedRank", "Freshman");
        db.collection("users").document(uid).update(m).addOnCompleteListener(t -> {
            db.collection("users").document(uid).collection("results").get().addOnCompleteListener(task -> {
                WriteBatch batch = db.batch();
                final int[] count = {0};
                if (task.isSuccessful() && task.getResult() != null) {
                    for (DocumentSnapshot d : task.getResult()) {
                        batch.delete(d.getReference());
                        count[0]++;
                    }
                }
                db.collection("results").whereEqualTo("userId", uid).get().addOnCompleteListener(globalTask -> {
                    if (globalTask.isSuccessful() && globalTask.getResult() != null) {
                        for (DocumentSnapshot d : globalTask.getResult()) {
                            batch.delete(d.getReference());
                            count[0]++;
                        }
                    }
                    if (count[0] == 0) { cb.done(true); return; }
                    batch.commit().addOnCompleteListener(bt -> cb.done(true)).addOnFailureListener(e -> cb.done(true));
                });
            });
        }).addOnFailureListener(e -> cb.done(false));
    }

    public void clearCasualResults(String uid, BoolCallback cb) {
        if (!online || uid == null || uid.isEmpty()) { cb.done(false); return; }
        db.collection("users").document(uid).collection("results").get()
            .addOnSuccessListener(task -> {
                for (DocumentSnapshot d : task) {
                    String mode = d.getString("mode");
                    boolean isRanked = mode != null && mode.toLowerCase(Locale.US).contains("ranked");
                    if (!isRanked) {
                        d.getReference().delete();
                    }
                }
            });

        db.collection("results").whereEqualTo("userId", uid).get()
            .addOnSuccessListener(globalTask -> {
                for (DocumentSnapshot d : globalTask) {
                    String mode = d.getString("mode");
                    boolean isRanked = mode != null && mode.toLowerCase(Locale.US).contains("ranked");
                    if (!isRanked) {
                        d.getReference().delete();
                    }
                }
                cb.done(true);
            })
            .addOnFailureListener(e -> cb.done(true));
    }

    public interface TitlesCallback { void done(boolean isGoat, boolean isWizard, boolean isMechanic, boolean isAlchemist); }

    public static class LeaderPlayer {
        public final String uid;
        public final String name;
        public int rating, math, machineDesign, powerplant;
        public LeaderPlayer(String uid, String name) {
            this.uid = uid; this.name = name;
        }
        public void add(String subject, int pts) {
            if ("Math".equals(subject)) math += pts;
            else if ("Machine Design".equals(subject)) machineDesign += pts;
            else if ("Powerplant".equals(subject)) powerplant += pts;
        }
    }

    public void checkUserTitles(String uid, TitlesCallback cb) {
        if (!online || uid == null || uid.isEmpty()) {
            cb.done(false, false, false, false);
            return;
        }

        db.collection("users").get().addOnSuccessListener(userSnapshot -> {
            Map<String, LeaderPlayer> map = new LinkedHashMap<>();
            for (DocumentSnapshot d : userSnapshot) {
                String uId = d.getId();
                String name = d.getString("username");
                if (uId.isEmpty()) continue;
                LeaderPlayer p = new LeaderPlayer(uId, name == null ? "Player" : name);
                Long rating = d.getLong("rankedRating");
                p.rating = rating == null ? 0 : rating.intValue();
                map.put(uId, p);
            }

            db.collection("results").get().addOnSuccessListener(resultSnapshot -> {
                for (DocumentSnapshot d : resultSnapshot) {
                    String uId = d.getString("userId");
                    String subject = displaySubjectStatic(d.getString("subject"));
                    Long score = d.getLong("score");
                    if (uId == null || score == null) continue;
                    LeaderPlayer p = map.get(uId);
                    if (p == null) {
                        p = new LeaderPlayer(uId, "Player");
                        map.put(uId, p);
                    }
                    p.add(subject, score.intValue());
                }

                List<LeaderPlayer> players = new ArrayList<>(map.values());
                if (players.isEmpty()) {
                    cb.done(false, false, false, false);
                    return;
                }

                Collections.sort(players, (a, b) -> Integer.compare(b.rating, a.rating));
                boolean isGoat = !players.isEmpty() && players.get(0).uid.equals(uid) && players.get(0).rating > 0;

                Collections.sort(players, (a, b) -> Integer.compare(b.math, a.math));
                boolean isWizard = !players.isEmpty() && players.get(0).uid.equals(uid) && players.get(0).math > 0;

                Collections.sort(players, (a, b) -> Integer.compare(b.machineDesign, a.machineDesign));
                boolean isMechanic = !players.isEmpty() && players.get(0).uid.equals(uid) && players.get(0).machineDesign > 0;

                Collections.sort(players, (a, b) -> Integer.compare(b.powerplant, a.powerplant));
                boolean isAlchemist = !players.isEmpty() && players.get(0).uid.equals(uid) && players.get(0).powerplant > 0;

                cb.done(isGoat, isWizard, isMechanic, isAlchemist);
            }).addOnFailureListener(e -> cb.done(false, false, false, false));
        }).addOnFailureListener(e -> cb.done(false, false, false, false));
    }

    private static String displaySubjectStatic(String subject) {
        if (subject == null) return "";
        if (subject.equalsIgnoreCase("Algebra") || subject.equalsIgnoreCase("Math")) return "Math";
        if (subject.equalsIgnoreCase("Machine Design")) return "Machine Design";
        if (subject.equalsIgnoreCase("Powerplant") || subject.equalsIgnoreCase("Power Plant")) return "Powerplant";
        return subject;
    }

    public void loadAllUsers(String currentUid, UsersCallback cb) {
        if (!online) { cb.done(new ArrayList<>()); return; }
        db.collection("users").get().addOnSuccessListener(s -> {
            List<UserRow> out = new ArrayList<>();
            for (DocumentSnapshot d : s) {
                if (!d.getId().equals(currentUid)) {
                    Long rLong = d.getLong("rankedRating");
                    int rInt = rLong == null ? 0 : rLong.intValue();
                    String rRank = d.getString("rankedRank");
                    if (rRank == null) rRank = "Freshman";
                    String role = safe(d.getString("role"));
                    if (role.isEmpty()) role = "student";
                    out.add(new UserRow(d.getId(), safe(d.getString("username")), safe(d.getString("email")), role, rInt, rRank));
                }
            }
            cb.done(out);
        }).addOnFailureListener(e -> cb.done(new ArrayList<>()));
    }

    public interface LeaderTitlesCallback { void done(Map<String, List<String>> titlesMap); }

    public void getLeaderTitles(LeaderTitlesCallback cb) {
        if (!online) { cb.done(new HashMap<>()); return; }
        db.collection("users").get().addOnSuccessListener(userSnapshot -> {
            Map<String, LeaderPlayer> map = new LinkedHashMap<>();
            for (DocumentSnapshot d : userSnapshot) {
                String uId = d.getId();
                String name = d.getString("username");
                if (uId.isEmpty()) continue;
                LeaderPlayer p = new LeaderPlayer(uId, name == null ? "Player" : name);
                Long rating = d.getLong("rankedRating");
                p.rating = rating == null ? 0 : rating.intValue();
                map.put(uId, p);
            }

            db.collection("results").get().addOnSuccessListener(resultSnapshot -> {
                for (DocumentSnapshot d : resultSnapshot) {
                    String uId = d.getString("userId");
                    String subject = displaySubjectStatic(d.getString("subject"));
                    Long score = d.getLong("score");
                    if (uId == null || score == null) continue;
                    LeaderPlayer p = map.get(uId);
                    if (p == null) {
                        p = new LeaderPlayer(uId, "Player");
                        map.put(uId, p);
                    }
                    p.add(subject, score.intValue());
                }

                List<LeaderPlayer> players = new ArrayList<>(map.values());
                Map<String, List<String>> out = new HashMap<>();
                if (players.isEmpty()) {
                    cb.done(out);
                    return;
                }

                Collections.sort(players, (a, b) -> Integer.compare(b.rating, a.rating));
                if (!players.isEmpty() && players.get(0).rating > 0) {
                    List<String> list = out.get(players.get(0).uid);
                    if (list == null) { list = new ArrayList<>(); out.put(players.get(0).uid, list); }
                    list.add("GOAT");
                }

                Collections.sort(players, (a, b) -> Integer.compare(b.math, a.math));
                if (!players.isEmpty() && players.get(0).math > 0) {
                    List<String> list = out.get(players.get(0).uid);
                    if (list == null) { list = new ArrayList<>(); out.put(players.get(0).uid, list); }
                    list.add("Wizard");
                }

                Collections.sort(players, (a, b) -> Integer.compare(b.machineDesign, a.machineDesign));
                if (!players.isEmpty() && players.get(0).machineDesign > 0) {
                    List<String> list = out.get(players.get(0).uid);
                    if (list == null) { list = new ArrayList<>(); out.put(players.get(0).uid, list); }
                    list.add("Mechanic");
                }

                Collections.sort(players, (a, b) -> Integer.compare(b.powerplant, a.powerplant));
                if (!players.isEmpty() && players.get(0).powerplant > 0) {
                    List<String> list = out.get(players.get(0).uid);
                    if (list == null) { list = new ArrayList<>(); out.put(players.get(0).uid, list); }
                    list.add("Alchemist");
                }

                cb.done(out);
            }).addOnFailureListener(e -> cb.done(new HashMap<>()));
        }).addOnFailureListener(e -> cb.done(new HashMap<>()));
    }

    private static String safe(String s){return s==null?"":s;}
    private static String message(Exception e,String fallback){return e==null?fallback:(e.getMessage()==null?fallback:e.getMessage());}
}
