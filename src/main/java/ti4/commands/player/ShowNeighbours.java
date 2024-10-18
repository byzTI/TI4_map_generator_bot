package ti4.commands.player;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.helpers.Constants;
import ti4.helpers.Helper;
import ti4.helpers.NeighbourHelper;
import ti4.map.Game;
import ti4.map.Player;
import ti4.message.MessageHelper;

public class ShowNeighbours extends PlayerSubcommandData{

    public ShowNeighbours() {
        super(Constants.SHOW_NEIGHBOURS, "Show information about a player's neighbours");
        addOptions(new OptionData(OptionType.STRING, Constants.FACTION_COLOR, "Faction or Color for which you set stats").setAutoComplete(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game game = getActiveGame();
        Player player = game.getPlayer(getUser().getId());
        player = Helper.getGamePlayer(game, player, event, null);
        player = Helper.getPlayer(game, player, event);
        if (player == null) {
            MessageHelper.sendMessageToEventChannel(event, "Player could not be found");
            return;
        }

        MessageHelper.sendMessageToEventChannel(event, NeighbourHelper.getNeighbourDistanceMap(player, 10).toString());

        StringBuilder sb = new StringBuilder();
        for (Player otherPlayer : player.getOtherRealPlayers()) {
            sb.append("Paths to ").append(otherPlayer.getFactionEmoji()).append(":\n");
            for (String path : NeighbourHelper.getNeighbourPathsTo(player, otherPlayer)) {
                sb.append("> ").append(path).append("\n");
            }
        }
        MessageHelper.sendMessageToEventChannel(event, sb.toString());
    }
    
}
