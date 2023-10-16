package ti4.draft.items;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.NotNull;
import ti4.draft.DraftItem;
import ti4.generator.Mapper;
import ti4.helpers.Emojis;
import ti4.helpers.Helper;
import ti4.model.FactionModel;
import ti4.model.TechnologyModel;

import java.util.List;

public class StartingTechDraftItem extends DraftItem {
    public StartingTechDraftItem(String itemId) {
        super(Category.STARTINGTECH, itemId);
    }

    private FactionModel getFaction() {
        if (ItemId.equals("keleres")) {
            return Mapper.getFactionSetup("keleresa");
        }
        return Mapper.getFactionSetup(ItemId);
    }
    @Override
    public MessageEmbed getItemCard() {
        EmbedBuilder eb = new EmbedBuilder();

        Emoji emoji = Emoji.fromFormatted(Helper.getFactionIconFromDiscord(getFaction().getAlias()));
        CustomEmoji customEmoji = (CustomEmoji) emoji;
        eb.setThumbnail(customEmoji.getImageUrl());

        eb.setTitle(getItemEmoji() + getItemName());
        eb.addField("Starting tech:", getTechString(), true);
        return eb.build();
    }

    @Override
    public String getItemName() {
        return getFaction().getFactionName() + " Starting Tech";
    }

    @NotNull
    private String getTechString() {
        if (ItemId.equals("winnu")) {
            return "Choose any 1 technology that has no prerequisites.";
        } else if (ItemId.equals("argent")) {
            return "Choose TWO of the following: :Biotictech: Neural Motivator, :Cybernetictech: Sarween Tools, :Warfaretech: Plasma Scoring";
        } else if (ItemId.equals("keleres")) {
            return "Choose 2 non-faction technologies owned by other players.";
        }
        List<String> techs = startingTechs();
        StringBuilder builder = new StringBuilder();
        TechnologyModel tech;
        for (int i = 0; i < techs.size()-1; i++) {
            tech = Mapper.getTech(techs.get(i));
            builder.append(Helper.getEmojiFromDiscord(tech.getType().toString().toLowerCase() + "tech"));
            builder.append(" ");
            builder.append(tech.getName());
            builder.append(", ");
        }
        tech = Mapper.getTech(techs.get(techs.size()-1));
        builder.append(Helper.getEmojiFromDiscord(tech.getType().toString().toLowerCase() + "tech"));
        builder.append(" ");
        builder.append(tech.getName());
        return String.join(",\n", builder.toString());
    }

    private List<String> startingTechs() {
        return getFaction().getStartingTech();
    }

    @Override
    public String getItemEmoji() {
        return Emojis.UnitTechSkip;
    }
}
