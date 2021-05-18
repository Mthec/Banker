package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.banker.BankerMod;
import mod.wurmunlimited.npcs.banker.BankerTemplate;
import mod.wurmunlimited.npcs.banker.FaceSetters;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.logging.Logger;

import static com.wurmonline.server.behaviours.BankerActions.isOwner;

public class BankerChangeFace implements ModAction, ActionPerformer {
    private static final Logger logger = Logger.getLogger(BankerChangeFace.class.getName());
    private final short actionId;

    public BankerChangeFace() {
        actionId = (short)ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Change face", "changing a face").build();
        BankerActions.changeFaceAction = actionEntry;
        ModActions.registerAction(actionEntry);
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
        return action(performer, target, num, isOwner(performer, source, target));
    }

    @Override
    public boolean action(Action action, Creature performer, Creature target, short num, float counter) {
        return action(performer, target, num, false);
    }

    private boolean action(Creature performer, Creature target, short num, boolean isOwner) {
        if (num == actionId && performer.isPlayer() && BankerTemplate.is(target)) {
            if (performer.getPower() >= 2 || isOwner) {
                try {
                    performer.getCommunicator().sendCustomizeFace(target.getFace(), BankerMod.faceSetters.createIdFor(target, (Player)performer));
                } catch (FaceSetters.TooManyTransactionsException e) {
                    logger.warning(e.getMessage());
                    performer.getCommunicator().sendAlertServerMessage(e.getMessage());
                }
            } else {
                performer.getCommunicator().sendNormalServerMessage("You try to touch the banker's face but they deftly dodge.");
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
