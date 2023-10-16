package ti4.draft.items;

import net.dv8tion.jda.api.entities.MessageEmbed;
import ti4.draft.DraftItem;
import ti4.generator.Mapper;
import ti4.helpers.Emojis;
import ti4.model.UnitModel;

public class MechDraftItem extends DraftItem {
    public MechDraftItem(String itemId) {
        super(Category.MECH, itemId);
    }

    private UnitModel getUnit() {
        return Mapper.getUnit(ItemId);
    }

    @Override
    public String getItemName() {
        return "Mech - " + getUnit().getName();
    }

    @Override
    public MessageEmbed getItemCard() {
        return getUnit().getRepresentationEmbed(false);
    }

    @Override
    public String getItemEmoji() {
        return Emojis.mech;
    }
}
