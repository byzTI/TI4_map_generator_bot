package ti4.draft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ti4.helpers.Constants;
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


    public String toStoreString()
    {
        StringBuilder sb = new StringBuilder();
        for (DraftItem item: Contents) {
            sb.append(item.getAlias());
            sb.append(",");
        }

        return sb.toString();
    }

    @JsonIgnore
    public int getCategoryCount(DraftItem.Category cat) {
        int count = 0;
        for (DraftItem item: Contents) {
            if (item.ItemCategory == cat) {
                count++;
            }
        }
        return count;
    }


    public RestAction<Void> addCardAsync(DraftItem card) {
        Contents.add(card);
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
        return thread.sendMessage(sb.toString()).and(thread.getParentChannel().asTextChannel().sendTyping());
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
        RestAction action = channel.sendMessage("# " + Name);
        for (DraftItem item : Contents) {
            StringBuilder sb = new StringBuilder();
            try {
                sb.append("### ").append(item.getItemEmoji()).append(" ");
                sb.append(item.getShortDescription()).append("\n - ");
                sb.append(item.getLongDescription());
            } catch (Exception e) {
                sb.append("ERROR BUILDING DESCRIPTION FOR ").append(item.getAlias());
            }
            action = action.and(channel.sendMessage(sb.toString()).onSuccess(message -> item.messageId = message.getId()));
            action = action.and(channel.getParentChannel().asTextChannel().sendTyping());
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
            channel.addThreadMemberById(player.getUserID())
        );
    }

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
        threadCreationAction.setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_24_HOURS);
        return threadCreationAction;
    }

    @NotNull
    @JsonIgnore
    private String getChannelName() {
        return Constants.BAG_INFO_THREAD_PREFIX + Draft.Game.getName() + "-" + Name;
    }
}
