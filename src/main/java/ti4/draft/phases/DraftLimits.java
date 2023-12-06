package ti4.draft.phases;

import ti4.draft.DraftItem;

import java.util.Map;

import static java.util.Map.entry;

public class DraftLimits {
    public Map<DraftItem.Category, Integer> BagLimits;
    public Map<DraftItem.Category, Integer> SheetLimits;

    public DraftLimits(){
    }

    public DraftLimits(Map<DraftItem.Category, Integer> bagLimits, Map<DraftItem.Category, Integer> sheetLimits) {
        BagLimits = bagLimits;
        SheetLimits = sheetLimits;
    }
    public static DraftLimits StandardFranken = new DraftLimits(Map.ofEntries(
            entry(DraftItem.Category.TECH, 3),
            entry(DraftItem.Category.ABILITY, 4),
            entry(DraftItem.Category.AGENT, 2),
            entry(DraftItem.Category.COMMANDER, 2),
            entry(DraftItem.Category.HERO, 2),
            entry(DraftItem.Category.MECH, 2),
            entry(DraftItem.Category.FLAGSHIP, 2),
            entry(DraftItem.Category.COMMODITIES, 2),
            entry(DraftItem.Category.PN, 2),
            entry(DraftItem.Category.HOMESYSTEM, 2),
            entry(DraftItem.Category.STARTINGTECH, 2),
            entry(DraftItem.Category.STARTINGFLEET, 2),
            entry(DraftItem.Category.BLUETILE, 3),
            entry(DraftItem.Category.REDTILE, 2),
            entry(DraftItem.Category.DRAFTORDER, 1)
            ),
            Map.ofEntries(
            entry(DraftItem.Category.TECH, 2),
            entry(DraftItem.Category.ABILITY, 3),
            entry(DraftItem.Category.AGENT, 1),
            entry(DraftItem.Category.COMMANDER, 1),
            entry(DraftItem.Category.HERO, 1),
            entry(DraftItem.Category.MECH, 1),
            entry(DraftItem.Category.FLAGSHIP, 1),
            entry(DraftItem.Category.COMMODITIES, 1),
            entry(DraftItem.Category.PN, 1),
            entry(DraftItem.Category.HOMESYSTEM, 1),
            entry(DraftItem.Category.STARTINGTECH, 1),
            entry(DraftItem.Category.STARTINGFLEET, 1),
            entry(DraftItem.Category.BLUETILE, 3),
            entry(DraftItem.Category.REDTILE, 2),
            entry(DraftItem.Category.DRAFTORDER, 1)
    ));
    public static DraftLimits PoweredFranken = new DraftLimits(Map.ofEntries(
            entry(DraftItem.Category.TECH, 4),
            entry(DraftItem.Category.ABILITY, 5),
            entry(DraftItem.Category.AGENT, 2),
            entry(DraftItem.Category.COMMANDER, 2),
            entry(DraftItem.Category.HERO, 2),
            entry(DraftItem.Category.MECH, 2),
            entry(DraftItem.Category.FLAGSHIP, 2),
            entry(DraftItem.Category.COMMODITIES, 2),
            entry(DraftItem.Category.PN, 2),
            entry(DraftItem.Category.HOMESYSTEM, 2),
            entry(DraftItem.Category.STARTINGTECH, 2),
            entry(DraftItem.Category.STARTINGFLEET, 2),
            entry(DraftItem.Category.BLUETILE, 3),
            entry(DraftItem.Category.REDTILE, 2),
            entry(DraftItem.Category.DRAFTORDER, 1)
    ),
            Map.ofEntries(
                    entry(DraftItem.Category.TECH, 3),
                    entry(DraftItem.Category.ABILITY, 4),
                    entry(DraftItem.Category.AGENT, 1),
                    entry(DraftItem.Category.COMMANDER, 1),
                    entry(DraftItem.Category.HERO, 1),
                    entry(DraftItem.Category.MECH, 1),
                    entry(DraftItem.Category.FLAGSHIP, 1),
                    entry(DraftItem.Category.COMMODITIES, 1),
                    entry(DraftItem.Category.PN, 1),
                    entry(DraftItem.Category.HOMESYSTEM, 1),
                    entry(DraftItem.Category.STARTINGTECH, 1),
                    entry(DraftItem.Category.STARTINGFLEET, 1),
                    entry(DraftItem.Category.BLUETILE, 3),
                    entry(DraftItem.Category.REDTILE, 2),
                    entry(DraftItem.Category.DRAFTORDER, 1)
            ));

}
