package com.reviewbetterbonia.app.ui.quiz;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import com.reviewbetterbonia.app.MainActivity;
import com.reviewbetterbonia.app.R;
import com.reviewbetterbonia.app.model.*;
import com.reviewbetterbonia.app.ui.*;
import java.util.*;

public class QuizFragment extends BaseFragment{
 private String subject,category,searchQuery;
 private int count,secondsPer;
 private boolean timed,ranked;
 private List<Question> session;
 private final List<Integer> userAnswers = new ArrayList<>();
 private int index=0,score=0;
 private long started;
 private CountDownTimer timer;
 private TextView timerView, progView, questionTextView;
 private RadioGroup answers;
 private ProgressBar progressBar;
 private Button nextButton;
 private boolean timerStarted=false;
 private boolean submitting=false;
 private final Map<String, int[]> subjectCorrectTotal = new HashMap<>();

 public static QuizFragment newCasual(String s,String c,int n,boolean t,int sec){
  return newCasual(s, c, "", n, t, sec);
 }

 public static QuizFragment newCasual(String s,String c,String query,int n,boolean t,int sec){
  QuizFragment f=new QuizFragment();
  Bundle b=new Bundle();
  b.putString("s",s);
  b.putString("c",c);
  b.putString("query",query);
  b.putInt("n",n);
  b.putBoolean("t",t);
  b.putInt("sec",sec);
  f.setArguments(b);
  return f;
 }

 public static QuizFragment newRanked(String s, String c, int n){
  QuizFragment f=new QuizFragment();
  Bundle b=new Bundle();
  b.putBoolean("rank",true);
  b.putString("s",s);
  b.putString("c",c);
  b.putInt("n",n);
  f.setArguments(b);
  return f;
 }

 public static QuizFragment newRanked(){
  return newRanked("All Subjects", "All Categories", 30);
 }

 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  Bundle a=getArguments();
  ranked=a!=null&&a.getBoolean("rank");
  subject=a!=null?a.getString("s","All Subjects"):"All Subjects";
  category=a!=null?a.getString("c","All Categories"):"All Categories";
  searchQuery=a!=null?a.getString("query",""):"";
  int defaultCount = ranked ? 30 : 10;
  count = a != null ? a.getInt("n", defaultCount) : defaultCount;
  timed=ranked||(a!=null&&a.getBoolean("t",false));
  secondsPer=ranked?app().secondsForRank():(a!=null?a.getInt("sec",30):30);
  List<Question> rawPool=app().quiz.pool(subject,category,searchQuery);
  List<Question> pool=new ArrayList<>();
  for(Question q:rawPool){
   if(ranked && q.isPrivate()) continue;
   pool.add(q);
  }
  Collections.shuffle(pool);
  if(pool.isEmpty()) throw new IllegalStateException("No questions match the selected quiz.");
  count=Math.min(count,pool.size());
  session=new ArrayList<>(pool.subList(0,count));
  for(Question q:session) Collections.shuffle(q.answers);
  userAnswers.clear();
  for(int i=0;i<session.size();i++) userAnswers.add(-1);
  started=System.currentTimeMillis();

  requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
   @Override public void handleOnBackPressed() {
    Toast.makeText(requireContext(), "You cannot go back while taking a quiz.", Toast.LENGTH_SHORT).show();
   }
  });
 }

 @Nullable public View onCreateView(LayoutInflater i,ViewGroup c,Bundle b){
  return render();
 }

 private View render(){
  Context x=requireContext();
  LinearLayout p=Ui.page(x);
  LinearLayout top=new LinearLayout(x);
  
  progView=Ui.text(x,(index+1)+" / "+session.size(),16,true);
  top.addView(progView,new LinearLayout.LayoutParams(0,Ui.dp(x,38),1));
  
  timerView=Ui.muted(x,timed?"⏱ "+secondsPer+"s":"Score "+score,13);
  top.addView(timerView);
  p.addView(top);
  
  progressBar=new ProgressBar(x,null,android.R.attr.progressBarStyleHorizontal);
  progressBar.setMax(session.size());
  progressBar.setProgress(index);
  Ui.add(p,progressBar,Ui.dp(x,7));
  
  LinearLayout card=Ui.card(x);
  card.addView(Ui.label(x,subject+"  •  "+category+(searchQuery!=null&&!searchQuery.isEmpty()?"  •  Search: \""+searchQuery+"\"":"")));
  questionTextView=Ui.text(x,"",20,true);
  ScrollView qScroll=new ScrollView(x);
  qScroll.addView(questionTextView);
  card.addView(qScroll,new LinearLayout.LayoutParams(-1,-1));
  Ui.add(p,card,Ui.dp(x,210));
  
  answers=new RadioGroup(x);
  answers.setOrientation(RadioGroup.VERTICAL);
  
  ScrollView sv=new ScrollView(x);
  sv.addView(answers);
  Ui.addWeight(p,sv);
  
  nextButton=Ui.button(x,index==session.size()-1?"Finish":"Continue",true);
  nextButton.setOnClickListener(v->{
   int id=answers.getCheckedRadioButtonId();
   View checkedView=id!=-1?answers.findViewById(id):null;
   int chosen=checkedView!=null?answers.indexOfChild(checkedView):-1;
   if(chosen<0){
    Toast.makeText(requireContext(),"Choose an answer first.",Toast.LENGTH_SHORT).show();
    return;
   }
   if(submitting) return;
   submitting=true;
   nextButton.setEnabled(false);
   submit();
  });
  Ui.add(p,nextButton,Ui.dp(x,56));
  
  bindQuestion();
  submitting=false;
  if(timed && !timerStarted) startTimer();
  return p;
 }

 private void bindQuestion(){
  if(index>=session.size()) return;
  Question q=session.get(index);
  if(progView!=null) progView.setText((index+1)+" / "+session.size());
  if(progressBar!=null) progressBar.setProgress(index);
  if(questionTextView!=null) questionTextView.setText(Ui.fromHtml(q.html));
  if(answers!=null){
   answers.clearCheck();
   answers.removeAllViews();
   for(Answer an:q.answers){
    RadioButton r=new RadioButton(requireContext());
    r.setText(Ui.fromHtml(an.html));
    r.setTextSize(16);
    r.setPadding(Ui.dp(requireContext(),8),Ui.dp(requireContext(),8),Ui.dp(requireContext(),8),Ui.dp(requireContext(),8));
    answers.addView(r,new RadioGroup.LayoutParams(-1,Ui.dp(requireContext(),55)));
   }
  }
  if(nextButton!=null){
   nextButton.setText(index==session.size()-1?"Finish":"Continue");
   nextButton.setEnabled(true);
  }
  submitting=false;
 }

 private void startTimer(){
  timerStarted=true;
  if(timer!=null) timer.cancel();
  long total=(long)secondsPer*session.size()*1000L;
  timer=new CountDownTimer(total,1000){
   public void onTick(long m){
    if(timerView!=null) timerView.setText("⏱ "+((m+999)/1000)+"s");
   }
   public void onFinish(){
    finish();
   }
  }.start();
 }

 private static String normalizeSubject(String s) {
  if (s == null || s.trim().isEmpty()) return "General";
  if (s.equalsIgnoreCase("Algebra") || s.equalsIgnoreCase("Math")) return "Math";
  if (s.equalsIgnoreCase("Machine Design")) return "Machine Design";
  if (s.equalsIgnoreCase("Powerplant") || s.equalsIgnoreCase("Power Plant")) return "Powerplant";
  return s.trim();
 }

 private void submit(){
  int id=answers.getCheckedRadioButtonId();
  View checkedView=id!=-1?answers.findViewById(id):null;
  int chosen=checkedView!=null?answers.indexOfChild(checkedView):-1;
  if(chosen<0 || chosen>=session.get(index).answers.size()){
   submitting=false;
   if(nextButton!=null) nextButton.setEnabled(true);
   Toast.makeText(requireContext(),"Choose an answer first.",Toast.LENGTH_SHORT).show();
   return;
  }
  
  userAnswers.set(index, chosen);
  Question q = session.get(index);
  boolean correct = q.answers.get(chosen).correct;

  for(int i=0; i<answers.getChildCount(); i++){
   RadioButton rb = (RadioButton) answers.getChildAt(i);
   Answer an = q.answers.get(i);
   GradientDrawable g = new GradientDrawable();
   g.setCornerRadius(Ui.dp(requireContext(), 10));
   if(i == chosen){
    if(correct){
     g.setColor(Color.rgb(235, 247, 240));
     g.setStroke(Ui.dp(requireContext(), 2), Ui.GREEN);
     rb.setTextColor(Ui.GREEN);
    } else {
     g.setColor(Color.rgb(253, 240, 240));
     g.setStroke(Ui.dp(requireContext(), 2), Ui.RED);
     rb.setTextColor(Ui.RED);
    }
   } else if(an.correct){
    g.setColor(Color.rgb(235, 247, 240));
    g.setStroke(Ui.dp(requireContext(), 2), Ui.GREEN);
    rb.setTextColor(Ui.GREEN);
   } else {
    g.setColor(Ui.CARD);
    g.setStroke(Ui.dp(requireContext(), 1), Ui.BORDER);
    rb.setTextColor(Ui.TEXT);
   }
   rb.setBackground(g);
   rb.setEnabled(false);
  }

  String sub = normalizeSubject(q.subject);
  int[] counts = subjectCorrectTotal.get(sub);
  if(counts == null){
   counts = new int[2];
   subjectCorrectTotal.put(sub, counts);
  }
  counts[1]++;
  if(correct){
   score++;
   counts[0]++;
  }

  if(!timed && timerView!=null){
   timerView.setText("Score "+score);
  }

  new Handler(Looper.getMainLooper()).postDelayed(() -> {
   if(index<session.size()-1){
    index++;
    bindQuestion();
   }else{
    finish();
   }
  }, 750);
 }

 private void finish(){
  if(timer!=null){
   timer.cancel();
   timer=null;
  }
  app().lastSession = session;
  app().lastUserAnswers = userAnswers;
  int sec=(int)((System.currentTimeMillis()-started)/1000);
  QuizResult r=new QuizResult(ranked?"Ranked • "+app().rank+" • "+subject+" • "+category+" • "+count+" Questions":subject+" • "+category+" • "+count+" Questions"+(timed?" • Timed • "+secondsPer+" sec/question":" • Untimed"),score,session.size(),sec,System.currentTimeMillis(),subject,category);

  boolean isPrivateSession = false;
  for(Question q : session){
   if(q.isPrivate()){
    isPrivateSession = true;
    break;
   }
  }
  if(r.isPrivateResult()) isPrivateSession = true;
  r.isPrivate = isPrivateSession;

  if(!isPrivateSession){
   app().local.saveResult(r);

   if(ranked){
    int delta=score*10-(r.total-score)*4;
    app().rating=Math.max(0,app().rating+delta);
    app().rank=app().rankForRating(app().rating);
    app().local.prefs().edit().putInt("ranked_rating",app().rating).apply();
   }
   if(app().firebase.online) app().firebase.saveResult(app().user.uid,app().user.username,r,ranked,app().rating,app().rank);

   for(Map.Entry<String, int[]> entry : subjectCorrectTotal.entrySet()){
    String subName = entry.getKey();
    int subCorrect = entry.getValue()[0];
    int subTotal = entry.getValue()[1];
    if(subTotal > 0){
     QuizResult subRes = new QuizResult("Subject Practice", subCorrect, subTotal, r.seconds, r.time, subName, r.category);
     subRes.isPrivate = false;
     app().local.saveResult(subRes);
     if(app().firebase.online){
      app().firebase.saveResult(app().user.uid, app().user.username, subRes, false, app().rating, app().rank);
     }
    }
   }
  } else {
   r.mode = "Private Practice • " + subject;
   app().local.saveResult(r);
  }

  ArrayList<String> subKeys = new ArrayList<>();
  ArrayList<Integer> subCorrects = new ArrayList<>();
  ArrayList<Integer> subTotals = new ArrayList<>();
  for(Map.Entry<String, int[]> entry : subjectCorrectTotal.entrySet()){
   subKeys.add(entry.getKey());
   subCorrects.add(entry.getValue()[0]);
   subTotals.add(entry.getValue()[1]);
  }

  app().navigate(ResultFragment.newResult(r, ranked, subKeys, subCorrects, subTotals), false);
 }

 @Override public void onDestroy(){
  if(timer!=null) timer.cancel();
  super.onDestroy();
 }

 @Override public void onResume(){
  super.onResume();
  if(getView()==null) return;
 }
}
