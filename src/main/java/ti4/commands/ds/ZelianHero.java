package ti4.commands.ds;

import java.util.List;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import ti4.commands.special.StellarConverter;
import ti4.commands.units.AddRemoveUnits;
import ti4.helpers.AliasHandler;
import ti4.helpers.ButtonHelperAbilities;
import ti4.helpers.ButtonHelperAgents;
import ti4.helpers.Constants;
import ti4.helpers.Helper;
import ti4.map.Game;
import ti4.map.Leader;
import ti4.map.Planet;
import ti4.map.Player;
import ti4.map.Tile;
import ti4.map.UnitHolder;
import ti4.message.MessageHelper;

public class ZelianHero extends DiscordantStarsSubcommandData {

    public ZelianHero() {
        super(Constants.ZELIAN_HERO, "Celestial Impact a system (replace with Zelian Asteroid field)");
        addOptions(new OptionData(OptionType.STRING, Constants.TILE_NAME, "System/Tile name").setRequired(true).setAutoComplete(true));
        addOptions(new OptionData(OptionType.STRING, Constants.FACTION_COLOR, "Faction or Color using Zelian Hero 'Cataclysm - Celestial Impact'").setAutoComplete(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game activeGame = getActiveGame();
        Player player = activeGame.getPlayer(getUser().getId());
        player = Helper.getGamePlayer(activeGame, player, event, null);
        player = Helper.getPlayer(activeGame, player, event);
        if (player == null) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "Player could not be found");
            return;
        }

        OptionMapping tileOption = event.getOption(Constants.TILE_NAME);
        if (tileOption == null) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "Specify a tile");
            return;
        }

        String tileID = AliasHandler.resolveTile(tileOption.getAsString().toLowerCase());
        Tile tile = AddRemoveUnits.getTile(event, tileID, activeGame);
        if (tile == null) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "Could not resolve tileID:  `" + tileID + "`. Tile not found");
            return;
        }

        secondHalfOfCelestialImpact(player, event, tile, activeGame);
    }

    public void secondHalfOfCelestialImpact(Player player, GenericInteractionCreateEvent event, Tile tile, Game activeGame) {
        String message1 = "Moments before disaster in game " + activeGame.getName();
        StellarConverter.postTileInDisasterWatch(activeGame, tile, 1, message1);

        //Remove all other players ground force units from the tile in question
        for (Player player_ : activeGame.getPlayers().values()) {
            if (player_ != player) {
                for (UnitHolder unitHolder : tile.getUnitHolders().values()) {
                    if (!unitHolder.getName().equals(Constants.SPACE)) {
                        unitHolder.removeAllUnits(player_.getColor());
                    }
                }
            }
        }

        //Gain TGs equal to the sum of the resource values of the planets in the system
        int resourcesSum = 0;
        List<Planet> planetsInSystem = tile.getPlanetUnitHolders().stream().map(uh -> (Planet) uh).toList();
        for (Planet p : planetsInSystem) {
            resourcesSum += p.getResources();
        }
        StringBuilder tgGainMsg = new StringBuilder(player.getFactionEmoji());
        tgGainMsg.append(" gained ").append(resourcesSum).append("tg from Celestial Impact (");
        tgGainMsg.append(player.getTg()).append("->").append(player.getTg() + resourcesSum).append(").");
        MessageHelper.sendMessageToChannel(event.getMessageChannel(), tgGainMsg.toString());
        player.gainTG(resourcesSum);
        ButtonHelperAbilities.pillageCheck(player, activeGame);
        ButtonHelperAgents.resolveArtunoCheck(player, activeGame, resourcesSum);

        //Add the zelian asteroid field to the map and copy over the space unitholder
        UnitHolder space = tile.getUnitHolders().get(Constants.SPACE);
        activeGame.removeTile(tile.getPosition());
        Tile asteroidTile = new Tile(AliasHandler.resolveTile("D36"), tile.getPosition(), space);
        activeGame.setTile(asteroidTile);

        //After shot to disaster channel
        StringBuilder message2 = new StringBuilder();
        message2.append(tile.getRepresentation());
        message2.append(" has been celestially impacted by ");
        message2.append(player.getRepresentation());
        StellarConverter.postTileInDisasterWatch(activeGame, asteroidTile, 1, message2.toString());

        if (player.hasLeaderUnlocked("zelianhero")) {
            Leader playerLeader = player.getLeader("zelianhero").orElse(null);
            StringBuilder message = new StringBuilder(player.getRepresentation()).append(" played ").append(Helper.getLeaderFullRepresentation(playerLeader));
            boolean purged = player.removeLeader(playerLeader);
            if (purged) {
                MessageHelper.sendMessageToChannel(event.getMessageChannel(), message + " - Leader " + "zelianhero" + " has been purged");
            } else {
                MessageHelper.sendMessageToChannel(event.getMessageChannel(), "Leader was not purged - something went wrong");
            }
        }
    }
}
