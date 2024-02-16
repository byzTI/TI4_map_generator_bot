package ti4.draft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ThreadMember;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ti4.map.Player;
import ti4.message.BotLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class DraftItemCollection {
    public class Display {
        public enum State {
            BLANK,
            DRAFTABLE,
            NOT_DRAFTABLE,
            QUEUED
        }

        public String MessageId;
        public DraftItem Item;
        public State CurrentDisplayState;

        public void updateState(State newState, ThreadChannel thread) {
            updateStateAsync(newState, thread).queue();
        }

        public RestAction updateStateAsync(State newState, ThreadChannel thread) {
            if (CurrentDisplayState == newState) {
                return new EmptyRestAction();
            }

            CurrentDisplayState = newState;

            if (CurrentDisplayState == State.BLANK) {
                return thread.editMessageComponentsById(MessageId, new ArrayList<>());
            }

            ButtonStyle style = ButtonStyle.UNKNOWN;
            String label = "";
            String action = "";
            switch (CurrentDisplayState) {
                case DRAFTABLE -> {
                    style = ButtonStyle.PRIMARY;
                    label = "Draft " + Item.getShortDescription();
                    action = "queue_" + Item.getAlias();
                }
                case QUEUED -> {
                    style = ButtonStyle.DANGER;
                    label = "Do not draft " + Item.getShortDescription();
                    action = "dequeue_" + Item.getAlias();
                }
                case NOT_DRAFTABLE -> {
                    style = ButtonStyle.SECONDARY;
                    label = "Undraftable";
                    action = "queue_" + Item.getAlias();
                }
            }

            Button button = Button.of(style, BagDraft.COMMAND_PREFIX + action, label).withEmoji(Emoji.fromFormatted(Item.getItemEmoji()));
            if (CurrentDisplayState == State.NOT_DRAFTABLE) {
                button = button.asDisabled();
            }
            return thread.editMessageComponentsById(MessageId, ActionRow.of(button));
        }
    }

    public String Name;
    public List<DraftItem> Contents = new ArrayList<>();

    @JsonIgnore
    public BagDraft Draft;

    @JsonInclude
    protected String headerMessageId;

    public List<Display> Messages = new ArrayList<>();

    public String toStoreString()
    {
        StringBuilder sb = new StringBuilder();
        for (DraftItem item: Contents) {
            sb.append(item.getAlias());
            sb.append(",");
        }

        return sb.toString();
    }

    public RestAction<Void> addCardAsync(DraftItem card) {
        Contents.add(card);
        return sendCardAsync(card);
    }

    @NotNull
    private RestAction<Void> sendCardAsync(DraftItem card) {
        ThreadChannel thread = getThread();
        if (thread == null) {
            TextChannel primaryBotLogChannel = BotLogger.getPrimaryBotLogChannel();
            return primaryBotLogChannel.sendMessage("Error adding " + card + " to draft item collection. Thread was null.").and(primaryBotLogChannel.sendTyping());
        }

        StringBuilder sb = new StringBuilder();
        try {
            sb.append("### ").append(card.getItemEmoji()).append(" ");
            sb.append(card.getShortDescription()).append("\n - ");
            sb.append(card.getLongDescription());
        } catch (Exception e) {
            sb.append("ERROR BUILDING DESCRIPTION FOR ").append(card.getAlias());
        }
        return thread.sendMessage(sb.toString()).onSuccess(message -> {
            Display display = new Display();
            display.Item = card;
            display.MessageId = message.getId();
            Messages.add(display);
        }).and(thread.getParentChannel().asTextChannel().sendTyping());
    }

    public RestAction<Void> removeCardAsync(DraftItem card) {
        Contents.remove(card);
        ThreadChannel thread = getThread();
        if (thread == null) {
            TextChannel primaryBotLogChannel = BotLogger.getPrimaryBotLogChannel();
            return primaryBotLogChannel.sendMessage("Error removing " + card + " from draft item collection. Thread was null.").and(primaryBotLogChannel.sendTyping());
        }

        for (Display d : Messages) {
            if (d.Item.equals(card)) {
                return thread.deleteMessageById(d.MessageId);
            }
        }
        return new EmptyRestAction();
    }

    public RestAction<Void> populateBagThreadAsync() {
        return showAllCardsAsync(getThread());
    }

    private RestAction<Void> showAllCardsAsync(ThreadChannel channel) {
        RestAction<Void> action = channel.sendMessage("# " + Name).onSuccess(message -> headerMessageId = message.getId()).and(channel.getParentChannel().asTextChannel().sendTyping());
        for (DraftItem item : Contents) {
            action = action.and(sendCardAsync(item));
        }

        return action;
    }

    public RestAction<Void> showBagToPlayer(Player player) {
        ThreadChannel channel = getThread();
        return channel.retrieveThreadMembers()
                .flatMap(allMembers -> {
                    Collection<RestAction<Void>> removeActions = new ArrayList<>();
                    removeActions.add(channel.sendTyping());
                    for (ThreadMember member : allMembers) {
                        String id = member.getUser().getId();
                        if (id.equals(player.getUserID())) {
                            continue;
                        }
                        if (member.getThread().getOwnerThreadMember() == member) {
                            continue;
                        }
                        removeActions.add(channel.removeThreadMember(member.getUser()));
                    }
                    return RestAction.allOf(removeActions);
                })
                .flatMap(unused -> beforeBagOpen(player))
                .flatMap(unused -> channel.addThreadMemberById(player.getUserID()))
                .flatMap(unused -> afterBagOpen(player));
    }

    protected abstract RestAction<Void> afterBagOpen(Player player);
    protected abstract RestAction<Void> beforeBagOpen(Player player);

    @JsonIgnore
    public ThreadChannel getThread() {
        ThreadChannel thread = getExistingThread();
        if (thread != null) return thread;

        return createThread().complete();
    }

    @Nullable
    @JsonIgnore
    public ThreadChannel getExistingThread() {
        TextChannel actionsChannel = Draft.Game.getMainGameChannel();

        List<ThreadChannel> allThreads = actionsChannel.getThreadChannels();
        for (ThreadChannel thread : allThreads) {
            if (thread.getName().equals(getChannelName())){
                return thread;
            }
        }
        return null;
    }

    @NotNull
    public ThreadChannelAction createThread() {
        TextChannel actionsChannel = Draft.Game.getMainGameChannel();
        boolean isPrivateChannel = true;
        ThreadChannelAction threadCreationAction = actionsChannel.createThreadChannel(getChannelName(), isPrivateChannel);
        threadCreationAction.setInvitable(false);
        threadCreationAction.setAutoArchiveDuration(getArchiveDuration());
        return threadCreationAction;
    }

    @NotNull
    @JsonIgnore
    protected abstract ThreadChannel.AutoArchiveDuration getArchiveDuration();

    @NotNull
    @JsonIgnore
    protected abstract String getChannelName();

}
