package mod.wurmunlimited.npcs.banker;

import com.wurmonline.server.Constants;
import com.wurmonline.server.behaviours.*;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.npcs.FaceSetter;
import mod.wurmunlimited.npcs.ModelSetter;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public abstract class BankerTest {
    protected BankerObjectsFactory factory;
    private static boolean init = false;
    protected static BankerActions actions;
    protected static BankerManageAccount manageAccount;
    protected static BankerWithdraw withdraw;
    protected static BankerMoveAccount move;
    protected static BankerHire hire;
    protected static BankerManage manage;
    protected static PlaceNpcMenu menu;
    protected Player player;
    protected Creature banker;

    protected interface Execute {
        void run(Connection db) throws SQLException;
    }

    protected static void execute(Execute execute) throws SQLException {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + Constants.dbHost + "/sqlite/" + "banker.db")) {
            db.prepareStatement("CREATE TABLE IF NOT EXISTS faces (" +
                                        "id INTEGER NOT NULL UNIQUE," +
                                        "face INTEGER NOT NULL" +
                                        ");").execute();
            execute.run(db);
        }
    }

    @BeforeEach
    protected void setUp() throws Exception {
        File file = new File("sqlite/banker.db");
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }

        factory = new BankerObjectsFactory();

        if (!init) {
            ActionEntryBuilder.init();
            actions = new BankerActions();
            manageAccount = new BankerManageAccount();
            withdraw = new BankerWithdraw();
            move = new BankerMoveAccount();
            hire = new BankerHire();
            manage = new BankerManage();
            new PlaceBankerAction();
            menu = PlaceNpcMenu.register();
            init = true;
        }

        ReflectionUtil.<List<FaceSetter>>getPrivateField(null, FaceSetter.class.getDeclaredField("faceSetters")).clear();
        ReflectionUtil.<List<ModelSetter>>getPrivateField(null, ModelSetter.class.getDeclaredField("modelSetters")).clear();

        BankerMod mod = new BankerMod();

        mod.faceSetter = new FaceSetter(BankerTemplate::is, "banker.db");
        mod.modelSetter = new ModelSetter(BankerTemplate::is, "banker.db");

        player = factory.createNewPlayer();
        banker = factory.createNewBanker();
        banker.setKingdomId(player.getKingdomId());
    }

    @BeforeAll
    static void reset() {
        Constants.dbHost = ".";
    }
}
