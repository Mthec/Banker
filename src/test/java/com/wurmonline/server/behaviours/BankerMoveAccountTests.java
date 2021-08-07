package com.wurmonline.server.behaviours;

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

public class BankerMoveAccountTests extends BankerTest {
    @Test
    public void actionHasBank() {
        factory.createBankFor(player);
        assert player.getKingdomId() == banker.getKingdomId();

        assertTrue(move.action(mock(Action.class), player, banker, move.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(1, comm.getMessages().length);
        assertEquals(1, comm.getBml().length);
        new BankerManageAccountQuestion(player, banker).sendQuestion();
        assertThat(player, bmlEqual());
    }

    @Test
    public void actionHasBankDifferentKingdom() {
        factory.createBankFor(player);
        banker.getStatus().kingdom = (byte)1;
        assert player.getKingdomId() != banker.getStatus().kingdom;

        assertTrue(move.action(mock(Action.class), player, banker, move.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(2, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
        assertThat(player, receivedMessageContaining("seems uncomfortable"));
    }

    @Test
    public void actionNoBank() {
        assertTrue(move.action(mock(Action.class), player, banker, move.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(0, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
    }
    
    @Test
    public void actionItemDoesNotMatter() {
        factory.createBankFor(player);
        assert player.getKingdomId() == banker.getKingdomId();

        assertTrue(move.action(mock(Action.class), player, factory.createNewItem(), banker, move.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(1, comm.getMessages().length);
        assertEquals(1, comm.getBml().length);
        new BankerManageAccountQuestion(player, banker).sendQuestion();
        assertThat(player, bmlEqual());
    }

    @Test
    public void actionCorrectActionIdOnly() {
        factory.createBankFor(player);
        assertTrue(move.action(mock(Action.class), player, banker, (short)(move.getActionId() + 1), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(1, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
    }

    @Test
    public void actionIsPlayerOnly() {
        factory.createBankFor(player);
        Creature creature = factory.createNewCreature();
        assertTrue(move.action(mock(Action.class), creature, banker, move.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(creature);
        assertEquals(0, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
    }

    @Test
    public void actionIsBankerOnly() {
        factory.createBankFor(player);
        Creature creature = factory.createNewCreature();
        assertTrue(move.action(mock(Action.class), player, creature, move.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(1, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
    }
}

