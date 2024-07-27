package ti4.commands.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.function.Consumers;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import ti4.buttons.Buttons;
import ti4.commands.fow.Whisper;
import ti4.commands.uncategorized.CardsInfo;
import ti4.generator.MapGenerator;
import ti4.generator.Mapper;
import ti4.helpers.ButtonHelper;
import ti4.helpers.ButtonHelperAgents;
import ti4.helpers.ButtonHelperCommanders;
import ti4.helpers.ButtonHelperFactionSpecific;
import ti4.helpers.Constants;
import ti4.helpers.Emojis;
import ti4.helpers.FoWHelper;
import ti4.helpers.Helper;
import ti4.map.Game;
import ti4.map.Leader;
import ti4.map.Player;
import ti4.message.BotLogger;
import ti4.message.MessageHelper;
import ti4.model.LeaderModel;

public class TurnStart extends PlayerSubcommandData {
    public TurnStart() {
        super(Constants.TURN_START, "Start Turn");
        addOptions(
            new OptionData(OptionType.STRING, Constants.FACTION_COLOR, "Faction or Color for which you set stats")
                .setAutoComplete(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game game = getActiveGame();
        Player mainPlayer = game.getPlayer(getUser().getId());
        mainPlayer = Helper.getGamePlayer(game, mainPlayer, event, null);
        mainPlayer = Helper.getPlayer(game, mainPlayer, event);

        if (mainPlayer == null) {
            MessageHelper.sendMessageToEventChannel(event, "Player/Faction/Color could not be found in map:" + game.getName());
            return;
        }
        turnStart(event, game, mainPlayer);
    }

    public static void turnStart(GenericInteractionCreateEvent event, Game game, Player player) {
        player.setWhetherPlayerShouldBeTenMinReminded(false);
        player.setTurnCount(player.getTurnCount() + 1);
        ButtonHelper.fullCommanderUnlockCheck(player, game, "hacan", event);
        Map<String, String> maps = new HashMap<>();
        maps.putAll(game.getMessagesThatICheckedForAllReacts());
        for (String id : maps.keySet()) {
            if (id.contains("combatRoundTracker")) {
                game.removeStoredValue(id);
            }
        }
        game.setStoredValue(player.getFaction() + "planetsExplored", "");
        game.setNaaluAgent(false);
        game.setL1Hero(false);
        game.setStoredValue("lawsDisabled", "no");
        game.checkSOLimit(player);
        game.setStoredValue("vaylerianHeroActive", "");
        game.setStoredValue("tnelisCommanderTracker", "");
        game.setStoredValue("planetsTakenThisRound", "");
        game.setStoredValue("absolLux", "");
        game.setStoredValue("mentakHero", "");
        CardsInfo.sendVariousAdditionalButtons(game, player);
        boolean goingToPass = false;
        if (game.getStoredValue("Pre Pass " + player.getFaction()) != null
            && game.getStoredValue("Pre Pass " + player.getFaction())
                .contains(player.getFaction())) {
            if (game.getStoredValue("Pre Pass " + player.getFaction()).contains(player.getFaction())
                && !player.isPassed()) {
                game.setStoredValue("Pre Pass " + player.getFaction(), "");
                goingToPass = true;
            }
        }
        String text = "" + player.getRepresentation(true, true) + " UP NEXT (Turn #" + player.getTurnCount() + ")";
        String buttonText = "Use buttons to do your turn. ";
        List<Button> buttons = getStartOfTurnButtons(player, game, false, event);
        MessageChannel gameChannel = game.getMainGameChannel() == null ? event.getMessageChannel()
            : game.getMainGameChannel();

        game.updateActivePlayer(player);
        game.setPhaseOfGame("action");
        ButtonHelperFactionSpecific.resolveMilitarySupportCheck(player, game);
        Helper.startOfTurnSaboWindowReminders(game, player);
        boolean isFowPrivateGame = FoWHelper.isPrivateGame(game, event);

        if (isFowPrivateGame) {
            FoWHelper.pingAllPlayersWithFullStats(game, event, player, "started turn");

            String fail = "User for next faction not found. Report to ADMIN";
            String success = "The next player has been notified";
            MessageHelper.sendPrivateMessageToPlayer(player, game, event, text, fail, success);
            if (!goingToPass) {
                MessageHelper.sendMessageToChannelWithButtons(player.getPrivateChannel(), buttonText, buttons);
            }
            if (getMissedSCFollowsText(game, player) != null
                && !"".equalsIgnoreCase(getMissedSCFollowsText(game, player))) {
                MessageHelper.sendMessageToChannel(player.getPrivateChannel(),
                    getMissedSCFollowsText(game, player));
            }
            Player privatePlayer = player;
            if (privatePlayer.getStasisInfantry() > 0) {
                if (ButtonHelper.getPlaceStatusInfButtons(game, privatePlayer).size() > 0) {
                    MessageHelper.sendMessageToChannelWithButtons(privatePlayer.getCorrectChannel(),
                        "Use buttons to revive infantry. You have " + privatePlayer.getStasisInfantry() + " infantry left to revive.",
                        ButtonHelper.getPlaceStatusInfButtons(game, privatePlayer));
                } else {
                    privatePlayer.setStasisInfantry(0);
                    MessageHelper.sendMessageToChannel(privatePlayer.getCorrectChannel(), privatePlayer.getRepresentation()
                        + " You had infantry II to be revived, but the bot couldn't find planets you own in your HS to place them, so per the rules they now disappear into the ether.");

                }
            }
            ButtonHelperFactionSpecific.resolveKolleccAbilities(player, game);
            ButtonHelperFactionSpecific.resolveMykoMechCheck(player, game);

            game.setPingSystemCounter(0);
            for (int x = 0; x < 10; x++) {
                game.setTileAsPinged(x, null);
            }
        } else {
            //checkhere
            if (game.isShowBanners()) {
                MapGenerator.drawBanner(player);
            }
            MessageHelper.sendMessageToChannel(gameChannel, text);
            if (!goingToPass) {
                MessageHelper.sendMessageToChannelWithButtons(gameChannel, buttonText, buttons);
            }
            if (getMissedSCFollowsText(game, player) != null
                && !"".equalsIgnoreCase(getMissedSCFollowsText(game, player))) {
                MessageHelper.sendMessageToChannel(gameChannel, getMissedSCFollowsText(game, player));
            }
            Player privatePlayer = player;
            if (privatePlayer.getStasisInfantry() > 0) {
                if (ButtonHelper.getPlaceStatusInfButtons(game, privatePlayer).size() > 0) {
                    MessageHelper.sendMessageToChannelWithButtons(privatePlayer.getCorrectChannel(),
                        "Use buttons to revive infantry. You have " + privatePlayer.getStasisInfantry() + " infantry left to revive.",
                        ButtonHelper.getPlaceStatusInfButtons(game, privatePlayer));
                } else {
                    privatePlayer.setStasisInfantry(0);
                    MessageHelper.sendMessageToChannel(privatePlayer.getCorrectChannel(), privatePlayer.getRepresentation()
                        + " You had infantry II to be revived, but the bot couldn't find planets you own in your HS to place them, so per the rules they now disappear into the ether.");

                }
            }
            ButtonHelperFactionSpecific.resolveMykoMechCheck(player, game);
            ButtonHelperFactionSpecific.resolveKolleccAbilities(player, game);

        }
        if (!game.getStoredValue("futureMessageFor" + player.getFaction()).isEmpty()) {
            MessageHelper.sendMessageToChannel(player.getCardsInfoThread(),
                player.getRepresentation(true, true) + " you left yourself the following message: \n"
                    + game.getStoredValue("futureMessageFor" + player.getFaction()));
            game.setStoredValue("futureMessageFor" + player.getFaction(), "");
        }
        for (Player p2 : game.getRealPlayers()) {
            if (!game
                .getStoredValue("futureMessageFor_" + player.getFaction() + "_" + p2.getFaction())
                .isEmpty()) {
                String msg2 = "This is a message sent from the past:\n" + game
                    .getStoredValue("futureMessageFor_" + player.getFaction() + "_" + p2.getFaction());
                MessageHelper.sendMessageToChannel(p2.getCardsInfoThread(),
                    p2.getRepresentation(true, true) + " your future message got delivered");
                Whisper.sendWhisper(game, p2, player, msg2, "n", p2.getCardsInfoThread(), event.getGuild());
                game.setStoredValue("futureMessageFor_" + player.getFaction() + "_" + p2.getFaction(), "");
            }
        }

        if (goingToPass) {
            player.setPassed(true);
            if (game.playerHasLeaderUnlockedOrAlliance(player, "olradincommander")) {
                ButtonHelperCommanders.olradinCommanderStep1(player, game);
            }
            String text2 = player.getRepresentation() + " PASSED";
            MessageHelper.sendMessageToChannel(player.getCorrectChannel(), text2);
            if (player.hasTech("absol_aida")) {
                String msg = player.getRepresentation()
                    + " since you have AI Development Algorithm, you may research 1 Unit Upgrade now for 6 influence.";
                MessageHelper.sendMessageToChannel(player.getCorrectChannel(), msg);
                if (!player.hasAbility("propagation")) {
                    MessageHelper.sendMessageToChannelWithButtons(player.getCorrectChannel(),
                        player.getRepresentation(true, true) + " you may use the button to get your tech.",
                        List.of(Buttons.GET_A_TECH));
                } else {
                    List<Button> buttons2 = ButtonHelper.getGainCCButtons(player);
                    String message2 = player.getRepresentation() + "! Your current CCs are "
                        + player.getCCRepresentation()
                        + ". Use buttons to gain CCs";
                    MessageHelper.sendMessageToChannelWithButtons(player.getCorrectChannel(),
                        message2, buttons2);
                    game.setStoredValue("originalCCsFor" + player.getFaction(), player.getCCRepresentation());
                }
            }
            if (player.hasAbility("deliberate_action") && (player.getTacticalCC() == 0 || player.getStrategicCC() == 0 || player.getFleetCC() == 0)) {
                String msg = player.getRepresentation()
                    + " since you have deliberate action ability and passed while one of your pools was at 0, you may gain 1 CC to that pool.";
                MessageHelper.sendMessageToChannel(player.getCorrectChannel(), msg);
                List<Button> buttons2 = ButtonHelper.getGainCCButtons(player);
                String message2 = player.getRepresentation() + "! Your current CCs are " + player.getCCRepresentation()
                    + ". Use buttons to gain CCs";
                MessageHelper.sendMessageToChannelWithButtons(player.getCorrectChannel(), message2, buttons2);
            }
            TurnEnd.pingNextPlayer(event, game, player, true);
        }
    }

    public static String getMissedSCFollowsText(Game game, Player player) {
        if (!game.isStratPings())
            return null;
        boolean sendReminder = false;

        StringBuilder sb = new StringBuilder();
        sb.append(player.getRepresentation(true, true));
        sb.append(" Please resolve these before doing anything else:\n");
        for (int sc : game.getPlayedSCsInOrder(player)) {
            if (game.getName().equalsIgnoreCase("pbd1000")) {
                String num = sc + "";
                num = num.substring(num.length() - 1, num.length());
                for (Integer sc2 : player.getSCs()) {
                    String num2 = sc2 + "";
                    num2 = num2.substring(num2.length() - 1, num2.length());
                    if (!num2.equalsIgnoreCase(num) && !player.hasFollowedSC(sc)) {
                        player.addFollowedSC(sc);
                    }
                }
            }
            if (!player.hasFollowedSC(sc)) {
                sb.append("> ").append(Helper.getSCRepresentation(game, sc));
                if (!game.getStoredValue("scPlay" + sc).isEmpty()) {
                    sb.append(" ").append(game.getStoredValue("scPlay" + sc));
                }
                sb.append("\n");
                sendReminder = true;
            }
        }
        sb.append("You currently have ").append(player.getStrategicCC()).append(" CC in your strategy pool.");
        return sendReminder ? sb.toString() : null;
    }

    public static List<Button> getStartOfTurnButtons(Player player, Game game, boolean doneActionThisTurn,
        GenericInteractionCreateEvent event) {

        if (!doneActionThisTurn) {
            for (Player p2 : game.getRealPlayers()) {
                if (!game.getStoredValue(p2.getFaction() + "graviton").isEmpty()) {
                    game.setStoredValue(p2.getFaction() + "graviton", "");
                }
            }
        }
        String finChecker = player.getFinsFactionCheckerPrefix();
        game.setDominusOrb(false);

        List<Button> startButtons = new ArrayList<>();
        Button tacticalAction = Button.success(finChecker + "tacticalAction",
            "Tactical Action (" + player.getTacticalCC() + ")");
        int numOfComponentActions = ButtonHelper.getAllPossibleCompButtons(game, player, event).size() - 2;
        Button componentAction = Button.success(finChecker + "componentAction", "Component Action (" + numOfComponentActions + ")");

        startButtons.add(tacticalAction);
        startButtons.add(componentAction);
        boolean hadAnyUnplayedSCs = false;
        for (Integer SC : player.getSCs()) {
            if (!game.getPlayedSCs().contains(SC)) {
                hadAnyUnplayedSCs = true;
                if (game.isHomebrewSCMode()) {
                    Button strategicAction = Button.success(finChecker + "strategicAction_" + SC, "Play " + Helper.getSCName(SC, game));
                    startButtons.add(strategicAction);
                } else {
                    Button strategicAction = Button.success(finChecker + "strategicAction_" + SC, "Play " + Helper.getSCName(SC, game))
                        .withEmoji(Emoji.fromFormatted(Emojis.getSCEmojiFromInteger(SC)));
                    startButtons.add(strategicAction);
                }
            }
        }

        if (!hadAnyUnplayedSCs && !doneActionThisTurn) {
            Button pass = Button.danger(finChecker + "passForRound", "Pass");
            if (ButtonHelper.getEndOfTurnAbilities(player, game).size() > 1) {
                startButtons.add(Button.primary("endOfTurnAbilities", "Do End Of Turn Ability ("
                    + (ButtonHelper.getEndOfTurnAbilities(player, game).size() - 1) + ")"));
            }

            startButtons.add(pass);
            if (!game.isFowMode()) {
                for (Player p2 : game.getRealPlayers()) {
                    for (int sc : player.getSCs()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(p2.getRepresentation(true, true));
                        sb.append(" You are getting this ping because " + Helper.getSCName(sc, game) + " has been played and now it is their turn again and you still haven't reacted. If you already reacted, check if your reaction got undone");
                        if (!game.getStoredValue("scPlay" + sc).isEmpty()) {
                            sb.append("Message link is: ").append(game.getStoredValue("scPlay" + sc)).append("\n");
                        }
                        sb.append("You currently have ").append(p2.getStrategicCC())
                            .append(" CC in your strategy pool.");
                        if (!p2.hasFollowedSC(sc)) {
                            MessageHelper.sendMessageToChannel(p2.getCardsInfoThread(), sb.toString());
                        }
                    }
                }
            }

        }
        if (doneActionThisTurn) {
            ButtonHelperFactionSpecific.checkBlockadeStatusOfEverything(player, game, event);
            if (ButtonHelper.getEndOfTurnAbilities(player, game).size() > 1) {
                startButtons.add(Button.primary("endOfTurnAbilities", "Do End Of Turn Ability ("
                    + (ButtonHelper.getEndOfTurnAbilities(player, game).size() - 1) + ")"));
            }
            startButtons.add(Button.danger(finChecker + "turnEnd", "End Turn"));
            if (ButtonHelper.isPlayerElected(game, player, "minister_war")) {
                startButtons.add(Button.secondary(finChecker + "ministerOfWar", "Use Minister of War"));
            }
            if (!game.isJustPlayedComponentAC()) {
                player.setWhetherPlayerShouldBeTenMinReminded(true);
            }
        } else {
            game.setJustPlayedComponentAC(false);
            if (player.getTechs().contains("cm")) {
                Button chaos = Button.secondary("startChaosMapping", "Use Chaos Mapping")
                    .withEmoji(Emoji.fromFormatted(Emojis.Saar));
                startButtons.add(chaos);
            }
            if (player.getTechs().contains("dscymiy") && !player.getExhaustedTechs().contains("dscymiy")) {
                Button chaos = Button.secondary("exhaustTech_dscymiy", "Exhaust Recursive Worm")
                    .withEmoji(Emoji.fromFormatted(Emojis.cymiae));
                startButtons.add(chaos);
            }
            if (player.hasUnexhaustedLeader("florzenagent")
                && ButtonHelperAgents.getAttachments(game, player).size() > 0) {
                startButtons.add(Button
                    .success(finChecker + "exhaustAgent_florzenagent_" + player.getFaction(),
                        "Use Florzen Agent")
                    .withEmoji(Emoji.fromFormatted(Emojis.florzen)));
            }
            if (player.hasUnexhaustedLeader("vadenagent")) {
                Button chaos = Button.secondary("exhaustAgent_vadenagent_" + player.getFaction(),
                    "Use Vaden Agent")
                    .withEmoji(Emoji.fromFormatted(Emojis.vaden));
                startButtons.add(chaos);
            }
            if (player.hasAbility("laws_order") && !game.getLaws().isEmpty()) {
                Button chaos = Button.secondary("useLawsOrder", "Pay To Ignore Laws")
                    .withEmoji(Emoji.fromFormatted(Emojis.Keleres));
                startButtons.add(chaos);
            }
            if (player.hasTech("td") && !player.getExhaustedTechs().contains("td")) {
                Button transit = Button.secondary(finChecker + "exhaustTech_td", "Exhaust Transit Diodes");
                transit = transit.withEmoji(Emoji.fromFormatted(Emojis.CyberneticTech));
                startButtons.add(transit);
            }
            if (player.hasUnexhaustedLeader("kolleccagent")) {
                Button nekroButton = Button.secondary("exhaustAgent_kolleccagent",
                    "Use Kollecc Agent")
                    .withEmoji(Emoji.fromFormatted(Emojis.kollecc));
                startButtons.add(nekroButton);
            }
        }
        if (player.hasTech("pa") && ButtonHelper.getPsychoTechPlanets(game, player).size() > 1) {
            Button psycho = Button.success(finChecker + "getPsychoButtons",
                "Use Psychoarcheology");
            psycho = psycho.withEmoji(Emoji.fromFormatted(Emojis.BioticTech));
            startButtons.add(psycho);
        }
        Player p1 = player;
        String prefix = "componentActionRes_";
        for (Leader leader : p1.getLeaders()) {
            if (!leader.isExhausted() && !leader.isLocked()) {
                String leaderID = leader.getId();
                LeaderModel leaderModel = Mapper.getLeader(leaderID);
                if (leaderModel == null) {
                    continue;
                }
                String leaderName = leaderModel.getName();
                String leaderAbilityWindow = leaderModel.getAbilityWindow();
                String factionEmoji = Emojis.getFactionLeaderEmoji(leader);
                if ("ACTION:".equalsIgnoreCase(leaderAbilityWindow) || leaderName.contains("Ssruu")) {
                    if (leaderName.contains("Ssruu")) {
                        String led = "naaluagent";
                        if (p1.hasExternalAccessToLeader(led)) {
                            Button lButton = Button
                                .secondary(finChecker + prefix + "leader_" + led,
                                    "Use " + leaderName + " as Naalu Agent")
                                .withEmoji(Emoji.fromFormatted(factionEmoji));
                            startButtons.add(lButton);
                        }
                    } else {
                        if (leaderID.equalsIgnoreCase("naaluagent")) {
                            Button lButton = Button
                                .secondary(finChecker + prefix + "leader_" + leaderID, "Use " + leaderName)
                                .withEmoji(Emoji.fromFormatted(factionEmoji));
                            startButtons.add(lButton);
                        }
                    }
                } else if ("mahactcommander".equalsIgnoreCase(leaderID) && p1.getTacticalCC() > 0
                    && ButtonHelper.getTilesWithYourCC(p1, game, event).size() > 0) {
                    Button lButton = Button.secondary(finChecker + "mahactCommander", "Use Mahact Commander")
                        .withEmoji(Emoji.fromFormatted(factionEmoji));
                    startButtons.add(lButton);
                }
            }
        }

        Button transaction = Button.primary("transaction", "Transaction");
        startButtons.add(transaction);
        Button modify = Button.secondary("getModifyTiles", "Modify Units");
        startButtons.add(modify);
        if (player.hasUnexhaustedLeader("hacanagent")) {
            Button hacanButton = Button.secondary("exhaustAgent_hacanagent",
                "Use Hacan Agent")
                .withEmoji(Emoji.fromFormatted(Emojis.Hacan));
            startButtons.add(hacanButton);
        }
        if (player.hasRelicReady("e6-g0_network")) {
            startButtons.add(Button.success("exhauste6g0network", "Exhaust E6-G0 Network Relic to Draw AC"));
        }
        if (player.hasUnexhaustedLeader("nekroagent") && player.getAc() > 0) {
            Button nekroButton = Button.secondary("exhaustAgent_nekroagent",
                "Use Nekro Agent")
                .withEmoji(Emoji.fromFormatted(Emojis.Nekro));
            startButtons.add(nekroButton);
        }

        if (game.getLatestTransactionMsg() != null
            && !"".equalsIgnoreCase(game.getLatestTransactionMsg())) {
            game.getMainGameChannel().deleteMessageById(game.getLatestTransactionMsg())
                .queue(Consumers.nop(), BotLogger::catchRestError);
            game.setLatestTransactionMsg("");
        }
        // if (game.getActionCards().size() > 130 &&
        // getButtonsToSwitchWithAllianceMembers(player, game, false).size() > 0)
        // {
        // startButtons.addAll(getButtonsToSwitchWithAllianceMembers(player, game,
        // false));
        // }
        if (!doneActionThisTurn && game.isFowMode()) {
            startButtons.add(Button.secondary("showGameAgain", "Show Game"));
        }

        return startButtons;
    }
}
