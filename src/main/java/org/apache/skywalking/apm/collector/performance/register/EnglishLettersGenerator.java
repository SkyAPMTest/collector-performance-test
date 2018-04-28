package org.apache.skywalking.apm.collector.performance.register;

import java.util.Random;

/**
 * @author peng-yongsheng
 */
public enum EnglishLettersGenerator {
    INSTANCE;

    private final String[] letters = new String[] {
        "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
        "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};

    private final Random random = new Random();

    public String generate(int size) {
        StringBuilder builder = new StringBuilder();
        random.ints(size, 0, 25).forEach(i -> builder.append(letters[i]));
        return builder.toString();
    }
}
