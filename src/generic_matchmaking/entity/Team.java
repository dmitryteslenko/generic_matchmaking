package generic_matchmaking.entity;

import generic_matchmaking.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class Team {

    // Each Player skill ranges in delta of first Player in a Team
    private final List<Player> players = new ArrayList<>();
    // Depends on oldest Player waiting time
    private int priorityLevel = 0;

    public static final int MIN_SQUAD_SIZE = 2;
    public static final int FULL_TEAM_SIZE = 6;

    public boolean isFull() {
        return FULL_TEAM_SIZE == players.size();
    }

    public boolean isEnoughSlotsFor(List<Player> squad) {
        return Team.FULL_TEAM_SIZE >= players.size() + squad.size();
    }

    public Integer getTotalSkill() {
        return players.stream().mapToInt(Player::getSkill).sum();
    }

    // Can player be added to this Team
    public boolean isPlayerEligible(Player player) {
        return players.isEmpty() || !isFull() && isSkillEligible(player);
    }

    // Can squad be added to this Team
    public boolean isSquadEligible(List<Player> squad) {
        return players.isEmpty() || isEnoughSlotsFor(squad) && squad.stream().anyMatch(this::isSkillEligible);
    }

    public void addPlayer(Player player) {
        if (!isFull()) {
            this.players.add(player);
        }
    }

    public void addSquad(List<Player> squad) {
        if (isEnoughSlotsFor(squad)) {
            players.addAll(squad);
        }
    }

    // Determines if difference between first Team Player and player
    // is within Player balance delta + priority overhead
    public boolean isSkillEligible(Player player) {
        return Constants.Thresholds.PLAYER_DELTA +
                Constants.Thresholds.PRIORITY_LEVEL_DELTAS.get(priorityLevel)
                >= Math.abs(players.get(0).getSkill() - player.getSkill());
    }


    public Team() {}

    public Team(List<Player> players) {
        this.players.addAll(players);
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(int priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

}
