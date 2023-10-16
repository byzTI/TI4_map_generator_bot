package ti4.draft.items;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import ti4.draft.DraftItem;
import ti4.helpers.Emojis;
import ti4.helpers.Helper;

public class SpeakerOrderDraftItem extends DraftItem {
    public SpeakerOrderDraftItem(String itemId) {
        super(Category.DRAFTORDER, itemId);
    }

    @Override
    public String getItemName() {
        return "Table Position " + ItemId;
    }

    @Override
    public MessageEmbed getItemCard() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(getItemEmoji() + getItemName());
        Emoji emoji = Emoji.fromFormatted(getItemEmoji());
        CustomEmoji customEmoji = (CustomEmoji) emoji;
        eb.setThumbnail(customEmoji.getImageUrl());
        return eb.build();
    }


    @Override
    public String getItemEmoji() {
        if (ItemId.equals("1")) {
            return Emojis.SpeakerToken;
        }
        return Helper.getResourceEmoji(Integer.parseInt(ItemId));
    }
}
