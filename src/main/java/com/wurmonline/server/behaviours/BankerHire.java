package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.BankerHireQuestion;
import mod.wurmunlimited.npcs.banker.BankerMod;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.Collections;
import java.util.List;

public class BankerHire implements ModAction, ActionPerformer, BehaviourProvider {
    private final short actionId;
    private final List<ActionEntry> hireEntry;
    private final List<ActionEntry> empty = Collections.emptyList();

    public BankerHire() {
        actionId = (short)ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Hire banker", "hiring banker").build();
        ModActions.registerAction(actionEntry);
        hireEntry = Collections.singletonList(actionEntry);
    }

    private boolean canManage(Creature performer, Item writ) {
        return performer.isPlayer() && writ.getTemplateId() == BankerMod.getContractTemplateId() &&
                       performer.getInventory().getItems().contains(writ) && writ.getData() == -1;
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target) {
        return getBehavioursFor(performer, target);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item target) {
        if (canManage(performer, target)) {
            return hireEntry;
        }

        return empty;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
        return action(action, performer, target, num, counter);
    }

    @Override
    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        if (num == actionId) {
            if (canManage(performer, target)) {
                new BankerHireQuestion((Player)performer, performer.getCurrentTile(), performer.getFloorLevel(), target, null).sendQuestion();
                return true;
            }
        }

        return false;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
