package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.FakeCommunicator;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.npcs.banker.BankerMod;
import mod.wurmunlimited.npcs.banker.BankerTemplate;
import mod.wurmunlimited.npcs.banker.BankerTest;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static mod.wurmunlimited.Assert.receivedBMLContaining;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BankerHireQuestionTests extends BankerTest {
    private VolaTile tile;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        factory.getAllCreatures().removeIf(c -> true);
    }
    
    // sendQuestion

    @Test
    public void testGenderCorrectlySet() {
        FakeCommunicator communicator = factory.getCommunicator(player);
        Properties properties = new Properties();

        properties.setProperty("gender", "male");
        new BankerHireQuestion(player, mock(VolaTile.class), 0, null, properties).sendQuestion();
        assertThat(player, receivedBMLContaining("Male\";selected=\"true\""));

        communicator.clearBml();
        properties.setProperty("gender", "female");
        new BankerHireQuestion(player, mock(VolaTile.class), 0, null, properties).sendQuestion();
        assertThat(player, receivedBMLContaining("Female\";selected=\"true\""));

        communicator.clearBml();
        new BankerHireQuestion(player, mock(VolaTile.class), 0, null, null).sendQuestion();
        assertThat(player, receivedBMLContaining("ale\";selected=\"true\""));
        assertThat(player, receivedBMLContaining("ale\"}"));
    }

    @Test
    public void testNameCorrectlySet() {
        FakeCommunicator communicator = factory.getCommunicator(player);
        Properties properties = new Properties();

        new BankerHireQuestion(player, mock(VolaTile.class), 0, null, null).sendQuestion();
        assertThat(player, receivedBMLContaining("input{text=\"\";id=\"name\";maxchars=\"20\"}"));

        communicator.clearBml();
        properties.setProperty("name", "test");
        new BankerHireQuestion(player, mock(VolaTile.class), 0, null, properties).sendQuestion();
        assertThat(player, receivedBMLContaining("input{text=\"test\";id=\"name\";maxchars=\"20\"}"));

        communicator.clearBml();
        properties.remove("name");
        new BankerHireQuestion(player, mock(VolaTile.class), 0, null, properties).sendQuestion();
        assertThat(player, receivedBMLContaining("input{text=\"\";id=\"name\";maxchars=\"20\"}"));
    }

    // answer
    private void answer(@Nullable String gender, @Nullable String name) {
        answer(gender, name, null);
    }

    private void answer(@Nullable String gender, @Nullable String name, @Nullable Item writ) {
        try {
            tile = Zones.getZone(player.getTileX(), player.getTileY(), player.isOnSurface()).getOrCreateTile(player.getTilePos());
            when(tile.getCreatures()).thenReturn(new Creature[0]);
            when(tile.getStructure()).thenReturn(null);
            answer(gender, name, writ, tile);
        } catch (NoSuchZoneException e) {
            throw new RuntimeException(e);
        }
    }

    private void answer(@Nullable String gender, @Nullable String name, @Nullable Item writ, VolaTile tile) {
        if (this.tile == null)
            this.tile = tile;
        Properties properties = new Properties();
        properties.setProperty("gender", gender != null ? gender : "female");
        properties.setProperty("name", name != null ? name : "");

        new BankerHireQuestion(player, tile, 0, writ, null).answer(properties);
    }

    private void hasArrived() {
        assert tile != null;
        Creature newBanker = factory.getAllCreatures().iterator().next();
        verify(tile, times(1)).broadCastAction(newBanker.getNameWithGenus() + " has arrived.", newBanker, null, false);
    }

    @Test
    public void testAnswer() {
        answer(null, null);

        hasArrived();
        assertEquals(1, factory.getAllCreatures().size());
        Creature banker = factory.getAllCreatures().iterator().next();
        assertTrue(BankerTemplate.is(banker));
    }

    @Test
    public void testAnswerGender() {
        answer("male", null);

        hasArrived();
        assertEquals(1, factory.getAllCreatures().size());
        Creature banker = factory.getAllCreatures().iterator().next();
        assertTrue(BankerTemplate.is(banker));
        assertEquals((byte)0, banker.getSex());

        factory.getAllCreatures().removeIf(c -> true);
        answer("female", null);

        hasArrived();
        assertEquals(1, factory.getAllCreatures().size());
        Creature banker2 = factory.getAllCreatures().iterator().next();
        assertTrue(BankerTemplate.is(banker2));
        assertEquals((byte)1, banker2.getSex());
    }

    @Test
    public void testAnswerName() {
        answer(null, "test");

        hasArrived();
        assertEquals(1, factory.getAllCreatures().size());
        Creature banker = factory.getAllCreatures().iterator().next();
        assertTrue(BankerTemplate.is(banker));
        assertEquals("Banker_Test", banker.getName());
    }

    @Test
    public void testAnswerNameWithoutPrefix() {
        Properties properties = factory.defaultProperties();
        properties.setProperty("name_prefix", "");
        new BankerMod().configure(properties);
        answer(null, "test");

        hasArrived();
        assertEquals(1, factory.getAllCreatures().size());
        Creature banker = factory.getAllCreatures().iterator().next();
        assertTrue(BankerTemplate.is(banker));
        assertEquals("Test", banker.getName());
    }

    @Test
    public void testAnswerProvideWrit() {
        Item writ = factory.createNewItem(BankerMod.getContractTemplateId());
        assert writ.getName().equals("banker contract");
        player.getInventory().insertItem(writ);
        answer(null, null, writ);

        hasArrived();
        assertEquals(1, factory.getAllCreatures().size());
        Creature banker2 = factory.getAllCreatures().iterator().next();
        assertTrue(BankerTemplate.is(banker2));
        assertEquals(1, player.getInventory().getItemCount());
        assertEquals(banker2.getWurmId(), writ.getData());
        assertEquals("banker writ", writ.getName());
    }

    @Test
    public void testAnswerTooManyCreatures() {
        Creature[] creatures = new Creature[] { player, factory.createNewCreature() };
        factory.removeCreature(creatures[1]);
        VolaTile tile = mock(VolaTile.class);
        when(tile.getCreatures()).thenReturn(creatures);
        when(tile.getStructure()).thenReturn(null);
        answer(null, null, null, tile);

        assertThat(player, receivedMessageContaining("no other creatures"));
        assertEquals(0, factory.getAllCreatures().size());
    }

    @Test
    public void testAnswerNotTooManyCreatures() throws NoSuchZoneException {
        VolaTile tile = Zones.getZone(player.getTileX(), player.getTileY(), player.isOnSurface()).getOrCreateTile(player.getTilePos());
        when(tile.getCreatures()).thenReturn(new Creature[] { player });
        when(tile.getStructure()).thenReturn(null);
        answer(null, null, null, tile);

        hasArrived();
        assertEquals(1, factory.getAllCreatures().size());
        Creature banker = factory.getAllCreatures().iterator().next();
        assertTrue(BankerTemplate.is(banker));
    }

    @Test
    public void testAnswerBuildingPermission() {
        VolaTile tile = mock(VolaTile.class);
        when(tile.getCreatures()).thenReturn(new Creature[] { player });
        Structure structure = mock(Structure.class);
        when(structure.mayPlaceMerchants(any())).thenReturn(false);
        when(tile.getStructure()).thenReturn(structure);
        answer(null, null, null, tile);

        assertThat(player, receivedMessageContaining("not have permission"));
        assertEquals(0, factory.getAllCreatures().size());
    }
}
