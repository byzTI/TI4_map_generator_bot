package ti4.commands.explore;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.generator.Mapper;
import ti4.helpers.Constants;
import ti4.map.Player;
import ti4.message.MessageHelper;

import java.util.List;

public class DrawSpecificRelic extends GenericRelicAction {

    public DrawSpecificRelic() {
        super(Constants.RELIC_DRAW_SPECIFIC, "Draw a specific relic", true);
        addOptions(new OptionData(OptionType.STRING, Constants.RELIC, "Relic to exhaust").setAutoComplete(true).setRequired(true));
        addOptions(new OptionData(OptionType.USER, Constants.PLAYER, "Player for which you set up faction").setRequired(false));
    }

    @Override
    public void doAction(Player player, SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption(Constants.RELIC);
        if (option == null) {
            MessageHelper.replyToMessage(event, "Specify relic");
            return;
        }
        String relicId = option.getAsString();
        List<String> allRelics = getActiveMap().getAllRelics();
        if (allRelics.contains(relicId)) {
            allRelics.remove(relicId);
            player.addRelic(relicId);
            String[] relicData = Mapper.getRelic(relicId).split(";");
            String relicString = "Relic: " + relicData[0] + " - " + relicData[1];
            MessageHelper.replyToMessage(event, relicString);
        } else {
            MessageHelper.replyToMessage(event, "Invalid relic or relic not present in deck");
        }
    }
}
