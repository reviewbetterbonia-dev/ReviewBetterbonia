package com.reviewbetterbonia.app.model;

import java.util.Locale;

public class QuizResult {
    public String mode, subject, category; public int score,total,seconds; public long time;
    public boolean isPrivate = false;
    public QuizResult(String mode,int score,int total,int seconds,long time,String subject,String category){this.mode=mode;this.score=score;this.total=total;this.seconds=seconds;this.time=time;this.subject=subject;this.category=category;}
    public int accuracy(){return total==0?0:(int)Math.round(score*100.0/total);}

    public boolean isPrivateResult() {
        if (isPrivate) return true;
        if (mode != null && mode.toLowerCase(Locale.US).contains("private")) return true;
        if (subject != null) {
            String s = subject.trim();
            if (!s.equalsIgnoreCase("Math") &&
                !s.equalsIgnoreCase("Algebra") &&
                !s.equalsIgnoreCase("Machine Design") &&
                !s.equalsIgnoreCase("Powerplant") &&
                !s.equalsIgnoreCase("Power Plant") &&
                !s.equalsIgnoreCase("General") &&
                !s.equalsIgnoreCase("HVAC") &&
                !s.equalsIgnoreCase("All Subjects") &&
                !s.toLowerCase(Locale.US).contains("ranked")) {
                return true;
            }
        }
        return false;
    }
}
