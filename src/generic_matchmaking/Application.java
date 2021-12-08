package generic_matchmaking;

import generic_matchmaking.service.MatchmakingService;
import generic_matchmaking.service.MockService;

public class Application {

    public static void main(String[] args) {
        MockService mockService = new MockService();
        MatchmakingService matchmakingService = new MatchmakingService();

        mockService.simulatePlayerQueue();
        matchmakingService.startMatchmaking(MockService.PLAYER_QUEUE);
    }

}