package com.wurmonline.server.behaviours;

import com.wurmonline.server.banks.Bank;
import com.wurmonline.server.banks.Banks;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.BankerManageAccountQuestion;
import mod.wurmunlimited.npcs.banker.BankerTemplate;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

public class BankerMoveAccount implements ModAction, ActionPerformer {
    private final short actionId;

    public BankerMoveAccount() {
        actionId = (short)ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Move account", "moving account").build();
        BankerActions.actions.add(actionEntry);
        ModActions.registerAction(actionEntry);
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
        return action(action, performer, target, num, counter);
    }

    @Override
    public boolean action(Action action, Creature performer, Creature target, short num, float counter) {
        if (num == actionId && performer.isPlayer() && BankerTemplate.is(target)) {
            Bank bank = Banks.getBank(performer.getWurmId());
            if (bank != null) {
                if (BankerActions.canUse(performer, target)) {
                    new BankerManageAccountQuestion(performer, target).sendQuestion();
                }

                return true;
            }
        }

        return true;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
