package com.reviewbetterbonia.app.model;

import java.util.ArrayList;
import java.util.List;

public class Question {
    public String id="", subject="", category="", html="", correctFeedback="", incorrectFeedback="";
    public final List<Answer> answers = new ArrayList<>();
}
