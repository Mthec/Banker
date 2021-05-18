package mod.wurmunlimited.npcs.banker;

import com.wurmonline.server.Items;
import com.wurmonline.server.Server;
import com.wurmonline.server.WurmId;
import com.wurmonline.server.behaviours.*;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.BankerHireQuestion;
import com.wurmonline.server.questions.CreatureCreationQuestion;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.questions.WithdrawMoneyQuestion;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import com.wurmonline.shared.constants.IconConstants;
import com.wurmonline.shared.constants.ItemMaterials;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;
import org.gotti.wurmunlimited.modsupport.creatures.ModCreatures;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BankerMod implements WurmServerMod, Configurable, Initable, PreInitable, ItemTemplatesCreatedListener, ServerStartedListener {
    private static final Logger logger = Logger.getLogger(BankerMod.class.getName());
    public static final FaceSetters faceSetters = new FaceSetters();
    public static final Set<Integer> withdrawals = new HashSet<>();
    private boolean updateTraders = false;
    private boolean contractsOnTraders = false;
    private static VillageOptions villageOption = VillageOptions.VILLAGE;
    private static boolean allowGuestAccounts = false;
    private static long briberyCost = 0;
    private static int contractTemplateId;
    private int contractPrice = MonetaryConstants.COIN_SILVER;
    private static String namePrefix = "Banker";

    public enum VillageOptions {
        STARTER, KINGDOM, ALLIANCE, VILLAGE
    }

    public static boolean isWrit(Item item) {
        return item.getTemplateId() == contractTemplateId;
    }

    public static int getContractTemplateId() {
        return contractTemplateId;
    }

    public static VillageOptions getVillageOptions() {
        return villageOption;
    }

    public static boolean allowGuestAccounts() {
        return allowGuestAccounts;
    }

    public static long getBriberyCost() {
        return briberyCost;
    }

    public static String getNamePrefix() {
        return namePrefix;
    }

    @Override
    public void configure(Properties properties) {
        updateTraders = properties.getOrDefault("update_traders", "false").equals("true");
        contractsOnTraders = properties.getOrDefault("contracts_on_traders", "false").equals("true");
        switch ((String)properties.getOrDefault("village_option", "village")) {
            case "starter":
                villageOption = VillageOptions.STARTER;
                break;
            case "kingdom":
                villageOption = VillageOptions.KINGDOM;
                break;
            case "alliance":
                villageOption = VillageOptions.ALLIANCE;
                break;
            default:
            case "village":
                villageOption = VillageOptions.VILLAGE;
                break;
        }
        allowGuestAccounts = properties.getOrDefault("allow_non_premium", "false").equals("true");
        try {
            String briberyString = properties.getProperty("bribery_cost");
            if (briberyString != null && !briberyString.isEmpty()) {
                briberyCost = Long.parseLong(briberyString);
                if (briberyCost < 0)
                    throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid value for bribery_cost, setting default (0).");
            briberyCost = 0;
        }
        namePrefix = properties.getProperty("name_prefix");
        if (namePrefix == null)
            namePrefix = "Banker";
        try {
            String contractPriceString = properties.getProperty("contract_price");
            if (contractPriceString != null && !contractPriceString.isEmpty()) {
                contractPrice = Integer.parseInt(contractPriceString);
                if (contractPrice <= 0)
                    throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid value for contract_price, setting default (1s).");
            contractPrice = MonetaryConstants.COIN_SILVER;
        }
    }

    @Override
    public void onItemTemplatesCreated() {
        try {
            contractTemplateId = new ItemTemplateBuilder("writ.banker")
                                         .name("banker contract", "banker contracts", "A contract to hire a banker, who will provide banking services.")
                                         .modelName("model.writ.merchant")
                                         .imageNumber((short)IconConstants.ICON_TRADER_CONTRACT)
                                         .weightGrams(0)
                                         .dimensions(1, 10, 10)
                                         .decayTime(Long.MAX_VALUE)
                                         .material(ItemMaterials.MATERIAL_PAPER)
                                         .itemTypes(new short[] {
                                                 ItemTypes.ITEM_TYPE_INDESTRUCTIBLE,
                                                 ItemTypes.ITEM_TYPE_NODROP,
                                                 ItemTypes.ITEM_TYPE_HASDATA,
                                                 ItemTypes.ITEM_TYPE_FULLPRICE,
                                                 ItemTypes.ITEM_TYPE_LOADED,
                                                 ItemTypes.ITEM_TYPE_NOT_MISSION,
                                                 ItemTypes.ITEM_TYPE_NOSELLBACK
                                         })
                                         .behaviourType(BehaviourList.itemBehaviour)
                                         .value(contractPrice)
                                         .difficulty(100.0F)
                                         .build().getTemplateId();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preInit() {
        ModActions.init();
    }

    @Override
    public void init() {
        HookManager manager = HookManager.getInstance();

        manager.registerHook("com.wurmonline.server.creatures.Creature",
                "getFace",
                "()J",
                () -> this::getFace);

        manager.registerHook("com.wurmonline.server.creatures.Creature",
                "getBlood",
                "()B",
                () -> this::getBlood);

        manager.registerHook("com.wurmonline.server.creatures.Communicator",
                "sendNewCreature",
                "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;FFFJFBZZZBJBZZB)V",
                () -> this::sendNewCreature);

        manager.registerHook("com.wurmonline.server.creatures.Creature",
                "destroy",
                "()V",
                () -> this::destroy);

        manager.registerHook("com.wurmonline.server.questions.QuestionParser",
                "parseCreatureCreationQuestion",
                "(Lcom/wurmonline/server/questions/CreatureCreationQuestion;)V",
                () -> this::creatureCreation);
        
        manager.registerHook("com.wurmonline.server.creatures.Communicator",
                "reallyHandle_CMD_NEW_FACE",
                "(Ljava/nio/ByteBuffer;)V",
                () -> this::setFace);
        
        manager.registerHook("com.wurmonline.server.questions.QuestionParser",
                "parseWithdrawMoneyQuestion",
                "(Lcom/wurmonline/server/questions/WithdrawMoneyQuestion;)V",
                () -> this::withdrawMoney);

        ModCreatures.init();
        ModCreatures.addCreature(new BankerTemplate());
    }

    @Override
    public void onServerStarted() {
        ModActions.registerAction(new BankerActions());
        ModActions.registerAction(new BankerManageAccount());
        ModActions.registerAction(new BankerWithdraw());
        ModActions.registerAction(new BankerMoveAccount());
        ModActions.registerAction(new BankerHire());
        ModActions.registerAction(new BankerManage());
        ModActions.registerAction(new BankerChangeFace());
        ModActions.registerAction(new BankerGive());

        BankerDatabase.loadFaces();
        logger.info("Banker mod settings - update_traders " + updateTraders + ", " +
                            "contracts_on_traders " + contractsOnTraders + ", " +
                            "village_option " + villageOption.name().toLowerCase() + ", " +
                            "allow_guest_accounts " + allowGuestAccounts + ", " +
                            "bribery_cost " + (briberyCost > 0 ? " (irons) " + briberyCost : "disabled") + ", " +
                            "contract_price (irons)" + contractPrice + ", " +
                            "name_prefix " + namePrefix + ".");

        if (updateTraders) {
            if (contractsOnTraders) {
                for (Shop shop : Economy.getTraders()) {
                    Creature creature = Creatures.getInstance().getCreatureOrNull(shop.getWurmId());
                    if (!shop.isPersonal() && creature != null && creature.isSalesman() && creature.getInventory().getItems().stream().noneMatch(i -> i.getTemplateId() == contractTemplateId)) {
                        try {
                            creature.getInventory().insertItem(Creature.createItem(contractTemplateId, (float) (10 + Server.rand.nextInt(80))));
                            shop.setMerchantData(shop.getNumberOfItems() + 1);
                        } catch (Exception e) {
                            logger.log(Level.INFO, "Failed to create trader inventory items for shop, creature: " + creature.getName(), e);
                        }
                    }
                }
            } else {
                for (Shop shop : Economy.getTraders()) {
                    Creature creature = Creatures.getInstance().getCreatureOrNull(shop.getWurmId());
                    if (!shop.isPersonal() && creature != null) {
                        creature.getInventory().getItems().stream().filter(i -> i.getTemplateId() == contractTemplateId).collect(Collectors.toList()).forEach(item -> {
                            Items.destroyItem(item.getWurmId());
                            shop.setMerchantData(shop.getNumberOfItems() - 1);
                        });
                    }
                }
            }
        }
    }

    Object getFace(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Creature creature = (Creature)o;
        if (BankerTemplate.is(creature)) {
            Long newFace = BankerDatabase.getFaceFor(creature);
            if (newFace == null) {
                newFace = new Random(creature.getWurmId()).nextLong();
                logger.warning("getFaceFor returned null.");
            }
            return newFace;
        }
        return method.invoke(o, args);
    }

    // Required for not having weird character model.
    Object getBlood(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Creature creature = (Creature)o;
        if (BankerTemplate.is(creature)) {
            return (byte)-1;
        }
        return method.invoke(o, args);
    }

    Object sendNewCreature(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        // If id is Creature id.
        if (WurmId.getType((Long)args[0]) == 1) {
            if ((Byte)args[15] == (byte)-1) {
                // Blood - Should only apply to players, so re-purposing it for this should be okay.
                args[15] = (byte)0;
                // isCopy
                args[17] = true;
            }
        }
        return method.invoke(o, args);
    }

    Object destroy(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Creature creature = (Creature)o;
        if (BankerTemplate.is(creature)) {
            BankerDatabase.deleteFaceFor(creature);
        }

        return method.invoke(o, args);
    }

    Object creatureCreation(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        CreatureCreationQuestion question = (CreatureCreationQuestion)args[0];
        Properties answers = ReflectionUtil.getPrivateField(question, Question.class.getDeclaredField("answer"));
        try {
            String templateIndexString = answers.getProperty("data1");
            String name = answers.getProperty("cname");
            if (name == null)
                answers.setProperty("name", "");
            else
                answers.setProperty("name", name);
            if (templateIndexString != null) {
                int templateIndex = Integer.parseInt(templateIndexString);
                List<CreatureTemplate> templates = ReflectionUtil.getPrivateField(question, CreatureCreationQuestion.class.getDeclaredField("cretemplates"));
                CreatureTemplate template = templates.get(templateIndex);

                Creature responder = question.getResponder();
                int floorLevel = responder.getFloorLevel();
                VolaTile tile = Zones.getOrCreateTile(question.getTileX(), question.getTileY(), responder.isOnSurface());
                if (BankerTemplate.is(template)) {
                    new BankerHireQuestion((Player)responder, tile, floorLevel, null, answers).sendQuestion();
                    return null;
                }
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            question.getResponder().getCommunicator().sendAlertServerMessage("An error occurred in the rifts of the void. The banker was not created.");
            e.printStackTrace();
        }

        return method.invoke(o, args);
    }

    Object setFace(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        ByteBuffer buf = (ByteBuffer)args[0];
        buf.mark();
        long face = buf.getLong();
        long itemId = buf.getLong();

        if (itemId < 0) {
            Player player = ((Communicator)o).player;
            if (player != null) {
                Creature banker = faceSetters.retrieveBankerOrNull(player, itemId);

                if (banker != null && BankerDatabase.isDifferentFace(face, banker)) {
                    try {
                        BankerDatabase.setFaceFor(banker, face);
                        BankerDatabase.resetPlayerFace(player);
                        player.getCommunicator().sendNormalServerMessage("The banker's face seems to shift about and takes a new form.");
                    } catch (SQLException e) {
                        logger.warning("Failed to set " + banker.getName() + "'s face.");
                        e.printStackTrace();
                        player.getCommunicator().sendNormalServerMessage("The banker's face seems to shift about, but then returns as it was.");
                    }
                } else {
                    player.getCommunicator().sendNormalServerMessage("The banker's face seems to shift about, but then returns as it was.");
                }

                return null;
            } else {
                logger.warning("Why is player null?  This should never happen.");
            }
        }

        buf.reset();
        return method.invoke(o, args);
    }

    Object withdrawMoney(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        WithdrawMoneyQuestion question = (WithdrawMoneyQuestion)args[0];
        
        if (withdrawals.remove(question.getId())) {
            Creature responder = question.getResponder();

            if (responder.isDead()) {
                responder.getCommunicator().sendNormalServerMessage("You are dead, and may not withdraw any money.");
                return null;
            }
            
            final long money = responder.getMoney();
            if (money > 0L) {
                final long valueWithdrawn = getValueWithdrawn(responder, ReflectionUtil.getPrivateField(question, Question.class.getDeclaredField("answer")));
                if (valueWithdrawn > 0L) {
                    try {
                        if (responder.chargeMoney(valueWithdrawn)) {
                            final Item[] coins = Economy.getEconomy().getCoinsFor(valueWithdrawn);
                            final Item inventory = responder.getInventory();
                            for (Item coin : coins) {
                                inventory.insertItem(coin);
                            }
                            final Change withdrawn = Economy.getEconomy().getChangeFor(valueWithdrawn);
                            responder.getCommunicator().sendNormalServerMessage("You withdraw " + withdrawn.getChangeString() + " from the bank.");
                            final Change c = new Change(money - valueWithdrawn);
                            responder.getCommunicator().sendNormalServerMessage("New balance: " + c.getChangeString() + ".");
                            logger.info(responder.getName() + " withdraws " + withdrawn.getChangeString() + " from the bank and should have " + c.getChangeString() + " now.");
                        } else {
                            responder.getCommunicator().sendNormalServerMessage("You can not withdraw that amount of money at the moment.");
                        }
                    } catch (IOException iox) {
                        logger.log(Level.WARNING, "Failed to withdraw money from " + responder.getName() + ":" + iox.getMessage(), iox);
                        responder.getCommunicator().sendNormalServerMessage("The transaction failed. Please contact the game masters using the <i>/dev</i> command.");
                    }
                } else {
                    responder.getCommunicator().sendNormalServerMessage("No money withdrawn.");
                }
            } else {
                responder.getCommunicator().sendNormalServerMessage("You have no money in the bank.");
            }

            return null;
        } else {
            return method.invoke(o, args);
        }
    }
    
    long getValueWithdrawn(Creature responder, Properties answers) {
        final Communicator comm = responder.getCommunicator();
        try {
            return (MonetaryConstants.COIN_GOLD * getWanted(answers, comm, "gold"))
                           + (MonetaryConstants.COIN_SILVER * getWanted(answers, comm, "silver"))
                           + (MonetaryConstants.COIN_COPPER * getWanted(answers, comm, "copper"))
                           + getWanted(answers, comm, "iron");
        }
        catch (NumberFormatException nfe) {
            comm.sendNormalServerMessage("The values were incorrect.");
            return 0L;
        }
    }

    private long getWanted(Properties answers, Communicator comm, String name) throws NumberFormatException {
        String str = answers.getProperty(name);
        if (str != null && str.length() > 0) {
            long coins = Long.parseLong(str);
            if (coins < 0) {
                comm.sendNormalServerMessage("You may not withdraw a negative amount of " + name + " coins!");
                return 0;
            }
            return coins;
        }

        return 0;
    }
}
