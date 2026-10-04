package com.reviewbetterbonia.app.data;

import android.content.Context;
import com.reviewbetterbonia.app.model.Answer;
import com.reviewbetterbonia.app.model.Question;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

public class QuizRepository {
    private final Context context;
    public final List<Question> questions = new ArrayList<>();
    public final List<String> subjects = new ArrayList<>();
    public final Map<String, List<String>> categoriesBySubject = new LinkedHashMap<>();

    public QuizRepository(Context c) {
        context = c.getApplicationContext();
        load();
    }

    public void load() {
        questions.clear();
        subjects.clear();
        categoriesBySubject.clear();
        try {
            walk("questions");
        } catch (Exception e) {
            throw new RuntimeException("Could not load question bank", e);
        }
        loadLocalPrivateQuestions();
        sortSubjectsAndCategories();
    }

    private void sortSubjectsAndCategories() {
        Collections.sort(subjects, (a, b) -> {
            if ("All Subjects".equals(a)) return -1;
            if ("All Subjects".equals(b)) return 1;
            return compareNatural(a, b);
        });
        subjects.remove("All Subjects");
        subjects.add(0, "All Subjects");
        for (List<String> x : categoriesBySubject.values()) {
            Collections.sort(x, QuizRepository::compareNatural);
        }
    }

    public static int compareNatural(String s1, String s2) {
        if (s1 == null) return s2 == null ? 0 : -1;
        if (s2 == null) return 1;

        int len1 = s1.length();
        int len2 = s2.length();
        int idx1 = 0, idx2 = 0;

        while (idx1 < len1 && idx2 < len2) {
            char c1 = s1.charAt(idx1);
            char c2 = s2.charAt(idx2);

            boolean isDigit1 = Character.isDigit(c1);
            boolean isDigit2 = Character.isDigit(c2);

            if (isDigit1 && isDigit2) {
                long num1 = 0;
                while (idx1 < len1 && Character.isDigit(s1.charAt(idx1))) {
                    num1 = num1 * 10 + (s1.charAt(idx1) - '0');
                    idx1++;
                }

                long num2 = 0;
                while (idx2 < len2 && Character.isDigit(s2.charAt(idx2))) {
                    num2 = num2 * 10 + (s2.charAt(idx2) - '0');
                    idx2++;
                }

                if (num1 != num2) {
                    return Long.compare(num1, num2);
                }
            } else {
                char lower1 = Character.toLowerCase(c1);
                char lower2 = Character.toLowerCase(c2);
                if (lower1 != lower2) {
                    return Character.compare(lower1, lower2);
                }
                idx1++;
                idx2++;
            }
        }
        return Integer.compare(len1, len2);
    }

    private void walk(String path) throws Exception {
        String[] entries = context.getAssets().list(path);
        if (entries == null) return;
        Arrays.sort(entries, QuizRepository::compareNatural);
        for (String e : entries) {
            String full = path + "/" + e;
            String[] children = context.getAssets().list(full);
            if (children != null && children.length > 0) {
                walk(full);
            } else if (e.toLowerCase(Locale.US).endsWith(".csv")) {
                readCsv(full);
            }
        }
    }

