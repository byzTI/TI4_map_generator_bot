package ti4.draft.items;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ti4.draft.DraftItem;
import ti4.generator.Mapper;
import ti4.generator.TileHelper;
import ti4.helpers.Helper;
import ti4.model.FactionModel;
import ti4.model.PlanetModel;
import ti4.model.TileModel;

public class HomeSystemDraftItem extends DraftItem {
    public HomeSystemDraftItem(String itemId) {
        super(Category.HOMESYSTEM, itemId);
    }

    @Override
    public MessageEmbed getItemCard() {
        EmbedBuilder eb = new EmbedBuilder();
        FactionModel faction = Mapper.getFactionSetup(ItemId);
        TileModel tile = TileHelper.getTile(faction.getHomeSystem());

        eb.setTitle(getItemEmoji() + getItemName());

        eb.setThumbnail("attachment://" + tile.getImagePath());
        if (!tile.getPlanets().isEmpty()) eb.setDescription("Planets: " + tile.getPlanets().toString());

        return eb.build();
    }

    @Override
    public String getItemName() {
        return Mapper.getFactionRepresentations().get(ItemId) + " Home System";
    }

    private void buildPlanetString(PlanetModel planet, StringBuilder sb) {
        sb.append(planet.getName());
        sb.append(" (");
        sb.append(planet.getResources()).append("/").append(planet.getInfluence());
        sb.append(") ");
    }
    @Override
    public String getItemEmoji() {
        return Helper.getFactionIconFromDiscord(ItemId);
    }
}
