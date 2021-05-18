package com.wurmonline.server.behaviours;

import com.wurmonline.server.banks.Bank;
import com.wurmonline.server.banks.Banks;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.npcs.banker.BankerMod;
import mod.wurmunlimited.npcs.banker.BankerTemplate;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class BankerActions implements ModAction, ActionPerformer, BehaviourProvider {
    private static final Logger logger = Logger.getLogger(BankerActions.class.getName());
    static final List<ActionEntry> actions = new ArrayList<>();
    static ActionEntry changeFaceAction = null;
    private final short actionId;
    private final ActionEntry openAccount;

    public BankerActions() {
        actionId = (short)ModActions.getNextActionId();
        openAccount = new ActionEntryBuilder(actionId, "Open account", "opening account").build();
        ModActions.registerAction(openAccount);
    }

    static boolean isOwner(Creature maybeOwner, Item maybeWrit, Creature maybeBanker) {
        return BankerMod.isWrit(maybeWrit) &&
                maybeOwner.getInventory().getItems().contains(maybeWrit) &&
                maybeWrit.getData() == maybeBanker.getWurmId();
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
        return getBehavioursFor(performer, target, isOwner(performer, subject, target));
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Creature target) {
        return getBehavioursFor(performer, target, false);
    }

    private List<ActionEntry> getBehavioursFor(Creature performer, Creature target, boolean isOwner) {
        if (performer.isPlayer() && BankerTemplate.is(target)) {
            List<ActionEntry> entries = new ArrayList<>();
            int count = 1;
            Bank bank = Banks.getBank(performer.getWurmId());
            boolean addChangeFace = false;

            if (performer.getPower() >= 2 || isOwner) {
                ++count;
                addChangeFace = true;
            }

            if (bank == null) {
                entries.add(new ActionEntryBuilder((short)-count, "Bank", "banking").build());
                entries.add(openAccount);
            } else {
                entries.add(new ActionEntryBuilder((short)-(count + actions.size() - 1), "Bank", "banking").build());
                entries.addAll(actions);
            }

            if (addChangeFace && changeFaceAction != null)
                entries.add(changeFaceAction);

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
}
