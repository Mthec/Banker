package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemsPackageFactory;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.BankerManageQuestion;
import mod.wurmunlimited.npcs.banker.BankerMod;
import mod.wurmunlimited.npcs.banker.BankerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mod.wurmunlimited.Assert.bmlEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class BankerManageTests extends BankerTest {
    private Player gm;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        gm = factory.createNewPlayer();
        gm.setPower((byte)2);
    }

    // getBehavioursFor

    private boolean isBehaviour(List<ActionEntry> entries) {
        return entries.size() == 3 &&
                       entries.get(0).getActionString().equals("Manage") &&
                       entries.get(1).getActionString().equals("Manage") &&
                       entries.get(2).getActionString().equals("Change face");
    }

    private boolean isEmpty(List<ActionEntry> entries) {
        return entries.isEmpty();
    }

    @Test
    public void testGetBehavioursFor() {
        assertTrue(isBehaviour(manage.getBehavioursFor(gm, banker)));
        assertTrue(isEmpty(manage.getBehavioursFor(player, banker)));
    }

    @Test
    public void testGetBehavioursForItem() {
        Item writ = factory.createWritFor(factory.createNewPlayer(), banker);
        assertTrue(isBehaviour(manage.getBehavioursFor(gm, writ, banker)));
        assertTrue(isEmpty(manage.getBehavioursFor(player, writ, banker)));
    }

    @Test
    public void testGetBehavioursForWrit() {
        Item writ = factory.createWritFor(player, banker);
        assertTrue(isBehaviour(manage.getBehavioursFor(gm, factory.createWritFor(gm, banker))));
        assertTrue(isEmpty(manage.getBehavioursFor(factory.createNewPlayer(), writ)));
        assertTrue(isBehaviour(manage.getBehavioursFor(player, writ)));
    }

    @Test
    public void testGetBehavioursForWritAndItem() {
        Item item = factory.createWritFor(factory.createNewPlayer(), banker);
        Item writ = factory.createWritFor(player, banker);
        assertTrue(isBehaviour(manage.getBehavioursFor(gm, item, factory.createWritFor(gm, banker))));
        assertTrue(isEmpty(manage.getBehavioursFor(factory.createNewPlayer(), item, writ)));
        assertTrue(isBehaviour(manage.getBehavioursFor(player, item, writ)));
    }

    @Test
    public void testGetBehavioursForIncorrectWrit() {
        Item notWrit = factory.createWritFor(factory.createNewPlayer(), banker);
        assertTrue(isBehaviour(manage.getBehavioursFor(gm, notWrit)));
        assertTrue(isEmpty(manage.getBehavioursFor(factory.createNewPlayer(), notWrit)));
        assertTrue(isEmpty(manage.getBehavioursFor(player, notWrit)));
    }

    @Test
    public void testGetBehavioursForIncorrectWritAndItem() {
        Item item = factory.createWritFor(factory.createNewPlayer(), banker);
        Item notWrit = factory.createWritFor(factory.createNewPlayer(), banker);
        assertTrue(isBehaviour(manage.getBehavioursFor(gm, item, notWrit)));
        assertTrue(isEmpty(manage.getBehavioursFor(factory.createNewPlayer(), item, notWrit)));
        assertTrue(isEmpty(manage.getBehavioursFor(player, item, notWrit)));
    }

    @Test
    public void testGetBehavioursForNotBanker() {
        Item writ = factory.createWritFor(player, banker);
        Creature notBanker = factory.createNewCreature();
        assertTrue(isEmpty(manage.getBehavioursFor(gm, notBanker)));
        assertTrue(isEmpty(manage.getBehavioursFor(player, notBanker)));
        assertTrue(isEmpty(manage.getBehavioursFor(gm, factory.createWritFor(gm, banker), notBanker)));
        assertTrue(isEmpty(manage.getBehavioursFor(player, writ, notBanker)));
    }

    @Test
    public void testGetBehavioursForBankerDoesNotExist() {
        Item writ = factory.createWritFor(player, banker);
        writ.setData(Long.MAX_VALUE);
        Item randomItem = factory.createNewItem();
        assertTrue(isEmpty(manage.getBehavioursFor(gm, writ)));
        assertTrue(isEmpty(manage.getBehavioursFor(player, writ)));
        assertTrue(isEmpty(manage.getBehavioursFor(gm, randomItem, writ)));
        assertTrue(isEmpty(manage.getBehavioursFor(player, randomItem, writ)));
    }

    @Test
    public void testGetBehavioursForPlayerDoesNotHaveWrit() {
        Item writ = factory.createWritFor(player, banker);
        ItemsPackageFactory.removeItem(player, writ);
        assertTrue(isEmpty(manage.getBehavioursFor(player, writ)));
        assertTrue(isEmpty(manage.getBehavioursFor(player, factory.createNewItem(BankerMod.getContractTemplateId()), writ)));
    }

    // action

    private void sendQuestion() {
        new BankerManageQuestion(gm, banker).sendQuestion();
        new BankerManageQuestion(player, banker).sendQuestion();
    }

    @Test
    public void testAction() {
        Action action = mock(Action.class);
        assertTrue(manage.action(action, gm, banker, manage.getActionId(), 0));
        assertFalse(manage.action(action, player, banker, manage.getActionId(), 0));

        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, factory.getCommunicator(player).getBml().length);
        sendQuestion();
        assertThat(gm, bmlEqual());
    }

    @Test
    public void testActionItem() {
        Action action = mock(Action.class);
        Item writ = factory.createWritFor(player, player);
        assertTrue(manage.action(action, gm, factory.createWritFor(gm, player), banker, manage.getActionId(), 0));
        assertFalse(manage.action(action, player, writ, banker, manage.getActionId(), 0));

        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, factory.getCommunicator(player).getBml().length);
        sendQuestion();
        assertThat(gm, bmlEqual());
    }

    @Test
    public void testActionWrit() {
        Action action = mock(Action.class);
        Player notOwner = factory.createNewPlayer();
        Item writ = factory.createWritFor(player, banker);
        assertTrue(manage.action(action, gm, factory.createWritFor(gm, banker), manage.getActionId(), 0));
        assertTrue(manage.action(action, player, writ, manage.getActionId(), 0));
        assertFalse(manage.action(action, notOwner, writ, manage.getActionId(), 0));

        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        assertEquals(1, factory.getCommunicator(player).getBml().length);
        assertEquals(0, factory.getCommunicator(notOwner).getBml().length);
        sendQuestion();
        assertThat(gm, bmlEqual());
        assertThat(player, bmlEqual());
    }

    @Test
    public void testActionWritAndItem() {
        Action action = mock(Action.class);
        Player notOwner = factory.createNewPlayer();
        Item item = factory.createNewItem(BankerMod.getContractTemplateId());
        Item writ = factory.createWritFor(player, banker);
        assertTrue(manage.action(action, gm, item, factory.createWritFor(gm, banker), manage.getActionId(), 0));
        assertTrue(manage.action(action, player, item, writ, manage.getActionId(), 0));
        assertFalse(manage.action(action, notOwner, item, writ, manage.getActionId(), 0));

        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        assertEquals(1, factory.getCommunicator(player).getBml().length);
        assertEquals(0, factory.getCommunicator(notOwner).getBml().length);
        sendQuestion();
        assertThat(gm, bmlEqual());
        assertThat(player, bmlEqual());
    }

    @Test
    public void testActionIncorrectWrit() {
        Action action = mock(Action.class);
        Player notOwner = factory.createNewPlayer();
        Item notWrit = factory.createWritFor(factory.createNewPlayer(), banker);
        assertTrue(manage.action(action, gm, notWrit, manage.getActionId(), 0));
        assertFalse(manage.action(action, player, notWrit, manage.getActionId(), 0));
        assertFalse(manage.action(action, notOwner, notWrit, manage.getActionId(), 0));

        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, factory.getCommunicator(player).getBml().length);
        assertEquals(0, factory.getCommunicator(notOwner).getBml().length);
        sendQuestion();
        assertThat(gm, bmlEqual());
    }

    @Test
    public void testActionIncorrectWritAndItem() {
        Action action = mock(Action.class);
        Player notOwner = factory.createNewPlayer();
        Item item = factory.createNewItem(BankerMod.getContractTemplateId());
        Item notWrit = factory.createWritFor(factory.createNewPlayer(), banker);
        assertTrue(manage.action(action, gm, item, factory.createWritFor(gm, banker), manage.getActionId(), 0));
        assertFalse(manage.action(action, player, item, notWrit, manage.getActionId(), 0));
        assertFalse(manage.action(action, notOwner, item, notWrit, manage.getActionId(), 0));

        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, factory.getCommunicator(player).getBml().length);
        assertEquals(0, factory.getCommunicator(notOwner).getBml().length);
        sendQuestion();
        assertThat(gm, bmlEqual());
    }

    @Test
    public void testActionNotBanker() {
        Action action = mock(Action.class);
        Creature notBanker = factory.createNewCreature();
        Item writ = factory.createWritFor(player, notBanker);
        assertFalse(manage.action(action, gm, notBanker, manage.getActionId(), 0));
        assertFalse(manage.action(action, player, notBanker, manage.getActionId(), 0));
        assertFalse(manage.action(action, gm, writ, notBanker, manage.getActionId(), 0));
        assertFalse(manage.action(action, player, writ, notBanker, manage.getActionId(), 0));

        assertEquals(0, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, factory.getCommunicator(player).getBml().length);
    }

    @Test
    public void testActionBankerDoesNotExist() {
        Action action = mock(Action.class);
        Item writ = factory.createWritFor(player, banker);
        writ.setData(Long.MAX_VALUE);
        Item randomItem = factory.createNewItem();
        assertFalse(manage.action(action, gm, writ, manage.getActionId(), 0));
        assertFalse(manage.action(action, player, writ, manage.getActionId(), 0));
        assertFalse(manage.action(action, gm, randomItem, writ, manage.getActionId(), 0));
        assertFalse(manage.action(action, player, randomItem, writ, manage.getActionId(), 0));

        assertEquals(0, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, factory.getCommunicator(player).getBml().length);
    }

    @Test
    public void testActionPlayerDoesNotHaveWrit() {
        Action action = mock(Action.class);
        Item writ = factory.createWritFor(player, banker);
        ItemsPackageFactory.removeItem(player, writ);
        assertFalse(manage.action(action, player, writ, manage.getActionId(), 0));
        assertFalse(manage.action(action, player, factory.createNewItem(BankerMod.getContractTemplateId()), writ, manage.getActionId(), 0));

        assertEquals(0, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, factory.getCommunicator(player).getBml().length);
    }

    @Test
    public void testActionWrongActionId() {
        Action action = mock(Action.class);
        assertFalse(manage.action(action, gm, banker, (short)(manage.getActionId() + 1), 0));
        assertFalse(manage.action(action, player, factory.createWritFor(player, banker), (short)(manage.getActionId() + 1), 0));

        assertEquals(0, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, factory.getCommunicator(player).getBml().length);
    }
}
