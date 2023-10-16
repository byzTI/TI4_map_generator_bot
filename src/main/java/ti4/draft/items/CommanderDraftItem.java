package ti4.draft.items;

import net.dv8tion.jda.api.entities.MessageEmbed;
import ti4.draft.DraftItem;
import ti4.generator.Mapper;
import ti4.helpers.Helper;
import ti4.model.LeaderModel;

public class CommanderDraftItem extends DraftItem {
    public CommanderDraftItem(String itemId) {
        super(Category.COMMANDER, itemId);
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
        }return "Commander - " + getLeader().getName();
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
