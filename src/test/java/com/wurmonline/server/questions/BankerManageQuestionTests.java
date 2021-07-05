package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplateFactory;
import com.wurmonline.server.creatures.FakeCreatureStatus;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.WurmObjectsFactory;
import mod.wurmunlimited.npcs.banker.BankerMod;
import mod.wurmunlimited.npcs.banker.BankerTest;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BankerManageQuestionTests extends BankerTest {
    private static final long face = 12345;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        banker = mock(Creature.class);
        when(banker.getWurmId()).thenReturn(987654L);
        BankerMod.mod.faceSetter.setFaceFor(banker, face);
        AtomicReference<String> name = new AtomicReference<>("Banker_Dave");
        when(banker.getName()).thenAnswer(i -> name.get());
        doAnswer((Answer<Void>)i -> {
            name.set(i.getArgument(0));
            return null;
        }).when(banker).setName(anyString());
        when(banker.getFace()).thenAnswer(i -> getFace());
        VolaTile tile = Zones.getOrCreateTile(10, 10, true);
        when(banker.getCurrentTile()).thenReturn(tile);
        when(banker.getTileX()).thenReturn(10);
        when(banker.getTileY()).thenReturn(10);
        when(banker.isOnSurface()).thenReturn(true);
        when(banker.getTemplate()).thenReturn(CreatureTemplateFactory.getInstance().getTemplate("banker"));
        FakeCreatureStatus status = new FakeCreatureStatus(banker);
        when(banker.getStatus()).thenReturn(status);
        AtomicBoolean dead = new AtomicBoolean(false);
        when(banker.isDead()).thenAnswer(i -> dead.get());
        doAnswer(i -> {
            dead.set(true);
            return null;
        }).when(banker).destroy();
        when(banker.getHisHerItsString()).thenReturn("their");
        ReflectionUtil.<Map<Long, Creature>>getPrivateField(factory, WurmObjectsFactory.class.getDeclaredField("creatures")).put(banker.getWurmId(), banker);
    }

    private Long getFace() {
        return BankerMod.mod.faceSetter.getFaceFor(banker);
    }

    // sendQuestion

    @Test
    public void testNameCorrectlySet() throws SQLException {
        new BankerManageQuestion(player, banker).sendQuestion();
        assertThat(player, receivedBMLContaining("input{text=\"Dave\";id=\"name\";maxchars=\"" + BankerMod.maxNameLength + "\"}"));
    }

    @Test
    public void testNameWithBlankPrefixCorrectlySet() throws SQLException {
        Properties properties = factory.defaultProperties();
        properties.setProperty("name_prefix", "");
        BankerMod.mod.configure(properties);

        new BankerManageQuestion(player, banker).sendQuestion();
        assertThat(player, receivedBMLContaining("input{text=\"Banker_Dave\";id=\"name\";maxchars=\"" + BankerMod.maxNameLength + "\"}"));
    }

    @Test
    public void testFaceCorrectlySet() throws SQLException {
        new BankerManageQuestion(player, banker).sendQuestion();
        assertThat(player, receivedBMLContaining("input{text=\"" + face + "\";id=\"face\";maxchars=\"" + Long.toString(Long.MAX_VALUE).length() + "\"}"));
    }

    // answer

    private void answer(@Nullable String name) {
        Properties properties = new Properties();
        properties.setProperty("name", name != null ? name : "");

        new BankerManageQuestion(player, banker).answer(properties);
    }

    @Test
    public void testAnswerBankerDead() {
        reset(banker.getCurrentTile());
        banker.destroy();
        answer(null);

        assertEquals(0, factory.getCommunicator(player).getBml().length);
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
        banker.currentTile = null;
        verify(banker.getCurrentTile(), times(1)).broadCastAction(banker.getName() + " grunts, packs " + banker.getHisHerItsString() + " things and is off.", banker, null, false);
        assertNull(factory.getCommunicator(player).sendCustomizeFace);
        assertThat(player, receivedMessageContaining("dismiss"));
        assertEquals("banker contract", writ.getName());
        assertEquals(-1, writ.getData());
        assertTrue(banker.isDead());
    }

    @Test
    public void testAnswerName() {
        String name = "Robin";
        String fullName = "Banker_" + name;
        assert !banker.getName().equals(fullName);
        reset(banker.getCurrentTile());
        answer(name);

        assertEquals(0, factory.getCommunicator(player).getBml().length);
        assertEquals(fullName, banker.getName());
        assertEquals(fullName, ((FakeCreatureStatus)banker.getStatus()).savedName);
        assertThat(player, receivedMessageContaining(banker.getName()));
    }

    @Test
    public void testAnswerNameIllegalCharacters() {
        String name = "Dave%^";
        String fullName = "Banker_" + name;
        String oldName = banker.getName();
        assert !banker.getName().equals(fullName);
        reset(banker.getCurrentTile());
        answer(name);

        assertEquals(0, factory.getCommunicator(player).getBml().length);
        assertEquals(oldName, banker.getName());
        assertEquals(FakeCreatureStatus.unset, ((FakeCreatureStatus)banker.getStatus()).savedName);
        assertThat(player, receivedMessageContaining("remain " + oldName));
    }

    @Test
    public void testAnswerCustomiseQuestionSent() {
        Properties properties = new Properties();
        properties.setProperty("name", "Albert");
        properties.setProperty("customise", "true");
        new BankerManageQuestion(player, banker).answer(properties);

        assertThat(player, didNotReceiveMessageContaining(banker.getName()));

        new CreatureCustomiserQuestion(player, banker, BankerMod.mod.faceSetter, BankerMod.mod.modelSetter, BankerManageAccountQuestion.modelOptions).sendQuestion();
        assertThat(player, bmlEqual());
    }
}
