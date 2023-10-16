package ti4.draft.items;

import net.dv8tion.jda.api.entities.MessageEmbed;
import ti4.draft.DraftItem;
import ti4.generator.Mapper;
import ti4.helpers.Helper;
import ti4.model.TechnologyModel;

public class TechDraftItem extends DraftItem {
    public TechDraftItem(String itemId) {
        super(Category.TECH, itemId);
    }

    @Override
    public String getItemName() {
        return getTech().getName();
    }

    @Override
    public MessageEmbed getItemCard() {
        return getTech().getRepresentationEmbed(false, true);
    }

    private TechnologyModel getTech() {
        return Mapper.getTech(ItemId);
    }

    @Override
    public String getItemEmoji() {
        TechnologyModel model = getTech();
        return Helper.getEmojiFromDiscord(model.getType().toString().toLowerCase() + "tech");
    }
}
