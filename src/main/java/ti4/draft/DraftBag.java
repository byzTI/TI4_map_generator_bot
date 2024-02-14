package ti4.draft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import ti4.helpers.Constants;
import ti4.map.Game;
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
        ThreadChannel channel = getThread();

        channel.getIterableHistory().forEachAsync(message -> {
            channel.deleteMessageById(message.getId()).queue();
            return true;
        });

        MessageHelper.sendMessageToChannel(channel, "# " + Name);

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

    public void openBagToPlayer(Player player) {
        ThreadChannel channel = getThread();
        boolean playerAlreadyPresent = false;
        for (var member : channel.getMembers()) {
            if (member.getUser().getId().equals(player.getUserID())) {
                playerAlreadyPresent = true;
                continue;
            }
            if (member == channel.getOwner()) {
                continue;
            }
            channel.removeThreadMember(member).queue();
        }
        if (!playerAlreadyPresent) {
            channel.addThreadMemberById(player.getUserID()).queue();
        }
        channel.retrieveStartMessage().onSuccess(message ->
            message.editMessage("# " + Name + '\n' + "Currently held by: " + player.getPing()).queue()
        ).queue();
    }

    @JsonIgnore
    public ThreadChannel getThread() {
        String channelName = Constants.BAG_INFO_THREAD_PREFIX + Draft.Game.getName() + "-" + Name;

        TextChannel actionsChannel = Draft.Game.getMainGameChannel();

        List<ThreadChannel> allThreads = actionsChannel.getThreadChannels();
        for (ThreadChannel thread : allThreads) {
            if (thread.getName().equals(channelName)){
                return thread;
            }
        }

        boolean isPrivateChannel = true;
        ThreadChannelAction threadCreationAction = actionsChannel.createThreadChannel(channelName, isPrivateChannel);
        threadCreationAction.setInvitable(true);
        threadCreationAction.setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_24_HOURS);

        return threadCreationAction.complete();
    }
}
