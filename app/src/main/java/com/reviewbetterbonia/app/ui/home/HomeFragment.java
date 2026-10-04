package com.reviewbetterbonia.app.ui.home;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import com.reviewbetterbonia.app.MainActivity;
import com.reviewbetterbonia.app.ui.*;
import com.reviewbetterbonia.app.ui.admin.AddQuestionFragment;
import com.reviewbetterbonia.app.ui.admin.AdminFragment;
import com.reviewbetterbonia.app.ui.quiz.CasualSetupFragment;
import com.reviewbetterbonia.app.ui.quiz.RankedSetupFragment;
import com.reviewbetterbonia.app.ui.social.LeaderboardFragment;
import com.reviewbetterbonia.app.ui.social.ProfileFragment;
import com.reviewbetterbonia.app.ui.social.SocialFragment;

public class HomeFragment extends BaseFragment{
 @Nullable public View onCreateView(LayoutInflater i,ViewGroup c,Bundle b){
     Context x=requireContext();MainActivity a=app();
     boolean offline = a.isOffline();
     LinearLayout p=Ui.page(x);
     
     LinearLayout hero=Ui.card(x);
     GradientDrawable heroBg = new GradientDrawable();
     heroBg.setColor(Ui.ACCENT);
     heroBg.setCornerRadius(Ui.dp(x, 36));
     hero.setBackground(heroBg);
     hero.setOrientation(LinearLayout.VERTICAL);

     TextView hi=Ui.text(x, a.user.username, 25, true);
     hi.setTextColor(Color.WHITE);
     hero.addView(hi);
     hero.addView(Ui.gap(x, 4));

     LinearLayout titleBadgeContainer = new LinearLayout(x);
     titleBadgeContainer.setOrientation(LinearLayout.HORIZONTAL);
     titleBadgeContainer.setGravity(Gravity.CENTER_VERTICAL);
     hero.addView(titleBadgeContainer);

     if (!offline && a.user.uid != null && !a.user.uid.isEmpty()) {
         a.firebase.checkUserTitles(a.user.uid, (isGoat, isWizard, isMechanic, isAlchemist) -> {
             if (!isAdded() || titleBadgeContainer == null) return;
             titleBadgeContainer.removeAllViews();

             boolean hasAny = false;
             if (isGoat) {
                 titleBadgeContainer.addView(createBadge(x, "GOAT", Color.parseColor("#212121"), true));
                 hasAny = true;
             }
             if (isWizard) {
                 titleBadgeContainer.addView(createBadge(x, "Wizard", Color.parseColor("#E53935"), false));
                 hasAny = true;
             }
             if (isMechanic) {
                 titleBadgeContainer.addView(createBadge(x, "Mechanic", Color.parseColor("#1E88E5"), false));
                 hasAny = true;
             }
             if (isAlchemist) {
                 titleBadgeContainer.addView(createBadge(x, "Alchemist", Color.parseColor("#43A047"), false));
                 hasAny = true;
             }

             if (!hasAny) {
                 TextView sub = Ui.text(x, "Tap to view profile & stats", 14, false);
                 sub.setTextColor(Color.parseColor("#E0E0E0"));
                 titleBadgeContainer.addView(sub);
             }
         });
     } else {
         TextView sub = Ui.text(x, "Tap to view profile & stats", 14, false);
         sub.setTextColor(Color.parseColor("#E0E0E0"));
         titleBadgeContainer.addView(sub);
     }

     hero.setOnClickListener(v -> a.navigate(new ProfileFragment(), true));
     p.addView(hero);
     Ui.add(p,Ui.gap(x,12),Ui.dp(x,12));

     LinearLayout rank=Ui.card(x);
     rank.addView(Ui.text(x,a.rank+"  •  "+a.rating+" RP",20,true));
     rank.addView(Ui.muted(x,"Finish a rated quiz to increase your rating",13));
     rank.addView(Ui.gap(x, 8));

     RankProgressBarView progressBarView = new RankProgressBarView(x, a.rating);
     rank.addView(progressBarView, new LinearLayout.LayoutParams(-1, Ui.dp(x, 54)));
     rank.addView(Ui.gap(x, 8));

     Button rb=Ui.button(x,"Rated Quiz",true);
     rb.setOnClickListener(v->a.navigate(new RankedSetupFragment(),true));
     setButtonState(rb, !offline);
     Ui.add(rank,rb,Ui.dp(x,50));
     p.addView(rank);
     Ui.add(p,Ui.gap(x,10),Ui.dp(x,10));



     LinearLayout grid=new LinearLayout(x);
     grid.setOrientation(LinearLayout.HORIZONTAL);
     Button cb = Ui.button(x, "Casual Quiz", false);
     Button addQBtn = Ui.button(x, "Add Questions", false);

     cb.setOnClickListener(v -> a.navigate(new CasualSetupFragment(), true));
     addQBtn.setOnClickListener(v -> a.navigate(new AddQuestionFragment(), true));

     setButtonState(addQBtn, !offline);

     LinearLayout grid2=new LinearLayout(x);
     grid2.setOrientation(LinearLayout.HORIZONTAL);
     Button lbBtn = Ui.button(x, "Leaderboards", false);
     Button socialBtn = Ui.button(x, "Social", false);

     lbBtn.setOnClickListener(v -> a.navigate(new LeaderboardFragment(), true));
     socialBtn.setOnClickListener(v -> a.navigate(new SocialFragment(), true));

     setButtonState(lbBtn, !offline);
     setButtonState(socialBtn, !offline);

     grid.addView(cb, new LinearLayout.LayoutParams(0, Ui.dp(x, 52), 1));
     grid.addView(new Space(x), new LinearLayout.LayoutParams(Ui.dp(x, 8), 1));
     grid.addView(addQBtn, new LinearLayout.LayoutParams(0, Ui.dp(x, 52), 1));
     p.addView(grid);

     Ui.add(p,Ui.gap(x,10),Ui.dp(x,10));

     grid2.addView(lbBtn, new LinearLayout.LayoutParams(0, Ui.dp(x, 52), 1));
     grid2.addView(new Space(x), new LinearLayout.LayoutParams(Ui.dp(x, 8), 1));
     grid2.addView(socialBtn, new LinearLayout.LayoutParams(0, Ui.dp(x, 52), 1));
     p.addView(grid2);

     if(a.isAdmin()){
      Ui.add(p,Ui.gap(x,10),Ui.dp(x,10));
      Button admin=Ui.button(x,"Admin tools",false);
      admin.setOnClickListener(v->a.navigate(new AdminFragment(),true));
      setButtonState(admin, !offline);
      Ui.add(p,admin,Ui.dp(x,52));
     }

     Ui.addWeight(p,new Space(x));
     TextView online=Ui.muted(x,offline?"📱 Offline Mode (No Internet Connection)":"☁ Account synced with Firebase",12);
     online.setGravity(Gravity.CENTER);
     if(offline){
      online.setTextColor(Color.parseColor("#D32F2F"));
     }
     Ui.add(p,online,Ui.dp(x,30));

     Button out=Ui.button(x,"Log out",false);
     out.setOnClickListener(v->a.signOut());
     Ui.add(p,out,Ui.dp(x,50));
     return p;
 }

