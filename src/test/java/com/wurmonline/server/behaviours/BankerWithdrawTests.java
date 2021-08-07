package com.wurmonline.server.behaviours;

import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.banks.Bank;
import com.wurmonline.server.banks.BankUnavailableException;
import com.wurmonline.server.banks.Banks;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.FakeCommunicator;
import mod.wurmunlimited.npcs.banker.BankerTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static mod.wurmunlimited.Assert.bmlEqual;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class BankerWithdrawTests extends BankerTest {
    private void sendWithdrawQuestion(Creature performer) {
        Bank bank = Banks.getBank(performer.getWurmId());
        try {
            Methods.sendWithdrawMoneyQuestion(performer, bank.getCurrentVillage().getToken());
        } catch (NoSuchItemException | BankUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void actionHasBank() {
        factory.createBankFor(player);
        assert player.getKingdomId() == banker.getKingdomId();

        assertTrue(withdraw.action(mock(Action.class), player, banker, withdraw.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(1, comm.getMessages().length);
        assertEquals(1, comm.getBml().length);
        sendWithdrawQuestion(player);
        assertThat(player, bmlEqual());
    }

    @Test
    public void actionHasBankDifferentKingdom() throws IOException {
        factory.createBankFor(player);
        banker.getStatus().kingdom = (byte)1;
        assert player.getKingdomId() != banker.getKingdomId();

        assertTrue(withdraw.action(mock(Action.class), player, banker, withdraw.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(2, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
        assertThat(player, receivedMessageContaining("seems uncomfortable"));
    }

    @Test
    public void actionNoBank() {
        assertTrue(withdraw.action(mock(Action.class), player, banker, withdraw.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(1, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
        assertThat(player, receivedMessageContaining("do not have a bank account"));
    }

    @Test
    public void actionBankMoving() {
        Bank bank = factory.createBankFor(player);
        bank.startMoving(factory.createVillageFor(factory.createNewPlayer()).getId());
        assertTrue(withdraw.action(mock(Action.class), player, banker, withdraw.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(2, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
        assertThat(player, receivedMessageContaining("not located in a village"));
    }

    @Test
    public void actionBankVillageDisbanded() {
        Bank bank = factory.createBankFor(player);
        bank.currentVillage = 12345;
        assertTrue(withdraw.action(mock(Action.class), player, banker, withdraw.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(2, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
        assertThat(player, receivedMessageContaining("not located in a village"));
    }

    @Test
    public void actionItemDoesNotMatter() {
        factory.createBankFor(player);
        assert player.getKingdomId() == banker.getKingdomId();

        assertTrue(withdraw.action(mock(Action.class), player, factory.createNewItem(), banker, withdraw.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(1, comm.getMessages().length);
        assertEquals(1, comm.getBml().length);
        sendWithdrawQuestion(player);
        assertThat(player, bmlEqual());
    }

    @Test
    public void actionCorrectActionIdOnly() {
        factory.createBankFor(player);
        assertTrue(withdraw.action(mock(Action.class), player, banker, (short)(withdraw.getActionId() + 1), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(1, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
    }

    @Test
    public void actionIsPlayerOnly() {
        factory.createBankFor(player);
        Creature creature = factory.createNewCreature();
        assertTrue(withdraw.action(mock(Action.class), creature, banker, withdraw.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(creature);
        assertEquals(0, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
    }

    @Test
    public void actionIsBankerOnly() {
        factory.createBankFor(player);
        Creature creature = factory.createNewCreature();
        assertTrue(withdraw.action(mock(Action.class), player, creature, withdraw.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(1, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
    }
}

