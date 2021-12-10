package generic_matchmaking.entity;

import generic_matchmaking.util.Constants;

public class Match {

    // Team 1 is always filled up first
    private Team team1;
    private Team team2;

    // Determines if difference between Team 1 and passed team2 skills
    // is within Team balance delta + priority overhead
    public boolean isMatchBalancedWith(Team team2) {
        return team1 != null && Constants.Thresholds.TEAM_DELTA +
                Constants.Thresholds.PRIORITY_LEVEL_DELTAS.get(team1.getPriorityLevel())
                >= Math.abs(team1.getTotalSkill() - team2.getTotalSkill());
    }

    // Is Team 1 already filled and waiting
    public boolean isTeamWaiting() {
        return team1 != null;
    }

    public Integer getTeamsSkillDifference() {
        return Math.abs(team1.getTotalSkill() - team2.getTotalSkill());
    }


    public Match() {}

    public Match(Team team1, Team team2) {
        this.team1 = team1;
        this.team2 = team2;
    }

    public Team getTeam1() {
        return team1;
    }

    public void setTeam1(Team team1) {
        this.team1 = team1;
    }

    public Team getTeam2() {
        return team2;
    }

    public void setTeam2(Team team2) {
        if (team1 != null) {
            this.team2 = team2;
        } else {
            throw new IllegalStateException("Can't set Team 2, when Team 1 is not present!");
        }
    }

}
