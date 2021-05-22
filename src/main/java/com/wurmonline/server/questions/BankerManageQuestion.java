package com.wurmonline.server.questions;

import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.npcs.banker.BankerDatabase;
import mod.wurmunlimited.npcs.banker.BankerMod;
import mod.wurmunlimited.npcs.banker.FaceSetters;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import static com.wurmonline.server.creatures.CreaturePackageCaller.saveCreatureName;

public class BankerManageQuestion extends BankerQuestionExtension {
    private final Player responder;
    private final Creature banker;

    public BankerManageQuestion(Player responder, Creature banker) {
        super(responder, "Manage banker", "", MANAGETRADER, banker.getWurmId());
        this.responder = responder;
        this.banker = banker;
    }

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);

        if (banker.isDead()) {
            responder.getCommunicator().sendNormalServerMessage("Unfortunately the banker disappeared before they could listen to your response.");
            return;
        }

        if (wasSelected("dismiss")) {
            Server.getInstance().broadCastAction(banker.getName() + " grunts, packs " + banker.getHisHerItsString() + " things and is off.", banker, 5);
            responder.getCommunicator().sendNormalServerMessage("You dismiss " + banker.getName() + " from " + banker.getHisHerItsString() + " post.");
            logger.info(responder.getName() + " dismisses banker " + banker.getName() + " with WurmID: " + target);

            for (Item item : responder.getInventory().getAllItems(true)) {
                if (item.getTemplateId() == BankerMod.getContractTemplateId() && item.getData() == target) {
                    item.setName("banker contract", true);
                    item.setData(-1);
                    break;
                }
            }

            banker.destroy();
            return;
        }

        String name = getStringProp("name");
        if (name != null && !name.isEmpty()) {
            String fullName = getPrefix() + StringUtilities.raiseFirstLetter(name);
            if (QuestionParser.containsIllegalCharacters(name)) {
                responder.getCommunicator().sendNormalServerMessage("The banker didn't like that name, so they shall remain " + banker.getName() + ".");
            } else if (!fullName.equals(banker.getName())) {
                try {
                    saveCreatureName(banker, fullName);
                    banker.refreshVisible();
                    responder.getCommunicator().sendNormalServerMessage("The banker will now be known as " + banker.getName() + ".");
                } catch (IOException e) {
                    logger.warning("Failed to set name (" + fullName + ") for creature (" + banker.getWurmId() + ").");
                    responder.getCommunicator().sendNormalServerMessage("The banker looks confused, what exactly is a database?");
                    e.printStackTrace();
                }
            }
        }

        String faceString = getStringProp("face");
        long face;
        if (faceString.isEmpty()) {
            face = banker.getFace();
        } else {
            try {
                face = Long.parseLong(faceString);
            } catch (NumberFormatException e) {
                responder.getCommunicator().sendAlertServerMessage("Invalid face value, ignoring.");
                face = banker.getFace();
            }
        }

        if (faceString.isEmpty()) {
            try {
                responder.getCommunicator().sendCustomizeFace(face, BankerMod.faceSetters.createIdFor(banker, responder));
            } catch (FaceSetters.TooManyTransactionsException e) {
                logger.warning(e.getMessage());
                responder.getCommunicator().sendAlertServerMessage(e.getMessage());
            }
        } else if (face != banker.getFace()) {
            try {
                BankerDatabase.setFaceFor(banker, face);
                responder.getCommunicator().sendNormalServerMessage("The banker's face seems to shift about and takes a new form.");
            } catch (SQLException e) {
                logger.warning("Failed to set face (" + face + ") for banker (" + banker.getWurmId() + ").");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendQuestion() {
        String bml = new BMLBuilder(id)
                             .text("Manage Banker").bold()
                             .harray(b -> b.label("Name: " + getPrefix()).entry("name", getNameWithoutPrefix(banker.getName()), BankerMod.maxNameLength))
                             .newLine()
                             .harray(b -> b.label("Face:").entry("face", Long.toString(banker.getFace()), BankerHireQuestion.faceMaxChars))
                             .text("Blank to create a face on the next screen, or paste a face code here.").italic()
                             .newLine()
                             .harray(b -> b.button("Send").spacer().button("dismiss", "Dismiss").confirm("Dismiss Banker", "Are you sure you want to dismiss " + banker.getName() + "?"))
                             .build();

        getResponder().getCommunicator().sendBml(400, 350, true, true, bml, 200, 200, 200, title);
    }
}
