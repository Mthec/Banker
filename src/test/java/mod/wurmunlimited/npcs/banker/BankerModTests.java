package mod.wurmunlimited.npcs.banker;

import com.wurmonline.server.banks.Bank;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.kingdom.Kingdom;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.BankerHireQuestion;
import com.wurmonline.server.questions.CreatureCreationQuestion;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.questions.WithdrawMoneyQuestion;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Properties;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BankerModTests extends BankerTest {
    private BankerMod mod;
    private Properties properties;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mod = new BankerMod();
        properties = new Properties();
        properties.setProperty("contract_price", Integer.toString(MonetaryConstants.COIN_SILVER));
        properties.setProperty("bribery_cost", Integer.toString(0));
        mod.configure(properties);
    }

    @Test
    public void testConfigureVillageOptions() {
        for (BankerMod.VillageOptions option : BankerMod.VillageOptions.values()) {
            properties.setProperty("village_option", option.name().toLowerCase());
            mod.configure(properties);

            assertEquals(option, BankerMod.getVillageOptions());
        }

        properties.setProperty("village_option", "blah");
        mod.configure(properties);

        assertEquals(BankerMod.VillageOptions.VILLAGE, BankerMod.getVillageOptions());
    }

    @Test
    public void testBriberyCost() {
        for (int i = -10; i < 10; ++i) {
            properties.setProperty("bribery_cost", Integer.toString(i));
            mod.configure(properties);

            assertEquals(Math.max(i, 0), BankerMod.getBriberyCost());
        }

        properties.setProperty("bribery_cost", "abc");
        mod.configure(properties);

        assertEquals(0, BankerMod.getBriberyCost());
    }

    @Test
    public void testContractPrice() {
        for (int i = -10; i < 10; ++i) {
            properties.setProperty("contract_price", Integer.toString(i));
            mod.configure(properties);
            mod.onItemTemplatesCreated();

            if (i < 1)
                assertEquals(MonetaryConstants.COIN_SILVER, factory.createWritFor(player, banker).getValue());
            else
                assertEquals(i, factory.createWritFor(player, banker).getValue(), i);
        }

        properties.setProperty("contract_price", "abc");
        mod.configure(properties);
        mod.onItemTemplatesCreated();

        assertEquals(MonetaryConstants.COIN_SILVER, factory.createWritFor(player, banker).getValue());
    }

    @Test
    public void testGetFace() throws Throwable {
        long face = 123456;
        BankerDatabase.setFaceFor(banker, face);
        InvocationHandler handler = mod::getFace;
        Method method = mock(Method.class);
        when(method.invoke(any(), any())).thenReturn(-10L);
        Object[] args = new Object[0];

        assertEquals(face, handler.invoke(banker, method, args));
    }

    @Test
    public void testGetFaceNotBanker() throws Throwable {
        InvocationHandler handler = mod::getFace;
        Method method = mock(Method.class);
        when(method.invoke(any(), any())).thenReturn(-10L);
        Object[] args = new Object[0];

        assertEquals(-10L, handler.invoke(factory.createNewCreature(), method, args));
    }

    @Test
    void testGetBloodReturnsMinus1ForBanker() throws Throwable {
        InvocationHandler handler = mod::getBlood;
        Method method = mock(Method.class);
        Object[] args = new Object[0];

        Byte blood = (Byte)handler.invoke(banker, method, args);
        assertNotNull(blood);
        assertEquals((byte)-1, (byte)blood);
        verify(method, never()).invoke(banker, args);
    }

    @Test
    void testGetBloodReturnsNormalForNonBanker() throws Throwable {
        Creature creature = factory.createNewCreature();
        InvocationHandler handler = mod::getBlood;
        Method method = mock(Method.class);
        Object[] args = new Object[0];

        assertNull(handler.invoke(creature, method, args));
        verify(method, times(1)).invoke(creature, args);
    }

    @Test
    void testSendNewCreatureModifiesReturnForMinus1Blood() throws Throwable {
        InvocationHandler handler = mod::sendNewCreature;
        Method method = mock(Method.class);
        Object[] args = new Object[] { banker.getWurmId(), banker.getName(), "", "model.whatsit", 1, 1, 1, 1, 1, (byte)1, true, false, true, Kingdom.KINGDOM_MOLREHAN, 0, (byte)-1, false, false, (byte)0 };

        assertNull(handler.invoke(banker, method, args));
        assertEquals((byte)0, (byte)args[15]);
        assertTrue((boolean)args[17]);
        verify(method, times(1)).invoke(banker, args);
    }

    @Test
    void testSendNewCreatureReturnsNormalForNoneCrafter() throws Throwable {
        Creature creature = factory.createNewCreature();
        InvocationHandler handler = mod::sendNewCreature;
        Method method = mock(Method.class);
        Object[] args = new Object[] { creature.getWurmId(), creature.getName(), "", "model.whatsit", 1, 1, 1, 1, 1, (byte)1, true, false, true, Kingdom.KINGDOM_MOLREHAN, 0, (byte)0, false, false, (byte)0 };
        assert !(boolean)args[17];

        assertNull(handler.invoke(creature, method, args));
        assertFalse((boolean)args[17]);
        verify(method, times(1)).invoke(creature, args);
    }

    @Test
    void testSendNewCreatureReturnsNormalForPlayerEvenIfBloodWasMinus1() throws Throwable {
        InvocationHandler handler = mod::sendNewCreature;
        Method method = mock(Method.class);
        Object[] args = new Object[] { player.getWurmId(), player.getName(), "", "model.whatsit", 1, 1, 1, 1, 1, (byte)1, true, false, true, Kingdom.KINGDOM_MOLREHAN, 0, (byte)-1, false, false, (byte)0 };
        assert !(boolean)args[17];

        assertNull(handler.invoke(player, method, args));
        assertFalse((boolean)args[17]);
        verify(method, times(1)).invoke(player, args);
    }

    @Test
    public void testDeleteFaceOnDestroy() throws Throwable {
        assert !banker.isDead();
        Long face = BankerDatabase.getFaceFor(banker);
        assert face != null;
        InvocationHandler handler = mod::destroy;
        Method method = mock(Method.class);
        Object[] args = new Object[0];

        assertNull(handler.invoke(banker, method, args));
        assertNull(BankerDatabase.getFaceFor(banker));
        verify(method, times(1)).invoke(banker, args);
    }

    @Test
    void testCreatureCreation() throws Throwable {
        Item wand = factory.createNewItem(ItemList.wandGM);
        String name = "Name";
        int tileX = 250;
        int tileY = 250;

        InvocationHandler handler = mod::creatureCreation;
        Method method = mock(Method.class);
        Object[] args = new Object[] { new CreatureCreationQuestion(player, "", "", wand.getWurmId(), tileX, tileY, -1, -10)};
        ((CreatureCreationQuestion)args[0]).sendQuestion();
        factory.getCommunicator(player).clearBml();
        int templateIndex = -1;
        int i = 0;
        //noinspection unchecked
        for (CreatureTemplate template : ((List<CreatureTemplate>)ReflectionUtil.getPrivateField(args[0], CreatureCreationQuestion.class.getDeclaredField("cretemplates")))) {
            if (template.getTemplateId() == factory.bankerTemplateId) {
                templateIndex = i;
                break;
            }
            ++i;
        }
        assert templateIndex != -1;
        Properties answers = new Properties();
        answers.setProperty("data1", String.valueOf(i));
        answers.setProperty("cname", name);
        answers.setProperty("gender", "female");
        ReflectionUtil.setPrivateField(args[0], Question.class.getDeclaredField("answer"), answers);

        for (Creature creature : factory.getAllCreatures().toArray(new Creature[0])) {
            factory.removeCreature(creature);
        }
        assert factory.getAllCreatures().size() == 0;

        assertNull(handler.invoke(null, method, args));
        verify(method, never()).invoke(null, args);
        assertEquals(0, factory.getAllCreatures().size());
        properties.setProperty("name", name);
        new BankerHireQuestion(player, player.getCurrentTile(), player.getFloorLevel(), null, properties).sendQuestion();
        assertThat(player, bmlEqual());
        assertThat(player, didNotReceiveMessageContaining("An error occurred"));
    }

    @Test
    void testNonCustomTraderCreatureCreation() throws Throwable {
        Item wand = factory.createNewItem(ItemList.wandGM);
        int tileX = 250;
        int tileY = 250;

        InvocationHandler handler = mod::creatureCreation;
        Method method = mock(Method.class);
        Object[] args = new Object[] { new CreatureCreationQuestion(player, "", "", wand.getWurmId(), tileX, tileY, -1, -10)};
        ((CreatureCreationQuestion)args[0]).sendQuestion();
        Properties answers = new Properties();
        answers.setProperty("data1", String.valueOf(0));
        answers.setProperty("cname", "MyName");
        answers.setProperty("gender", "female");
        ReflectionUtil.setPrivateField(args[0], Question.class.getDeclaredField("answer"), answers);

        for (Creature creature : factory.getAllCreatures().toArray(new Creature[0])) {
            factory.removeCreature(creature);
        }

        assertNull(handler.invoke(null, method, args));
        verify(method, times(1)).invoke(null, args);
        assertEquals(0, factory.getAllCreatures().size());
        assertEquals(1, factory.getCommunicator(player).getBml().length);
        assertThat(player, didNotReceiveMessageContaining("An error occurred"));
    }

    @Test
    public void testSetFace() throws Throwable {
        long face = 123456;
        Long current = BankerDatabase.getFaceFor(banker);
        assert current != null && current != face;
        InvocationHandler handler = mod::setFace;
        Method method = mock(Method.class);
        ByteBuffer buffer = mock(ByteBuffer.class);
        long id = BankerMod.faceSetters.createIdFor(banker, player);
        when(buffer.getLong()).thenReturn(face, id);
        Object[] args = new Object[] { buffer };

        assertNull(handler.invoke(player.getCommunicator(), method, args));
        assertEquals(face, BankerDatabase.getFaceFor(banker));
        verify(buffer, never()).reset();
        assertThat(player, receivedMessageContaining("banker's face"));
    }

    @Test
    public void testSetFaceNotBanker() throws Throwable {
        long face = 123456;
        InvocationHandler handler = mod::setFace;
        Method method = mock(Method.class);
        ByteBuffer buffer = mock(ByteBuffer.class);
        when(buffer.getLong()).thenReturn(face, factory.createNewItem(ItemList.handMirror).getWurmId());
        Object[] args = new Object[] { buffer };

        assertNull(handler.invoke(player.getCommunicator(), method, args));
        verify(buffer, times(1)).reset();
        verify(method, times(1)).invoke(player.getCommunicator(), args);
        assertThat(player, didNotReceiveMessageContaining("banker's face"));
    }

    @Test
    public void testSetFaceBankerChangingStatusLost() throws Throwable {
        long face = 123456;
        Long current = BankerDatabase.getFaceFor(banker);
        assert current != null && current != face;
        InvocationHandler handler = mod::setFace;
        Method method = mock(Method.class);
        ByteBuffer buffer = mock(ByteBuffer.class);
        when(buffer.getLong()).thenReturn(face, -10L);
        Object[] args = new Object[] { buffer };

        assertNull(handler.invoke(player.getCommunicator(), method, args));
        assertNotEquals(face, BankerDatabase.getFaceFor(banker));
        verify(buffer, never()).reset();
        assertThat(player, receivedMessageContaining("returns"));
        assertThat(player, didNotReceiveMessageContaining("a new form"));
    }

    @Test
    public void testWithdrawMoney() throws Throwable {
        Bank bank = factory.createBankFor(player);
        player.setMoney(12345);
        WithdrawMoneyQuestion question = new WithdrawMoneyQuestion(player, "", "", bank.getCurrentVillage().getToken().getWurmId());
        BankerMod.withdrawals.add(question.getId());
        properties.setProperty("gold", Integer.toString(0));
        properties.setProperty("silver", Integer.toString(1));
        properties.setProperty("copper", Integer.toString(23));
        properties.setProperty("iron", Integer.toString(45));
        ReflectionUtil.setPrivateField(question, Question.class.getDeclaredField("answer"), properties);

        InvocationHandler handler = mod::withdrawMoney;
        Method method = mock(Method.class);
        Object[] args = new Object[] { question };

        assertNull(handler.invoke(null, method, args));
        assertEquals(0, player.getMoney());
        assertThat(player, hasCoinsOfValue(12345));
        verify(method, never()).invoke(any(), any());
        assertThat(player, receivedMessageContaining("You withdraw"));
        assertThat(player, receivedMessageContaining("New balance"));
    }

    @Test
    public void testWithdrawMoneyDead() throws Throwable {
        Bank bank = factory.createBankFor(player);
        player.setMoney(0);
        WithdrawMoneyQuestion question = new WithdrawMoneyQuestion(player, "", "", bank.getCurrentVillage().getToken().getWurmId());
        BankerMod.withdrawals.add(question.getId());
        player.getSaveFile().dead = true;

        InvocationHandler handler = mod::withdrawMoney;
        Method method = mock(Method.class);
        Object[] args = new Object[] { question };

        assertNull(handler.invoke(null, method, args));
        assertEquals(0, player.getMoney());
        assertThat(player, hasCoinsOfValue(0));
        verify(method, never()).invoke(any(), any());
        assertThat(player, receivedMessageContaining("are dead"));
    }

    @Test
    public void testWithdrawMoneyNoMoney() throws Throwable {
        Bank bank = factory.createBankFor(player);
        player.setMoney(0);
        WithdrawMoneyQuestion question = new WithdrawMoneyQuestion(player, "", "", bank.getCurrentVillage().getToken().getWurmId());
        BankerMod.withdrawals.add(question.getId());

        InvocationHandler handler = mod::withdrawMoney;
        Method method = mock(Method.class);
        Object[] args = new Object[] { question };

        assertNull(handler.invoke(null, method, args));
        assertEquals(0, player.getMoney());
        assertThat(player, hasCoinsOfValue(0));
        verify(method, never()).invoke(any(), any());
        assertThat(player, receivedMessageContaining("no money"));
    }

    @Test
    public void testWithdrawMoneyWithdrawNone() throws Throwable {
        Bank bank = factory.createBankFor(player);
        player.setMoney(12345);
        WithdrawMoneyQuestion question = new WithdrawMoneyQuestion(player, "", "", bank.getCurrentVillage().getToken().getWurmId());
        BankerMod.withdrawals.add(question.getId());
        properties.setProperty("gold", Integer.toString(0));
        properties.setProperty("silver", Integer.toString(0));
        properties.setProperty("copper", Integer.toString(0));
        properties.setProperty("iron", Integer.toString(0));
        ReflectionUtil.setPrivateField(question, Question.class.getDeclaredField("answer"), properties);

        InvocationHandler handler = mod::withdrawMoney;
        Method method = mock(Method.class);
        Object[] args = new Object[] { question };

        assertNull(handler.invoke(null, method, args));
        assertEquals(12345, player.getMoney());
        assertThat(player, hasCoinsOfValue(0));
        verify(method, never()).invoke(any(), any());
        assertThat(player, receivedMessageContaining("No money withdrawn"));
    }

    @Test
    public void testWithdrawMoneyNotEnoughMoney() throws Throwable {
        Bank bank = factory.createBankFor(player);
        player.setMoney(12345);
        WithdrawMoneyQuestion question = new WithdrawMoneyQuestion(player, "", "", bank.getCurrentVillage().getToken().getWurmId());
        BankerMod.withdrawals.add(question.getId());
        properties.setProperty("gold", Integer.toString(1));
        properties.setProperty("silver", Integer.toString(1));
        properties.setProperty("copper", Integer.toString(23));
        properties.setProperty("iron", Integer.toString(45));
        ReflectionUtil.setPrivateField(question, Question.class.getDeclaredField("answer"), properties);

        InvocationHandler handler = mod::withdrawMoney;
        Method method = mock(Method.class);
        Object[] args = new Object[] { question };

        assertNull(handler.invoke(null, method, args));
        assertEquals(12345, player.getMoney());
        assertThat(player, hasCoinsOfValue(0));
        verify(method, never()).invoke(any(), any());
        assertThat(player, receivedMessageContaining("can not withdraw"));
    }

    @Test
    public void testWithdrawMoneyIOException() throws Throwable {
        Bank bank = factory.createBankFor(player);
        Player player = mock(Player.class);
        when(player.getMoney()).thenReturn(12345L);
        doThrow(IOException.class).when(player).chargeMoney(12345);
        Communicator comm = mock(Communicator.class);
        when(player.getCommunicator()).thenReturn(comm);
        WithdrawMoneyQuestion question = new WithdrawMoneyQuestion(player, "", "", bank.getCurrentVillage().getToken().getWurmId());
        BankerMod.withdrawals.add(question.getId());
        properties.setProperty("gold", Integer.toString(0));
        properties.setProperty("silver", Integer.toString(1));
        properties.setProperty("copper", Integer.toString(23));
        properties.setProperty("iron", Integer.toString(45));
        ReflectionUtil.setPrivateField(question, Question.class.getDeclaredField("answer"), properties);

        InvocationHandler handler = mod::withdrawMoney;
        Method method = mock(Method.class);
        Object[] args = new Object[] { question };

        assertNull(handler.invoke(null, method, args));
        verify(method, never()).invoke(any(), any());
        verify(comm).sendNormalServerMessage("The transaction failed. Please contact the game masters using the <i>/dev</i> command.");
    }

    @Test
    public void testWithdrawMoneyNotBankerTransaction() throws Throwable {
        Bank bank = factory.createBankFor(player);
        player.setMoney(12345);
        WithdrawMoneyQuestion question = new WithdrawMoneyQuestion(player, "", "", bank.getCurrentVillage().getToken().getWurmId());
        assert BankerMod.withdrawals.isEmpty();

        InvocationHandler handler = mod::withdrawMoney;
        Method method = mock(Method.class);
        Object[] args = new Object[] { question };

        assertNull(handler.invoke(null, method, args));
        assertEquals(12345, player.getMoney());
        assertThat(player, hasCoinsOfValue(0));
        verify(method, times(1)).invoke(null, args);
        assertEquals(1, factory.getCommunicator(player).getMessages().length);
    }

    @Test
    public void getValueWithdrawn() {
        properties.setProperty("gold", Integer.toString(123));
        properties.setProperty("silver", Integer.toString(45));
        properties.setProperty("copper", Integer.toString(67));
        properties.setProperty("iron", Integer.toString(89));
        assertEquals(123456789, mod.getValueWithdrawn(player, properties));
        assertEquals(0, factory.getCommunicator(player).getMessages().length);
    }

    @Test
    public void getValueWithdrawnIncorrectValues() {
        properties.setProperty("gold", "abc");
        properties.setProperty("silver", Integer.toString(45));
        properties.setProperty("copper", Integer.toString(67));
        properties.setProperty("iron", Integer.toString(89));
        assertEquals(0, mod.getValueWithdrawn(player, properties));
        assertThat(player, receivedMessageContaining("incorrect"));
    }

    @Test
    public void getValueWithdrawnNegative() {
        properties.setProperty("gold", Integer.toString(123));
        properties.setProperty("silver", Integer.toString(45));
        properties.setProperty("copper", Integer.toString(-10));
        properties.setProperty("iron", Integer.toString(89));
        assertEquals(123450089, mod.getValueWithdrawn(player, properties));
        assertThat(player, receivedMessageContaining("negative"));
    }
}
