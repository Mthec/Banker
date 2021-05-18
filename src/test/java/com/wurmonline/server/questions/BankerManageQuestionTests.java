package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.FakeCommunicator;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.npcs.banker.BankerDatabase;
import mod.wurmunlimited.npcs.banker.BankerMod;
import mod.wurmunlimited.npcs.banker.BankerTest;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import static mod.wurmunlimited.Assert.receivedBMLContaining;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BankerManageQuestionTests extends BankerTest {
    private Long getFace() {
        try {
            //noinspection unchecked
            return ((Map<Creature, Long>)ReflectionUtil.getPrivateField(null, BankerDatabase.class.getDeclaredField("faces"))).get(banker);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // sendQuestion

//    @Test
//    public void testNameCorrectlySet() {
//        FakeCommunicator communicator = factory.getCommunicator(player);
//        Properties properties = factory.defaultProperties();
//        BankerMod mod = new BankerMod();
//
//        banker.setName("Banker_Fred");
//        new BankerManageQuestion(player, banker).sendQuestion();
//        assertThat(player, receivedBMLContaining("input{text=\"Fred\";id=\"name\";maxchars=\"20\"}"));
//
//        communicator.clearBml();
//        properties.setProperty("name_prefix", "");
//        mod.configure(properties);
//        new BankerManageQuestion(player, banker).sendQuestion();
//        assertThat(player, receivedBMLContaining("input{text=\"Banker_Fred\";id=\"name\";maxchars=\"20\"}"));
//    }

    @Test
    public void testFaceCorrectlySet() throws SQLException {
        long face = 12345;
        Creature fakeBanker = mock(Creature.class);
        when(fakeBanker.getName()).thenReturn(banker.getName());
        when(fakeBanker.getFace()).thenReturn(face);

        BankerDatabase.setFaceFor(banker, face);
        new BankerManageQuestion(player, fakeBanker).sendQuestion();;
        assertThat(player, receivedBMLContaining("input{text=\"" + face + "\";id=\"face\";maxchars=\"" + Long.toString(Long.MAX_VALUE).length() + "\"}"));
    }

    // answer
    private void answer(@Nullable String face, @Nullable String name) {
        Properties properties = new Properties();
        properties.setProperty("face", face != null ? face : Long.toString(getFace()));
        properties.setProperty("name", name != null ? name : banker.getName());

        new BankerManageQuestion(player, banker).answer(properties);
    }

//    @Test
//    public void testAnswer() {
//        String oldName = banker.getName();
//        answer(null, "");
//
//        assertThat(player, receivedMessageContaining("chose a new one"));
//        assertNotEquals(oldName, banker.getName());
//        assertNull(factory.getCommunicator(player).sendCustomizeFace);
//    }
//
//    @Test
//    public void testAnswerName() {
//        answer(null, "test");
//
//        assertEquals(0, factory.getCommunicator(player).getBml().length);
//        assertEquals("Banker_Test", banker.getName());
//        assertNull(factory.getCommunicator(player).sendCustomizeFace);
//    }
//
//    @Test
//    public void testAnswerNameWithoutPrefix() {
//        Properties properties = factory.defaultProperties();
//        properties.setProperty("name_prefix", "");
//        new BankerMod().configure(properties);
//        answer(null, "test");
//
//        assertEquals(0, factory.getCommunicator(player).getBml().length);
//        assertEquals("Test", banker.getName());
//        assertNull(factory.getCommunicator(player).sendCustomizeFace);
//    }

    @Test
    public void testAnswerBankerDead() {
        reset(banker.getCurrentTile());
        banker.destroy();
        answer(Long.toString(12345), null);

        assertEquals(0, factory.getCommunicator(player).getBml().length);
        verify(banker.getCurrentTile(), never()).setNewFace(banker);
        assertNull(factory.getCommunicator(player).sendCustomizeFace);
        assertThat(player, receivedMessageContaining("disappeared"));
    }

    @Test
    public void testAnswerDismiss() {
        Item writ = factory.createWritFor(player, banker);
        reset(banker.getCurrentTile());
        Properties properties = new Properties();
        properties.setProperty("dismiss", "true");
        new BankerManageQuestion(player, banker).answer(properties);

        assertEquals(0, factory.getCommunicator(player).getBml().length);
        verify(banker.getCurrentTile(), never()).setNewFace(banker);
        banker.currentTile = null;
        verify(banker.getCurrentTile(), times(1)).broadCastAction(banker.getName() + " grunts, packs " + banker.getHisHerItsString() + " things and is off.", banker, null, false);
        assertNull(factory.getCommunicator(player).sendCustomizeFace);
        assertThat(player, receivedMessageContaining("dismiss"));
        assertEquals("banker contract", writ.getName());
        assertEquals(-1, writ.getData());
        assertTrue(banker.isDead());
    }

    @Test
    public void testAnswerFace() {
        long face = 12345;
        assert getFace() != face;
        reset(banker.getCurrentTile());
        answer(Long.toString(face), null);

        assertEquals(0, factory.getCommunicator(player).getBml().length);
        assertEquals(face, getFace());
        verify(banker.getCurrentTile(), times(1)).setNewFace(banker);
        assertNull(factory.getCommunicator(player).sendCustomizeFace);
    }

    @Test
    public void testAnswerEmptyFace() {
        reset(banker.getCurrentTile());
        answer("", null);

        assertEquals(0, factory.getCommunicator(player).getBml().length);
        verify(banker.getCurrentTile(), never()).setNewFace(banker);
        assertNotNull(factory.getCommunicator(player).sendCustomizeFace);
    }

    @Test
    public void testAnswerInvalidFace() {
        reset(banker.getCurrentTile());
        answer("abc", null);

        assertThat(player, receivedMessageContaining("Invalid face"));
        verify(banker.getCurrentTile(), never()).setNewFace(banker);
        assertNull(factory.getCommunicator(player).sendCustomizeFace);
    }
}
