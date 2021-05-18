package mod.wurmunlimited.npcs.banker;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureStatus;
import com.wurmonline.server.economy.FakeShop;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.WurmObjectsFactory;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;

import static org.mockito.Mockito.mock;

public class BankerObjectsFactory extends WurmObjectsFactory {
    public final int bankerTemplateId;

    public BankerObjectsFactory() throws Exception {
        super();
        bankerTemplateId = new BankerTemplate().createCreateTemplateBuilder().build().getTemplateId();
        BankerMod mod = new BankerMod();
        mod.configure(defaultProperties());
        mod.onItemTemplatesCreated();
        BankerMod.withdrawals.clear();

        try {
            Field faceSetters = BankerMod.class.getDeclaredField("faceSetters");
            faceSetters.setAccessible(true);
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(faceSetters, faceSetters.getModifiers() & ~Modifier.FINAL);
            faceSetters.set(null, new FaceSetters());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Properties defaultProperties() {
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty("bribery_cost", "0");
        defaultProperties.setProperty("contract_price", "10000");
        defaultProperties.setProperty("name_prefix", "Banker");
        return defaultProperties;
    }

    public Creature createNewBanker() {
        return createNewBanker((byte)0);
    }

    public Creature createNewBanker(byte kingdom) {
        Creature banker;
        try {
            banker = BankerTemplate.createCreature(createNewPlayer().getCurrentTile(), 0, "Banker_Fred", (byte)0, kingdom, 0);
            creatures.put(banker.getWurmId(), banker);
            banker.createPossessions();
            attachFakeCommunicator(banker);
            BankerDatabase.setFaceFor(banker, 98765);

            return banker;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Creature createNewBankerSkipSetup() {
        try {
            VolaTile tile = mock(VolaTile.class);
            Creature banker = Creature.doNew(bankerTemplateId, (float)(tile.getTileX() << 2) + 2.0F, (float)(tile.getTileY() << 2) + 2.0F, 180.0F, tile.getLayer(), "Banker_Bob", (byte)0, (byte)0);
            creatures.put(banker.getWurmId(), banker);

            return banker;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Item createWritFor(Creature owner, Creature banker) {
        Item writ = createNewItem(BankerMod.getContractTemplateId());
        writ.setData(banker.getWurmId());
        owner.getInventory().insertItem(writ);
        return writ;
    }
}
