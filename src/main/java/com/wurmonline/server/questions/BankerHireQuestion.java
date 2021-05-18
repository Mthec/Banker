package com.wurmonline.server.questions;

import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.behaviours.Methods;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.banker.BankerDatabase;
import mod.wurmunlimited.npcs.banker.BankerMod;
import mod.wurmunlimited.npcs.banker.BankerTemplate;
import mod.wurmunlimited.npcs.banker.FaceSetters;
import org.jetbrains.annotations.Nullable;

import java.util.Properties;
import java.util.Random;

public class BankerHireQuestion extends BankerQuestionExtension {
    private static final Random r = new Random();
    private final Player responder;
    private final VolaTile tile;
    private final int floorLevel;
    static final int faceMaxChars = Long.toString(Long.MAX_VALUE).length();
    private final Item contract;

    public BankerHireQuestion(Player responder, VolaTile tile, int floorLevel, @Nullable Item contract, @Nullable Properties answers) {
        super(responder, "Hire Banker", "", QuestionTypes.MANAGETRADER, -10);
        this.responder = responder;
        this.tile = tile;
        this.floorLevel = floorLevel;
        this.contract = contract;
        //noinspection ConstantConditions
        setAnswer(answers);
    }

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);

        String faceString = getStringProp("face");
        long face;
        boolean faceWasRandom = true;
        if (faceString.isEmpty()) {
            face = r.nextLong();
        } else {
            try {
                face = Long.parseLong(faceString);
                faceWasRandom = false;
            } catch (NumberFormatException e) {
                responder.getCommunicator().sendAlertServerMessage("Invalid face value, setting random.");
                face = r.nextLong();
            }
        }

        byte sex = 0;
        if (wasAnswered("gender", "female"))
            sex = 1;

        String name = StringUtilities.raiseFirstLetter(getStringProp("name"));
        if (name.isEmpty() || name.length() > 20 || QuestionParser.containsIllegalCharacters(name)) {
            if (sex == 0) {
                name = QuestionParser.generateGuardMaleName();
                responder.getCommunicator().sendSafeServerMessage("The banker didn't like the name, so he chose a new one.");
            } else {
                name = QuestionParser.generateGuardFemaleName();
                responder.getCommunicator().sendSafeServerMessage("The banker didn't like the name, so she chose a new one.");
            }
        }

        if (locationIsValid(responder)) {
            try {
                String fullName;
                String prefix = BankerMod.getNamePrefix();
                if (prefix.isEmpty())
                    fullName = name;
                else
                    fullName = prefix + "_" + name;
                Creature banker = BankerTemplate.createCreature(tile, floorLevel, fullName, sex, responder.getKingdomId(), face);
                logger.info(responder.getName() + " created a banker: " + banker.getWurmId());

                if (contract != null) {
                    contract.setName("banker writ", true);
                    contract.setData(banker.getWurmId());
                }

                if (faceWasRandom) {
                    responder.getCommunicator().sendCustomizeFace(face, BankerMod.faceSetters.createIdFor(banker, responder));
                } else {
                    BankerDatabase.setFaceFor(banker, face);
                }
            } catch (FaceSetters.TooManyTransactionsException e) {
                logger.warning(e.getMessage());
                responder.getCommunicator().sendAlertServerMessage(e.getMessage());
            } catch (Exception e) {
                responder.getCommunicator().sendAlertServerMessage("An error occurred in the rifts of the void. The banker was not created.");
                e.printStackTrace();
            }
        }
    }

    private boolean locationIsValid(Creature responder) {
        if (tile != null) {
            if (!Methods.isActionAllowed(responder, Actions.MANAGE_TRADERS)) {
                return false;
            }
            for (Creature creature : tile.getCreatures()) {
                if (!creature.isPlayer()) {
                    responder.getCommunicator().sendNormalServerMessage("The banker will only set up where no other creatures except you are standing.");
                    return false;
                }
            }

            Structure struct = tile.getStructure();
            if (struct != null && !struct.mayPlaceMerchants(responder)) {
                responder.getCommunicator().sendNormalServerMessage("You do not have permission to place a banker in this building.");
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public void sendQuestion() {
        boolean gender;
        Properties answers = getAnswer();
        if (answers != null)
            gender = wasAnswered("gender", "male");
        else
            gender = r.nextBoolean();

        String bml = new BMLBuilder(id)
                             .text("Hire Banker").bold()
                             .text("Place a banker that will offer banking services.")
                             .newLine()
                             .harray(b -> b.label("Name:").entry("name", (answers == null ? "" : (String)answers.getOrDefault("name", "")), 20))
                             .text("Leave blank for a random name.").italic()
                             .newLine()
                             .harray(b -> b.label("Face:").entry("face", "", faceMaxChars))
                             .text("Leave blank to create a face on the next screen, or paste a face code here.").italic()
                             .newLine()
                             .text("Gender:")
                             .radio("gender", "male", "Male", gender)
                             .radio("gender", "female", "Female", !gender)
                             .newLine()
                             .harray(b -> b.button("Send"))
                             .build();

        getResponder().getCommunicator().sendBml(400, 350, true, true, bml, 200, 200, 200, title);
    }
}
