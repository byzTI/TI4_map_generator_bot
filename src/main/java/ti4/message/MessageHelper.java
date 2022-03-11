package ti4.message;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.File;

public class MessageHelper {

    public static void sendMessageToChannel(MessageChannel channel, String messageText)
    {
        channel.sendMessage(messageText).queue();
    }

    public static void sendFileToChannel(MessageChannel channel, File file)
    {
        channel.sendFile(file).queue();
    }

    public static void replyToMessage(SlashCommandInteractionEvent event, String messageText)
    {
//        sendMessageToChannel(event.getChannel(), messageText);
//        event.getHook().sendMessage(messageText).queue();
        event.reply(messageText).queue();
    }
    public static void replyToMessage(SlashCommandInteractionEvent event, File file)
    {
        event.replyFile(file).queue();
//        event.getHook().sendFile(file).queue();
//        sendFileToChannel(event.getChannel(), file);
    }
}
