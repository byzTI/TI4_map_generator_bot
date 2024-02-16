package ti4.draft.phases;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.RestAction;
import ti4.commands.milty.MiltyDraftManager;
import ti4.commands.milty.StartMilty;
import ti4.draft.*;
import ti4.draft.items.*;
import ti4.map.GameSaveLoadManager;
import ti4.map.Player;
import ti4.message.MessageHelper;

import java.util.*;

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
    public boolean processCommandString (Player player, String commandString, IReplyCallback replyCallback) {
        String[] splitCommand = commandString.split("_");
        DraftBag bag = getCurrentlyDraftingBag(player);
        switch (splitCommand[0]) {
            case "queue":
                if (bag.queueItemForDraft(splitCommand[1])) {
                    bag.refreshDisplays(player).queue();
                }
                break;
            case "dequeue":
                if (bag.dequeueItemForDraft(splitCommand[1])) {
                    bag.refreshDisplays(player).queue();
                }
                break;
        }
        return false;
    }

    @Override
    public void onPhaseEnd() {

    }

    @Override
    public void onPhaseStart() {
        generateBags();

        Collection<RestAction<Void>> setupActions = new ArrayList<>();
        for (Player p : Draft.Game.getRealPlayers()) {
            DraftBag bag = Bags.get(p.getUserID());

            RestAction<ThreadChannel> threadGenerator;
            if (bag.getExistingThread() != null) {
                threadGenerator = bag.getThread().delete().flatMap(unused -> bag.createThread());
            }
            else {
                threadGenerator = bag.createThread();
            }

            RestAction<Void> bagPopulator = threadGenerator.flatMap(threadChannel -> bag.populateBagThreadAsync());
            bagPopulator = bagPopulator.onSuccess(unused -> openBagForPlayer(p));
            setupActions.add(bagPopulator);

            DraftItemCollection draftHand = p.getDraftHand();
            draftHand.Draft = Draft;
            draftHand.Name = p.getUserName() + " Hand";
            if (draftHand.getExistingThread() != null) {
                threadGenerator = draftHand.getThread().delete().flatMap(unused -> draftHand.createThread());
            }
            else {
                threadGenerator = draftHand.createThread();
            }

            RestAction<Void> handPopulator = threadGenerator.flatMap(threadChannel -> draftHand.populateBagThreadAsync());
            handPopulator = handPopulator.onSuccess(unused -> openHandForPlayer(p));
            setupActions.add(handPopulator);
        }
        RestAction.allOf(setupActions).onSuccess(unused -> GameSaveLoadManager.saveMap(Draft.Game)).queue();
        Draft.Game.getMainGameChannel().sendMessage("*Bags are being distributed. Please select 3 cards to draft. " +
                "After this, you will select at most 2 cards each round.*").queue(message -> lastPassMessageId = message.getId());
    }

    public void passBags() {
        PassCount++;
        Bags.values().forEach(bag -> bag.QueueLimit = 2);
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
        DraftBag bag = getCurrentlyDraftingBag(player);
        bag.showBagToPlayer(player).queue();
    }

    @JsonIgnore
    private DraftBag getCurrentlyDraftingBag(Player player) {
        int bagIdx = draftOrder.indexOf(player.getUserID());
        bagIdx += PassCount;
        bagIdx %= draftOrder.size();

        String draftingBagId = draftOrder.get(bagIdx);
        return Bags.get(draftingBagId);
    }

    public void openHandForPlayer(Player player) {
        player.getDraftHand().showBagToPlayer(player).queue();
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
            bag.QueueLimit = 3;

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
