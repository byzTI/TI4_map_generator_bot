package ti4.draft;

import ti4.draft.items.*;
import ti4.generator.Mapper;
import ti4.map.Game;
import ti4.model.FactionModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrankenUtils {
    public static final String[] EXCLUDED_FACTIONS = { "lazax", "admins", "franken", "keleresm", "keleresx", "miltymod", "qulane" };

    public static List<FactionModel> getDraftableFactionsForGame(Game activeGame) {
        List<FactionModel> factionSet = getAllFrankenLegalFactions();
        if (!activeGame.isDiscordantStarsMode()) {
            factionSet.removeIf(factionModel -> factionModel.getSource().isDs() && !factionModel.getSource().isPok());
        }
        return factionSet;
    }

    public static List<FactionModel> getAllFrankenLegalFactions() {
        List<FactionModel> factionSet = Mapper.getFactions();
        factionSet.removeIf((FactionModel model) -> {
            if (model.getSource().isPok() || model.getSource().isDs()){
                for (String excludedFaction : EXCLUDED_FACTIONS) {
                    if (model.getAlias().contains(excludedFaction)) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        });
        return factionSet;
    }

    // Generates all possible draftable cards from a given set of factions.
    public static Map<ti4.draft.DraftItem.Category, List<DraftItem>> generateAllCards(List<FactionModel> factions) {
        Map<DraftItem.Category, List<DraftItem>> allDraftableItems = new HashMap<>();
        allDraftableItems.put(DraftItem.Category.ABILITY, AbilityDraftItem.buildAllDraftableItems(factions));
        allDraftableItems.put(DraftItem.Category.TECH, TechDraftItem.buildAllDraftableItems(factions));
        allDraftableItems.put(DraftItem.Category.AGENT, AgentDraftItem.buildAllDraftableItems(factions));
        allDraftableItems.put(DraftItem.Category.COMMANDER, CommanderDraftItem.buildAllDraftableItems(factions));
        allDraftableItems.put(DraftItem.Category.HERO, HeroDraftItem.buildAllDraftableItems(factions));
        allDraftableItems.put(DraftItem.Category.COMMODITIES, CommoditiesDraftItem.buildAllDraftableItems(factions));
        allDraftableItems.put(DraftItem.Category.FLAGSHIP, FlagshipDraftItem.buildAllDraftableItems(factions));
        allDraftableItems.put(DraftItem.Category.MECH, MechDraftItem.buildAllDraftableItems(factions));
        allDraftableItems.put(DraftItem.Category.HOMESYSTEM, HomeSystemDraftItem.buildAllDraftableItems(factions));
        allDraftableItems.put(DraftItem.Category.PN, PNDraftItem.buildAllDraftableItems(factions));
        allDraftableItems.put(DraftItem.Category.STARTINGFLEET, StartingFleetDraftItem.buildAllDraftableItems(factions));
        allDraftableItems.put(DraftItem.Category.STARTINGTECH, StartingTechDraftItem.buildAllDraftableItems(factions));


        return allDraftableItems;
    }

    public static String IntToOrdinal(int i) {
        // Yeehaw Stackoverflow: https://stackoverflow.com/questions/6810336/is-there-a-way-in-java-to-convert-an-integer-to-its-ordinal-name
        String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        return switch (i % 100) {
            case 11, 12, 13 -> i + "th";
            default -> i + suffixes[i % 10];
        };
    }
}
