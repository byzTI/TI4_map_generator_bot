package ti4.draft;

import ti4.commands.milty.MiltyDraftManager;
import ti4.commands.milty.MiltyDraftTile;
import ti4.commands.milty.StartMilty;
import ti4.generator.Mapper;
import ti4.map.Game;
import ti4.model.DraftErrataModel;
import ti4.model.FactionModel;

import java.util.*;

public class FrankenDraft extends BagDraft {
    public FrankenDraft(Game owner) {
        super(owner);
    }

    @Override
    public int getItemLimitForCategory(DraftItem.Category category) {
        int limit = 0;
        switch (category) {
            case ABILITY, BLUETILE -> {
                limit = 3;
            }
            case TECH, REDTILE, STARTINGFLEET, STARTINGTECH, HOMESYSTEM, PN, COMMODITIES, FLAGSHIP, MECH, HERO, COMMANDER, AGENT -> {
                limit = 2;
            }
            case DRAFTORDER -> {
                limit = 1;
            }
        }
        return limit;
    }

    @Override
    public String getSaveString() {
        return "franken";
    }

    private static void filterUndraftablesAndShuffle(List<DraftItem> items, DraftItem.Category listCategory) {
        HashMap<String, DraftErrataModel> frankenErrata = Mapper.getFrankenErrata();
        items.removeIf((DraftItem item) -> frankenErrata.containsKey(item.getAlias()) && frankenErrata.get(item.getAlias()).Undraftable);
        items.addAll(DraftItem.GetAlwaysIncludeItems(listCategory));
        Collections.shuffle(items);
    }

    // All the generic types of draftable items (i.e. things like "Argent Starting Tech"
    private static final DraftItem.Category[] genericDraftableTypes = {
            DraftItem.Category.COMMODITIES,
            DraftItem.Category.HOMESYSTEM,
            DraftItem.Category.STARTINGFLEET,
            DraftItem.Category.STARTINGTECH
    };

    private static final String[] excludedFactions = {"lazax", "admins", "franken", "keleresm", "keleresx"};
    private static List<String> getAllFactionIds(Game activeGame) {
        Map<String, String> factionSet = Mapper.getFactionRepresentations();
        List<String> factionIds = new ArrayList<String>();
        factionSet.forEach((String id, String name) -> {
            if (name.contains("(DS)") && !activeGame.isDiscordantStarsMode()) {
                return;
            } else {
                for (String excludedFaction : excludedFactions) {
                    if (id.contains(excludedFaction)) {
                        return;
                    }
                }
            }
            factionIds.add(id);
        });
        return factionIds;
    }

    public static List<DraftItem> buildDraftOrderSet(Game activeGame) {
        List<DraftItem> allItems = new ArrayList<>();
        for(int i = 0; i < activeGame.getRealPlayers().size(); i++){
            allItems.add(DraftItem.Generate(DraftItem.Category.DRAFTORDER, Integer.toString(i+1)));
        }
        filterUndraftablesAndShuffle(allItems, DraftItem.Category.DRAFTORDER);
        return allItems;
    }

    public static List<DraftItem> buildTileSet(MiltyDraftManager draftManager, boolean blue) {
        List<DraftItem> allItems = new ArrayList<>();
        List<MiltyDraftTile> allTiles;
        if (blue) {
            allTiles = draftManager.getHigh();
            allTiles.addAll(draftManager.getMid());
            allTiles.addAll(draftManager.getLow());
        } else {
            allTiles = draftManager.getRed();
        }
        DraftItem.Category category = blue ? DraftItem.Category.BLUETILE : DraftItem.Category.REDTILE;
        for(MiltyDraftTile tile : allTiles) {
            allItems.add(DraftItem.Generate(category,
                    tile.getTile().getTileID()));
        }
        filterUndraftablesAndShuffle(allItems, category);
        return allItems;
    }

    public static List<DraftItem> buildAbilitySet(Game activeGame) {
        List<String> allFactions = getAllFactionIds(activeGame);
        List<DraftItem> allAbilityItems = new ArrayList<>();
        for (var factionId : allFactions) {
            FactionModel faction  = Mapper.getFactionSetup(factionId);
            for (var ability : faction.getAbilities()) {
                allAbilityItems.add(DraftItem.Generate(DraftItem.Category.ABILITY,ability));
            }
        }

        filterUndraftablesAndShuffle(allAbilityItems, DraftItem.Category.ABILITY);
        return allAbilityItems;
    }

    public static List<DraftItem> buildFactionTechSet(Game activeGame) {
        List<String> allFactions = getAllFactionIds(activeGame);
        List<DraftItem> allDraftableTechs = new ArrayList<>();
        for (var factionId : allFactions) {
            FactionModel faction = Mapper.getFactionSetup(factionId);
            for(var tech : faction.getFactionTech()) {
                allDraftableTechs.add(DraftItem.Generate(DraftItem.Category.TECH, tech));
            }
        }
        filterUndraftablesAndShuffle(allDraftableTechs, DraftItem.Category.TECH);
        return allDraftableTechs;
    }

