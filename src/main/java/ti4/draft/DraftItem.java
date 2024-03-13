package ti4.draft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import ti4.draft.items.AbilityDraftItem;
import ti4.draft.items.AgentDraftItem;
import ti4.draft.items.BlueTileDraftItem;
import ti4.draft.items.CommanderDraftItem;
import ti4.draft.items.CommoditiesDraftItem;
import ti4.draft.items.FlagshipDraftItem;
import ti4.draft.items.HeroDraftItem;
import ti4.draft.items.HomeSystemDraftItem;
import ti4.draft.items.MechDraftItem;
import ti4.draft.items.PNDraftItem;
import ti4.draft.items.RedTileDraftItem;
import ti4.draft.items.SpeakerOrderDraftItem;
import ti4.draft.items.StartingFleetDraftItem;
import ti4.draft.items.StartingTechDraftItem;
import ti4.draft.items.TechDraftItem;
import ti4.draft.phases.FrankenDraftCardsPhase;
import ti4.draft.phases.FrankenSetupPhase;
import ti4.generator.Mapper;
import ti4.map.Player;
import ti4.model.DraftErrataModel;
import ti4.model.ModelInterface;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "ItemCategory")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AbilityDraftItem.class, name = "ABILITY"),
        @JsonSubTypes.Type(value = AgentDraftItem.class, name = "AGENT"),
        @JsonSubTypes.Type(value = BlueTileDraftItem.class, name = "BLUETILE"),
        @JsonSubTypes.Type(value = CommanderDraftItem.class, name = "COMMANDER"),
        @JsonSubTypes.Type(value = CommoditiesDraftItem.class, name = "COMMODITIES"),
        @JsonSubTypes.Type(value = FlagshipDraftItem.class, name = "FLAGSHIP"),
        @JsonSubTypes.Type(value = HeroDraftItem.class, name = "HERO"),
        @JsonSubTypes.Type(value = HomeSystemDraftItem.class, name = "HOMESYSTEM"),
        @JsonSubTypes.Type(value = MechDraftItem.class, name = "MECH"),
        @JsonSubTypes.Type(value = PNDraftItem.class, name = "PN"),
        @JsonSubTypes.Type(value = RedTileDraftItem.class, name = "REDTILE"),
        @JsonSubTypes.Type(value = SpeakerOrderDraftItem.class, name = "DRAFTORDER"),
        @JsonSubTypes.Type(value = StartingFleetDraftItem.class, name = "STARTINGFLEET"),
        @JsonSubTypes.Type(value = StartingTechDraftItem.class, name = "STARTINGTECH"),
        @JsonSubTypes.Type(value = TechDraftItem.class, name = "TECH")
})
public abstract class DraftItem implements ModelInterface {

    public DraftItem() {}
    protected DraftItem(Category itemCategory) {ItemCategory = itemCategory;}
    @Override
    @JsonIgnore
    public boolean isValid() {
        return true;
    }

    @Override
    @JsonIgnore
    public String getAlias() {
        return ItemCategory.toString()+":"+ItemId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final DraftItem other = (DraftItem) obj;
        return other.getAlias().equals(this.getAlias());
    }

    @Override
    public int hashCode() {
        return getAlias().hashCode();
    }

    public enum Category{
        ABILITY,
        TECH,
        AGENT,
        COMMANDER,
        HERO,
        MECH,
        FLAGSHIP,
        COMMODITIES,
        PN,
        HOMESYSTEM,
        STARTINGTECH,
        STARTINGFLEET,
        BLUETILE,
        REDTILE,
        DRAFTORDER
    }

    public Category ItemCategory;

    // The system ID of the item. Only convert this to player-readable text when necessary
    public String ItemId;

    @JsonIgnore
    public DraftErrataModel Errata;

    public static DraftItem Generate(Category category, String itemId) {
        DraftItem item = null;
        switch (category) {
            case ABILITY -> item =  new AbilityDraftItem(itemId);
            case TECH -> item =  new TechDraftItem(itemId);
            case AGENT -> item =  new AgentDraftItem(itemId);
            case COMMANDER -> item =  new CommanderDraftItem(itemId);
            case HERO -> item =  new HeroDraftItem(itemId);
            case MECH -> item =  new MechDraftItem(itemId);
            case FLAGSHIP -> item =  new FlagshipDraftItem(itemId);
            case COMMODITIES -> item =  new CommoditiesDraftItem(itemId);
            case PN -> item =  new PNDraftItem(itemId);
            case HOMESYSTEM -> item =  new HomeSystemDraftItem(itemId);
            case STARTINGTECH -> item =  new StartingTechDraftItem(itemId);
            case STARTINGFLEET -> item =  new StartingFleetDraftItem(itemId);
            case BLUETILE -> item =  new BlueTileDraftItem(itemId);
            case REDTILE -> item =  new RedTileDraftItem(itemId);
            case DRAFTORDER -> item =  new SpeakerOrderDraftItem(itemId);
            default -> {
                return null;
            }
        }
        item.Errata = Mapper.getFrankenErrata().get(item.getAlias());
        return item;
    }

