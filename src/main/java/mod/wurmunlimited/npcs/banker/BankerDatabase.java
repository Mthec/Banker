package mod.wurmunlimited.npcs.banker;

import com.wurmonline.server.Constants;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.zones.VolaTile;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class BankerDatabase {
    private static final Logger logger = Logger.getLogger(BankerDatabase.class.getName());
    private static String dbString = "";
    private static boolean created = false;
    private static final Map<Creature, Long> faces = new HashMap<>();
    private static Long tempFace = null;

    public static void resetPlayerFace(Player player) {
        player.getCurrentTile().setNewFace(player);
    }

    public static boolean isDifferentFace(long face, Creature banker) {
        Long currentFace = faces.get(banker);
        return currentFace == null || currentFace != face;
    }

    private interface Execute {
        void run(Connection db) throws SQLException;
    }

    private static void execute(Execute execute) throws SQLException {
        Connection db = null;
        try {
            if (dbString.isEmpty())
                dbString = "jdbc:sqlite:" + Constants.dbHost + "/sqlite/" + "banker.db";
            db = DriverManager.getConnection(dbString);
            if (!created) {
                init(db);
            }
            execute.run(db);
        } finally {
            try {
                if (db != null)
                    db.close();
            } catch (SQLException e1) {
                logger.warning("Could not close connection to database.");
                e1.printStackTrace();
            }
        }
    }

    private static void init(Connection db) throws SQLException {
        db.prepareStatement("CREATE TABLE IF NOT EXISTS faces (" +
                                                 "id INTEGER NOT NULL UNIQUE," +
                                                 "face INTEGER NOT NULL" +
                                                 ");").execute();

        created = true;
    }

    static void loadFaces(){
        faces.clear();

        try {
            execute(db -> {
                ResultSet rs = db.prepareStatement("SELECT * FROM faces;").executeQuery();

                while (rs.next()) {
                    try {
                        faces.put(Creatures.getInstance().getCreature(rs.getLong(1)),
                                rs.getLong(2));
                    } catch (NoSuchCreatureException e) {
                        logger.warning("Could not find creature with id (" + rs.getLong(1) + "), ignoring.");
                        e.printStackTrace();
                    }
                }
            });
        } catch (SQLException e) {
            logger.warning("Could not load faces from database.");
            e.printStackTrace();
        }
    }

    public static Long getFaceFor(Creature banker) {
        if (!BankerTemplate.is(banker)) {
            logger.warning("Face requested for " + banker.getName() + " who is not a banker.");
            return null;
        }

        if (tempFace != null) {
            return tempFace;
        }

        return faces.get(banker);
    }

    public static void setTempFace(long face) {
        tempFace = face;
    }

    public static void removeTempFace(long face) {
        if (tempFace == face) {
            tempFace = null;
        } else {
            logger.warning("Temp face was different.");
        }
    }

    public static void setFaceFor(Creature banker, long face) throws SQLException {
        execute(db -> {
            PreparedStatement ps = db.prepareStatement("INSERT OR REPLACE INTO faces (id, face) VALUES (?, ?);");
            ps.setLong(1, banker.getWurmId());
            ps.setLong(2, face);
            ps.execute();
        });

        faces.put(banker, face);

        VolaTile currentTile = banker.getCurrentTile();
        if (currentTile != null) {
            currentTile.setNewFace(banker);
        }
    }

    public static void deleteFaceFor(Creature banker) {
        Long face = faces.remove(banker);

        try {
            if (face != null) {
                execute(db -> {
                    PreparedStatement ps = db.prepareStatement("DELETE FROM faces WHERE id=?;");
                    ps.setLong(1, banker.getWurmId());
                    ps.execute();
                });
            }
        } catch (SQLException e) {
            logger.warning("Failed to delete banker from database.");
            e.printStackTrace();
        }
    }
}
