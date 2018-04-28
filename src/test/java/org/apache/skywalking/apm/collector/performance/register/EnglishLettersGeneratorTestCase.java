package org.apache.skywalking.apm.collector.performance.register;

import org.junit.*;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class EnglishLettersGeneratorTestCase {

    private static final Logger logger = LoggerFactory.getLogger(EnglishLettersGeneratorTestCase.class);

    @Test
    public void testGenerate() {
        String str = EnglishLettersGenerator.INSTANCE.generate(5);
        logger.info(str);
        Assert.assertEquals(5, str.length());

        str = EnglishLettersGenerator.INSTANCE.generate(10);
        logger.info(str);
        Assert.assertEquals(10, str.length());
    }
}
