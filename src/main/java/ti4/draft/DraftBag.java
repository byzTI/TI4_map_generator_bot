package ti4.draft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;
import ti4.helpers.Constants;
import ti4.map.Player;

import java.util.ArrayList;
import java.util.List;

public class DraftBag extends DraftItemCollection {

    public List<DraftItem> queuedItems;
    public int QueueLimit;

    @Override
    protected RestAction<Void> beforeBagOpen(Player player) {
        queuedItems = new ArrayList<>();
        return refreshDisplays(player);
    }

    public RestAction<Void> refreshDisplays(Player player) {
        ThreadChannel thread = getThread();
        RestAction<Void> action = new EmptyRestAction();
        for (var item : Contents) {
            action = action.and(refreshDisplayForItem(item, player));
        }
        return action;
    }

    public RestAction<Void> refreshDisplayForItem(DraftItem item, Player player) {
        ThreadChannel thread = getThread();
        boolean canDraft = queuedItems.size() < QueueLimit;
        boolean isQueued = queuedItems.contains(item);
        Display display = Messages.get(item);
        if (display == null) {
            return this.sendCardAsync(item).flatMap(unused -> refreshDisplayForItem(item, player));
        }

        Display.State state;
        if (canDraft && !isQueued) {
            state = Display.State.DRAFTABLE;
        } else if (isQueued) {
            state = Display.State.QUEUED;
        } else {
            state = Display.State.NOT_DRAFTABLE;
        }
        return display.updateStateAsync(state, thread).and(new EmptyRestAction());
    }

    @Override
    protected RestAction<Void> afterBagOpen(Player player) {
        return new EmptyRestAction();
    }

    public boolean queueItemForDraft(String itemAlias) {
        if (queuedItems.size() >= QueueLimit) {
            return false;
        }

        for (DraftItem i : queuedItems) {
            if (i.getAlias().equals(itemAlias)) {
                return false;
            }
        }

        for (DraftItem i : Contents) {
            if (i.getAlias().equals(itemAlias)) {
                queuedItems.add(i);
                return true;
            }
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
