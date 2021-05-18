package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.FakeCommunicator;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.BankerManageAccountQuestion;
import mod.wurmunlimited.npcs.banker.BankerMod;
import mod.wurmunlimited.npcs.banker.BankerTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static mod.wurmunlimited.Assert.bmlEqual;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class BankerActionsTests extends BankerTest {
    // getBehavioursFor

    private String behavioursToString(List<ActionEntry> behaviours) {
        List<String> strings = behaviours.stream().map(ActionEntry::getActionString).collect(Collectors.toList());
        if (!strings.isEmpty())
            return String.join(", ", strings);
        else
            return "EMPTY";
    }

    @Test
    public void getBehavioursFor() {
        List<ActionEntry> behaviours = actions.getBehavioursFor(player, banker);
        assertEquals(2, behaviours.size(), behavioursToString(behaviours));
        assertEquals("Bank", behaviours.get(0).getActionString());
        assertEquals((short)-1, behaviours.get(0).getNumber());
        assertEquals("Open account", behaviours.get(1).getActionString());
    }

    @Test
    public void getBehavioursForNotPlayer() {
        List<ActionEntry> behaviours = actions.getBehavioursFor(factory.createNewCreature(), banker);
        assertEquals(0, behaviours.size(), behavioursToString(behaviours));
    }

    @Test
    public void getBehavioursForNotBanker() {
        List<ActionEntry> behaviours = actions.getBehavioursFor(player, factory.createNewCreature());
        assertEquals(0, behaviours.size(), behavioursToString(behaviours));
    }

    @Test
    public void getBehavioursForGM() throws IOException {
        player.setPower((byte)2);
        List<ActionEntry> behaviours = actions.getBehavioursFor(player, banker);
        assertEquals(3, behaviours.size(), behavioursToString(behaviours));
        assertEquals("Bank", behaviours.get(0).getActionString());
        assertEquals((short)-2, behaviours.get(0).getNumber());
        assertEquals("Open account", behaviours.get(1).getActionString());
        assertEquals("Change face", behaviours.get(2).getActionString());
    }

    @Test
    public void getBehavioursForOwner() {
        Item writ = factory.createWritFor(player, banker);
        List<ActionEntry> behaviours = actions.getBehavioursFor(player, writ, banker);
        assertEquals(3, behaviours.size(), behavioursToString(behaviours));
        assertEquals("Bank", behaviours.get(0).getActionString());
        assertEquals((short)-2, behaviours.get(0).getNumber());
        assertEquals("Open account", behaviours.get(1).getActionString());
        assertEquals("Change face", behaviours.get(2).getActionString());
    }

    @Test
    public void getBehavioursForAnotherWrit() {
        Item writ = factory.createNewItem(BankerMod.getContractTemplateId());
        player.getInventory().insertItem(writ);
        List<ActionEntry> behaviours = actions.getBehavioursFor(player, writ, banker);
        assertEquals(2, behaviours.size(), behavioursToString(behaviours));
        assertEquals("Bank", behaviours.get(0).getActionString());
        assertEquals((short)-1, behaviours.get(0).getNumber());
        assertEquals("Open account", behaviours.get(1).getActionString());
    }

    @Test
    public void getBehavioursForWithBank() {
        factory.createBankFor(player);
        List<ActionEntry> behaviours = actions.getBehavioursFor(player, banker);
        assertEquals(4, behaviours.size(), behavioursToString(behaviours));
        assertEquals("Bank", behaviours.get(0).getActionString());
        assertEquals((short)-3, behaviours.get(0).getNumber());
        assertEquals("Manage", behaviours.get(1).getActionString());
        assertEquals("Withdraw money", behaviours.get(2).getActionString());
        assertEquals("Move account", behaviours.get(3).getActionString());
    }

    @Test
    public void getBehavioursForGMWithBank() throws IOException {
        player.setPower((byte)2);
        factory.createBankFor(player);
        List<ActionEntry> behaviours = actions.getBehavioursFor(player, banker);
        assertEquals(5, behaviours.size(), behavioursToString(behaviours));
        assertEquals("Bank", behaviours.get(0).getActionString());
        assertEquals((short)-4, behaviours.get(0).getNumber());
        assertEquals("Manage", behaviours.get(1).getActionString());
        assertEquals("Withdraw money", behaviours.get(2).getActionString());
        assertEquals("Move account", behaviours.get(3).getActionString());
        assertEquals("Change face", behaviours.get(4).getActionString());
    }

    @Test
    public void getBehavioursForOwnerWithBank() {
        Item writ = factory.createWritFor(player, banker);
        factory.createBankFor(player);
        List<ActionEntry> behaviours = actions.getBehavioursFor(player, writ, banker);
        assertEquals(5, behaviours.size(), behavioursToString(behaviours));
        assertEquals("Bank", behaviours.get(0).getActionString());
        assertEquals((short)-4, behaviours.get(0).getNumber());
        assertEquals("Manage", behaviours.get(1).getActionString());
        assertEquals("Withdraw money", behaviours.get(2).getActionString());
        assertEquals("Move account", behaviours.get(3).getActionString());
        assertEquals("Change face", behaviours.get(4).getActionString());
    }

    @Test
    public void getBehavioursForAnotherWritWithBank() {
        Item writ = factory.createNewItem(BankerMod.getContractTemplateId());
        player.getInventory().insertItem(writ);
        factory.createBankFor(player);
        List<ActionEntry> behaviours = actions.getBehavioursFor(player, writ, banker);
        assertEquals(4, behaviours.size(), behavioursToString(behaviours));
        assertEquals("Bank", behaviours.get(0).getActionString());
        assertEquals((short)-3, behaviours.get(0).getNumber());
        assertEquals("Manage", behaviours.get(1).getActionString());
        assertEquals("Withdraw money", behaviours.get(2).getActionString());
        assertEquals("Move account", behaviours.get(3).getActionString());
    }

    // action

    @Test
    public void actionNoBank() {
        assert player.getKingdomId() == banker.getKingdomId();

        assertTrue(actions.action(mock(Action.class), player, banker, actions.getActionId(), 0));

        assertEquals(0, factory.getCommunicator(player).getMessages().length);
        assertEquals(1, factory.getCommunicator(player).getBml().length);
        new BankerManageAccountQuestion(player, banker).sendQuestion();
        assertThat(player, bmlEqual());
    }

    @Test
    public void actionNoBankItem() {
        assert player.getKingdomId() == banker.getKingdomId();

        assertTrue(actions.action(mock(Action.class), player, factory.createNewItem(), banker, actions.getActionId(), 0));

        assertEquals(0, factory.getCommunicator(player).getMessages().length);
        assertEquals(1, factory.getCommunicator(player).getBml().length);
        new BankerManageAccountQuestion(player, banker).sendQuestion();
        assertThat(player, bmlEqual());
    }

    @Test
    public void actionNoBankDifferentKingdom() {
        assert player.getKingdomId() == 0;
        banker.getStatus().kingdom = (byte)1;

        assertTrue(actions.action(mock(Action.class), player, banker, actions.getActionId(), 0));

        assertEquals(1, factory.getCommunicator(player).getMessages().length);
        assertThat(player, receivedMessageContaining("seems uncomfortable"));
    }

    @Test
    public void actionCorrectActionIdOnly() {
        assertFalse(actions.action(mock(Action.class), player, banker, (short)(actions.getActionId() + 1), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(0, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
    }

    @Test
    public void actionIsPlayerOnly() {
        Creature creature = factory.createNewCreature();
        assertFalse(actions.action(mock(Action.class), creature, banker, actions.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(creature);
        assertEquals(0, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
    }

    @Test
    public void actionIsBankerOnly() {
        Creature creature = factory.createNewCreature();
        assertFalse(actions.action(mock(Action.class), player, creature, actions.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(0, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
    }
}
