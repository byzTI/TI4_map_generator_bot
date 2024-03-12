package ti4.draft.phases;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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

public class FrankenDraftCardsPhase extends ItemDraftPhase {

    public FrankenDraftCardsPhase() {}
    @JsonInclude
    @JsonProperty
    private List<String> draftOrder = new ArrayList<>();

    @JsonInclude
    @JsonProperty
    private String lastPassMessageId;

    @Override
    public boolean processCommandString (Player player, String commandString, IReplyCallback replyCallback) {
        String[] splitCommand = commandString.split(";");
        DraftBag bag = getCurrentlyDraftingBag(player);
        DraftItem item = splitCommand.length > 1 ? DraftItem.GenerateFromAlias(splitCommand[1]) : null;
        switch (splitCommand[0]) {
            case "queue":
                if (item != null && bag.queueItemForDraft(splitCommand[1])) {
                    bag.updateCategoryDisplay(item.ItemCategory, player)
                            .and(bag.getFooterUpdateAction(player))
                            .onSuccess(unused -> GameSaveLoadManager.saveMap(Draft.Game))
                            .queue();
                    if (bag.queuedItems.size() >= bag.QueueLimit) {
                        bag.updateDisplay(player).onSuccess(unused -> GameSaveLoadManager.saveMap(Draft.Game)).queue();
                    }
                }
                break;
            case "dequeue":
                boolean wasAtLimit = bag.queuedItems.size() >= bag.QueueLimit;
                if (item != null && bag.dequeueItemForDraft(splitCommand[1])) {
                    bag.updateCategoryDisplay(item.ItemCategory, player)
                            .and(bag.getFooterUpdateAction(player))
                            .onSuccess(unused -> GameSaveLoadManager.saveMap(Draft.Game))
                            .queue();
                    if (wasAtLimit && bag.queuedItems.size() < bag.QueueLimit) {
                        bag.updateDisplay(player).onSuccess(unused -> GameSaveLoadManager.saveMap(Draft.Game)).queue();
                    }
                }
                break;
            case "reset-queue":
                ReadyFlags.put(player.getUserID(), false);
                bag.resetDraftQueue();
                bag.updateDisplay(player)
                        .onSuccess(unused -> GameSaveLoadManager.saveMap(Draft.Game)).queue();
                break;
            case "confirm-queue":
                ReadyFlags.put(player.getUserID(), true);
                tryPass();
                break;
            case "unconfirm-queue":
                ReadyFlags.put(player.getUserID(), false);
                break;
        }
        return false;
    }

    private void tryPass() {
        for (Player p : Draft.Game.getRealPlayers()) {
            if (!ReadyFlags.get(p.getUserID())) {
                return;
            }
        }
        passBags();
    }

    @Override
    public void onPhaseEnd() {

    }

    @Override
    public void onPhaseStart() {
        generateBags();
        for (Player p : Draft.Game.getRealPlayers()) {
            p.setDraftHand(new DraftHand(Draft.Game));
        }

        Collection<RestAction<Void>> setupActions = new ArrayList<>();
        TextChannel mainGameChannel = Draft.Game.getMainGameChannel();
        for (Player p : Draft.Game.getRealPlayers()) {
            DraftBag bag = Bags.get(p.getUserID());

            setupActions.add(bag.createDisplay()
                    .flatMap(unused -> mainGameChannel.sendTyping())
                    .flatMap(unused -> bag.removeAllPlayersFromThread())
                    .flatMap(unused -> bag.updateDisplay(p))
                    .flatMap(unused -> bag.giveThreadToPlayer(p)));
        }
        RestAction.allOf(setupActions).onSuccess(unused -> GameSaveLoadManager.saveMap(Draft.Game)).queue();
        mainGameChannel.sendMessage("*Bags are being distributed. Please select 3 cards to draft. " +
                "After this, you will select at most 2 cards each round.*").queue(message -> lastPassMessageId = message.getId());
        mainGameChannel.sendTyping();
    }

    public void passBags() {
        for (Player player : Draft.Game.getRealPlayers()) {
            DraftBag bag = getCurrentlyDraftingBag(player);
            for (DraftItem item : bag.queuedItems) {
                player.getDraftHand().addCard(item);
                bag.removeCard(item);
            }
            bag.queuedItems.clear();
        }
        PassCount++;
        Bags.values().forEach(bag -> bag.QueueLimit = 2);
        Draft.Game.getMainGameChannel().sendMessage("*Bags are being passed. Please select 2 cards to draft. " +
                "This was the " + FrankenUtils.IntToOrdinal(PassCount) + " round of cards.*").queue(message -> lastPassMessageId = message.getId());

        for (Player player : Draft.Game.getRealPlayers()){
            DraftBag bag = getCurrentlyDraftingBag(player);
            bag.removeAllPlayersFromThread().flatMap(unused -> bag.updateDisplay(player)).flatMap(unused -> bag.giveThreadToPlayer(player)).queue();
        }
    }

    public void openBagForPlayer(Player player) {
        DraftBag bag = getCurrentlyDraftingBag(player);
        bag.removeAllPlayersExcept(player).flatMap(unused -> bag.updateDisplay(player)).flatMap(unused -> bag.giveThreadToPlayer(player)).queue();
    }

    @JsonIgnore
    private DraftBag getCurrentlyDraftingBag(Player player) {
        int bagIdx = draftOrder.indexOf(player.getUserID());
        bagIdx += PassCount;
        bagIdx %= draftOrder.size();

        String draftingBagId = draftOrder.get(bagIdx);
        return Bags.get(draftingBagId);
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
            DraftBag bag = new DraftBag(Draft.Game);
            bag.Name = player.getColor() + " bag";
            bag.QueueLimit = 3;
            bag.draftPhase = this;

            // Walk through each type of draftable...
            for (Map.Entry<DraftItem.Category, List<DraftItem>> draftableCollection : allDraftItems.entrySet()) {
                DraftItem.Category category = draftableCollection.getKey();
                int categoryLimit = CategoryLimits.get(category);
                // ... and pull out the appropriate number of items from its collection...
                for (int j = 0; j < categoryLimit; j++) {
                    // ... and add it to the bag.
                    bag.addCard(draftableCollection.getValue().remove(0));
                }
            }

            String userID = player.getUserID();
            Bags.put(userID, bag);
            Queues.put(userID, new ArrayList<>());
            ReadyFlags.put(userID, false);
            draftOrder.add(userID);
        }
    }
}
