package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.FakeCommunicator;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.npcs.banker.BankerMod;
import mod.wurmunlimited.npcs.banker.BankerTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;

import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BankerChangeFaceTests extends BankerTest {
    @Test
    public void actionGMSendsCustomizeFace() throws IOException {
        player.setPower((byte)2);
        assertTrue(changeFace.action(mock(Action.class), player, banker, changeFace.getActionId(), 0));

        FakeCommunicator.CustomizeFace customize = factory.getCommunicator(player).sendCustomizeFace;
        assertNotNull(customize);
        assertEquals(banker.getFace(), customize.face);
        assertEquals(banker, Objects.requireNonNull(BankerMod.faceSetters.retrieveBankerOrNull(player, customize.itemId)));
    }

    @Test
    public void actionOwnerSendsCustomizeFace() {
        Item writ = factory.createWritFor(player, banker);
        assertTrue(changeFace.action(mock(Action.class), player, writ, banker, changeFace.getActionId(), 0));

        FakeCommunicator.CustomizeFace customize = factory.getCommunicator(player).sendCustomizeFace;
        assertNotNull(customize);
        assertEquals(banker.getFace(), customize.face);
        assertEquals(banker, Objects.requireNonNull(BankerMod.faceSetters.retrieveBankerOrNull(player, customize.itemId)));
    }

    @Test
    public void actionNotOwnerNotGMDoesNotSendsCustomizeFace() {
        assertTrue(changeFace.action(mock(Action.class), player, banker, changeFace.getActionId(), 0));

        FakeCommunicator.CustomizeFace customize = factory.getCommunicator(player).sendCustomizeFace;
        assertNull(customize);
        assertThat(player, receivedMessageContaining("deftly dodge"));
    }

    @Test
    public void actionNotOwnerDoesNotSendsCustomizeFace() {
        Item writ = factory.createNewItem(BankerMod.getContractTemplateId());
        assertTrue(changeFace.action(mock(Action.class), player, writ, banker, changeFace.getActionId(), 0));

        assertNull(factory.getCommunicator(player).sendCustomizeFace);
        assertThat(player, receivedMessageContaining("deftly dodge"));
    }

    @Test
    public void actionCorrectActionIdOnly() {
        assertFalse(changeFace.action(mock(Action.class), player, banker, (short)(changeFace.getActionId() + 1), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(0, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
        assertNull(comm.sendCustomizeFace);
    }

    @Test
    public void actionIsPlayerOnly() {
        Creature creature = factory.createNewCreature();
        assertFalse(changeFace.action(mock(Action.class), creature, banker, changeFace.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(creature);
        assertEquals(0, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
        assertNull(comm.sendCustomizeFace);
    }

    @Test
    public void actionIsBankerOnly() {
        Creature creature = factory.createNewCreature();
        assertFalse(changeFace.action(mock(Action.class), player, creature, changeFace.getActionId(), 0));

        FakeCommunicator comm = factory.getCommunicator(player);
        assertEquals(0, comm.getMessages().length);
        assertEquals(0, comm.getBml().length);
        assertNull(comm.sendCustomizeFace);
    }
}