    public static DraftItem GenerateFromAlias(String alias) {
        String[] split = alias.split(":");
        return Generate(Category.valueOf(split[0]), split[1]);
    }

    public static List<DraftItem> GetAlwaysIncludeItems(Category type) {
        List<DraftItem> alwaysInclude = new ArrayList<>();
        var frankenErrata = Mapper.getFrankenErrata().values();
        for(DraftErrataModel errataItem : frankenErrata) {
            if (errataItem.ItemCategory == type && errataItem.AlwaysAddToPool) {
                alwaysInclude.add(GenerateFromAlias(errataItem.getAlias()));
            }
        }

        return alwaysInclude;
    }

    public static String CategoryDescriptionPlural(Category category) {
        switch (category){
            case ABILITY -> {
                return "Faction Abilities";
            }
            case HERO -> {
                return "Heroes";
            }
            case DRAFTORDER -> {
                return "Table Positions";
            }
            default -> {
                return CategoryDescription(category) + "s";
            }
        }
    }

    public static String CategoryDescription(Category category) {
        switch (category) {
            case ABILITY -> {
                return "Faction Ability";
            }
            case TECH -> {
                return "Faction Tech";
            }
            case AGENT -> {
                return "Agent";
            }
            case COMMANDER -> {
                return "Commander";
            }
            case HERO -> {
                return "Hero";
            }
            case MECH -> {
                return "Mech";
            }
            case FLAGSHIP -> {
                return "Flagship";
            }
            case COMMODITIES -> {
                return "Commodity Value";
            }
            case PN -> {
                return "Faction Promissory Note";
            }
            case HOMESYSTEM -> {
                return "Home System";
            }
            case STARTINGTECH -> {
                return "Starting Tech";
            }
            case STARTINGFLEET -> {
                return "Starting Fleet";
            }
            case BLUETILE -> {
                return "Blue Tile";
            }
            case REDTILE -> {
                return "Red Tile";
            }
            case DRAFTORDER -> {
                return "Table Position/Speaker Order";
            }
            default -> throw new IllegalStateException("Unexpected value: " + category);
        }
    }

    protected DraftItem(Category category, String itemId)
    {
        ItemCategory = category;
        ItemId = itemId;
    }

    @JsonIgnore
    public abstract String getShortDescription();

    @JsonIgnore
    public String getLongDescription() {
        StringBuilder sb = new StringBuilder(getLongDescriptionImpl());
        if (Errata != null) {
            if (Errata.AdditionalComponents != null) {
                sb.append(" *Also adds: ");
                for (DraftErrataModel i: Errata.AdditionalComponents) {
                    DraftItem item = Generate(i.ItemCategory, i.ItemId);
                    sb.append(item.getItemEmoji()).append(" ").append(item.getShortDescription());
                    sb.append(", ");
                }
                sb.append("*");
            }
            if (Errata.OptionalSwaps != null) {
                sb.append(" *Includes optional swaps: ");
                for (DraftErrataModel i: Errata.OptionalSwaps) {
                    DraftItem item = Generate(i.ItemCategory, i.ItemId);
                    sb.append(item.getItemEmoji()).append(" ").append(item.getShortDescription());
                    sb.append(", ");
                }
                sb.append("*");
            }
        }
        return sb.toString();
    }

    @JsonIgnore
    protected abstract String getLongDescriptionImpl();

    @JsonIgnore
    public abstract String getItemEmoji();

    public class Serializer extends StdSerializer<DraftItem> {

        public Serializer() {
            this(null);
        }

        public Serializer(Class<DraftItem> t) {
            super(t);
        }

        @Override
        public void serialize(DraftItem draftItem, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeString(draftItem.getAlias());
        }
    }

    public class Deserializer extends StdDeserializer<DraftItem> {

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public DraftItem deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
            return DraftItem.GenerateFromAlias(jsonParser.readValueAs(String.class));
        }
    }
}
