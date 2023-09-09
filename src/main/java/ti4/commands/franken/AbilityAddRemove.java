package ti4.commands.franken;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.generator.Mapper;
import ti4.helpers.Constants;
import ti4.helpers.Helper;
import ti4.map.Game;
import ti4.map.Player;

public abstract class AbilityAddRemove extends FrankenSubcommandData {
    public AbilityAddRemove(String name, String description) {
        super(name, description);
        addOptions(new OptionData(OptionType.STRING, Constants.ABILITY, "Ability Name").setRequired(true).setAutoComplete(true));
        addOptions(new OptionData(OptionType.STRING, Constants.ABILITY_1, "Ability Name").setAutoComplete(true));
        addOptions(new OptionData(OptionType.STRING, Constants.ABILITY_2, "Ability Name").setAutoComplete(true));
        addOptions(new OptionData(OptionType.STRING, Constants.ABILITY_3, "Ability Name").setAutoComplete(true));
        addOptions(new OptionData(OptionType.STRING, Constants.ABILITY_4, "Ability Name").setAutoComplete(true));
        addOptions(new OptionData(OptionType.STRING, Constants.ABILITY_5, "Ability Name").setAutoComplete(true));
    }

    public void execute(SlashCommandInteractionEvent event) {
        List<String> abilityIDs = new ArrayList<>();

        //GET ALL ABILITY OPTIONS AS STRING
        for (OptionMapping option : event.getOptions().stream().filter(o -> o != null && o.getName().contains(Constants.ABILITY)).toList()) {
            abilityIDs.add(option.getAsString());
        }

        abilityIDs.removeIf(StringUtils::isEmpty);
        abilityIDs.removeIf(a -> !Mapper.getFactionAbilities().keySet().contains(a));

        Game activeGame = getActiveMap();
        Player player = activeGame.getPlayer(getUser().getId());
        player = Helper.getGamePlayer(activeGame, player, event, null);
        if (player == null) {
            sendMessage("Player could not be found");
            return;
        }

        doAction(player, abilityIDs);
    }

    public abstract void doAction(Player player, List<String> abilityIDs);

}
