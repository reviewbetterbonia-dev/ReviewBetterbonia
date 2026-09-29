package com.reviewbetterbonia.app.model;

public class QuizResult {
    public String mode, subject, category; public int score,total,seconds; public long time;
    public QuizResult(String mode,int score,int total,int seconds,long time,String subject,String category){this.mode=mode;this.score=score;this.total=total;this.seconds=seconds;this.time=time;this.subject=subject;this.category=category;}
    public int accuracy(){return total==0?0:(int)Math.round(score*100.0/total);}
}
