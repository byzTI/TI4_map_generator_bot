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
        return getAbilityModel().getName();
    }

    @Override
    public MessageEmbed getItemCard() {
        return getAbilityModel().getRepresentationEmbed(true);
    }

    @Override
    public String getItemEmoji() {
        return getAbilityModel().getFactionEmoji();
    }

    private AbilityModel getAbilityModel() {
        return Mapper.getAbility(ItemId);
    }
}
