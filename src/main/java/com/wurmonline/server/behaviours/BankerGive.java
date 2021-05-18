package com.wurmonline.server.behaviours;

import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.tutorial.MissionTriggers;
import mod.wurmunlimited.npcs.banker.BankerTemplate;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.Collections;
import java.util.List;

public class BankerGive implements ModAction, ActionPerformer, BehaviourProvider {
    private static final int actionTime = 70;
    private final short actionId;
    private final List<ActionEntry> giveEntry;
    private final List<ActionEntry> empty = Collections.emptyList();

    public BankerGive() {
        actionId = (short)ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Give", "giving").build();
        ModActions.registerAction(actionEntry);
        giveEntry = Collections.singletonList(actionEntry);
    }

    private boolean wearable(Item item) {
        return item.isArmour() || item.isWeapon() || item.isShield();
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
        if (performer.getPower() == 0 && BankerTemplate.is(target) && wearable(subject) && subject.getOwnerId() == performer.getWurmId()) {
            return giveEntry;
        }

        return empty;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
        if (num == actionId && performer.getPower() == 0 && !source.deleted && !source.isNoDrop() && wearable(source) && source.getOwnerId() == performer.getWurmId()) {
            if (counter == 1) {
                action.setTimeLeft(actionTime);
                performer.getCommunicator().sendNormalServerMessage("You start to give " + source.getNameWithGenus() + " to " + target.getName() + ".  You cannot get it back!");
                Server.getInstance().broadCastAction(performer.getNameWithGenus() + " starts to give " + source.getNameWithGenus() + " to " + target.getName() + ".", performer, 5);
                performer.sendActionControl("Giving to " + target.getName(), true, actionTime);
            } else if (counter * 10 > actionTime) {
                MissionTriggers.activateTriggers(performer, source, num, target.getWurmId(), action.currentSecond());
                source.putInVoid();
                target.getInventory().insertItem(source, true);
                if (target.isHuman() || target.getModelName().contains("humanoid")) {
                    target.wearItems();
                }

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
