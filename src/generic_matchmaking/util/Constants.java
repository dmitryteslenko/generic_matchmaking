package generic_matchmaking.util;

import java.util.HashMap;

public final class Constants {

    private Constants() {}

    public static class Thresholds {

        // Maximum skill difference between Players
        public final static int PLAYER_DELTA = 200;

        // Maximum skill difference between Teams
        public final static int TEAM_DELTA = 100;

        // Skill delta to increase by priority level
        private static final int PER_PRIORITY_LEVEL_DELTA = 25;

        public final static HashMap<Integer, Integer> PRIORITY_LEVEL_DELTAS;
        static {
            PRIORITY_LEVEL_DELTAS = new HashMap<>();
            for (int i = 0; i <= MAX_PRIORITY_LEVEL; i++) {
                PRIORITY_LEVEL_DELTAS.put(i, i * PER_PRIORITY_LEVEL_DELTA);
            }
        }

    }

    public static int MAX_PRIORITY_LEVEL = 4;

    // Player waiting time per priority level
    public static int SECONDS_PER_PRIORITY_LEVEL = 5;

}
