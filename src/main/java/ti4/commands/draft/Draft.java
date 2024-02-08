package ti4.commands.draft;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.handle.MessageReactionBulkRemoveHandler;
import ti4.ResourceHelper;
import ti4.generator.Mapper;
import ti4.generator.TileHelper;
import ti4.helpers.ButtonHelper;
import ti4.helpers.Constants;
import ti4.helpers.Emojis;
import ti4.helpers.ImageHelper;
import ti4.helpers.Storage;
import ti4.map.Game;
import ti4.map.MapFileDeleter;
import ti4.map.Planet;
import ti4.map.Player;
import ti4.map.Tile;
import ti4.map.UnitHolder;
import ti4.message.BotLogger;
import ti4.message.MessageHelper;
import ti4.model.TileModel;
import ti4.model.WormholeModel;

public class Draft extends DraftSubcommandData {

    public Draft() {
        super(Constants.START, "Start Milty Draft");
        addOptions(new OptionData(OptionType.INTEGER, Constants.SLICE_COUNT, "Slice Count").setRequired(false));
        addOptions(new OptionData(OptionType.INTEGER, Constants.FACTION_COUNT, "Faction Count").setRequired(false));
        addOptions(new OptionData(OptionType.BOOLEAN, Constants.ANOMALIES_CAN_TOUCH, "Anomalies can touch"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        
        Game activeGame = getActiveGame();
        MessageHelper.sendMessageToChannel(event.getMessageChannel(), "Starting Draft");
    }   
}
