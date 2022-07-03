package ti4.commands.player;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.helpers.Constants;
import ti4.helpers.Helper;
import ti4.map.Map;
import ti4.map.Player;
import ti4.message.MessageHelper;

public class SendCommodities extends PlayerSubcommandData {
    public SendCommodities() {
        super(Constants.SEND_COMMODITIES, "Sent Commodities to player/faction");
        addOptions(new OptionData(OptionType.INTEGER, Constants.COMMODITIES, "Commodities count").setRequired(true));
        addOptions(new OptionData(OptionType.STRING, Constants.FACTION_COLOR, "Faction or Color to which you send Commodities").setAutoComplete(true).setRequired(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {

        Map activeMap = getActiveMap();
        Player player = activeMap.getPlayer(getUser().getId());
        if (player == null) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "Player could not be found");
            return;
        }
        Player player_ = Helper.getPlayer(activeMap, player, event);
        if (player_ == null) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "Player to send TG/Commodities could not be found");
            return;
        }
        
        OptionMapping optionComms = event.getOption(Constants.COMMODITIES);
        if (optionComms != null) {
            int sendCommodities = optionComms.getAsInt();
            int commodities = player.getCommodities();
            sendCommodities = Math.min(sendCommodities, commodities);
            commodities -= sendCommodities;
            player.setCommodities(commodities);

            int targetTG = player_.getTg();
            targetTG += sendCommodities;
            player_.setTg(targetTG);
            MessageHelper.sendMessageToChannel(event.getChannel(), SendTG.getPlayerRepresentation(event, player) + " send " + sendCommodities + " commodities to: " + SendTG.getPlayerRepresentation(event, player_));
        }
    }
}
