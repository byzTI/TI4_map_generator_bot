package ti4.draft;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;
import ti4.helpers.Constants;
import ti4.map.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DraftBag extends DraftItemCollection {

    @JsonBackReference
    public ItemDraftPhase draftPhase;
    public List<DraftItem> queuedItems = new ArrayList<>();
    public int QueueLimit;

    public Map<DraftItem.Category, String> itemMessages;
    public String headerMessageId;
    public String footerMessageId;

    @Override
    public RestAction<Void> createDisplay() {
        itemMessages = new HashMap<>();
        ThreadChannel existingThread = getExistingThread();
        RestAction<Void> action = Draft.Game.getMainGameChannel().sendTyping();
        if (existingThread != null) {
            action = action.flatMap(unused -> existingThread.delete());
        }
        return action.flatMap(unused -> createThread()).flatMap(unused -> Draft.Game.getMainGameChannel().sendTyping());
    }

    @Override
    public RestAction<Void> updateDisplay(Player viewer) {
        Map<DraftItem.Category, List<DraftItem>> sortedItems = new HashMap<>();
        for (DraftItem item : Contents) {
            if (!sortedItems.containsKey(item.ItemCategory)) {
                sortedItems.put(item.ItemCategory, new ArrayList<>());
            }
            sortedItems.get(item.ItemCategory).add(item);
        }

        ThreadChannel thread = getThread();
        RestAction<Void> combinedUpdateAction = new EmptyRestAction();

        RestAction<Message> headerUpdateAction;
        String headerContent = "# " + this.Name;
        if (headerMessageId == null) {
            headerUpdateAction = thread.sendMessage(headerContent);
        }
        else {
            headerUpdateAction = thread.editMessageById(headerMessageId, headerContent);
        }

        combinedUpdateAction = combinedUpdateAction.flatMap(unused -> headerUpdateAction.onSuccess(message -> headerMessageId = message.getId()).and(thread.sendTyping()));

        ArrayList<RestAction<Void>> allUpdates = new ArrayList<>();
        for (var entry : sortedItems.entrySet()) {
            DraftItem.Category category = entry.getKey();
            RestAction<Void> update = updateCategoryDisplay(category, viewer).onSuccess(message ->{
                    itemMessages.put(category, message.getId());
                    System.out.println(message.getId());
            }).and(thread.sendTyping());
            allUpdates.add(update);
        }
        combinedUpdateAction = combinedUpdateAction.flatMap(unused -> RestAction.allOf(allUpdates)).flatMap(unused -> thread.sendTyping());

        RestAction<Message> footerUpdateAction;
        StringBuilder footerBuilder = new StringBuilder();
        footerBuilder.append("Please select ");
        if (queuedItems.isEmpty()) {
            footerBuilder.append(QueueLimit).append(" items to draft.");
        }
        else {
            footerBuilder.append(QueueLimit - queuedItems.size()).append(" more items.\n");
            footerBuilder.append("You are currently drafting:\n");
            for(DraftItem item:queuedItems) {
                footerBuilder.append("- ");
                footerBuilder.append(item.getItemEmoji());
                footerBuilder.append(item.getShortDescription());
                footerBuilder.append("\n");
            }
        }
        if (footerMessageId == null) {
            footerUpdateAction = thread.sendMessage(footerBuilder.toString());
        }
        else {
            footerUpdateAction = thread.editMessageById(footerMessageId, footerBuilder.toString());
        }

        combinedUpdateAction = combinedUpdateAction.flatMap(unused ->footerUpdateAction.onSuccess(message -> footerMessageId = message.getId()).and(thread.sendTyping()));
        combinedUpdateAction = combinedUpdateAction.flatMap(unused -> giveThreadToPlayer(viewer));

        return combinedUpdateAction;
    }

    public RestAction<Message> updateCategoryDisplay(DraftItem.Category category, Player viewer){
        List<DraftItem> itemList = Contents.stream().filter(i -> i.ItemCategory.equals(category)).toList();

        List<Button> buttonList = new ArrayList<>();
        StringBuilder messageContentBuilder = new StringBuilder("## ");
        messageContentBuilder.append(DraftItem.CategoryDescriptionPlural(category));
        messageContentBuilder.append("\n");

        for (DraftItem item : itemList) {
            boolean itemDraftable = isItemDraftable(item, viewer);
            boolean itemQueued = isItemQueued(item, viewer);

            messageContentBuilder.append("- ");
            if (itemQueued) {
                messageContentBuilder.append("**");
            } else if (!itemDraftable) {
                messageContentBuilder.append("~~");
            }
            messageContentBuilder.append(item.getShortDescription());
            if (itemQueued) {
                messageContentBuilder.append("**");
            } else if (!itemDraftable) {
                messageContentBuilder.append("~~");
            }
            messageContentBuilder.append("\n");

            ButtonStyle style;
            String label;
            String action;
            if (itemQueued) {
                style = ButtonStyle.DANGER;
                label = "Do not draft " + item.getShortDescription();
                action = "dequeue_" + item.getAlias();
            } else if (itemDraftable) {
                style = ButtonStyle.PRIMARY;
                label = "Draft " + item.getShortDescription();
                action = "queue_" + item.getAlias();
            } else {
                style = ButtonStyle.SECONDARY;
                label = item.getShortDescription() + " - Undraftable";
                action = "queue_" + item.getAlias();
            }

            Button button = Button.of(style, BagDraft.COMMAND_PREFIX + action, label).withEmoji(Emoji.fromFormatted(item.getItemEmoji()));
            if (!itemDraftable && !itemQueued) {
                button = button.asDisabled();
            }
            buttonList.add(button);
        }
        ActionRow row = ActionRow.of(buttonList);

        String messageContent = messageContentBuilder.toString();

        ThreadChannel thread = getThread();
        RestAction<Message> postOrEditAction;
        if (itemMessages.containsKey(category)) {
            postOrEditAction = thread.editMessageById(itemMessages.get(category), messageContent)
                    .flatMap(message -> thread.editMessageComponentsById(message.getId(), row));
        }
        else {
            var messageBuilder = new MessageCreateBuilder().addContent(messageContent);
            messageBuilder.addComponents(row);
            postOrEditAction = thread.sendMessage(messageBuilder.build());
        }
        return postOrEditAction;
    }

    @Override
    public RestAction<Void> destroyDisplay() {
        return getExistingThread().delete().onSuccess(unused -> System.out.println("Deleted"));
    }

    public boolean queueItemForDraft(String itemAlias) {
        DraftItem item = DraftItem.GenerateFromAlias(itemAlias);
        if (queuedItems.size() >= QueueLimit) {
            return false;
        }

        if (queuedItems.contains(item)) {
            return false;
        }

        if (Contents.contains(item)) {
            queuedItems.add(item);
            return true;
        }

        return false;
    }

    public boolean dequeueItemForDraft(String itemAlias) {
        for (DraftItem i : queuedItems) {
            if (i.getAlias().equals(itemAlias)) {
                queuedItems.remove(i);
                return true;
            }
        }
        return false;
    }

    private boolean isItemDraftable(DraftItem item, Player player) {
        DraftItemCollection draftHand = player.getDraftHand();

        // Can't draft more than you're allowed
        if (draftHand.itemCountForCategory(item.ItemCategory) >= draftPhase.getLimitForCategory(item.ItemCategory)) {
            return false;
        }

        // If players haven't seen all bags, you can only draft one item per category
        if (draftPhase.PassCount < Draft.Game.getRealPlayers().size()) {
            if (queuedItems.stream().anyMatch(i -> i.ItemCategory.equals(item.ItemCategory))) {
                return false;
            }
        }
        return true;
    }

    private boolean isItemQueued(DraftItem item, Player player) {
        return queuedItems.contains(item);
    }

    @NotNull
    @Override
    @JsonIgnore
    protected ThreadChannel.AutoArchiveDuration getArchiveDuration() {
        return ThreadChannel.AutoArchiveDuration.TIME_24_HOURS;
    }

    @NotNull
    @JsonIgnore
    @Override
    protected String getChannelName() {
        return Constants.BAG_INFO_THREAD_PREFIX + Draft.Game.getName() + "-" + Name;
    }
}
