package com.reviewbetterbonia.app.model;

public class Stat { public int correct,total,attempts; public void add(int c,int t){correct+=c;total+=t;attempts++;} public int pct(){return total==0?0:(int)Math.round(correct*100.0/total);} }
