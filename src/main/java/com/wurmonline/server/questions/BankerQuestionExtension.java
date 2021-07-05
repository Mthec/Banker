package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import mod.wurmunlimited.npcs.banker.BankerMod;

import java.util.Properties;

public abstract class BankerQuestionExtension extends Question {
    protected static final ModelOption[] modelOptions = new ModelOption[] { ModelOption.HUMAN, ModelOption.TRADER, ModelOption.CUSTOM };

    BankerQuestionExtension(Creature aResponder, String aTitle, String aQuestion, int aType, long aTarget) {
        super(aResponder, aTitle, aQuestion, aType, aTarget);
    }

    boolean wasSelected(String id) {
        String val = getAnswer().getProperty(id);
        return val != null && val.equals("true");
    }

    boolean wasAnswered(@SuppressWarnings("SameParameterValue") String id, String desiredValue) {
        Properties answers = getAnswer();
        if (answers == null)
            return false;
        String val = answers.getProperty(id);
        return val != null && val.equals(desiredValue);
    }

    String getPrefix() {
        String prefix = BankerMod.getNamePrefix();
        if (prefix.isEmpty()) {
            return "";
        } else {
            return prefix + "_";
        }
    }

    String getNameWithoutPrefix(String name) {
        String prefix = BankerMod.getNamePrefix();
        if (prefix.isEmpty() || name.length() < prefix.length() + 1) {
            return name;
        } else {
            return name.substring(prefix.length() + 1);
        }
    }
}

