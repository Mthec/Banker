package com.wurmonline.server.questions;

import com.wurmonline.server.banks.Bank;
import com.wurmonline.server.banks.BankUnavailableException;
import com.wurmonline.server.banks.Banks;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.villages.Village;
import mod.wurmunlimited.npcs.banker.BankerMod;
import mod.wurmunlimited.npcs.banker.BankerTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mod.wurmunlimited.Assert.didNotReceiveMessageContaining;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class BankerManageAccountQuestionAnswerTests extends BankerTest {
    private void answer(Properties properties) {
        Question question = new BankerManageAccountQuestion(player, banker);
        question.sendQuestion();
        question.answer(properties);
    }

    private Village createPermanentVillage() {
        Village village = factory.createVillageFor(factory.createNewPlayer());
        try {
            factory.setFinalField(village, Village.class.getDeclaredField("isPermanent"), true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        return village;
    }

    @Test
    public void testCancelNoBank() {
        assert Banks.getBank(player.getWurmId()) == null;

        Properties properties = new Properties();
        properties.setProperty("cancel", "true");
        properties.setProperty("open", "true");
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "true");
        properties.setProperty("village", "0");
        answer(properties);

        assertNull(Banks.getBank(player.getWurmId()));
        assertThat(player, didNotReceiveMessageContaining("You open a bank"));
    }

    @Test
    public void testCancelBank() throws BankUnavailableException {
        factory.createBankFor(player);

        Properties properties = new Properties();
        properties.setProperty("cancel", "true");
        properties.setProperty("open", "true");
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "true");
        properties.setProperty("village", "0");
        answer(properties);

        Bank notNewBank = Banks.getBank(player.getWurmId());
        assertNotNull(notNewBank);
        assertEquals(player.getCitizenVillage(), notNewBank.getCurrentVillage());
        assertEquals(-10, notNewBank.startedMoving);
        assertEquals(-10, notNewBank.targetVillage);
        assertThat(player, didNotReceiveMessageContaining("You open a bank"));
    }

    @Test
    public void testNoVillagesAvailableButNotCancelledSomeHow() {
        Properties properties = new Properties();
        properties.setProperty("open", "true");
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "true");
        properties.setProperty("village", "0");
        answer(properties);

        assertNull(Banks.getBank(player.getWurmId()));
        assertThat(player, receivedMessageContaining("stay where it is"));
    }

    @Test
    public void testGuestNoBank() {
        assert Banks.getBank(player.getWurmId()) == null;
        assert !BankerMod.allowGuestAccounts();
        createPermanentVillage();
        player.setGuest(true);

        Properties properties = new Properties();
        properties.setProperty("open", "true");
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "true");
        properties.setProperty("village", "0");
        answer(properties);

        assertNull(Banks.getBank(player.getWurmId()));
        assertThat(player, receivedMessageContaining("Guests may not"));
    }

    @Test
    public void testGuestNoBankAllowed() {
        assert Banks.getBank(player.getWurmId()) == null;
        player.setGuest(true);
        createPermanentVillage();
        Properties defaults = factory.defaultProperties();
        defaults.setProperty("allow_non_premium", "true");
        new BankerMod().configure(defaults);

        Properties properties = new Properties();
        properties.setProperty("open", "true");
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "true");
        properties.setProperty("village", "0");
        answer(properties);

        assertNotNull(Banks.getBank(player.getWurmId()));
        assertThat(player, receivedMessageContaining("You open a bank"));
    }

    @Test
    public void testGuestBankAllowed() throws BankUnavailableException {
        factory.createBankFor(player);
        player.setGuest(true);
        createPermanentVillage();
        Properties defaults = factory.defaultProperties();
        defaults.setProperty("allow_non_premium", "true");
        new BankerMod().configure(defaults);

        Properties properties = new Properties();
        properties.setProperty("open", "true");
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "true");
        properties.setProperty("village", "0");
        answer(properties);

        Bank notNewBank = Banks.getBank(player.getWurmId());
        assertNotNull(notNewBank);
        assertEquals(player.getCitizenVillage(), notNewBank.getCurrentVillage());
        assertEquals(-10, notNewBank.startedMoving);
        assertEquals(-10, notNewBank.targetVillage);
        assertThat(player, receivedMessageContaining("You already have a bank"));
    }

    @Test
    public void testVillageIndexMissing() {
        createPermanentVillage();

        Properties properties = new Properties();
        properties.setProperty("open", "true");
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "true");
        answer(properties);

        assertNull(Banks.getBank(player.getWurmId()));
        assertThat(player, receivedMessageContaining("chosen village was not found"));
    }

    @Test
    public void testOpenBank() throws BankUnavailableException {
        assert Banks.getBank(player.getWurmId()) == null;
        Village starter = createPermanentVillage();

        Properties properties = new Properties();
        properties.setProperty("open", "true");
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "true");
        properties.setProperty("village", "0");
        answer(properties);

        Bank newBank = Banks.getBank(player.getWurmId());
        assertNotNull(newBank);
        assertEquals(starter, newBank.getCurrentVillage());
        assertEquals(-10, newBank.startedMoving);
        assertEquals(-10, newBank.targetVillage);
        assertThat(player, receivedMessageContaining("You open a bank"));
    }

    @Test
    public void testOpenBankBadIndex() {
        assert Banks.getBank(player.getWurmId()) == null;
        createPermanentVillage();

        Properties properties = new Properties();
        properties.setProperty("open", "true");
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "true");
        properties.setProperty("village", "-10");
        answer(properties);

        Bank newBank = Banks.getBank(player.getWurmId());
        assertNull(newBank);
        assertThat(player, receivedMessageContaining("not found"));
        assertThat(player, didNotReceiveMessageContaining("You open a bank"));
    }

    @Test
    public void testOpenBankMoreOptions() throws BankUnavailableException {
        Village starter = createPermanentVillage();
        Village kingdom1 = factory.createVillageFor(factory.createNewPlayer());
        Village kingdom2 = factory.createVillageFor(factory.createNewPlayer());
        Map<String, Village> names = new HashMap<>();
        names.put(starter.getName(), starter);
        names.put(kingdom1.getName(), kingdom1);
        names.put(kingdom2.getName(), kingdom2);

        Properties properties = new Properties();
        properties.setProperty("open", "true");
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "true");

        Properties defaults = factory.defaultProperties();
        defaults.setProperty("village_option", "kingdom");
        new BankerMod().configure(defaults);
        Question question = new BankerManageAccountQuestion(player, banker);
        question.sendQuestion();
        Pattern regex = Pattern.compile("(Village\\d+,Village\\d+,Village\\d+)");
        Matcher matcher = regex.matcher(factory.getCommunicator(player).lastBmlContent);
        if (!matcher.find()) {
            System.out.println(factory.getCommunicator(player).lastBmlContent);
            throw new RuntimeException("No match found.");
        }

        int counter = 0;
        for (String villageName : matcher.group(1).split(",")) {
            assert Banks.getBank(player.getWurmId()) == null;
            properties.setProperty("village", Integer.toString(counter++));
            answer(properties);

            Bank newBank = Banks.getBank(player.getWurmId());
            assertNotNull(newBank);
            assertEquals(names.get(villageName), newBank.getCurrentVillage());
            assertEquals(-10, newBank.startedMoving);
            assertEquals(-10, newBank.targetVillage);
            factory.deleteBank(newBank);
        }
    }

    // Move account

    @Test
    public void testBankIsOpen() throws BankUnavailableException {
        Bank bank = factory.createBankFor(player);
        bank.open = true;
        createPermanentVillage();

        Properties properties = new Properties();
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "false");
        properties.setProperty("village", "0");

        answer(properties);

        assertThat(player, receivedMessageContaining("cannot manage it"));
        assertEquals(player.getCitizenVillage(), bank.getCurrentVillage());
    }

    @Test
    public void testBankIsAlreadyMovingThere() {
        Bank bank = factory.createBankFor(player);
        Village starter = createPermanentVillage();

        Properties properties = new Properties();
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "false");
        properties.setProperty("village", "0");

        Question question = new BankerManageAccountQuestion(player, banker);
        question.sendQuestion();
        bank.startMoving(starter.getId());
        question.answer(properties);

        assertThat(player, receivedMessageContaining("is already"));
        assertThrows(BankUnavailableException.class, bank::getCurrentVillage);
    }

    @Test
    public void testBankMoveInstantForGM() throws BankUnavailableException, IOException {
        Bank bank = factory.createBankFor(player);
        player.setPower((byte)1);
        Village starter = createPermanentVillage();

        Properties properties = new Properties();
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "false");
        properties.setProperty("village", "0");

        answer(properties);

        assertThat(player, receivedMessageContaining("extra powers"));
        assertEquals(starter, bank.getCurrentVillage());
    }

    @Test
    public void testBankMoveInstantForCurrentDisbanded() throws BankUnavailableException {
        Bank bank = factory.createBankFor(player);
        player.getCitizenVillage().disband("upkeep");
        Village starter = createPermanentVillage();

        Properties properties = new Properties();
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "false");
        properties.setProperty("village", "0");

        answer(properties);

        assertThat(player, receivedMessageContaining("has moved"));
        assertEquals(starter, bank.getCurrentVillage());
    }

    @Test
    public void testBankNotMoveInstantForTargetDisbanded() {
        Bank bank = factory.createBankFor(player);
        Village starter1 = createPermanentVillage();
        createPermanentVillage();
        bank.startMoving(starter1.getId());
        starter1.disband("upkeep");

        Properties properties = new Properties();
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "false");
        properties.setProperty("village", "1");

        answer(properties);

        assertThat(player, receivedMessageContaining("started moving"));
        assertThrows(BankUnavailableException.class, bank::getCurrentVillage);
    }

    @Test
    public void testBankMoveInstantForBribe() throws BankUnavailableException, IOException {
        Bank bank = factory.createBankFor(player);
        player.setMoney(MonetaryConstants.COIN_SILVER);
        Village starter = createPermanentVillage();

        Properties defaults = factory.defaultProperties();
        defaults.setProperty("bribery_cost", Integer.toString(MonetaryConstants.COIN_SILVER));
        new BankerMod().configure(defaults);

        Properties properties = new Properties();
        properties.setProperty("move", "false");
        properties.setProperty("bribe", "true");
        properties.setProperty("village", "0");

        answer(properties);

        assertThat(player, receivedMessageContaining("has been moved.  Thank you"));
        assertEquals(0, player.getMoney());
        assertEquals(starter, bank.getCurrentVillage());
    }

    @Test
    public void testBankMoveNotInstantForBribeWithoutEnoughMoney() throws IOException {
        Bank bank = factory.createBankFor(player);
        player.setMoney(MonetaryConstants.COIN_COPPER);
        Village starter = createPermanentVillage();

        Properties defaults = factory.defaultProperties();
        defaults.setProperty("bribery_cost", Integer.toString(MonetaryConstants.COIN_SILVER));
        new BankerMod().configure(defaults);

        Properties properties = new Properties();
        properties.setProperty("move", "false");
        properties.setProperty("bribe", "true");
        properties.setProperty("village", "0");

        answer(properties);
        long time = System.currentTimeMillis();

        assertThat(player, receivedMessageContaining("did not receive"));
        assertEquals(MonetaryConstants.COIN_COPPER, player.getMoney());
        assertThrows(BankUnavailableException.class, bank::getCurrentVillage);
        assertEquals(time / 100, bank.startedMoving / 100);
        assertEquals(starter.getId(), bank.targetVillage);
    }

    @Test
    public void testBankMoveNotInstantForNoBribe() throws IOException {
        Bank bank = factory.createBankFor(player);
        player.setMoney(MonetaryConstants.COIN_SILVER);
        Village starter = createPermanentVillage();

        Properties defaults = factory.defaultProperties();
        defaults.setProperty("bribery_cost", Integer.toString(MonetaryConstants.COIN_SILVER));
        new BankerMod().configure(defaults);

        Properties properties = new Properties();
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "false");
        properties.setProperty("village", "0");

        answer(properties);
        long time = System.currentTimeMillis();

        assertThat(player, receivedMessageContaining("will take 24 hours"));
        assertThat(player, didNotReceiveMessageContaining("did not receive"));
        assertEquals(MonetaryConstants.COIN_SILVER, player.getMoney());
        assertThrows(BankUnavailableException.class, bank::getCurrentVillage);
        assertEquals(time / 100, bank.startedMoving / 100);
        assertEquals(starter.getId(), bank.targetVillage);
    }

    @Test
    public void testBankMoveBribeAttemptWithBriberyDisabled() throws IOException {
        Bank bank = factory.createBankFor(player);
        player.setMoney(MonetaryConstants.COIN_SILVER);
        Village starter = createPermanentVillage();

        assert BankerMod.getBriberyCost() == 0;

        Properties properties = new Properties();
        properties.setProperty("move", "false");
        properties.setProperty("bribe", "true");
        properties.setProperty("village", "0");

        answer(properties);
        long time = System.currentTimeMillis();

        assertThat(player, receivedMessageContaining("will take 24 hours"));
        assertThat(player, didNotReceiveMessageContaining("did not receive"));
        assertEquals(MonetaryConstants.COIN_SILVER, player.getMoney());
        assertThrows(BankUnavailableException.class, bank::getCurrentVillage);
        assertEquals(time / 100, bank.startedMoving / 100);
        assertEquals(starter.getId(), bank.targetVillage);
    }

    @Test
    public void testBankMoveAccountWithBriberyDisabled() throws IOException {
        Bank bank = factory.createBankFor(player);
        player.setMoney(MonetaryConstants.COIN_SILVER);
        Village starter = createPermanentVillage();

        assert BankerMod.getBriberyCost() == 0;

        Properties properties = new Properties();
        properties.setProperty("move", "true");
        properties.setProperty("bribe", "false");
        properties.setProperty("village", "0");

        answer(properties);
        long time = System.currentTimeMillis();

        assertThat(player, receivedMessageContaining("will take 24 hours"));
        assertThat(player, didNotReceiveMessageContaining("did not receive"));
        assertEquals(MonetaryConstants.COIN_SILVER, player.getMoney());
        assertThrows(BankUnavailableException.class, bank::getCurrentVillage);
        assertEquals(time / 100, bank.startedMoving / 100);
        assertEquals(starter.getId(), bank.targetVillage);
    }

    @Test
    public void testBankMoveAgainWhenAlreadyMoving() {
        Bank bank = factory.createBankFor(player);
        Village starter1 = createPermanentVillage();
        Village starter2 = createPermanentVillage();
        bank.startMoving(starter1.getId());

        Properties properties = new Properties();
        properties.setProperty("move", "true");
        properties.setProperty("village", "1");

        answer(properties);
        long time = System.currentTimeMillis();

        assertThat(player, receivedMessageContaining("will take 24 hours"));
        assertThat(player, didNotReceiveMessageContaining("account has moved"));
        assertThrows(BankUnavailableException.class, bank::getCurrentVillage);
        assertEquals(time / 100, bank.startedMoving / 100);
        assertEquals(starter2.getId(), bank.targetVillage);
    }
}
