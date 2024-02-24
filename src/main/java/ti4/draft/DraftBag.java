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
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
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
        return action.flatMap(unused -> createThread()).flatMap(unused -> Draft.Game.getMainGameChannel().sendTyping()).flatMap(unused -> setupThreadPosts());
    }

    private RestAction<Void> setupThreadPosts() {
        Map<DraftItem.Category, List<DraftItem>> sortedItems = new HashMap<>();
        for (DraftItem item : Contents) {
            if (!sortedItems.containsKey(item.ItemCategory)) {
                sortedItems.put(item.ItemCategory, new ArrayList<>());
            }
            sortedItems.get(item.ItemCategory).add(item);
        }

        ThreadChannel thread = getThread();
        RestAction<Void> combinedUpdateAction = getHeaderUpdateAction().and(thread.sendTyping());

        for (var entry : sortedItems.entrySet()) {
            combinedUpdateAction = combinedUpdateAction.flatMap(unused -> updateCategoryDisplay(entry.getKey(), null)).flatMap(unused -> thread.sendTyping());
        }

        combinedUpdateAction = combinedUpdateAction.flatMap(unused -> getFooterUpdateAction(null)).and(thread.sendTyping());

        return combinedUpdateAction;
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

        getHeaderUpdateAction().queue();
        getFooterUpdateAction(viewer).queue();
        for (var entry : sortedItems.entrySet()) {
            updateCategoryDisplay(entry.getKey(), viewer).and(thread.sendTyping()).queue();
        }

        return new EmptyRestAction();
    }

    @NotNull
    @JsonIgnore
    public RestAction<Message> getFooterUpdateAction(Player viewer) {
        ThreadChannel thread = getThread();
        RestAction<Message> footerUpdateAction;
        StringBuilder footerText = new StringBuilder();
        footerText.append("# Please select ");
        if (queuedItems.isEmpty()) {
            footerText.append(QueueLimit).append(" items to draft.");
        }
        else {
            footerText.append(QueueLimit - queuedItems.size()).append(" more items.\n");
            footerText.append("**You are currently drafting:**\n");
            for(DraftItem item:queuedItems) {
                footerText.append("- ");
                footerText.append(item.getItemEmoji()).append(" ");
                footerText.append(item.getShortDescription());
                footerText.append("\n");
            }
        }

        Button resetButton = Button.of(ButtonStyle.DANGER, BagDraft.COMMAND_PREFIX + "reset_queue", "Reset")
                .withEmoji(Emoji.fromUnicode("U+267B"));

        Button confirmButton = Button.of(ButtonStyle.SUCCESS, BagDraft.COMMAND_PREFIX + "confirm_queue", "Confirm Draft and Pass")
                .withEmoji(Emoji.fromUnicode("U+1F44D"));

        List<Button> buttons = new ArrayList<>();
        if (!isAnythingDraftable(viewer)) {
            buttons.add(resetButton);
        }
        if (!queuedItems.isEmpty()) {
            buttons.add(confirmButton);
        }

        if (footerMessageId == null) {
            MessageCreateBuilder builder = new MessageCreateBuilder().addContent(footerText.toString());
            if (!buttons.isEmpty()) {
                builder.setActionRow(buttons);
            }
            footerUpdateAction = thread.sendMessage(builder.build());
        }
        else {
            MessageEditBuilder builder = new MessageEditBuilder().setContent(footerText.toString());
            if (!buttons.isEmpty()) {
                builder.setActionRow(buttons);
            }
            footerUpdateAction = thread.editMessageById(footerMessageId, builder.build());
        }
        return footerUpdateAction.onSuccess(message -> footerMessageId = message.getId());
    }

    @NotNull
    @JsonIgnore
    private RestAction<Message> getHeaderUpdateAction() {
        ThreadChannel thread = getThread();
        RestAction<Message> headerUpdateAction;
        String headerContent = "# " + this.Name;
        if (headerMessageId == null) {
            headerUpdateAction = thread.sendMessage(headerContent);
        }
        else {
            headerUpdateAction = thread.editMessageById(headerMessageId, headerContent);
        }
        return headerUpdateAction.onSuccess(message -> headerMessageId = message.getId());
    }

    public RestAction<Message> updateCategoryDisplay(DraftItem.Category category, Player viewer){
        List<DraftItem> itemList = Contents.stream().filter(i -> i.ItemCategory.equals(category)).toList();

        List<Button> buttonList = new ArrayList<>();
        StringBuilder messageContentBuilder = new StringBuilder("## ");
        messageContentBuilder.append(DraftItem.CategoryDescriptionPlural(category));
        messageContentBuilder.append("\n");

        for (DraftItem item : itemList) {
            boolean itemDraftable = isItemDraftable(item, viewer);
            boolean itemQueued = isItemQueued(item);

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
            MessageEditBuilder b = new MessageEditBuilder().setContent(messageContent).setComponents(row);
            postOrEditAction = thread.editMessageById(itemMessages.get(category), b.build());
        }
        else {
            var messageBuilder = new MessageCreateBuilder().addContent(messageContent);
            messageBuilder.addComponents(row);
            postOrEditAction = thread.sendMessage(messageBuilder.build());
        }
        return thread.sendTyping().flatMap(unused -> postOrEditAction).onSuccess(message -> {
            itemMessages.put(category, message.getId());
        });
    }

    @Override
    public RestAction<Void> destroyDisplay() {
        return getExistingThread().delete();
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

    public void resetDraftQueue() {
        queuedItems.clear();
    }

    private boolean isItemDraftable(DraftItem item, Player player) {
        if (player == null) {
            return false;
        }

        if(queuedItems.size() >= QueueLimit) {
            return false;
        }

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

    public boolean isAnythingDraftable(Player player) {
        return Contents.stream().anyMatch(item -> isItemDraftable(item, player));
    }

    private boolean isItemQueued(DraftItem item) {
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
