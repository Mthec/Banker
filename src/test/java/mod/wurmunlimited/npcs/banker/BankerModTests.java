package mod.wurmunlimited.npcs.banker;

import com.wurmonline.server.banks.Bank;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
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
import java.util.List;
import java.util.Properties;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
