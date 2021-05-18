package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.FakeCommunicator;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemsPackageFactory;
import com.wurmonline.server.questions.BankerHireQuestion;
import mod.wurmunlimited.npcs.banker.BankerMod;
import mod.wurmunlimited.npcs.banker.BankerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static mod.wurmunlimited.Assert.bmlEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class BankerHireTests extends BankerTest {
    private Item contract;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        contract = factory.createNewItem(BankerMod.getContractTemplateId());
        player.getInventory().insertItem(contract);
    }

    // getBehavioursFor

    private boolean isBehaviour(List<ActionEntry> entries) {
        return !entries.isEmpty() && entries.get(0).getActionString().equals("Hire banker");
    }

    private boolean isEmpty(List<ActionEntry> entries) {
        return entries.isEmpty();
    }

    @Test
    public void testGetBehavioursFor() {
        assertTrue(isBehaviour(hire.getBehavioursFor(player, contract)));
    }

    @Test
    public void testGetBehavioursForItem() {
        assertTrue(isBehaviour(hire.getBehavioursFor(player, factory.createNewItem(), contract)));
    }

    @Test
    public void testGetBehavioursForNotPlayer() {
        assertTrue(isEmpty(hire.getBehavioursFor(factory.createNewCreature(), contract)));
    }

    @Test
    public void testGetBehavioursForNotContract() {
        assertTrue(isEmpty(hire.getBehavioursFor(player, factory.createNewItem())));
    }

    @Test
    public void testGetBehavioursForContractNotInPlayerInventory() {
        ItemsPackageFactory.removeItem(player, contract);
        assertTrue(isEmpty(hire.getBehavioursFor(player, contract)));
    }

    @Test
    public void testGetBehavioursForAlreadyHired() {
        contract.setData(banker.getWurmId());
        assertTrue(isEmpty(hire.getBehavioursFor(player, contract)));
    }

    // action

    @Test
    public void testAction() {
        Action action = mock(Action.class);
        assertTrue(hire.action(action, player, contract, hire.getActionId(), 0));
        Properties properties = new Properties();
        FakeCommunicator comm = factory.getCommunicator(player);
        if (comm.lastBmlContent.contains("\"Male\";selected=\"true\""))
            properties.setProperty("gender", "male");
        else
            properties.setProperty("gender", "female");
        new BankerHireQuestion(player, player.getCurrentTile(), 0, contract, properties).sendQuestion();
        assertThat(player, bmlEqual());
    }

    @Test
    public void testActionItem() {
        Action action = mock(Action.class);
        assertTrue(hire.action(action, player, factory.createNewItem(), contract, hire.getActionId(), 0));
        Properties properties = new Properties();
        FakeCommunicator comm = factory.getCommunicator(player);
        if (comm.lastBmlContent.contains("\"Male\";selected=\"true\""))
            properties.setProperty("gender", "male");
        else
            properties.setProperty("gender", "female");
        new BankerHireQuestion(player, player.getCurrentTile(), 0, contract, properties).sendQuestion();
        assertThat(player, bmlEqual());
    }

    @Test
    public void testActionNotPlayer() {
        Action action = mock(Action.class);
        assertFalse(hire.action(action, factory.createNewCreature(), contract, hire.getActionId(), 0));
        assertEquals(0, factory.getCommunicator(player).getBml().length);
    }

    @Test
    public void testActionNotContract() {
        Action action = mock(Action.class);
        assertFalse(hire.action(action, factory.createNewCreature(), factory.createNewItem(), hire.getActionId(), 0));
        assertEquals(0, factory.getCommunicator(player).getBml().length);
    }

    @Test
    public void testActionContractNotInPlayerInventory() {
        Action action = mock(Action.class);
        ItemsPackageFactory.removeItem(player, contract);
        assertFalse(hire.action(action, factory.createNewCreature(), contract, hire.getActionId(), 0));
        assertEquals(0, factory.getCommunicator(player).getBml().length);
    }

    @Test
    public void testActionAlreadyHired() {
        Action action = mock(Action.class);
        contract.setData(banker.getWurmId());
        assertFalse(hire.action(action, factory.createNewCreature(), contract, hire.getActionId(), 0));
        assertEquals(0, factory.getCommunicator(player).getBml().length);
    }

    @Test
    public void testActionWrongActionId() {
        Action action = mock(Action.class);
        assertFalse(hire.action(action, factory.createNewCreature(), contract, (short)(hire.getActionId() + 1), 0));
        assertEquals(0, factory.getCommunicator(player).getBml().length);
    }
}
