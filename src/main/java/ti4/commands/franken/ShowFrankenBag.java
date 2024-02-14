package ti4.commands.franken;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import ti4.draft.phases.FrankenDraftCardsPhase;
import ti4.helpers.Constants;
import ti4.helpers.BagDraftHelper;
import ti4.map.Game;
import ti4.map.Player;

public class ShowFrankenBag extends FrankenSubcommandData {
    public ShowFrankenBag() {
        super(Constants.SHOW_BAG, "Shows your current FrankenDraft bag");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game activeGame = getActiveGame();
        var draft = activeGame.getActiveBagDraft();
        if (draft == null || !(draft.CurrentPhase instanceof FrankenDraftCardsPhase phase)) {
            event.reply("You are not currently drafting cards, so you don't have a bag to show.").queue();
            return;
        }
        Player player = activeGame.getPlayer(getUser().getId());
        phase.openBagForPlayer(player);
    }
}
