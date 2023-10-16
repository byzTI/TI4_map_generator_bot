package ti4.draft.items;

import net.dv8tion.jda.api.entities.MessageEmbed;
import ti4.draft.DraftItem;
import ti4.generator.Mapper;
import ti4.helpers.Helper;
import ti4.model.LeaderModel;

public class HeroDraftItem extends DraftItem {
    public HeroDraftItem(String itemId) {
        super(Category.HERO, itemId);
    }

    private LeaderModel getLeader() {
        return Mapper.getLeader(ItemId);
    }

    @Override
    public String getItemName() {
        LeaderModel leader = getLeader();
        if (leader == null)
        {
            return getAlias();
        }
        return "Hero - " + leader.getName().replace("\n", "");
    }

    @Override
    public MessageEmbed getItemCard() {
        return getLeader().getRepresentationEmbed(false, false, true, false);
    }

    @Override
    public String getItemEmoji() {

        LeaderModel leader = getLeader();
        if (leader != null) {
            return Helper.getEmojiFromDiscord(leader.getID());
        }
        return "";
    }
}
