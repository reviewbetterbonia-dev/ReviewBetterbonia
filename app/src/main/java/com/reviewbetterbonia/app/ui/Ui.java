package com.reviewbetterbonia.app.ui;

import android.content.Context;import android.graphics.Color;import android.graphics.Typeface;import android.text.Html;import android.text.Spanned;import android.view.*;import android.widget.*;import android.graphics.drawable.GradientDrawable;

public final class Ui {
    public static final int BG=Color.rgb(248,249,252), CARD=Color.WHITE, TEXT=Color.rgb(28,34,43), MUTED=Color.rgb(104,113,125), ACCENT=Color.rgb(83,76,202), ACCENT_DARK=Color.rgb(62,55,164), BORDER=Color.rgb(226,229,235), GREEN=Color.rgb(30,145,90), RED=Color.rgb(205,62,62);
    private Ui(){}
    public static int dp(Context c,int n){return (int)(n*c.getResources().getDisplayMetrics().density+.5f);}
    public static LinearLayout page(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(c,20),dp(c,50),dp(c,20),dp(c,18));l.setBackgroundColor(BG);return l;}
    public static TextView text(Context c,String s,float size,boolean bold){TextView v=new TextView(c);v.setText(fromHtml(s));v.setTextSize(size);v.setTextColor(TEXT);v.setPadding(0,dp(c,3),0,dp(c,3));if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    public static TextView muted(Context c,String s,float size){TextView v=text(c,s,size,false);v.setTextColor(MUTED);return v;}
    public static TextView heading(Context c,String s){TextView v=text(c,s,28,true);v.setPadding(0,0,0,dp(c,4));return v;}
    public static TextView label(Context c,String s){TextView v=text(c,s,13,true);v.setTextColor(MUTED);v.setAllCaps(true);return v;}
    public static LinearLayout card(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(c,18),dp(c,16),dp(c,18),dp(c,16));GradientDrawable g=new GradientDrawable();g.setColor(CARD);g.setCornerRadius(dp(c,18));g.setStroke(dp(c,1),BORDER);l.setBackground(g);return l;}
    public static Button button(Context c,String s,boolean filled){Button b=new Button(c);b.setText(s);b.setTextSize(15);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(filled?Color.WHITE:ACCENT);GradientDrawable g=new GradientDrawable();g.setColor(filled?ACCENT:Color.WHITE);g.setCornerRadius(dp(c,14));g.setStroke(dp(c,1),filled?ACCENT:BORDER);b.setBackground(g);return b;}
    public static void add(LinearLayout p,View v,int h){p.addView(v,new LinearLayout.LayoutParams(-1,h));}
    public static void addWeight(LinearLayout p,View v){p.addView(v,new LinearLayout.LayoutParams(-1,0,1));}
    public static Spanned fromHtml(String s){return Html.fromHtml(s==null?"":s,Html.FROM_HTML_MODE_LEGACY);}
    public static Space gap(Context c,int h){Space s=new Space(c);s.setLayoutParams(new LinearLayout.LayoutParams(1,dp(c,h)));return s;}

    public static class PageContainer {
        public final LinearLayout root;
        public final LinearLayout body;
        public final LinearLayout bottomBar;
        public PageContainer(Context c) {
            root = new LinearLayout(c);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(BG);
            root.setPadding(dp(c, 20), dp(c, 50), dp(c, 20), dp(c, 18));

            ScrollView sv = new ScrollView(c);
            sv.setFillViewport(true);
            body = new LinearLayout(c);
            body.setOrientation(LinearLayout.VERTICAL);
            sv.addView(body);

            root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

            bottomBar = new LinearLayout(c);
            bottomBar.setOrientation(LinearLayout.VERTICAL);
            bottomBar.setPadding(0, dp(c, 8), 0, 0);
            root.addView(bottomBar, new LinearLayout.LayoutParams(-1, -2));
        }
    }
}
