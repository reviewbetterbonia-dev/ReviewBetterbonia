package com.reviewbetterbonia.app.ui.admin;

import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.Nullable;

import com.reviewbetterbonia.app.model.Answer;
import com.reviewbetterbonia.app.model.Question;
import com.reviewbetterbonia.app.ui.*;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AddQuestionFragment extends BaseFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context x = requireContext();
        ScrollView sv = new ScrollView(x);
        sv.setFillViewport(true);
        LinearLayout p = Ui.page(x);
        sv.addView(p);

        Ui.add(p, Ui.heading(x, "Add Question"), Ui.dp(x, 44));

        TextView modeLabel = Ui.label(x, "Question Type");
        p.addView(modeLabel);

        RadioGroup typeGroup = new RadioGroup(x);
        typeGroup.setOrientation(RadioGroup.VERTICAL);

        RadioButton rbPublic = new RadioButton(x);
        rbPublic.setText("Public Question (For Approval)");
        rbPublic.setTextSize(15);
        rbPublic.setId(View.generateViewId());

        RadioButton rbPrivate = new RadioButton(x);
        rbPrivate.setText("Private Question");
        rbPrivate.setTextSize(15);
        rbPrivate.setId(View.generateViewId());

        typeGroup.addView(rbPublic);
        typeGroup.addView(rbPrivate);
        rbPublic.setChecked(true);

        LinearLayout.LayoutParams rgLp = new LinearLayout.LayoutParams(-1, -2);
        rgLp.setMargins(0, Ui.dp(x, 4), 0, Ui.dp(x, 16));
        p.addView(typeGroup, rgLp);

        // Subject section
        TextView subjectLabel = Ui.label(x, "Subject");
        p.addView(subjectLabel);

        // Public subject spinner
        Spinner publicSubjectSpinner = new Spinner(x);
        String[] publicSubjects = new String[]{"Math", "Machine Design", "Powerplant"};
        publicSubjectSpinner.setAdapter(new ArrayAdapter<>(
                x,
                android.R.layout.simple_spinner_dropdown_item,
                publicSubjects
        ));
        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 50));
        spinLp.setMargins(0, Ui.dp(x, 4), 0, Ui.dp(x, 4));
        p.addView(publicSubjectSpinner, spinLp);

        TextView targetInfoText = Ui.muted(x, "Will be saved under algebra/addedQuestions.csv upon admin approval.", 12);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1, -2);
        infoLp.setMargins(0, 0, 0, Ui.dp(x, 16));
        p.addView(targetInfoText, infoLp);

        // Private subject text view (read-only displaying user's name)
        String userName = app().user != null && app().user.username != null && !app().user.username.trim().isEmpty()
                ? app().user.username.trim()
                : "My Questions";
        TextView privateSubjectText = Ui.text(x, "Subject: " + userName + " (Private)", 15, true);
        privateSubjectText.setVisibility(View.GONE);
        LinearLayout.LayoutParams privLp = new LinearLayout.LayoutParams(-1, -2);
        privLp.setMargins(0, Ui.dp(x, 4), 0, Ui.dp(x, 16));
        p.addView(privateSubjectText, privLp);

        publicSubjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String subj = publicSubjects[position];
                if ("Math".equalsIgnoreCase(subj)) {
                    targetInfoText.setText("Will be saved under algebra/addedQuestions.csv upon admin approval.");
                } else if ("Machine Design".equalsIgnoreCase(subj)) {
                    targetInfoText.setText("Will be saved under machinedesign/addedQuestions.csv upon admin approval.");
                } else if ("Powerplant".equalsIgnoreCase(subj)) {
                    targetInfoText.setText("Will be saved under powerplant/addedQuestions.csv upon admin approval.");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        typeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isPublic = (checkedId == rbPublic.getId());
            if (isPublic) {
                publicSubjectSpinner.setVisibility(View.VISIBLE);
                targetInfoText.setVisibility(View.VISIBLE);
                privateSubjectText.setVisibility(View.GONE);
            } else {
                publicSubjectSpinner.setVisibility(View.GONE);
                targetInfoText.setVisibility(View.GONE);
                privateSubjectText.setVisibility(View.VISIBLE);
            }
        });

        EditText category = field(x, "Category");
        EditText question = field(x, "Question");
        question.setSingleLine(false);
        question.setGravity(Gravity.TOP | Gravity.START);

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
        Ui.add(p, correct, Ui.dp(x, 50));

        Ui.addWeight(p, new Space(x));

        Button save = Ui.button(x, "Submit Question", true);
        save.setOnClickListener(v -> {
            boolean isPublic = (typeGroup.getCheckedRadioButtonId() == rbPublic.getId());
            String cleanCategory = category.getText().toString().trim();
            String cleanQuestion = question.getText().toString().trim();
            String[] ch = new String[4];
            for (int j = 0; j < 4; j++) ch[j] = choices[j].getText().toString().trim();

            if (cleanCategory.isEmpty() || cleanQuestion.isEmpty()) {
                Toast.makeText(x, "Category and Question cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            for (int j = 0; j < ch.length; j++) {
                if (ch[j].isEmpty()) {
                    Toast.makeText(x, "Choice " + (char) ('A' + j) + " cannot be empty.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (isPublic) {
                String selectedSubject = publicSubjects[publicSubjectSpinner.getSelectedItemPosition()];
                String targetFile;
                if ("Math".equalsIgnoreCase(selectedSubject)) {
                    targetFile = "algebra/addedQuestions.csv";
                } else if ("Machine Design".equalsIgnoreCase(selectedSubject)) {
                    targetFile = "machinedesign/addedQuestions.csv";
                } else {
                    targetFile = "powerplant/addedQuestions.csv";
                }

                app().firebase.submitQuestion(
                        app().user.uid,
                        app().user.username,
                        selectedSubject,
                        cleanCategory,
                        cleanQuestion,
                        ch,
                        correct.getSelectedItem().toString(),
                        "Normal",
                        targetFile,
                        ok -> {
                            Toast.makeText(
                                    x,
                                    ok ? "Question submitted for admin approval." : "Could not submit question.",
                                    Toast.LENGTH_SHORT
                            ).show();
                            if (ok) requireActivity().getSupportFragmentManager().popBackStack();
                        }
                );
            } else {
                String privateSubject = userName;
                String qId = "priv_" + System.currentTimeMillis();
                Question q = new Question();
                q.id = qId;
                q.subject = privateSubject;
                q.category = cleanCategory;
                q.html = cleanQuestion;

                String correctLetter = correct.getSelectedItem().toString();
                for (int j = 0; j < 4; j++) {
                    Answer a = new Answer();
                    a.html = ch[j];
                    a.correct = String.valueOf((char) ('A' + j)).equalsIgnoreCase(correctLetter);
                    q.answers.add(a);
                }

                savePrivateQuestionToCsv(x, q, ch[0], ch[1], ch[2], ch[3], correctLetter);
                app().quiz.addPrivateQuestion(q);

                Toast.makeText(x, "Private question added successfully!", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
        Ui.add(p, save, Ui.dp(x, 56));

        Button back = Ui.button(x, "Cancel", false);
        back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        Ui.add(p, back, Ui.dp(x, 52));

        return sv;
    }

    private EditText field(Context x, String hint) {
        EditText e = new EditText(x);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setPadding(Ui.dp(x, 8), 0, Ui.dp(x, 8), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 54));
        lp.setMargins(0, 0, 0, Ui.dp(x, 8));
        e.setLayoutParams(lp);
        return e;
    }

    private void savePrivateQuestionToCsv(Context context, Question q, String choiceA, String choiceB, String choiceC, String choiceD, String correctLetter) {
        String csvRow = String.format(Locale.US, "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                escapeCsv(q.id),
                escapeCsv(q.subject),
                escapeCsv(q.category),
                escapeCsv(q.html),
                escapeCsv(choiceA),
                escapeCsv(choiceB),
                escapeCsv(choiceC),
                escapeCsv(choiceD),
                escapeCsv(correctLetter),
                escapeCsv(q.correctFeedback),
                escapeCsv(q.incorrectFeedback)
        );

        // 1. Save to internal app storage
        File localFile = new File(context.getFilesDir(), "addedQuestions.csv");
        boolean needHeader = !localFile.exists() || localFile.length() == 0;
        try (FileOutputStream fos = new FileOutputStream(localFile, true)) {
            if (needHeader) {
                fos.write("id,subject,category,question,choice_a,choice_b,choice_c,choice_d,answer,correct_feedback,incorrect_feedback\n".getBytes(StandardCharsets.UTF_8));
            }
            fos.write(csvRow.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Also save to target project asset path if running in dev host environment
        File projectFile = new File("C:/Users/ACER/Documents/Github/ReviewBetterbonia/app/src/main/assets/questions/addedQuestions.csv");
        if (projectFile.exists() || (projectFile.getParentFile() != null && projectFile.getParentFile().exists())) {
            try {
                boolean projNeedHeader = !projectFile.exists() || projectFile.length() == 0;
                try (FileOutputStream fos = new FileOutputStream(projectFile, true)) {
                    if (projNeedHeader) {
                        fos.write("id,subject,category,question,choice_a,choice_b,choice_c,choice_d,answer,correct_feedback,incorrect_feedback\n".getBytes(StandardCharsets.UTF_8));
                    }
                    fos.write(csvRow.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {}
        }
    }

    private static String escapeCsv(String str) {
        if (str == null) return "";
        String s = str.replace("\r", "");
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
