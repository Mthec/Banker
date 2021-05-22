package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.BankerHireQuestion;
import com.wurmonline.server.zones.VolaTile;

public class PlaceBankerAction implements NpcMenuEntry {
    public PlaceBankerAction() {
        PlaceNpcMenu.addNpcAction(this);
    }

    @Override
    public String getName() {
        return "Banker";
    }

    @Override
    public boolean doAction(Action action, short num, Creature performer, Item source, VolaTile tile, int floorLevel) {
        if (performer.isPlayer())
            new BankerHireQuestion((Player)performer, tile, floorLevel, null, null).sendQuestion();
        return true;
    }
}
