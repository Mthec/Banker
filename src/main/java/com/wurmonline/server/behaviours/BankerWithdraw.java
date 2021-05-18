package com.wurmonline.server.behaviours;

import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.banks.Bank;
import com.wurmonline.server.banks.BankUnavailableException;
import com.wurmonline.server.banks.Banks;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.WithdrawMoneyQuestion;
import com.wurmonline.server.villages.Village;
import mod.wurmunlimited.npcs.banker.BankerMod;
import mod.wurmunlimited.npcs.banker.BankerTemplate;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.logging.Logger;

public class BankerWithdraw implements ModAction, ActionPerformer {
    private static final Logger logger = Logger.getLogger(BankerWithdraw.class.getName());
    private final short actionId;

    public BankerWithdraw() {
        actionId = (short)ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Withdraw money", "withdrawing").build();
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
                if (performer.getKingdomId() == target.getKingdomId()) {
                    try {
                        WithdrawMoneyQuestion question = new WithdrawMoneyQuestion(performer, "Withdraw money", "Withdraw selected amount:", bank.getCurrentVillage().getToken().getWurmId());
                        BankerMod.withdrawals.add(question.getId());
                        question.sendQuestion();
                    } catch (NoSuchItemException e) {
                        logger.warning("Could not find token for village when attempting to withdraw money.");
                        e.printStackTrace();
                    } catch (BankUnavailableException e) {
                        // See Player.openBank() for precedent.
                        performer.getCommunicator().sendNormalServerMessage(e.getMessage());
                    }
                } else {
                    performer.getCommunicator().sendNormalServerMessage("The banker seems uncomfortable even standing near someone from another kingdom.");
                }
            } else {
                performer.getCommunicator().sendNormalServerMessage("You do not have a bank account to withdraw from.");
            }

            return true;
        }

        return false;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
