package generic_matchmaking.service;

import generic_matchmaking.config.AppConfig;
import generic_matchmaking.entity.Player;
import generic_matchmaking.entity.Team;
import generic_matchmaking.util.Constants;
import generic_matchmaking.util.RandomUtil;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MockService {

    // Using PriorityBlockingQueue to imitate real life queue of Players, Players in this
    // type of queue will be automatically sorted by their queueStartTime property and
    // squadId on call of take() method
    public static final PriorityBlockingQueue<Player> PLAYER_QUEUE = new PriorityBlockingQueue<>(
            1, Comparator.comparing(Player::getQueueStartTime).thenComparing(Player::getSquadId)
    );

    private final ExecutorService queueExecutor = Executors.newFixedThreadPool(AppConfig.MOCK_QUEUE_THREADS);

    // List of unique squadIds (from 1 to 1000) to take from when generating squad
    private final List<Integer> mockPlayersSquadIds = Collections.synchronizedList(
            Stream.iterate(1, v -> v + 1)
                    .limit(AppConfig.MOCK_PLAYERS_PER_QUEUE_TASK * AppConfig.MOCK_QUEUE_THREADS)
                    .collect(Collectors.toList())
    );

    public void simulatePlayerQueue() {
        System.out.println("Started player queue...");

        for (int i = 0; i < AppConfig.MOCK_QUEUE_THREADS; i++) {
            queueExecutor.execute(queuePlayersTask(AppConfig.MOCK_PLAYERS_PER_QUEUE_TASK));
        }
        queueExecutor.shutdown();
    }

    // Adds Players or Player squads (chosen randomly) to queue every 0.25 seconds
    private Runnable queuePlayersTask(Integer minNumberOfPlayers) {
        return () -> {
            try {
                for (int i = 0; i < minNumberOfPlayers; i++) {
                    // Less likely to add squad
                    if (RandomUtil.shouldGenerateSquad()) {
                        PLAYER_QUEUE.addAll(generatePlayerSquad());
                    } else {
                        PLAYER_QUEUE.add(generatePlayer());
                    }
                    Thread.sleep(250);
                }
            } catch (InterruptedException e) {
                System.out.println("Stopped player queue...");
            }
        };
    }

    // Generates random Player
    public Player generatePlayer() {
        return new Player(
                UUID.randomUUID().toString(),
                RandomUtil.generateIntBetween(AppConfig.MOCK_PLAYER_MIN_SKILL, AppConfig.MOCK_PLAYER_MAX_SKILL),
                Player.NO_SQUAD_ID
        );
    }

    // Generates squad with random Players within one Player skill delta
    public List<Player> generatePlayerSquad() {
        List<Player> playerSquad = new ArrayList<>();
        Collections.shuffle(mockPlayersSquadIds);

        Integer squadId = mockPlayersSquadIds.remove(RandomUtil.generateIntBetween(0, mockPlayersSquadIds.size()));
        int squadSize = RandomUtil.generateIntBetween(Team.MIN_SQUAD_SIZE, Team.FULL_TEAM_SIZE);
        int averageSquadSkill = RandomUtil.generateIntBetween(AppConfig.MOCK_PLAYER_MIN_SKILL, AppConfig.MOCK_PLAYER_MAX_SKILL);

        for (int i = 0; i < squadSize; i++) {
            int minSkill = averageSquadSkill - Constants.Thresholds.PLAYER_DELTA;
            int maxSkill = averageSquadSkill + Constants.Thresholds.PLAYER_DELTA;
            Player player = new Player(
                    UUID.randomUUID().toString(),
                    RandomUtil.generateIntBetween(minSkill > 0 ? minSkill : averageSquadSkill, maxSkill),
                    squadId
            );
            playerSquad.add(player);
        }

        return playerSquad;
    }

}
