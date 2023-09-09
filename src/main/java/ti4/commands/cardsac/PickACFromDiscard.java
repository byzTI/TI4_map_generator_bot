package ti4.commands.cardsac;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.generator.Mapper;
import ti4.helpers.Constants;
import ti4.helpers.Helper;
import ti4.map.Game;
import ti4.map.Player;
import ti4.message.MessageHelper;

public class PickACFromDiscard extends ACCardsSubcommandData {
    public PickACFromDiscard() {
        super(Constants.PICK_AC_FROM_DISCARD, "Pick an Action Card from discard pile into your hand");
        addOptions(new OptionData(OptionType.INTEGER, Constants.ACTION_CARD_ID, "Action Card ID that is sent between ()").setRequired(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game activeGame = getActiveMap();
        Player player = activeGame.getPlayer(getUser().getId());
        player = Helper.getGamePlayer(activeGame, player, event, null);
        if (player == null) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "Player could not be found");
            return;
        }
        OptionMapping option = event.getOption(Constants.ACTION_CARD_ID);
        if (option == null) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "Please select what Action Card to draw from discard pile");
            return;
        }

        int acIndex = option.getAsInt();
        String acID = null;
        for (java.util.Map.Entry<String, Integer> so : activeGame.getDiscardActionCards().entrySet()) {
            if (so.getValue().equals(acIndex)) {
                acID = so.getKey();
            }
        }

        if (acID == null) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "No such Action Card ID found, please retry");
            return;
        }
        boolean picked = activeGame.pickActionCard(player.getUserID(), acIndex);
        if (!picked) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "No such Action Card ID found, please retry");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Game: ").append(activeGame.getName()).append(" ");
        sb.append("Player: ").append(player.getUserName()).append("\n");
        sb.append("Picked card from Discards: ");
        sb.append(Mapper.getActionCard(acID).getRepresentation()).append("\n");
        MessageHelper.sendMessageToChannel(event.getChannel(), sb.toString());

        ACInfo.sendActionCardInfo(activeGame, player);
    }
}
