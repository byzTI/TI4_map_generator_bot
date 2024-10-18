package ti4.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ti4.map.Player;

public class NeighbourHelper {

    public static Map<Integer, List<Player>> getNeighbourDistanceMap(Player player, int maxDistance) {
        Set<Player> neighbours = player.getNeighbouringPlayers();
        List<Player> remainingPlayers = player.getGame().getRealPlayers();

        Map<Integer, List<Player>> distanceToPlayers = new HashMap<>();
        distanceToPlayers.put(1, new ArrayList<>(neighbours));
        remainingPlayers.removeAll(neighbours);

        for (int distance = 2; distance <= maxDistance && distance <= 10; distance++) {
            Set<Player> playersAtThisDistance = new HashSet<>();
            List<Player> playersAtPreviousDistance = distanceToPlayers.get(distance - 1);
            for (Player earlierPlayer : playersAtPreviousDistance) {
                for (Player remainingPlayer : remainingPlayers) {
                    if (earlierPlayer.isNeighboursWith(remainingPlayer)) {
                        playersAtThisDistance.add(player);
                    }
                }
            }
            distanceToPlayers.put(distance, new ArrayList<>(playersAtThisDistance));
            remainingPlayers.removeAll(playersAtThisDistance);
            if (remainingPlayers.isEmpty()) {
                break;
            }
        }
        return distanceToPlayers;
    }

    public static List<String> getNeighbourPathsTo(Player player, Player otherPlayer) {
        List<String> paths = new ArrayList<>();
        if (player.isNeighboursWith(otherPlayer)) {
            return List.of(player.getFactionEmoji() + otherPlayer.getFactionEmoji());
        }
        for (Player neighbour : player.getNeighbouringPlayers()) {
            if (neighbour.isNeighboursWith(otherPlayer)) {
                paths.add(player.getFactionEmoji() + neighbour.getFactionEmoji() + otherPlayer.getFactionEmoji());
            } else {
                for (Player neighneighbour : neighbour.getNeighbouringPlayers()) {
                    if (neighneighbour.isNeighboursWith(otherPlayer)) {
                        paths.add(player.getFactionEmoji() + neighbour.getFactionEmoji() + neighneighbour.getFactionEmoji() + otherPlayer.getFactionEmoji());
                    }
                }
            }
        }
        return paths;
    }
}