 private void setButtonState(Button b, boolean active){
  b.setEnabled(active);
  if(!active){
   b.setAlpha(0.35f);
  }else{
   b.setAlpha(1.0f);
  }
 }

 private static View createBadge(Context x, String text, int bgColor, boolean glow) {
     TextView tv = new TextView(x);
     tv.setText(text);
     tv.setTextSize(11);
     tv.setTypeface(Typeface.DEFAULT_BOLD);
     tv.setTextColor(Color.WHITE);
     tv.setGravity(Gravity.CENTER);
     int padH = Ui.dp(x, 10);
     int padV = Ui.dp(x, 4);
     tv.setPadding(padH, padV, padH, padV);

     GradientDrawable oval = new GradientDrawable();
     oval.setShape(GradientDrawable.RECTANGLE);
     oval.setCornerRadius(Ui.dp(x, 16));
     oval.setColor(bgColor);
     tv.setBackground(oval);

     if (glow) {
         tv.setShadowLayer(14f, 0f, 0f, Color.YELLOW);
     }

     LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
     params.setMargins(0, 0, Ui.dp(x, 8), 0);
     tv.setLayoutParams(params);

     return tv;
 }

 private static class RankProgressBarView extends View {
     private final int minRp, maxRp, currentRp;
     private final String curRank, nextRank;
     private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
     private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
     private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
     private final Paint boldTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
     private final Paint valueTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

