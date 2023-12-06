package ti4.draft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import ti4.AsyncTI4DiscordBot;
import ti4.helpers.Constants;
import ti4.map.Game;
import ti4.map.GameManager;
import ti4.message.BotLogger;

import java.util.ArrayList;
import java.util.List;

public class DraftBag {

    public String type;
    public String label;
    private BagDraft draft;
    private String gameId;
    private String threadId;

    public DraftBag() {
    }

    public DraftBag(String type, String label, String gameId) {
        setup(type, label,gameId);
    }

    public void setup(String type, String label, String gameId) {
        this.type = type;
        this.label = label;
        this.gameId = gameId;
    }

    public List<DraftItem> Contents = new ArrayList<>();

    public String toStoreString()
    {
        if (Contents.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(type).append("|").append(label).append("|").append(threadId).append("|");
        for (DraftItem item: Contents) {
            sb.append(item.getAlias());
            sb.append(",");
        }

        return sb.toString();
    }

    public static DraftBag fromStoreString(String storeString, String gameId) {
        String[] split = storeString.split("\\|");
        if (split.length == 1) {
            String cardList = split[0];
            split = new String[4];
            split[0] = "Bag";
            split[1] = "Label";
            split[2] = "";
            split[3] = cardList;
        }
        DraftBag bag = new DraftBag(split[0], split[1], gameId);
        bag.threadId = split[2];
        String[] cards = split[3].split(",");
        for (String card : cards) {
            bag.Contents.add(DraftItem.GenerateFromAlias(card));
        }

        return bag;
    }

    public int getCategoryCount(DraftItem.Category cat) {
        int count = 0;
        for (DraftItem item: Contents) {
            if (item.ItemCategory == cat) {
                count++;
            }
        }
        return count;
    }

    @JsonIgnore
    public String getDiscordThreadName() {
        return Constants.BAG_INFO_THREAD_PREFIX + type + " " + label + " - " + gameId;
    }

    @JsonIgnore
    public ThreadChannel getDiscordThread() {
        Game game = GameManager.getInstance().getGame(gameId);
        TextChannel actionsChannel = game.getMainGameChannel();
        if (actionsChannel == null) {
            BotLogger.log("`Helper.getBagChannel`: actionsChannel is null for game, or community game private channel not set: " + game.getName());
            return null;
        }

        String threadName = getDiscordThreadName();
        String bagInfoThread = threadId;
        try {
            if (bagInfoThread != null && !bagInfoThread.isBlank() && !bagInfoThread.isEmpty() && !"null".equals(bagInfoThread)) {
                List<ThreadChannel> threadChannels = actionsChannel.getThreadChannels();
                if (threadChannels == null) return null;

                ThreadChannel threadChannel = AsyncTI4DiscordBot.jda.getThreadChannelById(bagInfoThread);
                if (threadChannel != null) return threadChannel;

                // SEARCH FOR EXISTING OPEN THREAD
                for (ThreadChannel threadChannel_ : threadChannels) {
                    if (threadChannel_.getId().equals(bagInfoThread)) {
                        threadId = threadChannel_.getId();
                        return threadChannel_;
                    }
                }

                // SEARCH FOR EXISTING CLOSED/ARCHIVED THREAD
                List<ThreadChannel> hiddenThreadChannels = actionsChannel.retrieveArchivedPrivateThreadChannels().complete();
                for (ThreadChannel threadChannel_ : hiddenThreadChannels) {
                    if (threadChannel_.getId().equals(bagInfoThread)) {
                        threadId = threadChannel_.getId();
                        return threadChannel_;
                    }
                }
            }
        } catch (Exception e) {
            BotLogger.log("`Player.getBagInfoThread`: Could not find existing Bag Info thead using ID: " + bagInfoThread + " for potential thread name: " + threadName, e);
        }
        //ATTEMPT TO FIND BY NAME
        try {
            List<ThreadChannel> threadChannels = actionsChannel.getThreadChannels();
            if (threadChannels == null) return null;

            if (bagInfoThread != null) {
                ThreadChannel threadChannel = AsyncTI4DiscordBot.jda.getThreadChannelById(bagInfoThread);
                if (threadChannel != null) return threadChannel;
            }

            // SEARCH FOR EXISTING OPEN THREAD
            for (ThreadChannel threadChannel_ : threadChannels) {
                if (threadChannel_.getName().equals(threadName)) {
                    threadId = threadChannel_.getId();
                    return threadChannel_;
                }
            }

            // SEARCH FOR EXISTING CLOSED/ARCHIVED THREAD
            List<ThreadChannel> hiddenThreadChannels = actionsChannel.retrieveArchivedPrivateThreadChannels().complete();
            for (ThreadChannel threadChannel_ : hiddenThreadChannels) {
                if (threadChannel_.getName().equals(threadName)) {
                    threadId = threadChannel_.getId();
                    return threadChannel_;
                }
            }
        } catch (Exception e) {
            BotLogger.log("`Player.getBagInfoThread`: Could not find existing Bag Info thead using name: " + threadName, e);
        }
        return null;
    }

    public ThreadChannel createDiscordThread() {
        Game game = GameManager.getInstance().getGame(gameId);
        TextChannel actionsChannel = game.getMainGameChannel();

        boolean isPrivateChannel = true;
        ThreadChannelAction threadAction = actionsChannel.createThreadChannel(getDiscordThreadName(), isPrivateChannel);
        threadAction.setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_24_HOURS);
        if (isPrivateChannel) {
            threadAction.setInvitable(false);
        }
        ThreadChannel threadChannel = threadAction.complete();
        threadId = threadChannel.getId();
        return threadChannel;
    }

}
