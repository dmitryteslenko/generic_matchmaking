package generic_matchmaking.service;

import generic_matchmaking.config.AppConfig;
import generic_matchmaking.entity.Match;
import generic_matchmaking.entity.Player;
import generic_matchmaking.entity.Team;
import generic_matchmaking.util.Constants;
import generic_matchmaking.util.LogHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class MatchmakingService {

    // List of Matches which contain only Team 1
    private final List<Match> incompleteMatches = Collections.synchronizedList(new ArrayList<>());
    // List of Teams which are not full
    private final List<Team> incompleteTeams = Collections.synchronizedList(new ArrayList<>());

    private final MatchBalancingService matchBalancingService = new MatchBalancingService();
    private final ExecutorService matchmakingExecutor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService priorityUpdateExecutor = Executors.newSingleThreadScheduledExecutor();

    public void startMatchmaking(PriorityBlockingQueue<Player> playerQueue) {
        matchmakingExecutor.execute(matchmakingTask(playerQueue));
        // Every PRIORITY_UPDATE_TASK_PERIOD_IN_SEC checks if Team priority level should be updated
        priorityUpdateExecutor.scheduleAtFixedRate(
                priorityUpdateTask(), 0,
                AppConfig.PRIORITY_UPDATE_TASK_PERIOD_IN_SEC, TimeUnit.SECONDS);
    }

    private Runnable matchmakingTask(PriorityBlockingQueue<Player> playerQueue) {
        return () -> {
            System.out.println("Started player matchmaking...");

            // Temporary collection for squad members
            final List<Player> squad = new ArrayList<>();
            while (true) {
                try {
                    Player player = playerQueue.take();

                    // If squad list already contains some members and
                    // new Player is either solo or from another squad
                    boolean shouldStopCollectingSquad = squad.size() >= Team.MIN_SQUAD_SIZE &&
                            (player.isSolo() || !player.getSquadId().equals(squad.get(0).getSquadId()));

                    if (shouldStopCollectingSquad) {
                        Team team = findTeamForSquad(squad);
                        team.addSquad(squad);
                        squad.clear();

                        processTeam(team);
                    }

                    if (!player.isSolo()) {
                        squad.add(player);
                    } else {
                        Team team = findTeamForPlayer(player);
                        team.addPlayer(player);

                        processTeam(team);
                    }
                } catch (InterruptedException e) {
                    System.out.println("Stopped player matchmaking...");
                    break;
                }
            }
        };
    }

    // Iterates over all Players in all Teams to update Teams priorityLevel
    private Runnable priorityUpdateTask() {
        return () -> {
            incompleteTeams.parallelStream().forEach(updatePriority);
            incompleteMatches.parallelStream().map(Match::getTeam1).forEach(updatePriority);
        };
    }

    // Updates Team priority
    // E.g.
    // If first Player in Team getTimeInQueueInSeconds() = 10 -> Team priorityLevel = 2
    // Depends on MAX_PRIORITY_LEVEL and SECONDS_PER_PRIORITY_LEVEL values in Constants class
    private final Consumer<Team> updatePriority = team -> {
        Player oldestPlayer = team.getPlayers().get(0);
        int newPriorityLevel = Math.toIntExact(oldestPlayer.getTimeInQueueInSeconds() / Constants.SECONDS_PER_PRIORITY_LEVEL);

        if (newPriorityLevel <= Constants.MAX_PRIORITY_LEVEL) {
            int oldPriorityLevel = team.getPriorityLevel();
            team.setPriorityLevel(newPriorityLevel);

            // Internal log
            if (oldPriorityLevel != newPriorityLevel) {
                System.out.println("Updated priorityLevel (" + oldPriorityLevel +
                        "->" + newPriorityLevel + ") for oldest Player in Team (" +
                        oldestPlayer.getTimeInQueueInSeconds() + " seconds in queue)");
            }
        }
    };

    private void processTeam(Team team) {
        if (team.isFull()) {
            incompleteTeams.remove(team);

            Match match = findMatchForTeam(team);
            if (match.isTeamWaiting()) {
                match.setTeam2(team);
                incompleteMatches.remove(match);
                matchBalancingService.rebalanceMatch(match);

                startMatch(match);
            } else {
                match.setTeam1(team);
            }
        }
    }

    private Team findTeamForPlayer(Player player) {
        return incompleteTeams.parallelStream()
                .filter(t -> t.isPlayerEligible(player)).findFirst()
                .orElseGet(() -> {
                   Team newIncompleteTeam = new Team();
                   incompleteTeams.add(newIncompleteTeam);
                   return newIncompleteTeam;
                });
    }

    private Team findTeamForSquad(List<Player> players) {
        return incompleteTeams.parallelStream()
                .filter(t -> t.isSquadEligible(players)).findFirst()
                .orElseGet(() -> {
                    Team newIncompleteTeam = new Team();
                    incompleteTeams.add(newIncompleteTeam);
                    return newIncompleteTeam;
                });
    }

    private Match findMatchForTeam(Team team) {
        return incompleteMatches.parallelStream()
                .filter(m -> m.isMatchBalancedWith(team)).findFirst()
                .orElseGet(() -> {
                   Match newIncompleteMatch = new Match();
                   incompleteMatches.add(newIncompleteMatch);
                   return newIncompleteMatch;
                });
    }

    // At this point, more advanced Match info (e.g. server address)
    // should be sent back to the Players and Match needs to be started.
    // Currently, just log instead
    private void startMatch(Match match) {
        System.out.println("Match is ready! Team skills: " + LogHelper.getTeamSkillsLog(match) +
                " (difference: " + match.getTeamsSkillDifference() +
                ")\nTeam squads: " + LogHelper.getTeamSquadLog(match.getTeam1()) + ", " + LogHelper.getTeamSquadLog(match.getTeam2()) +
                "\nWorst player queue time: " + match.getTeam1().getPlayers().get(0).getTimeInQueueInSeconds() +
                "\nOther teams waiting: " + incompleteMatches.size() +
                "\nIncomplete teams: " + incompleteTeams.size() +
                "\nTotal people in queue: " + (incompleteMatches.size() * Team.FULL_TEAM_SIZE +
                incompleteTeams.parallelStream().mapToInt(t -> t.getPlayers().size()).sum()));
    }

}
