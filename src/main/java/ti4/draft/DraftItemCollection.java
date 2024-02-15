package ti4.draft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ti4.map.Player;
import ti4.message.BotLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class DraftItemCollection {
    public String Name;
    public List<DraftItem> Contents = new ArrayList<>();

    @JsonIgnore
    public BagDraft Draft;

    @JsonIgnore
    public Player CurrentHolder;

    @JsonInclude
    protected String headerMessageId;


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
        return thread.sendMessage(sb.toString()).onSuccess(message -> card.messageId = message.getId()).and(thread.getParentChannel().asTextChannel().sendTyping());
    }

    public RestAction<Void> removeCardAsync(DraftItem card) {
        Contents.remove(card);
        ThreadChannel thread = getThread();
        if (thread == null) {
            TextChannel primaryBotLogChannel = BotLogger.getPrimaryBotLogChannel();
            return primaryBotLogChannel.sendMessage("Error removing " + card + " from draft item collection. Thread was null.").and(primaryBotLogChannel.sendTyping());
        }

        return thread.deleteMessageById(card.messageId);
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

    public CompletableFuture<Void> showBagToPlayer(Player player) {
        ThreadChannel channel = getThread();
        return channel.retrieveThreadMembers().forEachAsync(member->{
            String id = member.getUser().getId();
            if (id.equals(player.getUserID())) {
                return true;
            }
            if (member.getThread().getOwnerThreadMember() == member) {
                return true;
            }
            member.getThread().removeThreadMember(member.getUser()).queue();
            return true;
        }).thenAccept(x->
            channel.addThreadMemberById(player.getUserID()).queue(unused -> afterBagOpen(player))
        );
    }

    protected abstract void afterBagOpen(Player player);

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
