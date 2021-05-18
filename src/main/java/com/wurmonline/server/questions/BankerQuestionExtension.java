package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;

import java.util.Properties;

public abstract class BankerQuestionExtension extends Question {
    BankerQuestionExtension(Creature aResponder, String aTitle, String aQuestion, int aType, long aTarget) {
        super(aResponder, aTitle, aQuestion, aType, aTarget);
    }

    boolean wasSelected(String id) {
        String val = getAnswer().getProperty(id);
        return val != null && val.equals("true");
    }

    boolean wasAnswered(String id, String desiredValue) {
        Properties answers = getAnswer();
        if (answers == null)
            return false;
        String val = answers.getProperty(id);
        return val != null && val.equals(desiredValue);
    }

    String getStringOrEmpty(String id) {
        Properties answers = getAnswer();
        if (answers == null)
            return "";
        String f = answers.getProperty(id);
        if (f != null) {
            return f;
        }
        return "";
    }
}

