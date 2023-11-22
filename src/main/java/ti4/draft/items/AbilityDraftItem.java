package ti4.draft.items;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ti4.draft.DraftItem;
import ti4.generator.Mapper;
import ti4.model.AbilityModel;

public class AbilityDraftItem extends DraftItem {
    public AbilityDraftItem(String itemId) {
        super(Category.ABILITY, itemId);
    }

    @Override
    public String getItemName() {
        String[] split = getAbilityStringSplit();
        return split[0];
    }

    @Override
    public MessageEmbed getItemCard() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(getItemEmoji() + getItemName());


        String[] split = getAbilityStringSplit();
        if (!split[2].equals(" ")) {
            eb.addField("Additional Text:", split[2], true);
        }
        if (!split[3].equals(" ")) {
            eb.addField("Window:", split[3], true);
        }
        if (!split[4].equals(" ")) {
            eb.addField("Ability:", split[4], true);
        }


        return eb.build();
    }

    @Override
    public String getItemEmoji() {
        return getAbilityModel().getFactionEmoji();
    }

    // #Columns: ID = Ability Name | Faction | Raw Modifier | AbilityWindow | AbilityText
    private String[] getAbilityStringSplit() {
        return Mapper.getAbility(ItemId).split("\\|");
    }
}
