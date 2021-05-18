package com.wurmonline.server.questions;

import com.wurmonline.server.Server;
import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.banks.Bank;
import com.wurmonline.server.banks.BankUnavailableException;
import com.wurmonline.server.banks.Banks;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.villages.NoSuchVillageException;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.Villages;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.banker.BankerMod;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BankerManageAccountQuestion extends BankerQuestionExtension {
    private final List<Village> villageOptions = new ArrayList<>();

    private static class ChosenVillageNullOrMissing extends Exception {
        ChosenVillageNullOrMissing() {
            super("Chosen village was null or missing.");
        }
    }

    public BankerManageAccountQuestion(Creature responder, Creature banker) {
        super(responder, "Bank management", "", QuestionTypes.BANK_MANAGEMENT, banker.getWurmId());
    }

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);

        if (wasSelected("cancel")) {
            return;
        }

        Creature responder = getResponder();

        if (BankerMod.allowGuestAccounts() || !responder.isGuest()) {
            if (villageOptions.isEmpty()) {
                responder.getCommunicator().sendNormalServerMessage("Your bank will stay where it is.");
                return;
            }

            Village village;
            String villageIndex = answers.getProperty("village");

            try {
                if (villageIndex == null || villageIndex.isEmpty())
                    throw new ChosenVillageNullOrMissing();
                int index = Integer.parseInt(villageIndex);
                village = villageOptions.get(index);
            } catch (IndexOutOfBoundsException | NumberFormatException | ChosenVillageNullOrMissing e) {
                logger.warning("Chosen village not found (" + villageIndex + ").");
                e.printStackTrace();
                responder.getCommunicator().sendNormalServerMessage("Your chosen village was not found.");
                return;
            }

            if (wasSelected("open")) {
                ((Player)responder).startBank(village);
            } else if (wasSelected("move") || wasSelected("bribe")) {
                Bank bank = Banks.getBank(responder.getWurmId());

                if (bank != null) {
                    if (!bank.open) {
                        if (bank.targetVillage != village.getId()) {
                            boolean disbanded = false;
                            if (bank.targetVillage > 0) {
                                try {
                                    Villages.getVillage(bank.targetVillage);
                                } catch (NoSuchVillageException e) {
                                    bank.stopMoving();
                                }
                            } else {
                                try {
                                    bank.getCurrentVillage();
                                } catch (BankUnavailableException e) {
                                    disbanded = true;
                                }
                            }

                            bank.startMoving(village.getId());
                            long briberyCost = BankerMod.getBriberyCost();
                            if (responder.getPower() > 0) {
                                bank.stopMoving();
                                responder.getCommunicator().sendNormalServerMessage("The bank account has moved there because you're a cool person with some extra powers.");
                            } else if (disbanded) {
                                bank.stopMoving();
                                responder.getCommunicator().sendNormalServerMessage("The bank account has moved.");
                            } else if (briberyCost > 0 && wasSelected("bribe")) {
                                try {
                                    if (responder.chargeMoney(briberyCost)) {
                                        bank.stopMoving();
                                        responder.getCommunicator().sendNormalServerMessage("The bank account has been moved.  Thank you.");
                                    } else {
                                        responder.getCommunicator().sendNormalServerMessage("I did not receive the money, moving will take 24 hours.");
                                    }
                                } catch (IOException e) {
                                    logger.warning("An exception occurred when attempting to charge money.");
                                    e.printStackTrace();
                                    responder.getCommunicator().sendNormalServerMessage("I did not receive the money, moving will take 24 hours.");
                                }
                            } else {
                                responder.getCommunicator().sendNormalServerMessage("You bank has started moving, it will take 24 hours.");
                            }
                        } else {
                            responder.getCommunicator().sendNormalServerMessage("Your bank is already moving there.");
                        }
                    } else {
                        responder.getCommunicator().sendNormalServerMessage("The bank account is open. You cannot manage it now.");
                    }
                }
            }
        } else {
            responder.getCommunicator().sendNormalServerMessage("Guests may not open bank accounts.");
        }
    }

    @Override
    public void sendQuestion() {
        Creature responder = getResponder();
        if (!BankerMod.allowGuestAccounts() && responder.isGuest()) {
            BML bml = new BMLBuilder(id)
                    .text("Guests may not open bank accounts on this server.")
                    .newLine()
                    .harray(b -> b.button("cancel", "OK"));
            responder.getCommunicator().sendBml(300, 300, true, true, bml.build(), 200, 200, 200, title);
            return;
        }
        Bank bank = Banks.getBank(responder.getWurmId());

        BML bml = new BMLBuilder(id);
        if (bank == null) {
            bml = bml.text("You may open a bank account here.")
                     .text("You can only have one bank account, but you may move it.")
                     .text("A bank account will currently not move items between servers, but your money will be available if you open a new bank account on another server.")
                     .text("It will take 24 hours to move it to another settlement.")
                     .text("Note that some items decay and may disappear inside the bank account, although slower than outside.")
                     .text("It will however be possible to rent a stasis spell to be cast upon the item in the future that will prevent decay.")
                     .newLine();
            bml = addVillageOptions(bml, responder)
                     .newLine()
                     .harray(b -> b.button("open", "Open account").spacer().button("cancel", "Cancel"));

        } else {
            long briberyCost = BankerMod.getBriberyCost();
            Village village = null;
            try {
                village = Villages.getVillage(bank.targetVillage);
            } catch (NoSuchVillageException ignored) {}
            bank.poll(System.currentTimeMillis());
            if (village != null && bank.startedMoving > 0) {
                bml = bml.text("Your bank is currently moving to " + village.getName() + ".")
                              .text("It will arrive in approximately " + Server.getTimeFor(bank.startedMoving + TimeConstants.DAY_MILLIS - System.currentTimeMillis()) + ".")
                              .text("You could change where your account will move to, but it will take another 24 hours.")
                              .If(briberyCost > 0, b -> b.text("However, I could help speed things up, for a price (" + new Change(briberyCost).getChangeShortString() + ")."))
                              .newLine();
                bml = addVillageOptions(bml, responder)
                              .newLine();
                bml = addAppropriateButtons(bml, briberyCost, true);
            } else {
                try {
                    final Village currentVillage = bank.getCurrentVillage();
                    bml = bml.text("Your bank is currently situated in " + currentVillage.getName() + ".")
                             .text("It will take 24 hours to move your bank account.")
                             .If(briberyCost > 0, b -> b.text("However, I could help speed things up, for a price (" + new Change(briberyCost).getChangeShortString() + ")."));
                    bml = addVillageOptions(bml, responder)
                             .newLine();
                    bml = addAppropriateButtons(bml, briberyCost, false);
                } catch (BankUnavailableException bu) {
                    bml = bml.text("Your bank is not currently located in a village as its previous location has been disbanded.");
                    bml = addVillageOptions(bml, responder)
                             .newLine();
                    bml = addAppropriateButtons(bml, 0, false);
                }
            }
        }

        responder.getCommunicator().sendBml(300, 300, true, true, bml.build(), 200, 200, 200, title);
    }

    private BML addVillageOptions(BML bml, Creature responder) {
        byte kingdom = responder.getKingdomId();
        Bank bank = Banks.getBank(responder.getWurmId());
        int excludeVillageId = bank != null ? (bank.targetVillage > 0 ? bank.targetVillage : bank.currentVillage) : -10;
        Collections.addAll(villageOptions, Villages.getPermanentVillagesForKingdom(kingdom));
        if (bank != null) {
            try {
                villageOptions.remove(bank.getCurrentVillage());
            } catch (BankUnavailableException ignored) {}
        }
        Stream<Village> options = Arrays.stream(Villages.getVillages())
                                          .filter(v -> !v.isPermanent)
                                          .filter(v -> excludeVillageId != v.id);

        switch (BankerMod.getVillageOptions()) {
            case KINGDOM:
                options = options.filter(v -> v.kingdom == kingdom);
                break;
            case ALLIANCE:
                options = options.filter(v -> v.isAlly(responder));
                break;
            case VILLAGE:
                Village village = responder.getCitizenVillage();
                if (village == null || village.id == excludeVillageId)
                    options = Stream.empty();
                else
                    options = Stream.of(village);
                break;
            case STARTER:
                options = Stream.empty();
                break;
        }

        options.sorted(Comparator.comparing(Village::getName))
               .forEach(villageOptions::add);

        if (villageOptions.isEmpty()) {
            return bml.text("Nowhere will allow you to open a bank account.");
        }

        return bml.text("Select where you would like your bank account to be located:")
                .dropdown("village", villageOptions.stream().map(Village::getName).collect(Collectors.toList()));
    }

    private BML addAppropriateButtons(BML bml, long briberyCost, boolean moving) {
        if (villageOptions == null || villageOptions.isEmpty()) {
            if (moving && briberyCost > 0) {
                return bml.harray(b -> b.button("bribe", "Bribe").spacer()
                                           .button("cancel", "Cancel"));
            } else {
                return bml.harray(b -> b.button("cancel", "OK"));
            }
        }

        if (moving) {
            return bml.harray(b -> b.button("move", "New location").spacer()
                                       .If(briberyCost > 0, b2 -> b2
                                       .button("bribe", "Bribe").spacer())
                                       .button("cancel", "Same location"));
        }

        return bml.harray(b -> b.button("move", "Move").spacer()
                                   .If(briberyCost > 0, b2 -> b2
                                   .button("bribe", "Bribe").spacer())
                                   .button("cancel", "Cancel"));
    }
}
