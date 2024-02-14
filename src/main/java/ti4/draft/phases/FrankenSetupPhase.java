package ti4.draft.phases;

import ti4.draft.DraftPhase;

public class FrankenSetupPhase extends DraftPhase {
    @Override
    public void onPhaseEnd() {

    }

    @Override
    public void onPhaseStart() {

    }

    @Override
    public boolean processCommandString(String command) {
        return false;
    }

    /**
     * Old rules:
     * Franken:
     *             case ABILITY, BLUETILE -> limit = 3;
     *             case TECH, REDTILE, STARTINGFLEET, STARTINGTECH, HOMESYSTEM, PN, COMMODITIES, FLAGSHIP, MECH, HERO, COMMANDER, AGENT -> limit = 2;
     *             case DRAFTORDER -> limit = 1;
     *
     * PoweredFranken:
     *             case ABILITY -> limit = 4;
     *             case TECH, BLUETILE -> limit = 3;
     *             case AGENT, COMMANDER, REDTILE, STARTINGFLEET, STARTINGTECH, HOMESYSTEM, PN, COMMODITIES, FLAGSHIP, MECH, HERO -> limit = 2;
     *             case DRAFTORDER -> limit = 1;
     */
}
