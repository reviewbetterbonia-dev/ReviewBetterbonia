package com.reviewbetterbonia.app.ui.quiz;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.*;
import com.reviewbetterbonia.app.model.*;
import com.reviewbetterbonia.app.ui.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class RankedSetupFragment extends BaseFragment {
    @Nullable
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b) {
        Context x = requireContext();
        Ui.PageContainer page = new Ui.PageContainer(x);
        LinearLayout p = page.body;
        LinearLayout bottomBar = page.bottomBar;

        Ui.add(p, Ui.heading(x, "Rated Quiz"), Ui.dp(x, 42));

        LinearLayout card = Ui.card(x);

        RecyclerView rankCarousel = new RecyclerView(x);
        LinearLayoutManager layoutManager = new LinearLayoutManager(x, LinearLayoutManager.HORIZONTAL, false);
        rankCarousel.setLayoutManager(layoutManager);
        rankCarousel.setClipToPadding(false);

        int screenWidth = x.getResources().getDisplayMetrics().widthPixels;
        int cardWidth = Ui.dp(x, 160);
        int horizontalPadding = Math.max(0, (screenWidth - cardWidth) / 2 - Ui.dp(x, 36));
        rankCarousel.setPadding(horizontalPadding, 0, horizontalPadding, 0);

        List<RankTierItem> tiers = Arrays.asList(
            new RankTierItem("Freshman", "0 - 599 RP", "🌱"),
            new RankTierItem("Sophomore", "600 - 899 RP", "⭐"),
            new RankTierItem("Junior", "900 - 1199 RP", "🥉"),
            new RankTierItem("Senior", "1200 - 1499 RP", "🥈"),
            new RankTierItem("Graduate", "1500 - 1799 RP", "🥇"),
            new RankTierItem("Engineer", "1800+ RP", "👑")
        );

        rankCarousel.setAdapter(new RankCarouselAdapter(tiers));
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rankCarousel);

        rankCarousel.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                updateItemAlphas(rv);
            }
        });

        int initialIndex = 0;
        for (int idx = 0; idx < tiers.size(); idx++) {
            if (tiers.get(idx).name.equalsIgnoreCase(app().rank)) {
                initialIndex = idx;
                break;
            }
        }
        rankCarousel.scrollToPosition(initialIndex);

        card.addView(rankCarousel, new LinearLayout.LayoutParams(-1, Ui.dp(x, 130)));
        card.addView(Ui.gap(x, 12));

        TextView ratingView = Ui.text(x, app().rating + " RP", 22, true);
        ratingView.setGravity(Gravity.CENTER);
        card.addView(ratingView);
        card.addView(Ui.gap(x, 4));

        TextView infoView = Ui.muted(x, app().secondsForRank() + " seconds per question  •  Correct: +10 RP", 13);
        infoView.setGravity(Gravity.CENTER);
        card.addView(infoView);
        p.addView(card);
        Ui.add(p, Ui.gap(x, 12), Ui.dp(x, 12));

        rankCarousel.post(() -> updateItemAlphas(rankCarousel));

        LinearLayout setupCard = Ui.card(x);
        setupCard.addView(Ui.text(x, "Rated Quiz Setup", 18, true));
        setupCard.addView(Ui.muted(x, "Customize your subject, category, and question count.", 13));
        setupCard.addView(Ui.gap(x, 8));

        List<String> ratedSubjects = app().quiz.publicSubjects();
        TextView subjectLabel = Ui.label(x, "Subject");
        setupCard.addView(subjectLabel);
        Spinner subjectSpinner = new Spinner(x);
        ArrayAdapter<String> sa = new ArrayAdapter<>(x, android.R.layout.simple_spinner_dropdown_item, ratedSubjects);
        subjectSpinner.setAdapter(sa);
        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 50));
        spinLp.setMargins(0, Ui.dp(x, 4), 0, Ui.dp(x, 12));
        setupCard.addView(subjectSpinner, spinLp);

        TextView categoryLabel = Ui.label(x, "Category");
        setupCard.addView(categoryLabel);
        Spinner categorySpinner = new Spinner(x);
        LinearLayout.LayoutParams catLp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 50));
        catLp.setMargins(0, Ui.dp(x, 4), 0, Ui.dp(x, 12));
        setupCard.addView(categorySpinner, catLp);

        subjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id){
                if (pos < 0 || pos >= ratedSubjects.size()) return;
                String s = ratedSubjects.get(pos);
                List<String> cats = new ArrayList<>();
                cats.add("All Categories");
                cats.addAll("All Subjects".equals(s) ? app().quiz.publicCategories() : app().quiz.publicCategoriesFor(s));
                categorySpinner.setAdapter(new ArrayAdapter<>(x, android.R.layout.simple_spinner_dropdown_item, cats));
            }
        });

        TextView countLabel = Ui.label(x, "Number of Questions");
        setupCard.addView(countLabel);
        Spinner countSpinner = new Spinner(x);
        String[] counts = {"10", "20", "30", "40", "50"};
        countSpinner.setAdapter(new ArrayAdapter<>(x, android.R.layout.simple_spinner_dropdown_item, counts));
        countSpinner.setSelection(2);
        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(-1, Ui.dp(x, 50));
        countLp.setMargins(0, Ui.dp(x, 4), 0, Ui.dp(x, 12));
        setupCard.addView(countSpinner, countLp);

        p.addView(setupCard);
        Ui.add(p, Ui.gap(x, 12), Ui.dp(x, 12));

        LinearLayout graphCard = Ui.card(x);
        graphCard.addView(Ui.text(x, "Rating Progression", 18, true));
        graphCard.addView(Ui.muted(x, "Your rating points history over time.", 13));
        graphCard.addView(Ui.gap(x, 8));

        List<QuizResult> rankedResults = loadRankedHistory();
        HorizontalScrollView hScroll = new HorizontalScrollView(x);
        RatingGraphView graphView = new RatingGraphView(x, rankedResults, app().rating);
        hScroll.addView(graphView);
        graphCard.addView(hScroll, new LinearLayout.LayoutParams(-1, Ui.dp(x, 220)));
        p.addView(graphCard);

        Button start = Ui.button(x, "Start a rated quiz", true);
        start.setOnClickListener(v -> {
            String selectedSubject = subjectSpinner.getSelectedItem() != null ? subjectSpinner.getSelectedItem().toString() : "All Subjects";
            String selectedCategory = categorySpinner.getSelectedItem() != null ? categorySpinner.getSelectedItem().toString() : "All Categories";
            int selectedCount = 30;
            try {
                selectedCount = Integer.parseInt(countSpinner.getSelectedItem().toString());
            } catch (Exception ignored) {}

            List<Question> pool = app().quiz.pool(selectedSubject, selectedCategory);
            if (pool.isEmpty()) {
                Toast.makeText(x, "No questions match this selection.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pool.size() < selectedCount) {
                Toast.makeText(x, "Only " + pool.size() + " questions match this selection.", Toast.LENGTH_SHORT).show();
                return;
            }
            app().navigate(QuizFragment.newRanked(selectedSubject, selectedCategory, selectedCount), true);
        });
        bottomBar.addView(start, new LinearLayout.LayoutParams(-1, Ui.dp(x, 52)));
        bottomBar.addView(Ui.gap(x, 8));

        Button back = Ui.button(x, "Back", false);
        back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        bottomBar.addView(back, new LinearLayout.LayoutParams(-1, Ui.dp(x, 52)));

        return page.root;
    }

    private void updateItemAlphas(RecyclerView rv) {
        int centerX = rv.getWidth() / 2;
        for (int i = 0; i < rv.getChildCount(); i++) {
            View child = rv.getChildAt(i);
            int childCenter = (child.getLeft() + child.getRight()) / 2;
            float distance = Math.abs(centerX - childCenter);
            float ratio = Math.min(1.0f, distance / (rv.getWidth() / 2.0f));
            float alpha = 1.0f - (ratio * 0.55f);
            float scale = 1.0f - (ratio * 0.12f);
            child.setAlpha(alpha);
            child.setScaleX(scale);
            child.setScaleY(scale);
        }
    }

    class RankCarouselAdapter extends RecyclerView.Adapter<RankCarouselAdapter.ViewHolder> {
        private final List<RankTierItem> items;
        RankCarouselAdapter(List<RankTierItem> items) { this.items = items; }

        class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout container;
            TextView iconView, nameView, rangeView;
            ViewHolder(View v) {
                super(v);
                container = (LinearLayout) v;
                iconView = (TextView) container.getChildAt(0);
                nameView = (TextView) container.getChildAt(2);
                rangeView = (TextView) container.getChildAt(3);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context ctx = parent.getContext();
            LinearLayout itemCard = new LinearLayout(ctx);
            itemCard.setOrientation(LinearLayout.VERTICAL);
            itemCard.setGravity(Gravity.CENTER);
            int pad = Ui.dp(ctx, 12);
            itemCard.setPadding(pad, pad, pad, pad);

            GradientDrawable tg = new GradientDrawable();
            tg.setColor(Color.WHITE);
            tg.setCornerRadius(Ui.dp(ctx, 16));
            itemCard.setBackground(tg);

            TextView icon = Ui.text(ctx, "", 26, true);
            icon.setGravity(Gravity.CENTER);
            itemCard.addView(icon);
            itemCard.addView(Ui.gap(ctx, 4));

            TextView name = Ui.text(ctx, "", 15, true);
            name.setGravity(Gravity.CENTER);
            itemCard.addView(name);

            TextView range = Ui.muted(ctx, "", 12);
            range.setGravity(Gravity.CENTER);
            itemCard.addView(range);

            int cardWidth = Ui.dp(ctx, 160);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(cardWidth, -2);
            lp.setMargins(Ui.dp(ctx, 6), Ui.dp(ctx, 4), Ui.dp(ctx, 6), Ui.dp(ctx, 4));
            itemCard.setLayoutParams(lp);

            return new ViewHolder(itemCard);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            RankTierItem tier = items.get(position);
            holder.iconView.setText(tier.iconEmoji);
            holder.nameView.setText(tier.name);
            holder.rangeView.setText(tier.range);

            boolean isCurrent = app().rank.equalsIgnoreCase(tier.name);
            GradientDrawable tg = (GradientDrawable) holder.container.getBackground();
            if (isCurrent) {
                tg.setColor(Color.parseColor("#F3F2FF"));
            } else {
                tg.setColor(Color.WHITE);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }
    }

    private static class RankTierItem {
        final String name, range, iconEmoji;
        RankTierItem(String n, String r, String e) { name = n; range = r; iconEmoji = e; }
    }

    private List<QuizResult> loadRankedHistory() {
        List<QuizResult> all = app().local.results();
        long rankedClearedAt = app().local.rankedClearedTime();
        List<QuizResult> ranked = new ArrayList<>();
        for (QuizResult r : all) {
            if (r.isPrivateResult()) continue;
            boolean isRanked = r.mode != null && r.mode.toLowerCase(Locale.US).contains("ranked");
            if (isRanked) {
                if (rankedClearedAt <= 0 || r.time > rankedClearedAt) {
                    ranked.add(r);
                }
            }
        }
        Collections.sort(ranked, (a, b) -> Long.compare(a.time, b.time));
        return ranked;
    }

    private static class PointData {
        long time;
        int rating;
        String dateStr;
        PointData(long time, int rating, String dateStr) {
            this.time = time;
            this.rating = rating;
            this.dateStr = dateStr;
        }
    }

    private static class RatingGraphView extends View {
        private final List<PointData> points = new ArrayList<>();
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rankTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint peakPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint peakOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint peakInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint peakTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private int maxRating = 200;
        private int peakIndex = 0;
        private final int stepYInterval = 50;
        private final int pxPer50Rp;
        private final int pxPerStepX;
        private final int leftMargin;
        private final int bottomMargin;
        private final int topMargin;

        public RatingGraphView(Context c, List<QuizResult> history, int currentRp) {
            super(c);
            pxPer50Rp = Ui.dp(c, 70);
            pxPerStepX = Ui.dp(c, 110);
            leftMargin = Ui.dp(c, 125);
            bottomMargin = Ui.dp(c, 60);
            topMargin = Ui.dp(c, 50);

            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.US);
            int runningRp = 0;
            points.add(new PointData(System.currentTimeMillis() - 86400000L, 0, "Start"));

            int highestFound = 0;
            for (QuizResult r : history) {
                int delta = r.score * 10 - (r.total - r.score) * 4;
                runningRp = Math.max(0, runningRp + delta);
                points.add(new PointData(r.time, runningRp, dateFormat.format(new Date(r.time))));
                if (runningRp > maxRating) maxRating = runningRp;
            }

            if (currentRp > maxRating) maxRating = currentRp;

            for (int i = 0; i < points.size(); i++) {
                if (points.get(i).rating >= highestFound) {
                    highestFound = points.get(i).rating;
                    peakIndex = i;
                }
            }

            maxRating = ((maxRating / 50) + 2) * 50;

            linePaint.setColor(Ui.ACCENT);
            linePaint.setStrokeWidth(Ui.dp(c, 4));
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeCap(Paint.Cap.ROUND);

            gridPaint.setColor(Ui.BORDER);
            gridPaint.setStrokeWidth(Ui.dp(c, 1));
            gridPaint.setPathEffect(new DashPathEffect(new float[]{Ui.dp(c, 6), Ui.dp(c, 6)}, 0));

            textPaint.setColor(Ui.MUTED);
            textPaint.setTextSize(Ui.dp(c, 12));
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);

            rankTextPaint.setColor(Ui.ACCENT_DARK);
            rankTextPaint.setTextSize(Ui.dp(c, 11));
            rankTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

            nodePaint.setColor(Ui.ACCENT);
            nodePaint.setStyle(Paint.Style.FILL);

            fillPaint.setColor(Color.WHITE);
            fillPaint.setStyle(Paint.Style.FILL);

            int amberColor = Color.parseColor("#FF9800");
            peakPaint.setColor(amberColor);
            peakPaint.setStrokeWidth(Ui.dp(c, 3));
            peakPaint.setPathEffect(new DashPathEffect(new float[]{Ui.dp(c, 8), Ui.dp(c, 6)}, 0));

            peakOuterPaint.setColor(Color.parseColor("#FFE082"));
            peakOuterPaint.setStyle(Paint.Style.FILL);

            peakInnerPaint.setColor(amberColor);
            peakInnerPaint.setStyle(Paint.Style.FILL);

            peakTextPaint.setColor(Color.parseColor("#E65100"));
            peakTextPaint.setTextSize(Ui.dp(c, 12));
            peakTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        private String rankForRp(int rp) {
            if (rp >= 1800) return "Engineer";
            if (rp >= 1500) return "Graduate";
            if (rp >= 1200) return "Senior";
            if (rp >= 900) return "Junior";
            if (rp >= 600) return "Sophomore";
            return "Freshman";
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int totalStepsY = maxRating / stepYInterval;
            int graphHeight = topMargin + bottomMargin + totalStepsY * pxPer50Rp;
            int totalPointsX = Math.max(points.size(), 4);
            int graphWidth = leftMargin + totalPointsX * pxPerStepX + Ui.dp(getContext(), 40);
            setMeasuredDimension(graphWidth, graphHeight);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();

            int totalStepsY = maxRating / stepYInterval;

            for (int i = 0; i <= totalStepsY; i++) {
                int rpValue = i * stepYInterval;
                float y = height - bottomMargin - (i * pxPer50Rp);

                canvas.drawLine(leftMargin, y, width, y, gridPaint);

                String rankStr = (rpValue % 300 == 0 || rpValue == 0) ? " (" + rankForRp(rpValue) + ")" : "";
                String label = rpValue + " RP" + rankStr;
                canvas.drawText(label, Ui.dp(getContext(), 8), y + Ui.dp(getContext(), 4),
                        (rpValue % 300 == 0 || rpValue == 0) ? rankTextPaint : textPaint);
            }

            canvas.drawLine(leftMargin, topMargin, leftMargin, height - bottomMargin, gridPaint);

            Path path = new Path();
            float[] px = new float[points.size()];
            float[] py = new float[points.size()];

            for (int i = 0; i < points.size(); i++) {
                PointData p = points.get(i);
                px[i] = leftMargin + i * pxPerStepX + Ui.dp(getContext(), 30);
                px[i] = Math.min(px[i], width - Ui.dp(getContext(), 20));

                float rpRatio = (float) p.rating / stepYInterval;
                py[i] = height - bottomMargin - (rpRatio * pxPer50Rp);

                if (i == 0) path.moveTo(px[i], py[i]);
                else path.lineTo(px[i], py[i]);

                canvas.drawText(p.dateStr, px[i] - Ui.dp(getContext(), 20), height - Ui.dp(getContext(), 20), textPaint);
            }

            if (peakIndex >= 0 && peakIndex < points.size()) {
                float peakY = py[peakIndex];
                canvas.drawLine(leftMargin, peakY, width, peakY, peakPaint);
            }

            canvas.drawPath(path, linePaint);

            float nodeRadius = Ui.dp(getContext(), 6);
            float innerRadius = Ui.dp(getContext(), 3);

            for (int i = 0; i < points.size(); i++) {
                if (i == peakIndex && points.get(i).rating > 0) continue;
                canvas.drawCircle(px[i], py[i], nodeRadius, nodePaint);
                canvas.drawCircle(px[i], py[i], innerRadius, fillPaint);

                String valLabel = points.get(i).rating + " RP";
                canvas.drawText(valLabel, px[i] - Ui.dp(getContext(), 16), py[i] - Ui.dp(getContext(), 10), textPaint);
            }

            if (peakIndex >= 0 && peakIndex < points.size() && points.get(peakIndex).rating > 0) {
                float peakX = px[peakIndex];
                float peakY = py[peakIndex];

                canvas.drawCircle(peakX, peakY, Ui.dp(getContext(), 11), peakOuterPaint);
                canvas.drawCircle(peakX, peakY, Ui.dp(getContext(), 7), peakInnerPaint);
                canvas.drawCircle(peakX, peakY, Ui.dp(getContext(), 4), fillPaint);

                String peakLabel = "🏆 PEAK: " + points.get(peakIndex).rating + " RP";
                canvas.drawText(peakLabel, peakX - Ui.dp(getContext(), 32), peakY - Ui.dp(getContext(), 16), peakTextPaint);
            }
        }
    }
}
