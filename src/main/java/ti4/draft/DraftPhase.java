package ti4.draft;

import com.fasterxml.jackson.annotation.JsonBackReference;

public abstract class DraftPhase {

    @JsonBackReference
    public BagDraft Draft;

    public abstract void onPhaseEnd();
    public abstract void onPhaseStart();

    public abstract boolean processCommandString(String command);
}
