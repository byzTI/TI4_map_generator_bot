package ti4.draft.phases;

import ti4.draft.BagDraft;
import ti4.map.Player;

public abstract class DraftPhase {
    public BagDraft Draft;
    public abstract void StartPhase();
    public abstract void EndPhase();

    public abstract void ProcessCommand(Player player, String command, String... args);
}
