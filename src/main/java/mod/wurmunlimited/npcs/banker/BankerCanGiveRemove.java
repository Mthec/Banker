package mod.wurmunlimited.npcs.banker;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.npcs.CanGive;
import mod.wurmunlimited.npcs.CanRemove;
import org.jetbrains.annotations.NotNull;

public class BankerCanGiveRemove implements CanGive, CanRemove {
    private boolean isOwnerOrGM(Creature performer, Creature target) {
        return performer.getPower() >= 2 || performer.getInventory().getItems().stream()
                                                   .anyMatch(it -> it.getTemplateId() == BankerMod.getContractTemplateId() && it.getData() == target.getWurmId());
    }
    
    @Override
    public boolean canGive(@NotNull Creature performer, @NotNull Item item, @NotNull Creature target) {
        return isWearable(item) && isOwnerOrGM(performer, target);
    }

    @Override
    public boolean canRemoveFrom(@NotNull Creature performer, @NotNull Creature target) {
        return isOwnerOrGM(performer, target) && isWearingItems(target);
    }

    @Override
    public boolean canRemove(@NotNull Creature performer, @NotNull Item item, @NotNull Creature target) {
        return canGive(performer, item, target);
    }
}
