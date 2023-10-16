package ti4.draft.items;

import net.dv8tion.jda.api.entities.MessageEmbed;
import ti4.draft.DraftItem;
import ti4.generator.Mapper;
import ti4.helpers.Emojis;
import ti4.model.PromissoryNoteModel;

public class PNDraftItem extends DraftItem {
    public PNDraftItem(String itemId) {
        super(Category.PN, itemId);
    }

    private PromissoryNoteModel getPn() {
        return Mapper.getPromissoryNoteByID(ItemId);
    }

    @Override
    public MessageEmbed getItemCard() {
        return getPn().getRepresentationEmbed(false, false, true);
    }

    @Override
    public String getItemName() {
        PromissoryNoteModel pn = getPn();
        return "Promissory Note - " + pn.getName();
    }

    @Override
    public String getItemEmoji() {
        return Emojis.PN;
    }
}
