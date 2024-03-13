package ti4.draft;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import ti4.draft.phases.FrankenDraftCardsPhase;
import ti4.draft.phases.FrankenSetupPhase;
import ti4.map.Player;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = FrankenSetupPhase.class, name = "FrankenSetup"),
        @JsonSubTypes.Type(value = FrankenDraftCardsPhase.class, name = "FrankenDraftCards") }
)
public abstract class DraftPhase {

    @JsonBackReference("draft")
    public BagDraft Draft;

    public abstract void onPhaseEnd();
    public abstract void onPhaseStart();

    public abstract boolean processCommandString(Player player, String commandString, IReplyCallback replyCallback);
}
