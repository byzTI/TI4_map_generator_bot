package ti4.draft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.dv8tion.jda.api.entities.ThreadMember;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ti4.map.Player;

import java.util.*;

public abstract class DraftItemCollection {

    public String Name;
    public List<DraftItem> Contents = new ArrayList<>();

    @JsonIgnore
    public BagDraft Draft;

    public String toStoreString()
    {
        StringBuilder sb = new StringBuilder();
        for (DraftItem item: Contents) {
            sb.append(item.getAlias());
            sb.append(",");
        }

        return sb.toString();
    }

    public void addCard(DraftItem card) {
        Contents.add(card);
    }

    public boolean removeCard(DraftItem card) {
        return Contents.remove(card);
    }

    public int itemCountForCategory(DraftItem.Category category) {
        return (int) Contents.stream().filter(item -> item.ItemCategory.equals(category)).count();
    }

    public abstract RestAction<Void> createDisplay();
    public abstract RestAction<Void> updateDisplay(Player viewer);
    protected abstract RestAction<Void> destroyDisplay();

    public RestAction<Void> deleteCollection() {
        Contents.clear();
        return destroyDisplay();
    }

    public RestAction<Void> giveThreadToPlayer(Player player) {
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
                .flatMap(unused -> channel.addThreadMemberById(player.getUserID()));
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
