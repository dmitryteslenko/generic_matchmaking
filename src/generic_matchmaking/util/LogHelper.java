package generic_matchmaking.util;

import generic_matchmaking.entity.Match;
import generic_matchmaking.entity.Player;
import generic_matchmaking.entity.Team;

import java.util.stream.Collectors;

public final class LogHelper {

    public LogHelper() {}

    public static String getTeamSquadLog(Team team) {
        return team.getPlayers().stream()
                .map(Player::getSquadId)
                .collect(Collectors.toList()).toString();
    }

    public static String getTeamSkillsLog(Match match) {
        return "[" + match.getTeam1().getTotalSkill() + ", " + match.getTeam2().getTotalSkill() + "]";
    }

}
