package ti4.draft.items;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.apache.commons.lang3.StringUtils;
import ti4.draft.DraftItem;
import ti4.generator.Mapper;
import ti4.helpers.Emojis;
import ti4.helpers.Helper;
import ti4.model.FactionModel;

public class StartingFleetDraftItem extends DraftItem {
    public StartingFleetDraftItem(String itemId) {
        super(Category.STARTINGFLEET, itemId);
    }

    private FactionModel getFaction() {
        if (ItemId.equals("keleres")) {
            return Mapper.getFactionSetup("keleresa");
        }
        return Mapper.getFactionSetup(ItemId);
    }

    @Override
    public String getItemName() {
        return getFaction().getFactionName() + " Starting Fleet";
    }

    @Override
    public MessageEmbed getItemCard() {
        EmbedBuilder eb = new EmbedBuilder();
        String[] fleetDesc = getFaction().getStartingFleet().split(",");

        Emoji emoji = Emoji.fromFormatted(Helper.getFactionIconFromDiscord(getFaction().getAlias()));
        CustomEmoji customEmoji = (CustomEmoji) emoji;
        eb.setThumbnail(customEmoji.getImageUrl());

        eb.setTitle(getItemEmoji() + getItemName());
        for (String desc: fleetDesc) {
            String[] split = desc.trim().split(" ");
            String alias;
            int count = 0;
            if (StringUtils.isNumeric(split[0])) {
                count = Integer.parseInt(split[0]);
                alias = split[1];
            } else {
                count = 1;
                alias = split[0];
            }
            eb.addField(Mapper.getUnit(alias).getName() + ":", String.valueOf(count), false);
        }

        return eb.build();
    }

    @Override
    public String getItemEmoji() {
        return Emojis.NonUnitTechSkip;
    }
}
