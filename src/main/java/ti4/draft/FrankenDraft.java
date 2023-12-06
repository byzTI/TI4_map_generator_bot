package ti4.draft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ti4.commands.milty.MiltyDraftManager;
import ti4.commands.milty.MiltyDraftTile;
import ti4.commands.milty.StartMilty;
import ti4.generator.Mapper;
import ti4.map.Game;
import ti4.message.BotLogger;
import ti4.model.DraftErrataModel;
import ti4.model.FactionModel;

import java.util.*;

public class FrankenDraft extends BagDraft {
    public FrankenDraft(Game owner) {
        super(owner);
    }

    @Override
    public int getItemLimitForCategory(DraftItem.Category category) {
        int limit = 0;
        switch (category) {
            case ABILITY, BLUETILE -> {
                limit = 3;
            }
            case TECH, REDTILE, STARTINGFLEET, STARTINGTECH, HOMESYSTEM, PN, COMMODITIES, FLAGSHIP, MECH, HERO, COMMANDER, AGENT -> {
                limit = 2;
            }
            case DRAFTORDER -> {
                limit = 1;
            }
        }
        return limit;
    }

    @Override
    public String getSaveString() {
        return "franken";
    }


    @Override
    public int getBagSize() {
        return 31;
    }
}
