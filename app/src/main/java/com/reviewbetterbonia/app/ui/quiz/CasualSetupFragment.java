package com.reviewbetterbonia.app.ui.quiz;

import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.reviewbetterbonia.app.MainActivity;
import com.reviewbetterbonia.app.model.Question;
import com.reviewbetterbonia.app.ui.*;
import java.util.*;

public class CasualSetupFragment extends BaseFragment {
    @Nullable public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle b) {
        Context x = requireContext();
        MainActivity a = app();

        ScrollView sv = new ScrollView(x);
        sv.setFillViewport(true);
        LinearLayout p = Ui.page(x);
        sv.addView(p);

        TextView heading = Ui.heading(x, "Casual review");
        p.addView(heading);

        TextView subtitle = Ui.muted(x, "Take a peek at the questions or build a casual quiz.", 14);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.setMargins(0, 0, 0, Ui.dp(x, 16));
        p.addView(subtitle, subLp);

        Button searchPageBtn = Ui.button(x, "Search & View Questions", true);
        searchPageBtn.setOnClickListener(v -> a.navigate(new CasualSearchFragment(), true));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 52));
        btnLp.setMargins(0, 0, 0, Ui.dp(x, 24));
        p.addView(searchPageBtn, btnLp);

        TextView setupHeading = Ui.heading(x, "Casual Quiz Setup");
        setupHeading.setTextSize(20);
        LinearLayout.LayoutParams setupLp = new LinearLayout.LayoutParams(-1, -2);
        setupLp.setMargins(0, 0, 0, Ui.dp(x, 12));
        p.addView(setupHeading, setupLp);

        TextView subjectLabel = Ui.label(x, "Subject");
        p.addView(subjectLabel);
        Spinner subject = new Spinner(x);
        ArrayAdapter<String> sa = new ArrayAdapter<>(x, android.R.layout.simple_spinner_dropdown_item, a.quiz.subjects);
        subject.setAdapter(sa);
        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 50));
        spinLp.setMargins(0, Ui.dp(x, 4), 0, Ui.dp(x, 16));
        p.addView(subject, spinLp);

        TextView categoryLabel = Ui.label(x, "Category");
        p.addView(categoryLabel);
        Spinner category = new Spinner(x);
        LinearLayout.LayoutParams catLp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 50));
        catLp.setMargins(0, Ui.dp(x, 4), 0, Ui.dp(x, 16));
        p.addView(category, catLp);

        subject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id){
                String s = a.quiz.subjects.get(pos);
                List<String> cats = new ArrayList<>();
                cats.add("All Categories");
                cats.addAll("All Subjects".equals(s) ? a.quiz.allCategories() : a.quiz.categoriesFor(s));
                category.setAdapter(new ArrayAdapter<>(x, android.R.layout.simple_spinner_dropdown_item, cats));
            }
        });

        TextView countLabel = Ui.label(x, "Questions");
        p.addView(countLabel);
        Spinner count = new Spinner(x);
        String[] counts = {"10", "20", "30", "40", "50", "100"};
        count.setAdapter(new ArrayAdapter<>(x, android.R.layout.simple_spinner_dropdown_item, counts));
        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 50));
        countLp.setMargins(0, Ui.dp(x, 4), 0, Ui.dp(x, 16));
        p.addView(count, countLp);

        CheckBox timed = new CheckBox(x);
        timed.setText("Timed mode");
        timed.setTextSize(15);
        LinearLayout.LayoutParams timedLp = new LinearLayout.LayoutParams(-1, -2);
        timedLp.setMargins(0, 0, 0, Ui.dp(x, 12));
        p.addView(timed, timedLp);

        TextView secLabel = Ui.label(x, "Seconds per question");
        p.addView(secLabel);
        EditText seconds = new EditText(x);
        seconds.setHint("Default 30");
        seconds.setInputType(2);
        LinearLayout.LayoutParams secLp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 50));
        secLp.setMargins(0, Ui.dp(x, 4), 0, Ui.dp(x, 24));
        p.addView(seconds, secLp);

        LinearLayout.LayoutParams spaceLp = new LinearLayout.LayoutParams(-1, 0, 1);
        p.addView(new Space(x), spaceLp);

        Button start = Ui.button(x, "Start casual quiz", true);
        LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 52));
        startLp.setMargins(0, 0, 0, Ui.dp(x, 12));
        p.addView(start, startLp);
        start.setOnClickListener(v -> {
            String s = subject.getSelectedItem().toString();
            String cat = category.getSelectedItem() == null ? "All Categories" : category.getSelectedItem().toString();
            int n = Integer.parseInt(count.getSelectedItem().toString());
            List<Question> pool = a.quiz.pool(s, cat);
            if (pool.size() < n) {
                Toast.makeText(x, "Only " + pool.size() + " questions match this selection.", Toast.LENGTH_SHORT).show();
                return;
            }
            int secFinal = 30;
            try {
                if (!seconds.getText().toString().trim().isEmpty())
                    secFinal = Math.max(5, Integer.parseInt(seconds.getText().toString().trim()));
            } catch (Exception ignored) {}
            a.navigate(QuizFragment.newCasual(s, cat, n, timed.isChecked(), secFinal), true);
        });

        Button back = Ui.button(x, "Back", false);
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 52));
        backLp.setMargins(0, 0, 0, Ui.dp(x, 16));
        p.addView(back, backLp);
        back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return sv;
    }
}
