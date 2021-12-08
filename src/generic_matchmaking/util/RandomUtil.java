package generic_matchmaking.util;

import generic_matchmaking.config.AppConfig;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomUtil {

    private RandomUtil() {}

    public static int generateIntBetween(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    public static boolean shouldGenerateSquad() {
        return generateIntBetween(0, 100) <= AppConfig.MOCK_SQUAD_CHANCE_TO_GENERATE;
    }

}