    private void readCsv(String path) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open(path), "UTF-8"))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;
                List<String> r = parse(line);
                if (r.size() < 11) continue;
                Question q = new Question();
                q.id = r.get(0).trim();
                q.subject = r.get(1).trim();
                q.category = r.get(2).trim();
                q.html = r.get(3);
                q.correctFeedback = r.get(9);
                q.incorrectFeedback = r.get(10);
                String correct = r.get(8).trim();
                String[] choices = {r.get(4), r.get(5), r.get(6), r.get(7)};
                for (int i = 0; i < 4; i++) {
                    Answer a = new Answer();
                    a.html = choices[i];
                    a.correct = correct.equalsIgnoreCase(String.valueOf((char) ('A' + i))) || correct.equals(String.valueOf(i));
                    q.answers.add(a);
                }
                boolean found = false;
                for (Answer a : q.answers) if (a.correct) found = true;
                if (!found) q.answers.get(0).correct = true;
                questions.add(q);
                if (!q.subject.isEmpty() && !subjects.contains(q.subject)) subjects.add(q.subject);
                if (!q.subject.isEmpty()) {
                    List<String> cats = categoriesBySubject.get(q.subject);
                    if (cats == null) {
                        cats = new ArrayList<>();
                        categoriesBySubject.put(q.subject, cats);
                    }
                    if (!q.category.isEmpty() && !cats.contains(q.category)) cats.add(q.category);
                }
            }
        }
    }

    private List<String> parse(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    b.append('"');
                    i++;
                } else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                out.add(b.toString());
                b.setLength(0);
            } else b.append(c);
        }
        out.add(b.toString());
        return out;
    }

    public List<String> categoriesFor(String subject) {
        List<String> cats = categoriesBySubject.get(subject);
        return cats != null ? cats : Collections.emptyList();
    }

    public List<String> allCategories() {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (List<String> x : categoriesBySubject.values()) set.addAll(x);
        return new ArrayList<>(set);
    }

    public List<String> publicSubjects() {
        List<String> list = new ArrayList<>();
        for (String s : subjects) {
            if ("All Subjects".equals(s)) {
                list.add(s);
                continue;
            }
            boolean hasPublic = false;
            for (Question q : questions) {
                if (s.equalsIgnoreCase(q.subject) && !q.isPrivate()) {
                    hasPublic = true;
                    break;
                }
            }
            if (hasPublic) {
                list.add(s);
            }
        }
        return list;
    }

    public List<String> publicCategoriesFor(String subject) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (Question q : questions) {
            if (!q.isPrivate() && q.category != null && !q.category.trim().isEmpty()) {
                if ("All Subjects".equals(subject) || subject.equalsIgnoreCase(q.subject)) {
                    set.add(q.category.trim());
                }
            }
        }
        List<String> list = new ArrayList<>(set);
        Collections.sort(list, QuizRepository::compareNatural);
        return list;
    }

    public List<String> publicCategories() {
        return publicCategoriesFor("All Subjects");
    }

    public void mergeRemoteQuestions(List<Question> remote) {
        for (Question rq : remote) {
            boolean exists = false;
            for (Question q : questions) if (q.id != null && q.id.equals(rq.id)) { exists = true; break; }
            if (exists) continue;
            questions.add(rq);
            if (rq.subject != null && !rq.subject.isEmpty() && !subjects.contains(rq.subject)) subjects.add(rq.subject);
            if (rq.subject != null && !rq.subject.isEmpty()) {
                List<String> cats = categoriesBySubject.get(rq.subject);
                if (cats == null) {
                    cats = new ArrayList<>();
                    categoriesBySubject.put(rq.subject, cats);
                }
                if (rq.category != null && !rq.category.isEmpty() && !cats.contains(rq.category)) cats.add(rq.category);
            }
        }
        sortSubjectsAndCategories();
    }

    public List<Question> pool(String subject, String category) {
        return pool(subject, category, "");
    }

    public List<Question> pool(String subject, String category, String searchQuery) {
        List<Question> out = new ArrayList<>();
        String query = searchQuery == null ? "" : searchQuery.toLowerCase(Locale.US).trim();
        for (Question q : questions) {
            boolean isPriv = q.isPrivate();
            if ("All Subjects".equals(subject) && isPriv) continue;
            if (!"All Subjects".equals(subject) && !subject.equals(q.subject)) continue;
            if (!"All Categories".equals(category) && !category.equals(q.category)) continue;
            if (!query.isEmpty()) {
                boolean match = (q.html != null && q.html.toLowerCase(Locale.US).contains(query)) ||
                                (q.subject != null && q.subject.toLowerCase(Locale.US).contains(query)) ||
                                (q.category != null && q.category.toLowerCase(Locale.US).contains(query)) ||
                                (q.correctFeedback != null && q.correctFeedback.toLowerCase(Locale.US).contains(query)) ||
                                (q.incorrectFeedback != null && q.incorrectFeedback.toLowerCase(Locale.US).contains(query)) ||
                                (q.id != null && q.id.toLowerCase(Locale.US).contains(query));
                if (!match) {
                    for (Answer a : q.answers) {
                        if (a.html != null && a.html.toLowerCase(Locale.US).contains(query)) {
                            match = true;
                            break;
                        }
                    }
                }
                if (!match) continue;
            }
            out.add(q);
        }
        return out;
    }

    private void loadLocalPrivateQuestions() {
        File file = new File(context.getFilesDir(), "addedQuestions.csv");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;
                List<String> r = parse(line);
                if (r.size() < 11) continue;
                Question q = new Question();
                q.id = r.get(0).trim();
                q.subject = r.get(1).trim();
                q.category = r.get(2).trim();
                q.html = r.get(3);
                q.correctFeedback = r.get(9);
                q.incorrectFeedback = r.get(10);
                q.isPrivate = true;
                String correct = r.get(8).trim();
                String[] choices = {r.get(4), r.get(5), r.get(6), r.get(7)};
                for (int i = 0; i < 4; i++) {
                    Answer a = new Answer();
                    a.html = choices[i];
                    a.correct = correct.equalsIgnoreCase(String.valueOf((char) ('A' + i))) || correct.equals(String.valueOf(i));
                    q.answers.add(a);
                }
                boolean found = false;
                for (Answer a : q.answers) if (a.correct) found = true;
                if (!found) q.answers.get(0).correct = true;

                boolean exists = false;
                for (Question existing : questions) {
                    if (existing.id != null && existing.id.equals(q.id)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    questions.add(q);
                    if (!q.subject.isEmpty() && !subjects.contains(q.subject)) subjects.add(q.subject);
                    if (!q.subject.isEmpty()) {
                        List<String> cats = categoriesBySubject.get(q.subject);
                        if (cats == null) {
                            cats = new ArrayList<>();
                            categoriesBySubject.put(q.subject, cats);
                        }
                        if (!q.category.isEmpty() && !cats.contains(q.category)) cats.add(q.category);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addPrivateQuestion(Question q) {
        if (q == null) return;
        q.isPrivate = true;
        boolean exists = false;
        for (Question existing : questions) {
            if (existing.id != null && existing.id.equals(q.id)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            questions.add(q);
            if (q.subject != null && !q.subject.isEmpty() && !subjects.contains(q.subject)) {
                subjects.add(q.subject);
            }
            if (q.subject != null && !q.subject.isEmpty()) {
                List<String> cats = categoriesBySubject.get(q.subject);
                if (cats == null) {
                    cats = new ArrayList<>();
                    categoriesBySubject.put(q.subject, cats);
                }
                if (q.category != null && !q.category.isEmpty() && !cats.contains(q.category)) {
                    cats.add(q.category);
                }
            }
            sortSubjectsAndCategories();
        }
    }
}
