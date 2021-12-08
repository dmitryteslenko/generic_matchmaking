package generic_matchmaking.entity;

import java.time.Duration;
import java.time.Instant;

public class Player {

    private String id;
    private Integer skill;
    private Integer squadId;

    public static final int NO_SQUAD_ID = -1;

    // For internal use
    private final Instant queueStartTime = Instant.now();

    public boolean isSolo() {
        return NO_SQUAD_ID == squadId;
    }

    public long getTimeInQueueInSeconds() {
        return Duration.between(queueStartTime, Instant.now()).getSeconds();
    }


    public Player(String id, Integer skill, Integer squadId) {
        this.id = id;
        this.skill = skill;
        this.squadId = squadId;
    }

    public Integer getSkill() {
        return skill;
    }

    public Integer getSquadId() {
        return squadId;
    }

    public Instant getQueueStartTime() {
        return queueStartTime;
    }

}
