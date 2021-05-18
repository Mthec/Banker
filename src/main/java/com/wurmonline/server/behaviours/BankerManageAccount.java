package com.wurmonline.server.behaviours;

import com.wurmonline.server.banks.Bank;
import com.wurmonline.server.banks.Banks;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.BankerManageAccountQuestion;
import mod.wurmunlimited.npcs.banker.BankerTemplate;
import org.gotti.wurmunlimited.modsupport.actions.*;
import org.jetbrains.annotations.Nullable;

public class BankerManageAccount implements ModAction, ActionPerformer {
    private final short actionId;

    public BankerManageAccount() {
        actionId = (short)ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Manage", "managing").build();
        BankerActions.actions.add(actionEntry);
        ModActions.registerAction(actionEntry);
    }

    static boolean action(Creature performer, Creature target, @Nullable Bank bank) {
        if (performer.isPlayer() && BankerTemplate.is(target)) {
            if (bank != null) {
                if (!bank.open) {
                    ((Player)performer).openBank();
                } else {
                    performer.getCommunicator().sendNormalServerMessage("Your bank account is already open.");
                }
            } else {
                if (performer.getKingdomId() == target.getKingdomId()) {
                    new BankerManageAccountQuestion(performer, target).sendQuestion();
                } else {
                    performer.getCommunicator().sendNormalServerMessage("The banker seems uncomfortable even standing near someone from another kingdom.");
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
        return action(action, performer, target, num, counter);
    }

    @Override
    public boolean action(Action action, Creature performer, Creature target, short num, float counter) {
        if (num == actionId) {
            return action(performer, target, Banks.getBank(performer.getWurmId()));
        }

        return false;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}