     public RankProgressBarView(Context c, int currentRp) {
         super(c);
         this.currentRp = currentRp;

         if (currentRp < 600) { minRp = 0; maxRp = 600; curRank = "Freshman"; nextRank = "Sophomore"; }
         else if (currentRp < 900) { minRp = 600; maxRp = 900; curRank = "Sophomore"; nextRank = "Junior"; }
         else if (currentRp < 1200) { minRp = 900; maxRp = 1200; curRank = "Junior"; nextRank = "Senior"; }
         else if (currentRp < 1500) { minRp = 1200; maxRp = 1500; curRank = "Senior"; nextRank = "Graduate"; }
         else if (currentRp < 1800) { minRp = 1500; maxRp = 1800; curRank = "Graduate"; nextRank = "Engineer"; }
         else { minRp = 1800; maxRp = 1800; curRank = "Engineer"; nextRank = "Max Rank"; }

         trackPaint.setColor(Ui.BORDER);
         trackPaint.setStyle(Paint.Style.FILL);

         fillPaint.setColor(Ui.ACCENT);
         fillPaint.setStyle(Paint.Style.FILL);

         tickPaint.setColor(Color.parseColor("#40FFFFFF"));
         tickPaint.setStrokeWidth(Ui.dp(c, 2));

         boldTextPaint.setColor(Ui.TEXT);
         boldTextPaint.setTextSize(Ui.dp(c, 12));
         boldTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

         valueTextPaint.setColor(Color.WHITE);
         valueTextPaint.setTextSize(Ui.dp(c, 11));
         valueTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
     }

     @Override
     protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
         int width = MeasureSpec.getSize(widthMeasureSpec);
         int height = Ui.dp(getContext(), 54);
         setMeasuredDimension(width, height);
     }

     @Override
     protected void onDraw(Canvas canvas) {
         super.onDraw(canvas);
         int width = getWidth();
         int pad = Ui.dp(getContext(), 4);
         int topTextY = Ui.dp(getContext(), 14);
         int barTop = Ui.dp(getContext(), 22);
         int barBottom = Ui.dp(getContext(), 38);
         int barHeight = barBottom - barTop;
         int cornerRadius = barHeight / 2;

         String leftLabel = curRank + " (" + minRp + " RP)";
         canvas.drawText(leftLabel, pad, topTextY, boldTextPaint);

         String rightLabel = nextRank + " (" + maxRp + " RP)";
         float rightLabelWidth = boldTextPaint.measureText(rightLabel);
         canvas.drawText(rightLabel, width - rightLabelWidth - pad, topTextY, boldTextPaint);

         float barLeft = pad;
         float barRight = width - pad;
         float barWidth = barRight - barLeft;

         RectF trackRect = new RectF(barLeft, barTop, barRight, barBottom);
         canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint);

         int range = maxRp - minRp;
         float ratio = (range <= 0) ? 1.0f : (float) (currentRp - minRp) / range;
         ratio = Math.max(0.0f, Math.min(1.0f, ratio));
         float fillRight = barLeft + barWidth * ratio;

         if (fillRight > barLeft) {
             RectF fillRect = new RectF(barLeft, barTop, fillRight, barBottom);
             canvas.save();
             canvas.clipRect(fillRect);
             canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, fillPaint);

             if (range > 0) {
                 for (int rpVal = minRp + 50; rpVal < maxRp; rpVal += 50) {
                     float tickRatio = (float) (rpVal - minRp) / range;
                     float tickX = barLeft + barWidth * tickRatio;
                     canvas.drawLine(tickX, barTop + 2, tickX, barBottom - 2, tickPaint);
                 }
             }
             canvas.restore();
         }

         Paint unfilledTickPaint = new Paint(tickPaint);
         unfilledTickPaint.setColor(Color.parseColor("#30000000"));
         if (range > 0) {
             for (int rpVal = minRp + 50; rpVal < maxRp; rpVal += 50) {
                 float tickRatio = (float) (rpVal - minRp) / range;
                 float tickX = barLeft + barWidth * tickRatio;
                 if (tickX > fillRight) {
                     canvas.drawLine(tickX, barTop + 2, tickX, barBottom - 2, unfilledTickPaint);
                 }
             }
         }

         String rpText = currentRp + " RP";
         float textWidth = valueTextPaint.measureText(rpText);
         float textX = barLeft + (barWidth - textWidth) / 2.0f;
         float textY = barTop + (barHeight / 2.0f) + Ui.dp(getContext(), 4);
         canvas.drawText(rpText, textX, textY, valueTextPaint);
     }
 }
}
