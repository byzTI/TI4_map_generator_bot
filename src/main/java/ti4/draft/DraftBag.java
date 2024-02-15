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
import ti4.message.MessageHelper;

import java.util.ArrayList;
import java.util.List;

public class DraftBag {
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

    public void populateBagThread() {
        showAllCards(getThread());
    }

    public RestAction<Void> populateBagThreadAsync() {
        return showAllCardsAsync(getThread());
    }


    private void showAllCards(ThreadChannel channel) {
        channel.sendMessage("# " + Name).queue();
        for (DraftItem item : Contents) {
            StringBuilder sb = new StringBuilder();
            try {
                sb.append("### ").append(item.getItemEmoji()).append(" ");
                sb.append(item.getShortDescription()).append("\n - ");
                sb.append(item.getLongDescription());
            } catch (Exception e) {
                sb.append("ERROR BUILDING DESCRIPTION FOR ").append(item.getAlias());
            }
            MessageHelper.sendMessageToChannel(channel, sb.toString());
        }
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
            action = action.and(channel.sendMessage(sb.toString()));
            action = action.and(channel.getParentChannel().asTextChannel().sendTyping());
        }

        return action;
    }

    public void openBagToPlayer(Player player) {
        ThreadChannel channel = getThread();
        channel.retrieveThreadMembers().forEachAsync(member->{
            String id = member.getUser().getId();
            if (id.equals(player.getUserID())) {
                return true;
            }
            if (member.getThread().getOwnerThreadMember() == member) {
                return true;
            }
            member.getThread().removeThreadMember(member.getUser()).queue();
            return true;
        }).thenApply(x -> {
            channel.addThreadMemberById(player.getUserID()).queue();
            return true;
        });
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
        threadCreationAction.setInvitable(true);
        threadCreationAction.setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_24_HOURS);
        return threadCreationAction;
    }

    @NotNull
    @JsonIgnore
    private String getChannelName() {
        return Constants.BAG_INFO_THREAD_PREFIX + Draft.Game.getName() + "-" + Name;
    }
}
