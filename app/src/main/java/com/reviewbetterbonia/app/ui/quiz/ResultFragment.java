package com.reviewbetterbonia.app.ui.quiz;
import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import com.reviewbetterbonia.app.model.QuizResult;
import com.reviewbetterbonia.app.ui.*;
import java.util.*;

public class ResultFragment extends BaseFragment{
 private QuizResult r;
 private boolean ranked;
 private final Map<String, int[]> subjectBreakdown = new LinkedHashMap<>();

 public static ResultFragment newResult(QuizResult r, boolean ranked, ArrayList<String> keys, ArrayList<Integer> corrects, ArrayList<Integer> totals){
  ResultFragment f=new ResultFragment();
  f.r=r;
  f.ranked=ranked;
  Bundle b=new Bundle();
  b.putString("mode",r.mode);
  b.putInt("score",r.score);
  b.putInt("total",r.total);
  b.putInt("sec",r.seconds);
  b.putLong("time",r.time);
  b.putString("subject",r.subject);
  b.putString("category",r.category);
  b.putBoolean("rank",ranked);
  if(keys != null){
   b.putStringArrayList("subKeys", keys);
   b.putIntegerArrayList("subCorrects", corrects);
   b.putIntegerArrayList("subTotals", totals);
  }
  f.setArguments(b);
  return f;
 }

 public static ResultFragment newResult(QuizResult r, boolean ranked){
  return newResult(r, ranked, null, null, null);
 }

 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  if(r==null&&b!=null) r=new QuizResult(b.getString("mode"),b.getInt("score"),b.getInt("total"),b.getInt("sec"),b.getLong("time"),b.getString("subject"),b.getString("category"));
  ranked=b!=null&&b.getBoolean("rank");
  Bundle args = getArguments();
  if(args!=null && args.containsKey("subKeys")){
   ArrayList<String> keys = args.getStringArrayList("subKeys");
   ArrayList<Integer> corrects = args.getIntegerArrayList("subCorrects");
   ArrayList<Integer> totals = args.getIntegerArrayList("subTotals");
   if(keys != null && corrects != null && totals != null){
    for(int i = 0; i < keys.size(); i++){
     subjectBreakdown.put(keys.get(i), new int[]{corrects.get(i), totals.get(i)});
    }
   }
  }
 }

 @Nullable public View onCreateView(LayoutInflater i,ViewGroup c,Bundle b){
  Context x=requireContext();
  LinearLayout p=Ui.page(x);
  Ui.add(p,Ui.heading(x,ranked?"Ranked match complete":"Quiz complete"),Ui.dp(x,42));
  
  LinearLayout hero=Ui.card(x);
  hero.setGravity(Gravity.CENTER);
  hero.addView(Ui.text(x,r.score+" / "+r.total,42,true));
  hero.addView(Ui.muted(x,r.accuracy()+"% accuracy  •  "+format(r.seconds),15));
  p.addView(hero);
  Ui.add(p,Ui.gap(x,12),Ui.dp(x,12));

  LinearLayout note=Ui.card(x);
  note.addView(Ui.text(x,r.accuracy()>=80?"Strong session":"Keep building the habit",19,true));
  note.addView(Ui.muted(x,"Your result is saved in Progress."+(ranked?" Rating: "+app().rating+" RP":""),14));
  p.addView(note);
  Ui.add(p,Ui.gap(x,12),Ui.dp(x,12));

  if(!subjectBreakdown.isEmpty()){
   LinearLayout subjectCard=Ui.card(x);
   subjectCard.addView(Ui.text(x,"Performance by subject",19,true));
   subjectCard.addView(Ui.gap(x,8));

   for(Map.Entry<String, int[]> entry : subjectBreakdown.entrySet()){
    String subName = entry.getKey();
    int correct = entry.getValue()[0];
    int total = entry.getValue()[1];
    int pct = total == 0 ? 0 : (int)Math.round(correct * 100.0 / total);

    LinearLayout row = new LinearLayout(x);
    row.setOrientation(LinearLayout.HORIZONTAL);

    TextView nameView = Ui.text(x, subName, 15, true);
    row.addView(nameView, new LinearLayout.LayoutParams(0, -2, 1));

    TextView statView = Ui.muted(x, correct + " / " + total + " (" + pct + "%)", 14);
    row.addView(statView);

    subjectCard.addView(row);
    subjectCard.addView(Ui.gap(x, 4));
   }
   p.addView(subjectCard);
  }

  Ui.addWeight(p,new Space(x));
  Button review=Ui.button(x,"Review Answers",false);
  review.setOnClickListener(v->app().navigate(new ReviewAnswersFragment(),true));
  Ui.add(p,review,Ui.dp(x,56));
  Ui.add(p,Ui.gap(x,10),Ui.dp(x,10));

  Button again=Ui.button(x,"Back to home",true);
  again.setOnClickListener(v->app().clearAndShowHome());
  Ui.add(p,again,Ui.dp(x,56));
  return p;
 }

 private String format(int s){return String.format(Locale.US,"%02d:%02d",s/60,s%60);}
}
