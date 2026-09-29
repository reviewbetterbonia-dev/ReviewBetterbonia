package com.reviewbetterbonia.app.ui.quiz;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.reviewbetterbonia.app.MainActivity;
import com.reviewbetterbonia.app.model.*;
import com.reviewbetterbonia.app.ui.*;
import java.util.*;

public class CasualSearchFragment extends BaseFragment {
    @Nullable public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle b) {
        Context x = requireContext();
        MainActivity a = app();

        ScrollView sv = new ScrollView(x);
        sv.setFillViewport(true);
        LinearLayout p = Ui.page(x);
        sv.addView(p);

        TextView heading = Ui.heading(x, "Search questions");
        p.addView(heading);

        TextView subtitle = Ui.muted(x, "Search and review questions, answers, and explanations without taking a quiz.", 14);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.setMargins(0, 0, 0, Ui.dp(x, 16));
        p.addView(subtitle, subLp);

        TextView subLabel = Ui.label(x, "Subject");
        p.addView(subLabel);
        Spinner subject = new Spinner(x);
        ArrayAdapter<String> sa = new ArrayAdapter<>(x, android.R.layout.simple_spinner_dropdown_item, a.quiz.subjects);
        subject.setAdapter(sa);
        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 50));
        spinLp.setMargins(0, Ui.dp(x, 4), 0, Ui.dp(x, 16));
        p.addView(subject, spinLp);

        TextView catLabel = Ui.label(x, "Category");
        p.addView(catLabel);
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

        TextView queryLabel = Ui.label(x, "Search Keyword");
        p.addView(queryLabel);
        EditText searchInput = new EditText(x);
        searchInput.setHint("Enter keyword to search questions...");
        searchInput.setSingleLine(true);
        LinearLayout.LayoutParams queryLp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 50));
        queryLp.setMargins(0, Ui.dp(x, 4), 0, Ui.dp(x, 16));
        p.addView(searchInput, queryLp);

        Button searchBtn = Ui.button(x, "Search Questions", true);
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 52));
        searchLp.setMargins(0, 0, 0, Ui.dp(x, 16));
        p.addView(searchBtn, searchLp);

        LinearLayout resultsLayout = new LinearLayout(x);
        resultsLayout.setOrientation(LinearLayout.VERTICAL);
        p.addView(resultsLayout);

        searchBtn.setOnClickListener(v -> {
            resultsLayout.removeAllViews();
            String s = subject.getSelectedItem().toString();
            String cat = category.getSelectedItem() == null ? "All Categories" : category.getSelectedItem().toString();
            String query = searchInput.getText().toString().trim();

            List<Question> matching = a.quiz.pool(s, cat, query);
            if (matching.isEmpty()) {
                TextView empty = Ui.muted(x, "No questions found matching your search.", 14);
                LinearLayout.LayoutParams emptyLp = new LinearLayout.LayoutParams(-1, -2);
                emptyLp.setMargins(0, Ui.dp(x, 8), 0, Ui.dp(x, 16));
                resultsLayout.addView(empty, emptyLp);
                return;
            }

            TextView countText = Ui.text(x, "Found " + matching.size() + " question(s):", 15, true);
            LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(-1, -2);
            countLp.setMargins(0, Ui.dp(x, 8), 0, Ui.dp(x, 12));
            resultsLayout.addView(countText, countLp);

            int limit = Math.min(matching.size(), 50);
            for (int iQ = 0; iQ < limit; iQ++) {
                Question q = matching.get(iQ);
                LinearLayout card = Ui.card(x);

                TextView meta = Ui.muted(x, (q.subject.isEmpty() ? "General" : q.subject) + " • " + (q.category.isEmpty() ? "General" : q.category), 12);
                card.addView(meta);
                card.addView(Ui.gap(x, 6));

                TextView qText = Ui.text(x, q.html, 15, true);
                card.addView(qText);
                card.addView(Ui.gap(x, 10));

                for (int ansIdx = 0; ansIdx < q.answers.size(); ansIdx++) {
                    Answer an = q.answers.get(ansIdx);
                    TextView ansView = new TextView(x);
                    ansView.setText(Ui.fromHtml(((char)('A' + ansIdx)) + ". " + an.html));
                    ansView.setTextSize(14);
                    ansView.setPadding(Ui.dp(x, 12), Ui.dp(x, 10), Ui.dp(x, 12), Ui.dp(x, 10));

                    GradientDrawable gd = new GradientDrawable();
                    gd.setCornerRadius(Ui.dp(x, 8));

                    if (an.correct) {
                        gd.setColor(Color.rgb(235, 247, 240));
                        gd.setStroke(Ui.dp(x, 2), Ui.GREEN);
                        ansView.setTextColor(Ui.GREEN);
                    } else {
                        gd.setColor(Ui.CARD);
                        gd.setStroke(Ui.dp(x, 1), Ui.BORDER);
                        ansView.setTextColor(Ui.TEXT);
                    }
                    ansView.setBackground(gd);

                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                    lp.setMargins(0, 0, 0, Ui.dp(x, 6));
                    card.addView(ansView, lp);
                }

                String feedback = q.correctFeedback != null && !q.correctFeedback.trim().isEmpty() ? q.correctFeedback : q.incorrectFeedback;
                if (feedback != null && !feedback.trim().isEmpty()) {
                    card.addView(Ui.gap(x, 6));
                    TextView fbView = Ui.muted(x, "Explanation: " + feedback, 13);
                    card.addView(fbView);
                }

                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
                cardLp.setMargins(0, 0, 0, Ui.dp(x, 16));
                resultsLayout.addView(card, cardLp);
            }

            if (matching.size() > 50) {
                TextView limitText = Ui.muted(x, "Showing first 50 results. Refine your search to see more specific questions.", 13);
                LinearLayout.LayoutParams limitLp = new LinearLayout.LayoutParams(-1, -2);
                limitLp.setMargins(0, Ui.dp(x, 8), 0, Ui.dp(x, 16));
                resultsLayout.addView(limitText, limitLp);
            }
        });

        Button back = Ui.button(x, "Back", false);
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 52));
        backLp.setMargins(0, Ui.dp(x, 16), 0, Ui.dp(x, 24));
        p.addView(back, backLp);
        back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return sv;
    }
}
