package com.wurmonline.server.behaviours;

import com.wurmonline.server.banks.Bank;
import com.wurmonline.server.banks.Banks;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.npcs.banker.BankerTemplate;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class BankerActions implements ModAction, ActionPerformer, BehaviourProvider {
    private static final Logger logger = Logger.getLogger(BankerActions.class.getName());
    static final List<ActionEntry> actions = new ArrayList<>();
    private final short actionId;
    private final ActionEntry openAccount;

    public BankerActions() {
        actionId = (short)ModActions.getNextActionId();
        openAccount = new ActionEntryBuilder(actionId, "Open account", "opening account").build();
        ModActions.registerAction(openAccount);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
        return getBehavioursFor(performer, target);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Creature target) {
        if (performer.isPlayer() && BankerTemplate.is(target)) {
            List<ActionEntry> entries = new ArrayList<>();
            int count = 1;
            Bank bank = Banks.getBank(performer.getWurmId());

            if (bank == null) {
                entries.add(new ActionEntryBuilder((short)-count, "Bank", "banking").build());
                entries.add(openAccount);
            } else {
                entries.add(new ActionEntryBuilder((short)-(count + actions.size() - 1), "Bank", "banking").build());
                entries.addAll(actions);
            }

            if (entries.isEmpty())
                logger.warning("No Banker Actions found.");

            return entries;
        }

        return Collections.emptyList();
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
        return action(action, performer, target, num, counter);
    }

    @Override
    public boolean action(Action action, Creature performer, Creature target, short num, float counter) {
        if (num == actionId) {
            return BankerManageAccount.action(performer, target, null);
        }

        return false;
    }

    @Override
    public short getActionId() {
        return actionId;
    }

    static boolean canUse(Creature performer, Creature target) {
        if (performer.getPower() < 2 && performer.getKingdomId() != target.getKingdomId()) {
            performer.getCommunicator().sendNormalServerMessage("The banker seems uncomfortable even standing near someone from another kingdom.");
            return false;
        }
        if (performer.getPower() < 2) {
            target.turnTowardsCreature(performer);
            try {
                target.getStatus().savePosition(target.getWurmId(), false, target.getStatus().getZoneId(), true);
            } catch (IOException ignored) {}
        }

        return true;
    }
}
