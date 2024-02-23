package ti4.draft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;
import ti4.helpers.Constants;
import ti4.map.Player;

public class DraftHand extends DraftItemCollection {
    @Override
    public RestAction<Void> createDisplay() {
        return new EmptyRestAction();
    }

    @Override
    public RestAction<Void> updateDisplay(Player viewer) {
        return new EmptyRestAction();
    }

    @Override
    protected RestAction<Void> destroyDisplay() {
        return new EmptyRestAction();
    }

    @NotNull
    @Override
    @JsonIgnore
    protected ThreadChannel.AutoArchiveDuration getArchiveDuration() {
        return ThreadChannel.AutoArchiveDuration.TIME_1_WEEK;
    }

    @JsonIgnore
    @NotNull
    @Override
    protected String getChannelName() {
        return Constants.DRAFT_HAND_THREAD_PREFIX + Draft.Game.getName() + "-" + Name;
    }
}
