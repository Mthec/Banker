package mod.wurmunlimited.npcs.banker;

import com.wurmonline.server.creatures.Creature;
import mod.wurmunlimited.npcs.AbstractFaceSetterMod;
import mod.wurmunlimited.npcs.FaceSetterDatabase;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseTests extends BankerTest {
    private FaceSetterDatabase database;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        database = ReflectionUtil.getPrivateField(BankerMod.mod, AbstractFaceSetterMod.class.getDeclaredField("database"));
    }

    @Test
    public void testIsDifferentFace() throws SQLException {
        long face = 12345;
        database.setFaceFor(banker, face);

        assertFalse(database.isDifferentFace(face, banker));
        assertTrue(database.isDifferentFace(face + 1, banker));

        database.deleteFaceFor(banker);
        assert database.getFaceFor(banker) == null;
        assertDoesNotThrow(() -> assertTrue(database.isDifferentFace(face, banker)));
    }

    @Test
    public void testLoadFaces() throws SQLException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
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

        ReflectionUtil.callPrivateMethod(database, FaceSetterDatabase.class.getDeclaredMethod("loadFaces"));

        faces.forEach((k, v) -> assertEquals(v, database.getFaceFor(k)));
    }

    @Test
    public void testGetFaceForNotBanker() {
        assertNull(database.getFaceFor(factory.createNewCreature()));
    }

    @Test
    public void testSetTempFace() {
        long face = 12345;
        database.setTempFace(face);

        for (int i = 0; i < 10; i++) {
            assertEquals(face, database.getFaceFor(factory.createNewBankerSkipSetup()));
        }

        database.removeTempFace(face);
        assertNull(database.getFaceFor(factory.createNewBankerSkipSetup()));
    }

    @Test
    public void testSetFaceFor() throws SQLException {
        long face = 123456;

        database.setFaceFor(banker, face);

        assertEquals(face, database.getFaceFor(banker));

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

        database.setFaceFor(banker, face - 1);
        Long current = database.getFaceFor(banker);
        assert current != null && current == face - 1;
        database.setFaceFor(banker, face);

        assertEquals(face, database.getFaceFor(banker));

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
        Long face = database.getFaceFor(banker);
        assert face != null;
        execute(db -> {
            ResultSet rs = db.prepareStatement("SELECT * FROM faces;").executeQuery();
            assertTrue(rs.next());
        });

        database.deleteFaceFor(banker);

        assertNull(database.getFaceFor(banker));

        execute(db -> {
            ResultSet rs = db.prepareStatement("SELECT * FROM faces;").executeQuery();
            assertFalse(rs.next());
        });
    }
}
