package ti4.draft.phases;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import ti4.commands.milty.MiltyDraftManager;
import ti4.commands.milty.MiltyDraftTile;
import ti4.commands.milty.StartMilty;
import ti4.draft.DraftBag;
import ti4.draft.DraftItem;
import ti4.generator.Mapper;
import ti4.helpers.FrankenDraftHelper;
import ti4.helpers.Helper;
import ti4.map.Game;
import ti4.map.Player;
import ti4.map.UnitHolder;
import ti4.message.BotLogger;
import ti4.message.MessageHelper;
import ti4.model.DraftErrataModel;
import ti4.model.FactionModel;

import java.util.*;

public class FrankenSetupPhase extends DraftPhase{

    @Override
    public void StartPhase() {
        MessageHelper.sendMessageToChannel(Draft.Game.getActionsChannel(), Draft.Game.getPing() + "\n# Welcome to FrankenDraft!");
        MessageHelper.sendMessageToChannel(Draft.Game.getActionsChannel(), "In this variant, you will build your own FrankenFaction out of components of other factions. First, we will draft our components, as well as system tiles for our slices. Then, we will build the map. Then, we will assemble our ~~monstrosities~~ factions, reveal them to the table, and begin the game.");
        MessageHelper.sendMessageToChannel(Draft.Game.getActionsChannel(),
                "In *Standard Franken* you will each draft 4 Abilities, 3 Faction Techs, 2 of each type of leader, and 2 home systems, starting fleets, starting techs, commodities, faction PNs, mechs, and flagships.\n" +
                        "In *Powered Franken* you will each draft 5 Abilities, 4 Faction Techs, 2 of each type of leader, and 2 home systems, starting fleets, starting techs, commodities, faction PNs, mechs, and flagships.\n" +
                        "In both modes, you will discard one of each type of card before building your faction.\n" +
                        "You may also chose to create your own custom draft rules.");
        ShowRuleSelectionOptions();
    }

