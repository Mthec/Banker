package mod.wurmunlimited.npcs.banker;

import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.shared.constants.StructureConstants;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class FaceSettersTests extends BankerTest {
    @Test
    public void testRetrieveBanker() throws FaceSetters.TooManyTransactionsException {
        FaceSetters faceSetters = new FaceSetters();
        long id = faceSetters.createIdFor(banker, player);
        assertEquals(banker, faceSetters.retrieveBankerOrNull(player, id));
    }

    @Test
    public void testRetrieveBankerNull() {
        FaceSetters faceSetters = new FaceSetters();
        assertNull(faceSetters.retrieveBankerOrNull(player, 1));
    }

    @Test
    public void testRetrieveBankerWrongPlayer() throws FaceSetters.TooManyTransactionsException {
        FaceSetters faceSetters = new FaceSetters();
        long id = faceSetters.createIdFor(banker, player);
        assertNull(faceSetters.retrieveBankerOrNull(factory.createNewPlayer(), id));
        assertNotNull(faceSetters.retrieveBankerOrNull(player, id));
    }

    @Test
    public void testRetrieveBankerOnlyWorksOnce() throws FaceSetters.TooManyTransactionsException {
        FaceSetters faceSetters = new FaceSetters();
        long id = faceSetters.createIdFor(banker, player);
        assertEquals(banker, faceSetters.retrieveBankerOrNull(player, id));
        assertNull(faceSetters.retrieveBankerOrNull(player, id));
    }

    @Test
    public void testTooManyTransactions() throws FaceSetters.TooManyTransactionsException, NoSuchFieldException, IllegalAccessException {
        FaceSetters faceSetters = new FaceSetters();

        for (int i = -1024; i < -10; i++) {
            faceSetters.createIdFor(factory.createNewBankerSkipSetup(), player);
        }

        //noinspection unchecked
        System.out.println(((Map<Long, StructureConstants.Pair<Creature, Creature>>)ReflectionUtil.getPrivateField(faceSetters, FaceSetters.class.getDeclaredField("transactions"))));

        assertThrows(FaceSetters.TooManyTransactionsException.class, () -> faceSetters.createIdFor(banker, player));

        // Putting this here as running this test twice would take up a lot of time.
        Item writ = factory.createWritFor(player, banker);
        assertTrue(changeFace.action(mock(Action.class), player, writ, banker, changeFace.getActionId(), 0));
    }

    @Test
    public void testOldTransactionsCleared() throws FaceSetters.TooManyTransactionsException, NoSuchFieldException, IllegalAccessException {
        FaceSetters faceSetters = new FaceSetters();

        Creature banker1 = factory.createNewBankerSkipSetup();
        long id1 = faceSetters.createIdFor(banker1, player);
        long newTime = System.currentTimeMillis() - TimeConstants.HOUR_MILLIS - 1;
        Map<Long, Object> startTimes = ReflectionUtil.getPrivateField(faceSetters, FaceSetters.class.getDeclaredField("transactionStartTimes"));
        Object timePlayer = startTimes.get(id1);
        factory.setFinalField(timePlayer, timePlayer.getClass().getDeclaredField("time"), newTime);

        Creature banker2 = factory.createNewBankerSkipSetup();
        long id2 = faceSetters.createIdFor(banker2, player);

        assertEquals(id1, id2);
        assertNotNull(faceSetters.retrieveBankerOrNull(player, id2));
    }
}
