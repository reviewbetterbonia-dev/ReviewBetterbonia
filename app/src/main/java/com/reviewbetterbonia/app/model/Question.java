package com.reviewbetterbonia.app.model;

import java.util.ArrayList;
import java.util.List;

public class Question {
    public String id="", subject="", category="", html="", correctFeedback="", incorrectFeedback="";
    public boolean isPrivate = false;
    public final List<Answer> answers = new ArrayList<>();

    public boolean isPrivate() {
        return isPrivate || (id != null && id.startsWith("priv_"));
    }
}
