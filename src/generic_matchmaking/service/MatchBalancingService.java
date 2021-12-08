package generic_matchmaking.service;

import generic_matchmaking.entity.Match;
import generic_matchmaking.entity.Player;
import generic_matchmaking.entity.Team;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MatchBalancingService {

    // Shuffles players in Teams for better balancing if possible
    public void rebalanceMatch(Match match) {
        Instant start = Instant.now();
        List<Match> allPossibleMatches = getPossibleTeamCompositions(match);
        List<Match> validMatches = getValidMatches(allPossibleMatches);
        Instant end = Instant.now();

        // Internal log
        if (!match.getTeamsSkillDifference().equals(validMatches.get(0).getTeamsSkillDifference())) {
            System.out.println("Match was rebalanced (" +
                    match.getTeamsSkillDifference() + "->" + validMatches.get(0).getTeamsSkillDifference() +
                    ") in: " + Duration.between(start, end));
        }

        match.setTeam1(validMatches.get(0).getTeam1());
        match.setTeam2(validMatches.get(0).getTeam2());
    }

    private List<Match> getPossibleTeamCompositions(Match match) {
        // Collects Players from both Teams in a single list
        List<Player> allPlayers = new ArrayList<>(match.getTeam1().getPlayers());
        allPlayers.addAll(match.getTeam2().getPlayers());

        List<Match> teamCompositions = new ArrayList<>();
        // Array of first indices, [0, 1, 2, 3, 4, 5]
        Integer[] startingIndices = Stream.iterate(0, v -> v + 1).limit(Team.FULL_TEAM_SIZE).toArray(Integer[]::new);
        teamCompositions.add(getTeamComposition(allPlayers, startingIndices));

        // Recursively iterates over all possible index combinations and adds them to teamCompositions list
        calculateTeamOutcomes(allPlayers, teamCompositions, startingIndices, 0);

        return teamCompositions;
    }

    /* Implementation of k-combinations without repetition algorithm
       Formula: n! / (k! * (n-k)!)
       In our case n = Team.FULL_TEAM_SIZE * 2, k = Team.FULL_TEAM_SIZE
       E.g. 12! / (6! * (12-6)!) = 924

       Algorithm in process:
       [0, 1, 2, 3, 4, 5] <- already added in getPossibleTeamCompositions()
       [0, 1, 2, 3, 4, 6]
       . . .
       [0, 1, 2, 3, 4, 11]
       [0, 1, 2, 3, 5, 6]
       . . .
       [0, 1, 2, 3, 10, 11]
       [0, 1, 2, 4, 5, 6]
       . . .
       [6, 7, 8, 9, 10, 11] <- final possible combination of indices
    */
    private void calculateTeamOutcomes(List<Player> allPlayers, List<Match> teamCompositions, Integer[] indices, int currentIndex) {
        while (currentIndex < Team.FULL_TEAM_SIZE && indices[currentIndex] != Team.FULL_TEAM_SIZE + currentIndex) {
            calculateTeamOutcomes(allPlayers, teamCompositions, indices, currentIndex + 1);
            ++indices[currentIndex];
            for (int nextIndex = currentIndex + 1; nextIndex < Team.FULL_TEAM_SIZE; nextIndex++) {
                indices[nextIndex] = indices[currentIndex] + nextIndex - currentIndex;
            }
            teamCompositions.add(getTeamComposition(allPlayers, indices));
        }
        ++currentIndex;
    }

    // Returns Match with Team 1 set by indices and Team 2 collected from leftover Players
    private Match getTeamComposition(List<Player> allPlayers, Integer[] indices) {
        List<Player> team1Players = new ArrayList<>();
        List<Player> team2Players = new ArrayList<>(allPlayers);

        Arrays.stream(indices).forEach(i -> team1Players.add(allPlayers.get(i)));
        team2Players.removeAll(team1Players);

        return new Match(new Team(team1Players), new Team(team2Players));
    }

    // Filters out matches where squads were broken, sorts result by skill difference
    private List<Match> getValidMatches(List<Match> allPossibleMatches) {
        return allPossibleMatches.stream().filter(m -> {
            // Collect squad ids of Team 1
            List<Integer> team1SquadIds = m.getTeam1().getPlayers().stream()
                    .map(Player::getSquadId)
                    .filter(squadId -> Player.NO_SQUAD_ID != squadId)
                    .collect(Collectors.toList());
            // If Team 2 contains one of Team 1 squad ids -> squads are broken
            return m.getTeam2().getPlayers().stream().noneMatch(p -> team1SquadIds.contains(p.getSquadId()));
            // Ascending sort by skill difference
        }).sorted(Comparator.comparing(Match::getTeamsSkillDifference)).collect(Collectors.toList());
    }

}