    public static List<DraftItem> buildLeaderItemSet(DraftItem.Category leaderType, Game activeGame) {
        List<String> allFactions = getAllFactionIds(activeGame);
        List<DraftItem> allLeaders = new ArrayList<>();
        for (var factionId : allFactions) {
            allLeaders.add(DraftItem.Generate(leaderType, factionId + leaderType.toString().toLowerCase()));
        }
        filterUndraftablesAndShuffle(allLeaders, leaderType);
        return allLeaders;
    }
    
    public static List<DraftItem> buildUnitItemSet(DraftItem.Category unitType, Game activeGame) {
        List<String> allFactions = getAllFactionIds(activeGame);
        List<DraftItem> allUnits = new ArrayList<>();
        for (var factionId : allFactions) {
            allUnits.add(DraftItem.Generate(unitType, unitType.toString().toLowerCase() + "_" + factionId));
        }
        filterUndraftablesAndShuffle(allUnits, unitType);
        return allUnits;
    }

    public static List<DraftItem> buildPNItemSet(Game activeGame) {
        List<String> allFactions = getAllFactionIds(activeGame);
        List<DraftItem> allPns = new ArrayList<>();
        for (var factionId : allFactions) {
            FactionModel faction = Mapper.getFactionSetup(factionId);
            allPns.add(DraftItem.Generate(DraftItem.Category.PN, faction.getPromissoryNotes().get(0)));
        }
        filterUndraftablesAndShuffle(allPns, DraftItem.Category.PN);
        return allPns;
    }

    public static List<DraftItem> buildGenericFactionItemSet(DraftItem.Category category, Game activeGame) {
        List<String> factionIds = getAllFactionIds(activeGame);
        List<DraftItem> allItems = new ArrayList<DraftItem>();
        for (String factionId: factionIds) {
            allItems.add(DraftItem.Generate(category, factionId));
        }
        filterUndraftablesAndShuffle(allItems, category);
        return allItems;
    }

    @Override
    public List<DraftBag> generateBags() {
        Map<DraftItem.Category, List<DraftItem>> allDraftableItems = getAllDraftableItems();

        List<DraftBag> bags = new ArrayList<>();

        for (int i = 0; i < ownerGame.getRealPlayers().size(); i++) {
            DraftBag bag = new DraftBag();

            // Walk through each type of draftable...
            for (Map.Entry<DraftItem.Category, List<DraftItem>> draftableCollection:allDraftableItems.entrySet()) {
                DraftItem.Category category = draftableCollection.getKey();
                int categoryLimit = getItemLimitForCategory(category);
                // ... and pull out the appropriate number of items from its collection...
                for (int j = 0; j < categoryLimit; j++) {
                    // ... and add it to the player's bag.
                    bag.Contents.add(draftableCollection.getValue().remove(0));
                }
            }

            bags.add(bag);
        }

        return bags;
    }

    public Map<DraftItem.Category, List<DraftItem>> getAllDraftableItems() {
        Map<DraftItem.Category, List<DraftItem>> allDraftableItems = new HashMap<DraftItem.Category, List<DraftItem>>();
        for (DraftItem.Category category: genericDraftableTypes) {
            allDraftableItems.put(category, buildGenericFactionItemSet(category, ownerGame));
        }

        allDraftableItems.put(DraftItem.Category.DRAFTORDER, buildDraftOrderSet(ownerGame));

        MiltyDraftManager draftManager = ownerGame.getMiltyDraftManager();
        new StartMilty().initDraftTiles(draftManager);
        allDraftableItems.put(DraftItem.Category.REDTILE, buildTileSet(draftManager, false));
        allDraftableItems.put(DraftItem.Category.BLUETILE, buildTileSet(draftManager, true));

        allDraftableItems.put(DraftItem.Category.ABILITY, buildAbilitySet(ownerGame));
        allDraftableItems.put(DraftItem.Category.TECH, buildFactionTechSet(ownerGame));

        allDraftableItems.put(DraftItem.Category.AGENT, buildLeaderItemSet(DraftItem.Category.AGENT, ownerGame));
        allDraftableItems.put(DraftItem.Category.COMMANDER, buildLeaderItemSet(DraftItem.Category.COMMANDER, ownerGame));
        allDraftableItems.put(DraftItem.Category.HERO, buildLeaderItemSet(DraftItem.Category.HERO, ownerGame));

        allDraftableItems.put(DraftItem.Category.PN, buildPNItemSet(ownerGame));

        allDraftableItems.put(DraftItem.Category.MECH, buildUnitItemSet(DraftItem.Category.MECH, ownerGame));
        allDraftableItems.put(DraftItem.Category.FLAGSHIP, buildUnitItemSet(DraftItem.Category.FLAGSHIP, ownerGame));

        return allDraftableItems;
    }

    @Override
    public int getBagSize() {
        return 31;
    }
}
