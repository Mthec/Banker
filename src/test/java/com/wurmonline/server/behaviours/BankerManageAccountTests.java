package com.wurmonline.server.behaviours;

import com.wurmonline.server.banks.Bank;
import com.wurmonline.server.banks.BankUnavailableException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.FakeCommunicator;
import com.wurmonline.server.questions.BankerManageAccountQuestion;
import mod.wurmunlimited.npcs.banker.BankerTest;
import org.junit.jupiter.api.Test;

import static mod.wurmunlimited.Assert.bmlEqual;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class BankerManageAccountTests extends BankerTest {
    @Test
    public void actionOpensBank() {
        Bank bank = factory.createBankFor(player);
        assert(!bank.open);

        assertTrue(manageAccount.action(mock(Action.class), player, banker, manageAccount.getActionId(), 0));

        assertTrue(bank.open);
        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(1, comm.getMessages().length);
        assertEquals(bank.id, comm.openedInventoryWindows.get(0));
    }

    @Test
    public void actionItemDoesNotMatter() {
        Bank bank = factory.createBankFor(player);
        assert(!bank.open);

        assertTrue(manageAccount.action(mock(Action.class), player, factory.createNewItem(), banker, manageAccount.getActionId(), 0));

        assertTrue(bank.open);
        assertEquals(1, factory.getCommunicator(player).getMessages().length);
    }

    @Test
    public void actionBankAlreadyOpen() throws BankUnavailableException {
        Bank bank = factory.createBankFor(player);
        bank.open();

        assertTrue(manageAccount.action(mock(Action.class), player, banker, manageAccount.getActionId(), 0));

        assertEquals(2, factory.getCommunicator(player).getMessages().length);
        assertThat(player, receivedMessageContaining("already open"));
        assertTrue(bank.open);
    }

    @Test
    public void actionNoBank() {
        assert player.getKingdomId() == banker.getKingdomId();

        assertTrue(manageAccount.action(mock(Action.class), player, banker, manageAccount.getActionId(), 0));

        assertEquals(0, factory.getCommunicator(player).getMessages().length);
        assertEquals(1, factory.getCommunicator(player).getBml().length);
        new BankerManageAccountQuestion(player, banker).sendQuestion();
        assertThat(player, bmlEqual());
    }

    @Test
    public void actionNoBankDifferentKingdom() {
        banker.getStatus().kingdom = (byte)1;
        assert player.getKingdomId() != banker.getStatus().kingdom;

        assertTrue(manageAccount.action(mock(Action.class), player, banker, manageAccount.getActionId(), 0));

        assertEquals(1, factory.getCommunicator(player).getMessages().length);
        assertThat(player, receivedMessageContaining("seems uncomfortable"));
    }

    @Test
    public void actionCorrectActionIdOnly() {
        assertTrue(manageAccount.action(mock(Action.class), player, banker, (short)(manageAccount.getActionId() + 1), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(0, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
    }

    @Test
    public void actionIsPlayerOnly() {
        Creature creature = factory.createNewCreature();
        assertTrue(manageAccount.action(mock(Action.class), creature, banker, manageAccount.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(creature);
        assertEquals(0, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
    }

    @Test
    public void actionIsBankerOnly() {
        Creature creature = factory.createNewCreature();
        assertTrue(manageAccount.action(mock(Action.class), player, creature, manageAccount.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(0, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
    }
}
