package ti4.draft.phases;

import com.fasterxml.jackson.annotation.JsonInclude;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import ti4.commands.milty.MiltyDraftManager;
import ti4.commands.milty.StartMilty;
import ti4.draft.DraftBag;
import ti4.draft.DraftItem;
import ti4.draft.DraftPhase;
import ti4.draft.FrankenUtils;
import ti4.draft.items.*;
import ti4.map.Player;
import ti4.message.BotLogger;
import ti4.message.MessageHelper;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FrankenDraftCardsPhase extends DraftPhase {

    public Map<DraftItem.Category, Integer> CategoryLimits = new HashMap<>();
    public Map<String, DraftBag> Bags = new HashMap<>();
    public Map<String, List<DraftItem>> Queues = new HashMap<>();
    public int PassCount;
    @JsonInclude
    private List<String> draftOrder = new ArrayList<>();

    @JsonInclude
    private String lastPassMessageId;

    @Override
    public void onPhaseEnd() {

    }

    @Override
    public void onPhaseStart() {
        generateBags();

        for (Player p : Draft.Game.getRealPlayers()) {
            DraftBag bag = Bags.get(p.getUserID());

            if (bag.getExistingThread() != null) {
                bag.getThread().delete().queue(unused -> {
                    bag.createThread().queue((threadChannel -> {
                        bag.populateBagThreadAsync().queue(u -> {
                            openBagForPlayer(p);
                        });
                    }));
                });
            }
            else {
                bag.createThread().queue((threadChannel -> {
                    bag.populateBagThreadAsync().queue(unused -> {
                        openBagForPlayer(p);
                    });
                }));
            }
        }
        Draft.Game.getMainGameChannel().sendMessage("*Bags are being distributed. Please select 3 cards to draft. " +
                "After this, you will select at most 2 cards each round.*").queue(message -> lastPassMessageId = message.getId());
    }

    public void passBags() {
        PassCount++;
        Draft.Game.getMainGameChannel().sendMessage("*Bags have been passed. Please select 2 cards to draft. " +
                "This was the " + FrankenUtils.IntToOrdinal(PassCount) + " round of cards.*").queue(message -> lastPassMessageId = message.getId());
        openAllBags();
    }

    public void openAllBags() {
        for (Player player : Draft.Game.getRealPlayers()){
            openBagForPlayer(player);
        }
    }

    public void openBagForPlayer(Player player) {
        int bagIdx = draftOrder.indexOf(player.getUserID());
        bagIdx += PassCount;
        bagIdx %= draftOrder.size();

        String draftingBagId = draftOrder.get(bagIdx);
        Bags.get(draftingBagId).openBagToPlayer(player);
    }

    @Override
    public boolean processCommandString(String command) {
        return false;
    }


    private void generateBags() {
        MessageHelper.sendMessageToChannel(Draft.Game.getMainGameChannel(), "Setting up tiles...");
        MiltyDraftManager draftManager = Draft.Game.getMiltyDraftManager();
        new StartMilty().initDraftTiles(draftManager);

        MessageHelper.sendMessageToChannel(Draft.Game.getMainGameChannel(), "Collecting components...");

        Map<DraftItem.Category, List<DraftItem>> allDraftItems;
        allDraftItems = FrankenUtils.generateAllCards(FrankenUtils.getDraftableFactionsForGame(Draft.Game));
        allDraftItems.put(DraftItem.Category.DRAFTORDER, SpeakerOrderDraftItem.buildAllDraftableItems(Draft.Game));
        allDraftItems.put(DraftItem.Category.REDTILE, RedTileDraftItem.buildAllDraftableItems(draftManager));
        allDraftItems.put(DraftItem.Category.BLUETILE, BlueTileDraftItem.buildAllDraftableItems(draftManager));

        StringBuilder componentLimits = new StringBuilder();
        for (Map.Entry<DraftItem.Category, Integer> limit : CategoryLimits.entrySet()) {
            componentLimits.append(limit.getValue()).append(" ").append(limit.getKey()).append(" cards\n");
        }

        MessageHelper.sendMessageToChannel(Draft.Game.getMainGameChannel(), "Sorting components into bags." +
                " Each bag will contain the following:\n" + componentLimits);

        for (Player player : Draft.Game.getRealPlayers()){
            DraftBag bag = new DraftBag();
            bag.Name = player.getColor() + " bag";
            bag.Draft = Draft;

            // Walk through each type of draftable...
            for (Map.Entry<DraftItem.Category, List<DraftItem>> draftableCollection : allDraftItems.entrySet()) {
                DraftItem.Category category = draftableCollection.getKey();
                int categoryLimit = CategoryLimits.get(category);
                // ... and pull out the appropriate number of items from its collection...
                for (int j = 0; j < categoryLimit; j++) {
                    // ... and add it to the bag.
                    bag.Contents.add(draftableCollection.getValue().remove(0));
                }
            }

            String userID = player.getUserID();
            Bags.put(userID, bag);
            Queues.put(userID, new ArrayList<>());
            draftOrder.add(userID);
        }
    }
}
