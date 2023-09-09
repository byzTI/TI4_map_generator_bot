package ti4.commands.explore;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.helpers.Constants;
import ti4.map.Game;
import ti4.message.MessageHelper;

public class ExpReset extends ExploreSubcommandData {

    public ExpReset() {
        super(Constants.RESET, "Reset the exploration decks, emptying discards and adding all cards to their respective decks.");
        addOptions(new OptionData(OptionType.STRING, Constants.CONFIRM, "Type YES").setRequired(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (event.getOption(Constants.CONFIRM).getAsString().equals("YES")) {
            Game activeGame = getActiveMap();
            activeGame.resetExplore();
            sendMessage("Exploration decks reset.");
        } else {
            sendMessage("Confirmation not received to reset exploration decks.");
            MessageHelper.sendMessageToChannel(event.getMessageChannel(), event.getOption(Constants.CONFIRM).getAsString());
        }
    }
}
