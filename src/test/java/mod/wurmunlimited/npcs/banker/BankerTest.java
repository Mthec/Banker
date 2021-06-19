package mod.wurmunlimited.npcs.banker;

import com.wurmonline.server.Constants;
import com.wurmonline.server.behaviours.*;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class BankerTest {
    protected BankerObjectsFactory factory;
    private static boolean init = false;
    protected static BankerActions actions;
    protected static BankerManageAccount manageAccount;
    protected static BankerWithdraw withdraw;
    protected static BankerMoveAccount move;
    protected static BankerHire hire;
    protected static BankerManage manage;
    protected static BankerChangeFace changeFace;
    protected static BankerGive give;
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
            changeFace = new BankerChangeFace();
            give = new BankerGive();
            new PlaceBankerAction();
            menu = PlaceNpcMenu.register();
            init = true;
        }

        player = factory.createNewPlayer();
        banker = factory.createNewBanker();
        banker.setKingdomId(player.getKingdomId());
    }

    @BeforeAll
    static void reset() {
        Constants.dbHost = ".";
    }
}
