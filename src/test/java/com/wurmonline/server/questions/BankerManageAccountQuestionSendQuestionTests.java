package com.wurmonline.server.questions;

import com.wurmonline.server.banks.Bank;
import com.wurmonline.server.banks.Banks;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.villages.*;
import mod.wurmunlimited.npcs.banker.BankerMod;
import mod.wurmunlimited.npcs.banker.BankerTest;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mod.wurmunlimited.Assert.didNotReceiveBMLContaining;
import static mod.wurmunlimited.Assert.receivedBMLContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class BankerManageAccountQuestionSendQuestionTests extends BankerTest {
    private final Pattern villagesRegex = Pattern.compile("dropdown\\{id=\"village\";options=\"([\\w ,]+)\";default=\"([0-9]+)\"}");

    private void configureVillageOption(BankerMod.VillageOptions option, boolean bribery) {
        Properties properties = factory.defaultProperties();
        properties.setProperty("village_option", option.name().toLowerCase());
        if (bribery) {
            properties.setProperty("bribery_cost", "10000");
        }
        new BankerMod().configure(properties);
        factory.getCommunicator(player).clearBml();
        new BankerManageAccountQuestion(player, banker).sendQuestion();
    }

    private List<String> getVillageNames() {
        List<String> names = new ArrayList<>();
        Matcher matcher = villagesRegex.matcher(factory.getCommunicator(player).lastBmlContent);

        if (matcher.find()) {
            Collections.addAll(names, matcher.group(1).split(","));
        }

        return names;
    }

    private void setAlliancePermissions(Village village, Creature creature) {
        try {
            VillageRole role = new DbVillageRole(village.id, "Role for " + creature.getName(), false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, (byte)0, 0, false, false, false, false, false, false, false, false, false, 0, 0, 0, 0);
            role.SetCanPerformActionsOnAlliedDeeds(true);
            village.addRole(role);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertVillagesEqual(List<String> actual, int excludeVillageId, Village... expected) {
        List<String> expectedNames = new ArrayList<>();
        List<String> notExpectedNames = new ArrayList<>();
        for (Village village : expected) {
            if (village.id != excludeVillageId) {
                expectedNames.add(village.getName());
            } else {
                notExpectedNames.add(village.getName());
            }
        }
        assertEquals(expectedNames.size(), actual.size(), actual.toString());
        assertTrue(actual.containsAll(expectedNames), actual.toString());
        assertTrue(actual.stream().noneMatch(notExpectedNames::contains), actual.toString());
    }

    private void validateVillageOptions(Function<BankerMod.VillageOptions, Void> func) throws IOException, NoSuchFieldException {
        validateVillageOptions(func, false);
    }

    private void validateVillageOptions(Function<BankerMod.VillageOptions, Void> func, boolean bribery) throws IOException, NoSuchFieldException {
        Village kingdom1 = factory.createVillageFor(factory.createNewPlayer());
        Village kingdom2 = factory.createVillageFor(factory.createNewPlayer());
        int allianceNumber = 123;
        Village alliance = factory.createVillageFor(factory.createNewPlayer());
        alliance.setAllianceNumber(allianceNumber);
        setAlliancePermissions(alliance, player);
        Village village = player.getCitizenVillage();
        if (village == null)
             village = factory.createVillageFor(player);
        village.setAllianceNumber(allianceNumber);
        Village starter = factory.createVillageFor(factory.createNewPlayer());
        factory.setFinalField(starter, Village.class.getDeclaredField("isPermanent"), true);
        Player enemy = factory.createNewPlayer();
        enemy.setKingdomId((byte)(player.getKingdomId() + 1));
        factory.createVillageFor(enemy);

        int excludeVillageId = -1;
        Bank bank = Banks.getBank(player.getWurmId());
        if (bank != null) {
            if (bank.targetVillage > 0)
                excludeVillageId = bank.targetVillage;
            else
                excludeVillageId = bank.currentVillage;
        }

        for (BankerMod.VillageOptions option : BankerMod.VillageOptions.values()) {
            configureVillageOption(option, bribery);
            List<String> villages = getVillageNames();
            assertFalse(villages.contains(enemy.getName()));

            switch (option) {
                case KINGDOM:
                    assertVillagesEqual(villages, excludeVillageId,
                            kingdom1, kingdom2, alliance, village, starter);
                    break;
                case ALLIANCE:
                    assertVillagesEqual(villages, excludeVillageId,
                            alliance, village, starter);
                    break;
                case VILLAGE:
                    assertVillagesEqual(villages, excludeVillageId,
                            village, starter);
                    break;
                case STARTER:
                    assertVillagesEqual(villages, excludeVillageId,
                            starter);
                    break;
            }

            func.apply(option);
        }
    }

    // Guest accounts

    @Test
    public void testNotGuest() {
        assert !BankerMod.allowGuestAccounts();
        assert !player.isGuest();
        new BankerManageAccountQuestion(player, banker).sendQuestion();
        assertThat(player, receivedBMLContaining("You may open a bank account"));
    }

    @Test
    public void testGuestsMayNotOpen() {
        assert !BankerMod.allowGuestAccounts();
        player.setGuest(true);
        new BankerManageAccountQuestion(player, banker).sendQuestion();
        assertThat(player, receivedBMLContaining("Guests may not open bank accounts"));
    }

    @Test
    public void testGuestsMayOpen() {
        Properties properties = factory.defaultProperties();
        properties.setProperty("allow_non_premium", "true");
        new BankerMod().configure(properties);
        assert BankerMod.allowGuestAccounts();
        player.setGuest(true);
        new BankerManageAccountQuestion(player, banker).sendQuestion();
        assertThat(player, receivedBMLContaining("You may open a bank account"));
    }

    // Open account

    @Test
    public void testOpenAccountVillageOptions() throws IOException, NoSuchFieldException {
        validateVillageOptions(option -> {
            assertThat(player, receivedBMLContaining("You may open a bank account"));
            return null;
        });
    }

    @Test
    public void testOpenAccountVillageOptionsNoVillages() {
        for (BankerMod.VillageOptions option : BankerMod.VillageOptions.values()) {
            System.out.println(option.name());
            configureVillageOption(option, false);
            assertThat(player, receivedBMLContaining("You may open a bank account"));
            assertThat(player, receivedBMLContaining("Nowhere"));
            assertThat(player, didNotReceiveBMLContaining("Bribe"));

            configureVillageOption(option, true);
            assertThat(player, receivedBMLContaining("You may open a bank account"));
            assertThat(player, receivedBMLContaining("Nowhere"));
            assertThat(player, didNotReceiveBMLContaining("Bribe"));
        }
    }

    // Move account

    @Test
    public void testBankAlreadyMoving() throws IOException, NoSuchFieldException {
        Bank bank = factory.createBankFor(player);
        int villageId = player.getCitizenVillage().getId() + 1;
        bank.startMoving(villageId);

        validateVillageOptions(option -> {
            assertThat(player, receivedBMLContaining("currently moving to Village" + villageId));
            assertThat(player, didNotReceiveBMLContaining("help speed things up"));
            assertThat(player, didNotReceiveBMLContaining("Bribe"));
            return null;
        });
    }

    @Test
    public void testBankAlreadyMovingBribery() throws IOException, NoSuchFieldException {
        Bank bank = factory.createBankFor(player);
        int villageId = player.getCitizenVillage().getId() + 1;
        bank.startMoving(villageId);

        validateVillageOptions(option -> {
            assertThat(player, receivedBMLContaining("currently moving to Village" + villageId));
            assertThat(player, receivedBMLContaining("help speed things up"));
            assertThat(player, receivedBMLContaining("Bribe"));
            return null;
        }, true);
    }

    @Test
    public void testBankAlreadyMovingNoVillages() {
        Bank bank = factory.createBankFor(player);
        Village movingTo = factory.createVillageFor(factory.createNewPlayer());
        bank.startMoving(movingTo.id);
        player.getCitizenVillage().disband("upkeep");

        for (BankerMod.VillageOptions option : BankerMod.VillageOptions.values()) {
            System.out.println(option.name());
            configureVillageOption(option, false);
            assertThat(player, receivedBMLContaining("currently moving to Village" + movingTo.id));
            assertThat(player, receivedBMLContaining("Nowhere"));
            assertThat(player, didNotReceiveBMLContaining("Bribe"));

            configureVillageOption(option, true);
            assertThat(player, receivedBMLContaining("currently moving to Village" + movingTo.id));
            assertThat(player, receivedBMLContaining("Nowhere"));
            assertThat(player, didNotReceiveBMLContaining("id=\"move\""));
            assertThat(player, receivedBMLContaining("Bribe"));
        }
    }

    @Test
    public void testBankNotAlreadyMoving() throws IOException, NoSuchFieldException {
        Bank bank = factory.createBankFor(player);
        int villageId = player.getCitizenVillage().getId() + 1;
        bank.currentVillage = villageId;

        validateVillageOptions(option -> {
            assertThat(player, receivedBMLContaining("currently situated in Village" + villageId));
            assertThat(player, didNotReceiveBMLContaining("help speed things up"));
            assertThat(player, didNotReceiveBMLContaining("Bribe"));
            return null;
        });
    }

    @Test
    public void testBankNotAlreadyMovingBribery() throws IOException, NoSuchFieldException {
        Bank bank = factory.createBankFor(player);
        int villageId = player.getCitizenVillage().getId() + 1;
        bank.currentVillage = villageId;

        validateVillageOptions(option -> {
            assertThat(player, receivedBMLContaining("currently situated in Village" + villageId));
            assertThat(player, receivedBMLContaining("help speed things up"));
            assertThat(player, receivedBMLContaining("Bribe"));
            return null;
        }, true);
    }

    @Test
    public void testBankCurrentVillageNotOptionEvenIfPermanent() throws IOException, NoSuchFieldException, IllegalAccessException {
        Bank bank = factory.createBankFor(player);
        ReflectionUtil.setPrivateField(player.getCitizenVillage(), Village.class.getDeclaredField("isPermanent"), true);
        int villageId = player.getCitizenVillage().getId();
        bank.currentVillage = villageId;

        validateVillageOptions(option -> {
            assertFalse(getVillageNames().contains("Village" + villageId));
            assertThat(player, receivedBMLContaining("currently situated in Village" + villageId));
            assertThat(player, didNotReceiveBMLContaining("help speed things up"));
            assertThat(player, didNotReceiveBMLContaining("Bribe"));
            return null;
        });
    }

    @Test
    public void testBankNotAlreadyMovingNoVillages() {
        factory.createBankFor(player);
        int villageId = player.getCitizenVillage().getId();

        for (BankerMod.VillageOptions option : BankerMod.VillageOptions.values()) {
            configureVillageOption(option, false);
            System.out.println(option.name());
            assertThat(player, receivedBMLContaining("currently situated in Village" + villageId));
            assertThat(player, receivedBMLContaining("Nowhere"));
            assertThat(player, didNotReceiveBMLContaining("Bribe"));

            configureVillageOption(option, true);
            assertThat(player, receivedBMLContaining("currently situated in Village" + villageId));
            assertThat(player, receivedBMLContaining("Nowhere"));
            assertThat(player, didNotReceiveBMLContaining("Bribe"));
            assertThat(player, receivedBMLContaining("OK"));
        }
    }
}
