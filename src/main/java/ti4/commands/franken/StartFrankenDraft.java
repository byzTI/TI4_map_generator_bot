package ti4.commands.franken;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.draft.BagDraft;
import ti4.draft.phases.FrankenDraftCardsPhase;
import ti4.helpers.ButtonHelper;
import ti4.helpers.Constants;
import ti4.map.Game;
import ti4.map.GameSaveLoadManager;

import java.util.Map;

import static ti4.draft.DraftItem.Category.*;

public class StartFrankenDraft extends FrankenSubcommandData {
    public StartFrankenDraft() {
        super(Constants.START_FRANKEN_DRAFT, "Start a franken draft");
        addOptions(new OptionData(OptionType.BOOLEAN, Constants.POWERED, "'True' to add 1 extra faction tech/ability (Default: False)"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game activeGame = getActiveGame();
        if(activeGame.getRealPlayers().size() < (activeGame.getPlayers().size()-2)){
            ButtonHelper.setUpFrankenFactions(activeGame, event);
        }

        BagDraft newDraft = new BagDraft();
        var cardDraft = new FrankenDraftCardsPhase();
        cardDraft.CategoryLimits = Map.ofEntries(Map.entry(ABILITY, 4),
                Map.entry(TECH, 4),
                Map.entry(BLUETILE, 3),
                Map.entry(AGENT, 2),
                Map.entry(COMMANDER, 2),
                Map.entry(HERO, 2),
                Map.entry(STARTINGTECH, 2),
                Map.entry(STARTINGFLEET, 2),
                Map.entry(HOMESYSTEM, 2),
                Map.entry(PN, 2),
                Map.entry(COMMODITIES, 2),
                Map.entry(MECH, 2),
                Map.entry(REDTILE, 2),
                Map.entry(FLAGSHIP, 2),
                Map.entry(DRAFTORDER, 1));
        newDraft.queuePhase(cardDraft);
        activeGame.setBagDraft(newDraft);

        newDraft.startNextPhase();
        GameSaveLoadManager.saveMap(activeGame, event);
    }
}
