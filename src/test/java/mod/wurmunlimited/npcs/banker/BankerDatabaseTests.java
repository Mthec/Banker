package mod.wurmunlimited.npcs.banker;

import com.wurmonline.server.creatures.Creature;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BankerDatabaseTests extends BankerTest {
    @Test
    public void testIsDifferentFace() throws SQLException {
        long face = 12345;
        BankerDatabase.setFaceFor(banker, face);

        assertFalse(BankerDatabase.isDifferentFace(face, banker));
        assertTrue(BankerDatabase.isDifferentFace(face + 1, banker));

        BankerDatabase.deleteFaceFor(banker);
        assert BankerDatabase.getFaceFor(banker) == null;
        assertDoesNotThrow(() -> assertTrue(BankerDatabase.isDifferentFace(face, banker)));
    }

    @Test
    public void testLoadFaces() throws SQLException {
        Map<Creature, Long> faces = new HashMap<>();

        execute(db -> {
            for (long i = 0; i < 5; i++) {
                Creature banker = factory.createNewBankerSkipSetup();
                faces.put(banker, i);
                PreparedStatement ps = db.prepareStatement("INSERT INTO faces (id, face) VALUES (?, ?);");
                ps.setLong(1, banker.getWurmId());
                ps.setLong(2, i);
                ps.execute();
            }
        });

        BankerDatabase.loadFaces();

        faces.forEach((k, v) -> assertEquals(v, BankerDatabase.getFaceFor(k)));
    }

    @Test
    public void testGetFaceForNotBanker() {
        assertNull(BankerDatabase.getFaceFor(factory.createNewCreature()));
    }

    @Test
    public void testSetTempFace() {
        long face = 12345;
        BankerDatabase.setTempFace(face);

        for (int i = 0; i < 10; i++) {
            assertEquals(face, BankerDatabase.getFaceFor(factory.createNewBankerSkipSetup()));
        }

        BankerDatabase.removeTempFace(face);
        assertNull(BankerDatabase.getFaceFor(factory.createNewBankerSkipSetup()));
    }

    @Test
    public void testSetFaceFor() throws SQLException {
        long face = 123456;

        BankerDatabase.setFaceFor(banker, face);

        assertEquals(face, BankerDatabase.getFaceFor(banker));

        execute(db -> {
            ResultSet rs = db.prepareStatement("SELECT * FROM faces;").executeQuery();
            assertTrue(rs.next());
            assertEquals(banker.getWurmId(), rs.getLong(1));
            assertEquals(face, rs.getLong(2));
            assertFalse(rs.next());
        });
    }

    @Test
    public void testSetFaceForChangeFace() throws SQLException {
        long face = 123456;

        BankerDatabase.setFaceFor(banker, face - 1);
        Long current = BankerDatabase.getFaceFor(banker);
        assert current != null && current == face - 1;
        BankerDatabase.setFaceFor(banker, face);

        assertEquals(face, BankerDatabase.getFaceFor(banker));

        execute(db -> {
            ResultSet rs = db.prepareStatement("SELECT * FROM faces;").executeQuery();
            assertTrue(rs.next());
            assertEquals(banker.getWurmId(), rs.getLong(1));
            assertEquals(face, rs.getLong(2));
            assertFalse(rs.next());
        });
    }

    @Test
    public void testDeleteFaceFor() throws SQLException {
        Long face = BankerDatabase.getFaceFor(banker);
        assert face != null;
        execute(db -> {
            ResultSet rs = db.prepareStatement("SELECT * FROM faces;").executeQuery();
            assertTrue(rs.next());
        });

        BankerDatabase.deleteFaceFor(banker);

        assertNull(BankerDatabase.getFaceFor(banker));

        execute(db -> {
            ResultSet rs = db.prepareStatement("SELECT * FROM faces;").executeQuery();
            assertFalse(rs.next());
        });
    }
}
