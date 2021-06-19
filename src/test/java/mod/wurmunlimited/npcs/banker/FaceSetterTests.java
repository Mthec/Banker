package mod.wurmunlimited.npcs.banker;

import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.npcs.FaceSetter;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class FaceSetterTests extends BankerTest {
    @Test
    public void testRetrieveBanker() throws FaceSetter.TooManyTransactionsException {
        FaceSetter faceSetter = new FaceSetter();
        long id = faceSetter.createIdFor(banker, player);
        assertEquals(banker, faceSetter.retrieveCreatureOrNull(player, id));
    }

    @Test
    public void testRetrieveBankerNull() {
        FaceSetter faceSetter = new FaceSetter();
        assertNull(faceSetter.retrieveCreatureOrNull(player, 1));
    }

    @Test
    public void testRetrieveBankerWrongPlayer() throws FaceSetter.TooManyTransactionsException {
        FaceSetter faceSetter = new FaceSetter();
        long id = faceSetter.createIdFor(banker, player);
        assertNull(faceSetter.retrieveCreatureOrNull(factory.createNewPlayer(), id));
        assertNotNull(faceSetter.retrieveCreatureOrNull(player, id));
    }

    @Test
    public void testRetrieveBankerOnlyWorksOnce() throws FaceSetter.TooManyTransactionsException {
        FaceSetter FaceSetter = new FaceSetter();
        long id = FaceSetter.createIdFor(banker, player);
        assertEquals(banker, FaceSetter.retrieveCreatureOrNull(player, id));
        assertNull(FaceSetter.retrieveCreatureOrNull(player, id));
    }

    @Test
    public void testTooManyTransactions() throws FaceSetter.TooManyTransactionsException, NoSuchFieldException, IllegalAccessException {
        FaceSetter faceSetter = new FaceSetter();

        for (int i = -1024; i < -10; i++) {
            faceSetter.createIdFor(factory.createNewBankerSkipSetup(), player);
        }

        assertThrows(FaceSetter.TooManyTransactionsException.class, () -> faceSetter.createIdFor(banker, player));

        Item writ = factory.createWritFor(player, banker);
        assertTrue(changeFace.action(mock(Action.class), player, writ, banker, changeFace.getActionId(), 0));
    }

    @Test
    public void testOldTransactionsCleared() throws FaceSetter.TooManyTransactionsException, NoSuchFieldException, IllegalAccessException {
        FaceSetter FaceSetter = new FaceSetter();

        Creature banker1 = factory.createNewBankerSkipSetup();
        long id1 = FaceSetter.createIdFor(banker1, player);
        long newTime = System.currentTimeMillis() - TimeConstants.HOUR_MILLIS - 1;
        Map<Long, Object> startTimes = ReflectionUtil.getPrivateField(FaceSetter, FaceSetter.class.getDeclaredField("transactionStartTimes"));
        Object timePlayer = startTimes.get(id1);
        BankerObjectsFactory.setFinalField(timePlayer, timePlayer.getClass().getDeclaredField("time"), newTime);

        Creature banker2 = factory.createNewBankerSkipSetup();
        long id2 = FaceSetter.createIdFor(banker2, player);

        assertEquals(id1, id2);
        assertNotNull(FaceSetter.retrieveCreatureOrNull(player, id2));
    }
}
