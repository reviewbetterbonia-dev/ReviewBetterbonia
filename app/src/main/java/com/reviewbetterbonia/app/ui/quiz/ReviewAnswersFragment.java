package com.reviewbetterbonia.app.ui.quiz;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import com.reviewbetterbonia.app.model.*;
import com.reviewbetterbonia.app.ui.*;
import java.util.List;

public class ReviewAnswersFragment extends BaseFragment {

 @Nullable public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b) {
     Context x = requireContext();
     LinearLayout p = Ui.page(x);
     Ui.add(p, Ui.heading(x, "Review Answers"), Ui.dp(x, 24));
     Ui.add(p, Ui.muted(x, "Check your answers, correct choices, and explanations.", 14), Ui.dp(x, 16));

     List<Question> session = app().lastSession;
     List<Integer> userAnswers = app().lastUserAnswers;

     ScrollView sv = new ScrollView(x);
     LinearLayout body = new LinearLayout(x);
     body.setOrientation(LinearLayout.VERTICAL);

     if(session != null && userAnswers != null){
         for(int idx = 0; idx < session.size(); idx++){
             Question q = session.get(idx);
             int chosen = idx < userAnswers.size() ? userAnswers.get(idx) : -1;
             boolean correct = chosen >= 0 && chosen < q.answers.size() && q.answers.get(chosen).correct;

             LinearLayout card = Ui.card(x);

             LinearLayout header = new LinearLayout(x);
             header.setOrientation(LinearLayout.HORIZONTAL);
             TextView numView = Ui.text(x, "Question " + (idx + 1), 15, true);
             header.addView(numView, new LinearLayout.LayoutParams(0, -2, 1));

             TextView badge = Ui.text(x, correct ? "Correct" : "Incorrect", 13, true);
             badge.setTextColor(correct ? Ui.GREEN : Ui.RED);
             header.addView(badge);
             card.addView(header);
             card.addView(Ui.gap(x, 8));

             TextView qText = Ui.text(x, q.html, 16, true);
             card.addView(qText);
             card.addView(Ui.gap(x, 12));

             for(int ansIdx = 0; ansIdx < q.answers.size(); ansIdx++){
                 Answer an = q.answers.get(ansIdx);
                 TextView ansView = new TextView(x);
                 ansView.setText(Ui.fromHtml(an.html));
                 ansView.setTextSize(15);
                 ansView.setPadding(Ui.dp(x, 12), Ui.dp(x, 10), Ui.dp(x, 12), Ui.dp(x, 10));

                 GradientDrawable gd = new GradientDrawable();
                 gd.setCornerRadius(Ui.dp(x, 10));

                 if(ansIdx == chosen){
                     if(correct){
                         gd.setColor(Color.rgb(235, 247, 240));
                         gd.setStroke(Ui.dp(x, 2), Ui.GREEN);
                         ansView.setTextColor(Ui.GREEN);
                     } else {
                         gd.setColor(Color.rgb(253, 240, 240));
                         gd.setStroke(Ui.dp(x, 2), Ui.RED);
                         ansView.setTextColor(Ui.RED);
                     }
                 } else if(an.correct){
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

             String feedback = correct ? q.correctFeedback : q.incorrectFeedback;
             if(feedback != null && !feedback.trim().isEmpty()){
                 card.addView(Ui.gap(x, 4));
                 TextView fbView = Ui.muted(x, "Explanation: " + feedback, 13);
                 card.addView(fbView);
             }

             LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
             cardLp.setMargins(0, 0, 0, Ui.dp(x, 16));
             body.addView(card, cardLp);
         }
     }

     sv.addView(body);
     Ui.addWeight(p, sv);

     Button backBtn = Ui.button(x, "Back to Results", false);
     backBtn.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
     Ui.add(p, backBtn, Ui.dp(x, 56));

     return p;
 }
}
