package ti4.draft;

import com.fasterxml.jackson.annotation.JsonBackReference;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import ti4.map.Player;

public abstract class DraftPhase {

    @JsonBackReference
    public BagDraft Draft;

    public abstract void onPhaseEnd();
    public abstract void onPhaseStart();

    public abstract boolean processCommandString(Player player, String commandString, IReplyCallback replyCallback);
}
