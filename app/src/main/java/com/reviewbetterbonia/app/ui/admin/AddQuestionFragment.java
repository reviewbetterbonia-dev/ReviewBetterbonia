package com.reviewbetterbonia.app.ui.admin;

import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.Nullable;

import com.reviewbetterbonia.app.ui.*;

import java.util.*;

public class AddQuestionFragment extends BaseFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context x = requireContext();
        LinearLayout p = Ui.page(x);
        Ui.add(p, Ui.heading(x, "Add question"), Ui.dp(x, 42));

        EditText subject = field(x, "Subject");
        EditText category = field(x, "Category");
        EditText question = field(x, "Question");
        question.setSingleLine(false);
        question.setGravity(Gravity.TOP | Gravity.START);

        p.addView(subject);
        p.addView(category);
        p.addView(question, new LinearLayout.LayoutParams(-1, Ui.dp(x, 100)));

        EditText[] choices = new EditText[4];
        for (int j = 0; j < 4; j++) {
            choices[j] = field(x, "Choice " + (char) ('A' + j));
            p.addView(choices[j]);
        }

        Spinner correct = new Spinner(x);
        correct.setAdapter(new ArrayAdapter<>(
                x,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"A", "B", "C", "D"}
        ));
        Ui.add(p, Ui.label(x, "Correct answer"), Ui.dp(x, 26));
        Ui.add(p, correct, Ui.dp(x, 52));

        List<String> files = questionBankFiles(x);
        Spinner targetFile = new Spinner(x);
        String[] displayFiles = files.toArray(new String[0]);
        targetFile.setAdapter(new ArrayAdapter<>(
                x,
                android.R.layout.simple_spinner_dropdown_item,
                displayFiles.length == 0 ? new String[]{"No CSV files found"} : displayFiles
        ));
        Ui.add(p, Ui.label(x, "Question bank CSV"), Ui.dp(x, 26));
        Ui.add(p, targetFile, Ui.dp(x, 52));

        Ui.addWeight(p, new Space(x));

        Button save = Ui.button(x, "Submit for approval", true);
        save.setOnClickListener(v -> {
            String cleanSubject = subject.getText().toString().trim();
            String cleanCategory = category.getText().toString().trim();
            String cleanQuestion = question.getText().toString().trim();
            String[] ch = new String[4];
            for (int j = 0; j < 4; j++) ch[j] = choices[j].getText().toString().trim();

            if (cleanSubject.isEmpty() || cleanCategory.isEmpty() || cleanQuestion.isEmpty()) {
                Toast.makeText(x, "Complete the required fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            for (int j = 0; j < ch.length; j++) {
                if (ch[j].isEmpty()) {
                    Toast.makeText(x, "Choice " + (char) ('A' + j) + " cannot be empty.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if (files.isEmpty()) {
                Toast.makeText(x, "No question-bank CSV files are available.", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedFile = files.get(targetFile.getSelectedItemPosition());
            app().firebase.submitQuestion(
                    app().user.uid,
                    app().user.username,
                    cleanSubject,
                    cleanCategory,
                    cleanQuestion,
                    ch,
                    correct.getSelectedItem().toString(),
                    "Normal",
                    selectedFile,
                    ok -> {
                        Toast.makeText(
                                x,
                                ok ? "Question submitted for approval." : "Could not submit question.",
                                Toast.LENGTH_SHORT
                        ).show();
                        if (ok) requireActivity().getSupportFragmentManager().popBackStack();
                    }
            );
        });
        Ui.add(p, save, Ui.dp(x, 56));

        Button back = Ui.button(x, "Cancel", false);
        back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        Ui.add(p, back, Ui.dp(x, 52));
        return p;
    }

    private EditText field(Context x, String hint) {
        EditText e = new EditText(x);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setPadding(Ui.dp(x, 8), 0, Ui.dp(x, 8), 0);
        e.setLayoutParams(new LinearLayout.LayoutParams(-1, Ui.dp(x, 54)));
        return e;
    }

    private List<String> questionBankFiles(Context x) {
        List<String> out = new ArrayList<>();
        collectCsvAssets(x, "questions", out);
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    private void collectCsvAssets(Context x, String path, List<String> out) {
        try {
            String[] entries = x.getAssets().list(path);
            if (entries == null) return;
            Arrays.sort(entries, String.CASE_INSENSITIVE_ORDER);
            for (String entry : entries) {
                String full = path + "/" + entry;
                String[] children = x.getAssets().list(full);
                if (children != null && children.length > 0) {
                    collectCsvAssets(x, full, out);
                } else if (entry.toLowerCase(Locale.US).endsWith(".csv")) {
                    out.add(full.substring("questions/".length()));
                }
            }
        } catch (Exception ignored) {
        }
    }
}