    private void ShowRuleSelectionOptions() {
        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.of(ButtonStyle.SUCCESS, FrankenDraftHelper.ActionName + "setup_standard", "Standard Franken").withEmoji(Emoji.fromFormatted(":troll:")));
        buttons.add(Button.of(ButtonStyle.SUCCESS, FrankenDraftHelper.ActionName + "setup_powered", "Powered Franken").withEmoji(Emoji.fromFormatted(":robot:")));
        MessageHelper.sendMessageToChannelWithButtons(Draft.Game.getActionsChannel(),"Please select your draft rules:", buttons);
    }

    @Override
    public void EndPhase() {
        MessageHelper.sendMessageToChannel(Draft.Game.getActionsChannel(), "Generating bags...");
        List<DraftBag> bags = generateBags();

        Draft.Bags = new HashMap<>();
        for (DraftBag bag : bags) {
            Draft.Bags.put(bag.label, bag);
        }

        StringBuilder sb = new StringBuilder(Draft.Game.getPing());
        sb.append("\nThe draft rules are as follows:\nYou will each draft ");
        for (Map.Entry<DraftItem.Category, Integer> rule: Draft.Limits.BagLimits.entrySet()) {
            sb.append(rule.getValue()).append(" ").append(rule.getKey()).append("s, ");
        }

        sb.append("\nThen you will build a faction and slice from ");
        for (Map.Entry<DraftItem.Category, Integer> rule: Draft.Limits.BagLimits.entrySet()) {
            sb.append(rule.getValue()).append(" ").append(rule.getKey()).append("s, ");
        }

        MessageHelper.sendMessageToChannelAndPin(Draft.Game.getActionsChannel(), sb.toString());
    }

    @Override
    public void ProcessCommand(Player player, String command, String... args) {
        List<Button> buttons = new ArrayList<>();
        if (command == "setup") {
            switch (args[0]) {
                case "standard":
                    Draft.Limits = DraftLimits.StandardFranken;
                    buttons.add(Button.of(ButtonStyle.SUCCESS, FrankenDraftHelper.ActionName + "complete", "Yes"));
                    buttons.add(Button.of(ButtonStyle.DANGER, FrankenDraftHelper.ActionName + "redo", "No"));
                    MessageHelper.sendMessageToChannelWithButtons(Draft.Game.getActionsChannel(), "Are you sure you want to use Standard Franken rules?", buttons);
                    return;
                case "powered":
                    Draft.Limits = DraftLimits.PoweredFranken;
                    buttons.add(Button.of(ButtonStyle.SUCCESS, FrankenDraftHelper.ActionName + "complete", "Yes"));
                    buttons.add(Button.of(ButtonStyle.DANGER, FrankenDraftHelper.ActionName + "redo", "No"));
                    MessageHelper.sendMessageToChannelWithButtons(Draft.Game.getActionsChannel(), "Are you sure you want to use Powered Franken rules?", buttons);
                    return;
                case "custom":
                    MessageHelper.sendMessageToChannel(Draft.Game.getActionsChannel(), "lol jk, this hasn't been implemented yet");
                    return;
            }
        }
        if (command == "redo") {
            ShowRuleSelectionOptions();
        }
        if (command == "complete") {
            EndPhase();
        }
    }

    private static void filterUndraftablesAndShuffle(List<DraftItem> items, DraftItem.Category listCategory) {
        Map<String, DraftErrataModel> frankenErrata = Mapper.getFrankenErrata();
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
    private static List<String> getAllFrankenFactions(Game activeGame) {
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

    private static List<DraftItem> buildDraftOrderSet(Game activeGame) {
        List<DraftItem> allItems = new ArrayList<>();
        for(int i = 0; i < activeGame.getRealPlayers().size(); i++){
            allItems.add(DraftItem.Generate(DraftItem.Category.DRAFTORDER, Integer.toString(i+1)));
        }
        filterUndraftablesAndShuffle(allItems, DraftItem.Category.DRAFTORDER);
        return allItems;
    }

    private static List<DraftItem> buildTileSet(MiltyDraftManager draftManager, boolean blue) {
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

    private static List<DraftItem> buildAbilitySet(Game activeGame) {
        List<String> allFactions = getAllFrankenFactions(activeGame);
        List<DraftItem> allAbilityItems = new ArrayList<>();
        for (var factionId : allFactions) {
            FactionModel faction  = Mapper.getFaction(factionId);
            if(faction != null){
                for (var ability : faction.getAbilities()) {
                    allAbilityItems.add(DraftItem.Generate(DraftItem.Category.ABILITY,ability));
                }
            }else{
                BotLogger.log("Franken faction returned null on this id"+factionId);
            }

        }

        filterUndraftablesAndShuffle(allAbilityItems, DraftItem.Category.ABILITY);
        return allAbilityItems;
    }

    private static List<DraftItem> buildFactionTechSet(Game activeGame) {
        List<String> allFactions = getAllFrankenFactions(activeGame);
        List<DraftItem> allDraftableTechs = new ArrayList<>();
        for (var factionId : allFactions) {
            FactionModel faction = Mapper.getFaction(factionId);
            for(var tech : faction.getFactionTech()) {
                allDraftableTechs.add(DraftItem.Generate(DraftItem.Category.TECH, tech));
            }
        }
        filterUndraftablesAndShuffle(allDraftableTechs, DraftItem.Category.TECH);
        return allDraftableTechs;
    }

    private static List<DraftItem> buildLeaderItemSet(DraftItem.Category leaderType, Game activeGame) {
        List<String> allFactions = getAllFrankenFactions(activeGame);
        List<DraftItem> allLeaders = new ArrayList<>();
        for (var factionId : allFactions) {
            allLeaders.add(DraftItem.Generate(leaderType, factionId + leaderType.toString().toLowerCase()));
        }
        filterUndraftablesAndShuffle(allLeaders, leaderType);
        return allLeaders;
    }

    private static List<DraftItem> buildUnitItemSet(DraftItem.Category unitType, Game activeGame) {
        List<String> allFactions = getAllFrankenFactions(activeGame);
        List<DraftItem> allUnits = new ArrayList<>();
        for (var factionId : allFactions) {
            allUnits.add(DraftItem.Generate(unitType, factionId + "_" + unitType.toString().toLowerCase()));
        }
        filterUndraftablesAndShuffle(allUnits, unitType);
        return allUnits;
    }

    private static List<DraftItem> buildPNItemSet(Game activeGame) {
        List<String> allFactions = getAllFrankenFactions(activeGame);
        List<DraftItem> allPns = new ArrayList<>();
        for (var factionId : allFactions) {
            FactionModel faction = Mapper.getFaction(factionId);
            allPns.add(DraftItem.Generate(DraftItem.Category.PN, faction.getPromissoryNotes().get(0)));
        }
        filterUndraftablesAndShuffle(allPns, DraftItem.Category.PN);
        return allPns;
    }

    private static List<DraftItem> buildGenericFactionItemSet(DraftItem.Category category, Game activeGame) {
        List<String> factionIds = getAllFrankenFactions(activeGame);
        List<DraftItem> allItems = new ArrayList<DraftItem>();
        for (String factionId: factionIds) {
            allItems.add(DraftItem.Generate(category, factionId));
        }
        filterUndraftablesAndShuffle(allItems, category);
        return allItems;
    }

    private List<DraftBag> generateBags() {
        Map<DraftItem.Category, List<DraftItem>> allDraftableItems = getAllDraftableItems();

        List<DraftBag> bags = new ArrayList<>();

        for (int i = 0; i < Draft.Game.getRealPlayers().size(); i++) {
            DraftBag bag = new DraftBag("Bag", Draft.Game.getRealPlayers().get(i).getColor(), Draft.Game.getName());

            // Walk through each type of draftable...
            for (Map.Entry<DraftItem.Category, List<DraftItem>> draftableCollection:allDraftableItems.entrySet()) {
                DraftItem.Category category = draftableCollection.getKey();
                int categoryLimit = Draft.Limits.BagLimits.get(category);
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

    @JsonIgnore
    private Map<DraftItem.Category, List<DraftItem>> getAllDraftableItems() {
        Map<DraftItem.Category, List<DraftItem>> allDraftableItems = new HashMap<DraftItem.Category, List<DraftItem>>();
        Game game = Draft.Game;

        for (DraftItem.Category category: genericDraftableTypes) {
            allDraftableItems.put(category, buildGenericFactionItemSet(category, game));
        }

        allDraftableItems.put(DraftItem.Category.DRAFTORDER, buildDraftOrderSet(game));

        MiltyDraftManager draftManager = game.getMiltyDraftManager();
        new StartMilty().initDraftTiles(draftManager);
        allDraftableItems.put(DraftItem.Category.REDTILE, buildTileSet(draftManager, false));
        allDraftableItems.put(DraftItem.Category.BLUETILE, buildTileSet(draftManager, true));

        allDraftableItems.put(DraftItem.Category.ABILITY, buildAbilitySet(game));
        allDraftableItems.put(DraftItem.Category.TECH, buildFactionTechSet(game));

        allDraftableItems.put(DraftItem.Category.AGENT, buildLeaderItemSet(DraftItem.Category.AGENT, game));
        allDraftableItems.put(DraftItem.Category.COMMANDER, buildLeaderItemSet(DraftItem.Category.COMMANDER, game));
        allDraftableItems.put(DraftItem.Category.HERO, buildLeaderItemSet(DraftItem.Category.HERO, game));

        allDraftableItems.put(DraftItem.Category.PN, buildPNItemSet(game));

        allDraftableItems.put(DraftItem.Category.MECH, buildUnitItemSet(DraftItem.Category.MECH, game));
        allDraftableItems.put(DraftItem.Category.FLAGSHIP, buildUnitItemSet(DraftItem.Category.FLAGSHIP, game));

        return allDraftableItems;
    }
}
