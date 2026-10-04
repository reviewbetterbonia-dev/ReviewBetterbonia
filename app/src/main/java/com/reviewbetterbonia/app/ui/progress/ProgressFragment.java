package com.reviewbetterbonia.app.ui.progress;

import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import com.reviewbetterbonia.app.model.*;
import com.reviewbetterbonia.app.ui.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ProgressFragment extends BaseFragment {
    @Nullable
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b) {
        Context x = requireContext();
        LinearLayout p = Ui.page(x);
        Ui.add(p, Ui.heading(x, "Rating Progress"), Ui.dp(x, 42));
        Ui.add(p, Ui.muted(x, "Rating points progression over time with rank thresholds.", 14), Ui.dp(x, 24));

        List<QuizResult> rankedResults = loadRankedHistory();

        ScrollView vScroll = new ScrollView(x);
        HorizontalScrollView hScroll = new HorizontalScrollView(x);

        RatingGraphView graphView = new RatingGraphView(x, rankedResults, app().rating);
        hScroll.addView(graphView);
        vScroll.addView(hScroll);

        Ui.addWeight(p, vScroll);

        Button back = Ui.button(x, "Back", false);
        back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        Ui.add(p, back, Ui.dp(x, 52));

        return p;
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
