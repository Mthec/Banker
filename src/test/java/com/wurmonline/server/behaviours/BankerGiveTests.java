package com.wurmonline.server.behaviours;

import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemsPackageFactory;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.banker.BankerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class BankerGiveTests extends BankerTest {
    private static final int actionTime = 70;
    private Item item;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        item = factory.createNewItem(ItemList.plateJacket);
        player.getInventory().insertItem(item);
    }

    // getBehavioursFor

    private boolean isBehaviour(List<ActionEntry> entries) {
        return !entries.isEmpty() && entries.get(0).getActionString().equals("Give");
    }

    private boolean isEmpty(List<ActionEntry> entries) {
        return entries.isEmpty();
    }

    @Test
    public void testGetBehavioursForArmour() {
        Item item = factory.createNewItem(ItemList.plateJacket);
        player.getInventory().insertItem(item);
        assertTrue(isBehaviour(give.getBehavioursFor(player, item, banker)));
    }

    @Test
    public void testGetBehavioursForWeapon() {
        Item item = factory.createNewItem(ItemList.swordLong);
        player.getInventory().insertItem(item);
        assertTrue(isBehaviour(give.getBehavioursFor(player, item, banker)));
    }

    @Test
    public void testGetBehavioursForShield() {
        Item item = factory.createNewItem(ItemList.shieldLargeMetal);
        player.getInventory().insertItem(item);
        assertTrue(isBehaviour(give.getBehavioursFor(player, item, banker)));
    }

    @Test
    public void testGetBehavioursForNotGM() throws IOException {
        Player gm = factory.createNewPlayer();
        gm.setPower((byte)1);
        assertTrue(isEmpty(give.getBehavioursFor(gm, item, banker)));
    }

    @Test
    public void testGetBehavioursForNotBanker() {
        assertTrue(isEmpty(give.getBehavioursFor(player, item, factory.createNewCreature())));
    }

    @Test
    public void testGetBehavioursForNotWearable() {
        Item item = factory.createNewItem(ItemList.carrot);
        assertTrue(isEmpty(give.getBehavioursFor(player, item, banker)));
    }

    @Test
    public void testGetBehavioursForNotOwned() {
        player.dropItem(item);
        assertTrue(isEmpty(give.getBehavioursFor(player, item, banker)));
    }

    // action

    @Test
    public void testAction() {
        Action action = mock(Action.class);
        assertFalse(give.action(action, player, item, banker, give.getActionId(), 1));
        verify(action, times(1)).setTimeLeft(actionTime);
        assertThat(player, receivedMessageContaining("start to give " + item.getNameWithGenus()));
        verify(player.getCurrentTile(), times(1)).sendActionControl(player, "Giving to " + banker.getName(), true, actionTime);
    }

    @Test
    public void testActionTimePasses() {
        Action action = mock(Action.class);
        assertFalse(give.action(action, player, item, banker, give.getActionId(), 2));
        verify(action, never()).setTimeLeft(actionTime);
        assertThat(player, didNotReceiveMessageContaining("start to give " + item.getNameWithGenus()));
        verify(player.getCurrentTile(), never()).sendActionControl(player, "Giving to " + banker.getName(), true, actionTime);
    }

    @Test
    public void testActionTimeElapsed() {
        Action action = mock(Action.class);
        assertTrue(give.action(action, player, item, banker, give.getActionId(), (actionTime +  1) / 10f));
        verify(action, never()).setTimeLeft(actionTime);
        assertThat(player, didNotReceiveMessageContaining("start to give " + item.getNameWithGenus()));
        verify(player.getCurrentTile(), never()).sendActionControl(player, "Giving to " + banker.getName(), true, actionTime);

        assertFalse(player.getInventory().getItems().contains(item));
        assertFalse(banker.getInventory().getItems().contains(item));
        assertThat(banker, isWearing(item));
    }

    @Test
    public void testActionItemWasDeleted() {
        Action action = mock(Action.class);
        item.deleted = true;
        assertFalse(give.action(action, player, item, banker, give.getActionId(), (actionTime + 1) / 10f));
        verify(action, never()).setTimeLeft(actionTime);
        assertThat(player, didNotReceiveMessageContaining("start to give " + item.getNameWithGenus()));
        verify(player.getCurrentTile(), never()).sendActionControl(player, "Giving to " + banker.getName(), true, actionTime);

        assertFalse(banker.getInventory().getItems().contains(item));
        assertThat(banker, isNotWearing(item));
    }

    @Test
    public void testActionItemIsNoDrop() {
        Action action = mock(Action.class);
        item.setIsNoDrop(true);
        assertFalse(give.action(action, player, item, banker, give.getActionId(), (actionTime + 1) / 10f));
        verify(action, never()).setTimeLeft(actionTime);
        assertThat(player, didNotReceiveMessageContaining("start to give " + item.getNameWithGenus()));
        verify(player.getCurrentTile(), never()).sendActionControl(player, "Giving to " + banker.getName(), true, actionTime);

        assertTrue(player.getInventory().getItems().contains(item));
        assertFalse(banker.getInventory().getItems().contains(item));
        assertThat(banker, isNotWearing(item));
    }

    @Test
    public void testActionItemNotOwned() {
        Action action = mock(Action.class);
        player.dropItem(item);
        assertFalse(give.action(action, player, item, banker, give.getActionId(), (actionTime + 1) / 10f));
        verify(action, never()).setTimeLeft(actionTime);
        assertThat(player, didNotReceiveMessageContaining("start to give " + item.getNameWithGenus()));
        verify(player.getCurrentTile(), never()).sendActionControl(player, "Giving to " + banker.getName(), true, actionTime);

        assertFalse(player.getInventory().getItems().contains(item));
        assertFalse(banker.getInventory().getItems().contains(item));
        assertThat(banker, isNotWearing(item));
    }

    @Test
    public void testActionWrongActionId() {
        Action action = mock(Action.class);
        assertFalse(give.action(action, player, item, banker, (short)(give.getActionId() + 1), (actionTime + 1) / 10f));
        verify(action, never()).setTimeLeft(actionTime);
        assertThat(player, didNotReceiveMessageContaining("start to give " + item.getNameWithGenus()));
        verify(player.getCurrentTile(), never()).sendActionControl(player, "Giving to " + banker.getName(), true, actionTime);

        assertTrue(player.getInventory().getItems().contains(item));
        assertFalse(banker.getInventory().getItems().contains(item));
        assertThat(banker, isNotWearing(item));
    }
}
