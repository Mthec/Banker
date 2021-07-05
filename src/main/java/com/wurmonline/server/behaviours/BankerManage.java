package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.BankerManageQuestion;
import mod.wurmunlimited.npcs.banker.BankerMod;
import mod.wurmunlimited.npcs.banker.BankerTemplate;
import org.gotti.wurmunlimited.modsupport.actions.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static mod.wurmunlimited.npcs.banker.BankerMod.gmManagePowerRequired;

public class BankerManage implements ModAction, ActionPerformer, BehaviourProvider {
    private final short actionId;
    private final List<ActionEntry> entry = new ArrayList<>();
    private final List<ActionEntry> empty = Collections.emptyList();

    public BankerManage() {
        actionId = (short)ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Manage", "managing").build();
        ModActions.registerAction(actionEntry);
        entry.add(actionEntry);
    }

    private static boolean writValid(Creature performer, Creature banker, @Nullable Item writ) {
        return writ != null && writ.getTemplateId() == BankerMod.getContractTemplateId() &&
               performer.getInventory().getItems().contains(writ) && writ.getData() == banker.getWurmId();
    }

    static boolean canManage(Creature performer, Creature banker, @Nullable Item writ) {
        return performer.isPlayer() && BankerTemplate.is(banker) &&
              (performer.getPower() >= gmManagePowerRequired || writValid(performer, banker, writ));
    }

    private List<ActionEntry> getBehaviours(Creature performer, Creature banker, @Nullable Item writ) {
        if (canManage(performer, banker, writ)) {
            return entry;
        }

        return empty;
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
        return getBehaviours(performer, target, subject);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Creature target) {
        return getBehaviours(performer, target, null);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target) {
        return getBehavioursFor(performer, target);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item target) {
        try {
            Creature banker = Creatures.getInstance().getCreature(target.getData());
            return getBehaviours(performer, banker, target);
        } catch (NoSuchCreatureException ignored) {
            return empty;
        }
    }

    private boolean action(Creature performer, Creature banker, @Nullable Item writ) {
        if (canManage(performer, banker, writ)) {
            new BankerManageQuestion((Player)performer, banker).sendQuestion();
            return true;
        }

        return true;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
        if (num == actionId) {
            return action(performer, target, source);
        }

        return true;
    }

    @Override
    public boolean action(Action action, Creature performer, Creature target, short num, float counter) {
        if (num == actionId) {
            return action(performer, target, null);
        }

        return true;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
        return action(action, performer, target, num, counter);
    }

    @Override
    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        if (num == actionId) {
            try {
                Creature banker = Creatures.getInstance().getCreature(target.getData());
                return action(performer, banker, target);
            } catch (NoSuchCreatureException ignored) {}
        }

        return true;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
